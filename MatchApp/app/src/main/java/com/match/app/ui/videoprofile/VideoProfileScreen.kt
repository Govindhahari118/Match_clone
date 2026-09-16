package com.match.app.ui.videoprofile

import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.match.app.data.local.dao.UserDao
import com.match.app.data.remote.FirebaseStorageService
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VideoProfileUiState(
    val loading: Boolean = true,
    val storedReference: String = "",
    val playbackUri: String = "",
    val selectedUri: String = "",
    val uploading: Boolean = false,
    val deleting: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class VideoProfileViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao,
    private val storageService: FirebaseStorageService,
    private val profileService: FirestoreProfileService
) : ViewModel() {

    private val _ui = MutableStateFlow(VideoProfileUiState())
    val ui = _ui.asStateFlow()

    init {
        loadExisting()
    }

    private fun loadExisting() = viewModelScope.launch {
        val localId = session.userId.first()
        val user = localId?.let { userDao.findById(it) }
        if (user == null) {
            _ui.value = VideoProfileUiState(loading = false, error = "Profile could not be loaded.")
            return@launch
        }
        val reference = user.videoUrl
        if (reference.isBlank()) {
            _ui.value = VideoProfileUiState(loading = false)
            return@launch
        }
        val playback = resolvePlayback(reference)
        _ui.value = VideoProfileUiState(
            loading = false,
            storedReference = reference,
            playbackUri = playback.getOrDefault(""),
            error = playback.exceptionOrNull()?.message?.take(180)
        )
    }

    fun selectVideo(uri: Uri) {
        _ui.update {
            it.copy(
                selectedUri = uri.toString(),
                playbackUri = uri.toString(),
                saved = false,
                error = null
            )
        }
    }

    fun cancelSelection() = viewModelScope.launch {
        val reference = _ui.value.storedReference
        if (reference.isBlank()) {
            _ui.update { it.copy(selectedUri = "", playbackUri = "", error = null) }
        } else {
            val playback = resolvePlayback(reference)
            _ui.update {
                it.copy(
                    selectedUri = "",
                    playbackUri = playback.getOrDefault(""),
                    error = playback.exceptionOrNull()?.message?.take(180)
                )
            }
        }
    }

    fun retryPlayback() = viewModelScope.launch {
        val reference = _ui.value.storedReference
        if (reference.isBlank()) return@launch
        _ui.update { it.copy(loading = true, error = null) }
        val playback = resolvePlayback(reference)
        _ui.update {
            it.copy(
                loading = false,
                playbackUri = playback.getOrDefault(""),
                error = playback.exceptionOrNull()?.message?.take(180)
            )
        }
    }

    fun saveVideo() = viewModelScope.launch {
        val localId = session.userId.first() ?: return@launch
        val user = userDao.findById(localId) ?: return@launch
        val firebaseUid = user.firebaseUid.takeIf { it.isNotBlank() } ?: run {
            _ui.update { it.copy(error = "Profile is not linked to your signed-in account.") }
            return@launch
        }
        val source = _ui.value.selectedUri.takeIf { it.isNotBlank() } ?: return@launch
        if (_ui.value.uploading) return@launch

        _ui.update { it.copy(uploading = true, saved = false, error = null) }
        runCatching {
            val storagePath = storageService.uploadProfileVideo(firebaseUid, Uri.parse(source)).getOrThrow()
            val updated = user.copy(videoUrl = storagePath)
            userDao.update(updated)
            profileService.updateFields(firebaseUid, mapOf("videoUrl" to storagePath))
            storagePath
        }.onSuccess { storagePath ->
            _ui.update {
                it.copy(
                    storedReference = storagePath,
                    selectedUri = "",
                    uploading = false,
                    saved = true,
                    error = null
                )
            }
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    uploading = false,
                    saved = false,
                    error = error.message?.take(180) ?: "Video upload failed. Please try again."
                )
            }
        }
    }

    fun removeVideo() = viewModelScope.launch {
        val localId = session.userId.first() ?: return@launch
        val user = userDao.findById(localId) ?: return@launch
        val reference = _ui.value.storedReference
        if (_ui.value.deleting) return@launch

        _ui.update { it.copy(deleting = true, error = null) }
        runCatching {
            if (reference.startsWith("videos/")) {
                storageService.deleteProfileVideo(reference).getOrThrow()
            }
            val updated = user.copy(videoUrl = "")
            userDao.update(updated)
            if (user.firebaseUid.isNotBlank()) {
                profileService.updateFields(user.firebaseUid, mapOf("videoUrl" to ""))
            }
        }.onSuccess {
            _ui.value = VideoProfileUiState(loading = false)
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    deleting = false,
                    error = error.message?.take(180) ?: "Could not remove the video."
                )
            }
        }
    }

    fun clearSavedMessage() = _ui.update { it.copy(saved = false) }

    private suspend fun resolvePlayback(reference: String): Result<String> = runCatching {
        when {
            reference.startsWith("videos/") -> {
                val path = storageService.downloadProfileVideo(reference).getOrThrow()
                Uri.fromFile(File(path)).toString()
            }
            reference.startsWith("content://") || reference.startsWith("file://") ||
                reference.startsWith("https://") || reference.startsWith("http://") -> reference
            File(reference).exists() -> Uri.fromFile(File(reference)).toString()
            else -> error("Stored video is unavailable. Upload it again to repair your profile video.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoProfileScreen(
    onBack: () -> Unit = {},
    vm: VideoProfileViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(vm::selectVideo)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("video_profile_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
                .testTag("video_profile_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Videocam, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Introduce yourself in your own words",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "A short optional video can help another member understand your personality and communication style. Matree does not promise a response or visibility multiplier.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (ui.loading) {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (ui.playbackUri.isNotBlank()) {
                VideoPreview(ui.playbackUri)
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.VideoLibrary, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No profile video yet", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Choose a video from your device when you are ready.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            ui.error?.let { error ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            error,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (ui.storedReference.isNotBlank() && ui.selectedUri.isBlank()) {
                            TextButton(onClick = vm::retryPlayback) { Text("Retry") }
                        }
                    }
                }
            }

            if (ui.saved) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Video uploaded and saved to your profile.",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        IconButton(onClick = vm::clearSavedMessage) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            Button(
                onClick = { picker.launch("video/*") },
                enabled = !ui.uploading && !ui.deleting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("video_profile_choose")
            ) {
                Icon(Icons.Filled.VideoLibrary, null)
                Spacer(Modifier.width(8.dp))
                Text(if (ui.playbackUri.isBlank()) "Choose video" else "Choose a different video")
            }

            if (ui.selectedUri.isNotBlank()) {
                Button(
                    onClick = vm::saveVideo,
                    enabled = !ui.uploading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("video_profile_save")
                ) {
                    if (ui.uploading) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Filled.CloudUpload, null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (ui.uploading) "Uploading…" else "Upload & save")
                }
                TextButton(
                    onClick = vm::cancelSelection,
                    enabled = !ui.uploading,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel selection")
                }
            } else if (ui.storedReference.isNotBlank()) {
                OutlinedButton(
                    onClick = vm::removeVideo,
                    enabled = !ui.deleting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("video_profile_remove")
                ) {
                    Icon(Icons.Filled.DeleteOutline, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (ui.deleting) "Removing…" else "Remove profile video")
                }
            }

            Text("Video guidance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            GuidanceRow(Icons.Filled.Timer, "Keep it concise", "Aim for a short introduction focused on who you are and what matters to you.")
            GuidanceRow(Icons.Filled.LightMode, "Use clear lighting", "Choose a well-lit, quiet place where your face and voice are easy to understand.")
            GuidanceRow(Icons.Filled.Security, "Protect private information", "Do not include phone numbers, addresses, financial details or other sensitive information.")
            GuidanceRow(Icons.Filled.Person, "Be authentic", "Talk naturally about your values, interests and what you hope to find in a partner.")

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Direct camera recording is intentionally not shown until Matree has a complete capture URI, permission and recovery flow. Gallery upload is the supported production path on this screen.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPreview(playbackUri: String) {
    val context = LocalContext.current
    val player = remember(playbackUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(playbackUri)))
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().height(260.dp)
        )
    }
}

@Composable
private fun GuidanceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
