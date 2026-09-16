package com.match.app.ui.deepcompat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private data class ScoreItem(val label: String, val score: Int)

/**
 * Explains comparable profile signals. It is intentionally not paywalled as a Kundali feature;
 * astrology is only one optional dimension and appears only when applicable and available.
 */
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityDeepDiveScreen(
    candidateId: Long = -1L,
    onBack: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    vm: CompatibilityDeepDiveViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(candidateId) {
        if (candidateId > 0) vm.load(candidateId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compatibility breakdown", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("deepcompat_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            candidateId <= 0 -> EmptyCompatState(
                modifier = Modifier.padding(padding),
                message = "Open this breakdown from a specific match profile."
            )

            state.isLoading -> Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.coveragePct == 0 -> EmptyCompatState(
                modifier = Modifier.padding(padding),
                message = "There is not enough comparable profile data for this breakdown yet."
            )

            else -> {
                val scores = buildList {
                    add(ScoreItem("Religion & community", state.religionCaste))
                    add(ScoreItem("Education & career", state.educationCareer))
                    add(ScoreItem("Location", state.location))
                    add(ScoreItem("Age", state.age))
                    add(ScoreItem("Family values", state.familyValues))
                    add(ScoreItem("Lifestyle", state.lifestyle))
                    add(ScoreItem("Values reflection", state.personality))
                    if (state.astrologyAvailable) {
                        add(ScoreItem("Astrology", state.astrology))
                    }
                }

                Column(
                    Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(16.dp).testTag("deep_compat_screen"),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ElevatedCard(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "${state.totalPct}%",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Profile similarity signal", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Based on ${state.coveragePct}% of currently comparable profile data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    scores.forEach { item -> ScoreCard(item) }

                    if (state.astrologyApplicable && !state.astrologyAvailable) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Astrology is applicable to these profiles but was not scored because the required Rasi/Nakshatra data is incomplete.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "These percentages describe similarities in profile fields that are actually available. Missing fields are excluded rather than treated as neutral matches. They are not predictions of relationship or marriage success.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ScoreCard(item: ScoreItem) {
    val safe = item.score.coerceIn(0, 100)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$safe%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { safe / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}

@Composable
private fun EmptyCompatState(modifier: Modifier = Modifier, message: String) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Info, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
            Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
