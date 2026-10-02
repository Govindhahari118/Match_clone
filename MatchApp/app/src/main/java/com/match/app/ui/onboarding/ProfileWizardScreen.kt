package com.match.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.profile.IndiaProfileCatalog
import com.match.app.domain.profile.ReligionFieldDefinition
import com.match.app.domain.profile.ReligionProfileSchemas
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.components.MatreeSecondaryButton
import com.match.app.ui.components.MatreeTopBar
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign

/** One shared 8-step profile flow for members across India and abroad. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileWizardScreen(
    onComplete: () -> Unit = {},
    vm: ProfileWizardViewModel = hiltViewModel()
) {
    val step by vm.currentStep.collectAsState()
    val saving by vm.saving.collectAsState()
    val error by vm.saveError.collectAsState()
    val religionConfirmationRequired by vm.religionConfirmationRequired.collectAsState()
    val wizardState by vm.wizardState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            MatreeTopBar(
                title = "${t("step", "Step")} ${step + 1} / 8",
                onBack = if (step > 0) vm::previousStep else null
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            LinearProgressIndicator(
                progress = { (step + 1) / 8f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(MatreeDesign.spacing.md)
            ) {
                AnimatedContent(targetState = step, label = "wizard_step") { currentStep ->
                    when (currentStep) {
                        0 -> StepIdentityLocation(vm)
                        1 -> StepCommunity(vm)
                        2 -> StepEducationCareer(vm)
                        3 -> StepPhysicalRelationship(vm)
                        4 -> StepFamily(vm)
                        5 -> StepLifestyle(vm)
                        6 -> StepResidence(vm)
                        else -> StepAstrology(vm)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(MatreeDesign.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
            ) {
                if (step in 1..6) {
                    MatreeSecondaryButton(
                        text = t("skip", "Skip for now"),
                        onClick = vm::skipStep,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (step < 7) {
                    MatreePrimaryButton(
                        text = t("next", "Next"),
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = vm::nextStep,
                        enabled = !saving,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    MatreePrimaryButton(
                        text = if (saving) "Saving…" else t("complete_profile", "Complete Profile"),
                        icon = if (saving) null else Icons.Filled.Check,
                        onClick = { vm.finish(onComplete) },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth()
                    )                }
            }
        }
    }

    if (religionConfirmationRequired) {
        AlertDialog(
            onDismissRequest = vm::cancelReligionConfirmation,
            icon = { Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Confirm religion") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You selected ${wizardState.religion}.")
                    Text("Religion becomes protected profile information after profile creation because it affects community and compatibility information. Theme choice remains separate and can be changed later.")
                    Text(
                        "If this selection is incorrect, review it now. A later correction uses the controlled account-review process rather than a casual profile edit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { Button(onClick = vm::confirmReligionAndContinue) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = vm::cancelReligionConfirmation) { Text("Review") } }
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xxs)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(MatreeDesign.spacing.xs))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    required: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(if (required) "$label *" else label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.distinct().forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}

@Composable
private fun ReligionFieldInput(
    definition: ReligionFieldDefinition,
    value: String,
    onValueChange: (String) -> Unit
) {
    if (definition.options.isNotEmpty()) {
        ChoiceField(definition.label, value, definition.options, onValueChange)
        if (definition.helper.isNotBlank()) {
            Text(definition.helper, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(160)) },
            label = { Text(definition.label) },
            supportingText = { if (definition.helper.isNotBlank()) Text(definition.helper) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionChips(values: List<String>, selected: String, onSelected: (String) -> Unit) {
    if (values.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.take(12).forEach { value ->
            FilterChip(selected = value.equals(selected, true), onClick = { onSelected(value) }, label = { Text(value) })
        }
    }
}

private val profileCreatedForOptions = listOf(
    "SELF" to "Myself",
    "SON" to "Son",
    "DAUGHTER" to "Daughter",
    "SIBLING" to "Sibling",
    "PARENT" to "Parent",
    "RELATIVE" to "Other family member"
)

@Composable
private fun StepIdentityLocation(vm: ProfileWizardViewModel) {
    val s by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
        SectionHeader("Identity & location", "The app is the same for everyone. Your state and language only personalize your profile; partner discovery preferences remain separate.")
        val createdForLabel = profileCreatedForOptions
            .firstOrNull { it.first == s.profileCreatedFor }
            ?.second ?: "Myself"
        ChoiceField(
            label = "Profile created for",
            value = createdForLabel,
            options = profileCreatedForOptions.map { it.second },
            onSelected = { selected ->
                val canonical = profileCreatedForOptions.first { it.second == selected }.first
                vm.update(s.copy(profileCreatedFor = canonical))
            },
            required = true
        )
        Text(
            "This tells members whether the account is self-managed or family-assisted. It does not change matching eligibility.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = s.username,
            onValueChange = { vm.update(s.copy(username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '.' || ch == '_' }.take(30))) },
            label = { Text("Username *") }, prefix = { Text("@") }, singleLine = true,
            supportingText = { Text("3–30 letters, numbers, dot or underscore. Username is unique.") },
            modifier = Modifier.fillMaxWidth().testTag("wiz_username")
        )
        OutlinedTextField(s.displayName, { vm.update(s.copy(displayName = it.take(80))) }, label = { Text("Full name *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("wiz_name"))
        OutlinedTextField(s.dateOfBirth, { vm.update(s.copy(dateOfBirth = it.take(10))) }, label = { Text("Date of birth * (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("wiz_dob"))
        ChoiceField("State / union territory", s.state, IndiaProfileCatalog.statesAndUnionTerritories, { vm.update(s.copy(state = it)) }, required = true)
        OutlinedTextField(s.city, { vm.update(s.copy(city = it.take(80))) }, label = { Text("City *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("wiz_city"))
        val languageOptions = (IndiaProfileCatalog.languageSuggestionsForState(s.state) + IndiaProfileCatalog.indianLanguages).distinct()
        ChoiceField("Mother tongue", s.motherTongue, languageOptions, { vm.update(s.copy(motherTongue = it)) }, required = true)
        OutlinedTextField(s.bio, { vm.update(s.copy(bio = it.take(1000))) }, label = { Text("About me") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth().testTag("wiz_bio"))
    }
}

@Composable
private fun StepCommunity(vm: ProfileWizardViewModel) {
    val s by vm.wizardState.collectAsState()
    val religionCategory = ReligionCategory.fromReligion(s.religion)
    val schema = remember(s.religion) { ReligionProfileSchemas.forReligion(s.religion) }
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
        SectionHeader("Religion & community", "Choose what describes you. Nothing here is inferred from your state, language, surname or location.")
        ChoiceField("Religion", s.religion, IndiaProfileCatalog.religions, { vm.update(s.copy(religion = it)) }, required = true)
        Text("Common community choices", style = MaterialTheme.typography.labelLarge)
        SuggestionChips(IndiaProfileCatalog.communitySuggestions(s.religion), s.caste) { vm.update(s.copy(caste = if (it == "Other") "" else it)) }
        OutlinedTextField(s.caste, { vm.update(s.copy(caste = it.take(80))) }, label = { Text("Community / caste (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(s.subCaste, { vm.update(s.copy(subCaste = it.take(80))) }, label = { Text("Sub-community / sub-caste (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (religionCategory == ReligionCategory.HINDU) {
            OutlinedTextField(s.gothra, { vm.update(s.copy(gothra = it.take(80))) }, label = { Text("Gothra (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (s.religion.isNotBlank()) {
            HorizontalDivider()
            Text(schema.sectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            schema.primary?.let { definition ->
                ReligionFieldInput(definition, s.faithTradition) { vm.update(s.copy(faithTradition = it)) }
            }
            schema.secondary?.let { definition ->
                ReligionFieldInput(definition, s.faithSubTradition) { vm.update(s.copy(faithSubTradition = it)) }
            }
            schema.institution?.let { definition ->
                ReligionFieldInput(definition, s.faithInstitution) { vm.update(s.copy(faithInstitution = it)) }
            }
        }
    }
}

@Composable
private fun StepEducationCareer(vm: ProfileWizardViewModel) {
    val s by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
        SectionHeader("Education & career", "Add the information people commonly use while filtering profiles.")
        ChoiceField("Highest education", s.education, IndiaProfileCatalog.educationLevels, { vm.update(s.copy(education = it)) }, required = true)
        OutlinedTextField(s.educationField, { vm.update(s.copy(educationField = it.take(100))) }, label = { Text("Field of study") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(s.institution, { vm.update(s.copy(institution = it.take(120))) }, label = { Text("College / university") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(if (s.graduationYear > 0) s.graduationYear.toString() else "", { vm.update(s.copy(graduationYear = it.toIntOrNull()?.coerceIn(1950, 2100) ?: 0)) }, label = { Text("Graduation year") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(s.profession, { vm.update(s.copy(profession = it.take(100))) }, label = { Text("Occupation / role *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChoiceField(
        "Occupation category",
        s.occupationCategory,
        IndiaProfileCatalog.occupationCategories,
        { vm.update(s.copy(occupationCategory = it)) },
        required = true
    )
        OutlinedTextField(s.employer, { vm.update(s.copy(employer = it.take(120))) }, label = { Text("Employer / business") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChoiceField("Employer type", s.employerType, IndiaProfileCatalog.employerTypes, { vm.update(s.copy(employerType = it)) })
        ChoiceField(
        "Annual income range",
        s.incomeBand,
        IndiaProfileCatalog.incomeBands,
        { vm.update(s.copy(incomeBand = it)) },
        required = true
    )
    }
}

@Composable
private fun StepPhysicalRelationship(vm: ProfileWizardViewModel) {
    val s by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
        SectionHeader("Personal details", "These fields improve filters while keeping the profile structure consistent across communities.")
        OutlinedTextField(if (s.heightCm > 0) s.heightCm.toString() else "", { vm.update(s.copy(heightCm = it.toIntOrNull() ?: 0)) }, label = { Text("Height (cm) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
        if (s.weight > 0f) s.weight.toString() else "",
        { vm.update(s.copy(weight = it.toFloatOrNull() ?: 0f)) },
        label = { Text("Weight (kg) *") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
        ChoiceField(
        "Skin tone (optional, self-described)",
        s.complexion,
        IndiaProfileCatalog.complexionOptions,
        { vm.update(s.copy(complexion = it)) }
    )
        ChoiceField("Physical status", s.physicalStatus, IndiaProfileCatalog.physicalStatuses, { vm.update(s.copy(physicalStatus = it)) })
        ChoiceField("Marital status", s.maritalStatus, IndiaProfileCatalog.maritalStatuses, { vm.update(s.copy(maritalStatus = it)) }, required = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = s.hasChildren, onCheckedChange = { vm.update(s.copy(hasChildren = it)) })
            Text("I have children")
        }
    }
}

@Composable
private fun StepFamily(vm: ProfileWizardViewModel) {
    val s by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
        SectionHeader("Family background", "Optional details can help families understand one another before connecting.")
        ChoiceField("Native state", s.nativeState, IndiaProfileCatalog.statesAndUnionTerritories, { vm.update(s.copy(nativeState = it)) })
        ChoiceField("Family type", s.familyType, IndiaProfileCatalog.familyTypes, { vm.update(s.copy(familyType = it)) })
        ChoiceField("Family status", s.familyStatus, IndiaProfileCatalog.familyStatuses, { vm.update(s.copy(familyStatus = it)) })
        ChoiceField("Family values", s.familyValues, IndiaProfileCatalog.familyValues, { vm.update(s.copy(familyValues = it)) })
        OutlinedTextField(s.fatherOccupation, { vm.update(s.copy(fatherOccupation = it.take(100))) }, label = { Text("Father's occupation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(s.motherOccupation, { vm.update(s.copy(motherOccupation = it.take(100))) }, label = { Text("Mother's occupation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(if (s.siblings > 0) s.siblings.toString() else "", { vm.update(s.copy(siblings = it.toIntOrNull()?.coerceIn(0, 20) ?: 0)) }, label = { Text("Number of siblings") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(s.aboutFamily, { vm.update(s.copy(aboutFamily = it.take(1000))) }, label = { Text("About family") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StepLifestyle(vm: ProfileWizardViewModel) {
    val s by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
        SectionHeader("Lifestyle & interests", "Add only what you are comfortable sharing.")
        ChoiceField("Diet", s.diet, IndiaProfileCatalog.diets, { vm.update(s.copy(diet = it)) })
        ChoiceField("Smoking", s.smoking, IndiaProfileCatalog.habitOptions, { vm.update(s.copy(smoking = it)) })
        ChoiceField("Drinking", s.drinking, IndiaProfileCatalog.habitOptions, { vm.update(s.copy(drinking = it)) })
        OutlinedTextField(s.hobbies, { vm.update(s.copy(hobbies = it.take(300))) }, label = { Text("Hobbies (comma-separated)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(s.spokenLanguages, { vm.update(s.copy(spokenLanguages = it.take(300))) }, label = { Text("Languages spoken (comma-separated)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(s.personalityType, { vm.update(s.copy(personalityType = it.take(80))) }, label = { Text("Personality / self-description") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(s.fitnessActivities, { vm.update(s.copy(fitnessActivities = it.take(300))) }, label = { Text("Fitness / activities") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StepResidence(vm: ProfileWizardViewModel) {
    val s by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
        SectionHeader("Residence & NRI", "Works for members in India and overseas without creating a separate app experience.")
        ChoiceField("Country of residence", s.countryOfResidence, IndiaProfileCatalog.countriesCommon, { vm.update(s.copy(countryOfResidence = it)) }, required = true)
        OutlinedTextField(s.citizenship, { vm.update(s.copy(citizenship = it.take(80))) }, label = { Text("Citizenship") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ChoiceField("Residential / visa category", s.residentialStatus, IndiaProfileCatalog.residentialStatuses, { vm.update(s.copy(residentialStatus = it, visaStatus = it)) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = s.willingToRelocate, onCheckedChange = { vm.update(s.copy(willingToRelocate = it)) })
            Spacer(Modifier.width(MatreeDesign.spacing.sm))
            Column {
                Text("Open to relocation", fontWeight = FontWeight.Medium)
                Text("Show this preference to improve matching.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StepAstrology(vm: ProfileWizardViewModel) {
    val s by vm.wizardState.collectAsState()
    val isHindu = ReligionCategory.fromReligion(s.religion) == ReligionCategory.HINDU
    Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)) {
        if (isHindu) {
            SectionHeader("Astrology / Kundali", "Optional. Used only where relevant to your selected profile and privacy settings.")
            OutlinedTextField(s.rasi, { vm.update(s.copy(rasi = it.take(80))) }, label = { Text("Rasi / moon sign") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.nakshatra, { vm.update(s.copy(nakshatra = it.take(80))) }, label = { Text("Nakshatra / birth star") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.manglik, { vm.update(s.copy(manglik = it.take(40))) }, label = { Text("Manglik (if applicable)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        } else {
            SectionHeader("Birth details", "Optional. Religion-specific Kundali fields are not shown when they do not apply.")
        }
        OutlinedTextField(s.birthTime, { vm.update(s.copy(birthTime = it.take(5))) }, label = { Text("Birth time (HH:MM, 24h)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(s.birthPlace, { vm.update(s.copy(birthPlace = it.take(100))) }, label = { Text("Birth place") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        MatreeInlineNotice(
            message = "Exact birth details stay private. Compatibility features use only data allowed by your privacy choices.",
            icon = Icons.Filled.PrivacyTip
        )
    }
}
