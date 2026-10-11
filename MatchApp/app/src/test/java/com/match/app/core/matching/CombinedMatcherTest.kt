package com.match.app.core.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedMatcherTest {
    @Test fun defaultWeightsCalculateExpectedScore() {
        assertEquals(0.6f, CombinedMatcher.combine(1f, 0f), 0.0001f)
    }

    @Test fun zeroWeightConfigurationIsNeutral() {
        assertEquals(0.5f, CombinedMatcher.combine(1f, 0f, 0f, 0f), 0f)
    }

    @Test fun missingOrInvalidScoresAreNeutral() {
        assertEquals(0.5f, CombinedMatcher.combine(Float.NaN, Float.NaN), 0f)
        assertEquals(0.5f, CombinedMatcher.combine(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY), 0f)
    }

    @Test fun boundedScoresStayInRange() {
        assertEquals(1f, CombinedMatcher.combine(9f, 2f), 0f)
        assertEquals(0f, CombinedMatcher.combine(-4f, -2f), 0f)
        assertEquals(0.5f, CombinedMatcher.combine(0.5f, 0.5f, Float.MAX_VALUE, Float.MAX_VALUE), 0.0001f)
    }

    @Test fun zeroWeightDimensionHasNoEffect() {
        assertEquals(0.2f, CombinedMatcher.combine(0.2f, 0.9f, 1f, 0f), 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeWeightIsRejected() {
        CombinedMatcher.combine(0.2f, 0.8f, -1f, 1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonfiniteWeightIsRejected() {
        CombinedMatcher.combine(0.2f, 0.8f, Float.NaN, 1f)
    }

    @Test fun symmetryUnderWeightSwap() {
        val a = CombinedMatcher.combine(0.2f, 0.9f, 0.7f, 0.3f)
        val b = CombinedMatcher.combine(0.9f, 0.2f, 0.3f, 0.7f)
        assertTrue(a.isFinite())
        assertEquals(a, b, 0.0001f)
    }
}
