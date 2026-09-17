package com.match.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundCheckService @Inject constructor() {
    data class Request(
        val id: String,
        val targetProfileId: String,
        val plan: String,
        val status: String,
        val createdAtMillis: Long
    )

    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    suspend fun request(profileId: String, plan: String): String {
        val result = functions.getHttpsCallable("requestBackgroundCheckByProfileId")
            .call(mapOf("profileId" to profileId.trim().uppercase(), "plan" to plan))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid background-check response")
        return data["requestId"] as? String ?: error("Missing request id")
    }

    suspend fun list(uid: String): List<Request> {
        if (uid.isBlank()) return emptyList()
        val snap = db.collection("backgroundChecks")
            .whereEqualTo("requestedBy", uid)
            .limit(50)
            .get()
            .await()
        return snap.documents.map { doc ->
            val data = doc.data.orEmpty()
            Request(
                id = doc.id,
                targetProfileId = data["targetProfileId"] as? String ?: "",
                plan = data["plan"] as? String ?: "",
                status = data["status"] as? String ?: "submitted",
                createdAtMillis = when (val raw = data["createdAt"]) {
                    is Timestamp -> raw.toDate().time
                    is Number -> raw.toLong()
                    else -> 0L
                }
            )
        }.sortedByDescending { it.createdAtMillis }
    }
}
