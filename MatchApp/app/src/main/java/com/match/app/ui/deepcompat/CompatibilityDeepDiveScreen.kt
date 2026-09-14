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
import com.match.app.domain.subscription.SubscriptionPlans

private data class ScoreItem(val label: String, val score: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityDeepDiveScreen(
    candidateId: Long = -1L,
    onBack: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    vm: CompatibilityDeepDiveViewModel = hiltViewModel()
) {
    val planKey by vm.planKey.collectAsState()
    val hasAccess = SubscriptionPlans.canAccess(planKey, SubscriptionPlans.Feature.KUNDALI_MATCH)
    var showPaywall by remember { mutableStateOf(false) }
    val state by vm.state.collectAsState()

    LaunchedEffect(candidateId) {
        if (candidateId > 0) vm.load(candidateId)
    }
    LaunchedEffect(hasAccess) {
        showPaywall = !hasAccess
    }

    if (showPaywall && !hasAccess) {
        com.match.app.ui.common.PaywallSheet(
            feature = SubscriptionPlans.Feature.KUNDALI_MATCH,
            minimumPlan = SubscriptionPlans.Plan.SILVER_3M,
            onUpgrade = onUpgrade,
            onDismiss = { showPaywall = false; onBack() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compatibility breakdown") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("deepcompat_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            candidateId <= 0 -> EmptyCompatState(
                modifier = Modifier.padding(padding),
                message = "Open this report from a specific match profile."
            )
            state.isLoading -> Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            else -> {
                val scores = listOf(
                    ScoreItem("Astrology", state.astrology),
                    ScoreItem("Religion & community", state.religionCaste),
                    ScoreItem("Education & career", state.educationCareer),
                    ScoreItem("Location", state.location),
                    ScoreItem("Age preference", state.age),
                    ScoreItem("Family values", state.familyValues),
                    ScoreItem("Lifestyle", state.lifestyle),
                    ScoreItem("Physical preferences", state.physical),
                    ScoreItem("Personality", state.personality)
                )

                if (scores.all { it.score == 0 } && state.totalPct == 0) {
                    EmptyCompatState(
                        modifier = Modifier.padding(padding),
                        message = "Compatibility data is not available for this profile yet."
                    )
                } else {
                    Column(
                        Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(16.dp).testTag("deep_compat_screen"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ElevatedCard(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${state.totalPct.coerceIn(0, 100)}%",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("Overall compatibility signal", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        scores.forEach { item ->
                            ScoreCard(item)
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "These percentages are generated from profile fields and the app's matching engine. They are guidance signals, not predictions of marriage success or relationship outcomes.",
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
}

@Composable
private fun ScoreCard(item: ScoreItem) {
    val safe = item.score.coerceIn(0, 100)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("$safe%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { safe / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
        }
    }
}

@Composable
private fun EmptyCompatState(modifier: Modifier = Modifier, message: String) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Icon(Icons.Filled.Info, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
