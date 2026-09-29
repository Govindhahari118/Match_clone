package com.match.app.core.matching

import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import com.match.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchScorerTest {

    private fun profile(
        id: Long,
        religion: String,
        rasi: String = "",
        nakshatra: String = "",
        verification: Int = 0,
        education: String = "B.Tech",
        occupation: String = "Engineer",
        diet: String = "Vegetarian",
        self: FloatArray? = null,
        partner: FloatArray? = null
    ) = UserProfile(
        id = id,
        email = "u$id@example.test",
        displayName = "User $id",
        age = 30,
        gender = if (id % 2L == 0L) Gender.MALE else Gender.FEMALE,
        lookingFor = if (id % 2L == 0L) LookingFor.FEMALE else LookingFor.MALE,
        city = "Hyderabad",
        bio = "",
        rasi = rasi,
        nakshatra = nakshatra,
        hasQuestionnaire = self != null && partner != null,
        religion = religion,
        education = education,
        occupationCategory = occupation,
        diet = diet,
        verificationLevel = verification,
        selfVector = self,
        partnerVector = partner
    )

    @Test
    fun `combined score is symmetric for the same pair`() {
        val a = profile(1, "Christian", verification = 1)
        val b = profile(2, "Christian", verification = 5)

        assertEquals(
            MatchScorer.calculate(a, b),
            MatchScorer.calculate(b, a)
        )
    }

    @Test
    fun `non Hindu pairs do not receive an astrology factor`() {
        val a = profile(1, "Christian", rasi = "Aries", nakshatra = "Ashwini")
        val b = profile(2, "Christian", rasi = "Aries", nakshatra = "Ashwini")

        val result = MatchScorer.explain(a, b)

        assertFalse(result.factors.any { it.key == "astrology" })
        assertEquals(MatchScorer.FORMULA_VERSION, result.formulaVersion)
    }

    @Test
    fun `Hindu astrology applies only when both profiles have required inputs`() {
        val completeA = profile(1, "Hindu", rasi = "Aries", nakshatra = "Ashwini")
        val completeB = profile(2, "Hindu", rasi = "Taurus", nakshatra = "Rohini")
        val missing = profile(2, "Hindu", rasi = "", nakshatra = "")

        assertTrue(
            MatchScorer.explain(completeA, completeB).factors.any { it.key == "astrology" }
        )
        assertFalse(
            MatchScorer.explain(completeA, missing).factors.any { it.key == "astrology" }
        )
    }

    @Test
    fun `bilateral preference fit is explainable and changes compatibility`() {
        val a = profile(1, "Christian", verification = 3)
        val b = profile(2, "Christian", verification = 3)

        val low = MatchScorer.explain(a, b, bilateralPreferenceFit = 0f)
        val high = MatchScorer.explain(a, b, bilateralPreferenceFit = 1f)

        assertTrue(high.percentage > low.percentage)
        assertTrue(high.factors.any {
            it.key == "bilateral_preferences" && it.score == 1f
        })
    }

    @Test
    fun `trusted astrology override is used without exposing local horoscope fields`() {
        val a = profile(1, "Hindu", rasi = "", nakshatra = "")
        val b = profile(2, "Hindu", rasi = "", nakshatra = "")

        val result = MatchScorer.explain(
            a,
            b,
            astrologyScoreOverride = 0.82f
        )

        val astrology = result.factors.single { it.key == "astrology" }
        assertEquals(0.82f, astrology.score)
    }

    @Test
    fun `unknown demographics are omitted instead of receiving a neutral bonus`() {
        val a = profile(1, "Other", education = "", occupation = "", diet = "")
        val b = profile(2, "Other", education = "", occupation = "", diet = "")

        val result = MatchScorer.explain(a, b)

        assertFalse(result.factors.any { it.key == "demographics_lifestyle" })
    }
}
