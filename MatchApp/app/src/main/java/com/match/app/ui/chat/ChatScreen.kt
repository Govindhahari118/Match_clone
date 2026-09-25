package com.match.app.ui.chat

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.match.app.core.analytics.AnalyticsManager
import com.match.app.data.local.entity.MessageEntity
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.ChatRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ChatUi(
    val peer: UserProfile? = null,
    val messages: List<MessageEntity> = emptyList(),
    val meId: Long = 0L,
    val isMutual: Boolean = false,
    val isBlocked: Boolean = false,
    val replyingTo: MessageEntity? = null,
    val loading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val chat: ChatRepository,
    private val social: SocialRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {
    private val peerIdFlow = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(ChatUi())
    val state: StateFlow<ChatUi> = _state.asStateFlow()
    private var boundPeerId: Long? = null

    fun bind(peerId: Long) {
        if (boundPeerId == peerId) return
        boundPeerId = peerId
        peerIdFlow.value = peerId
        analytics.logChatOpened(peerId)

        viewModelScope.launch {
            val me = session.userId.filterNotNull().first()
            val peer = auth.currentProfile(peerId)
            val mutual = social.isLiked(me, peerId) && social.isLiked(peerId, me)
            val blocked = social.isBlocked(me, peerId)
            _state.update { it.copy(peer = peer, meId = me, isMutual = mutual, isBlocked = blocked, loading = false) }
            if (mutual && !blocked) chat.markRead(me, peerId)
        }

        viewModelScope.launch {
            val me = session.userId.filterNotNull().first()
            chat.thread(me, peerId).collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }

        viewModelScope.launch {
            val me = session.userId.filterNotNull().first()
            val mutual = social.isLiked(me, peerId) && social.isLiked(peerId, me)
            val blocked = social.isBlocked(me, peerId)
            if (mutual && !blocked) chat.startFirestoreSync(me, peerId)
        }
    }

    fun send(body: String) = viewModelScope.launch {
        if (!_state.value.isMutual || _state.value.isBlocked) return@launch
        val me = _state.value.meId.takeIf { it > 0 } ?: return@launch
        val peer = peerIdFlow.value ?: return@launch
        val trimmed = body.trim()
        if (trimmed.isBlank()) return@launch
        chat.send(me, peer, trimmed, _state.value.replyingTo?.id)
        _state.update { it.copy(replyingTo = null, error = null) }
        analytics.logMessageSent()
    }

    fun sendImage(uri: String) = viewModelScope.launch {
        if (!_state.value.isMutual || _state.value.isBlocked) return@launch
        val peer = peerIdFlow.value ?: return@launch
        runCatching { chat.sendImage(_state.value.meId, peer, uri) }
            .onSuccess { analytics.logMessageSent() }
            .onFailure { e -> _state.update { it.copy(error = e.message ?: "Image could not be queued.") } }
    }

    fun sendVoice(path: String, durationMs: Long) = viewModelScope.launch {
        if (!_state.value.isMutual || _state.value.isBlocked) return@launch
        val peer = peerIdFlow.value ?: return@launch
        runCatching { chat.sendVoice(_state.value.meId, peer, path, durationMs) }
            .onSuccess { analytics.logVoiceMessageSent() }
            .onFailure { e -> _state.update { it.copy(error = e.message ?: "Voice message could not be queued.") } }
    }

    fun setReplyTo(message: MessageEntity?) = _state.update { it.copy(replyingTo = message) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun blockUser() = viewModelScope.launch {
        val peer = peerIdFlow.value ?: return@launch
        social.block(_state.value.meId, peer)
        _state.update { it.copy(isBlocked = true) }
    }

    fun unblockUser() = viewModelScope.launch {
        val peer = peerIdFlow.value ?: return@launch
        social.unblock(_state.value.meId, peer)
        val mutual = social.isLiked(_state.value.meId, peer) && social.isLiked(peer, _state.value.meId)
        _state.update { it.copy(isBlocked = false, isMutual = mutual) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerId: Long,
    onBack: () -> Unit,
    onUpgrade: () -> Unit = {},
    vm: ChatViewModel = hiltViewModel()
) {
    LaunchedEffect(peerId) { vm.bind(peerId) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var draft by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceFile by remember { mutableStateOf<File?>(null) }
    var recordStartedAt by remember { mutableLongStateOf(0L) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
        if (!granted) scope.launch { snackbar.showSnackbar("Microphone permission is required for voice messages.") }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.sendImage(it.toString()) }
    }

    fun stopRecorder(delete: Boolean, send: Boolean) {
        val duration = System.currentTimeMillis() - recordStartedAt
        isRecording = false
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        val file = voiceFile
        voiceFile = null
        if (send && duration >= 500 && file != null && file.exists() && file.length() > 0) {
            vm.sendVoice(file.absolutePath, duration)
        } else if (delete || duration < 500) {
            file?.delete()
        }
    }

    fun startRecording() {
        if (!hasMicPermission) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        runCatching {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            voiceFile = file
            recorder = mediaRecorder
            recordStartedAt = System.currentTimeMillis()
            isRecording = true
        }.onFailure {
            scope.launch { snackbar.showSnackbar("Could not start voice recording.") }
        }
    }

    LaunchedEffect(isRecording) {
        recordingSeconds = 0
        while (isRecording) {
            delay(1000)
            if (isRecording) recordingSeconds += 1
        }
    }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { recorder?.release() }
            recorder = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(state.peer?.displayName?.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(state.peer?.displayName ?: "Chat", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (state.isMutual) "Private match conversation" else "Chat unlocks after mutual interest",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, "More options") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (state.isBlocked) "Unblock member" else "Block member") },
                                leadingIcon = { Icon(if (state.isBlocked) Icons.Filled.LockOpen else Icons.Filled.Block, null) },
                                onClick = {
                                    showMenu = false
                                    if (state.isBlocked) vm.unblockUser() else showBlockDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            when {
                state.loading -> Unit
                state.isBlocked -> StatusBar("You blocked this member. Unblock them to continue the conversation.", MaterialTheme.colorScheme.errorContainer)
                !state.isMutual -> StatusBar("Messaging is available only after both members accept each other's interest.", MaterialTheme.colorScheme.secondaryContainer)
                isRecording -> {
                    Surface(tonalElevation = 2.dp) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { stopRecorder(delete = true, send = false) }) { Icon(Icons.Filled.Delete, "Cancel recording", tint = MaterialTheme.colorScheme.error) }
                            Text("Recording  %02d:%02d".format(recordingSeconds / 60, recordingSeconds % 60), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            FilledIconButton(onClick = { stopRecorder(delete = false, send = true) }) { Icon(Icons.Filled.Send, "Send voice") }
                        }
                    }
                }
                else -> {
                    Surface(tonalElevation = 2.dp) {
                        Column {
                            state.replyingTo?.let { reply ->
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Replying to ${if (reply.fromUserId == state.meId) "your message" else state.peer?.displayName.orEmpty()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            Text(reply.body.take(80), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                        IconButton(onClick = { vm.setReplyTo(null) }) { Icon(Icons.Filled.Close, "Cancel reply") }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { imagePicker.launch("image/*") }) { Icon(Icons.Filled.AddPhotoAlternate, "Send image") }
                                OutlinedTextField(
                                    value = draft,
                                    onValueChange = { draft = it.take(3000) },
                                    placeholder = { Text("Message") },
                                    modifier = Modifier.weight(1f).testTag("chat_input"),
                                    maxLines = 4
                                )
                                Spacer(Modifier.width(6.dp))
                                if (draft.isBlank()) {
                                    FilledIconButton(onClick = ::startRecording) { Icon(Icons.Filled.Mic, "Record voice") }
                                } else {
                                    FilledIconButton(onClick = {
                                        val text = draft
                                        draft = ""
                                        vm.send(text)
                                    }) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.peer == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { Text("Conversation unavailable.") }
            state.messages.isEmpty() -> {
                Column(
                    Modifier.padding(padding).fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Forum, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(if (state.isMutual) "Start your conversation" else "Mutual interest required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (state.isMutual) "Be respectful and avoid sharing sensitive information too early."
                        else "Both members must accept each other's interest before messages can be sent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(padding).fillMaxSize().testTag("chat_list"),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item("privacy_notice") {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text(
                                "Private match conversation. Local message copies are protected on this device; report or block any misuse.",
                                Modifier.padding(10.dp),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            mine = message.fromUserId == state.meId,
                            repliedMessage = message.replyToId?.let { id -> state.messages.firstOrNull { it.id == id } },
                            onLongPress = { vm.setReplyTo(message) }
                        )
                    }
                }
            }
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block ${state.peer?.displayName ?: "this member"}?") },
            text = { Text("Blocking stops new interactions between you and this member. You can unblock them later.") },
            confirmButton = {
                Button(
                    onClick = { showBlockDialog = false; vm.blockUser() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatusBar(text: String, color: Color) {
    Surface(color = color, modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageEntity,
    mine: Boolean,
    repliedMessage: MessageEntity?,
    onLongPress: () -> Unit
) {
    val background = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val time = remember(message.sentAt) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.sentAt)) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(16.dp)).background(background)
                .combinedClickable(onClick = onLongPress, onLongClick = onLongPress)
                .padding(10.dp)
        ) {
            repliedMessage?.let {
                Surface(shape = RoundedCornerShape(8.dp), color = foreground.copy(alpha = 0.12f)) {
                    Text(it.body.take(80), Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall, color = foreground.copy(alpha = 0.85f), maxLines = 1)
                }
                Spacer(Modifier.height(5.dp))
            }
            when {
                message.voiceUri != null -> VoiceMessage(message, foreground)
                message.imageUri != null -> AsyncImage(
                    model = message.imageUri,
                    contentDescription = "Image message",
                    modifier = Modifier.sizeIn(maxWidth = 240.dp, maxHeight = 320.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                else -> Text(message.body, color = foreground)
            }
            Spacer(Modifier.height(3.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(time, fontSize = 10.sp, color = foreground.copy(alpha = 0.65f))
                if (mine) {
                    Spacer(Modifier.width(4.dp))
                    val icon = when (message.status.lowercase()) {
                        "read" -> Icons.Filled.DoneAll
                        "failed" -> Icons.Filled.ErrorOutline
                        else -> Icons.Filled.Check
                    }
                    Icon(icon, message.status, Modifier.size(13.dp), tint = if (message.status.equals("failed", true)) MaterialTheme.colorScheme.error else foreground.copy(alpha = 0.75f))
                }
            }
        }
    }
}

@Composable
private fun VoiceMessage(message: MessageEntity, foreground: Color) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    val seconds = ((message.voiceDurationMs ?: 0L) / 1000).toInt()

    DisposableEffect(message.id) {
        onDispose {
            runCatching { player?.release() }
            player = null
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            if (playing) {
                player?.pause()
                playing = false
            } else {
                runCatching {
                    val mp = MediaPlayer().apply {
                        setDataSource(message.voiceUri!!)
                        prepare()
                        setOnCompletionListener { playing = false }
                        start()
                    }
                    player?.release()
                    player = mp
                    playing = true
                }.onFailure { playing = false }
            }
        }) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Play", tint = foreground) }
        Text(if (seconds > 0) "%02d:%02d".format(seconds / 60, seconds % 60) else "Voice message", color = foreground)
    }
}
