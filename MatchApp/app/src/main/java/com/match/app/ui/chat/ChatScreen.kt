package com.match.app.ui.chat

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.core.content.ContextCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.entity.MessageEntity
import com.match.app.data.local.dao.BlockDao
import com.match.app.data.local.entity.BlockEntity
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.ChatRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.UserProfile
import com.match.app.core.analytics.AnalyticsManager
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ChatUi(
    val peer: UserProfile? = null,
    val messages: List<MessageEntity> = emptyList(),
    val meId: Long = 0,
    val peerOnline: Boolean = false,
    val replyingTo: MessageEntity? = null,
    val peerTyping: Boolean = false,
    val isBlocked: Boolean = false,
    val blockSuccess: Boolean = false,
    /** True when free user has used up their free first message and needs to upgrade */
    val freeLimitReached: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val chat: ChatRepository,
    private val blockDao: BlockDao,
    private val analytics: AnalyticsManager
) : ViewModel() {

    private val peerIdFlow = MutableStateFlow<Long?>(null)

    private val messages: StateFlow<List<MessageEntity>> = combine(
        session.userId.filterNotNull(), peerIdFlow.filterNotNull()
    ) { me, peer -> me to peer }
        .flatMapLatest { (me, peer) -> chat.thread(me, peer) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val ui = MutableStateFlow(ChatUi())
    val state: StateFlow<ChatUi> = ui.asStateFlow()

    fun bind(peerId: Long) {
        peerIdFlow.value = peerId
        analytics.logChatOpened(peerId)
        viewModelScope.launch {
            val me = session.userId.filterNotNull().first()
            val peer = auth.currentProfile(peerId)
            ui.value = ui.value.copy(peer = peer, meId = me, peerOnline = peer?.isPremium == true)
            chat.markRead(me, peerId)
            val limitReached = chat.isFreeLimitReached(me, peerId)
            ui.update { it.copy(freeLimitReached = limitReached) }
        }
        viewModelScope.launch {
            messages.collect { msgs ->
                ui.update { it.copy(messages = msgs) }
                val me = session.userId.filterNotNull().first()
                val peer = peerIdFlow.value ?: return@collect
                val limitReached = chat.isFreeLimitReached(me, peer)
                ui.update { it.copy(freeLimitReached = limitReached) }
            }
        }
        // ── Real-time Firestore sync: incoming messages from the peer ──
        viewModelScope.launch {
            val me = session.userId.filterNotNull().first()
            chat.startFirestoreSync(me, peerId) // suspends until VM is cleared
        }
    }

    fun send(body: String) = viewModelScope.launch {
        val me = session.userId.filterNotNull().first()
        val peer = peerIdFlow.value ?: return@launch
        if (chat.isFreeLimitReached(me, peer)) {
            ui.update { it.copy(freeLimitReached = true) }
            return@launch
        }
        val replyId = ui.value.replyingTo?.id
        chat.send(me, peer, body, replyId)
        ui.update { it.copy(replyingTo = null) }
        analytics.logMessageSent()
    }

    fun sendVoice(voiceUri: String, durationMs: Long) = viewModelScope.launch {
        val me = session.userId.filterNotNull().first()
        val peer = peerIdFlow.value ?: return@launch
        chat.sendVoice(me, peer, voiceUri, durationMs)
        analytics.logVoiceMessageSent()
    }

    fun sendImage(imageUri: String) = viewModelScope.launch {
        val me = session.userId.filterNotNull().first()
        val peer = peerIdFlow.value ?: return@launch
        chat.sendImage(me, peer, imageUri)
        analytics.logMessageSent()
    }

    fun setReplyTo(message: MessageEntity?) {
        ui.update { it.copy(replyingTo = message) }
    }

    fun clearReply() {
        ui.update { it.copy(replyingTo = null) }
    }

    fun blockUser() = viewModelScope.launch {
        val me   = session.userId.filterNotNull().first()
        val peer = peerIdFlow.value ?: return@launch
        blockDao.block(BlockEntity(blockerId = me, blockedId = peer))
        ui.update { it.copy(isBlocked = true, blockSuccess = true) }
    }

    fun acknowledgeBlock() { ui.update { it.copy(blockSuccess = false) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(peerId: Long, onBack: () -> Unit, onUpgrade: () -> Unit = {}, vm: ChatViewModel = hiltViewModel()) {
    LaunchedEffect(peerId) { vm.bind(peerId) }
    val s by vm.state.collectAsState()

    // Handle block success — navigate back
    LaunchedEffect(s.blockSuccess) {
        if (s.blockSuccess) { vm.acknowledgeBlock(); onBack() }
    }
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    // ── Voice recording state ─────────────────────────────────────────
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceFile by remember { mutableStateOf<File?>(null) }
    var recordStart by remember { mutableStateOf(0L) }
    val snackbar = remember { SnackbarHostState() }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) scope.launch { snackbar.showSnackbar("Microphone permission required for voice messages") }
    }

    // ── Image picking state ──────────────────────────────────────────
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { vm.sendImage(it.toString()) }
    }

    // Tick the recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) { delay(1000); recordingSeconds++ }
        }
    }

    fun startRecording() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            voiceFile = file
            recordStart = System.currentTimeMillis()
            @Suppress("DEPRECATION")
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaRecorder(context) else MediaRecorder()
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            isRecording = true
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar("Could not start recording") }
        }
    }

    fun stopAndSend() {
        if (!isRecording) return
        isRecording = false
        val duration = System.currentTimeMillis() - recordStart
        // Require at least 500ms to avoid crash from stop() called too soon after start()
        if (duration < 500) {
            try { recorder?.apply { stop(); release() } } catch (_: Exception) {}
            recorder = null
            voiceFile?.delete(); voiceFile = null
            return
        }
        try {
            recorder?.apply { stop(); release() }
        } catch (_: Exception) {}
        recorder = null
        voiceFile?.let { f ->
            if (f.exists() && f.length() > 0) {
                vm.sendVoice(f.absolutePath, duration)
            }
        }
        voiceFile = null
    }

    fun cancelRecording() {
        isRecording = false
        try { recorder?.apply { stop(); release() } } catch (_: Exception) {}
        recorder = null
        voiceFile?.delete(); voiceFile = null
    }

    LaunchedEffect(s.messages.size) {
        if (s.messages.isNotEmpty()) listState.animateScrollToItem(s.messages.lastIndex)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag("chat_topbar"),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    s.peer?.displayName?.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (s.peerOnline) {
                            Box(
                                Modifier
                                    .offset(x = (-8).dp, y = 10.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(s.peer?.displayName ?: "Chat", style = MaterialTheme.typography.titleMedium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Lock, null,
                                    modifier = Modifier.size(9.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Text(
                                    t("end_to_end_encrypted", "End-to-end encrypted"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 3-dot overflow menu
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Block user") },
                                leadingIcon = { Icon(Icons.Filled.Block, null) },
                                onClick = { showOverflowMenu = false; showBlockDialog = true }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                // ── Free message limit banner ─────────────────────────────────
                if (s.freeLimitReached) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Free limit reached",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    "Upgrade to Premium to continue chatting",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            TextButton(onClick = onUpgrade) {
                                Text("Upgrade", color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            Surface(tonalElevation = 2.dp) {
                if (isRecording) {
                    // ── Recording UI ──────────────────────────────────────────
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = ::cancelRecording) {
                            Icon(Icons.Filled.Delete, contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error)
                        }
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Mic, null, Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error)
                                Text(
                                    "Recording… %02d:%02d".format(recordingSeconds / 60, recordingSeconds % 60),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        FilledIconButton(onClick = ::stopAndSend,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Send voice",
                                tint = MaterialTheme.colorScheme.onError)
                        }
                    }
                } else {
                    // ── Normal text + mic bar ─────────────────────────────────
                    Column(Modifier.fillMaxWidth()) {
                        // AI icebreakers — shown only when conversation is empty
                        val peer = s.peer
                        if (s.messages.isEmpty() && peer != null) {
                            val icebreakers = remember(peer) {
                                generateIcebreakers(peer)
                            }
                            if (icebreakers.isNotEmpty()) {
                                androidx.compose.foundation.lazy.LazyRow(
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    icebreakers.forEach { starter ->
                                        item {
                                            SuggestionChip(
                                                onClick = { draft = starter },
                                                label = { Text(text = starter, style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Reply preview banner
                        val replyingTo = s.replyingTo
                        if (replyingTo != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier
                                            .width(3.dp)
                                            .height(32.dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (replyingTo.fromUserId == s.meId) "You" else (s.peer?.displayName ?: ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            replyingTo.body.take(80),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { vm.clearReply() }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Close, "Cancel reply", Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Send image", tint = MaterialTheme.colorScheme.primary)
                        }
                        OutlinedTextField(
                            value = draft, onValueChange = { draft = it },
                            placeholder = { Text("Type a message…") },
                            modifier = Modifier.weight(1f).testTag("chat_input"),
                            maxLines = 4
                        )
                        Spacer(Modifier.width(8.dp))
                        if (draft.isBlank()) {
                            // Mic button — hold to record
                            FilledIconButton(
                                onClick = {},
                                modifier = Modifier
                                    .testTag("chat_mic")
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                startRecording() // startRecording checks permission internally
                                            }
                                        )
                                    },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = "Hold to record",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        } else {
                            FilledIconButton(
                                modifier = Modifier.testTag("chat_send"),
                                enabled = draft.isNotBlank(),
                                onClick = { vm.send(draft); draft = "" }
                            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
                        }
                    }
                    }
                }
            }
            } // end outer Column (freemium banner + Surface)
        }
    ) { pad ->
        if (s.messages.isEmpty()) {
            val templates = remember {
                listOf(
                    "Hi! I came across your profile and would love to connect 😊",
                    "Hello! Your profile really caught my attention. Would love to chat!",
                    "Hi there! We seem to have a lot in common. Let's talk?",
                    "Hello! I found your profile interesting. Hope we can get to know each other.",
                    "Hi! I'd love to learn more about you. Looking forward to a great conversation!"
                )
            }
            Column(
                Modifier.padding(pad).fillMaxSize().testTag("chat_empty"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Say hi to ${s.peer?.displayName ?: "them"} 👋",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Or use a quick starter:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                templates.forEach { tmpl ->
                    OutlinedButton(
                        onClick = { draft = tmpl },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(vertical = 3.dp)
                            .testTag("template_${tmpl.take(10)}")
                    ) {
                        Text(tmpl, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(pad).fillMaxSize().testTag("chat_list"),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item(key = "e2e_banner") {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Lock, null, Modifier.size(12.dp), tint = Color(0xFF388E3C))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                t("e2e_banner_msg", "Messages are end-to-end encrypted. No one outside this chat can read them."),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF388E3C)
                            )
                        }
                    }
                }
                items(s.messages, key = { it.id }) { m ->
                    Bubble(
                        m = m,
                        mine = m.fromUserId == s.meId,
                        repliedMessage = if (m.replyToId != null) s.messages.find { it.id == m.replyToId } else null,
                        onLongPress = { vm.setReplyTo(m) }
                    )
                }
            }
        }
    }

    // Block user confirmation dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            icon = { Icon(Icons.Filled.Block, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Block ${s.peer?.displayName ?: "this user"}?") },
            text = {
                Text("They won't be able to message you or see your profile. You can unblock them from Privacy & Safety settings.")
            },
            confirmButton = {
                Button(
                    onClick = { vm.blockUser(); showBlockDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Block") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Bubble(
    m: MessageEntity,
    mine: Boolean,
    repliedMessage: MessageEntity? = null,
    onLongPress: () -> Unit = {}
) {
    val align = if (mine) Arrangement.End else Arrangement.Start
    val bg = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val timeStr = remember(m.sentAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(m.sentAt))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = align) {
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .combinedClickable(onLongClick = onLongPress, onClick = {})
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag(if (mine) "bubble_mine_${m.id}" else "bubble_peer_${m.id}")
        ) {
            Column {
                // Reply context
                if (repliedMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = fg.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Row(Modifier.padding(6.dp)) {
                            Box(Modifier.width(2.dp).height(24.dp).background(fg.copy(alpha = 0.5f)))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                repliedMessage.body.take(60),
                                style = MaterialTheme.typography.labelSmall,
                                color = fg.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
                if (m.voiceUri != null) {
                    // ── Voice message bubble ───────────────────────────────
                    VoiceBubble(m, fg)
                } else if (m.imageUri != null) {
                    // ── Image message bubble ───────────────────────────────
                    AsyncImage(
                        model = m.imageUri,
                        contentDescription = "Image message",
                        modifier = Modifier
                            .sizeIn(maxWidth = 240.dp, maxHeight = 320.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                    )
                } else {
                    Text(m.body, color = fg)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(timeStr, color = fg.copy(alpha = 0.6f), fontSize = 10.sp)
                    if (mine) {
                        Spacer(Modifier.width(3.dp))
                        Icon(
                            if (m.readAt != null) Icons.Filled.DoneAll else Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (m.readAt != null) Color(0xFF4FC3F7) else fg.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceBubble(m: MessageEntity, fg: Color) {
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    val durationSec = ((m.voiceDurationMs ?: 0L) / 1000).toInt()
    @Suppress("UNUSED_VARIABLE") val totalMs = m.voiceDurationMs ?: 1L

    // Update progress while playing
    LaunchedEffect(playing) {
        if (playing) {
            while (playing) {
                val mp = player.value
                if (mp != null && mp.isPlaying) {
                    progress = mp.currentPosition.toFloat() / mp.duration.coerceAtLeast(1).toFloat()
                }
                delay(200)
            }
        } else {
            if (!playing) progress = 0f
        }
    }

    DisposableEffect(m.id) {
        onDispose {
            player.value?.apply { if (isPlaying) stop(); release() }
            player.value = null
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.widthIn(min = 140.dp, max = 220.dp)
    ) {
        IconButton(
            onClick = {
                if (playing) {
                    player.value?.pause()
                    playing = false
                } else {
                    try {
                        val mp = MediaPlayer().apply {
                            setDataSource(m.voiceUri!!)
                            prepare()
                            setOnCompletionListener { playing = false; progress = 0f }
                            start()
                        }
                        player.value?.release()
                        player.value = mp
                        playing = true
                    } catch (_: Exception) { playing = false }
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = fg,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = fg.copy(alpha = 0.8f),
                trackColor = fg.copy(alpha = 0.25f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (durationSec > 0) "%02d:%02d".format(durationSec / 60, durationSec % 60) else "Voice",
                fontSize = 10.sp,
                color = fg.copy(alpha = 0.7f)
            )
        }
    }
}

// ── AI Icebreaker generator ────────────────────────────────────────────────
// Generates personalized conversation starters based on peer profile details
private fun generateIcebreakers(peer: com.match.app.domain.model.UserProfile): List<String> {
    val starters = mutableListOf<String>()
    val name = peer.displayName.split(" ").firstOrNull() ?: "you"

    // Profession-based
    if (peer.profession.isNotBlank()) {
        starters += "Hi ${name}! I noticed you're in ${peer.profession} — what do you love most about your work? 😊"
    }
    // City-based
    if (peer.city.isNotBlank()) {
        starters += "Hey ${name}! I see you're from ${peer.city}. Any favourite spots there?"
    }
    // Hobbies-based
    if (peer.hobbies.isNotEmpty()) {
        val hobby = peer.hobbies.first()
        starters += "Hi! I love that you're into ${hobby}. Tell me more about it!"
    }
    // Bio-based keywords
    val bioKeywords = listOf("travel", "music", "food", "books", "fitness", "movies", "cooking", "art")
    for (kw in bioKeywords) {
        if (peer.bio.contains(kw, ignoreCase = true)) {
            starters += "I can tell from your bio you enjoy $kw — me too! What's your favourite?"
            break
        }
    }
    // Generic warm starters
    starters += "Hello ${name}! Your profile caught my eye. Would love to know more about you 🌸"
    starters += "Hi! I think we could have a lot in common. How are you doing today? 😊"

    return starters.take(5)
}
