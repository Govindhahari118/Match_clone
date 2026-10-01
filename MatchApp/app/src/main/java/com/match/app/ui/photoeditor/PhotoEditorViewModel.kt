package com.match.app.ui.photoeditor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.ConsentRepository
import com.match.app.data.repo.PhotoRepository
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoEditorUiState(
    val loading: Boolean = false,
    val consentLoading: Boolean = true,
    val mediaConsentCurrent: Boolean = false,
    val mediaConsentVersion: String = "",
    val sourceUri: Uri? = null,
    val saved: Boolean = false,
    val moderationStatus: String? = null,
    val error: String? = null
)

@HiltViewModel
class PhotoEditorViewModel @Inject constructor(
    private val session: SessionStore,
    private val photoRepo: PhotoRepository,
    private val consentRepository: ConsentRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(PhotoEditorUiState())
    val ui = _ui.asStateFlow()

    init {
        refreshConsent()
    }

    fun refreshConsent() = viewModelScope.launch {
        _ui.update { it.copy(consentLoading = true) }
        runCatching {
            consentRepository.getState().firstOrNull { it.purpose == "media_processing" }
        }.onSuccess { state ->
            _ui.update {
                it.copy(
                    consentLoading = false,
                    mediaConsentCurrent = state?.isCurrent == true,
                    mediaConsentVersion = state?.noticeVersion.orEmpty()
                )
            }
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    consentLoading = false,
                    error = error.message ?: "Could not load media-processing consent."
                )
            }
        }
    }

    fun setMediaConsent(granted: Boolean) = viewModelScope.launch {
        _ui.update { it.copy(consentLoading = true, error = null) }
        runCatching {
            consentRepository.set("media_processing", granted)
            consentRepository.getState().firstOrNull { it.purpose == "media_processing" }
        }.onSuccess { state ->
            _ui.update {
                it.copy(
                    consentLoading = false,
                    mediaConsentCurrent = state?.isCurrent == true,
                    mediaConsentVersion = state?.noticeVersion.orEmpty()
                )
            }
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    consentLoading = false,
                    error = error.message ?: "Could not update media-processing consent."
                )
            }
        }
    }

    fun setSourceUri(uri: Uri) {
        _ui.update {
            it.copy(sourceUri = uri, saved = false, moderationStatus = null, error = null)
        }
    }

    fun saveToProfile() = viewModelScope.launch {
        val state = _ui.value
        if (!state.mediaConsentCurrent) {
            _ui.update {
                it.copy(error = "Review and enable media processing before uploading this photo.")
            }
            return@launch
        }
        val uri = state.sourceUri ?: return@launch
        val userId = session.userId.firstOrNull() ?: return@launch

        _ui.update { it.copy(loading = true, error = null, moderationStatus = null) }
        val result = photoRepo.import(userId, uri)

        if (result.isSuccess) {
            _ui.update {
                it.copy(
                    loading = false,
                    saved = true,
                    moderationStatus = "PENDING"
                )
            }
        } else {
            _ui.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message
                        ?: "Failed to submit photo for moderation. Please try again."
                )
            }
        }
    }

    fun resetSaved() {
        _ui.update { it.copy(saved = false, moderationStatus = null, error = null) }
    }
}
