package com.match.app.data.repo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

@Singleton
class SubscriptionRepository @Inject constructor(
    private val userDao: UserDao,
    private val session: SessionStore
) {
    data class PlayEntitlement(
        val entitlementType: String,
        val entitlementId: String,
        val expiresAt: Long,
        val consumptionPending: Boolean
    )
    data class RevealedContact(val phoneNumber: String, val contactsUsed: Int, val contactsLimit: Int)

    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()


    fun observePremiumStatus(): Flow<Boolean> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val reg = db.collection("users").document(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(false)
                return@addSnapshotListener
            }
            val expiry = snap?.getTimestamp("premiumUntil")?.toDate()?.time
                ?: snap?.getLong("subscriptionExpiry") ?: 0L
            trySend((snap?.getBoolean("isPremium") == true) && expiry > System.currentTimeMillis())
        }
        awaitClose { reg.remove() }
    }

    suspend fun checkPremiumStatus(): Boolean = try {
        syncPremiumStatusFromServer()
    } catch (e: Exception) {
        Log.w("SubscriptionRepo", "Failed to check premium status", e)
        false
    }

    private suspend fun syncPremiumStatusFromServer(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val doc = db.collection("users").document(uid).get().await()
        val expiry = doc.getTimestamp("premiumUntil")?.toDate()?.time
            ?: doc.getLong("subscriptionExpiry") ?: 0L
        val active = (doc.getBoolean("isPremium") == true) && expiry > System.currentTimeMillis()
        val localId = session.userId.first()
        if (localId != null) {
            val user = userDao.findById(localId)
            if (user != null) {
                userDao.update(
                    user.copy(
                        isPremium = active,
                        subscriptionPlan = if (active) {
                            doc.getString("subscriptionPlan") ?: "FREE"
                        } else {
                            "FREE"
                        },
                        subscriptionExpiry = if (active) expiry else 0L
                    )
                )
            }
        }
        return active
    }

    suspend fun getPremiumExpiry(): Long {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return 0L
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.getTimestamp("premiumUntil")?.toDate()?.time
                ?: doc.getLong("subscriptionExpiry") ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Reads the authenticated account's boost from a trusted callable. The backing subscriptions
     * document is intentionally inaccessible to clients and is also the source used for ranking.
     */
    suspend fun getBoostExpiry(): Long = try {
        syncBoostStatusFromServer()
    } catch (e: Exception) {
        Log.w("SubscriptionRepo", "Failed to refresh boost status", e)
        0L
    }

    private suspend fun syncBoostStatusFromServer(): Long {
        val result = functions.getHttpsCallable("getMyBoostStatus").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid boost status response")
        val boostUntil = (data["boostUntil"] as? Number)?.toLong() ?: 0L
        val localId = session.userId.first()
        if (localId != null) {
            userDao.updateBoostExpiry(localId, boostUntil)
        }
        return boostUntil
    }

    /** Contact reveal is a privileged server action; no client-side counter mutation. */
    suspend fun revealContact(targetUid: String): Result<RevealedContact> = runCatching {
        val result = functions.getHttpsCallable("consumeContactReveal")
            .call(mapOf("targetUid" to targetUid)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid contact response")
        RevealedContact(
            phoneNumber = data["phoneNumber"] as? String ?: "",
            contactsUsed = (data["contactsUsed"] as? Number)?.toInt() ?: 0,
            contactsLimit = (data["contactsLimit"] as? Number)?.toInt() ?: 0
        )
    }
}
