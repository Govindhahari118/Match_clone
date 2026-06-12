package com.match.app.domain.subscription

/**
 * Subscription plan definitions and feature gating.
 *
 * Plans:
 *  - FREE: Basic browsing, 5 interests/day, no contact reveal
 *  - STANDARD (₹399/month): 15 interests/day, see who viewed, basic filters
 *  - PREMIUM (₹799/month): Unlimited interests, contact reveal, Kundali match, boost
 *  - PLATINUM (₹1499/month): All Premium + RM assistance, priority support, video calls
 */
object SubscriptionPlans {

    enum class Plan(
        val id: String,
        val displayName: String,
        val priceMonthly: Int,   // INR
        val priceAnnual: Int,    // INR (20% off)
        val interestsPerDay: Int,
        val canSeeWhoViewed: Boolean,
        val canRevealContact: Boolean,
        val canUseKundali: Boolean,
        val canBoost: Boolean,
        val canVideoCall: Boolean,
        val hasRMAssistance: Boolean,
        val prioritySupport: Boolean,
        val advancedFilters: Boolean,
        val readReceipts: Boolean,
        val stealthBrowse: Boolean,
    ) {
        FREE(
            id = "FREE",
            displayName = "Free",
            priceMonthly = 0,
            priceAnnual = 0,
            interestsPerDay = 5,
            canSeeWhoViewed = false,
            canRevealContact = false,
            canUseKundali = false,
            canBoost = false,
            canVideoCall = false,
            hasRMAssistance = false,
            prioritySupport = false,
            advancedFilters = false,
            readReceipts = false,
            stealthBrowse = false
        ),
        STANDARD(
            id = "STANDARD",
            displayName = "Standard",
            priceMonthly = 399,
            priceAnnual = 3830, // 399*12 * 0.8
            interestsPerDay = 15,
            canSeeWhoViewed = true,
            canRevealContact = false,
            canUseKundali = false,
            canBoost = false,
            canVideoCall = false,
            hasRMAssistance = false,
            prioritySupport = false,
            advancedFilters = true,
            readReceipts = true,
            stealthBrowse = false
        ),
        PREMIUM(
            id = "PREMIUM",
            displayName = "Premium",
            priceMonthly = 799,
            priceAnnual = 7670, // 799*12 * 0.8
            interestsPerDay = Int.MAX_VALUE,
            canSeeWhoViewed = true,
            canRevealContact = true,
            canUseKundali = true,
            canBoost = true,
            canVideoCall = false,
            hasRMAssistance = false,
            prioritySupport = false,
            advancedFilters = true,
            readReceipts = true,
            stealthBrowse = true
        ),
        PLATINUM(
            id = "PLATINUM",
            displayName = "Platinum",
            priceMonthly = 1499,
            priceAnnual = 14390, // 1499*12 * 0.8
            interestsPerDay = Int.MAX_VALUE,
            canSeeWhoViewed = true,
            canRevealContact = true,
            canUseKundali = true,
            canBoost = true,
            canVideoCall = true,
            hasRMAssistance = true,
            prioritySupport = true,
            advancedFilters = true,
            readReceipts = true,
            stealthBrowse = true
        );

        companion object {
            fun fromId(id: String): Plan = entries.find { it.id == id } ?: FREE
        }
    }

    /** Check if a feature is available for a given plan. */
    fun canAccess(userPlan: String, feature: Feature): Boolean {
        val plan = Plan.fromId(userPlan)
        return when (feature) {
            Feature.SEND_INTEREST -> true // All plans, but rate-limited
            Feature.SEE_WHO_VIEWED -> plan.canSeeWhoViewed
            Feature.REVEAL_CONTACT -> plan.canRevealContact
            Feature.KUNDALI_MATCH -> plan.canUseKundali
            Feature.PROFILE_BOOST -> plan.canBoost
            Feature.VIDEO_CALL -> plan.canVideoCall
            Feature.RM_ASSISTANCE -> plan.hasRMAssistance
            Feature.PRIORITY_SUPPORT -> plan.prioritySupport
            Feature.ADVANCED_FILTERS -> plan.advancedFilters
            Feature.READ_RECEIPTS -> plan.readReceipts
            Feature.STEALTH_BROWSE -> plan.stealthBrowse
        }
    }

    /** Get daily interest limit for a plan. */
    fun dailyInterestLimit(userPlan: String): Int {
        return Plan.fromId(userPlan).interestsPerDay
    }

    enum class Feature {
        SEND_INTEREST,
        SEE_WHO_VIEWED,
        REVEAL_CONTACT,
        KUNDALI_MATCH,
        PROFILE_BOOST,
        VIDEO_CALL,
        RM_ASSISTANCE,
        PRIORITY_SUPPORT,
        ADVANCED_FILTERS,
        READ_RECEIPTS,
        STEALTH_BROWSE
    }
}
