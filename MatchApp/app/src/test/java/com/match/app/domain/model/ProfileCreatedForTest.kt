package com.match.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileCreatedForTest {
    @Test
    fun canonicalValuesRoundTripFromWire() {
        ProfileCreatedFor.entries.forEach { value ->
            assertEquals(value, ProfileCreatedFor.fromWire(value.name))
        }
    }

    @Test
    fun unknownAndLegacyValuesFailClosedToSelfManaged() {
        assertEquals(ProfileCreatedFor.SELF, ProfileCreatedFor.fromWire(""))
        assertEquals(ProfileCreatedFor.SELF, ProfileCreatedFor.fromWire("AGENT"))
        assertEquals(ProfileCreatedFor.SELF, ProfileCreatedFor.fromWire("unknown"))
    }
}
