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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.platformLocale
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
import com.match.app.ui.i18n.t
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
    val isVerified: Boolean
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
            firestoreChat.observeThreads(myUid).map { threads ->
                threads.mapNotNull { thread ->
                    val peer = hydratePeer(thread.peerFirebaseUid) ?: return@mapNotNull null
                    ConversationItem(
                        peerId = peer.id,
                        peerFirebaseUid = peer.firebaseUid,
                        peerName = peer.displayName,
                        username = peer.username,
                        lastMessage = thread.lastMessage,
                        lastAt = thread.lastSentAt,
                        lastActiveAt = peer.lastActiveAt,
                        showLastActive = peer.showLastActive,
                        isVerified = peer.isVerified
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
            val merged = remote.copy(id = cached.id, email = cached.email, passwordHash = cached.passwordHash, isSeed = false)
            userDao.update(merged)
            return merged
        }
        val remote = runCatching { profileService.fetchProfile(firebaseUid) }.getOrNull() ?: return null
        val cacheCopy = remote.copy(email = "$firebaseUid@cache.invalid", passwordHash = "", isSeed = false)
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
    val filtered = remember(conversations, searchQuery) {
        val q = searchQuery.trim().removePrefix("@")
        if (q.isBlank()) conversations else conversations.filter {
            it.peerName.contains(q, ignoreCase = true) || it.username.contains(q, ignoreCase = true)
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
                placeholder = { Text("Search name or @username") },
                leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, "Clear", Modifier.size(18.dp))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp)
            )

            if (conversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Filled.Forum, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Text(t("no_conversations_yet", "No conversations yet"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Messaging opens after a mutual interest. Start from the Mutual tab in Interests.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(t("chat_safety_tips", "Chat safety tips"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                listOf(
                                    "Never share passwords, OTPs or financial credentials",
                                    "Verify the person before meeting",
                                    "Meet first in a public place",
                                    "Block and report suspicious behavior"
                                ).forEach { tip -> Text("• $tip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No conversations match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp), modifier = Modifier.fillMaxSize().testTag("chat_list")) {
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
    val locale = LocalLocale.current.platformLocale
    val time = remember(conv.lastAt, locale) { messageTime(conv.lastAt, locale) }
    val activity = remember(conv.lastActiveAt) { ActivityStatusHelper.from(conv.lastActiveAt) }

    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("conv_card_${conv.peerId}")) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                val avatarColors = remember(conv.peerId) {
                    val palette = listOf(
                        listOf(Color(0xFFE91E63), Color(0xFFFF5722)), listOf(Color(0xFF9C27B0), Color(0xFF3F51B5)),
                        listOf(Color(0xFF009688), Color(0xFF4CAF50)), listOf(Color(0xFF1976D2), Color(0xFF00BCD4)),
                        listOf(Color(0xFF795548), Color(0xFF607D8B))
                    )
                    palette[(conv.peerId % palette.size).toInt()]
                }
                Box(
                    Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(avatarColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(conv.peerName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                if (conv.showLastActive && activity.isOnline) {
                    Surface(
                        shape = CircleShape, color = Color(0xFF2E7D32),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                        modifier = Modifier.align(Alignment.BottomEnd).size(14.dp)
                    ) {}
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(conv.peerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (conv.isVerified) { Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Verified, "Verified", Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary) }
                    if (time.isNotBlank()) { Spacer(Modifier.width(8.dp)); Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (conv.username.isNotBlank()) Text("@${conv.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(conv.lastMessage.ifBlank { "Conversation started" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (conv.showLastActive) {
                    Text(activity.label, style = MaterialTheme.typography.labelSmall, color = if (activity.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline)
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
