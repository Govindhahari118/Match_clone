package com.match.app.ui.i18n

import java.util.Locale

/**
 * Single source of truth for UI locales that have real production translation assets.
 *
 * Do not add a code here until its catalog exists and translation/layout QA has been completed.
 * Mother tongue/profile language catalogs are separate and may contain many more languages.
 */
object SupportedUiLocales {
    val codes: Set<String> = linkedSetOf("en", "te", "hi")

    fun normalize(value: String?, systemLanguage: String = Locale.getDefault().language): String {
        val requested = value?.trim()?.lowercase().orEmpty()
        if (requested in codes) return requested

        val system = systemLanguage.trim().lowercase()
        return system.takeIf { it in codes } ?: "en"
    }
}
