package com.match.app.domain.profile

import com.match.app.domain.model.ReligionId

/**
 * Stable field keys for religion/community-specific profile data.
 * These keys describe applicability only; privacy and persistence are enforced separately.
 */
enum class ReligionFieldKey {
    COMMUNITY,
    SUB_COMMUNITY,
    GOTHRA,
    NAKSHATRA,
    RASHI,
    MANGLIK,
    BIRTH_TIME,
    BIRTH_PLACE,
    KUNDALI_AVAILABLE,
    SECT,
    MADHAB,
    PRACTICE_PREFERENCE,
    MARRIAGE_PREFERENCES,
    DENOMINATION,
    CHURCH,
    FAMILY_TRADITIONS
}

data class ReligionFieldDefinition(
    val key: ReligionFieldKey,
    val label: String,
    val optional: Boolean = true
)

interface ReligionProfileSchema {
    val religion: ReligionId
    val fields: List<ReligionFieldDefinition>

    fun supports(key: ReligionFieldKey): Boolean = fields.any { it.key == key }
}

private data class DefaultReligionProfileSchema(
    override val religion: ReligionId,
    override val fields: List<ReligionFieldDefinition>
) : ReligionProfileSchema

/**
 * Single source of truth for which religion-specific fields are applicable.
 * Shared profile fields (identity, location, education, career, family and lifestyle)
 * deliberately do not live here.
 */
object ReligionFieldRegistry {
    private val commonCommunity = listOf(
        ReligionFieldDefinition(ReligionFieldKey.COMMUNITY, "Community / caste"),
        ReligionFieldDefinition(ReligionFieldKey.SUB_COMMUNITY, "Sub-community / sub-caste")
    )

    private val schemas: Map<ReligionId, ReligionProfileSchema> = mapOf(
        ReligionId.HINDU to DefaultReligionProfileSchema(
            ReligionId.HINDU,
            commonCommunity + listOf(
                ReligionFieldDefinition(ReligionFieldKey.GOTHRA, "Gothra / clan"),
                ReligionFieldDefinition(ReligionFieldKey.NAKSHATRA, "Nakshatra / birth star"),
                ReligionFieldDefinition(ReligionFieldKey.RASHI, "Rashi / moon sign"),
                ReligionFieldDefinition(ReligionFieldKey.MANGLIK, "Manglik status"),
                ReligionFieldDefinition(ReligionFieldKey.BIRTH_TIME, "Birth time"),
                ReligionFieldDefinition(ReligionFieldKey.BIRTH_PLACE, "Birth place"),
                ReligionFieldDefinition(ReligionFieldKey.KUNDALI_AVAILABLE, "Kundali available")
            )
        ),
        ReligionId.MUSLIM to DefaultReligionProfileSchema(
            ReligionId.MUSLIM,
            commonCommunity + listOf(
                ReligionFieldDefinition(ReligionFieldKey.SECT, "Sect"),
                ReligionFieldDefinition(ReligionFieldKey.MADHAB, "Madhab"),
                ReligionFieldDefinition(ReligionFieldKey.PRACTICE_PREFERENCE, "Religious / practice preference"),
                ReligionFieldDefinition(ReligionFieldKey.MARRIAGE_PREFERENCES, "Marriage preferences")
            )
        ),
        ReligionId.CHRISTIAN to DefaultReligionProfileSchema(
            ReligionId.CHRISTIAN,
            commonCommunity + listOf(
                ReligionFieldDefinition(ReligionFieldKey.DENOMINATION, "Denomination"),
                ReligionFieldDefinition(ReligionFieldKey.CHURCH, "Church"),
                ReligionFieldDefinition(ReligionFieldKey.PRACTICE_PREFERENCE, "Religious / practice preference"),
                ReligionFieldDefinition(ReligionFieldKey.MARRIAGE_PREFERENCES, "Marriage preferences")
            )
        ),
        ReligionId.SIKH to DefaultReligionProfileSchema(
            ReligionId.SIKH,
            commonCommunity + listOf(
                ReligionFieldDefinition(ReligionFieldKey.PRACTICE_PREFERENCE, "Religious / practice preference"),
                ReligionFieldDefinition(ReligionFieldKey.FAMILY_TRADITIONS, "Family traditions"),
                ReligionFieldDefinition(ReligionFieldKey.MARRIAGE_PREFERENCES, "Marriage preferences")
            )
        ),
        ReligionId.BUDDHIST to DefaultReligionProfileSchema(ReligionId.BUDDHIST, commonCommunity),
        ReligionId.JAIN to DefaultReligionProfileSchema(ReligionId.JAIN, commonCommunity),
        ReligionId.PARSI_ZOROASTRIAN to DefaultReligionProfileSchema(ReligionId.PARSI_ZOROASTRIAN, commonCommunity),
        ReligionId.OTHER to DefaultReligionProfileSchema(ReligionId.OTHER, commonCommunity),
        ReligionId.PREFER_NOT_TO_SAY to DefaultReligionProfileSchema(ReligionId.PREFER_NOT_TO_SAY, emptyList())
    )

    fun schemaFor(religion: ReligionId): ReligionProfileSchema =
        checkNotNull(schemas[religion]) { "Missing religion schema for ${religion.storageKey}" }

    fun schemaForProfileValue(value: String?): ReligionProfileSchema =
        schemaFor(ReligionId.fromProfileValue(value) ?: ReligionId.OTHER)

    fun supports(value: String?, key: ReligionFieldKey): Boolean = schemaForProfileValue(value).supports(key)
}
