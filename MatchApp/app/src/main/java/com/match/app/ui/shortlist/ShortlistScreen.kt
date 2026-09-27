package com.match.app.ui.shortlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
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
import com.match.app.core.activity.ActivityStatusHelper
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.ShortlistRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.Gender
import com.match.app.domain.model.LookingFor
import com.match.app.domain.model.UserProfile
import com.match.app.ui.components.MatreeProfileCard
import com.match.app.ui.components.MatreeProfileCardVariant
import com.match.app.ui.components.MatreeStatePanel
import com.match.app.ui.theme.MatreeDesign
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ShortlistSort { NAME, AGE, VERIFIED }

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
    private val _sort = MutableStateFlow(ShortlistSort.NAME)
    val sort: StateFlow<ShortlistSort> = _sort.asStateFlow()

    val sorted: StateFlow<List<UserProfile>> = combine(profiles, _sort) { list, s ->
        when (s) {
            ShortlistSort.NAME -> list.sortedBy { it.displayName }
            ShortlistSort.AGE -> list.sortedBy { it.age }
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
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = MatreeDesign.spacing.md, vertical = MatreeDesign.spacing.xs),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(MatreeDesign.radii.card)
            ) {
                Row(
                    Modifier.padding(MatreeDesign.spacing.md, MatreeDesign.spacing.sm).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("$count", t("saved", "Saved"))
                    StatItem(profiles.count { it.isVerified }.toString(), t("verified", "Verified"))
                    StatItem(profiles.count { it.isPremium }.toString(), t("premium", "Premium"))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = MatreeDesign.spacing.md, vertical = MatreeDesign.spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
            ) {
                Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sort:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                    items(ShortlistSort.entries) { s ->
                        FilterChip(selected = sort == s, onClick = { vm.setSort(s) }, label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }, modifier = Modifier.testTag("sort_${s.name.lowercase()}"))
                    }
                }
            }
            if (profiles.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(MatreeDesign.spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    MatreeStatePanel(
                        title = t("no_shortlisted_profiles", "No shortlisted profiles"),
                        message = t("shortlist_hint", "Tap the bookmark icon on any match to save them here"),
                        icon = Icons.Filled.BookmarkBorder
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(MatreeDesign.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm),
                    modifier = Modifier.fillMaxSize().testTag("shortlist_list")
                ) {
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
    MatreeProfileCard(
        name = p.displayName,
        age = p.age,
        onClick = onOpen,
        variant = MatreeProfileCardVariant.STANDARD,
        username = p.username,
        primaryLine = listOf(p.city, p.profession).filter { it.isNotBlank() }.joinToString(" • "),
        secondaryLine = listOf(p.religion, p.motherTongue).filter { it.isNotBlank() }.joinToString(" • "),
        photoModel = p.primaryPhotoPath ?: p.photoUrl.takeIf { it.isNotBlank() },
        isVerified = p.isVerified,
        isPremium = p.isPremium,
        modifier = Modifier.testTag("shortlist_card_${p.id}")
    ) {
        OutlinedButton(
            onClick = onRemove,
            modifier = Modifier.fillMaxWidth().testTag("shortlist_remove_${p.id}")
        ) {
            Icon(Icons.Filled.Bookmark, "Remove from shortlist")
            Spacer(Modifier.width(MatreeDesign.spacing.xs))
            Text("Remove from shortlist")
        }
    }
}
