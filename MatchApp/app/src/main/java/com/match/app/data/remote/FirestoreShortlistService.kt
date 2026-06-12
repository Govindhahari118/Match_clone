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
        if (ownerUid.isBlank() || targetUid.isBlank()) return
        savedCol(ownerUid).document(targetUid).set(
            mapOf(
                "targetUid" to targetUid,
                "savedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Remove a profile from shortlist. */
    suspend fun remove(ownerUid: String, targetUid: String) {
        if (ownerUid.isBlank() || targetUid.isBlank()) return
        savedCol(ownerUid).document(targetUid).delete().await()
    }

    /** Check if a profile is shortlisted. */
    suspend fun isSaved(ownerUid: String, targetUid: String): Boolean {
        if (ownerUid.isBlank() || targetUid.isBlank()) return false
        return savedCol(ownerUid).document(targetUid).get().await().exists()
    }

    /** Toggle shortlist status. Returns true if now saved. */
    suspend fun toggle(ownerUid: String, targetUid: String): Boolean {
        val wasSaved = isSaved(ownerUid, targetUid)
        if (wasSaved) remove(ownerUid, targetUid) else save(ownerUid, targetUid)
        return !wasSaved
    }

    /** Observe all shortlisted UIDs for a user in real-time. */
    fun observeSavedUids(ownerUid: String): Flow<List<String>> = callbackFlow {
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
        val snap = savedCol(ownerUid).get().await()
        return snap.documents.mapNotNull { it.getString("targetUid") }
    }
}
