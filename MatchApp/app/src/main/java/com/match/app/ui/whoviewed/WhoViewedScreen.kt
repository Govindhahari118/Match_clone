package com.match.app.ui.whoviewed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Star
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
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.SocialRepository
import com.match.app.data.repo.WhoViewedRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WhoViewedViewModel @Inject constructor(
    private val session: SessionStore,
    private val repo: WhoViewedRepository,
    private val userDao: UserDao,
    private val social: SocialRepository,
    private val featureAccess: com.match.app.domain.usecase.CheckFeatureAccessUseCase
) : ViewModel() {
    val viewers: StateFlow<List<UserProfile>> = session.userId.filterNotNull()
        .flatMapLatest { me -> repo.observeViewerIds(me).map { ids -> ids.mapNotNull { userDao.findById(it)?.toProfile() } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val count: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { repo.observeViewCount(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _likedIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedIds: StateFlow<Set<Long>> = _likedIds.asStateFlow()

    val hasAccess: StateFlow<Boolean> = session.subscriptionPlan
        .map { featureAccess.checkSync(it, com.match.app.domain.subscription.SubscriptionPlans.Feature.SEE_WHO_VIEWED).granted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun sendInterest(targetId: Long) = viewModelScope.launch {
        val me = session.userId.first() ?: return@launch
        val nowLiked = social.toggleLike(me, targetId)
        _likedIds.value = if (nowLiked) _likedIds.value + targetId else _likedIds.value - targetId
    }

    private fun com.match.app.data.local.entity.UserEntity.toProfile() = UserProfile(
        id = id, email = email, displayName = displayName, age = age,
        gender = runCatching { Gender.valueOf(gender) }.getOrDefault(Gender.OTHER),
        lookingFor = runCatching { LookingFor.valueOf(lookingFor) }.getOrDefault(LookingFor.ANY),
        city = city, bio = bio, rasi = rasi, nakshatra = nakshatra,
        hasQuestionnaire = false, primaryPhotoPath = null,
        religion = religion, caste = caste, motherTongue = motherTongue,
        education = education, profession = profession,
        maritalStatus = maritalStatus, heightCm = heightCm,
        isVerified = isVerified, isPremium = isPremium, state = state
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoViewedScreen(
    onOpenProfile: (Long) -> Unit = {},
    onBack: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    vm: WhoViewedViewModel = hiltViewModel()
) {
    val viewers by vm.viewers.collectAsState()
    val count by vm.count.collectAsState()
    val likedIds by vm.likedIds.collectAsState()
    val hasAccess by vm.hasAccess.collectAsState()
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(hasAccess) { if (!hasAccess) showPaywall = true }

    if (showPaywall && !hasAccess) {
        com.match.app.ui.common.PaywallSheet(
            feature = com.match.app.domain.subscription.SubscriptionPlans.Feature.SEE_WHO_VIEWED,
            minimumPlan = com.match.app.domain.subscription.SubscriptionPlans.Plan.SILVER_3M,
            onUpgrade = onUpgrade,
            onDismiss = { showPaywall = false }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(t("who_viewed_me", "Who Viewed Me")) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("whoviewed_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            actions = {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        "$count views",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        )
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().testTag("who_viewed_screen")) {
            if (viewers.isNotEmpty()) {
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp, 12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WhoViewedStat("$count", t("total_views", "Total Views"))
                        WhoViewedStat(viewers.count { it.isVerified }.toString(), t("verified", "Verified"))
                        WhoViewedStat(viewers.count { it.isPremium }.toString(), t("premium", "Premium"))
                    }
                }
            }
            if (viewers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.RemoveRedEye, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Spacer(Modifier.height(12.dp))
                        Text(t("no_profile_views", "No profile views yet"), style = MaterialTheme.typography.titleMedium)
                        Text(t("who_viewed_hint", "When someone views your profile it will appear here"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(viewers, key = { it.id }) { p ->
                        ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Text(p.displayName.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${p.displayName}, ${p.age}", fontWeight = FontWeight.SemiBold)
                                            if (p.isVerified) { Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Star, null, Modifier.size(14.dp), tint = Color(0xFF1976D2)) }
                                        }
                                        Text("${p.city} • ${p.profession}", style = MaterialTheme.typography.bodySmall)
                                        Text("${p.religion} • ${p.motherTongue}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onOpenProfile(p.id) }, modifier = Modifier.weight(1f).testTag("whoviewed_view_${p.id}")) {
                                        Icon(Icons.Filled.RemoveRedEye, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(t("view", "View"))
                                    }
                                    Button(
                                        onClick = { vm.sendInterest(p.id) },
                                        modifier = Modifier.weight(1f).testTag("whoviewed_interest_${p.id}"),
                                        colors = if (p.id in likedIds) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(if (p.id in likedIds) t("interest_sent", "Sent!") else t("send_interest", "Interest"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhoViewedStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
