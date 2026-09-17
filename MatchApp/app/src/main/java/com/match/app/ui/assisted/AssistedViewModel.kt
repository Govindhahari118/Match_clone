package com.match.app.ui.assisted

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.remote.FirestoreFeatureService
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistedUiState(
    val loading: Boolean = false,
    val leadName: String = "",
    val leadPhone: String = "",
    val leadPreference: String = "",
    val selectedPlan: String = "Gold RM",
    val submitted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AssistedViewModel @Inject constructor(
    private val session: SessionStore,
    private val featureService: FirestoreFeatureService
) : ViewModel() {

    private val _ui = MutableStateFlow(AssistedUiState())
    val ui: StateFlow<AssistedUiState> = _ui.asStateFlow()

    init {
        loadLeadInfo()
    }

    private fun loadLeadInfo() = viewModelScope.launch {
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        runCatching { featureService.getRMRequest(uid) }
            .onSuccess { request ->
                if (request != null) {
                    _ui.update {
                        it.copy(
                            leadName = request["name"] as? String ?: "",
                            leadPhone = request["phone"] as? String ?: "",
                            leadPreference = request["preferences"] as? String ?: "",
                            selectedPlan = request["plan"] as? String ?: "Gold RM",
                            submitted = true,
                            error = null
                        )
                    }
                }
            }
            .onFailure {
                _ui.update { state ->
                    state.copy(error = "Unable to load your assisted-service request.")
                }
            }
    }

    fun onNameChange(name: String) = _ui.update {
        it.copy(leadName = name.take(100), error = null)
    }

    fun onPhoneChange(phone: String) = _ui.update {
        it.copy(leadPhone = phone.filter(Char::isDigit).take(10), error = null)
    }

    fun onPreferenceChange(preference: String) = _ui.update {
        it.copy(leadPreference = preference.take(2000), error = null)
    }

    fun onPlanSelect(plan: String) = _ui.update {
        it.copy(selectedPlan = plan, error = null)
    }

    fun submitRequest() = viewModelScope.launch {
        val uid = session.firebaseUid.firstOrNull()
        if (uid.isNullOrBlank()) {
            _ui.update { it.copy(error = "Sign in before requesting assisted matchmaking.") }
            return@launch
        }
        val state = _ui.value
        if (state.leadName.trim().length < 2) {
            _ui.update { it.copy(error = "Enter your full name.") }
            return@launch
        }
        if (state.leadPhone.length != 10) {
            _ui.update { it.copy(error = "Enter a valid 10-digit mobile number.") }
            return@launch
        }

        _ui.update { it.copy(loading = true, error = null) }
        runCatching {
            featureService.requestRM(
                uid = uid,
                plan = state.selectedPlan,
                preferences = state.leadPreference.trim(),
                name = state.leadName.trim(),
                phone = state.leadPhone
            )
        }.onSuccess {
            _ui.update { it.copy(loading = false, submitted = true) }
        }.onFailure {
            _ui.update {
                it.copy(
                    loading = false,
                    error = "Unable to submit the request. Check your connection and try again."
                )
            }
        }
    }
}
