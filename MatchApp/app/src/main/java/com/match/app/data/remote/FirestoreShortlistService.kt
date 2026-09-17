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
 * Firestore service for shortlisted (saved) profiles.
 *
 * Collection: `shortlists/{ownerUid}/saved/{targetUid}`
 */
@Singleton
class FirestoreShortlistService @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    private fun savedCol(ownerUid: String) =
        db.collection("shortlists").document(ownerUid).collection("saved")

    /** Save a profile to shortlist. */
    suspend fun save(ownerUid: String, targetUid: String) {
        requireValidPair(ownerUid, targetUid)
        savedCol(ownerUid).document(targetUid).set(
            mapOf(
                "targetUid" to targetUid,
                "savedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Remove a profile from shortlist. */
    suspend fun remove(ownerUid: String, targetUid: String) {
        requireValidPair(ownerUid, targetUid)
        savedCol(ownerUid).document(targetUid).delete().await()
    }

    /** Check if a profile is shortlisted. */
    suspend fun isSaved(ownerUid: String, targetUid: String): Boolean {
        requireValidPair(ownerUid, targetUid)
        return savedCol(ownerUid).document(targetUid).get().await().exists()
    }

    /**
     * Atomically toggles shortlist state and returns the committed state.
     *
     * The previous read-then-write implementation could lose a toggle when two signed-in devices
     * acted on the same profile at nearly the same time. Firestore transactions retry when the
     * document changes, so each completed caller observes and mutates one serializable state.
     */
    suspend fun toggle(ownerUid: String, targetUid: String): Boolean {
        requireValidPair(ownerUid, targetUid)
        val ref = savedCol(ownerUid).document(targetUid)
        return db.runTransaction { transaction ->
            val current = transaction.get(ref)
            if (current.exists()) {
                transaction.delete(ref)
                false
            } else {
                transaction.set(
                    ref,
                    mapOf(
                        "targetUid" to targetUid,
                        "savedAt" to FieldValue.serverTimestamp()
                    )
                )
                true
            }
        }.await()
    }

    /** Observe all shortlisted UIDs for a user in real-time. */
    fun observeSavedUids(ownerUid: String): Flow<List<String>> = callbackFlow {
        require(ownerUid.isNotBlank()) { "ownerUid is required" }
        val reg = savedCol(ownerUid)
            .orderBy("savedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val uids = snap?.documents?.mapNotNull { it.getString("targetUid") } ?: emptyList()
                trySend(uids)
            }
        awaitClose { reg.remove() }
    }

    /** Get all shortlisted UIDs (one-shot). */
    suspend fun getSavedUids(ownerUid: String): List<String> {
        require(ownerUid.isNotBlank()) { "ownerUid is required" }
        val snap = savedCol(ownerUid).get().await()
        return snap.documents.mapNotNull { it.getString("targetUid") }
    }

    private fun requireValidPair(ownerUid: String, targetUid: String) {
        require(ownerUid.isNotBlank()) { "ownerUid is required" }
        require(targetUid.isNotBlank()) { "targetUid is required" }
        require(ownerUid != targetUid) { "A profile cannot shortlist itself" }
    }
}
