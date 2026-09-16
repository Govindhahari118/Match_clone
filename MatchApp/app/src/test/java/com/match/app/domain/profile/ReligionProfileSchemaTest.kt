package com.match.app.domain.profile

import com.match.app.domain.model.ReligionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReligionProfileSchemaTest {
    @Test fun `all supported religions have a schema`() {
        ReligionId.entries.forEach { religion ->
            assertEquals(religion, ReligionFieldRegistry.schemaFor(religion).religion)
        }
    }

    @Test fun `hindu astrology fields are not leaked to unrelated schemas`() {
        assertTrue(ReligionFieldRegistry.supports("Hindu", ReligionFieldKey.NAKSHATRA))
        assertTrue(ReligionFieldRegistry.supports("Hindu", ReligionFieldKey.RASHI))
        assertTrue(ReligionFieldRegistry.supports("Hindu", ReligionFieldKey.MANGLIK))

        assertFalse(ReligionFieldRegistry.supports("Muslim", ReligionFieldKey.NAKSHATRA))
        assertFalse(ReligionFieldRegistry.supports("Christian", ReligionFieldKey.MANGLIK))
        assertFalse(ReligionFieldRegistry.supports("Sikh", ReligionFieldKey.RASHI))
    }

    @Test fun `religion parser supports canonical and historical values without inference`() {
        assertEquals(ReligionId.HINDU, ReligionId.fromProfileValue("Hindu"))
        assertEquals(ReligionId.MUSLIM, ReligionId.fromProfileValue("Islam"))
        assertEquals(ReligionId.CHRISTIAN, ReligionId.fromProfileValue("Catholic"))
        assertEquals(ReligionId.PARSI_ZOROASTRIAN, ReligionId.fromProfileValue("Zoroastrian"))
        assertEquals(ReligionId.PREFER_NOT_TO_SAY, ReligionId.fromProfileValue("Prefer not to say"))
        assertEquals(ReligionId.OTHER, ReligionId.fromProfileValue("self described value"))
        assertNull(ReligionId.fromProfileValue(""))
    }

    @Test fun `prefer not to say exposes no religion-specific fields`() {
        assertTrue(ReligionFieldRegistry.schemaFor(ReligionId.PREFER_NOT_TO_SAY).fields.isEmpty())
    }
}
