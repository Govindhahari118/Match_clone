package com.match.app.ui.recentlyjoined

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.MatchingRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.MatchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecentlyJoinedUi(
    val loading: Boolean = true,
    val results: List<MatchResult> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class RecentlyJoinedViewModel @Inject constructor(
    private val session: SessionStore,
    private val matching: MatchingRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(RecentlyJoinedUi())
    val ui = _ui.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val userId = session.userId.first()
        if (userId == null) {
            _ui.value = RecentlyJoinedUi(loading = false, error = "Sign in to view recently joined members.")
            return@launch
        }

        _ui.update { it.copy(loading = true, error = null) }
        runCatching {
            matching.recommendations(
                seekerId = userId,
                mode = MatchMode.ADVANCED,
                filter = MatchFilter(
                    recentlyJoinedDays = 30,
                    withPhotoOnly = false
                )
            )
        }.onSuccess { results ->
            _ui.value = RecentlyJoinedUi(loading = false, results = results)
        }.onFailure { error ->
            _ui.value = RecentlyJoinedUi(
                loading = false,
                error = error.message?.take(180) ?: "Unable to load recently joined members."
            )
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
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Joined") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh, enabled = !ui.loading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh recently joined members")
                    }
                }
            )
        }
    ) { padding ->
        when {
            ui.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ui.error != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            ui.results.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FiberNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "No eligible members joined in the last 30 days",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(
                        "This view uses the server-authoritative profile creation timestamp and normal discovery privacy rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).testTag("recently_joined_results"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Joined within the last 30 days • authorized discovery results",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(
                    ui.results,
                    key = { it.user.firebaseUid.ifBlank { it.user.id.toString() } }
                ) { result ->
                    val profile = result.user
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenProfile(profile.id) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    profile.displayName.ifBlank { "Member" },
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    listOfNotNull(
                                        profile.age.takeIf { it > 0 }?.toString(),
                                        profile.city.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (profile.profession.isNotBlank()) {
                                    Text(profile.profession, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (profile.isVerified) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = "Verified",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
