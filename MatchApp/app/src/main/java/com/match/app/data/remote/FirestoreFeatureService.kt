package com.match.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified backend service for events, counselling, referrals, assisted service,
 * background checks, secure call requests, analytics, communities and rewards.
 *
 * Security-sensitive mutations and reward-ledger reads go through authenticated callable
 * functions. Firestore is used directly only for owner-authorized reads and server-managed
 * catalogue data.
 */
@Singleton
class FirestoreFeatureService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    private val eventsCol = db.collection("events")
    private val eventRegsCol = db.collection("eventRegistrations")
    private val bookingsCol = db.collection("counsellingBookings")
    private val referralsCol = db.collection("referrals")
    private val rmRequestsCol = db.collection("rmRequests")
    private val bgChecksCol = db.collection("backgroundChecks")
    private val callRequestsCol = db.collection("callRequests")
    private val analyticsCol = db.collection("profileAnalytics")
    private val communitiesCol = db.collection("communities")

    suspend fun getUpcomingEvents(): List<Map<String, Any?>> {
        val snap = eventsCol
            .whereGreaterThanOrEqualTo("dateMillis", System.currentTimeMillis())
            .orderBy("dateMillis", Query.Direction.ASCENDING)
            .limit(30)
            .get().await()
        return snap.documents.map { doc -> (doc.data ?: emptyMap()) + ("id" to doc.id) }
    }

    suspend fun registerForEvent(uid: String, eventId: String) {
        require(uid.isNotBlank() && eventId.isNotBlank())
        functions.getHttpsCallable("registerForEvent")
            .call(mapOf("eventId" to eventId))
            .await()
    }

    suspend fun getRegisteredEventIds(uid: String): Set<String> {
        if (uid.isBlank()) return emptySet()
        val snap = eventRegsCol.whereEqualTo("uid", uid).get().await()
        return snap.documents.mapNotNull { it.getString("eventId") }.toSet()
    }

    suspend fun bookCounselling(
        uid: String,
        counsellorName: String,
        sessionType: String,
        mode: String,
        date: String,
        time: String
    ): String {
        require(uid.isNotBlank())
        val result = functions.getHttpsCallable("bookCounselling").call(
            mapOf(
                "counsellor" to counsellorName,
                "sessionType" to sessionType,
                "mode" to mode,
                "date" to date,
                "time" to time
            )
        ).await()
        return result.stringField("bookingId")
    }

    suspend fun getCounsellingBookings(uid: String): List<Map<String, Any?>> {
        if (uid.isBlank()) return emptyList()
        return ownerDocuments(bookingsCol.whereEqualTo("uid", uid), limit = 50)
    }

    suspend fun recordReferral(referrerUid: String, referredEmail: String) {
        require(referrerUid.isNotBlank())
        functions.getHttpsCallable("recordReferral")
            .call(mapOf("referredEmail" to referredEmail))
            .await()
    }

    suspend fun getReferralStats(uid: String): ReferralStats {
        if (uid.isBlank()) return ReferralStats(0, 0, 0)
        val snap = referralsCol.whereEqualTo("referrerUid", uid).get().await()
        val total = snap.size()
        val active = snap.documents.count { it.getString("status") == "joined" }
        return ReferralStats(total = total, active = active, coinsEarned = active * 100)
    }

    data class ReferralStats(val total: Int, val active: Int, val coinsEarned: Int)

    suspend fun requestRM(
        uid: String,
        plan: String,
        preferences: String,
        name: String = "",
        phone: String = ""
    ): String {
        require(uid.isNotBlank())
        val result = functions.getHttpsCallable("requestRelationshipManager").call(
            mapOf("plan" to plan, "preferences" to preferences, "name" to name, "phone" to phone)
        ).await()
        return result.stringField("requestId")
    }

    suspend fun getRMRequest(uid: String): Map<String, Any?>? {
        if (uid.isBlank()) return null
        return ownerDocuments(rmRequestsCol.whereEqualTo("uid", uid), limit = 50).firstOrNull()
    }

    suspend fun requestBackgroundCheck(uid: String, targetUid: String, plan: String): String {
        require(uid.isNotBlank() && targetUid.isNotBlank())
        val result = functions.getHttpsCallable("requestBackgroundCheck")
            .call(mapOf("targetUid" to targetUid, "plan" to plan))
            .await()
        return result.stringField("requestId")
    }

    suspend fun getBackgroundChecks(uid: String): List<Map<String, Any?>> {
        if (uid.isBlank()) return emptyList()
        return ownerDocuments(bgChecksCol.whereEqualTo("requestedBy", uid), limit = 50)
    }

    suspend fun requestCall(
        fromUid: String,
        toUid: String,
        type: String,
        scheduledAt: String = ""
    ): String {
        require(fromUid.isNotBlank() && toUid.isNotBlank() && fromUid != toUid)
        val result = functions.getHttpsCallable("requestSecureCall").call(
            mapOf("targetUid" to toUid, "type" to type, "scheduledAt" to scheduledAt)
        ).await()
        return result.stringField("requestId")
    }

    suspend fun getCallRequests(uid: String): List<Map<String, Any?>> {
        if (uid.isBlank()) return emptyList()
        val outgoing = callRequestsCol.whereEqualTo("fromUid", uid).limit(50).get().await()
        val incoming = callRequestsCol.whereEqualTo("toUid", uid).limit(50).get().await()
        return (outgoing.documents + incoming.documents)
            .distinctBy { it.id }
            .map { (it.data ?: emptyMap()) + ("id" to it.id) }
            .sortedByDescending { createdAtMillis(it) }
    }

    suspend fun respondToCall(requestId: String, accept: Boolean) {
        require(requestId.isNotBlank())
        functions.getHttpsCallable("respondToSecureCall")
            .call(mapOf("requestId" to requestId, "accept" to accept))
            .await()
    }

    /**
     * Legacy snapshot writer retained until analytics migration completes. Security rules keep
     * this server-managed, so callers should use read-only analytics in production.
     */
    suspend fun saveAnalyticsSnapshot(uid: String, data: Map<String, Any>) {
        val weekKey = (System.currentTimeMillis() / (7 * 24 * 60 * 60 * 1000L)).toString()
        analyticsCol.document(uid).collection("weekly").document(weekKey).set(
            data + ("timestamp" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun getAnalyticsSnapshots(uid: String, limit: Int = 8): List<Map<String, Any?>> {
        if (uid.isBlank()) return emptyList()
        val snap = analyticsCol.document(uid).collection("weekly")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong()).get().await()
        return snap.documents.map { it.data ?: emptyMap() }
    }

    suspend fun getCommunities(limit: Int = 30): List<Map<String, Any?>> {
        val snap = communitiesCol.orderBy("memberCount", Query.Direction.DESCENDING)
            .limit(limit.toLong()).get().await()
        return snap.documents.map { (it.data ?: emptyMap()) + ("id" to it.id) }
    }

    suspend fun joinCommunity(uid: String, communityId: String) {
        require(uid.isNotBlank() && communityId.isNotBlank())
        functions.getHttpsCallable("joinCommunity")
            .call(mapOf("communityId" to communityId))
            .await()
    }

    suspend fun getRewardsState(uid: String): Map<String, Any?> {
        require(uid.isNotBlank())
        val result = functions.getHttpsCallable("getRewardsState").call().await()
        @Suppress("UNCHECKED_CAST")
        return result.data as? Map<String, Any?> ?: emptyMap()
    }

    suspend fun claimDailyReward(uid: String, streak: Int, rewardCoins: Int) {
        require(uid.isNotBlank())
        // Client values are intentionally ignored; the server calculates streak and reward amount.
        functions.getHttpsCallable("claimDailyReward").call().await()
    }

    suspend fun redeemReward(uid: String, rewardId: String, cost: Int) {
        require(uid.isNotBlank())
        // Cost is intentionally ignored; the server owns the reward catalogue and price.
        functions.getHttpsCallable("redeemReward")
            .call(mapOf("rewardId" to rewardId))
            .await()
    }

    private suspend fun ownerDocuments(query: Query, limit: Long): List<Map<String, Any?>> {
        val snap = query.limit(limit).get().await()
        return snap.documents
            .map { (it.data ?: emptyMap()) + ("id" to it.id) }
            .sortedByDescending { createdAtMillis(it) }
    }

    private fun createdAtMillis(data: Map<String, Any?>): Long = when (val raw = data["createdAt"]) {
        is Timestamp -> raw.toDate().time
        is Number -> raw.toLong()
        else -> 0L
    }

    private fun com.google.firebase.functions.HttpsCallableResult.stringField(name: String): String {
        @Suppress("UNCHECKED_CAST")
        val payload = data as? Map<String, Any?> ?: error("Invalid server response")
        return payload[name] as? String ?: error("Missing $name")
    }
}
