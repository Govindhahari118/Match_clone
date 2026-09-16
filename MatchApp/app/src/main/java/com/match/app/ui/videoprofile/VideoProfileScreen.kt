package com.match.app.ui.videoprofile

import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.match.app.ui.common.rememberSecureMediaUri
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoProfileViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao,
    private val storageService: FirebaseStorageService,
    private val profileService: FirestoreProfileService,
) : ViewModel() {

    private val _videoUri = MutableStateFlow("")
    val videoUri = _videoUri.asStateFlow()

    private val _published = MutableStateFlow(false)
    val published = _published.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved = _saved.asStateFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading = _uploading.asStateFlow()

    private val _removing = MutableStateFlow(false)
    val removing = _removing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        viewModelScope.launch {
            val localUserId = session.userId.first() ?: return@launch
            val user = userDao.findById(localUserId) ?: return@launch
            _videoUri.value = user.videoUrl
            _published.value = isRemoteReference(user.videoUrl)
        }
    }

    fun setVideoUri(uri: Uri) {
        _videoUri.value = uri.toString()
        _published.value = false
        _saved.value = false
        _error.value = null
    }

    fun saveVideo() = viewModelScope.launch {
        val localUserId = session.userId.first() ?: return@launch
        val user = userDao.findById(localUserId) ?: return@launch
        val firebaseUid = user.firebaseUid
        val localUri = _videoUri.value
        if (firebaseUid.isBlank() || localUri.isBlank() || _uploading.value) return@launch

        _uploading.value = true
        _error.value = null
        try {
            val remoteRef = storageService.uploadProfileVideo(firebaseUid, Uri.parse(localUri)).getOrThrow()
            profileService.updateFields(firebaseUid, mapOf("videoUrl" to remoteRef))
            userDao.update(user.copy(videoUrl = remoteRef))
            _videoUri.value = remoteRef
            _published.value = true
            _saved.value = true
        } catch (e: Exception) {
            _saved.value = false
            _published.value = false
            _error.value = e.message ?: "Video upload failed. Please try again."
        } finally {
            _uploading.value = false
        }
    }

    fun removePublishedVideo() = viewModelScope.launch {
        if (!_published.value || _removing.value) return@launch
        val localUserId = session.userId.first() ?: return@launch
        val user = userDao.findById(localUserId) ?: return@launch
        val firebaseUid = user.firebaseUid
        val remoteRef = _videoUri.value
        if (firebaseUid.isBlank() || remoteRef.isBlank()) return@launch

        _removing.value = true
        _error.value = null
        try {
            storageService.deleteProtectedMedia(remoteRef).getOrThrow()
            profileService.updateFields(firebaseUid, mapOf("videoUrl" to ""))
            userDao.update(user.copy(videoUrl = ""))
            _videoUri.value = ""
            _published.value = false
            _saved.value = false
        } catch (e: Exception) {
            _error.value = e.message ?: "Video could not be removed. Please try again."
        } finally {
            _removing.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun isRemoteReference(value: String): Boolean =
        value.startsWith("gs://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("http://", ignoreCase = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoProfileScreen(
    onBack: () -> Unit = {},
    vm: VideoProfileViewModel = hiltViewModel()
) {
    val videoUri by vm.videoUri.collectAsState()
    val published by vm.published.collectAsState()
    val saved by vm.saved.collectAsState()
    val uploading by vm.uploading.collectAsState()
    val removing by vm.removing.collectAsState()
    val error by vm.error.collectAsState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.setVideoUri(it) } }

    val playableUri = rememberSecureMediaUri(videoUri)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("video_profile", "Video Profile")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Videocam, null, Modifier.size(48.dp), tint = Color(0xFF7B1FA2))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Express Yourself with Video",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF4A148C)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add a short introduction so families can understand your personality, lifestyle, and values beyond photos.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF6A1B9A)
                    )
                }
            }

            if (videoUri.isNotBlank()) {
                if (playableUri != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val exoPlayer = remember(playableUri) {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(playableUri))
                            prepare()
                        }
                    }
                    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                        )
                    }
                } else if (videoUri.startsWith("gs://")) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (published) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (published) Icons.Filled.CheckCircle else Icons.Filled.Preview,
                            null,
                            Modifier.size(32.dp),
                            tint = if (published) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (published) "Video profile active" else "Video selected for preview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (published) "This video is saved to your profile." else "Review it, then save to publish it to your profile.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (published) {
                            IconButton(onClick = vm::removePublishedVideo, enabled = !removing) {
                                if (removing) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Delete, "Remove video", tint = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }

            if (saved) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text("Video saved to your profile.", color = Color(0xFF1B5E20), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            error?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = vm::clearError) { Text("Dismiss") }
                    }
                }
            }

            Button(
                onClick = { videoPickerLauncher.launch("video/*") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !uploading && !removing
            ) {
                Icon(Icons.Filled.Upload, null)
                Spacer(Modifier.width(8.dp))
                Text(t("upload_from_gallery", "Choose Video from Gallery"))
            }

            if (videoUri.isNotBlank() && !published) {
                Button(
                    onClick = vm::saveVideo,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    enabled = !uploading
                ) {
                    if (uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Uploading…")
                    } else {
                        Icon(Icons.Filled.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text(t("save_to_profile", "Save to Profile"))
                    }
                }
            }

            Text("Tips for a great video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            VideoTip(Icons.Filled.Person, "Be Yourself", "Smile, be confident, and let your personality shine through.")
            VideoTip(Icons.Filled.EditNote, "Prepare", "Think about what you want to say — hobbies, values, what you're looking for.")
            VideoTip(Icons.Filled.LightMode, "Good Lighting", "Choose a well-lit area with a tidy background.")
            VideoTip(Icons.Filled.Timer, "Keep it Short", "A concise introduction is easier for families to review.")
            VideoTip(Icons.Filled.HighQuality, "Quality Matters", "Choose a clear video where your face and voice are easy to understand.")

            Text("What to include in your video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IncludeItem("✅", "Your name and a brief introduction")
                    IncludeItem("✅", "What you do (profession/interests)")
                    IncludeItem("✅", "What you're looking for in a partner")
                    IncludeItem("✅", "Your hobbies and lifestyle")
                    IncludeItem("✅", "What makes you unique")
                    HorizontalDivider()
                    IncludeItem("❌", "Personal contact details")
                    IncludeItem("❌", "Financial information")
                    IncludeItem("❌", "Negative comments about past relationships")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VideoTip(icon: ImageVector, title: String, desc: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun IncludeItem(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
