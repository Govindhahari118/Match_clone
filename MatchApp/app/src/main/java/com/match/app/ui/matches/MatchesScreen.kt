package com.match.app.ui.matches

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.match.app.core.activity.ActivityStatusHelper
import com.match.app.data.repo.MatchingRepository
import com.match.app.data.repo.SavedSearchPreset
import com.match.app.data.repo.SavedSearchRepository
import com.match.app.data.repo.ShortlistRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.MatchResult
import com.match.app.domain.profile.IndiaProfileCatalog
import com.match.app.ui.common.ShimmerList
import com.match.app.ui.components.EmptyState
import com.match.app.ui.theme.MatreeDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MatchesUi(
    val loading: Boolean = true,
    val items: List<MatchResult> = emptyList(),
    val mode: MatchMode = MatchMode.ADVANCED,
    val likedIds: Set<Long> = emptySet(),
    val shortlistedIds: Set<Long> = emptySet(),
    val filter: MatchFilter = MatchFilter(),
    val savedSearches: List<SavedSearchPreset> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    val astrologyApplicable: Boolean = false
)

enum class DiscoverySort(val label: String) {
    BEST("Best match"), ACTIVE("Recently active"), NEWEST("Newest"), AGE_LOW("Age: low to high"), AGE_HIGH("Age: high to low")
}

