package com.match.app.core.matching

import com.match.app.data.local.entity.UserEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchScoreEngineTest {

    @Test
    fun `non Hindu profiles do not receive astrology weight`() {
        val me = user(religion = "Muslim")
        val candidate = user(religion = "Muslim", email = "b@example.com")

        val result = MatchScoreEngine.compute(me, candidate)

        assertFalse(result.astrologyApplicable)
        assertEquals(0f, result.astrology, 0.0001f)
        assertTrue(result.coverage in 0f..1f)
        assertTrue("Sparse profile data must be visible as limited coverage", result.coverage < 0.5f)
    }

    @Test
    fun `applicable astrology with missing birth profile data is excluded rather than neutral scored`() {
        val me = user(religion = "Hindu")
        val candidate = user(religion = "Hindu", email = "b@example.com")

        val result = MatchScoreEngine.compute(me, candidate)

        assertTrue(result.astrologyApplicable)
        assertEquals(0f, result.astrology, 0.0001f)
        assertTrue(result.coverage < 0.5f)
    }

    @Test
    fun `height difference does not change compatibility without explicit physical preferences`() {
        val me = user(religion = "Christian", heightCm = 165)
        val sameHeight = user(religion = "Christian", email = "same@example.com", heightCm = 165)
        val differentHeight = user(religion = "Christian", email = "different@example.com", heightCm = 195)

        val first = MatchScoreEngine.compute(me, sameHeight)
        val second = MatchScoreEngine.compute(me, differentHeight)

        assertEquals(0f, first.physical, 0.0001f)
        assertEquals(0f, second.physical, 0.0001f)
        assertEquals(first.total, second.total, 0.0001f)
    }

    @Test
    fun `more comparable profile fields increase coverage`() {
        val sparseMe = user(religion = "Sikh")
        val sparseCandidate = user(religion = "Sikh", email = "sparse@example.com")
        val richMe = user(
            religion = "Sikh",
            motherTongue = "Punjabi",
            education = "Graduate",
            educationField = "Engineering",
            occupationCategory = "Technology",
            city = "Hyderabad",
            state = "Telangana",
            countryOfResidence = "India",
            familyValues = "Moderate",
            familyType = "Nuclear",
            diet = "Vegetarian",
            smoking = "No",
            drinking = "No",
            personalityType = "Balanced partnership"
        )
        val richCandidate = richMe.copy(email = "rich@example.com")

        val sparse = MatchScoreEngine.compute(sparseMe, sparseCandidate)
        val rich = MatchScoreEngine.compute(richMe, richCandidate)

        assertTrue(rich.coverage > sparse.coverage)
        assertTrue(rich.coverage > 0.8f)
    }

    private fun user(
        email: String = "a@example.com",
        religion: String,
        heightCm: Int = 170,
        motherTongue: String = "",
        education: String = "",
        educationField: String = "",
        occupationCategory: String = "",
        city: String = "",
        state: String = "",
        countryOfResidence: String = "",
        familyValues: String = "",
        familyType: String = "",
        diet: String = "",
        smoking: String = "",
        drinking: String = "",
        personalityType: String = ""
    ) = UserEntity(
        email = email,
        passwordHash = "hash",
        displayName = "Test User",
        age = 30,
        gender = "MALE",
        lookingFor = "FEMALE",
        city = city,
        bio = "",
        rasi = "",
        nakshatra = "",
        religion = religion,
        motherTongue = motherTongue,
        education = education,
        educationField = educationField,
        occupationCategory = occupationCategory,
        heightCm = heightCm,
        state = state,
        countryOfResidence = countryOfResidence,
        familyValues = familyValues,
        familyType = familyType,
        diet = diet,
        smoking = smoking,
        drinking = drinking,
        personalityType = personalityType
    )
}
