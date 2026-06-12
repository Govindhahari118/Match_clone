package com.match.app.ui.horoscope

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private val SAFFRON = Color(0xFFE65100)

private data class KootaItem(val name: String, val maxPoints: Int, val scored: Int, val desc: String)
private data class DoshaCheck(val name: String, val status: String, val severity: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedHoroscopeScreen(onBack: () -> Unit = {}) {
    var partnerRasi by remember { mutableStateOf("Mesha (Aries)") }
    var partnerNakshatra by remember { mutableStateOf("Ashwini") }
    var showReport by remember { mutableStateOf(false) }

    val rasiOptions = listOf("Mesha (Aries)", "Vrishabha (Taurus)", "Mithuna (Gemini)", "Karka (Cancer)", "Simha (Leo)", "Kanya (Virgo)", "Tula (Libra)", "Vrischika (Scorpio)", "Dhanu (Sagittarius)", "Makara (Capricorn)", "Kumbha (Aquarius)", "Meena (Pisces)")
    val nakshatraOptions = listOf("Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra", "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Moola", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada", "Uttara Bhadrapada", "Revati")

    val ashtaKoota = remember { listOf(
        KootaItem("Varna (Caste)", 1, 1, "Spiritual compatibility — both are compatible classes"),
        KootaItem("Vashya (Dominance)", 2, 2, "Mutual attraction and control — strong magnetic pull"),
        KootaItem("Tara (Star)", 3, 2, "Health and well-being — favourable birth star alignment"),
        KootaItem("Yoni (Nature)", 4, 3, "Physical and sexual compatibility — good match"),
        KootaItem("Graha Maitri (Planet)", 5, 4, "Mental compatibility — planetary lords are friendly"),
        KootaItem("Gana (Temperament)", 6, 6, "Deva-Deva match — highest temperament compatibility"),
        KootaItem("Bhakoot (Love)", 7, 5, "Emotional and financial well-being — auspicious"),
        KootaItem("Nadi (Health)", 8, 8, "Genetic and health compatibility — perfect score")
    )}
    val totalKoota = ashtaKoota.sumOf { it.scored }
    val maxKoota = ashtaKoota.sumOf { it.maxPoints }

    val doshas = remember { listOf(
        DoshaCheck("Manglik (Mars) Dosha", "Not present", "None"),
        DoshaCheck("Nadi Dosha", "Not present", "None"),
        DoshaCheck("Bhakoot Dosha", "Mild — remedies available", "Low"),
        DoshaCheck("Gana Dosha", "Not present", "None"),
        DoshaCheck("Rajju Dosha", "Not present", "None"),
        DoshaCheck("Vedha Dosha", "Not present", "None")
    )}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("advanced_horoscope", "Advanced Horoscope")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("horoscope_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("horoscope_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Hero
            Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.horizontalGradient(listOf(SAFFRON, Color(0xFFBF360C)))).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🕉️", style = MaterialTheme.typography.displayMedium)
                        Text(t("kundli_matching_report", "Kundli Matching Report"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        Text("Ashta Koota • Manglik Check • Dosha Analysis • Remedies", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), textAlign = TextAlign.Center)
                    }
                }
            }

            if (!showReport) {
                // Partner input
                Text(t("enter_partner_details", "Enter Partner's Details"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Text("Your Details (from profile)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = SAFFRON.copy(0.1f), modifier = Modifier.weight(1f)) {
                        Text("Simha (Leo)", Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = SAFFRON.copy(0.1f), modifier = Modifier.weight(1f)) {
                        Text("Magha", Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                    }
                }

                Text("Partner's Rasi", style = MaterialTheme.typography.labelMedium)
                var rasiExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = rasiExpanded, onExpandedChange = { rasiExpanded = it }) {
                    OutlinedTextField(value = partnerRasi, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(rasiExpanded) })
                    ExposedDropdownMenu(expanded = rasiExpanded, onDismissRequest = { rasiExpanded = false }) {
                        rasiOptions.forEach { r -> DropdownMenuItem(text = { Text(r) }, onClick = { partnerRasi = r; rasiExpanded = false }) }
                    }
                }

                Text("Partner's Nakshatra", style = MaterialTheme.typography.labelMedium)
                var nakExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = nakExpanded, onExpandedChange = { nakExpanded = it }) {
                    OutlinedTextField(value = partnerNakshatra, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(nakExpanded) })
                    ExposedDropdownMenu(expanded = nakExpanded, onDismissRequest = { nakExpanded = false }) {
                        nakshatraOptions.forEach { n -> DropdownMenuItem(text = { Text(n) }, onClick = { partnerNakshatra = n; nakExpanded = false }) }
                    }
                }

                Button(onClick = { showReport = true }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = SAFFRON)) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t("generate_kundli_report", "Generate Kundli Report"))
                }
            } else {
                // Koota score
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = if (totalKoota >= 25) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t("ashta_koota_score", "Ashta Koota Score"), style = MaterialTheme.typography.titleMedium)
                        Text("$totalKoota / $maxKoota", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = if (totalKoota >= 25) Color(0xFF2E7D32) else SAFFRON)
                        LinearProgressIndicator(progress = { totalKoota.toFloat() / maxKoota }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), color = if (totalKoota >= 25) Color(0xFF2E7D32) else SAFFRON)
                        Text(when { totalKoota >= 25 -> "Excellent match! Highly recommended for marriage."; totalKoota >= 18 -> "Good match. Marriage is advisable."; else -> "Average match. Consider consulting an astrologer." }, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, color = if (totalKoota >= 25) Color(0xFF2E7D32) else SAFFRON)
                    }
                }

                // Koota breakdown
                Text("Koota Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                ashtaKoota.forEach { k ->
                    ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(k.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(6.dp), color = if (k.scored == k.maxPoints) Color(0xFF2E7D32) else SAFFRON) {
                                    Text("${k.scored}/${k.maxPoints}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            LinearProgressIndicator(progress = { k.scored.toFloat() / k.maxPoints }, modifier = Modifier.fillMaxWidth().height(4.dp), color = if (k.scored == k.maxPoints) Color(0xFF2E7D32) else SAFFRON)
                            Text(k.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Dosha analysis
                Text(t("dosha_analysis", "Dosha Analysis"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                doshas.forEach { d ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (d.severity == "None") Icons.Filled.CheckCircle else Icons.Filled.Warning, null, Modifier.size(20.dp), tint = if (d.severity == "None") Color(0xFF2E7D32) else Color(0xFFE65100))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(d.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Remedies
                Text(t("recommended_remedies", "Recommended Remedies"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                listOf("Perform Navagraha Puja before engagement", "Wear yellow sapphire (Pukhraj) for Jupiter blessing", "Donate food on Tuesdays to strengthen Mars", "Chant Gayatri Mantra 108 times daily").forEach { r ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•", color = SAFFRON, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(r, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Auspicious dates
                Text(t("auspicious_wedding_dates", "Auspicious Wedding Dates"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                listOf("12 May 2026 — Akshaya Tritiya (Most auspicious)", "28 May 2026 — Rohini Nakshatra", "15 Jun 2026 — Dev Uthani Ekadashi", "22 Nov 2026 — Tulsi Vivah").forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp), tint = SAFFRON)
                        Spacer(Modifier.width(8.dp))
                        Text(d, style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedButton(onClick = { showReport = false }, modifier = Modifier.fillMaxWidth()) { Text(t("check_another_match", "Check Another Match")) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