@HiltViewModel
class MatchesViewModel @Inject constructor(
    private val session: SessionStore,
    private val matching: MatchingRepository,
    private val social: SocialRepository,
    private val shortlist: ShortlistRepository,
    private val savedSearches: SavedSearchRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(MatchesUi())
    val ui: StateFlow<MatchesUi> = _ui.asStateFlow()

    private val userId = session.userId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val mode = session.mode.stateIn(viewModelScope, SharingStarted.Eagerly, MatchMode.ADVANCED)
    private val filter = session.filter.stateIn(viewModelScope, SharingStarted.Eagerly, MatchFilter())

    init {
        viewModelScope.launch {
            combine(userId, mode, filter) { uid, m, f -> Triple(uid, m, f) }
                .collectLatest { (uid, m, f) ->
                    if (uid == null) return@collectLatest
                    load(uid, m, f)
                }
        }
        viewModelScope.launch {
            savedSearches.observeRemote()
                .catch { emit(emptyList()) }
                .collect { presets -> _ui.update { it.copy(savedSearches = presets) } }
        }
    }

    private suspend fun load(uid: Long, mode: MatchMode, filter: MatchFilter) {
        val astrologyApplicable = matching.astrologyApplicable(uid)
        val effectiveMode = if (!astrologyApplicable && mode == MatchMode.ASTROLOGY) MatchMode.ADVANCED else mode
        val effectiveFilter = if (astrologyApplicable) filter else filter.copy(
            rasi = "",
            nakshatra = "",
            manglik = "",
            hasHoroscope = "",
            minPoruthamScore = 0
        )
        _ui.update {
            it.copy(
                loading = true,
                mode = effectiveMode,
                filter = effectiveFilter,
                error = null,
                astrologyApplicable = astrologyApplicable
            )
        }
        runCatching {
            val items = matching.recommendations(uid, effectiveMode, effectiveFilter)
            val liked = items.map { it.user.id }.filter { social.isLiked(uid, it) }.toSet()
            val saved = items.map { it.user.id }.filter { shortlist.isSaved(uid, it) }.toSet()
            Triple(items, liked, saved)
        }.onSuccess { (items, liked, saved) ->
            _ui.update { it.copy(loading = false, items = items, likedIds = liked, shortlistedIds = saved) }
        }.onFailure { error ->
            _ui.update { it.copy(loading = false, error = error.message?.take(180) ?: "Unable to load profiles.") }
        }
    }

    fun refresh() = viewModelScope.launch {
        val uid = userId.value ?: return@launch
        load(uid, mode.value, filter.value)
    }

    fun setMode(value: MatchMode) = viewModelScope.launch { session.setMode(value) }
    fun setFilter(value: MatchFilter) = viewModelScope.launch { session.setFilter(value) }
    fun clearFilters() = setFilter(MatchFilter())
    fun setKeyword(value: String) = setFilter(filter.value.copy(keyword = value.trim().take(64)))

    fun toggleLike(targetId: Long) = viewModelScope.launch {
        val uid = userId.value ?: return@launch
        runCatching { social.toggleLike(uid, targetId) }
            .onSuccess { nowLiked ->
                _ui.update { state ->
                    state.copy(likedIds = if (nowLiked) state.likedIds + targetId else state.likedIds - targetId)
                }
            }
            .onFailure { showMessage(it.message ?: "Could not update interest.") }
    }

    fun toggleShortlist(targetId: Long) = viewModelScope.launch {
        val uid = userId.value ?: return@launch
        runCatching { shortlist.toggle(uid, targetId) }
            .onSuccess { nowSaved ->
                _ui.update { state ->
                    state.copy(shortlistedIds = if (nowSaved) state.shortlistedIds + targetId else state.shortlistedIds - targetId)
                }
            }
            .onFailure { showMessage(it.message ?: "Could not update shortlist.") }
    }

    fun saveCurrentSearch(name: String) = viewModelScope.launch {
        runCatching { savedSearches.saveRemote(name, filter.value) }
            .onSuccess { showMessage("Search saved to your account.") }
            .onFailure { showMessage(it.message ?: "Could not save this search.") }
    }

    fun applySavedSearch(preset: SavedSearchPreset) = viewModelScope.launch {
        session.setFilter(preset.filter)
        showMessage("${preset.name} applied.")
    }

    fun deleteSavedSearch(preset: SavedSearchPreset) = viewModelScope.launch {
        runCatching { savedSearches.deleteRemote(preset.id) }
            .onSuccess { showMessage("Saved search deleted.") }
            .onFailure { showMessage(it.message ?: "Could not delete saved search.") }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }
    private fun showMessage(value: String) = _ui.update { it.copy(message = value.take(180)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onOpen: (Long) -> Unit = {},
    vm: MatchesViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var keyword by remember { mutableStateOf(ui.filter.keyword) }
    var showFilters by remember { mutableStateOf(false) }
    var showSaveSearch by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(DiscoverySort.BEST) }
    var sortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(ui.filter.keyword) {
        if (keyword != ui.filter.keyword) keyword = ui.filter.keyword
    }
    LaunchedEffect(keyword) {
        delay(450)
        if (keyword.trim() != ui.filter.keyword) vm.setKeyword(keyword)
    }
    LaunchedEffect(ui.message) {
        ui.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    val sortedItems = remember(ui.items, sort) {
        when (sort) {
            DiscoverySort.BEST -> ui.items
            DiscoverySort.ACTIVE -> ui.items.sortedByDescending { it.user.lastActiveAt }
            DiscoverySort.NEWEST -> ui.items.sortedByDescending { it.user.createdAt }
            DiscoverySort.AGE_LOW -> ui.items.sortedBy { it.user.age }
            DiscoverySort.AGE_HIGH -> ui.items.sortedByDescending { it.user.age }
        }
    }
    val activeCount = remember(ui.filter) { activeFilterCount(ui.filter) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Discover") },
                actions = {
                    IconButton(onClick = { showSaveSearch = true }, modifier = Modifier.testTag("save_search")) {
                        Icon(Icons.Filled.BookmarkAdd, "Save this search")
                    }
                    IconButton(onClick = { showFilters = true }, modifier = Modifier.testTag("filter_open")) {
                        BadgedBox(badge = { if (activeCount > 0) Badge { Text(activeCount.toString()) } }) {
                            Icon(Icons.Filled.Tune, "Filters")
                        }
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, "Sort") }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            DiscoverySort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    leadingIcon = { if (option == sort) Icon(Icons.Filled.Check, null) },
                                    onClick = { sort = option; sortMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = vm::refresh) { Icon(Icons.Filled.Refresh, "Refresh") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().testTag("matches_screen")) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it.take(64) },
                placeholder = { Text("Search name, @username, profile ID, job or city") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (keyword.isNotBlank()) IconButton(onClick = { keyword = "" }) {
                        Icon(Icons.Filled.Close, "Clear search")
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("keyword_search"),
                shape = RoundedCornerShape(24.dp)
            )

            QuickFilters(ui.filter, vm::setFilter, onAllFilters = { showFilters = true })

            if (ui.savedSearches.isNotEmpty()) {
                SavedSearchRow(ui.savedSearches, vm::applySavedSearch, vm::deleteSavedSearch)
            }

            ActiveFilterSummary(ui.filter, activeCount, onClear = vm::clearFilters)

            ModeRow(ui.mode, ui.astrologyApplicable, vm::setMode)

            when {
                ui.loading -> ShimmerList()
                ui.error != null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    EmptyState("Unable to load profiles", ui.error ?: "Try again.", "Retry", vm::refresh, Icons.Filled.CloudOff)
                }
                sortedItems.isEmpty() -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    EmptyState(
                        "No profiles found",
                        "Try widening age, state, language, religion or community preferences.",
                        if (activeCount > 0) "Clear filters" else null,
                        if (activeCount > 0) vm::clearFilters else null,
                        Icons.Filled.SearchOff
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("matches_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${sortedItems.size} profiles in this result set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(sortedItems, key = { it.user.firebaseUid.ifBlank { it.user.id.toString() } }) { result ->
                        DiscoveryCard(
                            result = result,
                            liked = result.user.id in ui.likedIds,
                            shortlisted = result.user.id in ui.shortlistedIds,
                            onOpen = { onOpen(result.user.id) },
                            onInterest = { vm.toggleLike(result.user.id) },
                            onShortlist = { vm.toggleShortlist(result.user.id) }
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        AllIndiaFilterSheet(
            current = ui.filter,
            astrologyApplicable = ui.astrologyApplicable,
            onApply = { vm.setFilter(it); showFilters = false },
            onDismiss = { showFilters = false }
        )
    }
    if (showSaveSearch) {
        SaveSearchDialog(
            onDismiss = { showSaveSearch = false },
            onSave = { vm.saveCurrentSearch(it); showSaveSearch = false }
        )
    }
}

@Composable
private fun QuickFilters(filter: MatchFilter, onChange: (MatchFilter) -> Unit, onAllFilters: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = filter.lastActiveWithinDays == 1,
                onClick = { onChange(filter.copy(lastActiveWithinDays = if (filter.lastActiveWithinDays == 1) 0 else 1)) },
                label = { Text("Active today") },
                leadingIcon = { Icon(Icons.Filled.Circle, null, Modifier.size(9.dp), tint = MatreeDesign.colors.online) }
            )
        }
        item {
            FilterChip(
                selected = filter.verifiedOnly,
                onClick = { onChange(filter.copy(verifiedOnly = !filter.verifiedOnly)) },
                label = { Text("Verified") }
            )
        }
        item {
            FilterChip(
                selected = filter.withPhotoOnly,
                onClick = { onChange(filter.copy(withPhotoOnly = !filter.withPhotoOnly)) },
                label = { Text("With photo") }
            )
        }
        item {
            FilterChip(
                selected = filter.nriOnly,
                onClick = { onChange(filter.copy(nriOnly = !filter.nriOnly)) },
                label = { Text("NRI") }
            )
        }
        item { AssistChip(onClick = onAllFilters, label = { Text("All filters") }, leadingIcon = { Icon(Icons.Filled.Tune, null) }) }
    }
}

@Composable
private fun SavedSearchRow(
    presets: List<SavedSearchPreset>,
    onApply: (SavedSearchPreset) -> Unit,
    onDelete: (SavedSearchPreset) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Saved searches",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets, key = { it.id }) { preset ->
                InputChip(
                    selected = false,
                    onClick = { onApply(preset) },
                    label = { Text(preset.name, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Filled.Bookmark, null, Modifier.size(16.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { onDelete(preset) }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Filled.Close, "Delete ${preset.name}", Modifier.size(15.dp))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterSummary(filter: MatchFilter, count: Int, onClear: () -> Unit) {
    val summary = buildList {
        if (filter.state.isNotBlank()) add(filter.state)
        if (filter.motherTongue.isNotBlank()) add(filter.motherTongue)
        if (filter.religion.isNotBlank()) add(filter.religion)
        if (filter.caste.isNotBlank()) add(filter.caste)
        if (filter.subCaste.isNotBlank()) add(filter.subCaste)
        if (filter.ageMin != 18 || filter.ageMax != 70) add("${filter.ageMin}-${filter.ageMax} yrs")
        if (filter.lastActiveWithinDays > 0) add("Active ≤ ${filter.lastActiveWithinDays}d")
    }
    if (summary.isEmpty() && count == 0) return
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                summary.take(5).joinToString(" • ").ifBlank { "$count filters active" },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
            TextButton(onClick = onClear) { Text("Reset") }
        }
    }
}

@Composable
private fun ModeRow(
    selected: MatchMode,
    astrologyApplicable: Boolean,
    onSelect: (MatchMode) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { FilterChip(selected == MatchMode.ADVANCED, { onSelect(MatchMode.ADVANCED) }, { Text("Balanced") }) }
        item { FilterChip(selected == MatchMode.QUESTIONNAIRE, { onSelect(MatchMode.QUESTIONNAIRE) }, { Text("Values") }) }
        if (astrologyApplicable) {
            item { FilterChip(selected == MatchMode.ASTROLOGY, { onSelect(MatchMode.ASTROLOGY) }, { Text("Astrology") }) }
        }
    }
}

@Composable
private fun DiscoveryCard(
    result: MatchResult,
    liked: Boolean,
    shortlisted: Boolean,
    onOpen: () -> Unit,
    onInterest: () -> Unit,
    onShortlist: () -> Unit
) {
    val p = result.user
    val activity = remember(p.lastActiveAt, p.showLastActive) { ActivityStatusHelper.from(p) }
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (p.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = p.photoUrl,
                        contentDescription = "${p.displayName} profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(78.dp)
                    )
                } else {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(78.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(p.displayName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${p.displayName}, ${p.age}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (p.isVerified) {
                            Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Verified, "Verified", Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (p.username.isNotBlank()) Text("@${p.username}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(listOf(p.city, p.state, p.profession).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    Text(listOf(p.religion, p.caste, p.subCaste, p.motherTongue).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    if (p.showLastActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = if (activity.isOnline) MatreeDesign.colors.online else MaterialTheme.colorScheme.outline, modifier = Modifier.size(8.dp)) {}
                            Spacer(Modifier.width(6.dp))
                            Text(activity.label, style = MaterialTheme.typography.labelSmall, color = if (activity.isOnline) MatreeDesign.colors.online else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (p.education.isNotBlank() || p.maritalStatus.isNotBlank() || p.heightCm > 0) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (p.education.isNotBlank()) item { AssistChip(onClick = {}, enabled = false, label = { Text(p.education) }) }
                    if (p.maritalStatus.isNotBlank()) item { AssistChip(onClick = {}, enabled = false, label = { Text(p.maritalStatus) }) }
                    if (p.heightCm > 0) item { AssistChip(onClick = {}, enabled = false, label = { Text("${p.heightCm} cm") }) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onInterest, modifier = Modifier.weight(1f)) {
                    Icon(if (liked) Icons.Filled.Favorite else Icons.AutoMirrored.Filled.Send, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp)); Text(if (liked) "Interested" else "Send interest")
                }
                OutlinedIconButton(onClick = onShortlist) {
                    Icon(if (shortlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Shortlist")
                }
                OutlinedIconButton(onClick = onOpen) { Icon(Icons.Filled.Visibility, "View profile") }
            }
        }
    }
}

@Composable
private fun SaveSearchDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.BookmarkAdd, null) },
        title = { Text("Save this search") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Save the current filters to your account and reuse them on your other signed-in devices.")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Karnataka Telugu, Chennai professionals") },
                    singleLine = true
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllIndiaFilterSheet(
    current: MatchFilter,
    astrologyApplicable: Boolean,
    onApply: (MatchFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var f by remember { mutableStateOf(current) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scroll = rememberScrollState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 26.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Search preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("State → language → religion → community, plus any other preference you choose.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { f = MatchFilter() }) { Text("Reset") }
            }
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                FilterBlock("Basic") {
                    Text("Age ${f.ageMin}–${f.ageMax}")
                    RangeSlider(
                        value = f.ageMin.toFloat()..f.ageMax.toFloat(),
                        onValueChange = { f = f.copy(ageMin = it.start.toInt(), ageMax = it.endInclusive.toInt()) },
                        valueRange = 18f..70f,
                        steps = 51
                    )
                    OptionDropdown("Marital status", f.maritalStatus, listOf("Any") + IndiaProfileCatalog.maritalStatuses) { f = f.copy(maritalStatus = anyToBlank(it)) }
                    OptionDropdown("Last active", activityLabel(f.lastActiveWithinDays), listOf("Any time", "Online / today", "Last 7 days", "Last 30 days")) {
                        f = f.copy(lastActiveWithinDays = when (it) { "Online / today" -> 1; "Last 7 days" -> 7; "Last 30 days" -> 30; else -> 0 })
                    }
                    ToggleRow("Verified profiles only", f.verifiedOnly) { f = f.copy(verifiedOnly = it) }
                    ToggleRow("Profiles with photo", f.withPhotoOnly) { f = f.copy(withPhotoOnly = it) }
                }

                FilterBlock("Location") {
                    OptionDropdown("State / union territory", f.state, listOf("Any") + IndiaProfileCatalog.statesAndUnionTerritories) { selected ->
                        f = f.copy(state = anyToBlank(selected), motherTongue = if (selected == "Any") f.motherTongue else f.motherTongue)
                    }
                    OutlinedTextField(f.city, { f = f.copy(city = it.take(80)) }, label = { Text("City") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OptionDropdown("Native state", f.nativeState, listOf("Any") + IndiaProfileCatalog.statesAndUnionTerritories) { f = f.copy(nativeState = anyToBlank(it)) }
                    ToggleRow("NRI only", f.nriOnly) { f = f.copy(nriOnly = it) }
                    ToggleRow("Willing to relocate", f.willingToRelocate) { f = f.copy(willingToRelocate = it) }
                }

                FilterBlock("Language, religion & community") {
                    val suggestedLanguages = (IndiaProfileCatalog.languageSuggestionsForState(f.state) + IndiaProfileCatalog.indianLanguages).distinct()
                    OptionDropdown("Mother tongue", f.motherTongue, listOf("Any") + suggestedLanguages) { f = f.copy(motherTongue = anyToBlank(it)) }
                    OptionDropdown("Religion", f.religion, listOf("Any") + IndiaProfileCatalog.religions) { f = f.copy(religion = anyToBlank(it), caste = "", subCaste = "") }
                    Text("Common communities", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(IndiaProfileCatalog.communitySuggestions(f.religion)) { community ->
                            SuggestionChip(onClick = { if (community != "Other") f = f.copy(caste = community) }, label = { Text(community) })
                        }
                    }
                    OutlinedTextField(f.caste, { f = f.copy(caste = it.take(80)) }, label = { Text("Community / caste") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.subCaste, { f = f.copy(subCaste = it.take(80)) }, label = { Text("Sub-community / sub-caste") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.gothra, { f = f.copy(gothra = it.take(80)) }, label = { Text("Gothra / clan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                FilterBlock("Education & career") {
                    OptionDropdown("Education", f.educationLevel, listOf("Any") + IndiaProfileCatalog.educationLevels) { f = f.copy(educationLevel = anyToBlank(it)) }
                    OptionDropdown("Occupation category", f.occupationCategory, listOf("Any") + IndiaProfileCatalog.occupationCategories) { f = f.copy(occupationCategory = anyToBlank(it)) }
                    OptionDropdown("Employer type", f.employerType, listOf("Any") + IndiaProfileCatalog.employerTypes) { f = f.copy(employerType = anyToBlank(it)) }
                    OutlinedTextField(f.educationField, { f = f.copy(educationField = it.take(100)) }, label = { Text("Field of study") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.incomeMin, { f = f.copy(incomeMin = it.take(80)) }, label = { Text("Income range keyword") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                FilterBlock("Lifestyle & family") {
                    OptionDropdown("Diet", f.diet, listOf("Any") + IndiaProfileCatalog.diets) { f = f.copy(diet = anyToBlank(it)) }
                    OptionDropdown("Smoking", f.smoking, listOf("Any") + IndiaProfileCatalog.habitOptions) { f = f.copy(smoking = anyToBlank(it)) }
                    OptionDropdown("Drinking", f.drinking, listOf("Any") + IndiaProfileCatalog.habitOptions) { f = f.copy(drinking = anyToBlank(it)) }
                    OptionDropdown("Family type", f.familyType, listOf("Any") + IndiaProfileCatalog.familyTypes) { f = f.copy(familyType = anyToBlank(it)) }
                    OptionDropdown("Physical status", f.physicalStatus, listOf("Any") + IndiaProfileCatalog.physicalStatuses) { f = f.copy(physicalStatus = anyToBlank(it)) }
                }

                if (astrologyApplicable) {
                    FilterBlock("Astrology / Kundali") {
                        OptionDropdown("Horoscope", f.hasHoroscope.ifBlank { "Any" }, listOf("Any", "Yes", "No")) { f = f.copy(hasHoroscope = anyToBlank(it)) }
                        OutlinedTextField(f.rasi, { f = f.copy(rasi = it.take(60)) }, label = { Text("Rasi / moon sign") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(f.nakshatra, { f = f.copy(nakshatra = it.take(60)) }, label = { Text("Nakshatra / birth star") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OptionDropdown("Manglik", f.manglik.ifBlank { "Any" }, listOf("Any", "Yes", "No", "Partial (Anshik)", "Don't know")) { f = f.copy(manglik = anyToBlank(it)) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = { onApply(f) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("Apply filters")
            }
        }
    }
}

@Composable
private fun FilterBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
        HorizontalDivider()
    }
}

@Composable
private fun OptionDropdown(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.ifBlank { "Any" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.distinct().forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun anyToBlank(value: String) = if (value == "Any" || value == "Any time") "" else value
private fun activityLabel(days: Int) = when (days) { 1 -> "Online / today"; 7 -> "Last 7 days"; 30 -> "Last 30 days"; else -> "Any time" }

private fun activeFilterCount(f: MatchFilter): Int = listOf(
    f.ageMin != 18 || f.ageMax != 70,
    f.city.isNotBlank(), f.state.isNotBlank(), f.caste.isNotBlank(), f.subCaste.isNotBlank(),
    f.religion.isNotBlank(), f.motherTongue.isNotBlank(), f.maritalStatus.isNotBlank(),
    f.verifiedOnly, f.incomeMin.isNotBlank(), f.incomeMax.isNotBlank(), f.educationLevel.isNotBlank(),
    f.diet.isNotBlank(), f.residentialStatus.isNotBlank(), f.gothra.isNotBlank(), f.nativeState.isNotBlank(),
    f.countryOfResidence.isNotBlank(), f.nriOnly, f.willingToRelocate, f.recentlyJoinedDays > 0,
    f.smoking.isNotBlank(), f.drinking.isNotBlank(), f.familyType.isNotBlank(), f.familyStatus.isNotBlank(),
    f.physicalStatus.isNotBlank(), f.citizenship.isNotBlank(), f.educationField.isNotBlank(),
    f.occupationCategory.isNotBlank(), f.employerType.isNotBlank(), f.nakshatra.isNotBlank(),
    f.rasi.isNotBlank(), f.manglik.isNotBlank(), f.hobbies.isNotBlank(), !f.withPhotoOnly,
    f.verifiedLevel > 0, f.premiumOnly, f.lastActiveWithinDays > 0, f.hasHoroscope.isNotBlank(), f.keyword.isNotBlank()
).count { it }
