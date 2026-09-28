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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
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
import com.match.app.ui.common.resolveSecureMediaModel
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
        viewModelScope.launch {
            val localId = session.userId.first()
            val user = localId?.let { userDao.findById(it) }
            _ui.value = if (user == null) {
                VideoProfileUiState(loading = false, error = "Profile could not be loaded.")
            } else {
                VideoProfileUiState(
                    loading = false,
                    storedReference = user.videoUrl
                )
            }
        }
    }

    fun selectVideo(uri: Uri) {
        _ui.update {
            it.copy(
                selectedUri = uri.toString(),
                saved = false,
                error = null
            )
        }
    }

    fun cancelSelection() {
        _ui.update { it.copy(selectedUri = "", saved = false, error = null) }
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

        val previousReference = _ui.value.storedReference
        _ui.update { it.copy(uploading = true, saved = false, error = null) }

        runCatching {
            val newReference = storageService.uploadProfileVideo(firebaseUid, Uri.parse(source)).getOrThrow()

            // Publish only after the new object is completely uploaded. This preserves the old
            // working video if the replacement upload fails.
            userDao.update(user.copy(videoUrl = newReference))
            profileService.updateFields(firebaseUid, mapOf("videoUrl" to newReference))

            // Cleanup is deliberately after publication and best-effort. A cleanup failure cannot
            // turn a successfully published replacement into fake failure.
            if (previousReference.isProtectedRemoteReference() && previousReference != newReference) {
                runCatching { storageService.deleteProtectedMedia(previousReference).getOrThrow() }
            }
            newReference
        }.onSuccess { newReference ->
            _ui.update {
                it.copy(
                    storedReference = newReference,
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
        if (_ui.value.deleting) return@launch
        val reference = _ui.value.storedReference

        _ui.update { it.copy(deleting = true, error = null) }
        runCatching {
            if (reference.isProtectedRemoteReference()) {
                storageService.deleteProtectedMedia(reference).getOrThrow()
            }
            userDao.update(user.copy(videoUrl = ""))
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

    private fun String.isProtectedRemoteReference(): Boolean =
        startsWith("gs://", ignoreCase = true) ||
            startsWith("https://firebasestorage.googleapis.com", ignoreCase = true) ||
            startsWith("https://storage.googleapis.com", ignoreCase = true) ||
            startsWith("videos/", ignoreCase = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoProfileScreen(
    onBack: () -> Unit = {},
    vm: VideoProfileViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    var pendingCaptureUri by rememberSaveable { mutableStateOf<String?>(null) }
    var playbackRetry by remember { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(vm::selectVideo)
    }

    val recorder = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        val captured = pendingCaptureUri
        if (success && !captured.isNullOrBlank()) {
            vm.selectVideo(Uri.parse(captured))
        }
        if (!success) {
            captured?.let { runCatching { File(Uri.parse(it).path.orEmpty()).delete() } }
        }
        pendingCaptureUri = null
    }

    fun launchRecorder() {
        val captureFile = File.createTempFile("profile_video_", ".mp4", context.cacheDir)
        val captureUri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            captureFile
        )
        pendingCaptureUri = captureUri.toString()
        recorder.launch(captureUri)
    }

    val playbackSource = ui.selectedUri.ifBlank { ui.storedReference }
    var resolvedPlaybackUri by remember(playbackSource, playbackRetry) { mutableStateOf<Uri?>(null) }
    var resolvingPlayback by remember(playbackSource, playbackRetry) {
        mutableStateOf(playbackSource.isNotBlank())
    }

    LaunchedEffect(playbackSource, playbackRetry) {
        if (playbackSource.isBlank()) {
            resolvedPlaybackUri = null
            resolvingPlayback = false
        } else {
            resolvingPlayback = true
            val model = resolveSecureMediaModel(context.applicationContext, playbackSource)
            resolvedPlaybackUri = when (model) {
                is Uri -> model
                is File -> Uri.fromFile(model)
                is String -> Uri.parse(model)
                else -> null
            }
            resolvingPlayback = false
        }
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("video_profile_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Videocam,
                        null,
                        Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
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

            when {
                ui.loading || resolvingPlayback -> {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                resolvedPlaybackUri != null -> VideoPreview(resolvedPlaybackUri!!)
                playbackSource.isNotBlank() -> {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Video unavailable",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "The file may be unavailable or your current access may no longer allow it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            TextButton(onClick = { playbackRetry += 1 }) { Text("Retry") }
                        }
                    }
                }
                else -> {
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
                            Icon(
                                Icons.Filled.VideoLibrary,
                                null,
                                Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("No profile video yet", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Choose a video or record one when you are ready.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
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
                        Icon(
                            Icons.Filled.ErrorOutline,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            error,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
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
                        Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("video_profile_choose")
            ) {
                Icon(Icons.Filled.VideoLibrary, null)
                Spacer(Modifier.width(8.dp))
                Text(if (playbackSource.isBlank()) "Choose video" else "Choose a different video")
            }

            OutlinedButton(
                onClick = ::launchRecorder,
                enabled = !ui.uploading && !ui.deleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("video_profile_record")
            ) {
                Icon(Icons.Filled.FiberManualRecord, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Record new video")
            }

            if (ui.selectedUri.isNotBlank()) {
                Button(
                    onClick = vm::saveVideo,
                    enabled = !ui.uploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("video_profile_save")
                ) {
                    if (ui.uploading) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("video_profile_remove")
                ) {
                    Icon(Icons.Filled.DeleteOutline, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (ui.deleting) "Removing…" else "Remove profile video")
                }
            }

            Text(
                "Video guidance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            GuidanceRow(
                Icons.Filled.Timer,
                "Keep it concise",
                "Use a short introduction focused on who you are and what matters to you."
            )
            GuidanceRow(
                Icons.Filled.LightMode,
                "Use clear lighting",
                "Choose a well-lit, quiet place where your face and voice are easy to understand."
            )
            GuidanceRow(
                Icons.Filled.Security,
                "Protect private information",
                "Do not include phone numbers, addresses, financial details or other sensitive information."
            )
            GuidanceRow(
                Icons.Filled.Person,
                "Be authentic",
                "Talk naturally about your values, interests and what you hope to find in a partner."
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPreview(playbackUri: Uri) {
    val context = LocalContext.current
    val player = remember(playbackUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playbackUri))
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
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
private fun GuidanceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String
) {
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
                Icon(
                    icon,
                    null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
