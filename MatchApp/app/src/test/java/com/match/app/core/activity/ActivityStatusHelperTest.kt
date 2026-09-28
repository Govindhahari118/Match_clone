package com.match.app.core.activity

import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import com.match.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityStatusHelperTest {
    private val now = 2_000_000_000_000L

    @Test fun `less than five minutes is online`() {
        val status = ActivityStatusHelper.from(now - 2 * 60_000L, now)
        assertTrue(status.isOnline)
        assertEquals("Online now", status.label)
    }

    @Test fun `two days is shown precisely`() {
        val status = ActivityStatusHelper.from(now - 2 * 86_400_000L, now)
        assertFalse(status.isOnline)
        assertEquals("Active 2 days ago", status.label)
    }

    @Test fun `two weeks is shown precisely`() {
        val status = ActivityStatusHelper.from(now - 15 * 86_400_000L, now)
        assertEquals("Active 2 weeks ago", status.label)
    }

    @Test fun `very old activity becomes inactive`() {
        val status = ActivityStatusHelper.from(now - 120 * 86_400_000L, now)
        assertEquals("Inactive", status.label)
        assertFalse(status.isRecent)
    }

    @Test fun `missing activity is never presented as recent`() {
        val status = ActivityStatusHelper.from(0L, now)
        assertEquals("Activity unavailable", status.label)
        assertFalse(status.isOnline)
        assertFalse(status.isRecent)
    }

    @Test fun `profile completeness displays server aggregate instead of legacy weights`() {
        val profile = UserProfile(
            id = 1,
            email = "owner@example.test",
            displayName = "Owner",
            age = 30,
            gender = Gender.FEMALE,
            lookingFor = LookingFor.MALE,
            city = "Hyderabad",
            bio = "",
            rasi = "",
            nakshatra = "",
            hasQuestionnaire = false,
            profileCompleteness = 0.65f
        )

        assertEquals(65, ActivityStatusHelper.profileCompleteness(profile))
    }
}
