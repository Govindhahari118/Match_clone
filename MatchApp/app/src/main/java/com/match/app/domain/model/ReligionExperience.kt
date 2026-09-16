package com.match.app.domain.model

/**
 * Top-level discovery lenses. These are intentionally broader than the
 * profile's exact religion value so the home experience stays simple while
 * detailed religion/community/sub-community data remains available to filters.
 */
enum class ReligionCategory(val storageKey: String, val label: String) {
    HINDU("HINDU", "Hindu"),
    CHRISTIAN("CHRISTIAN", "Christian"),
    MUSLIM("MUSLIM", "Muslim"),
    SIKH("SIKH", "Sikh"),
    BUDDHIST("BUDDHIST", "Buddhist"),
    JAIN("JAIN", "Jain"),
    PARSI("PARSI", "Parsi / Zoroastrian"),
    OTHER("OTHER", "Other");

    companion object {
        fun fromReligion(value: String?): ReligionCategory {
            val normalized = value.orEmpty().trim().lowercase()
            return when {
                normalized.contains("hindu") -> HINDU
                normalized.contains("christ") || normalized.contains("catholic") || normalized.contains("orthodox") -> CHRISTIAN
                normalized.contains("muslim") || normalized.contains("islam") -> MUSLIM
                normalized.contains("sikh") -> SIKH
                normalized.contains("buddh") -> BUDDHIST
                normalized.contains("jain") -> JAIN
                normalized.contains("parsi") || normalized.contains("zoroastr") -> PARSI
                else -> OTHER
            }
        }

        fun fromStorageKey(value: String?): ReligionCategory? =
            entries.firstOrNull { it.storageKey.equals(value, ignoreCase = true) }
    }
}

/**
 * Private UX preference. It does not change the user's declared religion.
 * [locked] means discovery defaults to [selected] until the user explicitly
 * unlocks it. Users can still choose one or more lenses when unlocked.
 */
data class ReligionExperiencePreference(
    val selected: Set<ReligionCategory> = emptySet(),
    val locked: Boolean = false,
    val religionThemeEnabled: Boolean = false
) {
    val isAll: Boolean get() = selected.size == ReligionCategory.entries.size

    fun effective(defaultReligion: String?): Set<ReligionCategory> = when {
        selected.isNotEmpty() -> selected
        defaultReligion.isNullOrBlank() -> ReligionCategory.entries.toSet()
        else -> setOf(ReligionCategory.fromReligion(defaultReligion))
    }
}
