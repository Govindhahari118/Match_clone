package com.match.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProfileViewService @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val views = db.collection("profileViews")

    /** Records at most one cloud view per viewer/profile/day. */
    suspend fun record(viewerUid: String, viewedUid: String) {
        if (viewerUid.isBlank() || viewedUid.isBlank() || viewerUid == viewedUid) return
        val day = LocalDate.now(ZoneOffset.UTC).toString()
        val id = sha256("$viewerUid|$viewedUid|$day")
        try {
            views.document(id).create(
                mapOf(
                    "viewerUid" to viewerUid,
                    "viewedUid" to viewedUid,
                    "viewedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.ALREADY_EXISTS) throw e
        }
    }

    suspend fun fetchViewerUids(viewedUid: String, limit: Long = 100): List<String> {
        if (viewedUid.isBlank()) return emptyList()
        val snap = views.whereEqualTo("viewedUid", viewedUid).limit(limit).get().await()
        return snap.documents
            .sortedByDescending { it.getLong("viewedAt") ?: 0L }
            .mapNotNull { it.getString("viewerUid") }
            .distinct()
    }

    fun observeViewerUids(viewedUid: String): Flow<List<String>> = callbackFlow {
        if (viewedUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = views.whereEqualTo("viewedUid", viewedUid).limit(100)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val uids = snap?.documents.orEmpty()
                    .sortedByDescending { it.getLong("viewedAt") ?: 0L }
                    .mapNotNull { it.getString("viewerUid") }
                    .distinct()
                trySend(uids)
            }
        awaitClose { registration.remove() }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
