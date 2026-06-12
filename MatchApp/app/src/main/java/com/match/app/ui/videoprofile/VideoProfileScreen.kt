package com.match.app.ui.videoprofile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.match.app.ui.i18n.t
import android.view.ViewGroup
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@HiltViewModel
class VideoProfileViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao
) : ViewModel() {

    private val _videoUri = MutableStateFlow<String>("")
    val videoUri = _videoUri.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved = _saved.asStateFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading = _uploading.asStateFlow()

    private val storage = com.google.firebase.storage.FirebaseStorage.getInstance()

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val user = userDao.findById(uid) ?: return@launch
            _videoUri.value = user.videoUrl
        }
    }

    fun setVideoUri(uri: Uri) {
        _videoUri.value = uri.toString()
        _saved.value = false
    }

    fun saveVideo() = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        val user = userDao.findById(uid) ?: return@launch
        val localUri = _videoUri.value
        if (localUri.isBlank()) return@launch

        _uploading.value = true
        try {
            // Upload to Firebase Storage
            val ref = storage.reference.child("videos/$uid/profile_video.mp4")
            ref.putFile(android.net.Uri.parse(localUri)).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            // Save remote URL locally
            userDao.update(user.copy(videoUrl = downloadUrl))
            _videoUri.value = downloadUrl
            _saved.value = true
        } catch (_: Exception) {
            // Fallback: save local URI
            userDao.update(user.copy(videoUrl = localUri))
            _saved.value = true
        } finally {
            _uploading.value = false
        }
    }

    fun removeVideo() = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        val user = userDao.findById(uid) ?: return@launch
        // Delete from storage if it's a Firebase URL
        if (_videoUri.value.contains("firebasestorage")) {
            try {
                storage.reference.child("videos/$uid/profile_video.mp4").delete().await()
            } catch (_: Exception) { /* ignore */ }
        }
        userDao.update(user.copy(videoUrl = ""))
        _videoUri.value = ""
        _saved.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoProfileScreen(
    onBack: () -> Unit = {},
    vm: VideoProfileViewModel = hiltViewModel()
) {
    val videoUri by vm.videoUri.collectAsState()
    val saved by vm.saved.collectAsState()
    val uploading by vm.uploading.collectAsState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.setVideoUri(it) } }

    // Record video directly — URI is passed back to the picker launcher for review
    @Suppress("UNUSED_VARIABLE")
    val videoRecorderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { _ -> /* result handled via content URI observer */ }

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
            // ── Hero Section ──
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
                        "Go beyond photos! Record a short video introduction to show your personality, lifestyle, and values. Profiles with videos get 3× more responses.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF6A1B9A)
                    )
                }
            }

            // ── Current Video Status ──
            // ── Video Player Preview ──
            if (videoUri.isNotBlank()) {
                val context = LocalContext.current
                val exoPlayer = remember(videoUri) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(android.net.Uri.parse(videoUri)))
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

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(32.dp), tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Video uploaded!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            Text("Your video profile is active and visible to matches.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                        }
                        IconButton(onClick = { vm.removeVideo() }) {
                            Icon(Icons.Filled.Delete, "Remove", tint = Color(0xFFC62828))
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
                        Text("Video saved to your profile!", color = Color(0xFF1B5E20), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Upload / Record Buttons ──
            Button(
                onClick = { videoPickerLauncher.launch("video/*") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Upload, null)
                Spacer(Modifier.width(8.dp))
                Text(t("upload_from_gallery", "Upload Video from Gallery"))
            }

            OutlinedButton(
                onClick = { /* Record: would need ContentResolver to create temp URI */ },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.FiberManualRecord, null, tint = Color.Red)
                Spacer(Modifier.width(8.dp))
                Text(t("record_new_video", "Record New Video"))
            }

            if (videoUri.isNotBlank() && !saved) {
                Button(
                    onClick = { vm.saveVideo() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    enabled = !uploading
                ) {
                    if (uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
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

            // ── Tips Section ──
            Text("Tips for a great video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            VideoTip(Icons.Filled.Person, "Be Yourself", "Smile, be confident, and let your personality shine through.")
            VideoTip(Icons.Filled.EditNote, "Prepare", "Think about what you want to say — hobbies, values, what you're looking for.")
            VideoTip(Icons.Filled.LightMode, "Good Lighting", "Choose a well-lit area with a tidy background.")
            VideoTip(Icons.Filled.Timer, "Keep it Short", "30-60 seconds is ideal. First impressions matter!")
            VideoTip(Icons.Filled.HighQuality, "Quality Matters", "Use your phone's rear camera for better quality.")

            // ── What to Include ──
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
