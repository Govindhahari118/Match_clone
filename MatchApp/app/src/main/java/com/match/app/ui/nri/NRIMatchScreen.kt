package com.match.app.ui.nri

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NriDiscoveryUi(
    val loading: Boolean = true,
    val country: String = "",
    val results: List<MatchResult> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class NriDiscoveryViewModel @Inject constructor(
    private val session: SessionStore,
    private val matchingRepository: MatchingRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(NriDiscoveryUi())
    val ui: StateFlow<NriDiscoveryUi> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun setCountry(value: String) {
        _ui.update { it.copy(country = value.take(80), error = null) }
    }

    fun applyCountry() = refresh()

    fun refresh() = viewModelScope.launch {
        val userId = session.userId.first()
        if (userId == null) {
            _ui.update { it.copy(loading = false, error = "Sign in to use NRI discovery.") }
            return@launch
        }
        val country = _ui.value.country.trim()
        _ui.update { it.copy(loading = true, error = null) }
        runCatching {
            matchingRepository.recommendations(
                seekerId = userId,
                mode = MatchMode.ADVANCED,
                filter = MatchFilter(
                    nriOnly = true,
                    countryOfResidence = country,
                    withPhotoOnly = false
                )
            )
        }.onSuccess { results ->
            _ui.update { it.copy(loading = false, results = results) }
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    loading = false,
                    results = emptyList(),
                    error = error.message?.take(180) ?: "Unable to load NRI profiles."
                )
            }
        }
    }
}

/**
 * Real NRI discovery backed by the same server-authorized candidate endpoint as Discover.
 * No synthetic members, inventory totals or verification percentages are permitted here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NRIMatchScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: NriDiscoveryViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NRI discovery") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nri_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh NRI results")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().testTag("nri_screen")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Real NRI profiles only",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "Results respect reciprocal partner preferences, blocks, privacy, account status and server authorization. Leave country blank to browse all eligible NRI profiles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = ui.country,
                    onValueChange = vm::setCountry,
                    label = { Text("Country of residence") },
                    placeholder = { Text("Canada, United States, UAE…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.Button(
                    onClick = vm::applyCountry,
                    enabled = !ui.loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply NRI filter")
                }
            }

            when {
                ui.loading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Loading authorized NRI profiles…",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ui.error != null -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        ui.error.orEmpty(),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                ui.results.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No eligible NRI profiles found")
                    Text(
                        "Try another country or clear the country filter. Matree does not generate placeholder profiles when inventory is empty.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("nri_results"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "${ui.results.size} profiles in this authorized result set",
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProfile(profile.id) }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        profile.displayName.ifBlank { "Member" },
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (profile.isVerified) {
                                        Icon(
                                            Icons.Filled.Verified,
                                            contentDescription = "Verified",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    listOfNotNull(
                                        profile.age.takeIf { it > 0 }?.toString(),
                                        profile.city.takeIf { it.isNotBlank() },
                                        profile.countryOfResidence.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (profile.profession.isNotBlank()) {
                                    Text(profile.profession, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "Compatibility ${(result.combinedScore * 100).toInt().coerceIn(0, 100)}%",
                                    style = MaterialTheme.typography.labelMedium,
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
