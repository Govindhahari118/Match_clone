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

data class PartnerPreferenceSummaryItem(
    val key: String,
    val mode: PartnerPreferenceMode,
    val value: String
)

data class PartnerPreferenceSummary(
    val shared: Boolean = false,
    val items: List<PartnerPreferenceSummaryItem> = emptyList()
)

data class PartnerPreferences(
    val configured: Boolean = false,
    val sharePublicSummary: Boolean = false,
    val ageMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val ageMin: Int = 18,
    val ageMax: Int = 70,
    val heightMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val heightMinCm: Int = 90,
    val heightMaxCm: Int = 250,
    val weightMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val weightMinKg: Int = 30,
    val weightMaxKg: Int = 250,
    val incomeBandMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val incomeBands: List<String> = emptyList(),
    val complexionMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val complexions: List<String> = emptyList(),
    val religionMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val religions: List<String> = emptyList(),
    val casteMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val castes: List<String> = emptyList(),
    val subCasteMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val subCastes: List<String> = emptyList(),
    val gothraMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val gothras: List<String> = emptyList(),
    val faithTraditionMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val faithTraditions: List<String> = emptyList(),
    val faithSubTraditionMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val faithSubTraditions: List<String> = emptyList(),
    val faithInstitutionMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val faithInstitutions: List<String> = emptyList(),
    val stateMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val states: List<String> = emptyList(),
    val nativeStateMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val nativeStates: List<String> = emptyList(),
    val cityMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val cities: List<String> = emptyList(),
    val motherTongueMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val motherTongues: List<String> = emptyList(),
    val maritalStatusMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val maritalStatuses: List<String> = emptyList(),
    val educationMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val educationLevels: List<String> = emptyList(),
    val educationFieldMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val educationFields: List<String> = emptyList(),
    val occupationMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val occupationCategories: List<String> = emptyList(),
    val employerTypeMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val employerTypes: List<String> = emptyList(),
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
    val familyStatusMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val familyStatuses: List<String> = emptyList(),
    val familyValuesMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val familyValues: List<String> = emptyList(),
    val physicalStatusMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val physicalStatuses: List<String> = emptyList(),
    val residentialStatusMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val residentialStatuses: List<String> = emptyList(),
    val visaStatusMode: PartnerPreferenceMode = PartnerPreferenceMode.NO_PREFERENCE,
    val visaStatuses: List<String> = emptyList()
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

    suspend fun loadPublicSummary(targetUid: String): PartnerPreferenceSummary {
        require(targetUid.isNotBlank()) { "Target profile is required" }
        val result = functions.getHttpsCallable("getPartnerPreferenceSummary")
            .call(mapOf("targetUid" to targetUid))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: emptyMap()
        val items = (data["items"] as? List<*>).orEmpty().mapNotNull { raw ->
            @Suppress("UNCHECKED_CAST")
            val item = raw as? Map<String, Any?> ?: return@mapNotNull null
            val key = item["key"] as? String ?: return@mapNotNull null
            val value = item["value"] as? String ?: return@mapNotNull null
            val mode = mode(item["mode"])
            if (mode == PartnerPreferenceMode.NO_PREFERENCE || value.isBlank()) return@mapNotNull null
            PartnerPreferenceSummaryItem(key = key, mode = mode, value = value)
        }
        return PartnerPreferenceSummary(
            shared = data["shared"] as? Boolean ?: false,
            items = items
        )
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
        "sharePublicSummary" to value.sharePublicSummary,
        "ageMode" to value.ageMode.name,
        "ageMin" to value.ageMin,
        "ageMax" to value.ageMax,
        "heightMode" to value.heightMode.name,
        "heightMinCm" to value.heightMinCm,
        "heightMaxCm" to value.heightMaxCm,
        "weightMode" to value.weightMode.name,
        "weightMinKg" to value.weightMinKg,
        "weightMaxKg" to value.weightMaxKg,
        "incomeBandMode" to value.incomeBandMode.name,
        "incomeBands" to value.incomeBands,
        "complexionMode" to value.complexionMode.name,
        "complexions" to value.complexions,
        "religionMode" to value.religionMode.name,
        "religions" to value.religions,
        "casteMode" to value.casteMode.name,
        "castes" to value.castes,
        "subCasteMode" to value.subCasteMode.name,
        "subCastes" to value.subCastes,
        "gothraMode" to value.gothraMode.name,
        "gothras" to value.gothras,
        "faithTraditionMode" to value.faithTraditionMode.name,
        "faithTraditions" to value.faithTraditions,
        "faithSubTraditionMode" to value.faithSubTraditionMode.name,
        "faithSubTraditions" to value.faithSubTraditions,
        "faithInstitutionMode" to value.faithInstitutionMode.name,
        "faithInstitutions" to value.faithInstitutions,
        "stateMode" to value.stateMode.name,
        "states" to value.states,
        "nativeStateMode" to value.nativeStateMode.name,
        "nativeStates" to value.nativeStates,
        "cityMode" to value.cityMode.name,
        "cities" to value.cities,
        "motherTongueMode" to value.motherTongueMode.name,
        "motherTongues" to value.motherTongues,
        "maritalStatusMode" to value.maritalStatusMode.name,
        "maritalStatuses" to value.maritalStatuses,
        "educationMode" to value.educationMode.name,
        "educationLevels" to value.educationLevels,
        "educationFieldMode" to value.educationFieldMode.name,
        "educationFields" to value.educationFields,
        "occupationMode" to value.occupationMode.name,
        "occupationCategories" to value.occupationCategories,
        "employerTypeMode" to value.employerTypeMode.name,
        "employerTypes" to value.employerTypes,
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
        "familyStatusMode" to value.familyStatusMode.name,
        "familyStatuses" to value.familyStatuses,
        "familyValuesMode" to value.familyValuesMode.name,
        "familyValues" to value.familyValues,
        "physicalStatusMode" to value.physicalStatusMode.name,
        "physicalStatuses" to value.physicalStatuses,
        "residentialStatusMode" to value.residentialStatusMode.name,
        "residentialStatuses" to value.residentialStatuses,
        "visaStatusMode" to value.visaStatusMode.name,
        "visaStatuses" to value.visaStatuses
    )

    private fun fromMap(data: Map<String, Any?>): PartnerPreferences = PartnerPreferences(
        configured = data["configured"] as? Boolean ?: false,
        sharePublicSummary = data["sharePublicSummary"] as? Boolean ?: false,
        ageMode = mode(data["ageMode"]),
        ageMin = int(data["ageMin"], 18),
        ageMax = int(data["ageMax"], 70),
        heightMode = mode(data["heightMode"]),
        heightMinCm = int(data["heightMinCm"], 90),
        heightMaxCm = int(data["heightMaxCm"], 250),
        weightMode = mode(data["weightMode"]),
        weightMinKg = int(data["weightMinKg"], 30),
        weightMaxKg = int(data["weightMaxKg"], 250),
        incomeBandMode = mode(data["incomeBandMode"]),
        incomeBands = strings(data["incomeBands"]),
        complexionMode = mode(data["complexionMode"]),
        complexions = strings(data["complexions"]),
        religionMode = mode(data["religionMode"]),
        religions = strings(data["religions"]),
        casteMode = mode(data["casteMode"]),
        castes = strings(data["castes"]),
        subCasteMode = mode(data["subCasteMode"]),
        subCastes = strings(data["subCastes"]),
        gothraMode = mode(data["gothraMode"]),
        gothras = strings(data["gothras"]),
        faithTraditionMode = mode(data["faithTraditionMode"]),
        faithTraditions = strings(data["faithTraditions"]),
        faithSubTraditionMode = mode(data["faithSubTraditionMode"]),
        faithSubTraditions = strings(data["faithSubTraditions"]),
        faithInstitutionMode = mode(data["faithInstitutionMode"]),
        faithInstitutions = strings(data["faithInstitutions"]),
        stateMode = mode(data["stateMode"]),
        states = strings(data["states"]),
        nativeStateMode = mode(data["nativeStateMode"]),
        nativeStates = strings(data["nativeStates"]),
        cityMode = mode(data["cityMode"]),
        cities = strings(data["cities"]),
        motherTongueMode = mode(data["motherTongueMode"]),
        motherTongues = strings(data["motherTongues"]),
        maritalStatusMode = mode(data["maritalStatusMode"]),
        maritalStatuses = strings(data["maritalStatuses"]),
        educationMode = mode(data["educationMode"]),
        educationLevels = strings(data["educationLevels"]),
        educationFieldMode = mode(data["educationFieldMode"]),
        educationFields = strings(data["educationFields"]),
        occupationMode = mode(data["occupationMode"]),
        occupationCategories = strings(data["occupationCategories"]),
        employerTypeMode = mode(data["employerTypeMode"]),
        employerTypes = strings(data["employerTypes"]),
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
        familyStatusMode = mode(data["familyStatusMode"]),
        familyStatuses = strings(data["familyStatuses"]),
        familyValuesMode = mode(data["familyValuesMode"]),
        familyValues = strings(data["familyValues"]),
        physicalStatusMode = mode(data["physicalStatusMode"]),
        physicalStatuses = strings(data["physicalStatuses"]),
        residentialStatusMode = mode(data["residentialStatusMode"]),
        residentialStatuses = strings(data["residentialStatuses"]),
        visaStatusMode = mode(data["visaStatusMode"]),
        visaStatuses = strings(data["visaStatuses"])
    )

    private fun mode(value: Any?): PartnerPreferenceMode =
        runCatching { PartnerPreferenceMode.valueOf(value as? String ?: "") }
            .getOrDefault(PartnerPreferenceMode.NO_PREFERENCE)

    private fun int(value: Any?, fallback: Int): Int = (value as? Number)?.toInt() ?: fallback

    private fun strings(value: Any?): List<String> =
        (value as? List<*>)?.filterIsInstance<String>().orEmpty()
}
