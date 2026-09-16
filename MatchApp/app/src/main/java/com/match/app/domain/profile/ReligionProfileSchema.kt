package com.match.app.domain.profile

import com.match.app.domain.model.ReligionCategory

/**
 * One shared profile model with religion-aware labels/options. Values remain optional and
 * self-declared; the app never derives them from name, language, caste, location or family data.
 */
data class ReligionFieldDefinition(
    val key: String,
    val label: String,
    val helper: String,
    val options: List<String> = emptyList()
)

data class ReligionProfileSchema(
    val category: ReligionCategory,
    val sectionTitle: String,
    val primary: ReligionFieldDefinition?,
    val secondary: ReligionFieldDefinition?,
    val institution: ReligionFieldDefinition?
)

object ReligionProfileSchemas {
    fun forReligion(religion: String): ReligionProfileSchema = when (ReligionCategory.fromReligion(religion)) {
        ReligionCategory.HINDU -> ReligionProfileSchema(
            ReligionCategory.HINDU,
            "Hindu details",
            ReligionFieldDefinition(
                key = "faithTradition",
                label = "Tradition / sampradaya (optional)",
                helper = "Optional self-description; community, Gothra and Kundali details are collected separately.",
                options = listOf("Vaishnava", "Shaiva", "Shakta", "Smarta", "Lingayat / Veerashaiva", "Other")
            ),
            null,
            null
        )
        ReligionCategory.MUSLIM -> ReligionProfileSchema(
            ReligionCategory.MUSLIM,
            "Muslim details",
            ReligionFieldDefinition(
                key = "faithTradition",
                label = "Sect / tradition (optional)",
                helper = "Choose only if you want this shown on your matrimonial profile.",
                options = listOf("Sunni", "Shia", "Ibadi", "Other", "Prefer not to specify")
            ),
            ReligionFieldDefinition(
                key = "faithSubTradition",
                label = "School / madhhab (optional)",
                helper = "Optional and applicable only where meaningful to you.",
                options = listOf("Hanafi", "Shafi'i", "Maliki", "Hanbali", "Ja'fari", "Other", "Prefer not to specify")
            ),
            null
        )
        ReligionCategory.CHRISTIAN -> ReligionProfileSchema(
            ReligionCategory.CHRISTIAN,
            "Christian details",
            ReligionFieldDefinition(
                key = "faithTradition",
                label = "Denomination (optional)",
                helper = "Self-declared denomination; choose Other if your denomination is not listed.",
                options = listOf("Catholic", "Orthodox", "Protestant", "Pentecostal", "Evangelical", "Syrian Christian", "Other", "Prefer not to specify")
            ),
            null,
            ReligionFieldDefinition(
                key = "faithInstitution",
                label = "Church / parish (optional)",
                helper = "Add only if you are comfortable sharing it."
            )
        )
        ReligionCategory.SIKH -> ReligionProfileSchema(
            ReligionCategory.SIKH,
            "Sikh details",
            ReligionFieldDefinition(
                key = "faithTradition",
                label = "Tradition / self-description (optional)",
                helper = "Optional self-description; this does not affect account rights or verification.",
                options = listOf("Sikh", "Amritdhari", "Keshdhari", "Sehajdhari", "Other", "Prefer not to specify")
            ),
            null,
            ReligionFieldDefinition(
                key = "faithInstitution",
                label = "Gurdwara association (optional)",
                helper = "Add only if useful to your matrimonial profile."
            )
        )
        ReligionCategory.BUDDHIST -> ReligionProfileSchema(
            ReligionCategory.BUDDHIST,
            "Buddhist details",
            ReligionFieldDefinition(
                key = "faithTradition",
                label = "Tradition (optional)",
                helper = "Optional self-declared Buddhist tradition.",
                options = listOf("Theravada", "Mahayana", "Vajrayana", "Navayana", "Zen", "Other", "Prefer not to specify")
            ),
            null,
            null
        )
        ReligionCategory.JAIN -> ReligionProfileSchema(
            ReligionCategory.JAIN,
            "Jain details",
            ReligionFieldDefinition(
                key = "faithTradition",
                label = "Sect / tradition (optional)",
                helper = "Optional self-declared Jain tradition.",
                options = listOf("Digambar", "Shwetambar", "Sthanakvasi", "Terapanthi", "Other", "Prefer not to specify")
            ),
            null,
            null
        )
        ReligionCategory.PARSI -> ReligionProfileSchema(
            ReligionCategory.PARSI,
            "Parsi / Zoroastrian details",
            ReligionFieldDefinition(
                key = "faithTradition",
                label = "Community / tradition (optional)",
                helper = "Optional self-description; use Other for traditions not listed.",
                options = listOf("Parsi", "Irani Zoroastrian", "Other", "Prefer not to specify")
            ),
            null,
            ReligionFieldDefinition(
                key = "faithInstitution",
                label = "Anjuman / community association (optional)",
                helper = "Add only if you are comfortable sharing it."
            )
        )
        ReligionCategory.OTHER -> ReligionProfileSchema(
            ReligionCategory.OTHER,
            "Faith / belief details",
            ReligionFieldDefinition(
                key = "faithTradition",
                label = "Tradition / belief (optional)",
                helper = "Use your own wording if you want to add more context."
            ),
            null,
            null
        )
    }
}
