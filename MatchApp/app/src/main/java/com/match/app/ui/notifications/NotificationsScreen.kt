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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.entity.NotificationEntity
import com.match.app.data.repo.NotificationRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeStatePanel
import com.match.app.ui.components.MatreeStatusTone
import com.match.app.ui.components.MatreeTopBar
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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
    onOpenInterests: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    onOpenChat: (Long) -> Unit = {},
    vm: NotificationsViewModel = hiltViewModel()
) {
    val notifications by vm.notifications.collectAsState()
    val unread by vm.unreadCount.collectAsState()

    Scaffold(
        topBar = {
            MatreeTopBar(
                title = t("notifications", "Notifications"),
                onBack = onBack,
                actions = {
                    if (unread > 0) {
                        TextButton(onClick = vm::markAllRead) {
                            Text(t("mark_all_read", "Mark all read"))
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().testTag("notifications_screen")) {
            if (unread > 0) {
                MatreeInlineNotice(
                    message = t("unread_count", mapOf("count" to unread), "{count} unread"),
                    icon = Icons.Filled.Notifications,
                    tone = MatreeStatusTone.INTEREST,
                    modifier = Modifier.padding(horizontal = MatreeDesign.spacing.md, vertical = MatreeDesign.spacing.xs)
                )
            }
            if (notifications.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(MatreeDesign.spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    MatreeStatePanel(
                        title = t("no_notifications", "No notifications yet"),
                        message = t("activity_will_appear", "Account and match activity will appear here."),
                        icon = Icons.Filled.Notifications
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().testTag("notifications_list"),
                    contentPadding = PaddingValues(MatreeDesign.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
                ) {
                    items(notifications, key = { it.id }) { n ->
                        NotificationCard(n, onClick = {
                            vm.markRead(n.id)
                            when (notificationCardDestination(n.type, n.fromUserId != null)) {
                                NotificationCardDestination.INTERESTS -> onOpenInterests()
                                NotificationCardDestination.CHAT -> n.fromUserId?.let(onOpenChat)
                                NotificationCardDestination.PROFILE -> n.fromUserId?.let(onOpenProfile)
                                NotificationCardDestination.NONE -> Unit
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(n: NotificationEntity, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val formattedTime = remember(n.createdAt, locale) { SimpleDateFormat("MMM d, h:mm a", locale).format(Date(n.createdAt)) }
    val semantic = MatreeDesign.colors
    val (icon, color) = when (n.type) {
        "LIKE" -> Icons.Filled.Favorite to semantic.interest
        "INTEREST" -> Icons.Filled.PersonAdd to MaterialTheme.colorScheme.primary
        "MATCH" -> Icons.Filled.Stars to semantic.premium
        "VIEW" -> Icons.Filled.Visibility to semantic.verified
        "MESSAGE" -> Icons.Filled.Forum to MaterialTheme.colorScheme.primary
        else -> Icons.Filled.Notifications to MaterialTheme.colorScheme.primary
    }
    val localizedTitle = when (n.type) {
        "LIKE", "INTEREST" -> t("new_interest_title", "New interest")
        "MATCH" -> t("new_mutual_match_title", "New mutual match")
        "MESSAGE" -> t("new_message_title", "New message")
        "CONTACT_REQUEST" -> t("contact_request_title", "Contact access request")
        else -> n.title
    }
    val localizedBody = when (n.type) {
        "LIKE", "INTEREST" -> t(
            "new_interest_body",
            "Someone is interested in your profile. Open the app to view it."
        )
        "MATCH" -> t(
            "new_mutual_match_body",
            "You have a new mutual match. Open the app to view the profile."
        )
        "MESSAGE" -> t(
            "new_message_body",
            "Open the app to view your message."
        )
        "CONTACT_REQUEST" -> t(
            "contact_request_body",
            "A mutual match requested permission to reveal your contact. Open Privacy & visibility to respond."
        )
        else -> n.body
    }
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("notif_${n.id}"), shape = RoundedCornerShape(MatreeDesign.radii.card), colors = CardDefaults.elevatedCardColors(containerColor = if (!n.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(MatreeDesign.spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f), modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) } }
            Spacer(Modifier.width(MatreeDesign.spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(localizedTitle, fontWeight = if (!n.isRead) FontWeight.SemiBold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                Text(localizedBody, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formattedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                val actionLabel = when (n.type) {
                    "LIKE", "INTEREST" -> t("review_interest", "Review interest")
                    "MATCH", "MESSAGE" -> t("open_chat", "Open chat")
                    "VIEW" -> t("view_profile", "View profile")
                    else -> null
                }
                actionLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (!n.isRead) Box(Modifier.size(8.dp).padding(start = 4.dp)) { Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {} }
        }
    }
}
