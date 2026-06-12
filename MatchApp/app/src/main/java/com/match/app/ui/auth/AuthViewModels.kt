package com.match.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.core.analytics.AnalyticsManager
import com.match.app.core.matching.Astrology
import com.match.app.data.remote.GoogleAuthRequest
import com.match.app.data.remote.MatchApiProvider
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.AuthResult
import com.match.app.data.seed.SeedProvider
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
    private val seed: SeedProvider,
    private val analytics: AnalyticsManager,
    private val apiProvider: MatchApiProvider
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init { viewModelScope.launch { seed.seedIfNeeded() } }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val r = auth.signIn(email, password)
            _state.update {
                when (r) {
                    is AuthResult.Success -> { analytics.logSignIn("email"); it.copy(loading = false, done = true) }
                    is AuthResult.Error   -> it.copy(loading = false, error = r.message)
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.message ?: "Sign in failed") }
        }
    }

    /**
     * Called after Google One-Tap / Credential Manager returns a valid idToken.
     * The backend should verify this token and return/create a matching user account.
     * For now we use the email claim extracted from the token to sign in or auto-register.
     */
    fun signInWithGoogle(googleIdToken: String, displayName: String, email: String) =
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val r = auth.signInWithGoogle(googleIdToken, displayName, email)
            _state.update {
                when (r) {
                    is AuthResult.Success -> { analytics.logSignIn("google"); it.copy(loading = false, done = true) }
                    is AuthResult.Error   -> it.copy(loading = false, error = "Google sign-in failed: ${r.message}")
                }
            }
        }

    fun clearError() = _state.update { it.copy(error = null) }

    fun demoSignIn() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        seed.seedIfNeeded() // Ensure demo users exist
        val r = auth.signIn("demo1@match.app", "Password@123")
        _state.update {
            when (r) {
                is AuthResult.Success -> { analytics.logSignIn("demo"); it.copy(loading = false, done = true) }
                is AuthResult.Error   -> it.copy(loading = false, error = "Demo login failed: ${r.message}")
            }
        }
    }

    fun sendPasswordReset(email: String) = viewModelScope.launch {
        if (email.isBlank()) { _state.update { it.copy(error = "Enter your email address first.") }; return@launch }
        _state.update { it.copy(loading = true, error = null) }
        val err = auth.sendPasswordReset(email)
        _state.update { it.copy(loading = false, error = err, resetEmailSent = err == null) }
    }

    fun clearResetSent() = _state.update { it.copy(resetEmailSent = false) }
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val seed: SeedProvider,
    private val analytics: AnalyticsManager
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val rasis = Astrology.RASIS
    val nakshatras = Astrology.NAKSHATRAS

    init { viewModelScope.launch { seed.seedIfNeeded() } }

    fun signUp(
        email: String, password: String, displayName: String,
        age: Int, gender: Gender, lookingFor: LookingFor,
        city: String, bio: String, rasi: String, nakshatra: String,
        phone: String = ""
    ) = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val r = auth.signUp(email, password, displayName, age, gender, lookingFor, city, bio, rasi, nakshatra, phone)
            _state.update {
                when (r) {
                    is AuthResult.Success -> { analytics.logSignUp("email"); it.copy(loading = false, done = true) }
                    is AuthResult.Error   -> it.copy(loading = false, error = r.message)
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.message ?: "Sign up failed") }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
