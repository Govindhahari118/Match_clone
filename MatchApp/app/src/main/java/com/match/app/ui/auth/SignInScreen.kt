package com.match.app.ui.auth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.match.app.ui.i18n.t
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onGoSignUp: () -> Unit,
    vm: SignInViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPw by rememberSaveable { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.error) { state.error?.let { snackbar.showSnackbar(it); vm.clearError() } }
    LaunchedEffect(state.resetEmailSent) {
        if (state.resetEmailSent) {
            snackbar.showSnackbar("Password reset email sent. Check your inbox.")
            vm.clearResetSent()
            showForgotDialog = false
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .testTag("signin_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Gradient hero header ───────────────────────────────────────
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE91E63), Color(0xFF880E4F))
                    )
                ).padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Favorite, null, Modifier.size(40.dp), tint = Color.White)
                    }
                    Text("Match", style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(t("ai_picks_for_you", "Find someone who truly fits."),
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.85f))
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text(t("email", "Email")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("signin_email")
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text(t("password", "Password")) }, singleLine = true,
                visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { showPw = !showPw }) {
                        Icon(if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("signin_password")
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.signIn(email, password) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("signin_submit")
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(t("sign_in", "Sign in"), style = MaterialTheme.typography.titleMedium)
            }

            TextButton(
                onClick = { forgotEmail = email; showForgotDialog = true },
                modifier = Modifier.align(Alignment.End).testTag("signin_forgot")
            ) {
                Text(t("forgot_password", "Forgot password?"), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            // ── OR divider ────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(Modifier.weight(1f))
                Text(
                    "  ${t("or", "OR")}  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // ── Google Sign-In (Credential Manager) ───────────────────
            OutlinedButton(
                onClick = {
                    val webClientId = "REPLACE_WITH_YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
                    if (webClientId.startsWith("REPLACE_")) {
                        scope.launch {
                            snackbar.showSnackbar("Google Sign-In is not configured yet. Use email/password.")
                        }
                        return@OutlinedButton
                    }
                    scope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setServerClientId(webClientId)
                                .setFilterByAuthorizedAccounts(false)
                                .setAutoSelectEnabled(false)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val result = credentialManager.getCredential(context, request)
                            val credential = result.credential
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            vm.signInWithGoogle(
                                googleIdToken = googleIdTokenCredential.idToken,
                                displayName   = googleIdTokenCredential.displayName ?: "",
                                email         = googleIdTokenCredential.id
                            )
                        } catch (e: GetCredentialException) {
                            Log.w("SignIn", "Google sign-in cancelled or failed", e)
                        }
                    }
                },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("signin_google"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(t("continue_with_google", "Continue with Google"), fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onGoSignUp, modifier = Modifier.fillMaxWidth().testTag("signin_go_signup")) {
                Text(t("already_account", "New here? Create an account"))
            }
            Spacer(Modifier.height(24.dp))

            // ── Demo Login ────────────────────────────────────────────
            ElevatedCard(
                onClick = { vm.demoSignIn() },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Demo Login (One-Tap)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            } // end inner Column
        }
    }

    // ── Forgot Password dialog ────────────────────────────────────────────
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text(t("reset_password", "Reset Password")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("reset_password_desc", "Enter your registered email and we'll send you a reset link."),
                        style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text(t("email", "Email")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.sendPasswordReset(forgotEmail) },
                    enabled = !state.loading
                ) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(t("send_reset", "Send Reset Email"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text(t("cancel", "Cancel"))
                }
            }
        )
    }
}
