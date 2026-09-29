package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

enum class PartnerPreferenceMode {
    STRICT,
    PREFERRED,
    NO_PREFERENCE
}

data class PartnerPreferences(
    val configured: Boolean = false,
    val ageMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val ageMin: Int = 18,
    val ageMax: Int = 70,
    val heightMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val heightMinCm: Int = 90,
    val heightMaxCm: Int = 250,
    val religionMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val religions: List<String> = emptyList(),
    val casteMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val castes: List<String> = emptyList(),
    val subCasteMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val subCastes: List<String> = emptyList(),
    val stateMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val states: List<String> = emptyList(),
    val cityMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val cities: List<String> = emptyList(),
    val motherTongueMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val motherTongues: List<String> = emptyList(),
    val maritalStatusMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val maritalStatuses: List<String> = emptyList(),
    val educationMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val educationLevels: List<String> = emptyList(),
    val occupationMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val occupationCategories: List<String> = emptyList(),
    val dietMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val diets: List<String> = emptyList(),
    val smokingMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val smoking: List<String> = emptyList(),
    val drinkingMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val drinking: List<String> = emptyList(),
    val countryOfResidenceMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val countriesOfResidence: List<String> = emptyList(),
    val citizenshipMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val citizenships: List<String> = emptyList(),
    val childrenMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val childrenStatuses: List<String> = emptyList(),
    val nriMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val nriStatuses: List<String> = emptyList(),
    val relocationMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val relocationStatuses: List<String> = emptyList(),
    val familyTypeMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val familyTypes: List<String> = emptyList(),
    val familyValuesMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val familyValues: List<String> = emptyList(),
    val physicalStatusMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val physicalStatuses: List<String> = emptyList(),
    val residentialStatusMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val residentialStatuses: List<String> = emptyList()
)

