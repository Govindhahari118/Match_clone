package com.match.app.ui.shortlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
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
import com.match.app.core.activity.ActivityStatusHelper
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.ShortlistRepository
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

enum class ShortlistSort { NAME, AGE, ACTIVE, VERIFIED }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShortlistViewModel @Inject constructor(
    private val session: SessionStore,
    private val repo: ShortlistRepository,
    private val userDao: UserDao
) : ViewModel() {
    val profiles: StateFlow<List<UserProfile>> = session.firebaseUid.filterNotNull()
        .flatMapLatest { uid -> repo.observeSavedIdsRemote(uid).map { ids -> ids.mapNotNull { userDao.findById(it)?.toProfile() } } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val count: StateFlow<Int> = profiles.map { it.size }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    private val _sort = MutableStateFlow(ShortlistSort.ACTIVE)
    val sort: StateFlow<ShortlistSort> = _sort.asStateFlow()

    val sorted: StateFlow<List<UserProfile>> = combine(profiles, _sort) { list, s ->
        when (s) {
            ShortlistSort.NAME -> list.sortedBy { it.displayName }
            ShortlistSort.AGE -> list.sortedBy { it.age }
            ShortlistSort.ACTIVE -> list.sortedByDescending { it.lastActiveAt }
            ShortlistSort.VERIFIED -> list.sortedByDescending { it.isVerified }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSort(s: ShortlistSort) { _sort.value = s }
    fun remove(targetId: Long) = viewModelScope.launch {
        val me = session.userId.first() ?: return@launch
        runCatching { repo.toggle(me, targetId) }
    }

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
        lastActiveAt = lastActiveAt, showLastActive = showLastActive, username = username
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortlistScreen(onOpenProfile: (Long) -> Unit = {}, vm: ShortlistViewModel = hiltViewModel()) {
    val profiles by vm.sorted.collectAsState()
    val count by vm.count.collectAsState()
    val sort by vm.sort.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text(t("shortlists", "Shortlists")) }, actions = { Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("$count") } }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().testTag("shortlist_screen")) {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(16.dp, 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("$count", t("saved", "Saved"))
                    StatItem(profiles.count { it.isVerified }.toString(), t("verified", "Verified"))
                    StatItem(profiles.count { it.showLastActive && ActivityStatusHelper.from(it).isOnline }.toString(), "Online")
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sort:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ShortlistSort.entries) { s ->
                        FilterChip(selected = sort == s, onClick = { vm.setSort(s) }, label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }, modifier = Modifier.testTag("sort_${s.name.lowercase()}"))
                    }
                }
            }
            if (profiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.BookmarkBorder, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Spacer(Modifier.height(12.dp))
                        Text(t("no_shortlisted_profiles", "No shortlisted profiles"), style = MaterialTheme.typography.titleMedium)
                        Text(t("shortlist_hint", "Tap the bookmark icon on any match to save them here"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().testTag("shortlist_list")) {
                    items(profiles, key = { it.firebaseUid.ifBlank { it.id.toString() } }) { p ->
                        ShortlistCard(p, onOpen = { onOpenProfile(p.id) }, onRemove = { vm.remove(p.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShortlistCard(p: UserProfile, onOpen: () -> Unit, onRemove: () -> Unit) {
    val activity = remember(p.lastActiveAt) { ActivityStatusHelper.from(p) }
    ElevatedCard(onClick = onOpen, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("shortlist_card_${p.id}")) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(p.displayName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${p.displayName}, ${p.age}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (p.isVerified) { Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Star, "Verified", Modifier.size(14.dp), tint = Color(0xFF1976D2)) }
                }
                if (p.username.isNotBlank()) Text("@${p.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("${p.city} • ${p.profession}", style = MaterialTheme.typography.bodySmall)
                Text("${p.religion} • ${p.motherTongue}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (p.showLastActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (activity.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline, modifier = Modifier.size(7.dp)) {}
                        Spacer(Modifier.width(5.dp))
                        Text(activity.label, style = MaterialTheme.typography.labelSmall, color = if (activity.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.testTag("shortlist_remove_${p.id}")) {
                Icon(Icons.Filled.Bookmark, "Remove from shortlist", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
