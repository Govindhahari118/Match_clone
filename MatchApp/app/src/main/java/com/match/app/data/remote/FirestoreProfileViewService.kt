package com.match.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreProfileViewService @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val views = db.collection("profileViews")

    /**
     * Records a view through the trusted backend. The server authenticates the viewer,
     * enforces block state, validates the target and de-duplicates viewer/profile/day.
     */
    suspend fun record(viewerUid: String, viewedUid: String) {
        if (viewerUid.isBlank() || viewedUid.isBlank() || viewerUid == viewedUid) return
        functions.getHttpsCallable("recordProfileView")
            .call(mapOf("viewedUid" to viewedUid))
            .await()
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
}
