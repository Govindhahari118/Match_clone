package com.match.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.ui.i18n.t

/**
 * 7-step Profile Wizard — captures all Sprint-10 fields after sign-up.
 * Each step saves immediately so no data is lost on back-press.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileWizardScreen(
    onComplete: () -> Unit = {},
    vm: ProfileWizardViewModel = hiltViewModel()
) {
    val step by vm.currentStep.collectAsState()
    val saving by vm.saving.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${t("step", "Step")} ${step + 1} / 7") },
                navigationIcon = {
                    if (step > 0) {
                        IconButton(onClick = { vm.previousStep() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { (step + 1) / 7f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF8B1A1A)
            )

            // Step content
            Box(
                Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                AnimatedContent(targetState = step, label = "wizard_step") { currentStep ->
                    when (currentStep) {
                        0 -> StepBasicInfo(vm)
                        1 -> StepCommunity(vm)
                        2 -> StepEducationCareer(vm)
                        3 -> StepPhysical(vm)
                        4 -> StepFamily(vm)
                        5 -> StepLifestyle(vm)
                        6 -> StepAstrology(vm)
                    }
                }
            }

            // Navigation buttons
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step < 6) {
                    OutlinedButton(
                        onClick = { vm.skipStep() },
                        modifier = Modifier.weight(1f)
                    ) { Text(t("skip", "Skip for now")) }

                    Button(
                        onClick = { vm.nextStep() },
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1A1A))
                    ) {
                        Text(t("next", "Next"))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = { vm.finish(onComplete) },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1A1A))
                    ) {
                        Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t("complete_profile", "Complete Profile"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Step Composables ─────────────────────────────────────────────────────────

@Composable
private fun StepBasicInfo(vm: ProfileWizardViewModel) {
    val state by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Basic Information", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Tell us about yourself", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.displayName, onValueChange = { vm.update(state.copy(displayName = it)) },
            label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth().testTag("wiz_name"), singleLine = true)
        OutlinedTextField(value = state.dateOfBirth, onValueChange = { vm.update(state.copy(dateOfBirth = it)) },
            label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth().testTag("wiz_dob"), singleLine = true)
        OutlinedTextField(value = state.city, onValueChange = { vm.update(state.copy(city = it)) },
            label = { Text("City *") }, modifier = Modifier.fillMaxWidth().testTag("wiz_city"), singleLine = true)
        OutlinedTextField(value = state.bio, onValueChange = { vm.update(state.copy(bio = it)) },
            label = { Text("About Me (2-3 lines)") }, modifier = Modifier.fillMaxWidth().testTag("wiz_bio"),
            minLines = 3, maxLines = 5)
    }
}

@Composable
private fun StepCommunity(vm: ProfileWizardViewModel) {
    val state by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Community & Background", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Helps find culturally compatible matches", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.religion, onValueChange = { vm.update(state.copy(religion = it)) },
            label = { Text("Religion *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.caste, onValueChange = { vm.update(state.copy(caste = it)) },
            label = { Text("Caste") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.subCaste, onValueChange = { vm.update(state.copy(subCaste = it)) },
            label = { Text("Sub-caste") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.motherTongue, onValueChange = { vm.update(state.copy(motherTongue = it)) },
            label = { Text("Mother Tongue *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.gothra, onValueChange = { vm.update(state.copy(gothra = it)) },
            label = { Text("Gothra") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
}

@Composable
private fun StepEducationCareer(vm: ProfileWizardViewModel) {
    val state by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Education & Career", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Your professional background", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.education, onValueChange = { vm.update(state.copy(education = it)) },
            label = { Text("Highest Education *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.educationField, onValueChange = { vm.update(state.copy(educationField = it)) },
            label = { Text("Field of Study") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.institution, onValueChange = { vm.update(state.copy(institution = it)) },
            label = { Text("Institution") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.profession, onValueChange = { vm.update(state.copy(profession = it)) },
            label = { Text("Occupation *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.employer, onValueChange = { vm.update(state.copy(employer = it)) },
            label = { Text("Employer / Company") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.incomeBand, onValueChange = { vm.update(state.copy(incomeBand = it)) },
            label = { Text("Annual Income Range") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
}

@Composable
private fun StepPhysical(vm: ProfileWizardViewModel) {
    val state by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Physical Attributes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Height, weight & complexion", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = if (state.heightCm > 0) state.heightCm.toString() else "",
            onValueChange = { vm.update(state.copy(heightCm = it.toIntOrNull() ?: 0)) },
            label = { Text("Height (cm) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = if (state.weight > 0f) state.weight.toString() else "",
            onValueChange = { vm.update(state.copy(weight = it.toFloatOrNull() ?: 0f)) },
            label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.complexion, onValueChange = { vm.update(state.copy(complexion = it)) },
            label = { Text("Complexion") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.maritalStatus, onValueChange = { vm.update(state.copy(maritalStatus = it)) },
            label = { Text("Marital Status *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
}

@Composable
private fun StepFamily(vm: ProfileWizardViewModel) {
    val state by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Family Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Family background information", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.familyType, onValueChange = { vm.update(state.copy(familyType = it)) },
            label = { Text("Family Type (Joint / Nuclear)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.familyStatus, onValueChange = { vm.update(state.copy(familyStatus = it)) },
            label = { Text("Family Status") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.fatherOccupation, onValueChange = { vm.update(state.copy(fatherOccupation = it)) },
            label = { Text("Father's Occupation") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.motherOccupation, onValueChange = { vm.update(state.copy(motherOccupation = it)) },
            label = { Text("Mother's Occupation") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = if (state.siblings > 0) state.siblings.toString() else "",
            onValueChange = { vm.update(state.copy(siblings = it.toIntOrNull() ?: 0)) },
            label = { Text("Number of Siblings") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.familyValues, onValueChange = { vm.update(state.copy(familyValues = it)) },
            label = { Text("Family Values (Orthodox / Moderate / Liberal)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
}

@Composable
private fun StepLifestyle(vm: ProfileWizardViewModel) {
    val state by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Lifestyle", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Diet, habits & interests", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.diet, onValueChange = { vm.update(state.copy(diet = it)) },
            label = { Text("Diet (Veg / Non-Veg / Eggetarian)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.smoking, onValueChange = { vm.update(state.copy(smoking = it)) },
            label = { Text("Smoking (Never / Occasionally)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.drinking, onValueChange = { vm.update(state.copy(drinking = it)) },
            label = { Text("Drinking (Never / Socially)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.fitnessActivities, onValueChange = { vm.update(state.copy(fitnessActivities = it)) },
            label = { Text("Fitness / Hobbies (comma-separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
}

@Composable
private fun StepAstrology(vm: ProfileWizardViewModel) {
    val state by vm.wizardState.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Astrology / Kundali", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("For 10-Porutham compatibility matching", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.rasi, onValueChange = { vm.update(state.copy(rasi = it)) },
            label = { Text("Rasi (Moon Sign) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.nakshatra, onValueChange = { vm.update(state.copy(nakshatra = it)) },
            label = { Text("Nakshatra (Birth Star) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.manglik, onValueChange = { vm.update(state.copy(manglik = it)) },
            label = { Text("Manglik (Yes / No / Partial)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.birthTime, onValueChange = { vm.update(state.copy(birthTime = it)) },
            label = { Text("Birth Time (HH:MM, 24h format)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = state.birthPlace, onValueChange = { vm.update(state.copy(birthPlace = it)) },
            label = { Text("Birth Place (city)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    }
}
