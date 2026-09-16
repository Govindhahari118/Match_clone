package com.match.app.domain.model

/** Appearance is presentation-only and intentionally independent from profile/discovery data. */
enum class ThemePreference {
    AUTOMATIC,
    NEUTRAL,
    MANUAL;

    companion object {
        fun fromStorage(value: String?): ThemePreference =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AUTOMATIC
    }
}

enum class DisplayMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStorage(value: String?): DisplayMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

data class AppearancePreference(
    val themePreference: ThemePreference = ThemePreference.AUTOMATIC,
    val manualThemeKey: String = "VIVAH",
    val displayMode: DisplayMode = DisplayMode.SYSTEM
)
