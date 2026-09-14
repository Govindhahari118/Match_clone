package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun submitProfileReport(targetUid: String, reason: String, details: String = ""): Result<Boolean> = runCatching {
        require(targetUid.isNotBlank())
        val response = functions.getHttpsCallable("submitProfileReport")
            .call(mapOf("targetUid" to targetUid, "reason" to reason, "details" to details.take(1000)))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: error("Invalid report response")
        data["success"] as? Boolean ?: false
    }

    suspend fun submitSupportTicket(category: String, message: String): Result<String> = runCatching {
        require(message.isNotBlank())
        val response = functions.getHttpsCallable("submitSupportTicket")
            .call(mapOf("category" to category, "message" to message.take(2000)))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: error("Invalid support response")
        if (data["success"] as? Boolean != true) error("Support request was not accepted")
        data["ticketId"] as? String ?: error("Missing ticket id")
    }
}
