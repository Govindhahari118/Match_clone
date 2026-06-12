package com.match.app.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central analytics facade wrapping Firebase Analytics.
 * All user-action tracking goes through this singleton so events
 * stay consistent and can be swapped to another provider later.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val fa: FirebaseAnalytics
) {

    // ── Auth ─────────────────────────────────────────────────────────────

    fun logSignIn(method: String) = log("login", bundle("method" to method))

    fun logSignUp(method: String) = log("sign_up", bundle("method" to method))

    // ── Discovery & Matching ─────────────────────────────────────────────

    fun logProfileView(targetUserId: Long) =
        log("profile_view", bundle("target_user_id" to targetUserId.toString()))

    fun logInterestSent(targetUserId: Long) =
        log("interest_sent", bundle("target_user_id" to targetUserId.toString()))

    fun logMutualMatch(targetUserId: Long) =
        log("mutual_match", bundle("target_user_id" to targetUserId.toString()))

    fun logFilterApplied(filterName: String) =
        log("filter_applied", bundle("filter" to filterName))

    fun logSearchPerformed(query: String) =
        log("search_performed", bundle("query" to query.take(100)))

    // ── Chat ─────────────────────────────────────────────────────────────

    fun logMessageSent(type: String = "text") =
        log("message_sent", bundle("type" to type))

    fun logVoiceMessageSent() = logMessageSent("voice")

    fun logChatOpened(peerId: Long) =
        log("chat_opened", bundle("peer_id" to peerId.toString()))

    // ── Premium & Payments ───────────────────────────────────────────────

    fun logPricingViewed() = log("pricing_viewed")

    fun logPurchaseStarted(planName: String) =
        log("purchase_started", bundle("plan" to planName))

    fun logPurchaseCompleted(planName: String, amount: String) =
        log("purchase_completed", bundle("plan" to planName, "amount" to amount))

    fun logPurchaseFailed(reason: String) =
        log("purchase_failed", bundle("reason" to reason.take(100)))

    // ── Engagement ───────────────────────────────────────────────────────

    fun logScreenView(screenName: String) =
        log(FirebaseAnalytics.Event.SCREEN_VIEW, bundle(
            FirebaseAnalytics.Param.SCREEN_NAME to screenName
        ))

    fun logDailyRewardClaimed(day: Int) =
        log("daily_reward_claimed", bundle("day" to day.toString()))

    fun logShortlistToggled(targetUserId: Long, added: Boolean) =
        log("shortlist_toggled", bundle(
            "target_user_id" to targetUserId.toString(),
            "action" to if (added) "added" else "removed"
        ))

    fun logFeatureUsed(featureName: String) =
        log("feature_used", bundle("feature" to featureName))

    // ── Verification & Safety ────────────────────────────────────────────

    fun logVerificationStarted(type: String) =
        log("verification_started", bundle("type" to type))

    fun logProfileReported(reason: String) =
        log("profile_reported", bundle("reason" to reason.take(100)))

    fun logBlockToggled(blocked: Boolean) =
        log("block_toggled", bundle("action" to if (blocked) "blocked" else "unblocked"))

    // ── Internal helpers ─────────────────────────────────────────────────

    private fun log(event: String, params: Bundle? = null) {
        fa.logEvent(event, params)
    }

    private fun bundle(vararg pairs: Pair<String, String>): Bundle = Bundle().apply {
        pairs.forEach { (k, v) -> putString(k, v) }
    }
}
