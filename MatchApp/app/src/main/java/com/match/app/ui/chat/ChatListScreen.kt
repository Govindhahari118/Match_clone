package com.match.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.core.activity.ActivityStatusHelper
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.FirestoreChatService
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.session.SessionStore
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeStatePanel
import com.match.app.ui.components.MatreeStatusTone
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ConversationItem(
    val peerId: Long,
    val peerFirebaseUid: String,
    val peerName: String,
    val username: String,
    val lastMessage: String,
    val lastAt: Long,
    val lastActiveAt: Long,
    val showLastActive: Boolean,
    val isVerified: Boolean,
    val muted: Boolean = false,
    val archived: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val session: SessionStore,
    private val firestoreChat: FirestoreChatService,
    private val profileService: FirestoreProfileService,
    private val userDao: UserDao
) : ViewModel() {

    /** Firestore thread list is authoritative so a new device sees existing conversations. */
    val conversations: StateFlow<List<ConversationItem>> = session.firebaseUid.filterNotNull()
        .flatMapLatest { myUid ->
            combine(
                firestoreChat.observeThreads(myUid),
                firestoreChat.observeThreadPreferences(myUid)
            ) { threads, preferences ->
                threads.mapNotNull { thread ->
                    val peer = hydratePeer(thread.peerFirebaseUid) ?: return@mapNotNull null
                    val preference = preferences[FirestoreChatService.threadId(myUid, thread.peerFirebaseUid)]
                    ConversationItem(
                        peerId = peer.id,
                        peerFirebaseUid = peer.firebaseUid,
                        peerName = peer.displayName,
                        username = peer.username,
                        lastMessage = thread.lastMessage,
                        lastAt = thread.lastSentAt,
                        lastActiveAt = peer.lastActiveAt,
                        showLastActive = peer.showLastActive,
                        isVerified = peer.isVerified,
                        muted = preference?.muted == true,
                        archived = preference?.archived == true
                    )
                }.sortedByDescending { it.lastAt }
            }.catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = session.firebaseUid.filterNotNull()
        .flatMapLatest { firestoreChat.observeUnreadCount(it).catch { emit(0) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private suspend fun hydratePeer(firebaseUid: String): UserEntity? {
        userDao.findByFirebaseUid(firebaseUid)?.let { cached ->
            // Refresh authorization and public activity data when the conversation list is loaded.
            val remote = runCatching { profileService.fetchProfile(firebaseUid) }.getOrNull() ?: return cached
            val merged = remote.copy(id = cached.id, email = cached.email)
            userDao.update(merged)
            return merged
        }
        val remote = runCatching { profileService.fetchProfile(firebaseUid) }.getOrNull() ?: return null
        val cacheCopy = remote.copy(email = "$firebaseUid@cache.invalid")
        val id = userDao.insert(cacheCopy)
        return cacheCopy.copy(id = id)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onOpenChat: (Long) -> Unit = {},
    vm: ChatListViewModel = hiltViewModel()
) {
    val conversations by vm.conversations.collectAsState()
    val unread by vm.unreadCount.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val archivedCount = remember(conversations) { conversations.count { it.archived } }
    val filtered = remember(conversations, searchQuery, showArchived) {
        val q = searchQuery.trim().removePrefix("@")
        conversations.filter { conversation ->
            conversation.archived == showArchived &&
                (q.isBlank() ||
                    conversation.peerName.contains(q, ignoreCase = true) ||
                    conversation.username.contains(q, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("messages", "Messages")) },
                actions = {
                    BadgedBox(
                        badge = { if (unread > 0) Badge(containerColor = MaterialTheme.colorScheme.error) { Text(if (unread > 99) "99+" else "$unread") } },
                        modifier = Modifier.padding(end = 16.dp)
                    ) { Icon(Icons.Filled.MarkChatUnread, contentDescription = t("unread_messages", "Unread messages")) }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().testTag("chat_list_screen")) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it.take(64) },
                placeholder = { Text(t("search_chat_placeholder", "Search name or @username")) },
                leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, "Clear", Modifier.size(18.dp))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = MatreeDesign.spacing.sm, vertical = MatreeDesign.spacing.xs),
                shape = RoundedCornerShape(MatreeDesign.radii.large)
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = MatreeDesign.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
            ) {
                FilterChip(
                    selected = !showArchived,
                    onClick = { showArchived = false },
                    label = { Text(t("active_chats", "Active")) },
                    leadingIcon = if (!showArchived) {
                        { Icon(Icons.Filled.ChatBubble, null, Modifier.size(16.dp)) }
                    } else null
                )
                FilterChip(
                    selected = showArchived,
                    onClick = { showArchived = true },
                    label = { Text(t("archived_chats", mapOf("count" to archivedCount), "Archived ({count})")) },
                    leadingIcon = if (showArchived) {
                        { Icon(Icons.Filled.Archive, null, Modifier.size(16.dp)) }
                    } else null
                )
            }

            if (conversations.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(MatreeDesign.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.md)
                ) {
                    MatreeStatePanel(
                        title = t("no_conversations_yet", "No conversations yet"),
                        message = t("messaging_mutual_hint", "Messaging opens after a mutual interest. Start from the Mutual tab in Interests."),
                        icon = Icons.Filled.Forum
                    )
                    MatreeInlineNotice(
                        message = t("chat_safety_full", "Chat safely: never share passwords, OTPs or financial credentials; verify the person and meet first in a public place."),
                        icon = Icons.Filled.Security,
                        tone = MatreeStatusTone.WARNING
                    )
                }
            } else if (filtered.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(MatreeDesign.spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    MatreeStatePanel(
                        title = t("no_matching_conversations", "No matching conversations"),
                        message = if (showArchived) {
                            t("no_archived_conversations", "No archived conversations.")
                        } else {
                            t("no_conversations_search", "No conversations match your current search.")
                        },
                        icon = Icons.Filled.SearchOff
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = MatreeDesign.spacing.xs,
                        vertical = MatreeDesign.spacing.xxs
                    ),
                    modifier = Modifier.fillMaxSize().testTag("chat_list")
                ) {
                    items(filtered, key = { it.peerFirebaseUid }) { conversation ->
                        ConversationRow(conversation) { onOpenChat(conversation.peerId) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conv: ConversationItem, onClick: () -> Unit) {
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val time = remember(conv.lastAt, locale) { messageTime(conv.lastAt, locale) }
    val activity = remember(conv.lastActiveAt) { ActivityStatusHelper.from(conv.lastActiveAt) }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(MatreeDesign.radii.card),
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = MatreeDesign.spacing.xxs)
            .testTag("conv_card_${conv.peerId}")
    ) {
        Row(Modifier.padding(MatreeDesign.spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box {
                val avatarColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                )
                Box(
                    Modifier.size(MatreeDesign.sizes.avatarCompact).clip(RoundedCornerShape(MatreeDesign.radii.card)).background(Brush.linearGradient(avatarColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(conv.peerName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
                if (conv.showLastActive && activity.isOnline) {
                    Surface(
                        shape = CircleShape, color = MatreeDesign.colors.online,
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                        modifier = Modifier.align(Alignment.BottomEnd).size(14.dp)
                    ) {}
                }
            }
            Spacer(Modifier.width(MatreeDesign.spacing.sm))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(conv.peerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (conv.isVerified) { Spacer(Modifier.width(MatreeDesign.spacing.xxs)); Icon(Icons.Filled.Verified, t("verified", "Verified"), Modifier.size(15.dp), tint = MatreeDesign.colors.verified) }
                    if (conv.muted) { Spacer(Modifier.width(MatreeDesign.spacing.xxs)); Icon(Icons.Filled.NotificationsOff, t("muted_chat", "Muted"), Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (time.isNotBlank()) { Spacer(Modifier.width(8.dp)); Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (conv.username.isNotBlank()) Text("@${conv.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(conv.lastMessage.ifBlank { t("conversation_started", "Conversation started") }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (conv.showLastActive) {
                    Text(activity.label, style = MaterialTheme.typography.labelSmall, color = if (activity.isOnline) MatreeDesign.colors.online else MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

private fun messageTime(timestamp: Long, locale: Locale): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    val delta = (now - timestamp).coerceAtLeast(0L)
    return when {
        delta < 60_000L -> "Just now"
        delta < 3_600_000L -> "${delta / 60_000L}m"
        delta < 86_400_000L -> SimpleDateFormat("h:mm a", locale).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d", locale).format(Date(timestamp))
    }
}
