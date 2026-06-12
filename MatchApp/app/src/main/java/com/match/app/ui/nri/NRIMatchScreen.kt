package com.match.app.ui.nri

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.match.app.ui.i18n.t
import androidx.compose.ui.unit.dp

private val NAVY = Color(0xFF0D47A1)

private data class NRICountry(val flag: String, val name: String, val profiles: Int, val popular: Boolean = false)
private data class NRIProfile(val name: String, val age: Int, val country: String, val city: String, val profession: String, val flag: String, val score: Int)

private val COUNTRIES = listOf(
    NRICountry("🇺🇸", "United States", 12400, true),
    NRICountry("🇬🇧", "United Kingdom", 5600, true),
    NRICountry("🇨🇦", "Canada", 4800, true),
    NRICountry("🇦🇺", "Australia", 3200, true),
    NRICountry("🇦🇪", "UAE / Dubai", 6100, true),
    NRICountry("🇸🇬", "Singapore", 2100),
    NRICountry("🇩🇪", "Germany", 1800),
    NRICountry("🇳🇿", "New Zealand", 900),
    NRICountry("🇸🇦", "Saudi Arabia", 1500),
    NRICountry("🇰🇼", "Kuwait", 800),
    NRICountry("🇶🇦", "Qatar", 700),
    NRICountry("🇴🇲", "Oman", 500),
    NRICountry("🇮🇪", "Ireland", 600),
    NRICountry("🇳🇱", "Netherlands", 450)
)

private val FEATURED = listOf(
    NRIProfile("Ananya S.", 27, "USA", "San Francisco", "Product Manager, Google", "🇺🇸", 92),
    NRIProfile("Vikram R.", 30, "UK", "London", "Investment Banker", "🇬🇧", 88),
    NRIProfile("Priya M.", 26, "Canada", "Toronto", "Data Scientist", "🇨🇦", 91),
    NRIProfile("Arjun K.", 29, "Australia", "Sydney", "Surgeon", "🇦🇺", 85),
    NRIProfile("Kavya P.", 28, "UAE", "Dubai", "Architect", "🇦🇪", 87),
    NRIProfile("Rohit V.", 31, "Singapore", "Singapore", "VP Engineering", "🇸🇬", 89)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NRIMatchScreen(onBack: () -> Unit = {}) {
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = COUNTRIES.filter {
        selectedCountry == null || it.name == selectedCountry
    }.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("nri_matches", "NRI Matches")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("nri_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().testTag("nri_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero
            item {
                Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.background(Brush.horizontalGradient(listOf(NAVY, Color(0xFF1565C0)))).padding(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🌍", style = MaterialTheme.typography.displayMedium)
                            Text(t("nri_matrimony", "NRI Matrimony"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                            Text("Connect with ${COUNTRIES.sumOf { it.profiles }.let { "%,d".format(it) }}+ verified NRI profiles across ${COUNTRIES.size} countries", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), textAlign = TextAlign.Center)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${COUNTRIES.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(t("countries", "Countries"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("41K+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(t("nri_profiles", "NRI Profiles"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("92%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Verified", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                                }
                            }
                        }
                    }
                }
            }

            // Search
            item {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    label = { Text("Search by country") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Featured NRI profiles
            item { Text(t("featured_nri_profiles", "Featured NRI Profiles"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(FEATURED) { p ->
                ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = NAVY.copy(0.12f), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(p.flag, style = MaterialTheme.typography.titleLarge) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontWeight = FontWeight.SemiBold)
                            Text("${p.age} • ${p.city}, ${p.country}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(p.profession, style = MaterialTheme.typography.labelSmall, color = NAVY)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = if (p.score >= 90) Color(0xFF2E7D32) else NAVY) {
                            Text("${p.score}%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            // Countries grid
            item { Text(t("browse_by_country", "Browse by Country"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(filteredCountries.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { c ->
                        ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f), onClick = { selectedCountry = if (selectedCountry == c.name) null else c.name }) {
                            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(c.flag, style = MaterialTheme.typography.headlineMedium)
                                Text(c.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                Text("${"%,d".format(c.profiles)} profiles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (c.popular) Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFF6F00)) {
                                    Text("POPULAR", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
