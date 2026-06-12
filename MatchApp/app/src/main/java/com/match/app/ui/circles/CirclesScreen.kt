@file:Suppress("UNUSED_PARAMETER")
package com.match.app.ui.circles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.remote.CommunityGroupDto
import com.match.app.data.repo.CatalogRepository
import com.match.app.data.repo.MatchingRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.MatchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.match.app.ui.i18n.t

// ── Bundled circles ──────────────────────────────────────────────────────────
private val BUNDLED_CIRCLES = listOf(
    CommunityGroupDto("c1",  "Telugu Vivah",        "Matches for Telugu-speaking families across Andhra & Telangana", "Language",  12450, motherTongue = "Telugu"),
    CommunityGroupDto("c2",  "Tamil Matrimony",     "Curated profiles for Tamil community members worldwide",         "Language",   9800, motherTongue = "Tamil"),
    CommunityGroupDto("c3",  "Hindi Heartland",     "Hindi-speaking matches from UP, Bihar, MP, Rajasthan & more",   "Language",   8300, motherTongue = "Hindi"),
    CommunityGroupDto("c4",  "Hindu Matches",       "Hindu community profiles from across India and abroad",          "Religion",   7200, religion = "Hindu"),
    CommunityGroupDto("c5",  "Muslim Matrimony",    "Trusted profiles for Muslim community worldwide",               "Religion",   5100, religion = "Muslim"),
    CommunityGroupDto("c6",  "Christian Singles",    "Christian community profiles from across India & abroad",       "Religion",   3400, religion = "Christian"),
    CommunityGroupDto("c7",  "Reddy Community",     "Profiles from Reddy community — Andhra Pradesh & Telangana",   "Caste",      4200, caste = "Reddy"),
    CommunityGroupDto("c8",  "Kamma Community",     "Kamma community profiles — verified and premium members",       "Caste",      3600, caste = "Kamma"),
    CommunityGroupDto("c9",  "Brahmin Profiles",    "Brahmin community from all states across India",                "Caste",      2900, caste = "Brahmin"),
    CommunityGroupDto("c10", "Kapu Community",      "Kapu / Balija community profiles — Andhra & Telangana",        "Caste",      2100, caste = "Kapu"),
    CommunityGroupDto("c11", "Hyderabad Singles",   "Active profiles in and around Hyderabad, Secunderabad",        "City",       6700, city = "Hyderabad"),
    CommunityGroupDto("c12", "Mumbai Matches",      "Urban professionals and families based in Mumbai & suburbs",   "City",       8200, city = "Mumbai"),
    CommunityGroupDto("c13", "Bengaluru Profiles",  "Tech & startup crowd from India's Silicon Valley",             "City",       6100, city = "Bengaluru"),
    CommunityGroupDto("c14", "Delhi NCR",           "Profiles from Delhi, Gurgaon, Noida, Faridabad",              "City",       7500, city = "Delhi"),
    CommunityGroupDto("c15", "Verified Profiles",   "100% phone-verified, identity-checked profiles only",         "Trust",     15000),
    CommunityGroupDto("c16", "Premium Members",     "Serious seekers with active paid subscriptions",              "Trust",      4200),
)

private val CIRCLE_ACCENTS = mapOf(
    "Language" to Color(0xFF1565C0), "Religion" to Color(0xFF1B5E20),
    "Caste" to Color(0xFF6A1B9A), "City" to Color(0xFF4E342E),
    "Trust" to Color(0xFF2E7D32), "NRI" to Color(0xFF0277BD), "Career" to Color(0xFF558B2F),
)
private fun accentFor(cat: String?) = CIRCLE_ACCENTS[cat] ?: Color(0xFF37474F)

// ── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class CirclesViewModel @Inject constructor(
    private val session: SessionStore,
    private val catalog: CatalogRepository,
    private val matchingRepo: MatchingRepository
) : ViewModel() {

    val currentFilter = session.filter.stateIn(viewModelScope, SharingStarted.Eagerly, MatchFilter())

    private val _circles = MutableStateFlow<List<CommunityGroupDto>>(BUNDLED_CIRCLES)
    val circles: StateFlow<List<CommunityGroupDto>> = _circles.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Currently selected circle (null = browse mode) */
    private val _selectedCircle = MutableStateFlow<CommunityGroupDto?>(null)
    val selectedCircle = _selectedCircle.asStateFlow()

    /** Inline match results for the selected circle */
    private val _circleResults = MutableStateFlow<List<MatchResult>>(emptyList())
    val circleResults = _circleResults.asStateFlow()

    private val _loadingResults = MutableStateFlow(false)
    val loadingResults = _loadingResults.asStateFlow()

    init { refreshFromBackend() }

    fun refreshFromBackend() = viewModelScope.launch {
        _loading.value = true
        catalog.fetchCommunityGroups()
            .onSuccess { list -> if (list.isNotEmpty()) _circles.value = list }
        _loading.value = false
    }

    /** Tap a circle → show its filtered match results inline */
    fun selectCircle(c: CommunityGroupDto) = viewModelScope.launch {
        _selectedCircle.value = c
        loadCircleResults(c)
    }

    fun clearSelection() {
        _selectedCircle.value = null
        _circleResults.value = emptyList()
    }

    /** Also apply circle filters globally across the app */
    fun applyAsGlobalFilter(c: CommunityGroupDto) = viewModelScope.launch {
        val existing = currentFilter.value
        session.setFilter(existing.copy(
            religion     = c.religion?.takeIf { it.isNotBlank() } ?: existing.religion,
            caste        = c.caste?.takeIf { it.isNotBlank() } ?: existing.caste,
            city         = c.city?.takeIf { it.isNotBlank() } ?: existing.city,
            motherTongue = c.motherTongue?.takeIf { it.isNotBlank() } ?: existing.motherTongue,
            verifiedOnly = if (c.name.contains("Verified", ignoreCase = true)) true else existing.verifiedOnly
        ))
    }

    fun clearFilters() = viewModelScope.launch { session.setFilter(MatchFilter()) }

    private suspend fun loadCircleResults(c: CommunityGroupDto) {
        _loadingResults.value = true
        val uid = session.userId.first() ?: run { _loadingResults.value = false; return }
        val filter = MatchFilter(
            religion = c.religion ?: "",
            caste = c.caste ?: "",
            city = c.city ?: "",
            motherTongue = c.motherTongue ?: "",
            verifiedOnly = c.name.contains("Verified", ignoreCase = true)
        )
        _circleResults.value = matchingRepo.recommendations(uid, MatchMode.ADVANCED, filter)
        _loadingResults.value = false
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirclesScreen(
    onBack: () -> Unit = {},
    onGoMatches: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: CirclesViewModel = hiltViewModel()
) {
    var selectedCat by remember { mutableStateOf("All") }

    val allCircles     by vm.circles.collectAsState()
    val currentFilter  by vm.currentFilter.collectAsState()
    val loading        by vm.loading.collectAsState()
    val selectedCircle by vm.selectedCircle.collectAsState()
    val circleResults  by vm.circleResults.collectAsState()
    val loadingResults by vm.loadingResults.collectAsState()

    val categories = remember(allCircles) { listOf("All") + allCircles.map { it.category ?: "Other" }.distinct() }
    val filtered = if (selectedCat == "All") allCircles else allCircles.filter { (it.category ?: "Other") == selectedCat }
    val totalMembers = allCircles.sumOf { it.memberCount ?: 0 }

    val activeDesc = buildList {
        if (currentFilter.religion.isNotBlank())     add("Religion: ${currentFilter.religion}")
        if (currentFilter.caste.isNotBlank())         add("Caste: ${currentFilter.caste}")
        if (currentFilter.city.isNotBlank())         add("City: ${currentFilter.city}")
        if (currentFilter.motherTongue.isNotBlank()) add("Language: ${currentFilter.motherTongue}")
        if (currentFilter.verifiedOnly)              add("Verified only")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectedCircle != null)
                        Text(selectedCircle!!.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    else
                        Text(t("community_circles", "Community Circles"))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCircle != null) vm.clearSelection() else onBack()
                    }, modifier = Modifier.testTag("circles_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(20.dp).padding(2.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (selectedCircle != null) {
                        TextButton(onClick = {
                            vm.applyAsGlobalFilter(selectedCircle!!)
                            onGoMatches()
                        }) {
                            Icon(Icons.Filled.FilterAlt, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(t("apply_globally", "Apply globally"))
                        }
                    }
                    if (activeDesc.isNotEmpty() && selectedCircle == null) {
                        TextButton(onClick = vm::clearFilters, modifier = Modifier.testTag("circles_clear_filters")) {
                            Icon(Icons.Filled.FilterAltOff, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(t("clear", "Clear"))
                        }
                    }
                }
            )
        }
    ) { pad ->
        if (selectedCircle != null) {
            CircleDetailView(
                circle = selectedCircle!!,
                results = circleResults,
                loading = loadingResults,
                onOpenProfile = onOpenProfile,
                onApplyGlobal = { vm.applyAsGlobalFilter(selectedCircle!!); onGoMatches() },
                modifier = Modifier.padding(pad)
            )
        } else {
            CircleBrowseView(
                circles = filtered,
                allCircles = allCircles,
                categories = categories,
                selectedCat = selectedCat,
                onSelectCat = { selectedCat = it },
                totalMembers = totalMembers,
                activeDesc = activeDesc,
                onSelectCircle = vm::selectCircle,
                onClearFilters = vm::clearFilters,
                onGoMatches = onGoMatches,
                modifier = Modifier.padding(pad)
            )
        }
    }
}

