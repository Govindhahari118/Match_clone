package com.match.app.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.match.app.data.local.dao.UserDao
import com.match.app.data.session.SessionStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing subscription/premium status.
 *
 * On payment success:
 * 1. Client calls verifyRazorpayPayment Cloud Function with payment details
 * 2. Function verifies HMAC signature server-side
 * 3. Function updates Firestore `users/{uid}.isPremium = true`
 * 4. This repo observes Firestore and syncs to local Room cache
 */
@Singleton
class SubscriptionRepository @Inject constructor(
    private val userDao: UserDao,
    private val session: SessionStore
) {
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    /**
     * Set before calling Razorpay checkout.open() so that onPaymentSuccess in
     * MainActivity knows which plan was purchased without Android extras fragility.
     */
    @Volatile var pendingPlanType: String = "silver_3m"
    @Volatile var pendingAmountPaise: Int = 0

    /**
     * Create a secure Razorpay Order server-side.
     * Returns the Order ID string.
     */
    suspend fun createRazorpayOrder(amountPaise: Int, planType: String): Result<String> = runCatching {
        val data = hashMapOf(
            "amount" to amountPaise,
            "currency" to "INR",
            "planType" to planType
        )
        val result = functions
            .getHttpsCallable("createRazorpayOrder")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        val resultData = result.data as? Map<String, Any?>
        resultData?.get("id") as? String ?: throw Exception("Invalid order response")
    }

    /**
     * Verify a Razorpay payment server-side and activate premium.
     * Returns true if verification succeeded.
     */
    suspend fun verifyAndActivatePremium(
        orderId: String,
        paymentId: String,
        signature: String,
        planType: String,
        amount: Int
    ): Result<Long> = runCatching {
        val data = hashMapOf(
            "orderId" to orderId,
            "paymentId" to paymentId,
            "signature" to signature,
            "planType" to planType,
            "amount" to amount
        )

        val result = functions
            .getHttpsCallable("verifyRazorpayPayment")
            .call(data)
            .await()

        @Suppress("UNCHECKED_CAST")
        val resultData = result.data as? Map<String, Any?>
        val premiumUntil = (resultData?.get("premiumUntil") as? Number)?.toLong() ?: 0L

        // Sync premium status to local Room cache
        val userId = session.userId.first() ?: throw Exception("Not logged in")
        val user = userDao.findById(userId) ?: throw Exception("User not found")
        userDao.update(user.copy(isPremium = true))

        premiumUntil
    }

    /** Observe premium status changes from Firestore in real-time. */
    fun observePremiumStatus(): Flow<Boolean> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { trySend(false); close(); return@callbackFlow }

        val reg = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(false); return@addSnapshotListener }
                val isPremium = snap?.getBoolean("isPremium") ?: false
                trySend(isPremium)
            }
        awaitClose { reg.remove() }
    }

    /** Check current premium status from Firestore (one-shot). */
    suspend fun checkPremiumStatus(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val isPremium = doc.getBoolean("isPremium") ?: false
            // Sync to local
            val userId = session.userId.first()
            if (userId != null) {
                val user = userDao.findById(userId)
                if (user != null && user.isPremium != isPremium) {
                    userDao.update(user.copy(isPremium = isPremium))
                }
            }
            isPremium
        } catch (e: Exception) {
            Log.w("SubscriptionRepo", "Failed to check premium status", e)
            false
        }
    }

    /** Get premium expiry timestamp from Firestore. */
    suspend fun getPremiumExpiry(): Long {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return 0L
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val timestamp = doc.getTimestamp("premiumUntil")
            timestamp?.toDate()?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Count how many contact reveals the current user has used this calendar month.
     * Stored in Firestore under users/{uid}.contactsRevealedThisMonth / contactsResetAt.
     * Resets automatically when a new month begins.
     */
    suspend fun getContactsUsedThisMonth(): Int {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return 0
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val resetAt = doc.getLong("contactsResetAt") ?: 0L
            // If reset timestamp is from a previous month, count is 0
            val cal = java.util.Calendar.getInstance()
            val resetCal = java.util.Calendar.getInstance().also { it.timeInMillis = resetAt }
            val sameMonth = cal.get(java.util.Calendar.YEAR) == resetCal.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.MONTH) == resetCal.get(java.util.Calendar.MONTH)
            if (!sameMonth) 0 else (doc.getLong("contactsRevealedThisMonth") ?: 0L).toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Increment the contacts-revealed counter in Firestore.
     * Returns the updated count, or -1 on failure.
     */
    suspend fun incrementContactsUsed(): Int {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return -1
        return try {
            val ref = db.collection("users").document(uid)
            val doc = ref.get().await()
            val resetAt = doc.getLong("contactsResetAt") ?: 0L
            val cal = java.util.Calendar.getInstance()
            val resetCal = java.util.Calendar.getInstance().also { it.timeInMillis = resetAt }
            val sameMonth = cal.get(java.util.Calendar.YEAR) == resetCal.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.MONTH) == resetCal.get(java.util.Calendar.MONTH)
            val currentCount = if (!sameMonth) 0 else (doc.getLong("contactsRevealedThisMonth") ?: 0L).toInt()
            val newCount = currentCount + 1
            ref.update(mapOf(
                "contactsRevealedThisMonth" to newCount,
                "contactsResetAt" to if (!sameMonth) System.currentTimeMillis() else resetAt
            )).await()
            newCount
        } catch (e: Exception) {
            -1
        }
    }
}
