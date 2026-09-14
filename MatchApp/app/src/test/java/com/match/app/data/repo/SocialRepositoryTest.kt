package com.match.app.data.repo

import com.match.app.domain.subscription.SubscriptionPlans
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Production-plan regression coverage.
 *
 * Social actions surface paid capabilities in several screens. Keep the canonical entitlement
 * table under pure JVM tests so UI code cannot silently drift back to obsolete plan names.
 */
class SocialRepositoryTest {

    @Test
    fun `unknown plan is treated as free`() {
        val plan = SubscriptionPlans.Plan.fromId("LEGACY_UNKNOWN")
        assertEquals(SubscriptionPlans.Plan.FREE, plan)
        assertFalse(SubscriptionPlans.canAccess("LEGACY_UNKNOWN", SubscriptionPlans.Feature.SEE_WHO_VIEWED))
        assertFalse(SubscriptionPlans.canAccess("LEGACY_UNKNOWN", SubscriptionPlans.Feature.REVEAL_CONTACT))
    }

    @Test
    fun `silver enables core paid discovery but not premium actions`() {
        val plan = SubscriptionPlans.Plan.SILVER_3M.id
        assertTrue(SubscriptionPlans.canAccess(plan, SubscriptionPlans.Feature.SEE_WHO_VIEWED))
        assertTrue(SubscriptionPlans.canAccess(plan, SubscriptionPlans.Feature.REVEAL_CONTACT))
        assertTrue(SubscriptionPlans.canAccess(plan, SubscriptionPlans.Feature.KUNDALI_MATCH))
        assertFalse(SubscriptionPlans.canAccess(plan, SubscriptionPlans.Feature.PROFILE_BOOST))
        assertFalse(SubscriptionPlans.canAccess(plan, SubscriptionPlans.Feature.VIDEO_CALL))
    }

    @Test
    fun `gold enables boost and stealth but not platinum-only video or RM`() {
        val plan = SubscriptionPlans.Plan.GOLD_6M.id
        assertTrue(SubscriptionPlans.canAccess(plan, SubscriptionPlans.Feature.PROFILE_BOOST))
        assertTrue(SubscriptionPlans.canAccess(plan, SubscriptionPlans.Feature.STEALTH_BROWSE))
        assertFalse(SubscriptionPlans.canAccess(plan, SubscriptionPlans.Feature.VIDEO_CALL))
        assertFalse(SubscriptionPlans.canAccess(plan, SubscriptionPlans.Feature.RM_ASSISTANCE))
    }

    @Test
    fun `platinum enables all catalogued paid capabilities`() {
        val plan = SubscriptionPlans.Plan.PLATINUM_12M.id
        SubscriptionPlans.Feature.entries.forEach { feature ->
            assertTrue("Expected $feature for Platinum", SubscriptionPlans.canAccess(plan, feature))
        }
    }

    @Test
    fun `free send-interest limit remains finite while paid tiers are unlimited`() {
        assertEquals(5, SubscriptionPlans.dailyInterestLimit(SubscriptionPlans.Plan.FREE.id))
        assertEquals(Int.MAX_VALUE, SubscriptionPlans.dailyInterestLimit(SubscriptionPlans.Plan.SILVER_3M.id))
        assertEquals(Int.MAX_VALUE, SubscriptionPlans.dailyInterestLimit(SubscriptionPlans.Plan.GOLD_6M.id))
        assertEquals(Int.MAX_VALUE, SubscriptionPlans.dailyInterestLimit(SubscriptionPlans.Plan.PLATINUM_12M.id))
    }
}
