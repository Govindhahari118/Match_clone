package com.match.app.ui.preferences

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.ConsentRepository
import com.match.app.data.repo.PartnerPreferenceMode
import com.match.app.data.repo.PartnerPreferenceRepository
import com.match.app.data.repo.PartnerPreferences
import com.match.app.data.session.SessionStore
import com.match.app.domain.profile.IndiaProfileCatalog
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeTopBar
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PartnerPreferencesUi(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val consentSaving: Boolean = false,
    val sensitiveConsentCurrent: Boolean = false,
    val sensitiveConsentVersion: String = "",
    val profileReligion: String = "",
    val value: PartnerPreferences = PartnerPreferences(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class PartnerPreferencesViewModel @Inject constructor(
    private val repository: PartnerPreferenceRepository,
    private val consentRepository: ConsentRepository,
    private val session: SessionStore,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(PartnerPreferencesUi())
    val ui: StateFlow<PartnerPreferencesUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val preferences = runCatching { repository.load() }
            val consents = runCatching { consentRepository.getState() }
            val profileReligion = runCatching {
                session.userId.first()?.let { userId ->
                    authRepository.currentProfile(userId)?.religion
                }.orEmpty()
            }.getOrDefault("")
            val consent = consents.getOrNull()
                ?.firstOrNull { it.purpose == "sensitive_preferences" }
            _ui.value = PartnerPreferencesUi(
                loading = false,
                profileReligion = profileReligion,
                value = preferences.getOrDefault(PartnerPreferences()),
                sensitiveConsentCurrent = consent?.isCurrent == true,
                sensitiveConsentVersion = consent?.noticeVersion.orEmpty(),
                error = preferences.exceptionOrNull()?.message
                    ?: consents.exceptionOrNull()?.message
            )
        }
    }

    fun update(transform: (PartnerPreferences) -> PartnerPreferences) {
        _ui.update { it.copy(value = transform(it.value), error = null, message = null) }
    }

    fun setSensitiveConsent(granted: Boolean) = viewModelScope.launch {
        if (_ui.value.consentSaving) return@launch
        _ui.update { it.copy(consentSaving = true, error = null, message = null) }
        runCatching {
            consentRepository.set("sensitive_preferences", granted)
            consentRepository.getState()
                .firstOrNull { it.purpose == "sensitive_preferences" }
        }.onSuccess { state ->
            _ui.update {
                it.copy(
                    consentSaving = false,
                    sensitiveConsentCurrent = state?.isCurrent == true,
                    sensitiveConsentVersion = state?.noticeVersion.orEmpty()
                )
            }
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    consentSaving = false,
                    error = error.message ?: "Could not update sensitive-preference consent."
                )
            }
        }
    }

    fun useMyConfirmedReligion() {
        val religion = _ui.value.profileReligion.trim()
        if (religion.isBlank()) {
            _ui.update { it.copy(error = "Your profile does not have a confirmed religion to use.") }
            return
        }
        if (!_ui.value.sensitiveConsentCurrent) {
            _ui.update {
                it.copy(error = "Enable sensitive-preference processing before using this preset.")
            }
            return
        }
        update {
            it.copy(
                religionMode = PartnerPreferenceMode.STRICT,
                religions = listOf(religion)
            )
        }
        _ui.update {
            it.copy(message = "Using your confirmed religion as a strict preference. Save to apply it.")
        }
    }

    fun save() = viewModelScope.launch {
        if (_ui.value.saving) return@launch
        if (!_ui.value.sensitiveConsentCurrent) {
            _ui.update {
                it.copy(error = "Review and enable sensitive-preference processing before saving.")
            }
            return@launch
        }
        _ui.update { it.copy(saving = true, error = null, message = null) }
        runCatching { repository.save(_ui.value.value) }
            .onSuccess { saved ->
                _ui.update {
                    it.copy(
                        saving = false,
                        value = saved,
                        message = "Partner preferences saved and applied to discovery."
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        saving = false,
                        error = error.message ?: "Could not save partner preferences."
                    )
                }
            }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null, error = null) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PartnerPreferencesScreen(
    onBack: (() -> Unit)? = null,
    onSaved: () -> Unit = {},
    vm: PartnerPreferencesViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    val localizedFeedback = when (val raw = ui.error ?: ui.message) {
        "Could not update sensitive-preference consent." ->
            t("could_not_update_sensitive_consent", "Could not update sensitive-preference consent.")
        "Your profile does not have a confirmed religion to use." ->
            t("profile_religion_missing", "Your profile does not have a confirmed religion to use.")
        "Enable sensitive-preference processing before using this preset." ->
            t("enable_sensitive_before_preset", "Enable sensitive-preference processing before using this preset.")
        "Using your confirmed religion as a strict preference. Save to apply it." ->
            t("confirmed_religion_preset_ready", "Using your confirmed religion as a strict preference. Save to apply it.")
        "Review and enable sensitive-preference processing before saving." ->
            t("enable_sensitive_before_saving", "Review and enable sensitive-preference processing before saving.")
        "Partner preferences saved and applied to discovery." ->
            t("preferences_saved", "Partner preferences saved and applied to discovery.")
        "Could not save partner preferences." ->
            t("could_not_save_preferences", "Could not save partner preferences.")
        else -> raw
    }

    LaunchedEffect(localizedFeedback) {
        if (localizedFeedback != null) {
            if (ui.error == null && ui.value.configured) onSaved()
            snackbar.showSnackbar(localizedFeedback)
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { MatreeTopBar(title = t("partner_preferences", "Partner Preferences"), onBack = onBack) }
    ) { padding ->
        when {
            ui.loading -> Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            else -> Column(
                modifier = Modifier.padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(MatreeDesign.spacing.md)
                    .testTag("partner_preferences_screen"),
                verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.md)
            ) {
                MatreeInlineNotice(
                    message = t("strict_preference_explanation", "Strict preferences exclude profiles in both directions. Preferred preferences improve ordering but do not hide otherwise eligible members.")
                )

                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(MatreeDesign.spacing.md),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                t("sensitive_preference_processing", "Sensitive preference processing"),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                t("sensitive_preference_body", "Some partner preferences can reveal sensitive personal choices. Enable this only if you want Matree to store and use these preferences for reciprocal discovery. You can withdraw this choice later in Privacy & visibility."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (ui.sensitiveConsentVersion.isNotBlank()) {
                                Text(
                                    t("notice_version", mapOf("version" to ui.sensitiveConsentVersion), "Notice version {version}"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(MatreeDesign.spacing.sm))
                        Switch(
                            checked = ui.sensitiveConsentCurrent,
                            onCheckedChange = vm::setSensitiveConsent,
                            enabled = !ui.consentSaving
                        )
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(MatreeDesign.spacing.md),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                t("show_partner_summary_title", "Show a summary on my profile"),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                t("show_partner_summary_body", "Share only selected non-sensitive expectations such as age, location, education, occupation and lifestyle. Sensitive criteria remain private."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(MatreeDesign.spacing.sm))
                        Switch(
                            checked = ui.value.sharePublicSummary,
                            onCheckedChange = { enabled ->
                                vm.update { it.copy(sharePublicSummary = enabled) }
                            },
                            enabled = ui.sensitiveConsentCurrent
                        )
                    }
                }

                if (ui.profileReligion.isNotBlank()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(MatreeDesign.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
                        ) {
                            Text(
                                t("quick_setup", "Quick setup"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                t("confirmed_religion_notice", mapOf("religion" to ui.profileReligion), "Your confirmed profile religion is {religion}. Matching does not use it automatically."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FilledTonalButton(
                                onClick = vm::useMyConfirmedReligion,
                                enabled = ui.sensitiveConsentCurrent
                            ) {
                                Text(t("use_confirmed_religion", "Use my confirmed religion"))
                            }
                        }
                    }
                }

                PreferenceSectionHeader(
                    title = t("core_preferences", "Core preferences"),
                    subtitle = t("core_preferences_subtitle", "Start with the few criteria that matter most. Every item remains optional.")
                )
                RangePreferenceCard(
                    title = t("age", "Age"),
                    mode = ui.value.ageMode,
                    min = ui.value.ageMin,
                    max = ui.value.ageMax,
                    minAllowed = 18,
                    maxAllowed = 99,
                    suffix = t("years_unit", "years"),
                    onMode = { mode -> vm.update { it.copy(ageMode = mode) } },
                    onRange = { min, max -> vm.update { it.copy(ageMin = min, ageMax = max) } }
                )

                RangePreferenceCard(
                    title = t("height", "Height"),
                    mode = ui.value.heightMode,
                    min = ui.value.heightMinCm,
                    max = ui.value.heightMaxCm,
                    minAllowed = 90,
                    maxAllowed = 250,
                    suffix = "cm",
                    onMode = { mode -> vm.update { it.copy(heightMode = mode) } },
                    onRange = { min, max ->
                        vm.update { it.copy(heightMinCm = min, heightMaxCm = max) }
                    }
                )
                RangePreferenceCard(
                    title = t("weight", "Weight"),
                    mode = ui.value.weightMode,
                    min = ui.value.weightMinKg,
                    max = ui.value.weightMaxKg,
                    minAllowed = 30,
                    maxAllowed = 250,
                    suffix = t("kg_unit", "kg"),
                    onMode = { mode -> vm.update { it.copy(weightMode = mode) } },
                    onRange = { min, max ->
                        vm.update { it.copy(weightMinKg = min, weightMaxKg = max) }
                    }
                )
                ChoicePreferenceCard(
                    title = t("annual_income_range", "Annual income range"),
                    mode = ui.value.incomeBandMode,
                    selected = ui.value.incomeBands,
                    options = IndiaProfileCatalog.incomeBands.map { it to it },
                    onMode = { mode -> vm.update { it.copy(incomeBandMode = mode) } },
                    onSelected = { values -> vm.update { it.copy(incomeBands = values) } }
                )
                ChoicePreferenceCard(
                    title = t("skin_tone_optional", "Skin tone (optional, self-described)"),
                    mode = ui.value.complexionMode,
                    selected = ui.value.complexions,
                    options = IndiaProfileCatalog.complexionOptions.map { it to it },
                    onMode = { mode -> vm.update { it.copy(complexionMode = mode) } },
                    onSelected = { values -> vm.update { it.copy(complexions = values) } }
                )

                PreferenceSectionHeader(
                    title = t("community_faith", "Community & faith"),
                    subtitle = t("community_faith_subtitle", "Use only the religious or community details that matter to you.")
                )
                ListPreferenceCard(
                    title = t("religion", "Religion"),
                    mode = ui.value.religionMode,
                    values = ui.value.religions,
                    hint = t("religion_example", "Hindu, Muslim, Christian…"),
                    onMode = { mode -> vm.update { it.copy(religionMode = mode) } },
                    onValues = { values -> vm.update { it.copy(religions = values) } }
                )
                ListPreferenceCard(
                    title = t("community_caste", "Community / caste"),
                    mode = ui.value.casteMode,
                    values = ui.value.castes,
                    hint = t("community_example", "Reddy, Brahmin, Kamma…"),
                    onMode = { mode -> vm.update { it.copy(casteMode = mode) } },
                    onValues = { values -> vm.update { it.copy(castes = values) } }
                )
                ListPreferenceCard(
                    title = t("subcommunity_subcaste", "Sub-community / sub-caste"),
                    mode = ui.value.subCasteMode,
                    values = ui.value.subCastes,
                    hint = t("subcommunity_hint", "Optional sub-community preferences"),
                    onMode = { mode -> vm.update { it.copy(subCasteMode = mode) } },
                    onValues = { values -> vm.update { it.copy(subCastes = values) } }
                )
                ListPreferenceCard(
                    title = t("gothra_clan", "Gothra / clan"),
                    mode = ui.value.gothraMode,
                    values = ui.value.gothras,
                    hint = t("gothra_hint", "Optional lineage preference"),
                    onMode = { mode -> vm.update { it.copy(gothraMode = mode) } },
                    onValues = { values -> vm.update { it.copy(gothras = values) } }
                )
                ListPreferenceCard(
                    title = t("faith_tradition_denomination", "Faith tradition / denomination"),
                    mode = ui.value.faithTraditionMode,
                    values = ui.value.faithTraditions,
                    hint = t("faith_tradition_hint", "Denomination, sect, tradition…"),
                    onMode = { mode -> vm.update { it.copy(faithTraditionMode = mode) } },
                    onValues = { values -> vm.update { it.copy(faithTraditions = values) } }
                )
                ListPreferenceCard(
                    title = t("faith_subtradition", "Faith sub-tradition"),
                    mode = ui.value.faithSubTraditionMode,
                    values = ui.value.faithSubTraditions,
                    hint = t("faith_subtradition_hint", "Optional sub-tradition"),
                    onMode = { mode -> vm.update { it.copy(faithSubTraditionMode = mode) } },
                    onValues = { values -> vm.update { it.copy(faithSubTraditions = values) } }
                )
                ListPreferenceCard(
                    title = t("faith_institution_community", "Faith institution / community"),
                    mode = ui.value.faithInstitutionMode,
                    values = ui.value.faithInstitutions,
                    hint = t("faith_institution_hint", "Optional church, jamaat, samaj or institution"),
                    onMode = { mode -> vm.update { it.copy(faithInstitutionMode = mode) } },
                    onValues = { values -> vm.update { it.copy(faithInstitutions = values) } }
                )
                PreferenceSectionHeader(
                    title = t("location_residence", "Location & residence"),
                    subtitle = t("location_residence_subtitle", "Set geography, citizenship, overseas residence and relocation choices.")
                )
                ListPreferenceCard(
                    title = t("state_region", "State / region"),
                    mode = ui.value.stateMode,
                    values = ui.value.states,
                    hint = t("state_example", "Telangana, Karnataka…"),
                    onMode = { mode -> vm.update { it.copy(stateMode = mode) } },
                    onValues = { values -> vm.update { it.copy(states = values) } }
                )
                ListPreferenceCard(
                    title = t("native_state", "Native state"),
                    mode = ui.value.nativeStateMode,
                    values = ui.value.nativeStates,
                    hint = t("native_state_example", "Telangana, Andhra Pradesh…"),
                    onMode = { mode -> vm.update { it.copy(nativeStateMode = mode) } },
                    onValues = { values -> vm.update { it.copy(nativeStates = values) } }
                )
                ListPreferenceCard(
                    title = t("city", "City"),
                    mode = ui.value.cityMode,
                    values = ui.value.cities,
                    hint = t("city_example", "Hyderabad, Bengaluru…"),
                    onMode = { mode -> vm.update { it.copy(cityMode = mode) } },
                    onValues = { values -> vm.update { it.copy(cities = values) } }
                )
                ListPreferenceCard(
                    title = t("country_of_residence", "Country of residence"),
                    mode = ui.value.countryOfResidenceMode,
                    values = ui.value.countriesOfResidence,
                    hint = t("country_example", "India, United States, UAE…"),
                    onMode = { mode -> vm.update { it.copy(countryOfResidenceMode = mode) } },
                    onValues = { values -> vm.update { it.copy(countriesOfResidence = values) } }
                )
                ListPreferenceCard(
                    title = t("citizenship", "Citizenship"),
                    mode = ui.value.citizenshipMode,
                    values = ui.value.citizenships,
                    hint = t("citizenship_example", "India, United States…"),
                    onMode = { mode -> vm.update { it.copy(citizenshipMode = mode) } },
                    onValues = { values -> vm.update { it.copy(citizenships = values) } }
                )
                ListPreferenceCard(
                    title = t("residential_status", "Residential status"),
                    mode = ui.value.residentialStatusMode,
                    values = ui.value.residentialStatuses,
                    hint = t("residential_status_example", "Citizen, Permanent Resident, Work Visa…"),
                    onMode = { mode -> vm.update { it.copy(residentialStatusMode = mode) } },
                    onValues = { values -> vm.update { it.copy(residentialStatuses = values) } }
                )
                ListPreferenceCard(
                    title = t("visa_permit_status", "Visa / permit status"),
                    mode = ui.value.visaStatusMode,
                    values = ui.value.visaStatuses,
                    hint = t("visa_status_example", "Citizen, PR, H-1B, student visa…"),
                    onMode = { mode -> vm.update { it.copy(visaStatusMode = mode) } },
                    onValues = { values -> vm.update { it.copy(visaStatuses = values) } }
                )
                ChoicePreferenceCard(
                    title = t("residence_class", "Residence class"),
                    mode = ui.value.nriMode,
                    selected = ui.value.nriStatuses,
                    options = listOf(
                        "INDIA_RESIDENT" to t("india_resident", "India resident"),
                        "NRI" to t("nri_overseas_resident", "NRI / overseas resident")
                    ),
                    onMode = { mode -> vm.update { it.copy(nriMode = mode) } },
                    onSelected = { values -> vm.update { it.copy(nriStatuses = values) } }
                )
                ChoicePreferenceCard(
                    title = t("relocation", "Relocation"),
                    mode = ui.value.relocationMode,
                    selected = ui.value.relocationStatuses,
                    options = listOf(
                        "WILLING_TO_RELOCATE" to t("willing_to_relocate", "Willing to relocate"),
                        "NOT_WILLING_TO_RELOCATE" to t("not_willing_to_relocate", "Not willing to relocate")
                    ),
                    onMode = { mode -> vm.update { it.copy(relocationMode = mode) } },
                    onSelected = { values -> vm.update { it.copy(relocationStatuses = values) } }
                )
                PreferenceSectionHeader(
                    title = t("language_family_career", "Language, family & career"),
                    subtitle = t("language_family_career_subtitle", "Refine life-stage, education, work and family-background preferences.")
                )
                ListPreferenceCard(
                    title = t("mother_tongue", "Mother tongue"),
                    mode = ui.value.motherTongueMode,
                    values = ui.value.motherTongues,
                    hint = t("mother_tongue_example", "Telugu, Hindi…"),
                    onMode = { mode -> vm.update { it.copy(motherTongueMode = mode) } },
                    onValues = { values -> vm.update { it.copy(motherTongues = values) } }
                )
                ListPreferenceCard(
                    title = t("marital_status", "Marital status"),
                    mode = ui.value.maritalStatusMode,
                    values = ui.value.maritalStatuses,
                    hint = t("marital_status_example", "Never Married, Divorced…"),
                    onMode = { mode -> vm.update { it.copy(maritalStatusMode = mode) } },
                    onValues = { values -> vm.update { it.copy(maritalStatuses = values) } }
                )
                ChoicePreferenceCard(
                    title = t("children", "Children"),
                    mode = ui.value.childrenMode,
                    selected = ui.value.childrenStatuses,
                    options = listOf(
                        "NO_CHILDREN" to t("no_children", "No children"),
                        "HAS_CHILDREN" to t("has_children", "Has children")
                    ),
                    onMode = { mode -> vm.update { it.copy(childrenMode = mode) } },
                    onSelected = { values -> vm.update { it.copy(childrenStatuses = values) } }
                )
                ListPreferenceCard(
                    title = t("education", "Education"),
                    mode = ui.value.educationMode,
                    values = ui.value.educationLevels,
                    hint = t("education_example", "Bachelors, Masters…"),
                    onMode = { mode -> vm.update { it.copy(educationMode = mode) } },
                    onValues = { values -> vm.update { it.copy(educationLevels = values) } }
                )
                ListPreferenceCard(
                    title = t("education_field", "Education field"),
                    mode = ui.value.educationFieldMode,
                    values = ui.value.educationFields,
                    hint = t("education_field_example", "Engineering, Medicine, Commerce…"),
                    onMode = { mode -> vm.update { it.copy(educationFieldMode = mode) } },
                    onValues = { values -> vm.update { it.copy(educationFields = values) } }
                )
                ListPreferenceCard(
                    title = t("occupation", "Occupation"),
                    mode = ui.value.occupationMode,
                    values = ui.value.occupationCategories,
                    hint = t("occupation_example", "Software, Healthcare…"),
                    onMode = { mode -> vm.update { it.copy(occupationMode = mode) } },
                    onValues = { values -> vm.update { it.copy(occupationCategories = values) } }
                )
                ListPreferenceCard(
                    title = t("employer_type", "Employer type"),
                    mode = ui.value.employerTypeMode,
                    values = ui.value.employerTypes,
                    hint = t("employer_type_example", "Private, Government, Self-employed…"),
                    onMode = { mode -> vm.update { it.copy(employerTypeMode = mode) } },
                    onValues = { values -> vm.update { it.copy(employerTypes = values) } }
                )
                ListPreferenceCard(
                    title = t("family_type", "Family type"),
                    mode = ui.value.familyTypeMode,
                    values = ui.value.familyTypes,
                    hint = t("family_type_example", "Nuclear, Joint…"),
                    onMode = { mode -> vm.update { it.copy(familyTypeMode = mode) } },
                    onValues = { values -> vm.update { it.copy(familyTypes = values) } }
                )
                ListPreferenceCard(
                    title = t("family_status", "Family status"),
                    mode = ui.value.familyStatusMode,
                    values = ui.value.familyStatuses,
                    hint = t("family_status_example", "Middle class, Upper middle class…"),
                    onMode = { mode -> vm.update { it.copy(familyStatusMode = mode) } },
                    onValues = { values -> vm.update { it.copy(familyStatuses = values) } }
                )
                ListPreferenceCard(
                    title = t("family_values", "Family values"),
                    mode = ui.value.familyValuesMode,
                    values = ui.value.familyValues,
                    hint = t("family_values_example", "Traditional, Moderate, Liberal…"),
                    onMode = { mode -> vm.update { it.copy(familyValuesMode = mode) } },
                    onValues = { values -> vm.update { it.copy(familyValues = values) } }
                )
                PreferenceSectionHeader(
                    title = t("lifestyle_wellbeing", "Lifestyle & wellbeing"),
                    subtitle = t("lifestyle_wellbeing_subtitle", "Optional lifestyle criteria. Leave them at No preference to keep discovery broad.")
                )
                ListPreferenceCard(
                    title = t("physical_status", "Physical status"),
                    mode = ui.value.physicalStatusMode,
                    values = ui.value.physicalStatuses,
                    hint = t("physical_status_example", "Normal, Differently abled…"),
                    onMode = { mode -> vm.update { it.copy(physicalStatusMode = mode) } },
                    onValues = { values -> vm.update { it.copy(physicalStatuses = values) } }
                )
                ListPreferenceCard(
                    title = t("diet", "Diet"),
                    mode = ui.value.dietMode,
                    values = ui.value.diets,
                    hint = t("diet_example", "Vegetarian, Non-vegetarian…"),
                    onMode = { mode -> vm.update { it.copy(dietMode = mode) } },
                    onValues = { values -> vm.update { it.copy(diets = values) } }
                )
                ListPreferenceCard(
                    title = t("smoking", "Smoking"),
                    mode = ui.value.smokingMode,
                    values = ui.value.smoking,
                    hint = t("habit_example", "Never, Occasionally…"),
                    onMode = { mode -> vm.update { it.copy(smokingMode = mode) } },
                    onValues = { values -> vm.update { it.copy(smoking = values) } }
                )
                ListPreferenceCard(
                    title = t("drinking", "Drinking"),
                    mode = ui.value.drinkingMode,
                    values = ui.value.drinking,
                    hint = t("habit_example", "Never, Occasionally…"),
                    onMode = { mode -> vm.update { it.copy(drinkingMode = mode) } },
                    onValues = { values -> vm.update { it.copy(drinking = values) } }
                )

                Button(
                    onClick = vm::save,
                    enabled = !ui.saving && !ui.consentSaving && ui.sensitiveConsentCurrent,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("partner_preferences_save")
                ) {
                    if (ui.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.Tune, contentDescription = null)
                    }
                    Spacer(Modifier.width(MatreeDesign.spacing.xs))
                    Text(if (ui.saving) t("saving", "Saving…") else t("save_partner_preferences", "Save partner preferences"))
                }
                Spacer(Modifier.height(MatreeDesign.spacing.lg))
            }
        }
    }
}

@Composable
private fun PreferenceSectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceModeRow(
    selected: PartnerPreferenceMode,
    onSelect: (PartnerPreferenceMode) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
    ) {
        PartnerPreferenceMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = {
                    Text(
                        when (mode) {
                            PartnerPreferenceMode.STRICT -> t("strict", "Strict")
                            PartnerPreferenceMode.PREFERRED -> t("preferred", "Preferred")
                            PartnerPreferenceMode.NO_PREFERENCE -> t("no_preference", "No preference")
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun RangePreferenceCard(
    title: String,
    mode: PartnerPreferenceMode,
    min: Int,
    max: Int,
    minAllowed: Int,
    maxAllowed: Int,
    suffix: String,
    onMode: (PartnerPreferenceMode) -> Unit,
    onRange: (Int, Int) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(MatreeDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            PreferenceModeRow(mode, onMode)
            if (mode != PartnerPreferenceMode.NO_PREFERENCE) {
                Row(horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
                    NumericPreferenceField(
                        label = t("minimum", "Minimum"),
                        value = min,
                        allowed = minAllowed..maxAllowed,
                        suffix = suffix,
                        modifier = Modifier.weight(1f),
                        onValue = { value -> onRange(value, max.coerceAtLeast(value)) }
                    )
                    NumericPreferenceField(
                        label = t("maximum", "Maximum"),
                        value = max,
                        allowed = minAllowed..maxAllowed,
                        suffix = suffix,
                        modifier = Modifier.weight(1f),
                        onValue = { value -> onRange(min.coerceAtMost(value), value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericPreferenceField(
    label: String,
    value: Int,
    allowed: IntRange,
    suffix: String,
    modifier: Modifier,
    onValue: (Int) -> Unit
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { raw ->
            raw.filter(Char::isDigit).toIntOrNull()
                ?.coerceIn(allowed.first, allowed.last)
                ?.let(onValue)
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoicePreferenceCard(
    title: String,
    mode: PartnerPreferenceMode,
    selected: List<String>,
    options: List<Pair<String, String>>,
    onMode: (PartnerPreferenceMode) -> Unit,
    onSelected: (List<String>) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(MatreeDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            PreferenceModeRow(mode, onMode)
            if (mode != PartnerPreferenceMode.NO_PREFERENCE) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
                ) {
                    options.forEach { (value, label) ->
                        val active = value in selected
                        FilterChip(
                            selected = active,
                            onClick = {
                                onSelected(
                                    if (active) selected - value
                                    else (selected + value).distinct()
                                )
                            },
                            label = { Text(label) }
                        )
                    }
                }
                if (selected.isEmpty()) {
                    Text(
                        t("choose_option_or_no_preference", "Choose at least one option or use No preference."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ListPreferenceCard(
    title: String,
    mode: PartnerPreferenceMode,
    values: List<String>,
    hint: String,
    onMode: (PartnerPreferenceMode) -> Unit,
    onValues: (List<String>) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(MatreeDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            PreferenceModeRow(mode, onMode)
            if (mode != PartnerPreferenceMode.NO_PREFERENCE) {
                OutlinedTextField(
                    value = values.joinToString(", "),
                    onValueChange = { raw ->
                        onValues(
                            raw.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .distinctBy { it.lowercase() }
                                .take(20)
                        )
                    },
                    label = { Text(t("accepted_values", "Accepted values")) },
                    placeholder = { Text(hint) },
                    supportingText = {
                        Text(t("separate_values_commas", "Separate multiple values with commas."))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
