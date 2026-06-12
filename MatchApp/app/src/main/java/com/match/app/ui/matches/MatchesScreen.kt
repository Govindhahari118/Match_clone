@file:Suppress("UNUSED_PARAMETER", "UnusedPrivateMember")
package com.match.app.ui.matches

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
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
import com.match.app.data.repo.ShortlistRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.MatchResult
import com.match.app.ui.common.ShimmerList
import com.match.app.ui.components.EmptyState
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Filter chip model ────────────────────────────────────────────────────────
data class ActiveFilter(val label: String, val field: String)

data class MatchesUi(
    val loading: Boolean = true,
    val items: List<MatchResult> = emptyList(),
    val mode: MatchMode = MatchMode.ADVANCED,
    val likedIds: Set<Long> = emptySet(),
    val shortlistedIds: Set<Long> = emptySet(),
    val filter: MatchFilter = MatchFilter(),
    val recentFilters: List<MatchFilter> = emptyList(),
    val lastClearedFilter: MatchFilter? = null,
    val uiLanguage: String = "en",
    val communitySetupDone: Boolean = false
)

enum class SimpleCommunityPreset(val label: String) {
    ALL("All"),
    TELUGU("Telugu"),
    HINDU("Hindu"),
    BRAHMIN("Brahmin"),
    CHRISTIAN("Christian"),
    MUSLIM("Muslim")
}

@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val session: SessionStore,
    private val repo: MatchingRepository,
    private val social: SocialRepository,
    private val shortlistRepo: ShortlistRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(MatchesUi())
    val ui: StateFlow<MatchesUi> = _ui.asStateFlow()
    private val recentFilters = mutableListOf<MatchFilter>()

    private val userId = session.userId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val mode   = session.mode.stateIn(viewModelScope, SharingStarted.Eagerly, MatchMode.ADVANCED)
    private val filter = session.filter.stateIn(viewModelScope, SharingStarted.Eagerly, MatchFilter())
    private val uiLanguage = session.uiLanguage.stateIn(viewModelScope, SharingStarted.Eagerly, "en")
    private val setupDone = session.communitySetupDone.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            combine(userId, mode, filter, uiLanguage, setupDone) { uid, m, f, lang, setup ->
                Quint(uid, m, f, lang, setup)
            }.collectLatest { tuple ->
                    val uid = tuple.uid
                    val m = tuple.mode
                    val f = tuple.filter
                    val lang = tuple.lang
                    val setup = tuple.setupDone
                    if (uid == null) return@collectLatest
                    _ui.update { it.copy(loading = true, mode = m, filter = f, uiLanguage = lang, communitySetupDone = setup) }
                    try {
                        val items = repo.recommendations(uid, m, f)
                        // load liked + shortlisted
                        val likedIds   = items.map { it.user.id }.filter { social.isLiked(uid, it) }.toSet()
                        val savedIds   = items.map { it.user.id }.filter { shortlistRepo.isSaved(uid, it) }.toSet()
                        _ui.update { it.copy(loading = false, items = items,
                            likedIds = likedIds, shortlistedIds = savedIds) }
                    } catch (e: Exception) {
                        _ui.update { it.copy(loading = false) }
                    }
                }
        }
    }

    private data class Quint(
        val uid: Long?,
        val mode: MatchMode,
        val filter: MatchFilter,
        val lang: String,
        val setupDone: Boolean
    )

    fun setMode(m: MatchMode) = viewModelScope.launch { session.setMode(m) }

    fun toggleLike(targetId: Long) = viewModelScope.launch {
        val uid = userId.value ?: return@launch
        val nowLiked = social.toggleLike(uid, targetId)
        _ui.update { prev ->
            prev.copy(likedIds = if (nowLiked) prev.likedIds + targetId else prev.likedIds - targetId)
        }
    }

    fun toggleShortlist(targetId: Long) = viewModelScope.launch {
        val uid = userId.value ?: return@launch
        val nowSaved = shortlistRepo.toggle(uid, targetId)
        _ui.update { prev ->
            prev.copy(shortlistedIds = if (nowSaved) prev.shortlistedIds + targetId else prev.shortlistedIds - targetId)
        }
    }

    fun refresh() = viewModelScope.launch {
        val uid = userId.value ?: return@launch
        _ui.update { it.copy(loading = true) }
        try {
            val items = repo.recommendations(uid, mode.value, filter.value)
            _ui.update { it.copy(loading = false, items = items) }
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false) }
        }
    }

    /** Clear every active filter and reload. */
    fun clearAllFilters() = viewModelScope.launch {
        val previous = filter.value
        _ui.value = _ui.value.copy(lastClearedFilter = previous)
        session.setFilter(MatchFilter())
    }

    fun undoClearFilters() = viewModelScope.launch {
        val restore = _ui.value.lastClearedFilter ?: return@launch
        session.setFilter(restore)
        pushRecentFilter(restore)
        _ui.value = _ui.value.copy(lastClearedFilter = null)
    }

    /** Clear a single filter dimension by field name. */
    fun clearSingleFilter(field: String) = viewModelScope.launch {
        val cur = filter.value
        session.setFilter(when (field) {
            "religion"      -> cur.copy(religion = "")
            "caste"         -> cur.copy(caste = "")
            "motherTongue"  -> cur.copy(motherTongue = "")
            "city"          -> cur.copy(city = "")
            "state"         -> cur.copy(state = "")
            "maritalStatus" -> cur.copy(maritalStatus = "")
            "verifiedOnly"  -> cur.copy(verifiedOnly = false)
            "age"           -> cur.copy(ageMin = 18, ageMax = 70)
            "diet"          -> cur.copy(diet = "")
            "education"     -> cur.copy(educationLevel = "")
            "residential"   -> cur.copy(residentialStatus = "")
            "children"      -> cur.copy(hasChildren = "")
            "keyword"       -> cur.copy(keyword = "")
            "gothra"        -> cur.copy(gothra = "")
            "income"        -> cur.copy(incomeMin = "", incomeMax = "")
            "occupationCategory" -> cur.copy(occupationCategory = "")
            "nriOnly"       -> cur.copy(nriOnly = false)
            "smoking"       -> cur.copy(smoking = "")
            "manglik"       -> cur.copy(manglik = "")
            "rasi"          -> cur.copy(rasi = "")
            else            -> cur
        })
    }

    fun setKeyword(kw: String) = viewModelScope.launch {
        val cur = filter.value
        session.setFilter(cur.copy(keyword = kw))
    }

    fun setFilter(newFilter: MatchFilter) = viewModelScope.launch {
        session.setFilter(newFilter)
        pushRecentFilter(newFilter)
    }

    fun setQuickCommunity(field: String, value: String) = viewModelScope.launch {
        val cur = filter.value
        // If same value → toggle off (clear), else set
        val updated = when (field) {
            "religion"     -> if (cur.religion == value) cur.copy(religion = "") else cur.copy(religion = value)
            "motherTongue" -> if (cur.motherTongue == value) cur.copy(motherTongue = "") else cur.copy(motherTongue = value)
            "caste"        -> if (cur.caste == value) cur.copy(caste = "") else cur.copy(caste = value)
            "maritalStatus"-> if (cur.maritalStatus == value) cur.copy(maritalStatus = "") else cur.copy(maritalStatus = value)
            else           -> cur
        }
        session.setFilter(updated)
        pushRecentFilter(updated)
    }

    fun clearCommunityFilters() = viewModelScope.launch {
        val updated = filter.value.copy(religion = "", motherTongue = "", caste = "")
        session.setFilter(updated)
        pushRecentFilter(updated)
    }

    fun setSimpleCommunityPreset(preset: SimpleCommunityPreset) = viewModelScope.launch {
        val cur = filter.value
        val base = cur.copy(religion = "", motherTongue = "", caste = "")
        val updated = when (preset) {
            SimpleCommunityPreset.ALL -> base
            SimpleCommunityPreset.TELUGU -> base.copy(motherTongue = "Telugu")
            SimpleCommunityPreset.HINDU -> base.copy(religion = "Hindu")
            SimpleCommunityPreset.BRAHMIN -> base.copy(caste = "Brahmin")
            SimpleCommunityPreset.CHRISTIAN -> base.copy(religion = "Christian")
            SimpleCommunityPreset.MUSLIM -> base.copy(religion = "Muslim")
        }
        session.setFilter(updated)
        pushRecentFilter(updated)
    }

    fun applySavedProfile(profileName: String) = viewModelScope.launch {
        val cur = filter.value
        val updated = when (profileName) {
            "Family Preferred" -> cur.copy(verifiedOnly = true, religion = "Hindu", maritalStatus = "Never Married")
            "City + Language" -> cur.copy(city = "Hyderabad", motherTongue = "Telugu")
            "Verified only" -> cur.copy(verifiedOnly = true)
            else -> cur
        }
        session.setFilter(updated)
        pushRecentFilter(updated)
    }

    fun applyRecentFilter(recent: MatchFilter) = viewModelScope.launch {
        session.setFilter(recent)
        pushRecentFilter(recent)
    }

    fun setUiLanguage(lang: String) = viewModelScope.launch {
        session.setUiLanguage(lang)
    }

    fun completeCommunitySetup(newFilter: MatchFilter) = viewModelScope.launch {
        session.setFilter(newFilter)
        session.setCommunitySetupDone(true)
        pushRecentFilter(newFilter)
    }

    fun dismissCommunitySetup() = viewModelScope.launch {
        session.setCommunitySetupDone(true)
    }

    private fun pushRecentFilter(f: MatchFilter) {
        if (!hasAnyUsefulFilter(f)) return
        recentFilters.removeAll { sameFilter(it, f) }
        recentFilters.add(0, f)
        if (recentFilters.size > 6) recentFilters.removeAt(recentFilters.lastIndex)
        _ui.value = _ui.value.copy(recentFilters = recentFilters.toList())
    }

    private fun hasAnyUsefulFilter(f: MatchFilter): Boolean {
        return f.religion.isNotBlank() || f.caste.isNotBlank() || f.motherTongue.isNotBlank() ||
            f.city.isNotBlank() || f.state.isNotBlank() || f.maritalStatus.isNotBlank() ||
            f.verifiedOnly || f.ageMin != 18 || f.ageMax != 70 || f.keyword.isNotBlank()
    }

    private fun sameFilter(a: MatchFilter, b: MatchFilter): Boolean {
        return a == b
    }
}

