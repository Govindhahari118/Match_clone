package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

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
