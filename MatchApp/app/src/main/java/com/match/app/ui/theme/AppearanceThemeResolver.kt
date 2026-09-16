package com.match.app.ui.theme

import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.ThemePreference

/**
 * Resolves presentation from explicit appearance preference + the signed-in user's canonical
 * profile. Discovery lenses, partner preferences and search filters are intentionally absent.
 */
object AppearanceThemeResolver {
    fun resolve(
        themePreference: ThemePreference,
        manualPaletteKey: String,
        profileReligion: String?
    ): AppPalette = when (themePreference) {
        ThemePreference.NEUTRAL -> AppPalette.VIVAH
        ThemePreference.MANUAL -> AppPalette.fromKey(manualPaletteKey)
        ThemePreference.AUTOMATIC -> {
            if (profileReligion.isNullOrBlank()) {
                AppPalette.VIVAH
            } else {
                val religion = ReligionCategory.fromReligion(profileReligion)
                if (religion == ReligionCategory.OTHER) AppPalette.VIVAH else AppPalette.forReligion(religion)
            }
        }
    }
}
