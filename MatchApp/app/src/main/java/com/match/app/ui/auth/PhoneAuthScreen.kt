package com.match.app.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.AuthResult
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.theme.MatreeDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class PhoneAuthUi(
    val loading: Boolean = false,
    val verificationId: String? = null,
    val error: String? = null,
    val done: Boolean = false
)

@HiltViewModel
class PhoneAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(PhoneAuthUi())
    val ui: StateFlow<PhoneAuthUi> = _ui.asStateFlow()

    fun sendOtp(rawPhone: String, activity: Activity) {
        val compact = rawPhone.filter { it.isDigit() || it == '+' }
        val fullPhone = when {
            compact.startsWith("+") -> compact
            compact.length == 10 -> "+91$compact"
            else -> compact
        }
        if (!fullPhone.matches(Regex("^\\+[1-9]\\d{7,14}$"))) {
            _ui.value = _ui.value.copy(error = "Enter a valid phone number including country code.")
            return
        }

        _ui.value = PhoneAuthUi(loading = true)
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                completeCredential(credential)
            }

            override fun onVerificationFailed(error: com.google.firebase.FirebaseException) {
                _ui.value = PhoneAuthUi(error = "OTP could not be sent. Please retry.")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                _ui.value = PhoneAuthUi(verificationId = verificationId)
            }
        }

        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(fullPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
        )
    }

    fun verifyOtp(code: String) {
        val verificationId = _ui.value.verificationId ?: return
        if (!code.matches(Regex("^\\d{6}$"))) {
            _ui.value = _ui.value.copy(error = "Enter the 6-digit OTP.")
            return
        }
        completeCredential(PhoneAuthProvider.getCredential(verificationId, code))
    }

    private fun completeCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            when (val result = authRepository.signInWithPhoneCredential(credential)) {
                is AuthResult.Success -> _ui.value = PhoneAuthUi(done = true)
                is AuthResult.Error -> _ui.value = _ui.value.copy(
                    loading = false,
                    error = result.message
                )
            }
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onBack: () -> Unit,
    vm: PhoneAuthViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbar = remember { SnackbarHostState() }
    var phone by rememberSaveable { mutableStateOf("+91") }
    var otp by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Continue with phone") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding)
                .fillMaxSize()
                .padding(MatreeDesign.spacing.lg)
                .testTag("phone_auth_screen"),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(MatreeDesign.spacing.md))
            Text(
                if (ui.verificationId == null) "Verify your mobile number" else "Enter your OTP",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(MatreeDesign.spacing.sm))
            Text(
                if (ui.verificationId == null)
                    "We use Firebase Phone Auth. Carrier/SMS charges may apply."
                else
                    "Enter the 6-digit code sent to your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(MatreeDesign.spacing.lg))

            if (ui.verificationId == null) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(16)
                    },
                    label = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("phone_auth_number")
                )
                Spacer(Modifier.height(MatreeDesign.spacing.md))
                MatreePrimaryButton(
                    text = if (ui.loading) "Sending OTP…" else "Send OTP",
                    onClick = { activity?.let { vm.sendOtp(phone, it) } },
                    enabled = !ui.loading && activity != null,
                    modifier = Modifier.fillMaxWidth().testTag("phone_auth_send")
                )
            } else {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                    label = { Text("6-digit OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("phone_auth_otp")
                )
                Spacer(Modifier.height(MatreeDesign.spacing.md))
                MatreePrimaryButton(
                    text = if (ui.loading) "Verifying…" else "Verify & continue",
                    onClick = { vm.verifyOtp(otp) },
                    enabled = !ui.loading && otp.length == 6,
                    modifier = Modifier.fillMaxWidth().testTag("phone_auth_verify")
                )
            }
        }
    }
}
