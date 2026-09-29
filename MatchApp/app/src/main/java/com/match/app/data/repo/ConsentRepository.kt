package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client bridge to the server-owned consent ledger.
 *
 * Notice versions come from the backend so an old app cannot silently record consent against a
 * stale notice.
 */
@Singleton
class ConsentRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

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
        val result = functions.getHttpsCallable("getConsentState").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid consent-state response")
        val purposes = data["purposes"] as? List<*> ?: error("Missing consent purposes")
        val row = purposes
            .mapNotNull { it as? Map<*, *> }
            .firstOrNull { it["purpose"] == purpose }
            ?: error("Unsupported consent purpose")
        return row["noticeVersion"] as? String ?: error("Missing consent notice version")
    }
}
