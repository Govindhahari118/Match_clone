package com.match.app.data.remote

import android.util.Log
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
 * Firestore service for blocked users.
 *
 * Collection: `blocks/{blockerUid}/blocked/{blockedUid}`
 */
@Singleton
class FirestoreBlockService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    private fun blockedCol(blockerUid: String) =
        db.collection("blocks").document(blockerUid).collection("blocked")

    /** Block a user and cascade-remove any interests / matches between them. */
    suspend fun block(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return
        blockedCol(blockerUid).document(blockedUid).set(
            mapOf(
                "blockedUid" to blockedUid,
                "blockedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Best-effort cleanup: delete both-direction interests and any match
        try {
            val interestsCol = db.collection("interests")
            val matchesCol = db.collection("matches")

            // Delete interest docs in both directions
            val fwd = "${blockerUid}_${blockedUid}"
            val rev = "${blockedUid}_${blockerUid}"
            interestsCol.document(fwd).delete().await()
            interestsCol.document(rev).delete().await()

            // Delete match (deterministic doc ID = sorted UIDs joined by underscore)
            val matchId = listOf(blockerUid, blockedUid).sorted().joinToString("_")
            val matchDoc = matchesCol.document(matchId).get().await()
            if (matchDoc.exists()) matchesCol.document(matchId).delete().await()
        } catch (e: Exception) {
            Log.w("FirestoreBlockService", "Cascade cleanup on block failed", e)
        }
    }

    /** Unblock a user. */
    suspend fun unblock(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return
        blockedCol(blockerUid).document(blockedUid).delete().await()
    }

    /** Check if a user is blocked. */
    suspend fun isBlocked(blockerUid: String, blockedUid: String): Boolean {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return false
        return blockedCol(blockerUid).document(blockedUid).get().await().exists()
    }

    /** Observe all blocked UIDs in real-time. */
    fun observeBlockedUids(blockerUid: String): Flow<List<String>> = callbackFlow {
        val reg = blockedCol(blockerUid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val uids = snap?.documents?.mapNotNull { it.getString("blockedUid") } ?: emptyList()
            trySend(uids)
        }
        awaitClose { reg.remove() }
    }

    /** Get all blocked UIDs (one-shot). */
    suspend fun getBlockedUids(blockerUid: String): Set<String> {
        val snap = blockedCol(blockerUid).get().await()
        return snap.documents.mapNotNull { it.getString("blockedUid") }.toSet()
    }
}
