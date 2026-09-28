@file:Suppress("UNUSED_PARAMETER")
package com.match.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.NotificationRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.repo.ShortlistRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeActivityUi(
    val profile: UserProfile? = null,
    val mode: MatchMode = MatchMode.ADVANCED,
    val unreadNotif: Int = 0,
    val pendingInterests: Int = 0,
    val shortlistCount: Int = 0,
    val mutualCount: Int = 0,
    val hasQuestionnaire: Boolean = false,
    val newMutualName: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val notifRepo: NotificationRepository,
    private val social: SocialRepository,
    private val shortlistRepo: ShortlistRepository
) : ViewModel() {

    private var lastMutualCount = 0

    /** Network state is authoritative; Room is only the presentation/offline cache. */
    val ui: StateFlow<HomeActivityUi> = combine(session.userId, session.firebaseUid) { localId, firebaseUid ->
        localId to firebaseUid
    }.flatMapLatest { (me, firebaseUid) ->
        if (me == null || firebaseUid.isNullOrBlank()) {
            flowOf(HomeActivityUi())
        } else {
            combine(
                notifRepo.observeUnreadCount(me),
                social.observeReceivedInterestsRemote(firebaseUid),
                shortlistRepo.observeSavedIdsRemote(firebaseUid),
                social.observeMutualIdsRemote(firebaseUid),
                session.hasQuestionnaire
            ) { notif, incomingIds, savedIds, mutualIds, quizDone ->
                val profile = auth.currentProfile(me)
                val mutualCount = mutualIds.size
                val newMutualName = if (mutualCount > lastMutualCount && lastMutualCount > 0) "Someone" else null
                lastMutualCount = mutualCount
                HomeActivityUi(
                    profile = profile,
                    mode = MatchMode.ADVANCED,
                    unreadNotif = notif,
                    pendingInterests = incomingIds.size,
                    shortlistCount = savedIds.size,
                    mutualCount = mutualCount,
                    hasQuestionnaire = quizDone,
                    newMutualName = newMutualName
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeActivityUi())

    val mode: StateFlow<MatchMode> = session.mode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchMode.ADVANCED)

    fun setMode(m: MatchMode) = viewModelScope.launch { session.setMode(m) }
    fun dismissMutualCelebration() = Unit
}

@Composable
fun HomeScreen(
    onGoMatches:           () -> Unit = {},
    onGoQuiz:              () -> Unit = {},
    onGoStories:           () -> Unit = {},
    onGoPricing:           () -> Unit = {},
    onGoInterests:         () -> Unit = {},
    onGoNotifications:     () -> Unit = {},
    onGoShortlists:        () -> Unit = {},
    onGoRegions:           () -> Unit = {},
    onGoCircles:           () -> Unit = {},
    onGoMessages:          () -> Unit = {},
    onGoProfile:           () -> Unit = {},
    onGoVerification:      () -> Unit = {},
    onGoKundli:            () -> Unit = {},
    onGoWhoViewed:         () -> Unit = {},
    onGoFamily:            () -> Unit = {},
    onGoHelp:              () -> Unit = {},
    onGoSecondMarriage:    () -> Unit = {},
    onGoCompatibilityQuiz: () -> Unit = {},
    onGoAssisted:          () -> Unit = {},
    onGoVirtualMeet:       () -> Unit = {},
    onGoBioGen:            () -> Unit = {},
    onGoPhotoEditor:       () -> Unit = {},
    onGoCounselling:       () -> Unit = {},
    onGoGuides:            () -> Unit = {},
    onGoBoost:             () -> Unit = {},
    onGoVideoProfile:      () -> Unit = {},
    onGoRecentlyJoined:    () -> Unit = {},
    onGoTestimonials:      () -> Unit = {},
    onGoSwipeDiscover:     () -> Unit = {},
    onGoCommunityBrowse:   () -> Unit = {},
    onGoLiveEvents:        () -> Unit = {},
    onGoBgCheck:           () -> Unit = {},
    onGoSecureCall:        () -> Unit = {},
    onGoPrivacyDash:       () -> Unit = {},
    onGoAIInsights:        () -> Unit = {},
    onGoAnalytics:         () -> Unit = {},
    onGoWeddingPlanner:    () -> Unit = {},
    onGoAdvHoroscope:      () -> Unit = {},
    onGoDailyRewards:      () -> Unit = {},
    onGoNearby:            () -> Unit = {},
    onGoNRIMatch:          () -> Unit = {},
    onGoSafetyCenter:       () -> Unit = {},
    onGoTimeline:           () -> Unit = {},
    onGoReferral:           () -> Unit = {},
    onGoMuhurat:            () -> Unit = {},
    onGoDeepCompat:         () -> Unit = {},
    onGoWizard:             () -> Unit = {},
    onOpenProfile:          (Long) -> Unit = {},
    vm: HomeViewModel = hiltViewModel()
) {
    HomeLauncherScreen(
        onGoMatches = onGoMatches,
        onGoQuiz = onGoQuiz,
        onGoPricing = onGoPricing,
        onGoInterests = onGoInterests,
        onGoNotifications = onGoNotifications,
        onGoShortlists = onGoShortlists,
        onGoMessages = onGoMessages,
        onGoProfile = onGoProfile,
        onGoVerification = onGoVerification,
        onGoKundli = onGoKundli,
        onGoPrivacyDash = onGoPrivacyDash,
        onGoNearby = onGoNearby,
        vm = vm
    )
}

@Composable
private fun ActivityStat(icon: ImageVector, label: String, value: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ModeChip(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick,
        leadingIcon = { Icon(icon, null, Modifier.size(14.dp)) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier)
}

@Composable
private fun QuickCard(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
