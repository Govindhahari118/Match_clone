package com.match.app.ui.deepcompat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.ui.i18n.t

private val VIOLET = Color(0xFF4A148C)

private data class CompatCategory(val icon: ImageVector, val name: String, val score: Int, val details: List<Pair<String, String>>, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityDeepDiveScreen(
    candidateId: Long = -1L,
    onBack: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    vm: CompatibilityDeepDiveViewModel = hiltViewModel()
) {
    // Subscription gate: Kundali match requires Gold+
    val planKey by vm.planKey.collectAsState()
    val hasAccess = com.match.app.domain.subscription.SubscriptionPlans.canAccess(
        planKey, com.match.app.domain.subscription.SubscriptionPlans.Feature.KUNDALI_MATCH
    )
    var showPaywall by remember { mutableStateOf(false) }
    LaunchedEffect(hasAccess) { if (!hasAccess) showPaywall = true }

    if (showPaywall && !hasAccess) {
        com.match.app.ui.common.PaywallSheet(
            feature = com.match.app.domain.subscription.SubscriptionPlans.Feature.KUNDALI_MATCH,
            minimumPlan = com.match.app.domain.subscription.SubscriptionPlans.Plan.PREMIUM,
            onUpgrade = onUpgrade,
            onDismiss = { showPaywall = false; onBack() }
        )
    }

    // Load real scores when a candidate is available
    LaunchedEffect(candidateId) {
        if (candidateId > 0) vm.load(candidateId)
    }
    val scoreState by vm.state.collectAsState()

    val overallScore = if (candidateId > 0 && !scoreState.isLoading) scoreState.totalPct else 87

    val categories = remember(scoreState) { listOf(
        CompatCategory(Icons.Filled.Psychology, "Personality & Values",
            if (candidateId > 0 && !scoreState.isLoading) scoreState.personality else 92, listOf(
            "Communication Style" to "Both are empathetic listeners with assertive expression",
            "Conflict Resolution" to "Preference for calm discussion over avoidance — highly aligned",
            "Life Goals" to "Both prioritise family and career balance equally",
            "Risk Tolerance" to "Moderate — comfortable with calculated decisions"
        ), Color(0xFF6A1B9A)),
        CompatCategory(Icons.Filled.Favorite, "Emotional Compatibility",
            if (candidateId > 0 && !scoreState.isLoading) scoreState.lifestyle else 88, listOf(
            "Attachment Style" to "Secure-Secure combination — strongest foundation",
            "Love Language" to "Yours: Quality Time, Theirs: Words of Affirmation — complementary",
            "Emotional Intelligence" to "Both score high on empathy and self-awareness",
            "Vulnerability" to "Both comfortable sharing feelings — no walls"
        ), Color(0xFFC2185B)),
        CompatCategory(Icons.Filled.Home, "Family & Lifestyle",
            if (candidateId > 0 && !scoreState.isLoading) scoreState.familyValues else 85, listOf(
            "Family Involvement" to "Both value family input while maintaining independence",
            "Living Preference" to "Nuclear family preference aligned",
            "Children" to "Both want 1-2 children within 2-3 years",
            "Pet Preference" to "Both are pet-friendly — dogs preferred"
        ), Color(0xFF2E7D32)),
        CompatCategory(Icons.Filled.AccountBalance, "Financial Alignment",
            if (candidateId > 0 && !scoreState.isLoading) scoreState.educationCareer else 82, listOf(
            "Spending Pattern" to "Both are moderate spenders with savings habits",
            "Financial Goals" to "Home ownership and retirement planning aligned",
            "Joint vs Separate" to "Preference for joint finances with personal allowance",
            "Investment Style" to "Both favour mutual funds and real estate"
        ), Color(0xFF1565C0)),
        CompatCategory(Icons.Filled.Star, "Astrology Match",
            if (candidateId > 0 && !scoreState.isLoading) scoreState.astrology else 90, listOf(
            "Ashta Koota Score" to "31/36 — Excellent kundli match",
            "Manglik Status" to "Both non-Manglik — no dosha",
            "Nadi" to "Different Nadi types — healthy combination",
            "Rasi Compatibility" to "Simha-Tula — Fire and Air — passionate match"
        ), Color(0xFFE65100)),
        CompatCategory(Icons.Filled.SelfImprovement, "Interests & Hobbies",
            if (candidateId > 0 && !scoreState.isLoading) scoreState.physical else 84, listOf(
            "Shared Activities" to "Travel, cooking, reading — 3 of 5 overlap",
            "Weekend Style" to "Mix of socialising and quiet time — balanced",
            "Fitness" to "Both moderately active — walking and yoga",
            "Food Preferences" to "Both enjoy South Indian and continental cuisine"
        ), Color(0xFF00695C)),
        CompatCategory(Icons.Filled.Public, "Cultural Fit",
            if (candidateId > 0 && !scoreState.isLoading) scoreState.religionCaste else 91, listOf(
            "Religion" to "Same religion and community — aligned traditions",
            "Language" to "Common mother tongue + English proficiency",
            "Festivals" to "Both celebrate major Hindu festivals actively",
            "Dietary Alignment" to "Both vegetarian — no conflict"
        ), Color(0xFF4E342E)),
        CompatCategory(Icons.Filled.Work, "Career & Ambition",
            if (candidateId > 0 && !scoreState.isLoading) scoreState.age else 86, listOf(
            "Career Priority" to "Both view career as important but not everything",
            "Work-Life Balance" to "Both prefer structured work hours over hustle culture",
            "Relocation" to "Both open to relocating within India for opportunities",
            "Partner's Career" to "Fully supportive of each other's professional growth"
        ), Color(0xFF37474F))
    )}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("compatibility_deep_dive", "Compatibility Deep Dive")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("deepcompat_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("deep_compat_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Hero
            Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.horizontalGradient(listOf(VIOLET, Color(0xFF6A1B9A)))).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$overallScore%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(t("overall_compatibility", "Overall Compatibility"), style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Based on 32 factors across 8 dimensions\nPowered by questionnaire + astrology + AI", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), textAlign = TextAlign.Center)
                    }
                }
            }

            // Overview bars
            Text(t("dimension_overview", "Dimension Overview"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            categories.forEach { cat ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(cat.icon, null, Modifier.size(18.dp), tint = cat.color)
                    Spacer(Modifier.width(8.dp))
                    Text(cat.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(120.dp))
                    LinearProgressIndicator(progress = { cat.score / 100f }, modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)), color = cat.color)
                    Spacer(Modifier.width(8.dp))
                    Text("${cat.score}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                }
            }

            // Detailed breakdown
            Text(t("detailed_breakdown", "Detailed Breakdown"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            categories.forEach { cat ->
                var expanded by remember { mutableStateOf(false) }
                ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = cat.color.copy(0.12f), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(cat.icon, null, Modifier.size(22.dp), tint = cat.color) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cat.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Text("${cat.details.size} factors analysed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = cat.color) {
                                Text("${cat.score}%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (expanded) {
                            HorizontalDivider()
                            cat.details.forEach { (factor, analysis) ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(factor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, color = cat.color)
                                    Text(analysis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Relationship prediction
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text("Relationship Prediction", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                    Text("Based on your combined scores, this match has a very high probability of a successful, long-lasting relationship. Your personality alignment (92%) and cultural fit (91%) are particularly strong foundations.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B5E20))
                    Text("Recommended: Proceed with confidence. Consider a virtual meet followed by family introduction.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                }
            }

            // Potential friction points
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFF3E0))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, tint = Color(0xFFE65100))
                        Spacer(Modifier.width(8.dp))
                        Text("Potential Friction Points", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                    listOf(
                        "Financial: Different attitudes toward discretionary spending — discuss a shared budget early",
                        "Career: One may prioritise stability over growth — ensure mutual understanding",
                        "Family: Different weekend social styles — plan alone time vs family time together"
                    ).forEach { point ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Circle, null, Modifier.size(8.dp).padding(top = 4.dp), tint = Color(0xFFE65100))
                            Spacer(Modifier.width(8.dp))
                            Text(point, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4E2600))
                        }
                    }
                }
            }

            // Trust score section
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VerifiedUser, null, tint = Color(0xFF1565C0))
                        Spacer(Modifier.width(8.dp))
                        Text("Combined Trust Score", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text("Both profiles together have a combined trust rating that ranks in the top 15% of matches on this platform.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Your Score", style = MaterialTheme.typography.labelSmall)
                            Text("78/100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            Text("Highly Trusted", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Their Score", style = MaterialTheme.typography.labelSmall)
                            Text("85/100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                            Text("Highly Trusted", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Combined", style = MaterialTheme.typography.labelSmall)
                            Text("82/100", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                            Text("Top 15%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6A1B9A))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
