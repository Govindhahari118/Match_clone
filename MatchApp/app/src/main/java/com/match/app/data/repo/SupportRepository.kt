package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class SupportTicketSummary(
    val id: String,
    val category: String,
    val message: String,
    val status: String,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
    val resolvedAtMillis: Long?
)

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

    suspend fun submitChatMessageReport(
        targetUid: String,
        messageId: String,
        reasonCode: String,
        details: String = ""
    ): Result<Boolean> = runCatching {
        require(targetUid.isNotBlank())
        require(messageId.matches(Regex("[A-Za-z0-9_-]{16,128}")))
        require(reasonCode.isNotBlank())
        val response = functions.getHttpsCallable("submitChatMessageReport")
            .call(
                mapOf(
                    "targetUid" to targetUid,
                    "messageId" to messageId,
                    "reason" to reasonCode,
                    "details" to details.take(1000)
                )
            )
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: error("Invalid message report response")
        data["success"] as? Boolean ?: false
    }

    suspend fun listMySupportTickets(): Result<List<SupportTicketSummary>> = runCatching {
        val response = functions.getHttpsCallable("listMySupportTickets")
            .call()
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val tickets = data["tickets"] as? List<Map<String, Any?>> ?: emptyList()
        tickets.mapNotNull { value ->
            val id = value["id"] as? String ?: return@mapNotNull null
            SupportTicketSummary(
                id = id,
                category = value["category"] as? String ?: "",
                message = value["message"] as? String ?: "",
                status = value["status"] as? String ?: "OPEN",
                createdAtMillis = (value["createdAtMillis"] as? Number)?.toLong(),
                updatedAtMillis = (value["updatedAtMillis"] as? Number)?.toLong(),
                resolvedAtMillis = (value["resolvedAtMillis"] as? Number)?.toLong()
            )
        }
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
