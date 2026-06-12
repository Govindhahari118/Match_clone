package com.match.app.ui.counselling

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

data class CounsellingUiState(
    val loading: Boolean = false,
    val booked: Boolean = false,
    val bookings: List<Map<String, Any?>> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CounsellingViewModel @Inject constructor(
    private val session: SessionStore,
    private val featureService: FirestoreFeatureService
) : ViewModel() {

    private val _ui = MutableStateFlow(CounsellingUiState())
    val ui: StateFlow<CounsellingUiState> = _ui.asStateFlow()

    init {
        loadBookings()
    }

    private fun loadBookings() = viewModelScope.launch {
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        try {
            val history = featureService.getCounsellingBookings(uid)
            _ui.update { it.copy(bookings = history) }
        } catch (e: Exception) {
            // ignore for now
        }
    }

    fun bookSession(counsellor: String, sessionType: String, mode: String) = viewModelScope.launch {
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        _ui.update { it.copy(loading = true, error = null) }
        
        try {
            featureService.bookCounselling(
                uid = uid,
                counsellorName = counsellor,
                sessionType = sessionType,
                mode = mode,
                date = "TBD",
                time = "TBD"
            )
            _ui.update { it.copy(loading = false, booked = true) }
            loadBookings()
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, error = "Failed to book session. Please try again.") }
        }
    }

    fun resetBooking() = _ui.update { it.copy(booked = false) }
}
