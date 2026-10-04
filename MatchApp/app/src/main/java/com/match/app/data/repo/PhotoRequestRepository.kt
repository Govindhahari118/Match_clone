package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class PhotoRequestStatus(
    val status: String = "NONE",
    val requestedAtMillis: Long = 0L,
    val retryAfterMillis: Long = 0L
) {
    val pending: Boolean get() = status == "PENDING"
    val photoAvailable: Boolean get() = status == "PHOTO_AVAILABLE"
}

@Singleton
class PhotoRequestRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun status(targetUid: String): Result<PhotoRequestStatus> = call(
        "getPhotoRequestStatus",
        targetUid
    )

    suspend fun request(targetUid: String): Result<PhotoRequestStatus> = call(
        "requestProfilePhoto",
        targetUid
    )

    private suspend fun call(name: String, targetUid: String): Result<PhotoRequestStatus> = runCatching {
        require(targetUid.isNotBlank()) { "Target profile is required" }
        val response = functions.getHttpsCallable(name)
            .call(mapOf("targetUid" to targetUid))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: error("Invalid photo request response")
        PhotoRequestStatus(
            status = (data["status"] as? String)?.uppercase() ?: "NONE",
            requestedAtMillis = (data["requestedAtMillis"] as? Number)?.toLong() ?: 0L,
            retryAfterMillis = (data["retryAfterMillis"] as? Number)?.toLong() ?: 0L
        )
    }
}
