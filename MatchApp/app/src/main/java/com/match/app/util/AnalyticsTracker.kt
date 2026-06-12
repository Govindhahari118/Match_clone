package com.match.app.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized Firebase Analytics event logging for the 7-stage funnel
 * and all business-critical events.
 */
@Singleton
class AnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics
) {
    // ── Registration funnel ───────────────────────────────────────────────
    fun logRegistrationStart() = log("registration_start")

    fun logRegistrationComplete(method: String) = log("registration_complete") {
        putString("method", method) // "phone", "google", "email"
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
    fun logSubscriptionPurchased(planType: String, amount: Int) = log("subscription_purchased") {
        putString("plan_type", planType)
        putInt("amount_inr", amount)
    }

    fun logBoostPurchased() = log("boost_purchased")

    // ── Discovery / engagement ────────────────────────────────────────────
    fun logProfileViewed(viewedUid: String) = log("profile_viewed") {
        putString("viewed_uid", viewedUid)
    }

    fun logSearchPerformed(filterCount: Int) = log("search_performed") {
        putInt("filter_count", filterCount)
    }

    fun logDailyMatchOpened() = log("daily_match_opened")

    fun logKundaliViewed() = log("kundali_viewed")

    // ── Verification ──────────────────────────────────────────────────────
    fun logVerificationStarted(type: String) = log("verification_started") {
        putString("verification_type", type)
    }

    fun logVerificationCompleted(type: String) = log("verification_completed") {
        putString("verification_type", type)
    }

    // ── Internal helper ───────────────────────────────────────────────────
    private fun log(event: String, extras: (Bundle.() -> Unit)? = null) {
        val bundle = Bundle().apply { extras?.invoke(this) }
        analytics.logEvent(event, bundle)
    }
}
