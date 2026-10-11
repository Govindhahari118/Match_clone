package com.match.app.data.local.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserEntityPrivacyDefaultsTest {
    private fun member() = UserEntity(
        firebaseUid = "member-123",
        email = "member@example.test",
        displayName = "Member",
        age = 29,
        gender = "FEMALE",
        lookingFor = "MALE",
        city = "Hyderabad",
        bio = "",
        rasi = "",
        nakshatra = ""
    )

    @Test
    fun newProfilesDoNotAutomaticallyShareHoroscope() {
        assertFalse(member().showHoroscope)
    }

    @Test
    fun explicitHoroscopeSharingRequiresAndPreservesUserChoice() {
        val optedIn = member().copy(showHoroscope = true)
        assertTrue(optedIn.showHoroscope)
        assertFalse(optedIn.copy(showHoroscope = false).showHoroscope)
    }
}
