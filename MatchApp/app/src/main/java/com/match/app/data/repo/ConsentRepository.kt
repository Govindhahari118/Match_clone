package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class ConsentPurposeState(
    val purpose: String,
    val granted: Boolean,
    val noticeVersion: String,
    val recordedNoticeVersion: String?
) {
    val isCurrent: Boolean
        get() = granted && recordedNoticeVersion == noticeVersion
}

/**
 * Client bridge to the server-owned consent ledger.
 *
 * The backend is the notice-version authority. Android can display and change the current
 * projection, but cannot invent notice versions or write the audit ledger directly.
 */
@Singleton
class ConsentRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun list(): List<ConsentPurposeState> {
        val result = functions.getHttpsCallable("getConsentState").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid consent-state response")
        val purposes = data["purposes"] as? List<*> ?: error("Missing consent purposes")
        return purposes.mapNotNull { raw ->
            val row = raw as? Map<*, *> ?: return@mapNotNull null
            val purpose = row["purpose"] as? String ?: return@mapNotNull null
            val noticeVersion = row["noticeVersion"] as? String ?: return@mapNotNull null
            ConsentPurposeState(
                purpose = purpose,
                granted = row["granted"] as? Boolean ?: false,
                noticeVersion = noticeVersion,
                recordedNoticeVersion = row["recordedNoticeVersion"] as? String
            )
        }
    }

    suspend fun set(purpose: String, granted: Boolean) {
        val state = list().firstOrNull { it.purpose == purpose }
            ?: error("Unsupported consent purpose")
        val result = functions.getHttpsCallable("recordConsent")
            .call(
                mapOf(
                    "purpose" to purpose,
                    "granted" to granted,
                    "noticeVersion" to state.noticeVersion,
                    "locale" to "en-IN"
                )
            )
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid consent response")
        check(data["success"] == true) { "Consent decision was not recorded" }
    }
}
