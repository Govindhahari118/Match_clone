package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class ConsentState(
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
 * Notice versions come from the backend so an old app cannot silently record consent against a
 * stale notice. The Privacy Dashboard also reads this projection instead of maintaining a second
 * device-local source of truth.
 */
@Singleton
class ConsentRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun getState(): List<ConsentState> {
        val result = functions.getHttpsCallable("getConsentState").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid consent-state response")
        val purposes = data["purposes"] as? List<*> ?: error("Missing consent purposes")
        return purposes.mapNotNull { raw ->
            val row = raw as? Map<*, *> ?: return@mapNotNull null
            val purpose = row["purpose"] as? String ?: return@mapNotNull null
            val noticeVersion = row["noticeVersion"] as? String ?: return@mapNotNull null
            ConsentState(
                purpose = purpose,
                granted = row["granted"] == true,
                noticeVersion = noticeVersion,
                recordedNoticeVersion = row["recordedNoticeVersion"] as? String
            )
        }
    }

    suspend fun set(purpose: String, granted: Boolean) {
        val version = currentNoticeVersion(purpose)
        val result = functions.getHttpsCallable("recordConsent")
            .call(
                mapOf(
                    "purpose" to purpose,
                    "granted" to granted,
                    "noticeVersion" to version,
                    "locale" to "en-IN"
                )
            )
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid consent response")
        check(data["success"] == true) { "Consent decision was not recorded" }
    }

    private suspend fun currentNoticeVersion(purpose: String): String {
        return getState()
            .firstOrNull { it.purpose == purpose }
            ?.noticeVersion
            ?: error("Unsupported consent purpose")
    }
}
