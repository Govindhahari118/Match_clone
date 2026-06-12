package com.match.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import com.match.app.ui.i18n.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(onBack: () -> Unit, vm: SignUpViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var ageTxt by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf(Gender.MALE) }
    var lookingFor by rememberSaveable { mutableStateOf(LookingFor.FEMALE) }
    var city by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var rasi by rememberSaveable { mutableStateOf(vm.rasis.first()) }
    var nakshatra by rememberSaveable { mutableStateOf(vm.nakshatras.first()) }
    var subCaste by rememberSaveable { mutableStateOf("") }
    var gothra by rememberSaveable { mutableStateOf("") }
    var incomeBand by rememberSaveable { mutableStateOf("") }
    var diet by rememberSaveable { mutableStateOf("Veg") }
    var familyType by rememberSaveable { mutableStateOf("Nuclear") }
    var profileFor by rememberSaveable { mutableStateOf("Myself") }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it); vm.clearError() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("account_created", "Create account")) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            // ── Profile For (BharatMatrimony pattern) ────────────────
            Text(t("create_profile_for", "Creating profile for"), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Myself", "Son", "Daughter", "Brother", "Sister", "Relative", "Friend").forEach { opt ->
                    FilterChip(
                        selected = profileFor == opt,
                        onClick = { profileFor = opt },
                        label = { Text(opt, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("signup_for_$opt")
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("signup_email"))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(10) },
                label = { Text(t("mobile_number", "Mobile number")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                leadingIcon = { Text("+91", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier.fillMaxWidth().testTag("signup_phone"))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(password, { password = it }, label = { Text(t("password", "Password (min 8 chars)")) },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("signup_password"))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(name, { name = it }, label = { Text(t("display_name", "Display name")) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_name"))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(ageTxt, { ageTxt = it.filter(Char::isDigit).take(3) },
                label = { Text(t("age", "Age (18+)")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = ageTxt.isNotEmpty() && (ageTxt.toIntOrNull() ?: 0) !in 18..99,
                supportingText = if (ageTxt.isNotEmpty() && (ageTxt.toIntOrNull() ?: 0) !in 18..99) {
                    { Text("Age must be between 18 and 99") }
                } else null,
                modifier = Modifier.fillMaxWidth().testTag("signup_age"))
            Spacer(Modifier.height(10.dp))
            Text(t("i_am", "I am"), style = MaterialTheme.typography.labelLarge)
            SegmentedRow(
                options = Gender.values().map { it.name },
                selected = gender.name,
                onSelect = { gender = Gender.valueOf(it) }
            )
            Spacer(Modifier.height(10.dp))
            Text(t("looking_for", "Looking for"), style = MaterialTheme.typography.labelLarge)
            SegmentedRow(
                options = LookingFor.values().map { it.name },
                selected = lookingFor.name,
                onSelect = { lookingFor = LookingFor.valueOf(it) }
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(city, { city = it }, label = { Text(t("city", "City")) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_city"))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(bio, { bio = it }, label = { Text(t("bio", "Short bio")) },
                minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth().testTag("signup_bio"))
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(subCaste, { subCaste = it }, label = { Text("Sub-caste (optional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("signup_subcaste"))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(gothra, { gothra = it }, label = { Text("Gothra (optional)") },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("signup_gothra"))
            Spacer(Modifier.height(10.dp))
            Text(t("income_band", "Income band"), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("3-6L","6-10L","10-15L","15-25L","25-50L","50L+").forEach { band ->
                    FilterChip(selected = incomeBand == band, onClick = { incomeBand = band },
                        label = { Text(band, style = MaterialTheme.typography.labelSmall) })
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(t("diet_preference", "Diet preference"), style = MaterialTheme.typography.labelLarge)
            SegmentedRow(options = listOf("Veg","Non-Veg","Eggetarian","Jain"), selected = diet, onSelect = { diet = it })
            Spacer(Modifier.height(10.dp))
            Text(t("family_type", "Family type"), style = MaterialTheme.typography.labelLarge)
            SegmentedRow(options = listOf("Nuclear","Joint","Extended"), selected = familyType, onSelect = { familyType = it })
            Spacer(Modifier.height(14.dp))
            DropdownPicker(t("rasi", "Rasi (Moon sign)"), vm.rasis, rasi) { rasi = it }
            Spacer(Modifier.height(10.dp))
            DropdownPicker(t("nakshatra", "Nakshatra"), vm.nakshatras, nakshatra) { nakshatra = it }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    val age = ageTxt.toIntOrNull() ?: 0
                    if (age !in 18..99) return@Button
                    vm.signUp(email, password, name, age,
                        gender, lookingFor, city, bio, rasi, nakshatra, phone)
                },
                enabled = !state.loading && (ageTxt.toIntOrNull() ?: 0) in 18..99,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("signup_submit")
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(t("account_created", "Create account"))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SegmentedRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(opt) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownPicker(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(o); expanded = false })
            }
        }
    }
}
