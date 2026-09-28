package com.match.app.domain.model

/** Appearance is presentation-only and intentionally independent from profile/discovery data. */
enum class ThemePreference(val storageKey: String) {
    AUTOMATIC("PROFILE_RELIGION"),
    NEUTRAL("NEUTRAL"),
    MANUAL("MANUAL");

    companion object {
        /**
         * The old "AUTOMATIC" value was previously also the product default, so it cannot prove
         * explicit consent to a religion-derived visual theme. Under the Neutral-first contract
         * it safely migrates to NEUTRAL. New explicit Automatic selection is persisted as PROFILE_RELIGION.
         */
        fun fromStorage(value: String?): ThemePreference = when (value.orEmpty().trim().uppercase()) {
            "PROFILE_RELIGION" -> AUTOMATIC
            "NEUTRAL" -> NEUTRAL
            "MANUAL" -> MANUAL
            "AUTOMATIC" -> NEUTRAL
            else -> NEUTRAL
        }
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
    val themePreference: ThemePreference = ThemePreference.NEUTRAL,
    val manualThemeKey: String = "VIVAH",
    val displayMode: DisplayMode = DisplayMode.SYSTEM
)
