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
import com.match.app.data.repo.MemberPresence
import com.match.app.data.repo.PresenceRepository
import com.match.app.data.repo.SupportRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.UserProfile
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeLoadingState
import com.match.app.ui.components.MatreeStatePanel
import com.match.app.ui.components.MatreeStatusTone
import com.match.app.ui.common.reportReasonOptions
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign
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
    val presence: MemberPresence = MemberPresence(),
    val isPeerTyping: Boolean = false,
    val reportSubmitting: Boolean = false,
    val reportMessage: String? = null,
    val loading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val chat: ChatRepository,
    private val social: SocialRepository,
    private val presenceRepository: PresenceRepository,
    private val supportRepository: SupportRepository,
    private val analytics: AnalyticsManager
) : ViewModel() {
    private val peerIdFlow = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(ChatUi())
    val state: StateFlow<ChatUi> = _state.asStateFlow()
    private var boundPeerId: Long? = null
    private var typingPublished = false

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
            while (peerIdFlow.value == peerId) {
                val current = _state.value
                val targetUid = current.peer?.firebaseUid.orEmpty()
                if (current.isMutual && !current.isBlocked && targetUid.isNotBlank()) {
                    presenceRepository.memberPresence(targetUid)
                        .onSuccess { memberPresence ->
                            _state.update { it.copy(presence = memberPresence) }
                        }
                } else {
                    _state.update { it.copy(presence = MemberPresence()) }
                }
                delay(60_000)
            }
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

        viewModelScope.launch {
            val myUid = session.firebaseUid.filterNotNull().first()
            val peer = auth.currentProfile(peerId)
            val peerUid = peer?.firebaseUid.orEmpty()
            val mutual = social.isLiked(session.userId.filterNotNull().first(), peerId) &&
                social.isLiked(peerId, session.userId.filterNotNull().first())
            val blocked = social.isBlocked(session.userId.filterNotNull().first(), peerId)
            if (mutual && !blocked && myUid.isNotBlank() && peerUid.isNotBlank()) {
                chat.observePeerTyping(myUid, peerUid)
                    .catch { emit(false) }
                    .collect { typing ->
                        _state.update { it.copy(isPeerTyping = typing) }
                    }
            } else {
                _state.update { it.copy(isPeerTyping = false) }
            }
        }
    }

    fun send(body: String) = viewModelScope.launch {
        if (!_state.value.isMutual || _state.value.isBlocked) return@launch
        val me = _state.value.meId.takeIf { it > 0 } ?: return@launch
        val peer = peerIdFlow.value ?: return@launch
        val trimmed = body.trim()
        if (trimmed.isBlank()) return@launch
        runCatching { chat.send(me, peer, trimmed, _state.value.replyingTo?.id) }
            .onSuccess {
                _state.update { it.copy(replyingTo = null, error = null) }
                analytics.logMessageSent()
            }
            .onFailure { error ->
                _state.update {
                    it.copy(error = error.message ?: "Message could not be stored securely.")
                }
            }
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

    fun retryFailed(message: MessageEntity) = viewModelScope.launch {
        if (message.fromUserId != _state.value.meId || !message.status.equals("failed", true)) return@launch
        runCatching { chat.retryFailed(message) }
            .onFailure { error ->
                _state.update {
                    it.copy(error = error.message ?: "This message could not be retried.")
                }
            }
    }

    fun setReplyTo(message: MessageEntity?) = _state.update { it.copy(replyingTo = message) }
    fun clearError() = _state.update { it.copy(error = null) }

    fun setTyping(typing: Boolean) {
        val current = _state.value
        val peerUid = current.peer?.firebaseUid.orEmpty()
        val allowed = current.isMutual && !current.isBlocked && peerUid.isNotBlank()
        val desired = typing && allowed
        if (typingPublished == desired) return
        typingPublished = desired
        viewModelScope.launch {
            runCatching { chat.setTyping(peerUid, desired) }
                .onFailure {
                    if (desired) typingPublished = false
                }
        }
    }

    fun clearReportMessage() = _state.update { it.copy(reportMessage = null) }

    fun reportMessage(message: MessageEntity, reasonCode: String) = viewModelScope.launch {
        val current = _state.value
        val targetUid = current.peer?.firebaseUid.orEmpty()
        val messageId = message.clientMessageId
        if (
            targetUid.isBlank() ||
            reasonCode.isBlank() ||
            message.fromUserId == current.meId ||
            !messageId.matches(Regex("[A-Za-z0-9_-]{16,128}")) ||
            current.reportSubmitting
        ) return@launch

        _state.update { it.copy(reportSubmitting = true, reportMessage = null) }
        supportRepository.submitChatMessageReport(
            targetUid = targetUid,
            messageId = messageId,
            reasonCode = reasonCode,
            details = "Reported from private match conversation"
        ).onSuccess {
            _state.update {
                it.copy(reportSubmitting = false, reportMessage = "message_report_submitted")
            }
        }.onFailure {
            _state.update {
                it.copy(reportSubmitting = false, reportMessage = "message_report_failed")
            }
        }
    }

    fun reportMember(reasonCode: String) = viewModelScope.launch {
        val targetUid = _state.value.peer?.firebaseUid.orEmpty()
        if (targetUid.isBlank() || reasonCode.isBlank() || _state.value.reportSubmitting) return@launch
        _state.update { it.copy(reportSubmitting = true, reportMessage = null) }
        supportRepository.submitProfileReport(targetUid, reasonCode, "Reported from private match conversation")
            .onSuccess {
                analytics.logProfileReported(reasonCode)
                _state.update {
                    it.copy(reportSubmitting = false, reportMessage = "report_submitted")
                }
            }
            .onFailure {
                _state.update {
                    it.copy(reportSubmitting = false, reportMessage = "report_failed")
                }
            }
    }

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
    onSecureCall: () -> Unit = {},
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
    var showReportDialog by remember { mutableStateOf(false) }
    var messageActions by remember { mutableStateOf<MessageEntity?>(null) }
    var messageToReport by remember { mutableStateOf<MessageEntity?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceFile by remember { mutableStateOf<File?>(null) }
    var recordStartedAt by remember { mutableLongStateOf(0L) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    val micPermissionRequired = t("mic_permission_voice", "Microphone permission is required for voice messages.")
    val voiceRecordingStartFailed = t("voice_recording_start_failed", "Could not start voice recording.")

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
        if (!granted) scope.launch { snackbar.showSnackbar(micPermissionRequired) }
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
            scope.launch { snackbar.showSnackbar(voiceRecordingStartFailed) }
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
    LaunchedEffect(draft, state.isMutual, state.isBlocked) {
        if (draft.isBlank() || !state.isMutual || state.isBlocked) {
            vm.setTyping(false)
        } else {
            vm.setTyping(true)
            delay(2_500)
            vm.setTyping(false)
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }
    val reportFeedback = when (state.reportMessage) {
        "report_submitted" -> t("report_submitted", "Report submitted for moderation.")
        "report_failed" -> t("report_failed", "Report could not be submitted. Check your connection and try again.")
        "message_report_submitted" -> t("message_report_submitted", "Message report submitted for moderation.")
        "message_report_failed" -> t("message_report_failed", "Message report could not be submitted. Check your connection and try again.")
        else -> null
    }
    LaunchedEffect(reportFeedback) {
        reportFeedback?.let {
            snackbar.showSnackbar(it)
            vm.clearReportMessage()
        }
    }
    DisposableEffect(peerId) {
        onDispose {
            vm.setTyping(false)
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(MatreeDesign.sizes.avatarSmall)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(state.peer?.displayName?.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(MatreeDesign.spacing.sm))
                        Column {
                            Text(state.peer?.displayName ?: t("chat", "Chat"), style = MaterialTheme.typography.titleMedium)
                            val subtitle = when {
                                !state.isMutual -> t("chat_unlocks_after_mutual", "Chat unlocks after mutual interest")
                                state.isBlocked -> t("chat_blocked_status", "Conversation blocked")
                                state.isPeerTyping -> t("typing", "Typing…")
                                state.presence.online -> t("online_now", "Online now")
                                state.presence.lastActiveAt > 0L -> t(
                                    "last_active",
                                    mapOf("time" to localizedPresenceTime(state.presence.lastActiveAt)),
                                    "Last active {time}"
                                )
                                else -> t("private_match_conversation", "Private match conversation")
                            }
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.presence.online && state.isMutual && !state.isBlocked) {
                                    MatreeDesign.colors.online
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (state.isMutual && !state.isBlocked) {
                        IconButton(
                            onClick = onSecureCall,
                            modifier = Modifier.testTag("chat_secure_call")
                        ) {
                            Icon(
                                Icons.Filled.Call,
                                contentDescription = t("secure_call", "Secure Call")
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, "More options") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (state.isBlocked) t("unblock_member", "Unblock member") else t("block_member", "Block member")) },
                                leadingIcon = { Icon(if (state.isBlocked) Icons.Filled.LockOpen else Icons.Filled.Block, null) },
                                onClick = {
                                    showMenu = false
                                    if (state.isBlocked) vm.unblockUser() else showBlockDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(t("report_profile", "Report profile")) },
                                leadingIcon = { Icon(Icons.Filled.Flag, null) },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
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
                state.isBlocked -> StatusBar(
                    t("chat_blocked_notice", "You blocked this member. Unblock them to continue the conversation."),
                    MatreeStatusTone.ERROR
                )
                !state.isMutual -> StatusBar(
                    t("chat_mutual_required_notice", "Messaging is available only after both members accept each other's interest."),
                    MatreeStatusTone.WARNING
                )
                isRecording -> {
                    Surface(tonalElevation = 2.dp) {
                        Row(Modifier.fillMaxWidth().padding(MatreeDesign.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { stopRecorder(delete = true, send = false) }) { Icon(Icons.Filled.Delete, t("cancel_recording", "Cancel recording"), tint = MaterialTheme.colorScheme.error) }
                            Text(t("recording_time", mapOf("time" to "%02d:%02d".format(recordingSeconds / 60, recordingSeconds % 60)), "Recording {time}"), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            FilledIconButton(onClick = { stopRecorder(delete = false, send = true) }) { Icon(Icons.Filled.Send, t("send_voice", "Send voice")) }
                        }
                    }
                }
                else -> {
                    Surface(tonalElevation = 2.dp) {
                        Column {
                            state.replyingTo?.let { reply ->
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        Modifier.padding(
                                            horizontal = MatreeDesign.spacing.sm,
                                            vertical = MatreeDesign.spacing.xs
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(t("replying_to", mapOf("name" to if (reply.fromUserId == state.meId) t("your_message", "your message") else state.peer?.displayName.orEmpty()), "Replying to {name}"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            Text(reply.body.take(80), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                        IconButton(onClick = { vm.setReplyTo(null) }) { Icon(Icons.Filled.Close, t("cancel_reply", "Cancel reply")) }
                                    }
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(MatreeDesign.spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { imagePicker.launch("image/*") }) { Icon(Icons.Filled.AddPhotoAlternate, t("send_image", "Send image")) }
                                OutlinedTextField(
                                    value = draft,
                                    onValueChange = { draft = it.take(3000) },
                                    placeholder = { Text(t("message_placeholder", "Message")) },
                                    modifier = Modifier.weight(1f).testTag("chat_input"),
                                    maxLines = 4
                                )
                                Spacer(Modifier.width(MatreeDesign.spacing.xs))
                                if (draft.isBlank()) {
                                    FilledIconButton(onClick = ::startRecording) { Icon(Icons.Filled.Mic, t("record_voice", "Record voice")) }
                                } else {
                                    FilledIconButton(onClick = {
                                        val text = draft
                                        draft = ""
                                        vm.send(text)
                                    }) { Icon(Icons.AutoMirrored.Filled.Send, t("send", "Send")) }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> Box(
                Modifier.padding(padding).fillMaxSize().padding(MatreeDesign.spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                MatreeLoadingState(message = t("loading_conversation", "Loading conversation…"), rows = 2)
            }
            state.peer == null -> Box(
                Modifier.padding(padding).fillMaxSize().padding(MatreeDesign.spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                MatreeStatePanel(
                    title = t("conversation_unavailable", "Conversation unavailable"),
                    message = t("conversation_unavailable_message", "This conversation can no longer be opened with your current account or relationship state."),
                    icon = Icons.Filled.Forum,
                    tone = MatreeStatusTone.WARNING
                )
            }
            state.messages.isEmpty() -> {
                Column(
                    Modifier.padding(padding).fillMaxSize().padding(MatreeDesign.spacing.xl),
                    verticalArrangement = Arrangement.Center
                ) {
                    MatreeStatePanel(
                        title = if (state.isMutual) t("start_your_conversation", "Start your conversation") else t("mutual_interest_required", "Mutual interest required"),
                        message = if (state.isMutual) {
                            t("chat_respect_notice", "Be respectful and avoid sharing sensitive information too early.")
                        } else {
                            t("both_accept_before_messages", "Both members must accept each other's interest before messages can be sent.")
                        },
                        icon = Icons.Filled.Forum,
                        tone = if (state.isMutual) MatreeStatusTone.NEUTRAL else MatreeStatusTone.WARNING
                    )
                    if (state.isMutual && !state.isBlocked) {
                        Spacer(Modifier.height(MatreeDesign.spacing.md))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("conversation_starters"),
                            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
                        ) {
                            Text(
                                t("conversation_starters_title", "Start respectfully"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                t(
                                    "conversation_starters_body",
                                    "Choose a starter, edit it if you want, then send it yourself."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val starters = listOf(
                                t(
                                    "conversation_starter_1",
                                    "Hi! I’m glad we matched. What would you like me to know about you first?"
                                ),
                                t(
                                    "conversation_starter_2",
                                    "Hello! I enjoyed reading your profile. What matters most to you in a life partner?"
                                ),
                                t(
                                    "conversation_starter_3",
                                    "Hi! Would you like to start by talking about family, work, interests, or something else?"
                                )
                            )
                            starters.forEachIndexed { index, starter ->
                                OutlinedButton(
                                    onClick = { draft = starter },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("conversation_starter_$index")
                                ) {
                                    Text(
                                        starter,
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.padding(padding).fillMaxSize().testTag("chat_list"),
                    contentPadding = PaddingValues(MatreeDesign.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
                ) {
                    item("privacy_notice") {
                        MatreeInlineNotice(
                            message = t("private_chat_safety_notice", "Private match conversation. Local message copies are protected on this device; report or block any misuse."),
                            icon = Icons.Filled.Security,
                            modifier = Modifier.padding(bottom = MatreeDesign.spacing.xs)
                        )
                    }
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            mine = message.fromUserId == state.meId,
                            repliedMessage = message.replyToId?.let { id -> state.messages.firstOrNull { it.id == id } },
                            onLongPress = { messageActions = message },
                            onRetry = { vm.retryFailed(message) }
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        val reasons = reportReasonOptions()
        AlertDialog(
            onDismissRequest = { if (!state.reportSubmitting) showReportDialog = false },
            title = { Text(t("report_profile", "Report profile")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                    Text(t("report_choose_reason", "Choose the reason that best describes the issue."))
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                showReportDialog = false
                                vm.reportMember(reason.code)
                            },
                            enabled = !state.reportSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(reason.label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showReportDialog = false },
                    enabled = !state.reportSubmitting
                ) { Text(t("cancel", "Cancel")) }
            }
        )
    }

    messageActions?.let { message ->
        val mine = message.fromUserId == state.meId
        AlertDialog(
            onDismissRequest = { messageActions = null },
            title = { Text(t("message_actions", "Message actions")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                    TextButton(
                        onClick = {
                            vm.setReplyTo(message)
                            messageActions = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(t("reply", "Reply"))
                    }
                    if (!mine && message.clientMessageId.matches(Regex("[A-Za-z0-9_-]{16,128}"))) {
                        TextButton(
                            onClick = {
                                messageToReport = message
                                messageActions = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(t("report_message", "Report message"))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { messageActions = null }) {
                    Text(t("cancel", "Cancel"))
                }
            }
        )
    }

    messageToReport?.let { message ->
        val reasons = reportReasonOptions()
        AlertDialog(
            onDismissRequest = {
                if (!state.reportSubmitting) messageToReport = null
            },
            title = { Text(t("report_message", "Report message")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                    Text(
                        t(
                            "report_message_body",
                            "Choose why this received message should be reviewed. The server preserves the original message evidence for moderation."
                        )
                    )
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                messageToReport = null
                                vm.reportMessage(message, reason.code)
                            },
                            enabled = !state.reportSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reason.label)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { messageToReport = null },
                    enabled = !state.reportSubmitting
                ) {
                    Text(t("cancel", "Cancel"))
                }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(t("block_member_title", mapOf("name" to (state.peer?.displayName ?: t("this_member", "this member"))), "Block {name}?")) },
            text = { Text(t("block_member_body", "Blocking stops new interactions between you and this member. You can unblock them later.")) },
            confirmButton = {
                Button(
                    onClick = { showBlockDialog = false; vm.blockUser() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(t("block", "Block")) }
            },
            dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text(t("cancel", "Cancel")) } }
        )
    }
}

@Composable
private fun localizedPresenceTime(lastActiveAt: Long): String {
    val deltaMinutes = ((System.currentTimeMillis() - lastActiveAt).coerceAtLeast(0L) / 60_000L).toInt()
    return when {
        deltaMinutes < 1 -> t("just_now", "just now")
        deltaMinutes < 60 -> t("minutes_ago", mapOf("count" to deltaMinutes), "{count}m ago")
        deltaMinutes < 24 * 60 -> t("hours_ago", mapOf("count" to (deltaMinutes / 60)), "{count}h ago")
        else -> t("days_ago", mapOf("count" to (deltaMinutes / (24 * 60))), "{count}d ago")
    }
}

@Composable
private fun StatusBar(text: String, tone: MatreeStatusTone) {
    MatreeInlineNotice(
        message = text,
        tone = tone,
        modifier = Modifier.padding(MatreeDesign.spacing.xs)
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageEntity,
    mine: Boolean,
    repliedMessage: MessageEntity?,
    onLongPress: () -> Unit,
    onRetry: () -> Unit
) {
    val background = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val time = remember(message.sentAt) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.sentAt)) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = MatreeDesign.sizes.chatBubbleMaxWidth)
                .clip(RoundedCornerShape(MatreeDesign.radii.card))
                .background(background)
                .combinedClickable(
                    onClick = {
                        if (mine && message.status.equals("failed", true)) onRetry()
                        else onLongPress()
                    },
                    onLongClick = onLongPress
                )
                .padding(MatreeDesign.spacing.sm)
        ) {
            repliedMessage?.let {
                Surface(shape = RoundedCornerShape(MatreeDesign.radii.small), color = foreground.copy(alpha = 0.12f)) {
                    Text(it.body.take(80), Modifier.padding(MatreeDesign.spacing.xs), style = MaterialTheme.typography.labelSmall, color = foreground.copy(alpha = 0.85f), maxLines = 1)
                }
                Spacer(Modifier.height(MatreeDesign.spacing.xxs))
            }
            when {
                message.voiceUri != null -> VoiceMessage(message, foreground)
                message.imageUri != null -> AsyncImage(
                    model = message.imageUri,
                    contentDescription = t("image_message", "Image message"),
                    modifier = Modifier
                        .sizeIn(
                            maxWidth = MatreeDesign.sizes.chatMediaMaxWidth,
                            maxHeight = MatreeDesign.sizes.chatMediaMaxHeight
                        )
                        .clip(RoundedCornerShape(MatreeDesign.radii.medium)),
                    contentScale = ContentScale.Crop
                )
                else -> Text(message.body, color = foreground)
            }
            Spacer(Modifier.height(MatreeDesign.spacing.xxs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(time, fontSize = 10.sp, color = foreground.copy(alpha = 0.65f))
                if (mine) {
                    Spacer(Modifier.width(MatreeDesign.spacing.xxs))
                    val icon = when (message.status.lowercase()) {
                        "read" -> Icons.Filled.DoneAll
                        "failed" -> Icons.Filled.ErrorOutline
                        else -> Icons.Filled.Check
                    }
                    Icon(icon, message.status, Modifier.size(13.dp), tint = if (message.status.equals("failed", true)) MaterialTheme.colorScheme.error else foreground.copy(alpha = 0.75f))
                    if (message.status.equals("failed", true)) {
                        Spacer(Modifier.width(MatreeDesign.spacing.xxs))
                        Text(
                            t("tap_to_retry", "Tap to retry"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
        }) { Icon(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (playing) t("pause", "Pause") else t("play", "Play"),
            tint = foreground
        ) }
        Text(if (seconds > 0) "%02d:%02d".format(seconds / 60, seconds % 60) else t("voice_message", "Voice message"), color = foreground)
    }
}
