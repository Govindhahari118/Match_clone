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
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.components.MatreeProfileCard
import com.match.app.ui.components.MatreeProfileCardVariant
import com.match.app.ui.components.MatreeSecondaryButton
import com.match.app.ui.components.MatreeStatePanel
import com.match.app.ui.theme.MatreeDesign
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
    val receivedNotes: StateFlow<Map<String, String>> = session.firebaseUid.filterNotNull()
        .flatMapLatest { uid -> social.observeReceivedInterestNotesRemote(uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
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

    fun withdraw(targetId: Long) = viewModelScope.launch {
        if (_actions.value.busyId != null) return@launch
        val me = session.userId.first() ?: return@launch
        _actions.value = InterestActionState(busyId = targetId)
        runCatching { social.unlike(me, targetId) }
            .onSuccess { _actions.value = InterestActionState(message = "Interest withdrawn.") }
            .onFailure { _actions.value = InterestActionState(message = it.message ?: "Could not withdraw this interest.") }
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
    val receivedNotes by vm.receivedNotes.collectAsState()
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
                shape = RoundedCornerShape(MatreeDesign.radii.card)
            ) {
                Row(
                    Modifier.padding(MatreeDesign.spacing.md, MatreeDesign.spacing.sm).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
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
                Box(
                    Modifier.fillMaxSize().padding(MatreeDesign.spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    val (icon, title, message) = when (tab) {
                        InterestTab.RECEIVED -> Triple(
                            Icons.Filled.MoveToInbox,
                            t("no_interests_received", "No interests received yet"),
                            "New pending requests will appear here after server validation."
                        )
                        InterestTab.SENT -> Triple(
                            Icons.AutoMirrored.Filled.Send,
                            t("no_interests_sent", "You haven't sent any interests yet"),
                            t("interests_sent_hint", "Discover profiles and send an interest when you want to connect.")
                        )
                        InterestTab.MUTUAL -> Triple(
                            Icons.Filled.Favorite,
                            t("no_mutual_interests", "No mutual interests yet"),
                            t("interests_mutual_hint", "Mutual interests appear after both members express interest.")
                        )
                    }
                    MatreeStatePanel(title = title, message = message, icon = icon)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(MatreeDesign.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm),
                    modifier = Modifier.fillMaxSize().testTag("interests_list_${tab.name.lowercase()}")
                ) {
                    items(list, key = { it.firebaseUid.ifBlank { it.id.toString() } }) { p ->
                        InterestCard(
                            profile = p,
                            tab = tab,
                            busy = actions.busyId == p.id,
                            introNote = if (tab == InterestTab.RECEIVED) {
                                receivedNotes[p.firebaseUid].orEmpty()
                            } else "",
                            onOpen = { onOpenProfile(p.id) }, onKundli = { onCheckKundli(p.id) },
                            onChat = { onOpenChat(p.id) }, onAccept = { vm.accept(p.id) },
                            onDecline = { vm.decline(p.id) }, onWithdraw = { vm.withdraw(p.id) }
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
    profile: UserProfile,
    tab: InterestTab,
    busy: Boolean,
    introNote: String,
    onOpen: () -> Unit,
    onKundli: () -> Unit,
    onChat: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onWithdraw: () -> Unit
) {
    MatreeProfileCard(
        name = profile.displayName,
        age = profile.age,
        onClick = onOpen,
        variant = MatreeProfileCardVariant.STANDARD,
        username = profile.username,
        primaryLine = listOf(profile.city, profile.profession)
            .filter { it.isNotBlank() }
            .joinToString(" • "),
        secondaryLine = listOf(profile.religion, profile.motherTongue)
            .filter { it.isNotBlank() }
            .joinToString(" • "),
        photoModel = profile.primaryPhotoPath ?: profile.photoUrl.takeIf { it.isNotBlank() },
        isVerified = profile.isVerified,
        isPremium = profile.isPremium,
        modifier = Modifier.testTag("interest_card_${profile.id}")
    ) {
        when (tab) {
            InterestTab.RECEIVED -> {
                if (introNote.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(MatreeDesign.radii.card),
                        modifier = Modifier.fillMaxWidth().testTag("interest_intro_${profile.id}")
                    ) {
                        Column(
                            Modifier.padding(MatreeDesign.spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "Personal note",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                introNote,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
                ) {
                    MatreeSecondaryButton(
                        text = "Profile",
                        icon = Icons.Filled.Person,
                        onClick = onOpen,
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    )
                    MatreeSecondaryButton(
                        text = "Kundali",
                        icon = Icons.Filled.AutoAwesome,
                        onClick = onKundli,
                        enabled = !busy && profile.showHoroscope,
                        modifier = Modifier.weight(1f).testTag("interest_kundli_${profile.id}")
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
                ) {
                    MatreeSecondaryButton(
                        text = "Decline",
                        icon = Icons.Filled.Close,
                        onClick = onDecline,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).testTag("interest_decline_${profile.id}")
                    )
                    MatreePrimaryButton(
                        text = t("accept", "Accept"),
                        icon = Icons.Filled.Check,
                        onClick = onAccept,
                        enabled = !busy,
                        modifier = Modifier.weight(1f).testTag("interest_accept_${profile.id}")
                    )
                }
            }
            InterestTab.SENT -> {
                MatreeSecondaryButton(
                    text = "Withdraw interest",
                    icon = Icons.Filled.Undo,
                    onClick = onWithdraw,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().testTag("interest_withdraw_${profile.id}")
                )
            }
            InterestTab.MUTUAL -> {
                MatreePrimaryButton(
                    text = t("start_chatting", "Start chatting"),
                    icon = Icons.AutoMirrored.Filled.Chat,
                    onClick = onChat,
                    modifier = Modifier.fillMaxWidth().testTag("interest_chat_${profile.id}")
                )
            }
        }
    }
}
