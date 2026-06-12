package com.match.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore service for interests (likes) and mutual matches.
 *
 * Collections:
 * - `interests/{fromUid}_{toUid}` — a like from one user to another
 * - `matches/{matchId}`           — created when both users like each other
 */
@Singleton
class FirestoreInterestService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val interestsCol = db.collection("interests")
    private val matchesCol = db.collection("matches")

    // ── Like / Unlike ─────────────────────────────────────────────────────

    /** Send interest (like) and detect mutual match atomically. Returns true if mutual match detected. */
    suspend fun sendInterest(fromUid: String, toUid: String, isSuperLike: Boolean = false): Boolean {
        if (fromUid.isBlank() || toUid.isBlank()) return false
        val docId = "${fromUid}_${toUid}"
        val reverseDocId = "${toUid}_${fromUid}"

        // Write the interest document
        interestsCol.document(docId).set(
            mapOf(
                "fromUid" to fromUid,
                "toUid" to toUid,
                "isSuperLike" to isSuperLike,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Atomically check for reverse interest + create match in a transaction
        return db.runTransaction { txn ->
            val reverseRef = interestsCol.document(reverseDocId)
            val reverseSnap = txn.get(reverseRef)
            if (reverseSnap.exists()) {
                // Mutual match! Create match doc inside the same transaction
                val matchId = matchDocId(fromUid, toUid)
                val matchRef = matchesCol.document(matchId)
                txn.set(matchRef, mapOf(
                    "users" to listOf(fromUid, toUid).sorted(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastActivity" to FieldValue.serverTimestamp()
                ))
                true
            } else {
                false
            }
        }.await()
    }

    /** Remove interest (unlike). Also removes any existing match. */
    suspend fun removeInterest(fromUid: String, toUid: String) {
        if (fromUid.isBlank() || toUid.isBlank()) return
        interestsCol.document("${fromUid}_${toUid}").delete().await()
        // Remove match if it existed
        val matchId = matchDocId(fromUid, toUid)
        val matchDoc = matchesCol.document(matchId).get().await()
        if (matchDoc.exists()) {
            matchesCol.document(matchId).delete().await()
        }
    }

    /** Check if an interest exists. */
    suspend fun isInterested(fromUid: String, toUid: String): Boolean {
        if (fromUid.isBlank() || toUid.isBlank()) return false
        return interestsCol.document("${fromUid}_${toUid}").get().await().exists()
    }

    // ── Matches ───────────────────────────────────────────────────────────

    /** Observe mutual matches for a user in real-time. */
    fun observeMatches(myUid: String): Flow<List<MatchDoc>> = callbackFlow {
        val reg = matchesCol
            .whereArrayContains("users", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val matches = snap?.documents?.mapNotNull { doc ->
                    @Suppress("UNCHECKED_CAST")
                    val users = doc.get("users") as? List<String> ?: return@mapNotNull null
                    val otherUid = users.firstOrNull { it != myUid } ?: return@mapNotNull null
                    MatchDoc(
                        matchId = doc.id,
                        myUid = myUid,
                        otherUid = otherUid,
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                } ?: emptyList()
                trySend(matches)
            }
        awaitClose { reg.remove() }
    }

    /** Observe interests sent TO this user (incoming likes). */
    fun observeIncomingInterests(myUid: String): Flow<List<InterestDoc>> = callbackFlow {
        val reg = interestsCol
            .whereEqualTo("toUid", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val interests = snap?.documents?.mapNotNull { doc ->
                    InterestDoc(
                        fromUid = doc.getString("fromUid") ?: return@mapNotNull null,
                        toUid = myUid,
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                } ?: emptyList()
                trySend(interests)
            }
        awaitClose { reg.remove() }
    }

    /** Observe interests sent BY this user (outgoing likes). */
    fun observeOutgoingInterests(myUid: String): Flow<List<InterestDoc>> = callbackFlow {
        val reg = interestsCol
            .whereEqualTo("fromUid", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val interests = snap?.documents?.mapNotNull { doc ->
                    InterestDoc(
                        fromUid = myUid,
                        toUid = doc.getString("toUid") ?: return@mapNotNull null,
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                } ?: emptyList()
                trySend(interests)
            }
        awaitClose { reg.remove() }
    }

    /** Get UIDs of all users this person has liked (one-shot). */
    suspend fun getSentInterestUids(myUid: String): Set<String> {
        val snap = interestsCol.whereEqualTo("fromUid", myUid).get().await()
        return snap.documents.mapNotNull { it.getString("toUid") }.toSet()
    }

    /** Get UIDs of all users who liked this person (one-shot). */
    suspend fun getReceivedInterestUids(myUid: String): Set<String> {
        val snap = interestsCol.whereEqualTo("toUid", myUid).get().await()
        return snap.documents.mapNotNull { it.getString("fromUid") }.toSet()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun matchDocId(uid1: String, uid2: String): String {
        val sorted = listOf(uid1, uid2).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }
}

data class MatchDoc(
    val matchId: String,
    val myUid: String,
    val otherUid: String,
    val createdAt: Long
)

data class InterestDoc(
    val fromUid: String,
    val toUid: String,
    val createdAt: Long
)
