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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.PartnerPreferenceMode
import com.match.app.data.repo.PartnerPreferenceRepository
import com.match.app.data.repo.PartnerPreferences
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeTopBar
import com.match.app.ui.theme.MatreeDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PartnerPreferencesUi(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val value: PartnerPreferences = PartnerPreferences(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class PartnerPreferencesViewModel @Inject constructor(
    private val repository: PartnerPreferenceRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(PartnerPreferencesUi())
    val ui: StateFlow<PartnerPreferencesUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.load() }
                .onSuccess { value -> _ui.value = PartnerPreferencesUi(loading = false, value = value) }
                .onFailure { error ->
                    _ui.value = PartnerPreferencesUi(
                        loading = false,
                        error = error.message ?: "Could not load partner preferences."
                    )
                }
        }
    }

    fun update(transform: (PartnerPreferences) -> PartnerPreferences) {
        _ui.update { it.copy(value = transform(it.value), error = null, message = null) }
    }

    fun save() = viewModelScope.launch {
        if (_ui.value.saving) return@launch
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
    onBack: () -> Unit,
    vm: PartnerPreferencesViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.message, ui.error) {
        val message = ui.error ?: ui.message
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { MatreeTopBar(title = "Partner Preferences", onBack = onBack) }
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
                    message = "Strict preferences exclude profiles in both directions. Preferred preferences improve ordering but do not hide otherwise eligible members."
                )

                RangePreferenceCard(
                    title = "Age",
                    mode = ui.value.ageMode,
                    min = ui.value.ageMin,
                    max = ui.value.ageMax,
                    minAllowed = 18,
                    maxAllowed = 99,
                    suffix = "years",
                    onMode = { mode -> vm.update { it.copy(ageMode = mode) } },
                    onRange = { min, max -> vm.update { it.copy(ageMin = min, ageMax = max) } }
                )

                RangePreferenceCard(
                    title = "Height",
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

                ListPreferenceCard(
                    title = "Religion",
                    mode = ui.value.religionMode,
                    values = ui.value.religions,
                    hint = "Hindu, Muslim, Christian…",
                    onMode = { mode -> vm.update { it.copy(religionMode = mode) } },
                    onValues = { values -> vm.update { it.copy(religions = values) } }
                )
                ListPreferenceCard(
                    title = "State / region",
                    mode = ui.value.stateMode,
                    values = ui.value.states,
                    hint = "Telangana, Karnataka…",
                    onMode = { mode -> vm.update { it.copy(stateMode = mode) } },
                    onValues = { values -> vm.update { it.copy(states = values) } }
                )
                ListPreferenceCard(
                    title = "City",
                    mode = ui.value.cityMode,
                    values = ui.value.cities,
                    hint = "Hyderabad, Bengaluru…",
                    onMode = { mode -> vm.update { it.copy(cityMode = mode) } },
                    onValues = { values -> vm.update { it.copy(cities = values) } }
                )
                ListPreferenceCard(
                    title = "Mother tongue",
                    mode = ui.value.motherTongueMode,
                    values = ui.value.motherTongues,
                    hint = "Telugu, Hindi…",
                    onMode = { mode -> vm.update { it.copy(motherTongueMode = mode) } },
                    onValues = { values -> vm.update { it.copy(motherTongues = values) } }
                )
                ListPreferenceCard(
                    title = "Marital status",
                    mode = ui.value.maritalStatusMode,
                    values = ui.value.maritalStatuses,
                    hint = "Never Married, Divorced…",
                    onMode = { mode -> vm.update { it.copy(maritalStatusMode = mode) } },
                    onValues = { values -> vm.update { it.copy(maritalStatuses = values) } }
                )
                ListPreferenceCard(
                    title = "Education",
                    mode = ui.value.educationMode,
                    values = ui.value.educationLevels,
                    hint = "Bachelors, Masters…",
                    onMode = { mode -> vm.update { it.copy(educationMode = mode) } },
                    onValues = { values -> vm.update { it.copy(educationLevels = values) } }
                )
                ListPreferenceCard(
                    title = "Occupation",
                    mode = ui.value.occupationMode,
                    values = ui.value.occupationCategories,
                    hint = "Software, Healthcare…",
                    onMode = { mode -> vm.update { it.copy(occupationMode = mode) } },
                    onValues = { values -> vm.update { it.copy(occupationCategories = values) } }
                )
                ListPreferenceCard(
                    title = "Diet",
                    mode = ui.value.dietMode,
                    values = ui.value.diets,
                    hint = "Vegetarian, Non-vegetarian…",
                    onMode = { mode -> vm.update { it.copy(dietMode = mode) } },
                    onValues = { values -> vm.update { it.copy(diets = values) } }
                )
                ListPreferenceCard(
                    title = "Smoking",
                    mode = ui.value.smokingMode,
                    values = ui.value.smoking,
                    hint = "Never, Occasionally…",
                    onMode = { mode -> vm.update { it.copy(smokingMode = mode) } },
                    onValues = { values -> vm.update { it.copy(smoking = values) } }
                )
                ListPreferenceCard(
                    title = "Drinking",
                    mode = ui.value.drinkingMode,
                    values = ui.value.drinking,
                    hint = "Never, Occasionally…",
                    onMode = { mode -> vm.update { it.copy(drinkingMode = mode) } },
                    onValues = { values -> vm.update { it.copy(drinking = values) } }
                )

                Button(
                    onClick = vm::save,
                    enabled = !ui.saving,
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
                    Text(if (ui.saving) "Saving…" else "Save partner preferences")
                }
                Spacer(Modifier.height(MatreeDesign.spacing.lg))
            }
        }
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
                            PartnerPreferenceMode.STRICT -> "Strict"
                            PartnerPreferenceMode.PREFERRED -> "Preferred"
                            PartnerPreferenceMode.NO_PREFERENCE -> "No preference"
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
                        label = "Minimum",
                        value = min,
                        allowed = minAllowed..maxAllowed,
                        suffix = suffix,
                        modifier = Modifier.weight(1f),
                        onValue = { value -> onRange(value, max.coerceAtLeast(value)) }
                    )
                    NumericPreferenceField(
                        label = "Maximum",
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
                    label = { Text("Accepted values") },
                    placeholder = { Text(hint) },
                    supportingText = {
                        Text("Separate multiple values with commas.")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
