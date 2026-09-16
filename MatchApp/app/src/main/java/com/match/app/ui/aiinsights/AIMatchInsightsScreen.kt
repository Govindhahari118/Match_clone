package com.match.app.ui.aiinsights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
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
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.MatchingRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.MatchResult
import com.match.app.domain.model.ReligionId
import com.match.app.ui.components.EmptyState
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIInsightsUi(
    val loading: Boolean = true,
    val mode: MatchMode = MatchMode.QUESTIONNAIRE,
    val results: List<MatchResult> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AIInsightsViewModel @Inject constructor(
    private val session: SessionStore,
    private val matching: MatchingRepository,
    private val userDao: UserDao
) : ViewModel() {
    private val _ui = MutableStateFlow(AIInsightsUi())
    val ui = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        runCatching {
            val uid = session.userId.first() ?: error("You are not signed in.")
            val user = userDao.findById(uid) ?: error("Your profile could not be loaded.")
            val religion = ReligionId.fromProfileValue(user.religion)
            val mode = if (religion == ReligionId.HINDU) MatchMode.ADVANCED else MatchMode.QUESTIONNAIRE
            val filter = session.filter.first()
            val results = matching.recommendations(uid, mode, filter)
                .sortedByDescending { it.primary() }
                .take(20)
            AIInsightsUi(loading = false, mode = mode, results = results)
        }.onSuccess { _ui.value = it }
            .onFailure { _ui.value = AIInsightsUi(loading = false, error = it.message ?: "Could not load recommendation insights.") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIMatchInsightsScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: AIInsightsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("ai_match_insights", "Match Insights"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ai_insights_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            ui.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            ui.error != null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(10.dp))
                Text(ui.error.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = vm::refresh) { Text("Retry") }
            }
            else -> InsightList(ui, onOpenProfile, Modifier.padding(padding))
        }
    }
}

@Composable
private fun InsightList(ui: AIInsightsUi, onOpenProfile: (Long) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize().testTag("ai_insights_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Explainable recommendation signals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (ui.mode == MatchMode.ADVANCED)
                            "These rankings use stored questionnaire signals and optional astrology data when available and applicable."
                        else
                            "These rankings use stored questionnaire and profile-preference signals. Astrology is not forced into this member's recommendation mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Scores are ranking signals—not a prediction of relationship success or a substitute for your own judgement.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Current recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("${ui.results.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (ui.results.isEmpty()) {
            item {
                EmptyState(
                    modifier = Modifier.fillParentMaxHeight(0.6f),
                    icon = Icons.Filled.AutoAwesome,
                    title = "No recommendation insights yet",
                    subtitle = "Complete your profile and compatibility questionnaire, then adjust discovery preferences to generate explainable recommendations."
                )
            }
        } else {
            items(ui.results, key = { it.user.id }) { result ->
                RecommendationInsightCard(result = result, mode = ui.mode, onOpen = { onOpenProfile(result.user.id) })
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun RecommendationInsightCard(
    result: MatchResult,
    mode: MatchMode,
    onOpen: () -> Unit
) {
    ElevatedCard(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            result.user.displayName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(result.user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOf(
                            result.user.age.takeIf { it > 0 }?.toString(),
                            result.user.city.takeIf { it.isNotBlank() },
                            result.user.profession.takeIf { it.isNotBlank() }
                        ).filterNotNull().joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {
                    Text(
                        "${result.displayScore}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            LinearProgressIndicator(
                progress = { result.primary().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SignalChip("Questionnaire", result.questionnaireScore)
                if (mode == MatchMode.ADVANCED && result.astrologyScore > 0f) {
                    SignalChip("Astrology", result.astrologyScore)
                }
            }
        }
    }
}

@Composable
private fun SignalChip(label: String, score: Float) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            "$label ${(score.coerceIn(0f, 1f) * 100).toInt()}%",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
