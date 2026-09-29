package com.match.app.ui.photoeditor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val sourceUri: Uri? = null,
    val submittedForReview: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PhotoEditorViewModel @Inject constructor(
    private val session: SessionStore,
    private val photoRepo: PhotoRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(PhotoEditorUiState())
    val ui = _ui.asStateFlow()

    fun setSourceUri(uri: Uri) {
        _ui.update {
            it.copy(
                sourceUri = uri,
                submittedForReview = false,
                error = null
            )
        }
    }

    fun submitForReview() = viewModelScope.launch {
        val uri = _ui.value.sourceUri ?: return@launch
        val userId = session.userId.firstOrNull()
        if (userId == null) {
            _ui.update { it.copy(error = "Sign in before uploading a profile photo.") }
            return@launch
        }

        _ui.update { it.copy(loading = true, error = null) }
        photoRepo.import(userId, uri)
            .onSuccess {
                _ui.update {
                    it.copy(
                        loading = false,
                        submittedForReview = true,
                        error = null
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        loading = false,
                        submittedForReview = false,
                        error = error.message?.take(180)
                            ?: "Photo submission failed. Please try again."
                    )
                }
            }
    }

    fun chooseAnother() {
        _ui.update { PhotoEditorUiState() }
    }
}
