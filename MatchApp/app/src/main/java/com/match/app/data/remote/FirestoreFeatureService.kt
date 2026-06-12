package com.match.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified Firestore service for premium features:
 * events, counselling, referrals, assisted-RM, background checks,
 * secure call requests, virtual meet scheduling.
 *
 * Each feature uses its own top-level collection.
 */
@Singleton
class FirestoreFeatureService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    // ── Live Events ─────────────────────────────────────────────────────

    private val eventsCol     = db.collection("events")
    private val eventRegsCol  = db.collection("eventRegistrations")

    /** Fetch upcoming events ordered by date. */
    suspend fun getUpcomingEvents(): List<Map<String, Any?>> {
        val snap = eventsCol
            .whereGreaterThanOrEqualTo("dateMillis", System.currentTimeMillis())
            .orderBy("dateMillis", Query.Direction.ASCENDING)
            .limit(30)
            .get().await()
        return snap.documents.map { doc ->
            (doc.data ?: emptyMap()) + ("id" to doc.id)
        }
    }

    /** Register a user for an event. */
    suspend fun registerForEvent(uid: String, eventId: String) {
        val docId = "${uid}_${eventId}"
        eventRegsCol.document(docId).set(mapOf(
            "uid" to uid,
            "eventId" to eventId,
            "registeredAt" to FieldValue.serverTimestamp()
        )).await()
        // Increment attendee count
        eventsCol.document(eventId).update(
            "attendees", FieldValue.increment(1)
        ).await()
    }

    /** Get event IDs the user has registered for. */
    suspend fun getRegisteredEventIds(uid: String): Set<String> {
        val snap = eventRegsCol.whereEqualTo("uid", uid).get().await()
        return snap.documents.mapNotNull { it.getString("eventId") }.toSet()
    }

    // ── Counselling ─────────────────────────────────────────────────────

    private val bookingsCol = db.collection("counsellingBookings")

    /** Book a counselling session. */
    suspend fun bookCounselling(
        uid: String, counsellorName: String, sessionType: String,
        mode: String, date: String, time: String
    ): String {
        val ref = bookingsCol.add(mapOf(
            "uid" to uid,
            "counsellor" to counsellorName,
            "sessionType" to sessionType,
            "mode" to mode,
            "date" to date,
            "time" to time,
            "status" to "confirmed",
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        return ref.id
    }

    /** Get user's counselling bookings. */
    suspend fun getCounsellingBookings(uid: String): List<Map<String, Any?>> {
        val snap = bookingsCol.whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20).get().await()
        return snap.documents.map { (it.data ?: emptyMap()) + ("id" to it.id) }
    }

    // ── Referrals ───────────────────────────────────────────────────────

    private val referralsCol = db.collection("referrals")

    /** Record a referral. */
    suspend fun recordReferral(referrerUid: String, referredEmail: String) {
        referralsCol.add(mapOf(
            "referrerUid" to referrerUid,
            "referredEmail" to referredEmail,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
    }

    /** Get referral stats for a user. */
    suspend fun getReferralStats(uid: String): ReferralStats {
        val snap = referralsCol.whereEqualTo("referrerUid", uid).get().await()
        val total = snap.size()
        val active = snap.documents.count { it.getString("status") == "joined" }
        return ReferralStats(total = total, active = active, coinsEarned = active * 100)
    }

    data class ReferralStats(val total: Int, val active: Int, val coinsEarned: Int)

    // ── Assisted RM ─────────────────────────────────────────────────────

    private val rmRequestsCol = db.collection("rmRequests")

    /** Submit an RM service request. */
    suspend fun requestRM(uid: String, plan: String, preferences: String): String {
        val ref = rmRequestsCol.add(mapOf(
            "uid" to uid,
            "plan" to plan,
            "preferences" to preferences,
            "status" to "pending",
            "assignedRM" to null,
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        return ref.id
    }

    /** Get user's RM request status. */
    suspend fun getRMRequest(uid: String): Map<String, Any?>? {
        val snap = rmRequestsCol.whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1).get().await()
        return snap.documents.firstOrNull()?.let { (it.data ?: emptyMap()) + ("id" to it.id) }
    }

    // ── Background Check ────────────────────────────────────────────────

    private val bgChecksCol = db.collection("backgroundChecks")

    /** Request a background check on a target profile. */
    suspend fun requestBackgroundCheck(
        uid: String, targetUid: String, plan: String
    ): String {
        val ref = bgChecksCol.add(mapOf(
            "requestedBy" to uid,
            "targetUid" to targetUid,
            "plan" to plan,
            "status" to "submitted",
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        return ref.id
    }

    /** Get user's background check requests. */
    suspend fun getBackgroundChecks(uid: String): List<Map<String, Any?>> {
        val snap = bgChecksCol.whereEqualTo("requestedBy", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20).get().await()
        return snap.documents.map { (it.data ?: emptyMap()) + ("id" to it.id) }
    }

    // ── Secure Call / Virtual Meet ──────────────────────────────────────

    private val callRequestsCol = db.collection("callRequests")

    /** Request a secure call or virtual meet with another user. */
    suspend fun requestCall(
        fromUid: String, toUid: String, type: String, scheduledAt: String = ""
    ): String {
        val ref = callRequestsCol.add(mapOf(
            "fromUid" to fromUid,
            "toUid" to toUid,
            "type" to type, // "voice", "video", "virtual_meet"
            "scheduledAt" to scheduledAt,
            "status" to "requested",
            "createdAt" to FieldValue.serverTimestamp()
        )).await()
        return ref.id
    }

    /** Get call requests for a user (incoming + outgoing). */
    suspend fun getCallRequests(uid: String): List<Map<String, Any?>> {
        val outgoing = callRequestsCol.whereEqualTo("fromUid", uid).get().await()
        val incoming = callRequestsCol.whereEqualTo("toUid", uid).get().await()
        return (outgoing.documents + incoming.documents).map {
            (it.data ?: emptyMap()) + ("id" to it.id)
        }
    }

    /** Accept or decline a call request. */
    suspend fun respondToCall(requestId: String, accept: Boolean) {
        callRequestsCol.document(requestId).update(
            "status", if (accept) "accepted" else "declined"
        ).await()
    }

    // ── Profile Analytics ───────────────────────────────────────────────

    private val analyticsCol = db.collection("profileAnalytics")

    /** Record a weekly analytics snapshot for a user. */
    suspend fun saveAnalyticsSnapshot(uid: String, data: Map<String, Any>) {
        val weekKey = (System.currentTimeMillis() / (7 * 24 * 60 * 60 * 1000L)).toString()
        analyticsCol.document(uid).collection("weekly").document(weekKey).set(
            data + ("timestamp" to FieldValue.serverTimestamp())
        ).await()
    }

    /** Get recent analytics snapshots. */
    suspend fun getAnalyticsSnapshots(uid: String, limit: Int = 8): List<Map<String, Any?>> {
        val snap = analyticsCol.document(uid).collection("weekly")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong()).get().await()
        return snap.documents.map { it.data ?: emptyMap() }
    }

    // ── Community Groups ────────────────────────────────────────────────

    private val communitiesCol = db.collection("communities")

    /** Fetch community groups. */
    suspend fun getCommunities(limit: Int = 30): List<Map<String, Any?>> {
        val snap = communitiesCol.orderBy("memberCount", Query.Direction.DESCENDING)
            .limit(limit.toLong()).get().await()
        return snap.documents.map { (it.data ?: emptyMap()) + ("id" to it.id) }
    }

    /** Join a community. */
    suspend fun joinCommunity(uid: String, communityId: String) {
        communitiesCol.document(communityId).collection("members")
            .document(uid).set(mapOf(
                "uid" to uid,
                "joinedAt" to FieldValue.serverTimestamp()
            )).await()
        communitiesCol.document(communityId).update(
            "memberCount", FieldValue.increment(1)
        ).await()
    }

    // ── Rewards & Gamification ─────────────────────────────────────────

    private val rewardsCol = db.collection("rewards")

    /** Get user's rewards state: coins, streak, lastClaimed. */
    suspend fun getRewardsState(uid: String): Map<String, Any?> {
        val doc = rewardsCol.document(uid).get().await()
        return doc.data ?: mapOf("coins" to 0, "streak" to 0, "lastClaimed" to null)
    }

    /** Claim daily login reward. */
    suspend fun claimDailyReward(uid: String, streak: Int, rewardCoins: Int) {
        rewardsCol.document(uid).set(mapOf(
            "coins" to FieldValue.increment(rewardCoins.toLong()),
            "streak" to streak,
            "lastClaimed" to FieldValue.serverTimestamp()
        ), com.google.firebase.firestore.SetOptions.merge()).await()
    }

    /** Update completion status of a daily task. */
    suspend fun completeTask(uid: String, taskId: String, coins: Int) {
        val taskRef = rewardsCol.document(uid).collection("tasks").document(taskId)
        val taskDoc = taskRef.get().await()
        if (taskDoc.exists() && taskDoc.getBoolean("done") == true) return

        taskRef.set(mapOf(
            "done" to true,
            "completedAt" to FieldValue.serverTimestamp()
        )).await()

        rewardsCol.document(uid).update("coins", FieldValue.increment(coins.toLong())).await()
    }

    /** Get user's completed tasks for today. */
    suspend fun getCompletedTasks(uid: String): Set<String> {
        val snap = rewardsCol.document(uid).collection("tasks").get().await()
        return snap.documents.map { it.id }.toSet()
    }

    /** Redeem coins for a specific reward. */
    suspend fun redeemReward(uid: String, rewardId: String, cost: Int) {
        // Atomic transaction: Check balance and decrement in one go
        db.runTransaction { transaction ->
            val ref = rewardsCol.document(uid)
            val snap = transaction.get(ref)
            val currentCoins = snap.getLong("coins") ?: 0L
            
            if (currentCoins < cost) {
                throw Exception("Insufficient coins")
            }
            
            transaction.update(ref, "coins", currentCoins - cost)
            
            // Log the redemption
            val logRef = ref.collection("redemptions").document()
            transaction.set(logRef, mapOf(
                "rewardId" to rewardId,
                "cost" to cost,
                "redeemedAt" to FieldValue.serverTimestamp()
            ))
        }.await()
    }
}
