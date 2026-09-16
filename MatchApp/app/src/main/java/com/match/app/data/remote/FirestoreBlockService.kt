package com.match.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read service for blocked users plus trusted callable mutations.
 *
 * Collection: `blocks/{blockerUid}/blocked/{blockedUid}`
 * Blocking must be server-authoritative because it also removes relationship state atomically.
 */
@Singleton
class FirestoreBlockService @Inject constructor() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    private fun blockedCol(blockerUid: String) =
        db.collection("blocks").document(blockerUid).collection("blocked")

    suspend fun block(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return
        require(auth.currentUser?.uid == blockerUid) { "Block owner does not match signed-in account" }
        functions.getHttpsCallable("blockUser")
            .call(mapOf("targetUid" to blockedUid))
            .await()
    }

    suspend fun unblock(blockerUid: String, blockedUid: String) {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return
        require(auth.currentUser?.uid == blockerUid) { "Block owner does not match signed-in account" }
        functions.getHttpsCallable("unblockUser")
            .call(mapOf("targetUid" to blockedUid))
            .await()
    }

    suspend fun isBlocked(blockerUid: String, blockedUid: String): Boolean {
        if (blockerUid.isBlank() || blockedUid.isBlank()) return false
        return blockedCol(blockerUid).document(blockedUid).get().await().exists()
    }

    fun observeBlockedUids(blockerUid: String): Flow<List<String>> = callbackFlow {
        val reg = blockedCol(blockerUid).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            val uids = snap?.documents?.mapNotNull { it.getString("blockedUid") } ?: emptyList()
            trySend(uids)
        }
        awaitClose { reg.remove() }
    }

    suspend fun getBlockedUids(blockerUid: String): Set<String> {
        val snap = blockedCol(blockerUid).get().await()
        return snap.documents.mapNotNull { it.getString("blockedUid") }.toSet()
    }
}