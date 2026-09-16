package com.match.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceThemeResolverTest {

    @Test
    fun `automatic theme follows canonical profile religion`() {
        assertEquals(
            AppPalette.HINDU,
            AppearanceThemeResolver.resolve(
                automaticReligionTheme = true,
                manualPaletteKey = "VIVAH",
                profileReligion = "Hindu"
            )
        )
        assertEquals(
            AppPalette.MUSLIM,
            AppearanceThemeResolver.resolve(true, "VIVAH", "Islam")
        )
        assertEquals(
            AppPalette.CHRISTIAN,
            AppearanceThemeResolver.resolve(true, "VIVAH", "Christian")
        )
    }

    @Test
    fun `manual palette wins when automatic religion theme is disabled`() {
        assertEquals(
            AppPalette.OCEAN,
            AppearanceThemeResolver.resolve(
                automaticReligionTheme = false,
                manualPaletteKey = "OCEAN",
                profileReligion = "Hindu"
            )
        )
    }

    @Test
    fun `unknown or absent profile religion does not invent a religious theme`() {
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(true, "VIVAH", null))
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(true, "VIVAH", ""))
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(true, "VIVAH", "Prefer not to say"))
    }
}
