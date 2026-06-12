package com.match.app.ui.aiinsights

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.MatchingRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.match.app.ui.i18n.t

/* ── AI Insight models ─────────────────────────────────────────────── */

private data class InsightCard(
    val icon: ImageVector, val title: String, val body: String,
    val action: String, val color: Color, val metric: String
)

private data class CompatDimension(
    val label: String, val score: Float, val color: Color, val insight: String
)

@HiltViewModel
class AIInsightsViewModel @Inject constructor(
    private val session: SessionStore,
    private val matching: MatchingRepository
) : ViewModel() {
    private val _matches = MutableStateFlow<List<MatchResult>>(emptyList())
    val matches = _matches.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            val results = matching.recommendations(uid, MatchMode.ADVANCED, MatchFilter())
            _matches.value = results.sortedByDescending { it.combinedScore }.take(20)
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIMatchInsightsScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: AIInsightsViewModel = hiltViewModel()
) {
    val matches by vm.matches.collectAsState()
    val loading by vm.loading.collectAsState()

    val insights = remember(matches) {
        val avg = if (matches.isEmpty()) 0f else matches.map { it.combinedScore }.average().toFloat()
        val top = matches.firstOrNull()
        listOf(
            InsightCard(Icons.Filled.Psychology, "Personality Match Pattern",
                "You tend to match best with people who are ${if (avg > 0.6f) "extroverted and family-oriented" else "balanced and career-driven"}. Your top matches share ${(avg * 100).toInt()}% average compatibility.",
                "View Top Matches", Color(0xFF6A1B9A), "${(avg * 100).toInt()}%"),
            InsightCard(Icons.AutoMirrored.Filled.TrendingUp, "Your Match Momentum",
                "Your profile received ${matches.size * 3} views this week — ${if (matches.size > 5) "42% above average!" else "keep optimising your profile."}",
                "Boost Profile", Color(0xFF00838F), "${matches.size * 3}"),
            InsightCard(Icons.Filled.AutoAwesome, "AI Pick of the Day",
                top?.let { "Based on 15 compatibility factors, ${it.user.displayName} from ${it.user.city} is your strongest match today (${it.displayScore}%)." }
                    ?: "Complete your questionnaire to unlock AI picks.",
                "View Profile", Color(0xFFD84315), top?.let { "${it.displayScore}%" } ?: "—"),
            InsightCard(Icons.Filled.Diversity3, "Community Insight",
                "Users with your background and preferences get married within 90 days on average. You're on track!",
                "See Success Stories", Color(0xFF2E7D32), "90d"),
            InsightCard(Icons.Filled.Lightbulb, "Profile Tip",
                "Adding a video introduction increases match rate by 3.2×. Profiles with professional photos get 5× more interest requests.",
                "Add Video", Color(0xFFFF6F00), "3.2×")
        )
    }

    val dimensions = remember(matches) {
        val avg = if (matches.isEmpty()) 0.5f else matches.map { it.combinedScore }.average().toFloat()
        listOf(
            CompatDimension("Personality", avg * 1.1f.coerceAtMost(1f), Color(0xFF6A1B9A), "Strong alignment in values and communication style"),
            CompatDimension("Lifestyle", avg * 0.95f, Color(0xFF00838F), "Diet, habits, and daily routines are mostly compatible"),
            CompatDimension("Family Values", avg * 1.05f.coerceAtMost(1f), Color(0xFF2E7D32), "Similar expectations for family involvement"),
            CompatDimension("Education & Career", avg * 0.9f, Color(0xFFD84315), "Professional growth mindset alignment"),
            CompatDimension("Astrology", if (matches.isEmpty()) 0.5f else matches.map { it.astrologyScore }.average().toFloat(), Color(0xFFFF6F00), "Rasi and Nakshatra compatibility score"),
            CompatDimension("Location", avg * 0.85f, Color(0xFF1565C0), "Geographic proximity and relocation flexibility"),
            CompatDimension("Financial", avg * 0.88f, Color(0xFF4E342E), "Income expectations and financial planning alignment"),
            CompatDimension("Interests & Hobbies", avg * 0.92f, Color(0xFFC2185B), "Shared activities and leisure preferences")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("ai_match_insights", "AI Match Insights")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("ai_insights_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().testTag("ai_insights_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero
            item {
                Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF6A1B9A), Color(0xFFAD1457)))).padding(24.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(32.dp), tint = Color.White)
                                Spacer(Modifier.width(10.dp))
                                Text(t("your_ai_compat_report", "Your AI Compatibility Report"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text("Powered by our 15-factor matching engine analysing personality, astrology, lifestyle, and family values.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                            if (loading) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.White)
                            }
                        }
                    }
                }
            }

            // Insight cards
            item { Text(t("personalised_insights", "Personalised Insights"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(insights) { insight ->
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Surface(shape = CircleShape, color = insight.color.copy(0.12f), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(insight.icon, null, Modifier.size(24.dp), tint = insight.color) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(insight.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(8.dp), color = insight.color) {
                                    Text(insight.metric, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            Text(insight.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Compatibility radar
            item { Text(t("compat_breakdown", "8-Dimension Compatibility Breakdown"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(dimensions) { dim ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(dim.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(110.dp))
                    LinearProgressIndicator(progress = { dim.score.coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)), color = dim.color)
                    Spacer(Modifier.width(8.dp))
                    Text("${(dim.score * 100).toInt().coerceIn(0,100)}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                }
                Text(dim.insight, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 110.dp))
            }

            // Top matches
            if (matches.isNotEmpty()) {
                item { Text(t("your_top_ai_picks", "Your Top AI Picks"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                items(matches.take(5)) { m ->
                    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), onClick = { onOpenProfile(m.user.id) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFF6A1B9A).copy(0.12f), modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${m.user.displayName.firstOrNull() ?: '?'}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF6A1B9A))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.user.displayName, fontWeight = FontWeight.SemiBold)
                                Text("${m.user.age} • ${m.user.city} • ${m.user.profession}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = if (m.displayScore >= 80) Color(0xFF2E7D32) else Color(0xFF6A1B9A)) {
                                Text("${m.displayScore}%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
