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
    val drinking: List<String> = emptyList()
)

@Singleton
class PartnerPreferenceRepository @Inject constructor(
    private val consentRepository: ConsentRepository
) {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun load(): PartnerPreferences {
        val result = functions.getHttpsCallable("getPartnerPreferences").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: emptyMap()
        return fromMap(data)
    }

    suspend fun save(value: PartnerPreferences): PartnerPreferences {
        // Saving durable partner intent is the explicit action that records the current
        // sensitive-preferences notice before the backend accepts those fields.
        consentRepository.set("sensitive_preferences", true)
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
        "drinking" to value.drinking
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
        drinking = strings(data["drinking"])
    )

    private fun mode(value: Any?): PartnerPreferenceMode =
        runCatching { PartnerPreferenceMode.valueOf(value as? String ?: "") }
            .getOrDefault(PartnerPreferenceMode.NO_PREFERENCE)

    private fun int(value: Any?, fallback: Int): Int = (value as? Number)?.toInt() ?: fallback

    private fun strings(value: Any?): List<String> =
        (value as? List<*>)?.filterIsInstance<String>().orEmpty()
}
