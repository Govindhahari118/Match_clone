package com.match.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
    var ageTxt by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf(Gender.MALE) }
    var lookingFor by rememberSaveable { mutableStateOf(LookingFor.FEMALE) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("account_created", "Create account")) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Create your secure account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "After this, you’ll complete one profile flow for any state, language, religion or community. We don’t force astrology or community fields during account creation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "You must be 18 or older. State, mother tongue, religion, community, education, career and other matching details are collected next and can be changed later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim().take(254) },
                label = { Text("Email *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("signup_email")
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.take(128) },
                label = { Text(t("password", "Password (min 8 characters) *")) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                supportingText = { Text("Use a password unique to this account.") },
                modifier = Modifier.fillMaxWidth().testTag("signup_password")
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("Full name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("signup_name")
            )
            OutlinedTextField(
                value = ageTxt,
                onValueChange = { ageTxt = it.filter(Char::isDigit).take(2) },
                label = { Text(t("age", "Age *")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                isError = ageTxt.isNotEmpty() && (ageTxt.toIntOrNull() ?: 0) !in 18..99,
                supportingText = {
                    Text(if (ageTxt.isNotEmpty() && (ageTxt.toIntOrNull() ?: 0) !in 18..99) "Age must be 18–99" else "Your date of birth is entered and validated in the profile step.")
                },
                modifier = Modifier.fillMaxWidth().testTag("signup_age")
            )

            Text(t("i_am", "I am"), style = MaterialTheme.typography.labelLarge)
            SegmentedRow(
                options = Gender.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                selected = gender.name.lowercase().replaceFirstChar(Char::uppercase),
                onSelect = { label -> gender = Gender.valueOf(label.uppercase()) }
            )

            Text(t("looking_for", "Looking for"), style = MaterialTheme.typography.labelLarge)
            SegmentedRow(
                options = LookingFor.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                selected = lookingFor.name.lowercase().replaceFirstChar(Char::uppercase),
                onSelect = { label -> lookingFor = LookingFor.valueOf(label.uppercase()) }
            )

            Spacer(Modifier.height(8.dp))
            val age = ageTxt.toIntOrNull() ?: 0
            val ready = email.contains('@') && password.length >= 8 && name.trim().length >= 2 && age in 18..99
            Button(
                onClick = { vm.signUp(email, password, name.trim(), age, gender, lookingFor) },
                enabled = !state.loading && ready,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("signup_submit")
            ) {
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create account & continue")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.ArrowForward, null, Modifier.size(18.dp))
                }
            }
            Text(
                "Creating an account signs you in. The required profile wizard opens immediately afterward before Discover or messaging is available.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SegmentedRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
