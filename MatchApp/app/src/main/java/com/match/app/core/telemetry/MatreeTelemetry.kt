package com.match.app.core.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy-safe product telemetry boundary.
 *
 * Callers choose from explicit events and coarse parameters only. This class deliberately has no
 * generic logEvent(name, Bundle) API, which prevents feature code from sending message bodies,
 * phone/email, KYC values, exact location, purchase tokens, birth details or arbitrary profile data.
 */
@Singleton
class MatreeTelemetry @Inject constructor(
    @ApplicationContext context: Context
) {
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun profileCreated() = event(Event.PROFILE_CREATED)
    fun profileCompleted() = event(Event.PROFILE_COMPLETED)
    fun verificationStarted() = event(Event.VERIFICATION_STARTED)
    fun verificationCompleted(level: Int) =
        event(Event.VERIFICATION_COMPLETED, "level" to level.coerceIn(0, 10))

    fun searchUsed(activeFilterCount: Int, resultCount: Int? = null) =
        event(
            Event.SEARCH_USED,
            "active_filter_count" to activeFilterCount.coerceAtLeast(0),
            "result_count" to resultCount?.coerceAtLeast(0)
        )

    fun profileViewed(source: String) =
        event(Event.PROFILE_VIEWED, "source" to safeEnum(source))

    fun interestSent(mutual: Boolean) =
        event(Event.INTEREST_SENT, "mutual" to mutual)

    fun interestAccepted() = event(Event.INTEREST_ACCEPTED)
    fun contactRevealed(channel: String) =
        event(Event.CONTACT_REVEALED, "channel" to safeEnum(channel))

    fun messageSent(type: String) =
        event(Event.MESSAGE_SENT, "message_type" to safeEnum(type))

    fun profileHidden() = event(Event.PROFILE_HIDDEN)
    fun memberBlocked() = event(Event.MEMBER_BLOCKED)
    fun reportSubmitted(reasonCategory: String) =
        event(Event.REPORT_SUBMITTED, "reason_category" to safeEnum(reasonCategory))

    fun subscriptionVerification(result: String, entitlementType: String) =
        event(
            Event.SUBSCRIPTION_VERIFICATION,
            "result" to safeEnum(result),
            "entitlement_type" to safeEnum(entitlementType)
        )

    fun supportSubmitted(category: String) =
        event(Event.SUPPORT_SUBMITTED, "category" to safeEnum(category))

    fun themeChanged(mode: String, palette: String) =
        event(
            Event.THEME_CHANGED,
            "mode" to safeEnum(mode),
            "palette" to safeEnum(palette)
        )

    fun nearbyChanged(enabled: Boolean) =
        event(Event.NEARBY_CHANGED, "enabled" to enabled)

    /**
     * Records operational failure class only. Throwable messages/stacks from arbitrary provider
     * exceptions can contain URLs, identifiers or payload fragments, so they are not forwarded.
     */
    fun recordFailure(operation: Operation, throwable: Throwable) {
        crashlytics.setCustomKey("matree_operation", operation.key)
        crashlytics.setCustomKey(
            "matree_failure_class",
            throwable::class.java.simpleName.take(80)
        )
        crashlytics.recordException(
            MatreeSanitizedException(operation.key, throwable::class.java.simpleName.take(80))
        )
    }

    private fun event(event: Event, vararg values: Pair<String, Any?>) {
        val params = Bundle()
        values.forEach { (key, value) ->
            if (key !in event.allowedParameters || value == null) return@forEach
            when (value) {
                is String -> params.putString(key, value.take(MAX_VALUE_LENGTH))
                is Int -> params.putLong(key, value.toLong())
                is Long -> params.putLong(key, value)
                is Boolean -> params.putLong(key, if (value) 1 else 0)
            }
        }
        analytics.logEvent(event.key, params)
    }

    private fun safeEnum(value: String): String =
        value.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9_]+"), "_")
            .trim('_')
            .take(MAX_VALUE_LENGTH)
            .ifBlank { "UNKNOWN" }

    internal enum class Event(
        val key: String,
        val allowedParameters: Set<String> = emptySet()
    ) {
        PROFILE_CREATED("profile_created"),
        PROFILE_COMPLETED("profile_completed"),
        VERIFICATION_STARTED("verification_started"),
        VERIFICATION_COMPLETED("verification_completed", setOf("level")),
        SEARCH_USED("search_used", setOf("active_filter_count", "result_count")),
        PROFILE_VIEWED("profile_viewed", setOf("source")),
        INTEREST_SENT("interest_sent", setOf("mutual")),
        INTEREST_ACCEPTED("interest_accepted"),
        CONTACT_REVEALED("contact_revealed", setOf("channel")),
        MESSAGE_SENT("message_sent", setOf("message_type")),
        PROFILE_HIDDEN("profile_hidden"),
        MEMBER_BLOCKED("member_blocked"),
        REPORT_SUBMITTED("report_submitted", setOf("reason_category")),
        SUBSCRIPTION_VERIFICATION(
            "subscription_verification",
            setOf("result", "entitlement_type")
        ),
        SUPPORT_SUBMITTED("support_submitted", setOf("category")),
        THEME_CHANGED("theme_changed", setOf("mode", "palette")),
        NEARBY_CHANGED("nearby_changed", setOf("enabled")),
    }

    enum class Operation(val key: String) {
        AUTH("auth"),
        PROFILE_SYNC("profile_sync"),
        MEDIA_UPLOAD("media_upload"),
        CHAT_OUTBOX("chat_outbox"),
        BILLING_VERIFY("billing_verify"),
        ACCOUNT_DELETE("account_delete"),
        NEARBY("nearby"),
        FUNCTIONS("functions"),
    }

    private class MatreeSanitizedException(operation: String, failureClass: String) :
        RuntimeException("Matree operation failed: $operation ($failureClass)")

    private companion object {
        const val MAX_VALUE_LENGTH = 40
    }
}
