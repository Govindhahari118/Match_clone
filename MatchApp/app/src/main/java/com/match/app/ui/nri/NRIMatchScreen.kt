package com.match.app.ui.nri

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class NRICountry(val flag: String, val name: String)

private val NRI_COUNTRIES = listOf(
    NRICountry("🇺🇸", "United States"),
    NRICountry("🇬🇧", "United Kingdom"),
    NRICountry("🇨🇦", "Canada"),
    NRICountry("🇦🇺", "Australia"),
    NRICountry("🇦🇪", "United Arab Emirates"),
    NRICountry("🇸🇬", "Singapore"),
    NRICountry("🇩🇪", "Germany"),
    NRICountry("🇳🇿", "New Zealand"),
    NRICountry("🇸🇦", "Saudi Arabia"),
    NRICountry("🇰🇼", "Kuwait"),
    NRICountry("🇶🇦", "Qatar"),
    NRICountry("🇴🇲", "Oman"),
    NRICountry("🇮🇪", "Ireland"),
    NRICountry("🇳🇱", "Netherlands")
)

@HiltViewModel
class NRIMatchViewModel @Inject constructor(
    private val session: SessionStore
) : ViewModel() {
    private val _applying = MutableStateFlow(false)
    val applying = _applying.asStateFlow()

    fun openDiscovery(
        country: String,
        verifiedOnly: Boolean,
        willingToRelocate: Boolean,
        onReady: () -> Unit
    ) = viewModelScope.launch {
        _applying.value = true
        session.setFilter(
            MatchFilter(
                countryOfResidence = country,
                nriOnly = true,
                nriStatus = "only",
                verifiedOnly = verifiedOnly,
                willingToRelocate = willingToRelocate
            )
        )
        _applying.value = false
        onReady()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NRIMatchScreen(
    onBack: () -> Unit = {},
    onBrowseResults: () -> Unit = {},
    vm: NRIMatchViewModel = hiltViewModel()
) {
    val applying by vm.applying.collectAsState()
    var selectedCountry by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var verifiedOnly by rememberSaveable { mutableStateOf(false) }
    var willingToRelocate by rememberSaveable { mutableStateOf(false) }

    val filteredCountries = remember(searchQuery) {
        NRI_COUNTRIES.filter {
            searchQuery.isBlank() || it.name.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NRI discovery") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nri_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize().testTag("nri_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Search real NRI profiles",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Matree does not display invented profile counts or demo members here. Your selections are applied to the same server-authoritative Discover pipeline used by the rest of the app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(60) },
                    label = { Text("Find a country") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear country search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("nri_country_search")
                )
            }

            item {
                Text(
                    "Country of residence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (selectedCountry.isBlank()) "Any country outside India" else selectedCountry,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                FilterChip(
                    selected = selectedCountry.isBlank(),
                    onClick = { selectedCountry = "" },
                    label = { Text("Any NRI country") },
                    leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null) }
                )
            }

            items(filteredCountries, key = { it.name }) { country ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        selectedCountry = if (selectedCountry == country.name) "" else country.name
                    }
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(country.flag, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        Text(country.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        RadioButton(
                            selected = selectedCountry == country.name,
                            onClick = {
                                selectedCountry = if (selectedCountry == country.name) "" else country.name
                            }
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
                Text(
                    "Result requirements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Verified profiles only", fontWeight = FontWeight.Medium)
                        Text(
                            "Require Matree's server-authoritative verification flag.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = verifiedOnly,
                        onCheckedChange = { verifiedOnly = it },
                        enabled = !applying
                    )
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Willing to relocate", fontWeight = FontWeight.Medium)
                        Text(
                            "Require profiles that explicitly marked willingness to relocate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = willingToRelocate,
                        onCheckedChange = { willingToRelocate = it },
                        enabled = !applying
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        vm.openDiscovery(
                            country = selectedCountry,
                            verifiedOnly = verifiedOnly,
                            willingToRelocate = willingToRelocate,
                            onReady = onBrowseResults
                        )
                    },
                    enabled = !applying,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .testTag("nri_browse_results")
                ) {
                    if (applying) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (applying) "Applying filters…" else "Browse live NRI results")
                }
            }

            item {
                Text(
                    "Actual result counts are shown only after the Discover backend evaluates current inventory, privacy, block rules and mutual preferences.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