// ── Browse View ──────────────────────────────────────────────────────────────
@Composable
private fun CircleBrowseView(
    circles: List<CommunityGroupDto>,
    allCircles: List<CommunityGroupDto>,
    categories: List<String>,
    selectedCat: String,
    onSelectCat: (String) -> Unit,
    totalMembers: Int,
    activeDesc: List<String>,
    onSelectCircle: (CommunityGroupDto) -> Unit,
    onClearFilters: () -> Unit,
    onGoMatches: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier.fillMaxSize().testTag("circles_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active filter banner
        if (activeDesc.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.FilterAlt, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(activeDesc.joinToString("  •  "), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = onGoMatches, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                            Text("Browse", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Hero
        item {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vivah Circles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Tap a community circle to instantly see matching profiles with shared roots, religion, caste and values.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        StatPill("${allCircles.size}", "Circles")
                        StatPill("${(totalMembers / 1000)}K+", "Members")
                        StatPill("${categories.size - 1}", "Categories")
                    }
                }
            }
        }

        // Category filter
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Filter by category", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(selected = selectedCat == cat, onClick = { onSelectCat(cat) },
                            label = { Text(cat) }, modifier = Modifier.testTag("circles_cat_${cat.lowercase()}"))
                    }
                }
            }
        }

        item {
            Text("${circles.size} circle${if (circles.size != 1) "s" else ""}" +
                if (selectedCat != "All") " in $selectedCat" else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(circles, key = { it.id ?: it.name }) { c ->
            CircleCard(c) { onSelectCircle(c) }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Circle Detail View (shows inline match results) ──────────────────────────
@Composable
private fun CircleDetailView(
    circle: CommunityGroupDto,
    results: List<MatchResult>,
    loading: Boolean,
    onOpenProfile: (Long) -> Unit,
    onApplyGlobal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filterLines = buildList {
        circle.religion?.takeIf { it.isNotBlank() }?.let { add("Religion: $it") }
        circle.caste?.takeIf { it.isNotBlank() }?.let { add("Caste: $it") }
        circle.city?.takeIf { it.isNotBlank() }?.let { add("City: $it") }
        circle.motherTongue?.takeIf { it.isNotBlank() }?.let { add("Language: $it") }
        if (circle.name.contains("Verified", ignoreCase = true)) add("Verified profiles only")
    }

    LazyColumn(
        modifier.fillMaxSize().testTag("circles_detail"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Circle info header
        item {
            val accent = accentFor(circle.category)
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.15f), modifier = Modifier.size(56.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text((circle.name.firstOrNull() ?: 'C').uppercase(),
                                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent)
                            }
                        }
                        Column {
                            Text(circle.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            circle.category?.let {
                                Text(it, style = MaterialTheme.typography.labelMedium, color = accent)
                            }
                        }
                    }
                    circle.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (filterLines.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        Text("Active filters for this circle:", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium)
                        filterLines.forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(line, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    circle.memberCount?.let { cnt ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Group, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("${cnt.abbreviated()} community members", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    FilledTonalButton(onClick = onApplyGlobal) {
                        Icon(Icons.Filled.FilterAlt, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apply as global filter & go to Matches")
                    }
                }
            }
        }

        // Results count
        item {
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Loading circle matches...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("${results.size} match${if (results.size != 1) "es" else ""} in this circle",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }

        if (!loading && results.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.SearchOff, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Text("No matches in this circle yet", style = MaterialTheme.typography.titleSmall)
                        Text("Try a different community circle", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Inline match cards
        items(results, key = { it.user.id }) { r ->
            InlineMatchCard(r, onOpenProfile)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Inline Match Card ────────────────────────────────────────────────────────
@Composable
private fun InlineMatchCard(r: MatchResult, onOpen: (Long) -> Unit) {
    val p = r.user
    ElevatedCard(onClick = { onOpen(p.id) }, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("circle_match_${p.id}")) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(50.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(p.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${p.displayName}, ${p.age}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (p.isVerified) Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = Color(0xFF1976D2))
                    if (p.isPremium) Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = Color(0xFFFFB300))
                }
                Text("${p.city} • ${p.profession}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val attrs = buildList {
                    if (p.religion.isNotBlank()) add(p.religion)
                    if (p.caste.isNotBlank()) add(p.caste)
                    if (p.motherTongue.isNotBlank()) add(p.motherTongue)
                }
                if (attrs.isNotEmpty()) {
                    Text(attrs.joinToString(" • "), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            val bg = when { r.displayScore >= 90 -> Color(0xFF2E7D32); r.displayScore >= 70 -> Color(0xFF1565C0)
                else -> MaterialTheme.colorScheme.surfaceVariant }
            val fg = when { r.displayScore >= 90 -> Color(0xFF2E7D32); r.displayScore >= 70 -> Color(0xFF1565C0)
                else -> MaterialTheme.colorScheme.onSurfaceVariant }
            Surface(shape = RoundedCornerShape(20.dp), color = bg.copy(alpha = 0.15f)) {
                Text("${r.displayScore}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    color = fg, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────
@Composable
private fun StatPill(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CircleCard(c: CommunityGroupDto, onBrowse: () -> Unit) {
    val accent = accentFor(c.category)
    ElevatedCard(shape = RoundedCornerShape(16.dp), onClick = onBrowse,
        modifier = Modifier.fillMaxWidth().testTag("circle_card_${c.id}")) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.15f), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text((c.name.firstOrNull() ?: 'C').uppercase(),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    c.category?.let { cat ->
                        Surface(shape = RoundedCornerShape(20.dp), color = accent.copy(alpha = 0.12f)) {
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = accent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
                        }
                    }
                }
                c.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                val tags = buildList {
                    c.religion?.takeIf { it.isNotBlank() }?.let { add(it) }
                    c.caste?.takeIf { it.isNotBlank() }?.let { add(it) }
                    c.city?.takeIf { it.isNotBlank() }?.let { add(it) }
                    c.motherTongue?.takeIf { it.isNotBlank() }?.let { add(it) }
                }
                if (tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(tags) { tag ->
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(tag, style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                c.memberCount?.let { cnt ->
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Group, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("${cnt.abbreviated()} members", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp).padding(2.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun Int.abbreviated(): String = if (this >= 1_000) "${this / 1_000}K" else "$this"
