package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class SecureCallRequest(
    val exists: Boolean = false,
    val pairId: String = "",
    val requesterUid: String = "",
    val targetUid: String = "",
    val kind: String = "VOICE",
    val proposedAtMs: Long = 0L,
    val status: String = "NONE"
)

data class SecureCallCapability(
    val eligible: Boolean = false,
    val reason: String = "unavailable",
    val providerReady: Boolean = false,
    val voiceAvailable: Boolean = false,
    val videoAvailable: Boolean = false,
    val numberMaskingAvailable: Boolean = false
)

@Singleton
class SecureCallRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun callRequest(targetUid: String): SecureCallRequest {
        require(targetUid.isNotBlank()) { "Target profile is required" }
        val result = functions.getHttpsCallable("getSecureCallRequest")
            .call(mapOf("targetUid" to targetUid))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: emptyMap()
        return SecureCallRequest(
            exists = data["exists"] as? Boolean ?: false,
            pairId = data["pairId"] as? String ?: "",
            requesterUid = data["requesterUid"] as? String ?: "",
            targetUid = data["targetUid"] as? String ?: "",
            kind = data["kind"] as? String ?: "VOICE",
            proposedAtMs = (data["proposedAtMs"] as? Number)?.toLong() ?: 0L,
            status = data["status"] as? String ?: "NONE"
        )
    }

    suspend fun requestCall(targetUid: String, proposedAtMs: Long, kind: String = "VOICE") {
        require(targetUid.isNotBlank()) { "Target profile is required" }
        functions.getHttpsCallable("requestSecureCall")
            .call(mapOf("targetUid" to targetUid, "proposedAtMs" to proposedAtMs, "kind" to kind))
            .await()
    }

    suspend fun respondToCallRequest(targetUid: String, accept: Boolean) {
        require(targetUid.isNotBlank()) { "Target profile is required" }
        functions.getHttpsCallable("respondSecureCallRequest")
            .call(mapOf("targetUid" to targetUid, "response" to if (accept) "ACCEPTED" else "DECLINED"))
            .await()
    }

    suspend fun cancelCallRequest(targetUid: String) {
        require(targetUid.isNotBlank()) { "Target profile is required" }
        functions.getHttpsCallable("cancelSecureCallRequest")
            .call(mapOf("targetUid" to targetUid))
            .await()
    }

    suspend fun capability(targetUid: String): SecureCallCapability {
        require(targetUid.isNotBlank()) { "Target profile is required" }
        val result = functions.getHttpsCallable("getSecureCallCapability")
            .call(mapOf("targetUid" to targetUid))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: emptyMap()
        return SecureCallCapability(
            eligible = data["eligible"] as? Boolean ?: false,
            reason = data["reason"] as? String ?: "unavailable",
            providerReady = data["providerReady"] as? Boolean ?: false,
            voiceAvailable = data["voiceAvailable"] as? Boolean ?: false,
            videoAvailable = data["videoAvailable"] as? Boolean ?: false,
            numberMaskingAvailable = data["numberMaskingAvailable"] as? Boolean ?: false
        )
    }
}
