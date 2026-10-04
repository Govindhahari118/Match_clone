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
    data class ContactAccessRequestResult(val status: String, val canReveal: Boolean)
    data class ContactAccessRequest(
        val requesterUid: String,
        val targetUid: String,
        val status: String,
        val createdAtMillis: Long,
        val updatedAtMillis: Long
    )

    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    /**
     * The backend validates the opaque token with Google Play and owns all product-to-entitlement
     * mapping. Android never chooses membership duration, boost duration, price or expiry.
     */
    suspend fun verifyGooglePlayPurchase(productId: String, purchaseToken: String): Result<PlayEntitlement> = runCatching {
        require(productId.isNotBlank() && purchaseToken.isNotBlank())
        val result = functions.getHttpsCallable("verifyGooglePlayPurchase")
            .call(mapOf("productId" to productId, "purchaseToken" to purchaseToken)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid Play verification response")
        if (data["success"] as? Boolean != true) error("Play purchase was not activated")

        val entitlementType = data["entitlementType"] as? String ?: "MEMBERSHIP"
        val entitlementId = data["entitlementId"] as? String
            ?: data["planId"] as? String
            ?: error("Missing entitlement id")
        val expiresAt = (data["expiresAt"] as? Number)?.toLong()
            ?: (data["premiumUntil"] as? Number)?.toLong()
            ?: (data["boostUntil"] as? Number)?.toLong()
            ?: error("Missing entitlement expiry")

        val entitlement = PlayEntitlement(
            entitlementType = entitlementType,
            entitlementId = entitlementId,
            expiresAt = expiresAt,
            consumptionPending = data["consumptionPending"] as? Boolean ?: false
        )
        if (entitlementType == "BOOST") {
            syncBoostStatusFromServer()
        } else {
            syncPremiumStatusFromServer()
        }
        entitlement
    }

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
            // The public document carries only a lightweight badge. Detailed plan and expiry
            // are private and refreshed through getMyMembershipStatus.
            trySend(snap?.getBoolean("isPremium") == true)
        }
        awaitClose { reg.remove() }
    }

    suspend fun checkPremiumStatus(): Boolean = try {
        syncPremiumStatusFromServer()
    } catch (e: Exception) {
        Log.w("SubscriptionRepo", "Failed to check premium status", e)
        false
    }

    private data class MembershipStatus(
        val active: Boolean,
        val planId: String,
        val expiresAtMillis: Long
    )

    private suspend fun fetchMembershipStatus(): MembershipStatus {
        if (FirebaseAuth.getInstance().currentUser?.uid.isNullOrBlank()) {
            return MembershipStatus(false, "FREE", 0L)
        }
        val result = functions.getHttpsCallable("getMyMembershipStatus").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid membership status response")
        val active = data["active"] as? Boolean ?: false
        val expiry = (data["expiresAtMillis"] as? Number)?.toLong() ?: 0L
        val planId = if (active) data["planId"] as? String ?: "FREE" else "FREE"
        return MembershipStatus(active, planId, if (active) expiry else 0L)
    }

    private suspend fun syncPremiumStatusFromServer(): Boolean {
        val status = fetchMembershipStatus()
        session.setSubscriptionPlan(status.planId)
        val localId = session.userId.first()
        if (localId != null) {
            val user = userDao.findById(localId)
            if (user != null) {
                userDao.update(
                    user.copy(
                        isPremium = status.active,
                        subscriptionPlan = status.planId,
                        subscriptionExpiry = status.expiresAtMillis
                    )
                )
            }
        }
        return status.active
    }

    suspend fun getPremiumExpiry(): Long = try {
        fetchMembershipStatus().expiresAtMillis
    } catch (e: Exception) {
        Log.w("SubscriptionRepo", "Failed to refresh premium expiry", e)
        0L
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

    fun observeIncomingContactRequests(): Flow<List<ContactAccessRequest>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = db.collection("contactRequests")
            .whereEqualTo("targetUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val values = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val requesterUid = doc.getString("requesterUid")?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val targetUid = doc.getString("targetUid")?.takeIf { it.isNotBlank() }
                        ?: return@mapNotNull null
                    val status = doc.getString("status").orEmpty()
                    if (status != "PENDING") return@mapNotNull null
                    ContactAccessRequest(
                        requesterUid = requesterUid,
                        targetUid = targetUid,
                        status = status,
                        createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.updatedAtMillis }
                trySend(values)
            }
        awaitClose { reg.remove() }
    }

    suspend fun requestContactAccess(targetUid: String): Result<ContactAccessRequestResult> = runCatching {
        require(targetUid.isNotBlank()) { "Missing target profile" }
        val result = functions.getHttpsCallable("requestContactAccess")
            .call(mapOf("targetUid" to targetUid)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid contact request response")
        ContactAccessRequestResult(
            status = data["status"] as? String ?: "UNKNOWN",
            canReveal = data["canReveal"] as? Boolean ?: false
        )
    }

    suspend fun respondContactAccess(requesterUid: String, approve: Boolean): Result<String> = runCatching {
        require(requesterUid.isNotBlank()) { "Missing requester profile" }
        val result = functions.getHttpsCallable("respondContactAccess")
            .call(mapOf("requesterUid" to requesterUid, "approve" to approve)).await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid contact response")
        data["status"] as? String ?: error("Missing contact request status")
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
