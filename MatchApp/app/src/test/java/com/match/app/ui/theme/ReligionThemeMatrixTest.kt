package com.match.app.ui.theme

import com.match.app.domain.model.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReligionThemeMatrixTest {

    @Test
    fun `automatic theme maps every supported religion family without changing neutral fallback`() {
        val cases = mapOf(
            "Hindu" to AppPalette.HINDU,
            "Islam" to AppPalette.MUSLIM,
            "Muslim" to AppPalette.MUSLIM,
            "Christian" to AppPalette.CHRISTIAN,
            "Sikh" to AppPalette.SIKH,
            "Buddhist" to AppPalette.BUDDHIST,
            "Jain" to AppPalette.JAIN,
            "Parsi" to AppPalette.PARSI,
            "Zoroastrian" to AppPalette.PARSI
        )

        cases.forEach { (religion, expected) ->
            assertEquals(
                expected,
                AppearanceThemeResolver.resolve(
                    themePreference = ThemePreference.AUTOMATIC,
                    manualPaletteKey = "HINDU",
                    profileReligion = religion
                )
            )
        }

        listOf(null, "", "Other", "Prefer not to say", "Unknown value").forEach { religion ->
            assertEquals(
                AppPalette.VIVAH,
                AppearanceThemeResolver.resolve(
                    themePreference = ThemePreference.AUTOMATIC,
                    manualPaletteKey = "MUSLIM",
                    profileReligion = religion
                )
            )
        }
    }

    @Test
    fun `manual and neutral appearance never depend on profile religion`() {
        val religions = listOf("Hindu", "Muslim", "Christian", "Sikh", "Buddhist", "Jain", "Parsi")
        val manualPalettes = listOf(
            AppPalette.HINDU,
            AppPalette.MUSLIM,
            AppPalette.CHRISTIAN,
            AppPalette.SIKH,
            AppPalette.BUDDHIST,
            AppPalette.JAIN,
            AppPalette.PARSI
        )

        religions.forEach { religion ->
            assertEquals(
                AppPalette.VIVAH,
                AppearanceThemeResolver.resolve(ThemePreference.NEUTRAL, "HINDU", religion)
            )
            manualPalettes.forEach { palette ->
                assertEquals(
                    palette,
                    AppearanceThemeResolver.resolve(ThemePreference.MANUAL, palette.name, religion)
                )
            }
        }
    }

    @Test
    fun `danger semantics are stable across religion palettes in each display mode`() {
        val palettes = listOf(
            AppPalette.VIVAH,
            AppPalette.HINDU,
            AppPalette.MUSLIM,
            AppPalette.CHRISTIAN,
            AppPalette.SIKH,
            AppPalette.BUDDHIST,
            AppPalette.JAIN,
            AppPalette.PARSI
        )

        listOf(false, true).forEach { dark ->
            val reference = colorSchemeFor(AppPalette.VIVAH, dark)
            palettes.forEach { palette ->
                val scheme = colorSchemeFor(palette, dark)
                assertEquals(reference.error, scheme.error)
                assertEquals(reference.onError, scheme.onError)
                assertEquals(reference.errorContainer, scheme.errorContainer)
                assertEquals(reference.onErrorContainer, scheme.onErrorContainer)
            }
        }
    }

    @Test
    fun `religion palettes remain visually distinct from neutral identity`() {
        val religionPalettes = listOf(
            AppPalette.HINDU,
            AppPalette.MUSLIM,
            AppPalette.CHRISTIAN,
            AppPalette.SIKH,
            AppPalette.BUDDHIST,
            AppPalette.JAIN,
            AppPalette.PARSI
        )
        religionPalettes.forEach { palette ->
            assertNotEquals(AppPalette.VIVAH.swatch, palette.swatch)
        }
    }
}
