package com.match.app.ui.muhurat

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
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private val SAFFRON = Color(0xFFE65100)

private data class MuhuratDate(
    val date: String, val day: String, val occasion: String,
    val nakshatra: String, val tithi: String, val quality: String
)

private data class MonthGroup(val month: String, val dates: List<MuhuratDate>)

private val MUHURAT_DATA = listOf(
    MonthGroup("April 2026", listOf(
        MuhuratDate("22 Apr", "Wed", "Akshaya Tritiya", "Rohini", "Tritiya", "Most Auspicious"),
        MuhuratDate("26 Apr", "Sun", "Shubh Muhurat", "Mrigashira", "Saptami", "Auspicious"),
        MuhuratDate("29 Apr", "Wed", "Shubh Muhurat", "Pushya", "Dashami", "Auspicious")
    )),
    MonthGroup("May 2026", listOf(
        MuhuratDate("3 May", "Sun", "Parashurama Jayanti", "Uttara Phalguni", "Tritiya", "Auspicious"),
        MuhuratDate("12 May", "Tue", "Shubh Muhurat", "Swati", "Dwadashi", "Most Auspicious"),
        MuhuratDate("18 May", "Mon", "Shubh Muhurat", "Shravana", "Tritiya", "Auspicious"),
        MuhuratDate("26 May", "Tue", "Shubh Muhurat", "Revati", "Ekadashi", "Auspicious"),
        MuhuratDate("28 May", "Thu", "Shubh Muhurat", "Rohini", "Trayodashi", "Most Auspicious")
    )),
    MonthGroup("June 2026", listOf(
        MuhuratDate("2 Jun", "Tue", "Ganga Dussehra", "Hasta", "Dashami", "Auspicious"),
        MuhuratDate("8 Jun", "Mon", "Shubh Muhurat", "Anuradha", "Pratipada", "Auspicious"),
        MuhuratDate("15 Jun", "Mon", "Dev Uthani Ekadashi", "Dhanishta", "Ekadashi", "Most Auspicious"),
        MuhuratDate("22 Jun", "Mon", "Shubh Muhurat", "Ashwini", "Tritiya", "Auspicious")
    )),
    MonthGroup("November 2026", listOf(
        MuhuratDate("14 Nov", "Sat", "Dev Diwali", "Krittika", "Purnima", "Most Auspicious"),
        MuhuratDate("22 Nov", "Sun", "Tulsi Vivah", "Uttara Ashadha", "Dwadashi", "Most Auspicious"),
        MuhuratDate("25 Nov", "Wed", "Shubh Muhurat", "Shravana", "Purnima", "Auspicious"),
        MuhuratDate("30 Nov", "Mon", "Shubh Muhurat", "Revati", "Panchami", "Auspicious")
    )),
    MonthGroup("December 2026", listOf(
        MuhuratDate("1 Dec", "Tue", "Shubh Muhurat", "Ashwini", "Shashthi", "Auspicious"),
        MuhuratDate("7 Dec", "Mon", "Shubh Muhurat", "Pushya", "Dwadashi", "Most Auspicious"),
        MuhuratDate("14 Dec", "Mon", "Shubh Muhurat", "Magha", "Tritiya", "Auspicious"),
        MuhuratDate("21 Dec", "Mon", "Shubh Muhurat", "Uttara Phalguni", "Dashami", "Auspicious")
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstroCalendarScreen(onBack: () -> Unit = {}) {
    var selectedQuality by remember { mutableStateOf<String?>(null) }
    val filtered = MUHURAT_DATA.map { group ->
        group.copy(dates = group.dates.filter { selectedQuality == null || it.quality == selectedQuality })
    }.filter { it.dates.isNotEmpty() }
    val totalDates = MUHURAT_DATA.sumOf { it.dates.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("muhurat_calendar", "Muhurat Calendar")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("muhurat_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().testTag("muhurat_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero
            item {
                Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.background(Brush.horizontalGradient(listOf(SAFFRON, Color(0xFFBF360C)))).padding(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🕉️", style = MaterialTheme.typography.displayMedium)
                            Text(t("shubh_muhurat_2026", "Shubh Muhurat 2026"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                            Text("$totalDates auspicious dates for engagement, wedding & ceremonies\nBased on Hindu Panchang and Vedic astrology", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Filters
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedQuality == null, onClick = { selectedQuality = null }, label = { Text("All Dates") })
                    FilterChip(selected = selectedQuality == "Most Auspicious", onClick = { selectedQuality = if (selectedQuality == "Most Auspicious") null else "Most Auspicious" }, label = { Text("Most Auspicious") }, leadingIcon = if (selectedQuality == "Most Auspicious") {{ Icon(Icons.Filled.Star, null, Modifier.size(14.dp)) }} else null)
                    FilterChip(selected = selectedQuality == "Auspicious", onClick = { selectedQuality = if (selectedQuality == "Auspicious") null else "Auspicious" }, label = { Text("Auspicious") })
                }
            }

            // Month groups
            filtered.forEach { group ->
                item {
                    Text(group.month, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SAFFRON)
                }
                items(group.dates) { d ->
                    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), colors = if (d.quality == "Most Auspicious") CardDefaults.elevatedCardColors(containerColor = SAFFRON.copy(0.06f)) else CardDefaults.elevatedCardColors()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Surface(shape = RoundedCornerShape(10.dp), color = if (d.quality == "Most Auspicious") SAFFRON else Color(0xFF2E7D32), modifier = Modifier.width(56.dp)) {
                                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(d.date.split(" ")[0], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(d.date.split(" ").getOrElse(1) { "" }, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(d.occasion, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(6.dp))
                                    Text(d.day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("☆ ${d.nakshatra}", style = MaterialTheme.typography.labelSmall)
                                    Text("☾ ${d.tithi}", style = MaterialTheme.typography.labelSmall)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = if (d.quality == "Most Auspicious") SAFFRON else Color(0xFF2E7D32)) {
                                    Text(d.quality, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
