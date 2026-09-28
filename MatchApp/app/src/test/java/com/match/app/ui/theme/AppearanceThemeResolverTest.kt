package com.match.app.ui.theme

import com.match.app.domain.model.DisplayMode
import com.match.app.domain.model.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceThemeResolverTest {

    @Test
    fun `system display mode follows device without changing theme family`() {
        assertTrue(AppearanceThemeResolver.resolveDarkMode(DisplayMode.SYSTEM, systemDark = true))
        assertFalse(AppearanceThemeResolver.resolveDarkMode(DisplayMode.SYSTEM, systemDark = false))
    }

    @Test
    fun `explicit display modes ignore device dark setting`() {
        assertFalse(AppearanceThemeResolver.resolveDarkMode(DisplayMode.LIGHT, systemDark = true))
        assertTrue(AppearanceThemeResolver.resolveDarkMode(DisplayMode.DARK, systemDark = false))
    }

    @Test
    fun `automatic theme follows canonical profile religion`() {
        assertEquals(AppPalette.HINDU, AppearanceThemeResolver.resolve(ThemePreference.AUTOMATIC, "VIVAH", "Hindu"))
        assertEquals(AppPalette.MUSLIM, AppearanceThemeResolver.resolve(ThemePreference.AUTOMATIC, "VIVAH", "Islam"))
        assertEquals(AppPalette.CHRISTIAN, AppearanceThemeResolver.resolve(ThemePreference.AUTOMATIC, "VIVAH", "Christian"))
    }

    @Test
    fun `neutral theme ignores profile religion`() {
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(ThemePreference.NEUTRAL, "OCEAN", "Hindu"))
    }

    @Test
    fun `manual theme ignores profile religion`() {
        assertEquals(AppPalette.SIKH, AppearanceThemeResolver.resolve(ThemePreference.MANUAL, "SIKH", "Hindu"))
    }

    @Test
    fun `legacy unsupported manual palette falls back to Matree Signature`() {
        assertEquals(
            AppPalette.VIVAH,
            AppearanceThemeResolver.resolve(ThemePreference.MANUAL, "OCEAN", "Hindu")
        )
        assertEquals(
            AppPalette.VIVAH,
            AppearanceThemeResolver.resolve(ThemePreference.MANUAL, "TELUGU", "Muslim")
        )
    }

    @Test
    fun `automatic theme never invents a religion for missing or other profile value`() {
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(ThemePreference.AUTOMATIC, "MUSLIM", null))
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(ThemePreference.AUTOMATIC, "MUSLIM", ""))
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(ThemePreference.AUTOMATIC, "MUSLIM", "Prefer not to say"))
    }
    @Test
    fun `unspecified appearance defaults to Matree Signature instead of religion automatic`() {
        assertEquals(ThemePreference.NEUTRAL, com.match.app.domain.model.AppearancePreference().themePreference)
        assertEquals(ThemePreference.NEUTRAL, ThemePreference.fromStorage(null))
        assertEquals(ThemePreference.NEUTRAL, ThemePreference.fromStorage("unsupported"))
        assertEquals(ThemePreference.NEUTRAL, ThemePreference.fromStorage("AUTOMATIC"))
        assertEquals(ThemePreference.AUTOMATIC, ThemePreference.fromStorage("PROFILE_RELIGION"))
        assertEquals("PROFILE_RELIGION", ThemePreference.AUTOMATIC.storageKey)
    }

}
