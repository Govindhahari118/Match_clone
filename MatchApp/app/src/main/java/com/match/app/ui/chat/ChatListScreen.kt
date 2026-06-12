package com.match.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.MessageDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.MessageEntity
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ConversationItem(
    val peerId: Long,
    val peerName: String,
    val lastMessage: String,
    val lastAt: Long,
    val unread: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val session: SessionStore,
    private val messageDao: MessageDao,
    private val userDao: UserDao
) : ViewModel() {
    val conversations: StateFlow<List<ConversationItem>> = session.userId.filterNotNull()
        .flatMapLatest { me ->
            messageDao.observeConversationPeerIds(me).map { peerIds ->
                peerIds.mapNotNull { peerId ->
                    val peer = userDao.findById(peerId) ?: return@mapNotNull null
                    val last: MessageEntity? = messageDao.lastMessage(me, peerId)
                    ConversationItem(
                        peerId      = peerId,
                        peerName    = peer.displayName,
                        lastMessage = last?.body ?: "",
                        lastAt      = last?.sentAt ?: 0L,
                        unread      = last != null && last.toUserId == me && last.readAt == null
                    )
                }.sortedByDescending { it.lastAt }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { messageDao.observeUnread(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
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
        if (searchQuery.isBlank()) conversations
        else conversations.filter { it.peerName.contains(searchQuery, ignoreCase = true) }
    }

    // Simulated "new matches" (conversations with no messages yet — start of chat strip)
    val newMatches = remember(conversations) {
        conversations.take(5) // in production: filter mutual matches not yet messaged
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("messages", "Messages")) },
                actions = {
                    BadgedBox(
                        badge = {
                            if (unread > 0) Badge(containerColor = MaterialTheme.colorScheme.error) { Text("$unread") }
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(Icons.Filled.MarkChatUnread, contentDescription = t("unread_messages", "Unread messages"))
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().testTag("chat_list_screen")) {
            // ── Premium chat upgrade banner ─────────────────────────────
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFF3E0),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(28.dp), tint = Color(0xFFE65100))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("upgrade_to_chat_freely", "Upgrade to chat freely"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text(t("free_chat_limit_desc", "Free users can send 5 messages/day. Premium members get unlimited messaging + read receipts."),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4E342E))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {},
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFE65100),
                            contentColor = Color.White
                        )
                    ) { Text(t("upgrade", "Upgrade"), style = MaterialTheme.typography.labelMedium) }
                }
            }

            // ── Search bar ─────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(t("search_conversations", "Search conversations...")) },
                leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, null, Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )

            // ── New Matches strip ──────────────────────────────────────
            if (conversations.isNotEmpty() && searchQuery.isBlank()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("New Matches",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(newMatches, key = { "nm_${it.peerId}" }) { conv ->
                            NewMatchBubble(conv) { onOpenChat(conv.peerId) }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
            }

            // ── Chat tips for empty state ───────────────────────────────
            if (conversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Filled.Forum, null, Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Text(t("no_conversations_yet", "No conversations yet"), style = MaterialTheme.typography.titleMedium)
                        Text(t("conversations_empty_hint", "When you start chatting with a match, it will appear here."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))

                        // Safety tips card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(t("chat_safety_tips", "Chat safety tips"), style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                listOf(
                                    t("chat_tip_1", "Never share passwords, OTPs, or financial details"),
                                    t("chat_tip_2", "Video call before meeting in person"),
                                    t("chat_tip_3", "Meet first in a public place"),
                                    t("chat_tip_4", "Report suspicious profiles immediately")
                                ).forEach { tip ->
                                    Text("- $tip", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize().testTag("chat_list")
                ) {
                    items(filtered, key = { it.peerId }) { conv ->
                        ConversationRow(conv) { onOpenChat(conv.peerId) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conv: ConversationItem, onClick: () -> Unit) {
    val time = remember(conv.lastAt) {
        val now = System.currentTimeMillis()
        when {
            conv.lastAt == 0L -> ""
            now - conv.lastAt < 60_000L -> "Just now"
            now - conv.lastAt < 3_600_000L -> "${(now - conv.lastAt) / 60_000}m ago"
            now - conv.lastAt < 86_400_000L -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(conv.lastAt))
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(conv.lastAt))
        }
    }
    // Simulate online: last message within last 10 minutes means "online"
    val isOnline = remember(conv.lastAt) { conv.lastAt > 0 && System.currentTimeMillis() - conv.lastAt < 600_000L }

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("conv_card_${conv.peerId}")
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar with optional online dot
            Box {
                val avatarColors = remember(conv.peerId) {
                    val palette = listOf(
                        listOf(Color(0xFFE91E63), Color(0xFFFF5722)),
                        listOf(Color(0xFF9C27B0), Color(0xFF3F51B5)),
                        listOf(Color(0xFF009688), Color(0xFF4CAF50)),
                        listOf(Color(0xFF1976D2), Color(0xFF00BCD4)),
                        listOf(Color(0xFF795548), Color(0xFF607D8B)),
                        listOf(Color(0xFFFF9800), Color(0xFFFFEB3B)),
                    )
                    palette[(conv.peerId % palette.size).toInt()]
                }
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(avatarColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(conv.peerName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
                if (isOnline) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(conv.peerName, style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (conv.unread) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f))
                    if (time.isNotBlank()) {
                        Text(time, style = MaterialTheme.typography.labelSmall,
                            color = if (conv.unread) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    conv.lastMessage.ifBlank { "Start chatting" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (conv.unread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (conv.unread) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (conv.unread) {
                Spacer(Modifier.width(8.dp))
                Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("●") }
            }
        }
    }
}

@Composable
private fun NewMatchBubble(conv: ConversationItem, onClick: () -> Unit) {
    val avatarColors = remember(conv.peerId) {
        val palette = listOf(
            listOf(Color(0xFFE91E63), Color(0xFFFF5722)),
            listOf(Color(0xFF9C27B0), Color(0xFF3F51B5)),
            listOf(Color(0xFF009688), Color(0xFF4CAF50)),
            listOf(Color(0xFF1976D2), Color(0xFF00BCD4)),
            listOf(Color(0xFF795548), Color(0xFF607D8B)),
            listOf(Color(0xFFFF9800), Color(0xFFFFEB3B)),
        )
        palette[(conv.peerId % palette.size).toInt()]
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp).clickable(onClick = onClick)
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(avatarColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(conv.peerName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(conv.peerName.split(" ").firstOrNull() ?: conv.peerName,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