@Singleton
class PartnerPreferenceRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun load(): PartnerPreferences {
        val result = functions.getHttpsCallable("getPartnerPreferences").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: emptyMap()
        return fromMap(data)
    }

    suspend fun save(value: PartnerPreferences): PartnerPreferences {
        val result = functions.getHttpsCallable("setPartnerPreferences")
            .call(toMap(value))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid partner-preference response")
        return fromMap(data)
    }

    private fun toMap(value: PartnerPreferences): Map<String, Any> = mapOf(
        "ageMode" to value.ageMode.name,
        "ageMin" to value.ageMin,
        "ageMax" to value.ageMax,
        "heightMode" to value.heightMode.name,
        "heightMinCm" to value.heightMinCm,
        "heightMaxCm" to value.heightMaxCm,
        "religionMode" to value.religionMode.name,
        "religions" to value.religions,
        "casteMode" to value.casteMode.name,
        "castes" to value.castes,
        "subCasteMode" to value.subCasteMode.name,
        "subCastes" to value.subCastes,
        "stateMode" to value.stateMode.name,
        "states" to value.states,
        "cityMode" to value.cityMode.name,
        "cities" to value.cities,
        "motherTongueMode" to value.motherTongueMode.name,
        "motherTongues" to value.motherTongues,
        "maritalStatusMode" to value.maritalStatusMode.name,
        "maritalStatuses" to value.maritalStatuses,
        "educationMode" to value.educationMode.name,
        "educationLevels" to value.educationLevels,
        "occupationMode" to value.occupationMode.name,
        "occupationCategories" to value.occupationCategories,
        "dietMode" to value.dietMode.name,
        "diets" to value.diets,
        "smokingMode" to value.smokingMode.name,
        "smoking" to value.smoking,
        "drinkingMode" to value.drinkingMode.name,
        "drinking" to value.drinking,
        "countryOfResidenceMode" to value.countryOfResidenceMode.name,
        "countriesOfResidence" to value.countriesOfResidence,
        "citizenshipMode" to value.citizenshipMode.name,
        "citizenships" to value.citizenships,
        "childrenMode" to value.childrenMode.name,
        "childrenStatuses" to value.childrenStatuses,
        "nriMode" to value.nriMode.name,
        "nriStatuses" to value.nriStatuses,
        "relocationMode" to value.relocationMode.name,
        "relocationStatuses" to value.relocationStatuses,
        "familyTypeMode" to value.familyTypeMode.name,
        "familyTypes" to value.familyTypes,
        "familyValuesMode" to value.familyValuesMode.name,
        "familyValues" to value.familyValues,
        "physicalStatusMode" to value.physicalStatusMode.name,
        "physicalStatuses" to value.physicalStatuses,
        "residentialStatusMode" to value.residentialStatusMode.name,
        "residentialStatuses" to value.residentialStatuses
    )

    private fun fromMap(data: Map<String, Any?>): PartnerPreferences = PartnerPreferences(
        configured = data["configured"] as? Boolean ?: false,
        ageMode = mode(data["ageMode"]),
        ageMin = int(data["ageMin"], 18),
        ageMax = int(data["ageMax"], 70),
        heightMode = mode(data["heightMode"]),
        heightMinCm = int(data["heightMinCm"], 90),
        heightMaxCm = int(data["heightMaxCm"], 250),
        religionMode = mode(data["religionMode"]),
        religions = strings(data["religions"]),
        casteMode = mode(data["casteMode"]),
        castes = strings(data["castes"]),
        subCasteMode = mode(data["subCasteMode"]),
        subCastes = strings(data["subCastes"]),
        stateMode = mode(data["stateMode"]),
        states = strings(data["states"]),
        cityMode = mode(data["cityMode"]),
        cities = strings(data["cities"]),
        motherTongueMode = mode(data["motherTongueMode"]),
        motherTongues = strings(data["motherTongues"]),
        maritalStatusMode = mode(data["maritalStatusMode"]),
        maritalStatuses = strings(data["maritalStatuses"]),
        educationMode = mode(data["educationMode"]),
        educationLevels = strings(data["educationLevels"]),
        occupationMode = mode(data["occupationMode"]),
        occupationCategories = strings(data["occupationCategories"]),
        dietMode = mode(data["dietMode"]),
        diets = strings(data["diets"]),
        smokingMode = mode(data["smokingMode"]),
        smoking = strings(data["smoking"]),
        drinkingMode = mode(data["drinkingMode"]),
        drinking = strings(data["drinking"]),
        countryOfResidenceMode = mode(data["countryOfResidenceMode"]),
        countriesOfResidence = strings(data["countriesOfResidence"]),
        citizenshipMode = mode(data["citizenshipMode"]),
        citizenships = strings(data["citizenships"]),
        childrenMode = mode(data["childrenMode"]),
        childrenStatuses = strings(data["childrenStatuses"]),
        nriMode = mode(data["nriMode"]),
        nriStatuses = strings(data["nriStatuses"]),
        relocationMode = mode(data["relocationMode"]),
        relocationStatuses = strings(data["relocationStatuses"]),
        familyTypeMode = mode(data["familyTypeMode"]),
        familyTypes = strings(data["familyTypes"]),
        familyValuesMode = mode(data["familyValuesMode"]),
        familyValues = strings(data["familyValues"]),
        physicalStatusMode = mode(data["physicalStatusMode"]),
        physicalStatuses = strings(data["physicalStatuses"]),
        residentialStatusMode = mode(data["residentialStatusMode"]),
        residentialStatuses = strings(data["residentialStatuses"])
    )

    private fun mode(value: Any?): PartnerPreferenceMode =
        runCatching { PartnerPreferenceMode.valueOf(value as? String ?: "") }
            .getOrDefault(PartnerPreferenceMode.NO_PREFERENCE)

    private fun int(value: Any?, fallback: Int): Int = (value as? Number)?.toInt() ?: fallback

    private fun strings(value: Any?): List<String> =
        (value as? List<*>)?.filterIsInstance<String>().orEmpty()
}
