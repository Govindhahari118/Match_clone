package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class PhotoRequestStatus(
    val canRequest: Boolean,
    val reason: String,
    val nextAllowedAtMillis: Long
)

data class PhotoRequestResult(
    val requested: Boolean,
    val coolingDown: Boolean,
    val nextAllowedAtMillis: Long
)

@Singleton
class PhotoRequestRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun status(targetUid: String): Result<PhotoRequestStatus> = runCatching {
        require(targetUid.isNotBlank()) { "Target profile is required" }
        val response = functions.getHttpsCallable("getProfilePhotoRequestStatus")
            .call(mapOf("targetUid" to targetUid))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: error("Invalid photo request status")
        PhotoRequestStatus(
            canRequest = data["canRequest"] as? Boolean ?: false,
            reason = data["reason"] as? String ?: "",
            nextAllowedAtMillis = (data["nextAllowedAtMillis"] as? Number)?.toLong() ?: 0L
        )
    }

    suspend fun request(targetUid: String): Result<PhotoRequestResult> = runCatching {
        require(targetUid.isNotBlank()) { "Target profile is required" }
        val response = functions.getHttpsCallable("requestProfilePhoto")
            .call(mapOf("targetUid" to targetUid))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: error("Invalid photo request response")
        PhotoRequestResult(
            requested = data["requested"] as? Boolean ?: false,
            coolingDown = data["coolingDown"] as? Boolean ?: false,
            nextAllowedAtMillis = (data["nextAllowedAtMillis"] as? Number)?.toLong() ?: 0L
        )
    }
}
