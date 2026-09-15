package com.match.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.core.analytics.AnalyticsManager
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.AuthResult
import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
    val resetEmailSent: Boolean = false
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun signIn(email: String, password: String) = viewModelScope.launch {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Enter your email and password.") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null) }
        try {
            val result = auth.signIn(email, password)
            _state.update {
                when (result) {
                    is AuthResult.Success -> {
                        analytics.logSignIn("email")
                        it.copy(loading = false, done = true)
                    }
                    is AuthResult.Error -> it.copy(loading = false, error = result.message)
                }
            }
        } catch (error: Exception) {
            _state.update { it.copy(loading = false, error = error.message ?: "Sign in failed") }
        }
    }

    fun signInWithGoogle(googleIdToken: String, displayName: String, email: String) =
        viewModelScope.launch {
            if (googleIdToken.isBlank()) {
                _state.update { it.copy(error = "Google sign-in could not be verified.") }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null) }
            val result = auth.signInWithGoogle(googleIdToken, displayName, email)
            _state.update {
                when (result) {
                    is AuthResult.Success -> {
                        analytics.logSignIn("google")
                        it.copy(loading = false, done = true)
                    }
                    is AuthResult.Error -> it.copy(loading = false, error = "Google sign-in failed: ${result.message}")
                }
            }
        }

    fun clearError() = _state.update { it.copy(error = null) }

    fun sendPasswordReset(email: String) = viewModelScope.launch {
        if (email.isBlank()) {
            _state.update { it.copy(error = "Enter your email address first.") }
            return@launch
        }
        _state.update { it.copy(loading = true, error = null) }
        val err = auth.sendPasswordReset(email)
        _state.update { it.copy(loading = false, error = err, resetEmailSent = err == null) }
    }

    fun clearResetSent() = _state.update { it.copy(resetEmailSent = false) }
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /**
     * Account creation intentionally collects only credentials and relationship basics. State,
     * language, religion, community, education, career, family, lifestyle and optional astrology
     * belong to the required all-India profile wizard immediately after authentication.
     */
    fun signUp(
        email: String,
        password: String,
        displayName: String,
        age: Int,
        gender: Gender,
        lookingFor: LookingFor
    ) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val result = auth.signUp(
                email = email,
                password = password,
                displayName = displayName,
                age = age,
                gender = gender,
                lookingFor = lookingFor,
                city = "",
                bio = "",
                rasi = "",
                nakshatra = "",
                phone = ""
            )
            _state.update {
                when (result) {
                    is AuthResult.Success -> {
                        analytics.logSignUp("email")
                        it.copy(loading = false, done = true)
                    }
                    is AuthResult.Error -> it.copy(loading = false, error = result.message)
                }
            }
        } catch (error: Exception) {
            _state.update { it.copy(loading = false, error = error.message ?: "Sign up failed") }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
