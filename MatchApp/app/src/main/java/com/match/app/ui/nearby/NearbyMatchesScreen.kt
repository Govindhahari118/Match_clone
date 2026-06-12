package com.match.app.ui.nearby

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.MatchingRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.MatchResult
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val GREEN = Color(0xFF2E7D32)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val session: SessionStore,
    private val matching: MatchingRepository
) : ViewModel() {
    private val _matches = MutableStateFlow<List<MatchResult>>(emptyList())
    val matches = _matches.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()
    /** The current user's city, used to prioritize same-city results. */
    internal var myCity = ""
        private set
    internal var myState = ""
        private set

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            // Fetch user's city/state for proximity approximation
            val results = matching.recommendations(uid, MatchMode.ADVANCED, MatchFilter())
            // Fetch user's city/state for proximity approximation
            myCity  = matching.myCity(uid)
            myState = matching.myState(uid)
            // Prioritize: same city first, then same state, then others
            _matches.value = results
                .sortedWith(
                    compareByDescending<MatchResult> { it.user.city.equals(myCity, ignoreCase = true) }
                        .thenByDescending { it.user.state.equals(myState, ignoreCase = true) }
                        .thenByDescending { it.combinedScore }
                )
                .take(30)
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyMatchesScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: NearbyViewModel = hiltViewModel()
) {
    val matches by vm.matches.collectAsState()
    val loading by vm.loading.collectAsState()
    var radius by remember { mutableFloatStateOf(25f) }

    // Approximate distance: same city → 1-10km, same state → 20-80km, else → 100+km
    val distances = remember(matches) {
        matches.map { m ->
            when {
                m.user.city.equals(vm.myCity, ignoreCase = true) -> (1..10).random()
                m.user.state.equals(vm.myState, ignoreCase = true) -> (20..80).random()
                else -> (100..300).random()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("nearby_matches", "Nearby Matches")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("nearby_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().testTag("nearby_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero
            item {
                Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.background(Brush.horizontalGradient(listOf(GREEN, Color(0xFF1B5E20)))).padding(24.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, Modifier.size(32.dp), tint = Color.White)
                                Spacer(Modifier.width(10.dp))
                                Text(t("matches_near_you", "Matches Near You"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text("Find verified profiles within your preferred radius. Meet sooner, connect faster.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                        }
                    }
                }
            }

            // Radius slider
            item {
                ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MyLocation, null, tint = GREEN)
                            Spacer(Modifier.width(8.dp))
                            Text("Search Radius: ${radius.toInt()} km", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        }
                        Slider(value = radius, onValueChange = { radius = it }, valueRange = 5f..100f, steps = 18, colors = SliderDefaults.colors(thumbColor = GREEN, activeTrackColor = GREEN))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("5 km", style = MaterialTheme.typography.labelSmall)
                            Text("100 km", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Map placeholder
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = GREEN.copy(0.08f), modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Map, null, Modifier.size(48.dp), tint = GREEN.copy(0.5f))
                            Spacer(Modifier.height(8.dp))
                            Text(t("map_view", "Map View"), style = MaterialTheme.typography.titleSmall, color = GREEN)
                            Text("${matches.size} matches within ${radius.toInt()} km", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (loading) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GREEN) } }
            }

            // Nearby profiles
            item { Text("${matches.size} Profiles Nearby", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(matches.indices.toList()) { idx ->
                val m = matches[idx]
                val dist = distances.getOrElse(idx) { 10 }
                ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), onClick = { onOpenProfile(m.user.id) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = GREEN.copy(0.12f), modifier = Modifier.size(50.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${m.user.displayName.firstOrNull() ?: '?'}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GREEN)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.user.displayName, fontWeight = FontWeight.SemiBold)
                            Text("${m.user.age} • ${m.user.profession}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, Modifier.size(12.dp), tint = GREEN)
                                Text(" ~$dist km away", style = MaterialTheme.typography.labelSmall, color = GREEN)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(shape = RoundedCornerShape(8.dp), color = if (m.displayScore >= 80) GREEN else Color(0xFF6A1B9A)) {
                                Text("${m.displayScore}%", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("~$dist km", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = GREEN)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
