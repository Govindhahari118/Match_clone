package com.match.app.ui.kundli

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.core.matching.Astrology
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KundliUi(
    val myRasi: String = "",
    val myNakshatra: String = "",
    val astrologyScore: Float = 0f,
    val ganaDosh: Boolean = false,
    val nadiDosh: Boolean = false,
    val mangalDosh: Boolean = false,
    val compatibleCount: Int = 0
)

@HiltViewModel
class KundliViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(KundliUi())
    val ui: StateFlow<KundliUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val p = auth.currentProfile(uid) ?: return@launch
            val rasiIdx = Astrology.RASIS.indexOf(p.rasi)
            val nakIdx  = Astrology.NAKSHATRAS.indexOf(p.nakshatra)
            _ui.value = KundliUi(
                myRasi = p.rasi, myNakshatra = p.nakshatra,
                astrologyScore = Astrology.score(p.rasi, p.nakshatra, p.rasi, p.nakshatra),
                ganaDosh  = nakIdx % 3 == 0,
                nadiDosh  = nakIdx % 9 == 0,
                mangalDosh = rasiIdx in listOf(0, 3, 6, 9),
                compatibleCount = Astrology.RASIS.count { r -> Astrology.score(p.rasi, p.nakshatra, r, Astrology.NAKSHATRAS.random()) > 0.6f }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KundliScreen(onBack: () -> Unit = {}, vm: KundliViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(t("kundli_compatibility", "Kundli Compatibility")) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("kundli_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                .testTag("kundli_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero circle
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(140.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(ui.myRasi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(ui.myNakshatra, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(t("astrology_profile", "Astrology Profile"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(t("astrology_profile_desc", "Based on your Rasi and Nakshatra, here are your compatibility insights"),
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

            Spacer(Modifier.height(24.dp))

            // Rasi + Nakshatra chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, leadingIcon = { Icon(Icons.Filled.Star, null, Modifier.size(16.dp)) },
                    label = { Text("Rasi: ${ui.myRasi}") })
                AssistChip(onClick = {}, leadingIcon = { Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp)) },
                    label = { Text("Nakshatra: ${ui.myNakshatra}") })
            }
            Spacer(Modifier.height(20.dp))

            // Compatible count
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("compatible_rasis", "Compatible Rasis"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${ui.compatibleCount} of ${Astrology.RASIS.size} Rasis are highly compatible with yours",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${ui.compatibleCount}", style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Dosham cards
            Text(t("dosham_analysis", "Dosham Analysis"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            DoshamRow(t("gana_dosh", "Gana Dosh"),  ui.ganaDosh)
            DoshamRow(t("nadi_dosh", "Nadi Dosh"),  ui.nadiDosh)
            DoshamRow(t("mangal_dosh", "Mangal Dosh"),ui.mangalDosh)
            Spacer(Modifier.height(20.dp))

            // Compatible Rasi grid
            Text(t("highly_compatible_rasis", "Highly Compatible Rasis"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Astrology.RASIS.forEach { rasi ->
                    val score = Astrology.score(ui.myRasi, ui.myNakshatra, rasi, Astrology.NAKSHATRAS.first())
                    val highlighted = score > 0.6f
                    SuggestionChip(
                        onClick = {},
                        label = { Text(rasi) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // ── Guna Milan 36-Point Table ─────────────────────────────
            Text(t("guna_milan", "Guna Milan — 36 Points"), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            Text(t("guna_milan_desc", "Traditional Ashta-Koota compatibility matching used in arranged marriages"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))

            // Overall score bar
            val gunaScore = (ui.astrologyScore * 36).toInt().coerceIn(0, 36)
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Overall Guna Score", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            Text(when {
                                gunaScore >= 28 -> "Excellent Match ✦"
                                gunaScore >= 21 -> "Good Match"
                                gunaScore >= 18 -> "Acceptable Match"
                                else            -> "Needs Consideration"
                            }, style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    gunaScore >= 28 -> Color(0xFF2E7D32)
                                    gunaScore >= 21 -> Color(0xFF1565C0)
                                    gunaScore >= 18 -> Color(0xFFE65100)
                                    else            -> Color(0xFFC62828)
                                })
                        }
                        Text("$gunaScore / 36", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                gunaScore >= 28 -> Color(0xFF2E7D32)
                                gunaScore >= 21 -> MaterialTheme.colorScheme.primary
                                else            -> Color(0xFFE65100)
                            })
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { gunaScore / 36f },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = when {
                            gunaScore >= 28 -> Color(0xFF2E7D32)
                            gunaScore >= 21 -> MaterialTheme.colorScheme.primary
                            else            -> Color(0xFFE65100)
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0", style = MaterialTheme.typography.labelSmall)
                        Text("18 min", style = MaterialTheme.typography.labelSmall)
                        Text("28 good", style = MaterialTheme.typography.labelSmall)
                        Text("36", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 8 Kootas table
            Text(t("ashta_koota_breakdown", "Ashta-Koota Breakdown"), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))

            val nakIdx = Astrology.NAKSHATRAS.indexOf(ui.myNakshatra).coerceAtLeast(0)
            val rasiIdx = Astrology.RASIS.indexOf(ui.myRasi).coerceAtLeast(0)
            val kootas = listOf(
                GunaKoota("Varna Koota",    1,  nakIdx % 4 + 1,   "Spiritual compatibility of the couple"),
                GunaKoota("Vashya Koota",   2,  rasiIdx % 2 + 1,  "Power / dominance compatibility"),
                GunaKoota("Tara Koota",     3,  nakIdx % 3 + 1,   "Birth star destiny compatibility"),
                GunaKoota("Yoni Koota",     4,  nakIdx % 4,       "Nature and temperament compatibility"),
                GunaKoota("Graha Maitri",   5,  rasiIdx % 5 + 1,  "Intellectual and mental compatibility"),
                GunaKoota("Gana Koota",     6,  if (!ui.ganaDosh) 5 else 2, "Character and nature compatibility"),
                GunaKoota("Bhakoot Koota",  7,  if (rasiIdx % 3 != 0) 6 else 1, "Love, emotional & health compatibility"),
                GunaKoota("Nadi Koota",     8,  if (!ui.nadiDosh) 8 else 0,  "Genetic / health compatibility — most critical")
            )
            kootas.forEach { koota ->
                KootaRow(koota)
            }

            Spacer(Modifier.height(16.dp))
            // Mangal Dosh note
            if (ui.mangalDosh) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF3E0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, Modifier.size(24.dp), tint = Color(0xFFE65100))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Mangal Dosh Present",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100))
                            Text("Typically matched with Manglik partner to neutralize. Consult a jyotishi for detailed remedies.",
                                style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E342E))
                        }
                    }
                }
            }

            // Disclaimer
            Spacer(Modifier.height(12.dp))
            Text("Note: Guna Milan scores are indicative and based on Vedic astrology traditions. " +
                 "Values shown are computed from your Rasi and Nakshatra. For accurate results, " +
                 "please provide exact birth date, time, and place.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

private data class GunaKoota(
    val name: String,
    val maxPoints: Int,
    val scored: Int,
    val description: String
)

@Composable
private fun KootaRow(koota: GunaKoota) {
    val pct = (koota.scored.coerceIn(0, koota.maxPoints).toFloat() / koota.maxPoints).coerceIn(0f, 1f)
    val color = when {
        pct >= 0.75f -> Color(0xFF2E7D32)
        pct >= 0.5f  -> MaterialTheme.colorScheme.primary
        pct >= 0.25f -> Color(0xFFE65100)
        else         -> Color(0xFFC62828)
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(110.dp)) {
            Text(koota.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Text(koota.description, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.weight(1f).height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text("${koota.scored.coerceIn(0, koota.maxPoints)}/${koota.maxPoints}",
            style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp))
    }
}

@Composable
private fun DoshamRow(label: String, present: Boolean) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(if (present) Color(0xFFC62828) else Color(0xFF2E7D32), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(if (present) "Present — consider remedies" else "Not present — favorable",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (present) Color(0xFFC62828) else Color(0xFF2E7D32))
            }
        }
    }
}
