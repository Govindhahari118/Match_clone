package com.match.app.ui.theme

import com.match.app.domain.model.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceThemeResolverTest {

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
    fun `automatic theme never invents a religion for missing or other profile value`() {
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(ThemePreference.AUTOMATIC, "MUSLIM", null))
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(ThemePreference.AUTOMATIC, "MUSLIM", ""))
        assertEquals(AppPalette.VIVAH, AppearanceThemeResolver.resolve(ThemePreference.AUTOMATIC, "MUSLIM", "Prefer not to say"))
    }
}
