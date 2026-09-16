package com.match.app.ui.nri

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
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
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class NRICountry(val flag: String, val name: String)

private val NRI_COUNTRIES = listOf(
    NRICountry("🌍", "All countries"),
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
    fun browse(country: String, onApplied: () -> Unit) = viewModelScope.launch {
        val current = session.filter.first()
        session.setFilter(
            current.copy(
                nriOnly = true,
                countryOfResidence = if (country == "All countries") "" else country
            )
        )
        onApplied()
    }
}

/** Real NRI discovery launcher backed by the app's shared match filter; no fabricated profile counts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NRIMatchScreen(
    onBack: () -> Unit = {},
    onOpenMatches: () -> Unit = {},
    vm: NRIMatchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        NRI_COUNTRIES.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("nri_matches", "NRI Matches"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nri_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("nri_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Browse by country of residence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Choose a country to apply an NRI discovery filter to the real Matches screen. No synthetic member counts or featured profiles are shown here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search countries") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("nri_country_search"),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            items(filtered, key = { it.name }) { country ->
                ElevatedCard(
                    onClick = { vm.browse(country.name, onOpenMatches) },
                    modifier = Modifier.fillMaxWidth().testTag("nri_country_${country.name}"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(country.flag, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(12.dp))
                        Text(country.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Browse ${country.name}", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No country matches your search.",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
