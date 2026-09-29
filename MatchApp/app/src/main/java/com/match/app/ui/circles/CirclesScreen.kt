package com.match.app.ui.circles

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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
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
import com.match.app.data.remote.CommunityGroupDto
import com.match.app.data.repo.CatalogRepository
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

data class CirclesUiState(
    val loading: Boolean = true,
    val groups: List<CommunityGroupDto> = emptyList(),
    val selected: CommunityGroupDto? = null,
    val results: List<MatchResult> = emptyList(),
    val loadingResults: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CirclesViewModel @Inject constructor(
    private val session: SessionStore,
    private val catalog: CatalogRepository,
    private val matchingRepo: MatchingRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(CirclesUiState())
    val ui = _ui.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        catalog.fetchCommunityGroups()
            .onSuccess { raw ->
                val groups = raw.filter { group ->
                    group.name.isNotBlank() && listOf(
                        group.religion,
                        group.caste,
                        group.city,
                        group.motherTongue
                    ).any { !it.isNullOrBlank() }
                }
                _ui.update { it.copy(loading = false, groups = groups) }
            }
            .onFailure {
                _ui.update {
                    it.copy(
                        loading = false,
                        groups = emptyList(),
                        error = "Community catalogue is unavailable. Matree does not substitute fabricated circles."
                    )
                }
            }
    }

    fun select(group: CommunityGroupDto) = viewModelScope.launch {
        val userId = session.userId.first()
        if (userId == null) {
            _ui.update { it.copy(error = "Sign in to browse community matches.") }
            return@launch
        }
        _ui.update {
            it.copy(selected = group, results = emptyList(), loadingResults = true, error = null)
        }
        runCatching {
            matchingRepo.recommendations(
                seekerId = userId,
                mode = MatchMode.ADVANCED,
                filter = MatchFilter(
                    religion = group.religion.orEmpty(),
                    caste = group.caste.orEmpty(),
                    city = group.city.orEmpty(),
                    motherTongue = group.motherTongue.orEmpty()
                )
            )
        }.onSuccess { results ->
            _ui.update { it.copy(loadingResults = false, results = results) }
        }.onFailure {
            _ui.update {
                it.copy(
                    loadingResults = false,
                    results = emptyList(),
                    error = "Unable to load authorized profiles for this filter."
                )
            }
        }
    }

    fun clearSelection() {
        _ui.update { it.copy(selected = null, results = emptyList(), error = null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirclesScreen(
    onBack: () -> Unit = {},
    onGoMatches: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: CirclesViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.selected?.name ?: "Community Circles") },
                navigationIcon = {
                    IconButton(
                        onClick = { if (ui.selected != null) vm.clearSelection() else onBack() },
                        modifier = Modifier.testTag("circles_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (ui.selected == null) {
                        IconButton(onClick = vm::refresh, enabled = !ui.loading) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh community catalogue")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            ui.selected != null -> CircleResults(
                group = ui.selected!!,
                loading = ui.loadingResults,
                results = ui.results,
                error = ui.error,
                onOpenProfile = onOpenProfile,
                modifier = Modifier.padding(padding)
            )

            ui.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).testTag("circles_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "Circles are server-provided browse filters. Member counts, verification " +
                            "claims and premium status are never invented from bundled data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ui.error?.let { message ->
                    item {
                        Text(message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = vm::refresh) { Text("Retry") }
                    }
                }
                if (ui.groups.isEmpty() && ui.error == null) {
                    item {
                        Text(
                            "No community filters are currently published.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(ui.groups, key = { it.id ?: it.name }) { group ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { vm.select(group) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(group.name, fontWeight = FontWeight.SemiBold)
                                group.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    explicitFilterSummary(group),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleResults(
    group: CommunityGroupDto,
    loading: Boolean,
    results: List<MatchResult>,
    error: String?,
    onOpenProfile: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> LazyColumn(
            modifier = modifier.fillMaxSize().testTag("circle_results"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Current authorized results for ${explicitFilterSummary(group)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (results.isEmpty() && error == null) {
                item { Text("No eligible profiles found for this filter.") }
            }
            items(results, key = { it.user.firebaseUid.ifBlank { it.user.id.toString() } }) { result ->
                val profile = result.user
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenProfile(profile.id) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(profile.displayName.ifBlank { "Member" }, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(
                                    profile.age.takeIf { it > 0 }?.toString(),
                                    profile.city.takeIf { it.isNotBlank() }
                                ).filterNotNull().joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (profile.isVerified) {
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

private fun explicitFilterSummary(group: CommunityGroupDto): String =
    listOfNotNull(
        group.religion?.takeIf { it.isNotBlank() }?.let { "Religion: $it" },
        group.caste?.takeIf { it.isNotBlank() }?.let { "Community: $it" },
        group.city?.takeIf { it.isNotBlank() }?.let { "City: $it" },
        group.motherTongue?.takeIf { it.isNotBlank() }?.let { "Language: $it" }
    ).joinToString(" • ")
