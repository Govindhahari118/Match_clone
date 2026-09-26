package com.match.app.ui.auth

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalResources
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
import androidx.credentials.exceptions.NoCredentialException
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
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    // google-services.xml generates this resource. Use LocalResources so configuration changes
    // invalidate the composable and lint can verify that we do not retain stale resource values.
    val googleWebClientId = remember(resources, context.packageName) {
        val resourceId = resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resourceId == 0) "" else runCatching { resources.getString(resourceId) }.getOrDefault("").trim()
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }
    LaunchedEffect(state.resetEmailSent) {
        if (state.resetEmailSent) {
            snackbar.showSnackbar("Password reset email sent. Check your inbox.")
            vm.clearResetSent()
            showForgotDialog = false
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag("signin_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFFE91E63), Color(0xFF880E4F))))
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Favorite, null, Modifier.size(40.dp), tint = Color.White)
                    }
                    Text("Matree", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(
                        t("ai_picks_for_you", "Find someone who truly fits."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.85f)
                    )
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim().take(254) },
                    label = { Text(t("email", "Email")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("signin_email")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it.take(128) },
                    label = { Text(t("password", "Password")) },
                    singleLine = true,
                    visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    trailingIcon = {
                        IconButton(onClick = { showPw = !showPw }) {
                            Icon(if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (showPw) "Hide password" else "Show password")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("signin_password")
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { vm.signIn(email, password) },
                    enabled = !state.loading && email.isNotBlank() && password.isNotBlank(),
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

                if (googleWebClientId.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f))
                        Text("  ${t("or", "OR")}  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setServerClientId(googleWebClientId)
                                        .setFilterByAuthorizedAccounts(false)
                                        .setAutoSelectEnabled(false)
                                        .build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()
                                    val result = credentialManager.getCredential(context, request)
                                    val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                    vm.signInWithGoogle(
                                        googleIdToken = credential.idToken,
                                        displayName = credential.displayName ?: "",
                                        email = credential.id
                                    )
                                } catch (error: NoCredentialException) {
                                    Log.i("SignIn", "No Google credential available", error)
                                    snackbar.showSnackbar("No Google account is available. Try email/password or add a Google account.")
                                } catch (error: GetCredentialException) {
                                    Log.i("SignIn", "Google sign-in cancelled or unavailable", error)
                                } catch (error: Exception) {
                                    Log.w("SignIn", "Google sign-in failed", error)
                                    snackbar.showSnackbar("Google sign-in is unavailable right now. Try email/password.")
                                }
                            }
                        },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("signin_google"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(t("continue_with_google", "Continue with Google"), fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onGoSignUp, modifier = Modifier.fillMaxWidth().testTag("signin_go_signup")) {
                    Text(t("already_account", "New here? Create an account"))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text(t("reset_password", "Reset Password")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        t("reset_password_desc", "Enter your registered email and we'll send you a reset link."),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it.trim().take(254) },
                        label = { Text(t("email", "Email")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { vm.sendPasswordReset(forgotEmail) }, enabled = !state.loading && forgotEmail.isNotBlank()) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(t("send_reset", "Send Reset Email"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text(t("cancel", "Cancel")) }
            }
        )
    }
}
