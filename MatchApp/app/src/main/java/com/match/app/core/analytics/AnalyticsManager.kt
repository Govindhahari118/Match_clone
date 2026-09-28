package com.match.app.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy compatibility facade for existing call sites.
 *
 * New feature instrumentation should use MatreeTelemetry. This facade intentionally discards
 * member identifiers, free-form query/reason text, price strings and provider error text so older
 * screens cannot bypass Matree's privacy-safe analytics contract while they are migrated.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    fun logSignIn(method: String) =
        log("login", "method" to safeEnum(method))

    fun logSignUp(method: String) =
        log("sign_up", "method" to safeEnum(method))

    fun logProfileView(@Suppress("UNUSED_PARAMETER") targetUserId: Long) =
        log("profile_view")

    fun logInterestSent(@Suppress("UNUSED_PARAMETER") targetUserId: Long) =
        log("interest_sent")

    fun logMutualMatch(@Suppress("UNUSED_PARAMETER") targetUserId: Long) =
        log("mutual_match")

    fun logFilterApplied(filterName: String) =
        log("filter_applied", "filter" to safeEnum(filterName))

    fun logSearchPerformed(query: String) =
        log("search_performed", "has_query" to query.isNotBlank())

    fun logMessageSent(type: String = "text") =
        log("message_sent", "type" to safeEnum(type))

    fun logVoiceMessageSent() = logMessageSent("voice")

    fun logChatOpened(@Suppress("UNUSED_PARAMETER") peerId: Long) =
        log("chat_opened")

    fun logPricingViewed() = log("pricing_viewed")

    fun logPurchaseStarted(planName: String) =
        log("purchase_started", "plan" to safeEnum(planName))

    fun logPurchaseCompleted(planName: String, @Suppress("UNUSED_PARAMETER") amount: String) =
        log("purchase_completed", "plan" to safeEnum(planName))

    fun logPurchaseFailed(@Suppress("UNUSED_PARAMETER") reason: String) =
        log("purchase_failed")

    fun logScreenView(screenName: String) =
        log(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            FirebaseAnalytics.Param.SCREEN_NAME to safeEnum(screenName)
        )

    fun logDailyRewardClaimed(day: Int) =
        log("daily_reward_claimed", "day" to day.coerceIn(1, 365))

    fun logShortlistToggled(
        @Suppress("UNUSED_PARAMETER") targetUserId: Long,
        added: Boolean
    ) = log("shortlist_toggled", "action" to if (added) "ADDED" else "REMOVED")

    fun logFeatureUsed(featureName: String) =
        log("feature_used", "feature" to safeEnum(featureName))

    fun logVerificationStarted(type: String) =
        log("verification_started", "type" to safeEnum(type))

    fun logProfileReported(reason: String) =
        log("profile_reported", "reason_category" to safeEnum(reason))

    fun logBlockToggled(blocked: Boolean) =
        log("block_toggled", "action" to if (blocked) "BLOCKED" else "UNBLOCKED")

    private fun log(event: String, vararg params: Pair<String, Any>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value.take(MAX_VALUE_LENGTH))
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Boolean -> bundle.putLong(key, if (value) 1 else 0)
            }
        }
        analytics.logEvent(event, bundle)
    }

    private fun safeEnum(value: String): String =
        value.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9_]+"), "_")
            .trim('_')
            .take(MAX_VALUE_LENGTH)
            .ifBlank { "UNKNOWN" }

    private companion object {
        const val MAX_VALUE_LENGTH = 40
    }
}
