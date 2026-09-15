package com.match.app.ui.interests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.core.activity.ActivityStatusHelper
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.SocialRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import com.match.app.domain.model.UserProfile
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InterestTab { RECEIVED, SENT, MUTUAL }

data class InterestActionState(val busyId: Long? = null, val message: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InterestsViewModel @Inject constructor(
    private val session: SessionStore,
    private val social: SocialRepository,
    private val userDao: UserDao
) : ViewModel() {
    private val _tab = MutableStateFlow(InterestTab.RECEIVED)
    val tab: StateFlow<InterestTab> = _tab.asStateFlow()
    private val _actions = MutableStateFlow(InterestActionState())
    val actions: StateFlow<InterestActionState> = _actions.asStateFlow()

    val received: StateFlow<List<UserProfile>> = session.firebaseUid.filterNotNull()
        .flatMapLatest { uid -> social.observeReceivedInterestsRemote(uid).map { ids -> ids.mapNotNull { userDao.findById(it)?.toProfile() } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val sent: StateFlow<List<UserProfile>> = session.firebaseUid.filterNotNull()
        .flatMapLatest { uid -> social.observeSentInterestsRemote(uid).map { ids -> ids.mapNotNull { userDao.findById(it)?.toProfile() } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val mutual: StateFlow<List<UserProfile>> = session.firebaseUid.filterNotNull()
        .flatMapLatest { uid -> social.observeMutualIdsRemote(uid).map { ids -> ids.mapNotNull { userDao.findById(it)?.toProfile() } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setTab(t: InterestTab) { _tab.value = t }

    fun accept(targetId: Long) = viewModelScope.launch {
        if (_actions.value.busyId != null) return@launch
        val me = session.userId.first() ?: return@launch
        _actions.value = InterestActionState(busyId = targetId)
        runCatching { social.like(me, targetId) }
            .onSuccess { _actions.value = InterestActionState(message = "Interest accepted. You are now matched.") }
            .onFailure { _actions.value = InterestActionState(message = it.message ?: "Could not accept this request.") }
    }

    fun decline(targetId: Long) = viewModelScope.launch {
        if (_actions.value.busyId != null) return@launch
        val me = session.userId.first() ?: return@launch
        _actions.value = InterestActionState(busyId = targetId)
        runCatching { social.declineIncoming(me, targetId) }
            .onSuccess { _actions.value = InterestActionState(message = "Request declined.") }
            .onFailure { _actions.value = InterestActionState(message = it.message ?: "Could not decline this request.") }
    }

    fun clearMessage() { _actions.update { it.copy(message = null) } }

    private fun com.match.app.data.local.entity.UserEntity.toProfile() = UserProfile(
        id = id, firebaseUid = firebaseUid, email = email, displayName = displayName, age = age,
        gender = runCatching { Gender.valueOf(gender) }.getOrDefault(Gender.OTHER),
        lookingFor = runCatching { LookingFor.valueOf(lookingFor) }.getOrDefault(LookingFor.ANY),
        city = city, bio = bio, rasi = rasi, nakshatra = nakshatra, hasQuestionnaire = false,
        primaryPhotoPath = photoUrl.ifBlank { null }, religion = religion, caste = caste,
        motherTongue = motherTongue, education = education, profession = profession,
        maritalStatus = maritalStatus, heightCm = heightCm, isVerified = isVerified, isPremium = isPremium,
        state = state, photoUrl = photoUrl, verificationLevel = verificationLevel,
        subscriptionPlan = subscriptionPlan, subscriptionExpiry = subscriptionExpiry,
        showHoroscope = showHoroscope, lastActiveAt = lastActiveAt, showLastActive = showLastActive,
        username = username
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestsScreen(
    onOpenProfile: (Long) -> Unit = {},
    onCheckKundli: (Long) -> Unit = {},
    onOpenChat: (Long) -> Unit = {},
    vm: InterestsViewModel = hiltViewModel()
) {
    val tab by vm.tab.collectAsState()
    val received by vm.received.collectAsState()
    val sent by vm.sent.collectAsState()
    val mutual by vm.mutual.collectAsState()
    val actions by vm.actions.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(actions.message) {
        actions.message?.let { snackbar.showSnackbar(it); vm.clearMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(t("interests", "Interests")) },
                actions = {
                    if (received.isNotEmpty()) {
                        BadgedBox(
                            badge = { Badge(containerColor = MaterialTheme.colorScheme.error) { Text("${received.size}") } },
                            modifier = Modifier.padding(end = 16.dp)
                        ) { Icon(Icons.Filled.MoveToInbox, contentDescription = "Pending interests") }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().testTag("interests_screen")) {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(16.dp, 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    InterestStatItem("${received.size}", t("received", "Received"), MaterialTheme.colorScheme.error)
                    InterestStatItem("${mutual.size}", t("mutual", "Mutual"), MaterialTheme.colorScheme.primary)
                    InterestStatItem("${sent.size}", t("sent", "Sent"), MaterialTheme.colorScheme.tertiary)
                }
            }
            TabRow(selectedTabIndex = tab.ordinal) {
                InterestTab.entries.forEach { interestTab ->
                    Tab(
                        selected = tab == interestTab,
                        onClick = { vm.setTab(interestTab) },
                        text = {
                            val count = when (interestTab) {
                                InterestTab.RECEIVED -> received.size
                                InterestTab.SENT -> sent.size
                                InterestTab.MUTUAL -> mutual.size
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(interestTab.name.lowercase().replaceFirstChar { it.uppercase() })
                                if (count > 0) Badge { Text("$count") }
                            }
                        }
                    )
                }
            }

            val list = when (tab) {
                InterestTab.RECEIVED -> received
                InterestTab.SENT -> sent
                InterestTab.MUTUAL -> mutual
            }
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val (icon, msg) = when (tab) {
                            InterestTab.RECEIVED -> Icons.Filled.MoveToInbox to t("no_interests_received", "No interests received yet")
                            InterestTab.SENT -> Icons.AutoMirrored.Filled.Send to t("no_interests_sent", "You haven't sent any interests yet")
                            InterestTab.MUTUAL -> Icons.Filled.Favorite to t("no_mutual_interests", "No mutual interests yet")
                        }
                        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Text(msg, style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (tab) {
                                InterestTab.RECEIVED -> "Review their profile and Kundali before accepting if you want."
                                InterestTab.SENT -> t("interests_sent_hint", "Like someone from Discover to send an interest")
                                InterestTab.MUTUAL -> t("interests_mutual_hint", "Mutual interests appear when you both like each other")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("interests_list_${tab.name.lowercase()}")
                ) {
                    items(list, key = { it.firebaseUid.ifBlank { it.id.toString() } }) { p ->
                        InterestCard(
                            profile = p, tab = tab, busy = actions.busyId == p.id,
                            onOpen = { onOpenProfile(p.id) }, onKundli = { onCheckKundli(p.id) },
                            onChat = { onOpenChat(p.id) }, onAccept = { vm.accept(p.id) }, onDecline = { vm.decline(p.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterestStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InterestCard(
    profile: UserProfile, tab: InterestTab, busy: Boolean,
    onOpen: () -> Unit, onKundli: () -> Unit, onChat: () -> Unit,
    onAccept: () -> Unit, onDecline: () -> Unit
) {
    val activity = remember(profile.lastActiveAt) { ActivityStatusHelper.from(profile) }
    ElevatedCard(onClick = onOpen, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("interest_card_${profile.id}")) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val gradColors = remember(profile.id) {
                    val palette = listOf(
                        listOf(Color(0xFFE91E63), Color(0xFFFF5722)), listOf(Color(0xFF9C27B0), Color(0xFF3F51B5)),
                        listOf(Color(0xFF009688), Color(0xFF4CAF50)), listOf(Color(0xFF1976D2), Color(0xFF00BCD4)),
                        listOf(Color(0xFF795548), Color(0xFF607D8B))
                    )
                    palette[(profile.id % palette.size).toInt()]
                }
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(gradColors)), contentAlignment = Alignment.Center) {
                    Text(profile.displayName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${profile.displayName}, ${profile.age}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (profile.isVerified) Icon(Icons.Filled.Verified, "Verified", Modifier.size(14.dp), tint = Color(0xFF1976D2))
                        if (profile.isPremium) Icon(Icons.Filled.Star, "Premium", Modifier.size(14.dp), tint = Color(0xFFFFB300))
                    }
                    if (profile.username.isNotBlank()) Text("@${profile.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("${profile.city} • ${profile.profession}", style = MaterialTheme.typography.bodySmall)
                    Text("${profile.religion} • ${profile.motherTongue}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (profile.showLastActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = if (activity.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline, modifier = Modifier.size(7.dp)) {}
                            Spacer(Modifier.width(5.dp))
                            Text(activity.label, style = MaterialTheme.typography.labelSmall, color = if (activity.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (tab == InterestTab.RECEIVED) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onOpen, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Person, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Profile")
                    }
                    OutlinedButton(onClick = onKundli, enabled = !busy && profile.showHoroscope, modifier = Modifier.weight(1f).testTag("interest_kundli_${profile.id}")) {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Kundali")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDecline, enabled = !busy, modifier = Modifier.weight(1f).testTag("interest_decline_${profile.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Icon(Icons.Filled.Close, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Decline") }
                    Button(onClick = onAccept, enabled = !busy, modifier = Modifier.weight(1f).testTag("interest_accept_${profile.id}")) {
                        if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text(t("accept", "Accept"))
                    }
                }
            } else if (tab == InterestTab.MUTUAL) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onChat, modifier = Modifier.fillMaxWidth().testTag("interest_chat_${profile.id}")) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(t("start_chatting", "Start chatting"))
                }
            }
        }
    }
}
