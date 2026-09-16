package com.match.app.ui.theme

import com.match.app.domain.model.ReligionCategory

/**
 * Resolves presentation from appearance preference + the signed-in user's canonical profile.
 * Discovery lenses, partner preferences and search filters are intentionally absent from this API.
 */
object AppearanceThemeResolver {
    fun resolve(
        automaticReligionTheme: Boolean,
        manualPaletteKey: String,
        profileReligion: String?
    ): AppPalette {
        if (!automaticReligionTheme || profileReligion.isNullOrBlank()) {
            return AppPalette.fromKey(manualPaletteKey)
        }

        val religion = ReligionCategory.fromReligion(profileReligion)
        return if (religion == ReligionCategory.OTHER) {
            AppPalette.fromKey(manualPaletteKey)
        } else {
            AppPalette.forReligion(religion)
        }
    }
}
