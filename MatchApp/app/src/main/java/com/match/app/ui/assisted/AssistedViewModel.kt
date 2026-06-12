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
        val request = featureService.getRMRequest(uid)
        if (request != null) {
            _ui.update { it.copy(
                leadName = request["name"] as? String ?: "",
                leadPhone = request["phone"] as? String ?: "",
                selectedPlan = request["plan"] as? String ?: "Gold RM",
                submitted = true
            )}
        }
    }

    fun onNameChange(name: String) = _ui.update { it.copy(leadName = name) }
    fun onPhoneChange(phone: String) = _ui.update { it.copy(leadPhone = phone.filter { it.isDigit() }.take(10)) }
    fun onPreferenceChange(pref: String) = _ui.update { it.copy(leadPreference = pref) }
    fun onPlanSelect(plan: String) = _ui.update { it.copy(selectedPlan = plan) }

    fun submitRequest() = viewModelScope.launch {
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        _ui.update { it.copy(loading = true, error = null) }
        
        try {
            featureService.requestRM(
                uid = uid,
                plan = _ui.value.selectedPlan,
                preferences = "Name: ${_ui.value.leadName}, Phone: ${_ui.value.leadPhone}, Prefs: ${_ui.value.leadPreference}"
            )
            _ui.update { it.copy(loading = false, submitted = true) }
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = "Failed to submit request. Please try again.") }
        }
    }

    fun resetSubmission() = _ui.update { it.copy(submitted = false) }
}
