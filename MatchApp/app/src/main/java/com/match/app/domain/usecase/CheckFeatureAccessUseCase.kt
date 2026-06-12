package com.match.app.domain.usecase

import com.match.app.data.session.SessionStore
import com.match.app.domain.subscription.SubscriptionPlans
import com.match.app.domain.subscription.SubscriptionPlans.Feature
import com.match.app.domain.subscription.SubscriptionPlans.Plan
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for all subscription feature gate checks.
 *
 * Usage in ViewModels:
 *   val allowed = featureAccess.check(Feature.SEE_WHO_VIEWED)
 *   if (!allowed.granted) { _showPaywall.value = allowed.minimumPlan }
 */
@Singleton
class CheckFeatureAccessUseCase @Inject constructor(
    private val session: SessionStore
) {
    data class AccessResult(
        val granted: Boolean,
        val currentPlan: Plan,
        val minimumPlan: Plan? // null if granted
    )

    suspend fun check(feature: Feature): AccessResult {
        val planId = session.subscriptionPlan.first()
        val plan = Plan.fromId(planId)
        val granted = SubscriptionPlans.canAccess(planId, feature)
        val minPlan = if (granted) null else findMinimumPlan(feature)
        return AccessResult(granted = granted, currentPlan = plan, minimumPlan = minPlan)
    }

    fun checkSync(planId: String, feature: Feature): AccessResult {
        val plan = Plan.fromId(planId)
        val granted = SubscriptionPlans.canAccess(planId, feature)
        val minPlan = if (granted) null else findMinimumPlan(feature)
        return AccessResult(granted = granted, currentPlan = plan, minimumPlan = minPlan)
    }

    /** Returns the cheapest plan that grants access to this feature. */
    private fun findMinimumPlan(feature: Feature): Plan {
        return Plan.entries.first { SubscriptionPlans.canAccess(it.id, feature) }
    }

    /** Check daily interest limit (special case — rate-limited, not boolean gate). */
    suspend fun remainingInterestsToday(sentToday: Int): Int {
        val planId = session.subscriptionPlan.first()
        val limit = SubscriptionPlans.dailyInterestLimit(planId)
        return (limit - sentToday).coerceAtLeast(0)
    }
}
