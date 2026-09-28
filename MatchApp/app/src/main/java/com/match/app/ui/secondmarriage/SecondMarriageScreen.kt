package com.match.app.ui.secondmarriage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.MatchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.match.app.ui.i18n.t

@HiltViewModel
class SecondMarriageViewModel @Inject constructor(
    private val session: SessionStore,
    private val repo: MatchingRepository
) : ViewModel() {

    private val _matches = MutableStateFlow<List<MatchResult>>(emptyList())
    val matches = _matches.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            _loading.value = true
            val filter = MatchFilter(maritalStatus = "Divorced")
            val results = repo.recommendations(uid, MatchMode.ADVANCED, filter)
            _matches.value = results.take(20)
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondMarriageScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: SecondMarriageViewModel = hiltViewModel()
) {
    val matches by vm.matches.collectAsState()
    val loading by vm.loading.collectAsState()
    var acceptKids by remember { mutableStateOf(true) }

    val filteredMatches = if (acceptKids) matches else matches.filter { !it.user.hasChildren }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("second_marriage", "Second Marriage")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("second_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize().testTag("second_marriage_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Hero banner ──────────────────────────────────────────────
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFF4A148C).copy(alpha = 0.08f)
                    )
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("💜", style = MaterialTheme.typography.displaySmall,
                            textAlign = TextAlign.Center)
                        Text("Begin Again",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4A148C),
                            textAlign = TextAlign.Center)
                        Text(
                            "No matter how hard the past, you can always begin again. " +
                            "Find a compassionate, understanding partner who respects your journey.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Why Choose Second Marriage Section ───────────────────────
            item {
                Text("Why Match for Second Marriage?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WhyCard(Icons.Filled.Verified, "Verified Profiles",
                        "All members verified — real people, honest journeys",
                        Color(0xFF1565C0), Modifier.weight(1f))
                    WhyCard(Icons.Filled.PrivacyTip, "100% Private",
                        "Your second marriage search remains completely confidential",
                        Color(0xFF2E7D32), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WhyCard(Icons.Filled.FamilyRestroom, "Family Friendly",
                        "Profiles include children status, family situation, and values",
                        Color(0xFF8B1A1A), Modifier.weight(1f))
                    WhyCard(Icons.Filled.SupportAgent, "Expert Support",
                        "Dedicated counsellors to guide your journey with sensitivity",
                        Color(0xFF6A1B9A), Modifier.weight(1f))
                }
            }

            // ── Guidance tips ────────────────────────────────────────────
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Guidance for Second Marriages",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        val tips = listOf(
                            Triple(Icons.Filled.Favorite, "Be honest about your past",
                                "Transparency about your previous marriage builds lasting trust"),
                            Triple(Icons.Filled.ChildCare, "Children & step-family",
                                "Discuss blended family expectations early and openly"),
                            Triple(Icons.Filled.Timer, "Take your time",
                                "Don't rush — take time to know the person before committing"),
                            Triple(Icons.Filled.Psychology, "Consider counselling",
                                "Professional pre-marriage counselling can help align expectations"),
                            Triple(Icons.Filled.Group, "Family acceptance",
                                "Engage families early to reduce social friction later")
                        )
                        tips.forEach { (icon, title, subtitle) ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(icon, null, Modifier.size(18.dp).padding(top = 2.dp),
                                    tint = Color(0xFF4A148C))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(subtitle, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // ── Accept Kids Filter ────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ChildCare, null, Modifier.size(20.dp), tint = Color(0xFF4A148C))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("I'm happy to accept kids", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Show profiles with children", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = acceptKids, onCheckedChange = { acceptKids = it })
                    }
                }
            }

            // ── Profiles ──────────────────────────────────────────────────
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Profiles Open to Second Marriage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    if (!loading) {
                        Surface(shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF4A148C).copy(alpha = 0.1f)) {
                            Text("${filteredMatches.size} found",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4A148C),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF4A148C))
                    }
                }
            } else if (filteredMatches.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.SearchOff, null, Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(8.dp))
                            Text("No profiles found — check back soon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredMatches, key = { it.user.id }) { r ->
                    SecondMarriageProfileCard(r, onOpenProfile)
                }
            }

            // Counselling is a provider-backed request flow. Do not advertise a free or
            // immediately bookable session until real provider availability and pricing exist.
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFF4A148C).copy(alpha = 0.06f)
                    )
                ) {
                    Row(
                        Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Psychology,
                            null,
                            Modifier.size(24.dp),
                            tint = Color(0xFF4A148C)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Counselling support",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4A148C)
                            )
                            Text(
                                "Counselling requests are confirmed only after real provider availability, schedule and pricing are established.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WhyCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = color)
            Text(title, style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold, color = color)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SecondMarriageProfileCard(r: MatchResult, onOpen: (Long) -> Unit) {
    val p = r.user
    ElevatedCard(
        onClick = { onOpen(p.id) },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = Color(0xFF4A148C).copy(alpha = 0.12f),
                modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(p.displayName.first().uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = Color(0xFF4A148C))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${p.displayName}, ${p.age}",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (p.isVerified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = Color(0xFF1976D2))
                    }
                }
                Text("${p.city} • ${p.profession}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${p.maritalStatus} • ${p.religion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4A148C))
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF4A148C)) {
                Text("${r.displayScore}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}
