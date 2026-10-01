package com.match.app.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy funnel facade retained for compatibility.
 *
 * It intentionally strips member identifiers, exact monetary values and arbitrary free-form
 * strings. New instrumentation should use MatreeTelemetry's typed privacy-safe event contract.
 */
@Singleton
class AnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    // ── Registration funnel ───────────────────────────────────────────────
    fun logRegistrationStart() = log("registration_start")

    fun logRegistrationComplete(method: String) = log("registration_complete") {
        putString("method", safeEnum(method))
    }

    // ── Profile funnel ────────────────────────────────────────────────────
    fun logProfilePhotoUploaded() = log("profile_photo_uploaded")

    fun logProfileComplete50() = log("profile_complete_50")

    fun logProfileComplete100() = log("profile_complete_100")

    // ── Interest funnel ───────────────────────────────────────────────────
    fun logInterestSent(isSuperInterest: Boolean = false) = log("interest_sent") {
        putBoolean("is_super", isSuperInterest)
    }

    fun logMatchAccepted() = log("match_accepted")

    // ── Communication funnel ──────────────────────────────────────────────
    fun logChatInitiated() = log("chat_initiated")

    fun logVoiceCallStarted() = log("voice_call_started")

    // ── Subscription/monetization ─────────────────────────────────────────
    fun logSubscriptionPurchased(
        planType: String,
        @Suppress("UNUSED_PARAMETER") amount: Int
    ) = log("subscription_purchased") {
        putString("plan_type", safeEnum(planType))
    }

    fun logBoostPurchased() = log("boost_purchased")

    // ── Discovery / engagement ────────────────────────────────────────────
    fun logProfileViewed(@Suppress("UNUSED_PARAMETER") viewedUid: String) =
        log("profile_viewed")

    fun logSearchPerformed(filterCount: Int) = log("search_performed") {
        putInt("filter_count", filterCount)
    }

    fun logDailyMatchOpened() = log("daily_match_opened")

    fun logKundaliViewed() = log("kundali_viewed")

    // ── Verification ──────────────────────────────────────────────────────
    fun logVerificationStarted(type: String) = log("verification_started") {
        putString("verification_type", safeEnum(type))
    }

    fun logVerificationCompleted(type: String) = log("verification_completed") {
        putString("verification_type", safeEnum(type))
    }

    // ── Internal helper ───────────────────────────────────────────────────
    private fun safeEnum(value: String): String =
        value.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9_]+"), "_")
            .trim('_')
            .take(40)
            .ifBlank { "UNKNOWN" }

    private fun log(event: String, extras: (Bundle.() -> Unit)? = null) {
        val bundle = Bundle().apply { extras?.invoke(this) }
        analytics.logEvent(event, bundle)
    }
}
