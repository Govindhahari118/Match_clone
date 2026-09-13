package com.match.app.domain.subscription

/** Canonical membership catalogue shared conceptually with the server payment catalogue. */
object SubscriptionPlans {

    enum class Plan(
        val id: String,
        val displayName: String,
        val durationMonths: Int,
        val amountPaise: Int,
        val interestsPerDay: Int,
        val contactLimit: Int,
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
        FREE("FREE", "Free", 0, 0, 5, 0, false, false, false, false, false, false, false, false, false, false),
        SILVER_3M("SILVER_3M", "Silver", 3, 299900, Int.MAX_VALUE, 75, true, true, true, false, false, false, false, true, true, false),
        GOLD_6M("GOLD_6M", "Gold", 6, 499900, Int.MAX_VALUE, 150, true, true, true, true, false, false, true, true, true, true),
        PLATINUM_12M("PLATINUM_12M", "Platinum", 12, 749900, Int.MAX_VALUE, 300, true, true, true, true, true, true, true, true, true, true);

        companion object {
            fun fromId(id: String): Plan = entries.find { it.id == id } ?: FREE
        }
    }

    fun canAccess(userPlan: String, feature: Feature): Boolean {
        val plan = Plan.fromId(userPlan)
        return when (feature) {
            Feature.SEND_INTEREST -> true
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

    fun dailyInterestLimit(userPlan: String): Int = Plan.fromId(userPlan).interestsPerDay
    fun contactRevealLimit(userPlan: String): Int = Plan.fromId(userPlan).contactLimit

    enum class Feature {
        SEND_INTEREST, SEE_WHO_VIEWED, REVEAL_CONTACT, KUNDALI_MATCH, PROFILE_BOOST,
        VIDEO_CALL, RM_ASSISTANCE, PRIORITY_SUPPORT, ADVANCED_FILTERS, READ_RECEIPTS,
        STEALTH_BROWSE
    }
}
