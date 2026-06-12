package com.match.app.core.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AstrologyTest {

    @Test fun `same rasi and nakshatra gives high compatibility`() {
        val s = Astrology.score("Cancer", "Pushya", "Cancer", "Pushya")
        assertTrue("same signs should be very compatible, got $s", s > 0.85f)
    }

    @Test fun `fire + water with clashing ganas scores low`() {
        val s = Astrology.score("Aries", "Magha", "Cancer", "Ashwini") // Rakshasa vs Deva
        assertTrue("expected low, got $s", s < 0.5f)
    }

    @Test fun `score always in 0 to 1`() {
        for (r1 in Astrology.RASIS) for (r2 in Astrology.RASIS) {
            for (n1 in Astrology.NAKSHATRAS.take(5)) for (n2 in Astrology.NAKSHATRAS.take(5)) {
                val s = Astrology.score(r1, n1, r2, n2)
                assertTrue("out of range: $s", s in 0f..1f)
            }
        }
    }

    @Test fun `combined weighs questionnaire and astrology`() {
        assertEquals(0.6f, CombinedMatcher.combine(1f, 0f, 0.6f, 0.4f), 1e-5f)
        assertEquals(0.4f, CombinedMatcher.combine(0f, 1f, 0.6f, 0.4f), 1e-5f)
        assertEquals(0.5f, CombinedMatcher.combine(0.5f, 0.5f), 1e-5f)
    }
}
