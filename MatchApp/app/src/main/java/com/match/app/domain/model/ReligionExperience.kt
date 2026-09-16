package com.match.app.domain.model

/**
 * Stable matrimonial-profile religion identifiers. UI labels are presentation only;
 * storage and business rules use [storageKey].
 */
enum class ReligionId(val storageKey: String, val label: String) {
    HINDU("HINDU", "Hindu"),
    MUSLIM("MUSLIM", "Muslim"),
    CHRISTIAN("CHRISTIAN", "Christian"),
    SIKH("SIKH", "Sikh"),
    BUDDHIST("BUDDHIST", "Buddhist"),
    JAIN("JAIN", "Jain"),
    PARSI_ZOROASTRIAN("PARSI_ZOROASTRIAN", "Parsi / Zoroastrian"),
    OTHER("OTHER", "Other"),
    PREFER_NOT_TO_SAY("PREFER_NOT_TO_SAY", "Prefer not to say");

    companion object {
        fun fromStorageKey(value: String?): ReligionId? =
            entries.firstOrNull { it.storageKey.equals(value?.trim(), ignoreCase = true) }

        /**
         * Compatibility parser for historical display-string data. New writes should use storageKey.
         * Unknown non-blank values intentionally map to OTHER instead of inferring identity.
         */
        fun fromProfileValue(value: String?): ReligionId? {
            val raw = value?.trim().orEmpty()
            if (raw.isBlank()) return null
            fromStorageKey(raw)?.let { return it }
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) }?.let { return it }

            val normalized = raw.lowercase()
            return when {
                normalized.contains("hindu") -> HINDU
                normalized.contains("muslim") || normalized.contains("islam") -> MUSLIM
                normalized.contains("christ") || normalized.contains("catholic") || normalized.contains("orthodox") -> CHRISTIAN
                normalized.contains("sikh") -> SIKH
                normalized.contains("buddh") -> BUDDHIST
                normalized.contains("jain") -> JAIN
                normalized.contains("parsi") || normalized.contains("zoroastr") -> PARSI_ZOROASTRIAN
                normalized.contains("prefer not") -> PREFER_NOT_TO_SAY
                else -> OTHER
            }
        }
    }
}

/**
 * Discovery lenses are kept separate from the member's canonical profile identity.
 * They currently mirror supported profile religions so legacy callers remain source-compatible.
 */
enum class ReligionCategory(val storageKey: String, val label: String, val religionId: ReligionId) {
    HINDU("HINDU", "Hindu", ReligionId.HINDU),
    MUSLIM("MUSLIM", "Muslim", ReligionId.MUSLIM),
    CHRISTIAN("CHRISTIAN", "Christian", ReligionId.CHRISTIAN),
    SIKH("SIKH", "Sikh", ReligionId.SIKH),
    BUDDHIST("BUDDHIST", "Buddhist", ReligionId.BUDDHIST),
    JAIN("JAIN", "Jain", ReligionId.JAIN),
    PARSI_ZOROASTRIAN("PARSI_ZOROASTRIAN", "Parsi / Zoroastrian", ReligionId.PARSI_ZOROASTRIAN),
    OTHER("OTHER", "Other", ReligionId.OTHER),
    PREFER_NOT_TO_SAY("PREFER_NOT_TO_SAY", "Prefer not to say", ReligionId.PREFER_NOT_TO_SAY);

    companion object {
        fun fromReligion(value: String?): ReligionCategory {
            val id = ReligionId.fromProfileValue(value) ?: ReligionId.OTHER
            return entries.first { it.religionId == id }
        }

        fun fromStorageKey(value: String?): ReligionCategory? =
            entries.firstOrNull { it.storageKey.equals(value?.trim(), ignoreCase = true) }
    }
}

/**
 * Private discovery/appearance preference. It never changes the user's declared religion.
 * [locked] applies only to discovery lenses, not to the canonical profile religion lock.
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
