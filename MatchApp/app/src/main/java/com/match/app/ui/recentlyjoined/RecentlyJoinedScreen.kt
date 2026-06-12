package com.match.app.ui.recentlyjoined

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.match.app.ui.i18n.t

@HiltViewModel
class RecentlyJoinedViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles = _profiles.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            _loading.value = true
            // Get the most recently created users (highest IDs, excluding self)
            val entities = userDao.allExcluding(uid).takeLast(30).reversed()
            val profileList = mutableListOf<UserProfile>()
            for (entity in entities) {
                auth.currentProfile(entity.id)?.let { profileList.add(it) }
            }
            _profiles.value = profileList
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyJoinedScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: RecentlyJoinedViewModel = hiltViewModel()
) {
    val profiles by vm.profiles.collectAsState()
    val loading by vm.loading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("recently_joined", "Recently Joined")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FiberNew, null, Modifier.size(28.dp), tint = Color(0xFF1565C0))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "${profiles.size} new profiles",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D47A1)
                                )
                                Text(
                                    "Browse the newest members on the platform",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }
                    }
                }

                items(profiles, key = { it.id }) { profile ->
                    RecentProfileCard(profile = profile, onClick = { onOpenProfile(profile.id) })
                }

                if (profiles.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text(t("no_new_profiles", "No new profiles yet"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentProfileCard(profile: UserProfile, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar placeholder
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        profile.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (profile.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Verified, null, Modifier.size(16.dp), tint = Color(0xFF1976D2))
                    }
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE3F2FD)) {
                        Text(
                            "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1565C0),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    "${profile.age} yrs • ${profile.city} • ${profile.profession}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${profile.religion} • ${profile.caste.ifBlank { profile.motherTongue }} • ${profile.education}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Profile ID
                Text(
                    "ID: M${profile.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
