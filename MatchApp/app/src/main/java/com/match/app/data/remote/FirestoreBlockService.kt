package com.match.app.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore service for blocked users.
 *
 * Collection: `blocks/{blockerUid}/blocked/{blockedUid}`.
 * Creating the protected block record is the only client mutation. Trusted Cloud Functions
 * cascade relationship cleanup because clients are intentionally forbidden from deleting
 * authoritative interests/matches themselves.
 */
@Singleton
class FirestoreBlockService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    private fun blockedCol(blockerUid: String) =
        db.collection("blocks").document(blockerUid).collection("blocked")

    suspend fun block(blockerUid: String, blockedUid: String) {
        require(blockerUid.isNotBlank() && blockedUid.isNotBlank()) { "Missing account identity" }
        require(blockerUid != blockedUid) { "You cannot block yourself" }
        blockedCol(blockerUid).document(blockedUid).set(
            mapOf(
                "blockedUid" to blockedUid,
                "blockedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun unblock(blockerUid: String, blockedUid: String) {
        require(blockerUid.isNotBlank() && blockedUid.isNotBlank()) { "Missing account identity" }
        blockedCol(blockerUid).document(blockedUid).delete().await()
    }

    suspend fun isBlocked(blockerUid: String, blockedUid: String): Boolean {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return false
        return blockedCol(blockerUid).document(blockedUid).get().await().exists()
    }

    fun observeBlockedUids(blockerUid: String): Flow<List<String>> = callbackFlow {
        if (blockerUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = blockedCol(blockerUid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val uids = snap?.documents?.mapNotNull { it.getString("blockedUid") } ?: emptyList()
            trySend(uids)
        }
        awaitClose { reg.remove() }
    }

    suspend fun getBlockedUids(blockerUid: String): Set<String> {
        if (blockerUid.isBlank()) return emptySet()
        val snap = blockedCol(blockerUid).get().await()
        return snap.documents.mapNotNull { it.getString("blockedUid") }.toSet()
    }
}