// ── Sort options ─────────────────────────────────────────────────────────────
enum class SortOption(val label: String) {
    BEST_MATCH("Best Match"),
    NEWEST("Newest"),
    AGE_ASC("Age ↑"),
    AGE_DESC("Age ↓"),
    SCORE_DESC("Score ↓")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onOpen: (Long) -> Unit = {},
    vm: MatchesViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSimpleFilterWizard by remember { mutableStateOf(false) }
    var showCommunitySetup by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(SortOption.BEST_MATCH) }
    var showSortMenu by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    
    // Build typed filter chip list
    val f = ui.filter
    val lang = ui.uiLanguage
    LaunchedEffect(ui.communitySetupDone) {
        showCommunitySetup = !ui.communitySetupDone
    }
    LaunchedEffect(f.keyword) {
        if (keyword != f.keyword) keyword = f.keyword
    }
    val suggestionTerms = remember {
        listOf(
            "Telugu", "Tamil", "Kannada", "Hindi", "Malayalam",
            "Hindu", "Christian", "Muslim", "Jain", "Sikh",
            "Brahmin", "Reddy", "Kamma", "Naidu", "Nair", "Iyengar", "Iyer",
            "Hyderabad", "Chennai", "Bengaluru", "Vijayawada", "Visakhapatnam"
        )
    }
    val keywordSuggestions = remember(keyword) {
        if (keyword.isBlank()) emptyList() else suggestionTerms
            .filter { it.contains(keyword, ignoreCase = true) && !it.equals(keyword, ignoreCase = true) }
            .take(6)
    }
    val activeFilters = buildList<ActiveFilter> {
        if (f.religion.isNotBlank())           add(ActiveFilter("Religion: ${f.religion}", "religion"))
        if (f.caste.isNotBlank())               add(ActiveFilter("Caste: ${f.caste}", "caste"))
        if (f.motherTongue.isNotBlank())       add(ActiveFilter("Language: ${f.motherTongue}", "motherTongue"))
        if (f.city.isNotBlank())               add(ActiveFilter("City: ${f.city}", "city"))
        if (f.state.isNotBlank())              add(ActiveFilter("State: ${f.state}", "state"))
        if (f.maritalStatus.isNotBlank())      add(ActiveFilter("Status: ${f.maritalStatus}", "maritalStatus"))
        if (f.verifiedOnly)                    add(ActiveFilter("Verified only", "verifiedOnly"))
        if (f.ageMin != 18 || f.ageMax != 70) add(ActiveFilter("Age: ${f.ageMin}–${f.ageMax}", "age"))
        if (f.diet.isNotBlank())               add(ActiveFilter("Diet: ${f.diet}", "diet"))
        if (f.educationLevel.isNotBlank())     add(ActiveFilter("Edu: ${f.educationLevel}", "education"))
        if (f.residentialStatus.isNotBlank())  add(ActiveFilter("Status: ${f.residentialStatus}", "residential"))
        if (f.hasChildren.isNotBlank() && f.hasChildren != "Any") add(ActiveFilter("Children: ${f.hasChildren}", "children"))
        if (f.keyword.isNotBlank())            add(ActiveFilter("Search: ${f.keyword}", "keyword"))
        if (f.gothra.isNotBlank())             add(ActiveFilter("Gothra: ${f.gothra}", "gothra"))
        if (f.incomeMin.isNotBlank() || f.incomeMax.isNotBlank()) add(ActiveFilter("Income: ${f.incomeMin}-${f.incomeMax}", "income"))
    }

    // Client-side sort of items
    val sortedItems = remember(ui.items, sortOption) {
        when (sortOption) {
            SortOption.BEST_MATCH  -> ui.items
            SortOption.NEWEST      -> ui.items.sortedByDescending { it.user.id }
            SortOption.AGE_ASC     -> ui.items.sortedBy { it.user.age }
            SortOption.AGE_DESC    -> ui.items.sortedByDescending { it.user.age }
            SortOption.SCORE_DESC  -> ui.items.sortedByDescending { it.displayScore }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("browse", tr(lang, "Browse Profiles", "ప్రొఫైల్స్ బ్రౌజ్ చేయండి"))) },
                actions = {
                    if (activeFilters.isNotEmpty()) {
                        IconButton(
                            onClick = vm::clearAllFilters,
                            modifier = Modifier.testTag("matches_clear_filters")
                        ) {
                            Icon(Icons.Filled.FilterAltOff, contentDescription = "Clear all filters",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { vm.setUiLanguage(if (lang == "en") "te" else "en") }) {
                        Icon(Icons.Filled.Translate, contentDescription = "Language")
                    }
                    IconButton(onClick = { showFilterSheet = true }, modifier = Modifier.testTag("filter_open")) {
                        BadgedBox(badge = { if (activeFilters.isNotEmpty()) Badge { Text("${activeFilters.size}") } }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                        }
                    }
                    IconButton(onClick = vm::refresh, modifier = Modifier.testTag("matches_refresh")) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    // Sort dropdown
                    Box {
                        IconButton(onClick = { showSortMenu = true }, modifier = Modifier.testTag("sort_open")) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            SortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (opt == sortOption) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            else Spacer(Modifier.size(16.dp))
                                            Text(opt.label)
                                        }
                                    },
                                    onClick = { sortOption = opt; showSortMenu = false }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().testTag("matches_screen")) {

            // ── Match mode switch (compact) ──────────────────────────────
            ModeSwitch(selected = ui.mode, onSelect = vm::setMode)

            // ── Quick Discovery Bar ─────────────────────────────────────
            QuickDiscoveryBar(
                filter = f,
                onToggleVerified = { vm.setFilter(f.copy(verifiedOnly = !f.verifiedOnly)) },
                onTogglePhoto = { vm.setFilter(f.copy(withPhotoOnly = !f.withPhotoOnly)) },
                onToggleNri = { vm.setFilter(f.copy(nriOnly = !f.nriOnly)) },
                onOpenFilter = { showFilterSheet = true }
            )

            // ── Daily Matches Row ───────────────────────────────────────
            if (!ui.loading && ui.items.isNotEmpty()) {
                DailyMatchesRow(items = ui.items, onOpen = onOpen)
            }

            // ── Keyword search bar ───────────────────────────────────────
            OutlinedTextField(
                value = keyword, onValueChange = { keyword = it },
                placeholder = { Text(t("search", tr(lang, "Search name, profession, city...", "పేరు, ఉద్యోగం, నగరం శోధించండి..."))) },
                leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(20.dp)) },
                trailingIcon = {
                    if (keyword.isNotBlank()) {
                        IconButton(onClick = { keyword = ""; vm.setKeyword("") }) {
                            Icon(Icons.Filled.Close, null, Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("keyword_search"),
                shape = RoundedCornerShape(24.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
            LaunchedEffect(keyword) {
                kotlinx.coroutines.delay(400)
                if (keyword != f.keyword) vm.setKeyword(keyword)
            }

            if (keywordSuggestions.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(keywordSuggestions) { suggestion ->
                        SuggestionChip(
                            onClick = {
                                keyword = suggestion
                                vm.setKeyword(suggestion)
                            },
                            label = { Text("${t("search", if (lang == "te") "శోధన" else "Search")}: $suggestion") }
                        )
                    }
                }
            }

            // ── Active filter chips (dismissible) + undo ─────────────────
            if (activeFilters.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("matches_filter_row")
                ) {
                    items(activeFilters) { chip ->
                        InputChip(
                            selected = true,
                            onClick = { vm.clearSingleFilter(chip.field) },
                            label = { Text(chip.label, style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove ${chip.label}",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { vm.clearSingleFilter(chip.field) }
                                )
                            },
                            modifier = Modifier.testTag("filter_chip_${chip.field}")
                        )
                    }
                    if (activeFilters.size > 1) {
                        item {
                            InputChip(
                                selected = false,
                                onClick = vm::clearAllFilters,
                                label = { Text("Clear all", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Filled.FilterAltOff, null, Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("matches_clear_all_inline")
                            )
                        }
                    }
                }
            }

            if (ui.lastClearedFilter != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    AssistChip(onClick = vm::undoClearFilters, label = { Text("Undo clear filters") })
                }
            }

            // ── Results count ────────────────────────────────────────────
            if (!ui.loading) {
                Text(
                    if (activeFilters.isNotEmpty()) "${sortedItems.size} profiles found · ${activeFilters.size} filter${if (activeFilters.size > 1) "s" else ""} active"
                    else "${sortedItems.size} profiles found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            when {
                ui.loading -> ShimmerList()
                ui.items.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(32.dp).testTag("matches_empty"),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "No matches found",
                        subtitle = if (activeFilters.isNotEmpty())
                            "Try clearing some filters above, or adjust in Settings."
                        else
                            "Try adjusting your filters in Settings, or switch matching mode.",
                        actionLabel = if (activeFilters.isNotEmpty()) "Clear all filters" else null,
                        onAction = { if (activeFilters.isNotEmpty()) vm.clearAllFilters() },
                        icon = Icons.Filled.SearchOff
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().testTag("matches_list")
                ) {
                    items(sortedItems, key = { it.user.id }) { r ->
                        SwipeableMatchCard(
                            r = r,
                            onOpen = onOpen,
                            liked = r.user.id in ui.likedIds,
                            shortlisted = r.user.id in ui.shortlistedIds,
                            onToggleLike = vm::toggleLike,
                            onToggleShortlist = vm::toggleShortlist
                        )
                    }
                }
            }
        }
    }

    // ── Quick filter bottom sheet ────────────────────────────────────────
    if (showFilterSheet) {
        QuickFilterSheet(
            current = f,
            onApply = { newFilter ->
                vm.setFilter(newFilter)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showSimpleFilterWizard) {
        SimpleFilterWizardSheet(
            lang = lang,
            current = f,
            onApply = { newFilter ->
                vm.setFilter(newFilter)
                showSimpleFilterWizard = false
            },
            onDismiss = { showSimpleFilterWizard = false }
        )
    }

    if (showCommunitySetup) {
        CommunitySetupSheet(
            lang = lang,
            current = f,
            onApply = {
                vm.completeCommunitySetup(it)
                showCommunitySetup = false
            },
            onSkip = {
                vm.dismissCommunitySetup()
                showCommunitySetup = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Community Chips (tap-to-filter shortcuts)
// ─────────────────────────────────────────────────────────────────────────────
private enum class QuickCategory(val label: String) {
    LANGUAGE("Language"),
    RELIGION("Religion"),
    COMMUNITY("Community")
}

private data class QuickChip(val label: String, val field: String, val value: String)

@Composable
private fun QuickCommunityBar(filter: MatchFilter, onSelect: (String, String) -> Unit, onClear: () -> Unit) {
    var selectedCategory by remember { mutableStateOf(QuickCategory.LANGUAGE) }
    val byCategory = remember {
        mapOf(
            QuickCategory.LANGUAGE to listOf(
                QuickChip("Telugu", "motherTongue", "Telugu"),
                QuickChip("Tamil", "motherTongue", "Tamil"),
                QuickChip("Kannada", "motherTongue", "Kannada"),
                QuickChip("Hindi", "motherTongue", "Hindi"),
                QuickChip("Malayalam", "motherTongue", "Malayalam")
            ),
            QuickCategory.RELIGION to listOf(
                QuickChip("Hindu", "religion", "Hindu"),
                QuickChip("Christian", "religion", "Christian"),
                QuickChip("Muslim", "religion", "Muslim"),
                QuickChip("Jain", "religion", "Jain"),
                QuickChip("Sikh", "religion", "Sikh")
            ),
            QuickCategory.COMMUNITY to listOf(
                QuickChip("Brahmin", "caste", "Brahmin"),
                QuickChip("Reddy", "caste", "Reddy"),
                QuickChip("Kamma", "caste", "Kamma"),
                QuickChip("Naidu", "caste", "Naidu"),
                QuickChip("Nair", "caste", "Nair")
            )
        )
    }
    val visibleChips = byCategory[selectedCategory].orEmpty()

    Column(Modifier.fillMaxWidth()) {
        Text(
            "Filter by one preference",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(QuickCategory.entries) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(visibleChips) { chip ->
                val active = when (chip.field) {
                    "motherTongue" -> filter.motherTongue == chip.value
                    "religion" -> filter.religion == chip.value
                    else -> filter.caste == chip.value
                }
                FilterChip(
                    selected = active,
                    onClick = { onSelect(chip.field, chip.value) },
                    label = { Text(chip.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("quick_${chip.field}_${chip.label}")
                )
            }
            item {
                AssistChip(
                    onClick = onClear,
                    label = { Text("Clear") }
                )
            }
        }
    }
}

@Composable
private fun CommunityEntryCards(onApplyPreset: (SimpleCommunityPreset) -> Unit) {
    val cards = listOf(
        SimpleCommunityPreset.TELUGU,
        SimpleCommunityPreset.BRAHMIN,
        SimpleCommunityPreset.HINDU,
        SimpleCommunityPreset.CHRISTIAN,
        SimpleCommunityPreset.MUSLIM,
        SimpleCommunityPreset.ALL
    )
    val gradients = listOf(
        listOf(Color(0xFFEF5350), Color(0xFFFF7043)),
        listOf(Color(0xFF5C6BC0), Color(0xFF42A5F5)),
        listOf(Color(0xFFFFA726), Color(0xFFFFD54F)),
        listOf(Color(0xFF26A69A), Color(0xFF80CBC4)),
        listOf(Color(0xFF8D6E63), Color(0xFFBCAAA4)),
        listOf(Color(0xFF7E57C2), Color(0xFF9575CD))
    )

    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(
            "Choose your community path",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cards.indices.toList()) { idx ->
                val preset = cards[idx]
                ElevatedCard(
                    onClick = { onApplyPreset(preset) },
                    modifier = Modifier.size(width = 138.dp, height = 88.dp)
                ) {
                    Box(
                        Modifier.fillMaxSize().background(Brush.linearGradient(gradients[idx])).padding(12.dp)
                    ) {
                        Text(
                            preset.label,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedProfilesRow(onApply: (String) -> Unit) {
    val profiles = listOf("Family Preferred", "City + Language", "Verified only")
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Saved filter profiles",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(profiles) { name ->
                SuggestionChip(onClick = { onApply(name) }, label = { Text(name) })
            }
        }
    }
}

@Composable
private fun StickyFilterSummary(filter: MatchFilter, resultCount: Int, onClear: () -> Unit) {
    val parts = buildList {
        if (filter.motherTongue.isNotBlank()) add(filter.motherTongue)
        if (filter.religion.isNotBlank()) add(filter.religion)
        if (filter.caste.isNotBlank()) add(filter.caste)
        if (filter.city.isNotBlank()) add(filter.city)
        if (filter.verifiedOnly) add("Verified")
        if (filter.ageMin != 18 || filter.ageMax != 70) add("Age ${filter.ageMin}-${filter.ageMax}")
    }
    val summary = if (parts.isEmpty()) "All profiles" else parts.joinToString(" + ")

    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$resultCount profiles for: $summary",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (parts.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun FilterCategoryBar(onCategoryClick: (String) -> Unit) {
    val categories = listOf("Basic", "Location", "Community", "Work/Edu", "Lifestyle", "Astrology")
    val icons = listOf(Icons.Filled.Person, Icons.Filled.Place, Icons.Filled.Groups, Icons.Filled.Work, Icons.Filled.Restaurant, Icons.Filled.AutoAwesome)
    
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories.size) { i ->
            FilterChip(
                selected = false,
                onClick = { onCategoryClick(categories[i]) },
                label = { Text(categories[i], style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(icons[i], null, Modifier.size(14.dp)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableMatchCard(
    r: MatchResult,
    onOpen: (Long) -> Unit,
    liked: Boolean,
    shortlisted: Boolean,
    onToggleLike: (Long) -> Unit,
    onToggleShortlist: (Long) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                // Swipe Right -> Like
                onToggleLike(r.user.id)
                false // Don't actually dismiss the card from UI, just trigger action
            } else if (it == SwipeToDismissBoxValue.EndToStart) {
                // Swipe Left -> Shortlist
                onToggleShortlist(r.user.id)
                false // Don't actually dismiss
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFFE91E63) // Pink for like
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary // Blue for shortlist
                else -> Color.Transparent
            }
            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Favorite
                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Bookmark
                else -> Icons.Filled.Favorite
            }
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(color).padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        },
        content = {
            MatchCard(
                r = r,
                onOpen = onOpen,
                liked = liked,
                shortlisted = shortlisted,
                onToggleLike = onToggleLike,
                onToggleShortlist = onToggleShortlist
            )
        },
        modifier = Modifier.animateContentSize()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Filter Bottom Sheet (Advanced version)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickFilterSheet(
    current: MatchFilter,
    initialSection: String = "Basic",
    onApply: (MatchFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var f by remember { mutableStateOf(current) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val religions   = listOf("Any", "Hindu", "Christian", "Muslim", "Jain", "Buddhist", "Sikh", "Parsi")
    val languages   = listOf("Any", "Telugu", "Tamil", "Kannada", "Hindi", "Malayalam", "Marathi", "Gujarati", "Bengali", "Punjabi", "Odia", "Assamese")
    val castes      = listOf("Any", "Brahmin", "Kshatriya", "Vaishya", "Reddy", "Kamma", "Kapu", "Naidu", "Nair", "Mudaliar", "Gounder", "Iyer", "Iyengar", "Velama", "Pillai", "Chettiar", "Lingayat", "Vokkaliga", "Bania", "Rajput", "Jat", "Arora", "Khatri")
    val maritalList = listOf("Any", "Never Married", "Divorced", "Widowed", "Awaiting Divorce")
    val dietList    = listOf("Any", "Vegetarian", "Non-Vegetarian", "Eggetarian", "Vegan")
    val eduList     = listOf("Any", "High School", "Diploma", "Graduate", "Post Graduate", "Doctorate")
    val incomeList  = listOf("Any", "Under 2 LPA", "2–5 LPA", "5–10 LPA", "10–20 LPA", "20–50 LPA", "50+ LPA")
    val workList    = listOf("Any", "Private Sector", "Government Sector", "Business/Self-Employed", "Not Working", "Home Maker")
    val rasiList    = listOf("Any", "Mesha", "Vrishabha", "Mithuna", "Karka", "Simha", "Kanya", "Tula", "Vrishchika", "Dhanu", "Makara", "Kumbha", "Meena")

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Search Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { f = MatchFilter() }) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            }

            Column(Modifier.weight(1f, fill = false).verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                
                // ── Basic Section ───────────────────────────────────────
                FilterGroup("Basic Preferences", Icons.Filled.Person) {
                    Text("Age Range: ${f.ageMin}–${f.ageMax} yrs", style = MaterialTheme.typography.titleSmall)
                    RangeSlider(
                        value = f.ageMin.toFloat()..f.ageMax.toFloat(),
                        onValueChange = { r -> f = f.copy(ageMin = r.start.toInt(), ageMax = r.endInclusive.toInt()) },
                        valueRange = 18f..70f,
                        steps = 51
                    )
                    FilterSection("Marital Status", maritalList, f.maritalStatus) { v -> f = f.copy(maritalStatus = if (v == "Any") "" else v) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Verified only", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = f.verifiedOnly, onCheckedChange = { f = f.copy(verifiedOnly = it) })
                    }
                }

                // ── Location Section ────────────────────────────────────
                FilterGroup("Location", Icons.Filled.Place) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = f.city, onValueChange = { f = f.copy(city = it) }, label = { Text("City") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(value = f.state, onValueChange = { f = f.copy(state = it) }, label = { Text("State") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("NRI Profiles Only", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = f.nriOnly, onCheckedChange = { f = f.copy(nriOnly = it) })
                    }
                }

                // ── Community Section ───────────────────────────────────
                FilterGroup("Community & Religion", Icons.Filled.Groups) {
                    FilterSection("Religion", religions, f.religion) { v -> f = f.copy(religion = if (v == "Any") "" else v) }
                    FilterSection("Mother Tongue", languages, f.motherTongue) { v -> f = f.copy(motherTongue = if (v == "Any") "" else v) }
                    FilterSection("Caste", castes, f.caste) { v -> f = f.copy(caste = if (v == "Any") "" else v) }
                    OutlinedTextField(value = f.gothra, onValueChange = { f = f.copy(gothra = it) }, label = { Text("Gothra") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }

                // ── Work & Education ────────────────────────────────────
                FilterGroup("Education & Career", Icons.Filled.Work) {
                    FilterSection("Education", eduList, f.educationLevel) { v -> f = f.copy(educationLevel = if (v == "Any") "" else v) }
                    FilterSection("Occupation Category", workList, f.occupationCategory) { v -> f = f.copy(occupationCategory = if (v == "Any") "" else v) }
                    FilterSection("Annual Income", incomeList, f.incomeMin.ifBlank { "Any" }) { v ->
                        if (v == "Any") f = f.copy(incomeMin = "", incomeMax = "")
                        else f = f.copy(incomeMin = v, incomeMax = v)
                    }
                }

                // ── Lifestyle ───────────────────────────────────────────
                FilterGroup("Lifestyle", Icons.Filled.Restaurant) {
                    FilterSection("Diet", dietList, f.diet) { v -> f = f.copy(diet = if (v == "Any") "" else v) }
                    FilterSection("Smoking", listOf("Any", "Never", "Occasionally", "Regularly"), f.smoking) { v -> f = f.copy(smoking = if (v == "Any") "" else v) }
                }

                // ── Astrology ───────────────────────────────────────────
                FilterGroup("Astrology", Icons.Filled.AutoAwesome) {
                    FilterSection("Rasi", rasiList, f.rasi) { v -> f = f.copy(rasi = if (v == "Any") "" else v) }
                    FilterSection("Manglik", listOf("Any", "Yes", "No", "Don't know"), f.manglik) { v -> f = f.copy(manglik = if (v == "Any") "" else v) }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onApply(f) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Apply Advanced Filters", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FilterGroup(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recently used filters
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecentFiltersRow(recent: List<MatchFilter>, onApply: (MatchFilter) -> Unit) {
    fun labelOf(f: MatchFilter): String {
        return when {
            f.motherTongue.isNotBlank() -> "${f.motherTongue}"
            f.caste.isNotBlank() -> "${f.caste}"
            f.religion.isNotBlank() -> "${f.religion}"
            f.city.isNotBlank() -> f.city
            f.verifiedOnly -> "Verified"
            else -> "Recent"
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            "Recently used",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recent.take(6)) { f ->
                AssistChip(
                    onClick = { onApply(f) },
                    label = { Text(labelOf(f)) }
                )
            }
        }
    }
}

private enum class SimpleWizardStep(val title: String) {
    LANGUAGE("Choose language"),
    RELIGION("Choose religion"),
    COMMUNITY("Choose community"),
    AGE("Choose age range")
}

private fun tr(lang: String, en: String, te: String): String = if (lang == "te") te else en

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunitySetupSheet(
    lang: String,
    current: MatchFilter,
    onApply: (MatchFilter) -> Unit,
    onSkip: () -> Unit
) {
    var f by remember { mutableStateOf(current) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val languages = listOf("Any", "Telugu", "Tamil", "Kannada", "Hindi")
    val religions = listOf("Any", "Hindu", "Christian", "Muslim", "Jain")
    val communities = listOf("Any", "Brahmin", "Reddy", "Kamma", "Naidu", "Nair")

    ModalBottomSheet(onDismissRequest = onSkip, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tr(lang, "Welcome to community setup", "కమ్యూనిటీ సెటప్‌కి స్వాగతం"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(tr(lang, "Pick your preferences to get cleaner matches from day one.", "మొదటి రోజు నుంచే మంచి మ్యాచులు కోసం మీ ప్రాధాన్యతలు ఎంచుకోండి."), style = MaterialTheme.typography.bodyMedium)

            FilterSection(tr(lang, "Mother Tongue", "మాతృభాష"), languages, f.motherTongue) { v ->
                f = f.copy(motherTongue = if (v == "Any") "" else v)
            }
            FilterSection(tr(lang, "Religion", "మతం"), religions, f.religion) { v ->
                f = f.copy(religion = if (v == "Any") "" else v)
            }
            FilterSection(tr(lang, "Community", "కులం / కమ్యూనిటీ"), communities, f.caste) { v ->
                f = f.copy(caste = if (v == "Any") "" else v)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                    Text(tr(lang, "Skip", "స్కిప్"))
                }
                Button(onClick = { onApply(f) }, modifier = Modifier.weight(1f)) {
                    Text(tr(lang, "Apply", "వర్తింపజేయి"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleFilterWizardSheet(
    lang: String,
    current: MatchFilter,
    onApply: (MatchFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var f by remember { mutableStateOf(current) }
    var step by remember { mutableStateOf(SimpleWizardStep.LANGUAGE) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val languages = listOf("Any", "Telugu", "Tamil", "Kannada", "Hindi", "Malayalam")
    val religions = listOf("Any", "Hindu", "Christian", "Muslim", "Jain", "Sikh")
    val castes = listOf("Any", "Brahmin", "Reddy", "Kamma", "Naidu", "Nair", "Iyer", "Iyengar")
    val steps = SimpleWizardStep.entries
    val stepIndex = steps.indexOf(step)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(tr(lang, "Simple Filter", "సింపుల్ ఫిల్టర్"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                when (step) {
                    SimpleWizardStep.LANGUAGE -> tr(lang, "Choose language", "భాష ఎంచుకోండి")
                    SimpleWizardStep.RELIGION -> tr(lang, "Choose religion", "మతం ఎంచుకోండి")
                    SimpleWizardStep.COMMUNITY -> tr(lang, "Choose community", "కమ్యూనిటీ ఎంచుకోండి")
                    SimpleWizardStep.AGE -> tr(lang, "Choose age range", "వయస్సు పరిధి ఎంచుకోండి")
                },
                style = MaterialTheme.typography.titleMedium
            )
            LinearProgressIndicator(progress = { (stepIndex + 1) / steps.size.toFloat() }, modifier = Modifier.fillMaxWidth())

            when (step) {
                SimpleWizardStep.LANGUAGE -> {
                    FilterSection(tr(lang, "Mother Tongue", "మాతృభాష"), languages, f.motherTongue) { v ->
                        f = f.copy(motherTongue = if (v == "Any") "" else v)
                    }
                    TextButton(onClick = { f = f.copy(motherTongue = "") }) { Text(tr(lang, "Reset this section", "ఈ భాగాన్ని రీసెట్ చేయండి")) }
                }
                SimpleWizardStep.RELIGION -> {
                    FilterSection(tr(lang, "Religion", "మతం"), religions, f.religion) { v ->
                        f = f.copy(religion = if (v == "Any") "" else v)
                    }
                    TextButton(onClick = { f = f.copy(religion = "") }) { Text(tr(lang, "Reset this section", "ఈ భాగాన్ని రీసెట్ చేయండి")) }
                }
                SimpleWizardStep.COMMUNITY -> {
                    FilterSection(tr(lang, "Community", "కమ్యూనిటీ"), castes, f.caste) { v ->
                        f = f.copy(caste = if (v == "Any") "" else v)
                    }
                    TextButton(onClick = { f = f.copy(caste = "") }) { Text(tr(lang, "Reset this section", "ఈ భాగాన్ని రీసెట్ చేయండి")) }
                }
                SimpleWizardStep.AGE -> {
                    Text(tr(lang, "Age: ${f.ageMin}-${f.ageMax}", "వయస్సు: ${f.ageMin}-${f.ageMax}"), style = MaterialTheme.typography.bodyMedium)
                    RangeSlider(
                        value = f.ageMin.toFloat()..f.ageMax.toFloat(),
                        onValueChange = { range ->
                            f = f.copy(ageMin = range.start.toInt(), ageMax = range.endInclusive.toInt())
                        },
                        valueRange = 18f..70f,
                        steps = 51
                    )
                    TextButton(onClick = { f = f.copy(ageMin = 18, ageMax = 70) }) { Text(tr(lang, "Reset this section", "ఈ భాగాన్ని రీసెట్ చేయండి")) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        step = if (stepIndex == 0) SimpleWizardStep.LANGUAGE else steps[stepIndex - 1]
                    },
                    enabled = stepIndex > 0,
                    modifier = Modifier.weight(1f)
                ) { Text(tr(lang, "Back", "వెనక్కి")) }

                if (stepIndex < steps.lastIndex) {
                    Button(
                        onClick = { step = steps[stepIndex + 1] },
                        modifier = Modifier.weight(1f)
                    ) { Text(tr(lang, "Next", "తర్వాత")) }
                } else {
                    Button(
                        onClick = { onApply(f) },
                        modifier = Modifier.weight(1f)
                    ) { Text(tr(lang, "Apply", "వర్తింపజేయి")) }
                }
            }
        }
    }
}

@Composable
private fun CommunityPresetRow(
    filter: MatchFilter,
    onApplyPreset: (SimpleCommunityPreset) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("community_presets")
    ) {
        items(SimpleCommunityPreset.entries) { preset ->
            FilterChip(
                selected = isPresetActive(preset, filter),
                onClick = { onApplyPreset(preset) },
                label = { Text(preset.label) }
            )
        }
    }
}

private fun isPresetActive(preset: SimpleCommunityPreset, f: MatchFilter): Boolean {
    return when (preset) {
        SimpleCommunityPreset.ALL -> f.religion.isBlank() && f.motherTongue.isBlank() && f.caste.isBlank()
        SimpleCommunityPreset.TELUGU -> f.motherTongue == "Telugu" && f.religion.isBlank() && f.caste.isBlank()
        SimpleCommunityPreset.HINDU -> f.religion == "Hindu" && f.motherTongue.isBlank() && f.caste.isBlank()
        SimpleCommunityPreset.BRAHMIN -> f.caste == "Brahmin" && f.religion.isBlank() && f.motherTongue.isBlank()
        SimpleCommunityPreset.CHRISTIAN -> f.religion == "Christian" && f.motherTongue.isBlank() && f.caste.isBlank()
        SimpleCommunityPreset.MUSLIM -> f.religion == "Muslim" && f.motherTongue.isBlank() && f.caste.isBlank()
    }
}

@Composable
private fun FilterSection(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        val scrollState = rememberScrollState()
        Row(Modifier.horizontalScroll(scrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { opt ->
                val isSelected = (opt == "Any" && selected.isBlank()) || opt == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(opt) },
                    label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun ModeSwitch(selected: MatchMode, onSelect: (MatchMode) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("mode_switch")
    ) {
        item { ModeChip("Questions", Icons.AutoMirrored.Filled.List, selected == MatchMode.QUESTIONNAIRE, "mode_questionnaire") { onSelect(MatchMode.QUESTIONNAIRE) } }
        item { ModeChip("Astrology", Icons.Filled.AutoAwesome, selected == MatchMode.ASTROLOGY, "mode_astrology") { onSelect(MatchMode.ASTROLOGY) } }
        item { ModeChip("Advanced", Icons.Filled.TipsAndUpdates, selected == MatchMode.ADVANCED, "mode_advanced") { onSelect(MatchMode.ADVANCED) } }
    }
}

@Composable
private fun ModeChip(label: String, icon: ImageVector, selected: Boolean, tag: String, onClick: () -> Unit) {
    FilterChip(
        modifier = Modifier.testTag(tag),
        selected = selected, onClick = onClick,
        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
        label = { Text(label) }
    )
}

@Composable
private fun MatchCard(
    r: MatchResult,
    onOpen: (Long) -> Unit,
    liked: Boolean,
    shortlisted: Boolean,
    onToggleLike: (Long) -> Unit,
    onToggleShortlist: (Long) -> Unit
) {
    val p = r.user
    ElevatedCard(
        onClick = { onOpen(p.id) },
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("match_card_${p.id}").animateContentSize()
    ) {
        Column(Modifier.padding(16.dp)) {
            // ── Header row ────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gradient avatar
                val avatarColors = remember(p.id) {
                    val palette = listOf(
                        listOf(Color(0xFFE91E63), Color(0xFFFF5722)),
                        listOf(Color(0xFF9C27B0), Color(0xFF3F51B5)),
                        listOf(Color(0xFF009688), Color(0xFF4CAF50)),
                        listOf(Color(0xFF1976D2), Color(0xFF00BCD4)),
                        listOf(Color(0xFF795548), Color(0xFF607D8B)),
                        listOf(Color(0xFFFF9800), Color(0xFFFFEB3B)),
                    )
                    palette[(p.id % palette.size).toInt()]
                }
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(avatarColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        p.displayName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${p.displayName}, ${p.age}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        if (p.isVerified) Icon(Icons.Filled.Verified, null, Modifier.size(15.dp), tint = Color(0xFF1976D2))
                        if (p.isPremium)  Icon(Icons.Filled.Star,     null, Modifier.size(14.dp), tint = Color(0xFFFFB300))
                        // "New" badge — joined in the last 7 days
                        if (p.createdAt > 0L && (System.currentTimeMillis() - p.createdAt) < 7 * 24 * 60 * 60 * 1000L) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                color = Color(0xFF4CAF50)
                            ) {
                                Text("NEW", style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold, color = Color.White,
                                    modifier = androidx.compose.ui.Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }
                    Text("${p.city} • ${p.profession}", style = MaterialTheme.typography.bodySmall)
                    Text("${p.religion} • ${p.motherTongue} • ${p.maritalStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Score badge
                ScoreBadge(r.displayScore)
            }

            // ── Community / caste chip row ────────────────────────────
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (p.religion.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9)) {
                        Text(p.religion, style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1B5E20), fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
                if (p.caste.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(p.caste, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
                if (p.state.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(p.state, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                }
            }

            // ── Extra details pills ───────────────────────────────────
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { InfoPill(Icons.Filled.School, p.education) }
                item { InfoPill(Icons.Filled.Height, "${p.heightCm} cm") }
                item { InfoPill(Icons.Filled.AutoAwesome, "${p.rasi} / ${p.nakshatra}") }
                if (p.diet.isNotBlank()) { item { InfoPill(Icons.Filled.Restaurant, p.diet) } }
                if (p.incomeBand.isNotBlank()) { item { InfoPill(Icons.Filled.CurrencyRupee, p.incomeBand) } }
                if (p.personalityType.isNotBlank()) { item { InfoPill(Icons.Filled.Psychology, p.personalityType) } }
                if (p.familyType.isNotBlank()) { item { InfoPill(Icons.Filled.Group, p.familyType) } }
            }

            if (p.bio.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(p.bio, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── Hobbies & Video indicator ─────────────────────────────
            val visibleHobbies = p.hobbies.take(4)
            if (visibleHobbies.isNotEmpty() || p.videoUrl.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    visibleHobbies.forEach { hobby ->
                        Surface(shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(hobby, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                    if (p.videoUrl.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1565C0).copy(alpha = 0.10f)) {
                            Row(Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayCircle, null, Modifier.size(12.dp),
                                    tint = Color(0xFF1565C0))
                                Spacer(Modifier.width(3.dp))
                                Text("Video", style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── AI "Why this match?" explanation ─────────────────────
            var whyExpanded by remember { mutableStateOf(false) }
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { whyExpanded = !whyExpanded },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.testTag("why_match_${p.id}")
            ) {
                Icon(Icons.Filled.TipsAndUpdates, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(4.dp))
                Text("Why this match?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(2.dp))
                Icon(
                    if (whyExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary
                )
            }
            if (whyExpanded) {
                val qPct = (r.questionnaireScore * 100).toInt()
                val aPct = (r.astrologyScore * 100).toInt()
                val reasons = buildList {
                    if (qPct >= 70) add("✅ ${qPct}% lifestyle & values alignment")
                    if (aPct >= 70) add("⭐ ${aPct}% Rasi / Nakshatra compatibility")
                    if (p.motherTongue.isNotBlank()) add("🗣 Same mother tongue — ${p.motherTongue}")
                    if (p.diet.isNotBlank()) add("🍽 Diet match — ${p.diet}")
                    if (p.familyType.isNotBlank()) add("🏠 ${p.familyType} family background")
                }
                if (reasons.isEmpty()) reasons.toMutableList().also { it.add("📊 ${r.displayScore}% overall compatibility score") }
                Surface(shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        (reasons.ifEmpty { listOf("📊 ${r.displayScore}% overall compatibility score") }).forEach { line ->
                            Text(line, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }

            // ── Score sub-bar ─────────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SubScore("Q", r.questionnaireScore)
                SubScore("A", r.astrologyScore)
                SubScore("★", r.combinedScore)
            }

            // ── Action buttons ────────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Send Interest (primary CTA)
                Button(
                    onClick = { onToggleLike(p.id) },
                    modifier = Modifier.weight(1f).height(42.dp).testTag("like_btn_${p.id}"),
                    colors = if (liked) ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE91E63),
                        contentColor   = Color.White
                    ) else ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(if (liked) Icons.Filled.Favorite else Icons.AutoMirrored.Filled.Send, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (liked) "Interested" else "Send Interest", style = MaterialTheme.typography.labelMedium)
                }
                // Shortlist
                OutlinedIconButton(
                    onClick = { onToggleShortlist(p.id) },
                    modifier = Modifier.testTag("shortlist_btn_${p.id}")
                ) {
                    Icon(
                        if (shortlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Shortlist",
                        tint = if (shortlisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                // View profile
                OutlinedIconButton(
                    onClick = { onOpen(p.id) },
                    modifier = Modifier.testTag("view_btn_${p.id}")
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = "View profile")
                }
            }
        }
    }
}

@Composable
private fun SubScore(label: String, score: Float) {
    val pct   = (score * 100).toInt()
    val color = when { pct >= 80 -> Color(0xFF2E7D32); pct >= 60 -> Color(0xFFEF6C00); else -> Color(0xFFC62828) }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.1f)) {
        Text("$label $pct%", style = MaterialTheme.typography.labelSmall, color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun InfoPill(icon: ImageVector, text: String) {
    if (text.isBlank()) return
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScoreBadge(percent: Int) {
    val color = when {
        percent >= 80 -> Color(0xFF2E7D32)
        percent >= 60 -> Color(0xFFEF6C00)
        else          -> Color(0xFFC62828)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
        Text("$percent%", style = MaterialTheme.typography.labelLarge, color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun QuickDiscoveryBar(
    filter: MatchFilter,
    onToggleVerified: () -> Unit,
    onTogglePhoto: () -> Unit,
    onToggleNri: () -> Unit,
    onOpenFilter: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = filter.verifiedOnly,
                onClick = onToggleVerified,
                label = { Text("Verified", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { if (filter.verifiedOnly) Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
            )
        }
        item {
            FilterChip(
                selected = filter.withPhotoOnly,
                onClick = onTogglePhoto,
                label = { Text("With Photo", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { if (filter.withPhotoOnly) Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
            )
        }
        item {
            FilterChip(
                selected = filter.nriOnly,
                onClick = onToggleNri,
                label = { Text("NRI", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { if (filter.nriOnly) Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
            )
        }
        item {
            AssistChip(
                onClick = onOpenFilter,
                label = { Text("All Filters", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Filled.Tune, null, Modifier.size(14.dp)) }
            )
        }
    }
}

@Composable
private fun DailyMatchesRow(items: List<MatchResult>, onOpen: (Long) -> Unit) {
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            "Daily Discovery",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items.take(5)) { r ->
                val p = r.user
                ElevatedCard(
                    onClick = { onOpen(p.id) },
                    modifier = Modifier.size(width = 160.dp, height = 200.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        // Gradient placeholder for photo
                        Box(Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))
                        ))
                        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(p.displayName, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("${p.age}, ${p.city}", color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                            ScoreBadge(r.displayScore)
                        }
                    }
                }
            }
        }
    }
}
