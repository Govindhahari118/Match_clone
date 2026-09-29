package com.match.app.core.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TenPoruthamTest {

    @Test
    fun `canonical inputs produce deterministic bounded results`() {
        for (nakshatra in Astrology.NAKSHATRAS) {
            for (rasi in Astrology.RASIS) {
                val first = TenPorutham.calculate(nakshatra, rasi, nakshatra, rasi)
                val second = TenPorutham.calculate(nakshatra, rasi, nakshatra, rasi)
                assertEquals(first, second)
                assertEquals(10, first.poruthams.size)
                assertTrue(first.score in 0..10)
                assertEquals(10, first.totalPossible)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown nakshatra is rejected instead of mapped to Ashwini`() {
        TenPorutham.calculate("Unknown", "Cancer", "Pushya", "Cancer")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown rasi is rejected instead of mapped to Aries`() {
        TenPorutham.calculate("Pushya", "Unknown", "Pushya", "Cancer")
    }
}
