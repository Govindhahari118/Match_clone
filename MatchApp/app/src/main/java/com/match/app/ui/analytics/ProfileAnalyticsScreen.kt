package com.match.app.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.remote.ProfileAnalyticsService
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class MetricCard(
    val icon: ImageVector,
    val label: String,
    val value: Int
)

data class ProfileAnalyticsUiState(
    val loading: Boolean = true,
    val data: ProfileAnalyticsService.Snapshot? = null,
    val error: String? = null
)

@HiltViewModel
class ProfileAnalyticsViewModel @Inject constructor(
    private val service: ProfileAnalyticsService
) : ViewModel() {
    private val _ui = MutableStateFlow(ProfileAnalyticsUiState())
    val ui: StateFlow<ProfileAnalyticsUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { service.load() }
            .onSuccess { snapshot ->
                _ui.value = ProfileAnalyticsUiState(loading = false, data = snapshot)
            }
            .onFailure {
                _ui.value = ProfileAnalyticsUiState(
                    loading = false,
                    error = "Unable to load profile analytics. Check your connection and try again."
                )
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAnalyticsScreen(
    onBack: () -> Unit = {},
    vm: ProfileAnalyticsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("profile_analytics", "Profile Analytics")) },
                navigationIcon = {
                    IconButton(onClick = onBack, Modifier.testTag("analytics_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh, enabled = !ui.loading) {
                        Icon(Icons.Filled.Refresh, "Refresh analytics")
                    }
                }
            )
        }
    ) { pad ->
        when {
            ui.loading && ui.data == null -> Box(
                Modifier.padding(pad).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            ui.error != null && ui.data == null -> Box(
                Modifier.padding(pad).fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            ui.error.orEmpty(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = vm::refresh) { Text("Retry") }
                    }
                }
            }

            else -> ui.data?.let { snapshot ->
                val metrics = listOf(
                    MetricCard(Icons.Filled.Visibility, "Profile Views", snapshot.profileViews),
                    MetricCard(Icons.Filled.Favorite, "Interests Received", snapshot.interestsReceived),
                    MetricCard(Icons.AutoMirrored.Filled.Send, "Interests Sent", snapshot.interestsSent),
                    MetricCard(Icons.Filled.Handshake, "Mutual Matches", snapshot.mutualMatches),
                    MetricCard(Icons.AutoMirrored.Filled.Chat, "Conversations", snapshot.conversations),
                    MetricCard(Icons.Filled.Bookmark, "Shortlisted By", snapshot.shortlistedBy)
                )
                val hasActivity = metrics.any { it.value > 0 }

                Column(
                    Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .testTag("analytics_screen"),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    t("your_profile_performance", "Your Profile Activity"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "These numbers come from real account activity. We do not generate synthetic ranks, demographic estimates, or comparison percentages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (snapshot.isVerified) Icons.Filled.Verified else Icons.Filled.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (snapshot.isVerified) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (snapshot.isVerified) {
                                        "Your profile is verified."
                                    } else {
                                        "Your profile is not currently verified."
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    if (!hasActivity) {
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Insights,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "No activity yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Real analytics will appear as members view, shortlist, contact, and match with your profile.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        t("key_metrics", "All-time Activity"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    metrics.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { metric ->
                                ElevatedCard(
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            metric.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            metric.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            metric.value.toString(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Recorded activity",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }

                    Text(
                        t("optimisation_tips", "Profile Quality Checklist"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    listOf(
                        "Use clear, recent profile photos that accurately represent you.",
                        "Keep education, career, family, and lifestyle details complete and current.",
                        "Set realistic partner preferences so discovery remains relevant.",
                        "Complete available verification steps to give members trustworthy signals.",
                        "Respond respectfully to genuine interests and keep your profile active."
                    ).forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(tip, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
