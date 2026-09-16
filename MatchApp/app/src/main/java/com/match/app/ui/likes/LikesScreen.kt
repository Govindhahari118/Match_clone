package com.match.app.ui.likes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.SocialRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import com.match.app.domain.model.UserProfile
import com.match.app.ui.components.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MutualMatch(val profile: UserProfile)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LikesViewModel @Inject constructor(
    private val session: SessionStore,
    private val social: SocialRepository,
    private val userDao: UserDao
) : ViewModel() {

    val mutuals: StateFlow<List<MutualMatch>> = session.userId.filterNotNull()
        .flatMapLatest { me ->
            social.observeMutual(me).map { likes ->
                likes.mapNotNull { like ->
                    userDao.findById(like.toUserId)?.let { u ->
                        MutualMatch(
                            UserProfile(
                                id = u.id,
                                email = u.email,
                                displayName = u.displayName,
                                age = u.age,
                                gender = runCatching { Gender.valueOf(u.gender) }.getOrDefault(Gender.OTHER),
                                lookingFor = runCatching { LookingFor.valueOf(u.lookingFor) }.getOrDefault(LookingFor.ANY),
                                city = u.city,
                                bio = u.bio,
                                rasi = u.rasi,
                                nakshatra = u.nakshatra,
                                hasQuestionnaire = false,
                                primaryPhotoPath = null,
                                religion = u.religion,
                                caste = u.caste,
                                motherTongue = u.motherTongue,
                                education = u.education,
                                profession = u.profession,
                                maritalStatus = u.maritalStatus,
                                heightCm = u.heightCm,
                                isVerified = u.isVerified,
                                isPremium = u.isPremium,
                                state = u.state
                            )
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val incomingCount: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { social.observeIncoming(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikesScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    onOpenChat: (Long) -> Unit = {},
    vm: LikesViewModel = hiltViewModel()
) {
    val mutuals by vm.mutuals.collectAsState()
    val incoming by vm.incomingCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mutual matches", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("likes_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag("likes_screen")) {
            if (incoming > 0) {
                Surface(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().testTag("likes_incoming_banner"),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "$incoming person${if (incoming > 1) "s" else ""} liked you. A mutual interest will appear here after you both choose to connect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (mutuals.isEmpty()) {
                EmptyState(
                    modifier = Modifier.fillMaxSize().testTag("likes_empty"),
                    icon = Icons.Filled.Favorite,
                    title = "No mutual matches yet",
                    subtitle = "When you and another member both express interest, the connection will appear here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("likes_list")
                ) {
                    items(mutuals, key = { it.profile.id }) { m ->
                        MutualCard(m, onOpenProfile, onOpenChat)
                    }
                }
            }
        }
    }
}

@Composable
private fun MutualCard(m: MutualMatch, onOpen: (Long) -> Unit, onChat: (Long) -> Unit) {
    ElevatedCard(
        onClick = { onOpen(m.profile.id) },
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().testTag("mutual_card_${m.profile.id}")
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        m.profile.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${m.profile.displayName}, ${m.profile.age}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (m.profile.isVerified) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Verified profile",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    listOf(m.profile.city, m.profile.profession).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    listOf(m.profile.religion, m.profile.motherTongue).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(
                onClick = { onChat(m.profile.id) },
                modifier = Modifier.testTag("mutual_chat_${m.profile.id}")
            ) {
                Icon(Icons.Filled.Chat, contentDescription = "Open chat")
            }
        }
    }
}
