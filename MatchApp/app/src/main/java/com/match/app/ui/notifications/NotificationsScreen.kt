package com.match.app.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.entity.NotificationEntity
import com.match.app.data.repo.NotificationRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val session: SessionStore,
    private val repo: NotificationRepository
) : ViewModel() {
    val notifications: StateFlow<List<NotificationEntity>> = session.userId.filterNotNull()
        .flatMapLatest { repo.observe(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadCount: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { repo.observeUnreadCount(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun markAllRead() = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        repo.markAllRead(uid)
    }

    fun markRead(id: Long) = viewModelScope.launch { repo.markRead(id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    onOpenChat: (Long) -> Unit = {},
    vm: NotificationsViewModel = hiltViewModel()
) {
    val notifications by vm.notifications.collectAsState()
    val unread by vm.unreadCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("notifications", "Notifications")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("notifications_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unread > 0) {
                        TextButton(onClick = vm::markAllRead) { Text(t("mark_all_read", "Mark all read")) }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().testTag("notifications_screen")) {
            if (unread > 0) {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "$unread unread",
                        Modifier.padding(12.dp, 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            if (notifications.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Notifications,
                            null,
                            Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(t("no_notifications", "No notifications yet"), style = MaterialTheme.typography.titleMedium)
                        Text(t("activity_will_appear", "Activity will appear here"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().testTag("notifications_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { n ->
                        NotificationCard(
                            n,
                            onClick = {
                                vm.markRead(n.id)
                                val from = n.fromUserId
                                if (from != null) {
                                    when (n.type) {
                                        "LIKE", "INTEREST", "MATCH", "VIEW" -> onOpenProfile(from)
                                        "MESSAGE" -> onOpenChat(from)
                                        else -> Unit
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(n: NotificationEntity, onClick: () -> Unit) {
    val locale = Locale.getDefault()
    val formattedTime = remember(n.createdAt, locale) {
        SimpleDateFormat("MMM d, h:mm a", locale).format(Date(n.createdAt))
    }
    val (icon, color) = when (n.type) {
        "LIKE" -> Icons.Filled.Favorite to Color(0xFFE91E63)
        "INTEREST" -> Icons.Filled.PersonAdd to Color(0xFF1976D2)
        "MATCH" -> Icons.Filled.Stars to Color(0xFFFFB300)
        "VIEW" -> Icons.Filled.Visibility to Color(0xFF388E3C)
        else -> Icons.Filled.Notifications to MaterialTheme.colorScheme.primary
    }
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("notif_${n.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (!n.isRead) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(50),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    n.title,
                    fontWeight = if (!n.isRead) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    n.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (!n.isRead) {
                Box(Modifier.size(8.dp).padding(start = 4.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {}
                }
            }
        }
    }
}
