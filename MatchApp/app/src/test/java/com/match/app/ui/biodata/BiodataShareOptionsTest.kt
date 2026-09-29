package com.match.app.ui.biodata

import org.junit.Assert.assertFalse
import org.junit.Test

class BiodataShareOptionsTest {

    @Test
    fun `optional personal sections are excluded by default`() {
        val defaults = BiodataShareOptions()

        assertFalse(defaults.includeCommunity)
        assertFalse(defaults.includeIncome)
        assertFalse(defaults.includeAstrology)
        assertFalse(defaults.includeFamily)
        assertFalse(defaults.includeAboutMe)
    }
}
