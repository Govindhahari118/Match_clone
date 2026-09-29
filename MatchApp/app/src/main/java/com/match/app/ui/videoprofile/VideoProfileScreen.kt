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
import com.google.firebase.functions.FirebaseFunctions
import com.match.app.data.local.dao.UserDao
import com.match.app.data.remote.FirebaseStorageService
import com.match.app.data.repo.ConsentRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.common.resolveSecureMediaModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

data class VideoProfileUiState(
    val loading: Boolean = true,
    val storedReference: String = "",
    val pendingReference: String = "",
    val selectedUri: String = "",
    val reviewStatus: String = "NONE",
    val uploading: Boolean = false,
    val deleting: Boolean = false,
    val submittedForReview: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class VideoProfileViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao,
    private val storageService: FirebaseStorageService,
    private val consentRepository: ConsentRepository
) : ViewModel() {

    private val functions = FirebaseFunctions.getInstance()
    private val _ui = MutableStateFlow(VideoProfileUiState())
    val ui = _ui.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() = viewModelScope.launch {
        val localId = session.userId.first()
        val user = localId?.let { userDao.findById(it) }
        if (user == null || user.firebaseUid.isBlank()) {
            _ui.value = VideoProfileUiState(
                loading = false,
                error = "Profile could not be loaded."
            )
            return@launch
        }

        runCatching {
            val result = functions.getHttpsCallable("getMyProfileVideoState").call().await()
            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?> ?: emptyMap()
            Triple(
                data["publishedPath"] as? String ?: user.videoUrl,
                data["pendingPath"] as? String ?: "",
                data["status"] as? String ?: "NONE"
            )
        }.onSuccess { (published, pending, status) ->
            if (published != user.videoUrl) {
                userDao.update(user.copy(videoUrl = published))
            }
            _ui.value = VideoProfileUiState(
                loading = false,
                storedReference = published,
                pendingReference = pending,
                reviewStatus = status
            )
        }.onFailure { error ->
            _ui.value = VideoProfileUiState(
                loading = false,
                storedReference = user.videoUrl,
                error = error.message?.take(180) ?: "Could not load profile video status."
            )
        }
    }

    fun selectVideo(uri: Uri) {
        _ui.update {
            it.copy(
                selectedUri = uri.toString(),
                submittedForReview = false,
                error = null
            )
        }
    }

    fun cancelSelection() {
        _ui.update { it.copy(selectedUri = "", submittedForReview = false, error = null) }
    }

    fun submitVideo() = viewModelScope.launch {
        val localId = session.userId.first() ?: return@launch
        val user = userDao.findById(localId) ?: return@launch
        val firebaseUid = user.firebaseUid.takeIf { it.isNotBlank() } ?: run {
            _ui.update { it.copy(error = "Profile is not linked to your signed-in account.") }
            return@launch
        }
        val source = _ui.value.selectedUri.takeIf { it.isNotBlank() } ?: return@launch
        if (_ui.value.uploading) return@launch

        _ui.update { it.copy(uploading = true, submittedForReview = false, error = null) }
        var uploadedReference = ""
        runCatching {
            consentRepository.set("media_processing", true)
            uploadedReference = storageService
                .uploadProfileVideo(firebaseUid, Uri.parse(source))
                .getOrThrow()
            val result = functions.getHttpsCallable("submitProfileVideo")
                .call(mapOf("storagePath" to uploadedReference))
                .await()
            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?> ?: error("Invalid video submission response")
            check((data["status"] as? String) == "PENDING") {
                "Video was not accepted for review"
            }
            uploadedReference
        }.onSuccess { pending ->
            _ui.update {
                it.copy(
                    pendingReference = pending,
                    selectedUri = "",
                    reviewStatus = "PENDING",
                    uploading = false,
                    submittedForReview = true,
                    error = null
                )
            }
        }.onFailure { error ->
            if (uploadedReference.isNotBlank()) {
                runCatching {
                    storageService.deleteProtectedMedia(uploadedReference).getOrThrow()
                }
            }
            _ui.update {
                it.copy(
                    uploading = false,
                    submittedForReview = false,
                    error = error.message?.take(180)
                        ?: "Video submission failed. Please try again."
                )
            }
        }
    }

    fun removeVideo() = viewModelScope.launch {
        val localId = session.userId.first() ?: return@launch
        val user = userDao.findById(localId) ?: return@launch
        if (_ui.value.deleting) return@launch

        _ui.update { it.copy(deleting = true, error = null) }
        runCatching {
            functions.getHttpsCallable("removeProfileVideo").call().await()
            userDao.update(user.copy(videoUrl = ""))
        }.onSuccess {
            _ui.value = VideoProfileUiState(loading = false, reviewStatus = "REMOVED")
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    deleting = false,
                    error = error.message?.take(180) ?: "Could not remove the profile video."
                )
            }
        }
    }

    fun clearSubmittedMessage() =
        _ui.update { it.copy(submittedForReview = false) }
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

    val playbackSource = ui.selectedUri.ifBlank {
        ui.pendingReference.ifBlank { ui.storedReference }
    }
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
                        "A short optional video can help another member understand your personality and communication style. Uploading records the current Media processing notice, and the video stays owner-only until an authorized review publishes it. Matree does not promise a response or visibility multiplier.",
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

            if (ui.submittedForReview || ui.reviewStatus == "PENDING") {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Schedule,
                            null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Video submitted for review. Your currently approved video stays published until this review is approved.",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        IconButton(onClick = vm::clearSubmittedMessage) {
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
                    onClick = vm::submitVideo,
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
                    Text(if (ui.uploading) "Submitting…" else "Upload for review")
                }
                TextButton(
                    onClick = vm::cancelSelection,
                    enabled = !ui.uploading,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cancel selection")
                }
            } else if (ui.storedReference.isNotBlank() || ui.pendingReference.isNotBlank()) {
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
