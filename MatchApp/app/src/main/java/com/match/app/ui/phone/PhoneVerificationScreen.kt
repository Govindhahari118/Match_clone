package com.match.app.ui.phone

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.match.app.data.local.dao.UserDao
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ── ViewModel ────────────────────────────────────────────────────────────────

sealed class PhoneVerifState {
    data object Idle : PhoneVerifState()
    data object SendingOtp : PhoneVerifState()
    data class OtpSent(val verificationId: String) : PhoneVerifState()
    data object Verifying : PhoneVerifState()
    data object Verified : PhoneVerifState()
    data class Error(val msg: String) : PhoneVerifState()
}

@HiltViewModel
class PhoneVerificationViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao
) : ViewModel() {

    private val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()

    private val _state = MutableStateFlow<PhoneVerifState>(PhoneVerifState.Idle)
    val state: StateFlow<PhoneVerifState> = _state

    fun sendOtp(phone: String, activity: android.app.Activity) {
        if (phone.length < 10) {
            _state.value = PhoneVerifState.Error("Enter a valid 10-digit phone number")
            return
        }
        _state.value = PhoneVerifState.SendingOtp
        val fullPhone = if (phone.startsWith("+")) phone else "+91$phone"

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-retrieval succeeded — link to existing account
                viewModelScope.launch {
                    try {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                            ?: error("Sign in to your existing account before verifying a phone number")
                        currentUser.linkWithCredential(credential).await()
                        confirmPhoneVerified()
                        _state.value = PhoneVerifState.Verified
                    } catch (e: Exception) {
                        _state.value = PhoneVerifState.Error(e.message ?: "Verification failed")
                    }
                }
            }
            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                _state.value = PhoneVerifState.Error(e.message ?: "Failed to send OTP")
            }
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _state.value = PhoneVerifState.OtpSent(verificationId)
            }
        }

        val opts = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(fullPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(opts)
    }

    fun verifyOtp(verificationId: String, otp: String) {
        if (otp.length != 6) {
            _state.value = PhoneVerifState.Error("Enter the 6-digit OTP")
            return
        }
        _state.value = PhoneVerifState.Verifying
        viewModelScope.launch {
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                val currentUser = FirebaseAuth.getInstance().currentUser
                    ?: error("Sign in to your existing account before verifying a phone number")
                currentUser.linkWithCredential(credential).await()
                confirmPhoneVerified()
                _state.value = PhoneVerifState.Verified
            } catch (e: Exception) {
                _state.value = PhoneVerifState.Error(e.message ?: "Invalid OTP")
            }
        }
    }

    private suspend fun confirmPhoneVerified() {
        val authUser = FirebaseAuth.getInstance().currentUser
            ?: error("Signed-in account is required")
        val result = functions.getHttpsCallable("confirmPhoneVerification")
            .call()
            .await()
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?> ?: emptyMap()
        val confirmedPhone = (payload["phoneNumber"] as? String)
            ?.takeIf { it.isNotBlank() }
            ?: authUser.phoneNumber.orEmpty()
        require(confirmedPhone.isNotBlank()) { "Phone verification could not be confirmed" }

        val userId = session.userId.first() ?: return
        val user = userDao.findById(userId) ?: return
        // A phone signal is not identity/KYC verification. Never elevate isVerified here.
        userDao.update(user.copy(phoneNumber = confirmedPhone))
    }

    fun reset() { _state.value = PhoneVerifState.Idle }
}

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneVerificationScreen(
    onBack: () -> Unit = {},
    onVerified: () -> Unit = {},
    vm: PhoneVerificationViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val activity = LocalActivityCompat()

    var phone by remember { mutableStateOf("") }
    var otp   by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is PhoneVerifState.Verified) onVerified()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("verify_phone", "Verify Phone Number")) },
                navigationIcon = {
                    IconButton(onClick = onBack, Modifier.testTag("phone_verif_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Phone, null, Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            Text(
                if (state is PhoneVerifState.OtpSent || state is PhoneVerifState.Verifying)
                    "Enter the OTP" else "Verify your phone number",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            when (val s = state) {
                is PhoneVerifState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(s.msg, Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is PhoneVerifState.Verified -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Verified, null, Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Phone verified!", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Your phone number is verified. Identity verification is separate.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    return@Column
                }
                else -> {}
            }

            if (state !is PhoneVerifState.OtpSent && state !is PhoneVerifState.Verifying) {
                // Phone entry step
                Text(
                    "We'll send a 6-digit OTP via SMS to verify your phone number.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (it.length <= 15) phone = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Phone number") },
                    placeholder = { Text("+91 9876543210") },
                    leadingIcon = { Text("+91", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("phone_verif_input")
                )
                Button(
                    onClick = { if (activity != null) vm.sendOtp(phone, activity) },
                    enabled = phone.length >= 10 && state !is PhoneVerifState.SendingOtp,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("phone_verif_send_btn")
                ) {
                    if (state is PhoneVerifState.SendingOtp)
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Send OTP")
                }
            } else {
                // OTP entry step
                val verificationId = (state as? PhoneVerifState.OtpSent)?.verificationId ?: ""
                Text(
                    "OTP sent to +91 $phone. Enter the 6-digit code below.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it.filter { c -> c.isDigit() } },
                    label = { Text("6-digit OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("phone_verif_otp_input")
                )
                Button(
                    onClick = { vm.verifyOtp(verificationId, otp) },
                    enabled = otp.length == 6 && state !is PhoneVerifState.Verifying,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("phone_verif_verify_btn")
                ) {
                    if (state is PhoneVerifState.Verifying)
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Verify OTP")
                }
                TextButton(onClick = { vm.reset() }) { Text("Change phone number") }
            }
        }
    }
}

// Helper to get Activity from Composable context
@Composable
private fun LocalActivityCompat(): android.app.Activity? {
    val context = androidx.compose.ui.platform.LocalContext.current
    return context as? android.app.Activity
}
