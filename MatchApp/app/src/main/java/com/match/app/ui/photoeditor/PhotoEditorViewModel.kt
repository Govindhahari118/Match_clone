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
    val saved: Boolean = false,
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
        _ui.update { it.copy(sourceUri = uri, saved = false, error = null) }
    }

    fun saveToProfile() = viewModelScope.launch {
        val uri = _ui.value.sourceUri ?: return@launch
        val userId = session.userId.firstOrNull() ?: return@launch
        
        _ui.update { it.copy(loading = true, error = null) }
        
        val result = photoRepo.import(userId, uri)
        
        if (result.isSuccess) {
            _ui.update { it.copy(loading = false, saved = true) }
        } else {
            _ui.update { it.copy(loading = false, error = "Failed to save photo. Please try again.") }
        }
    }

    fun resetSaved() {
        _ui.update { it.copy(saved = false) }
    }
}
