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
import com.match.app.ui.components.MatreeLoadingState
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.components.MatreeProfileCard
import com.match.app.ui.components.MatreeProfileCardVariant
import com.match.app.ui.components.MatreeStatePanel
import com.match.app.ui.components.MatreeStatusTone
import com.match.app.ui.i18n.t
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

enum class DiscoverySort {
    BEST, NEWEST, AGE_LOW, AGE_HIGH
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
    val localizedMessage = when (ui.message) {
        "Search saved to your account." -> t("search_saved", "Search saved to your account.")
        "Saved search deleted." -> t("saved_search_deleted", "Saved search deleted.")
        "Could not save this search." -> t("could_not_save_search", "Could not save this search.")
        "Could not delete saved search." -> t("could_not_delete_search", "Could not delete saved search.")
        "Could not update interest." -> t("could_not_update_interest", "Could not update interest.")
        "Could not update shortlist." -> t("could_not_update_shortlist", "Could not update shortlist.")
        else -> ui.message
    }
    LaunchedEffect(localizedMessage) {
        localizedMessage?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    val sortedItems = remember(ui.items, sort) {
        when (sort) {
            DiscoverySort.BEST -> ui.items
            DiscoverySort.NEWEST -> ui.items.sortedByDescending { it.user.createdAt }
            DiscoverySort.AGE_LOW -> ui.items.sortedBy { it.user.age }
            DiscoverySort.AGE_HIGH -> ui.items.sortedByDescending { it.user.age }
        }
    }
    val activeCount = remember(ui.filter) { activeFilterCount(ui.filter) }
    val emptyState = remember(ui.filter) { DiscoveryEmptyStatePolicy.resolve(ui.filter) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(t("discover", "Discover")) },
                actions = {
                    IconButton(onClick = { showSaveSearch = true }, modifier = Modifier.testTag("save_search")) {
                        Icon(Icons.Filled.BookmarkAdd, t("save_this_search", "Save this search"))
                    }
                    IconButton(onClick = { showFilters = true }, modifier = Modifier.testTag("filter_open")) {
                        BadgedBox(badge = { if (activeCount > 0) Badge { Text(activeCount.toString()) } }) {
                            Icon(Icons.Filled.Tune, t("filters", "Filters"))
                        }
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, t("sort", "Sort")) }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            DiscoverySort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(discoverySortLabel(option)) },
                                    leadingIcon = { if (option == sort) Icon(Icons.Filled.Check, null) },
                                    onClick = { sort = option; sortMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = vm::refresh) { Icon(Icons.Filled.Refresh, t("refresh", "Refresh")) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().testTag("matches_screen")) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it.take(64) },
                placeholder = { Text(t("search_profiles_hint", "Search name, @username, profile ID, job or city")) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (keyword.isNotBlank()) IconButton(onClick = { keyword = "" }) {
                        Icon(Icons.Filled.Close, t("clear_search", "Clear search"))
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = MatreeDesign.spacing.md, vertical = MatreeDesign.spacing.xs)
                    .testTag("keyword_search"),
                shape = RoundedCornerShape(MatreeDesign.radii.large)
            )

            QuickFilters(ui.filter, vm::setFilter, onAllFilters = { showFilters = true })

            if (ui.savedSearches.isNotEmpty()) {
                SavedSearchRow(ui.savedSearches, vm::applySavedSearch, vm::deleteSavedSearch)
            }

            ActiveFilterSummary(ui.filter, activeCount, onClear = vm::clearFilters)

            ModeRow(ui.mode, ui.astrologyApplicable, vm::setMode)

            when {
                ui.loading -> MatreeLoadingState(
                    modifier = Modifier.padding(MatreeDesign.spacing.md),
                    message = t("finding_eligible_profiles", "Finding eligible profiles…"),
                    rows = 4
                )
                ui.error != null -> Box(
                    Modifier.fillMaxSize().padding(MatreeDesign.spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    MatreeStatePanel(
                        title = t("unable_load_profiles", "Unable to load profiles"),
                        message = ui.error ?: t("try_again", "Try again."),
                        icon = Icons.Filled.CloudOff,
                        tone = MatreeStatusTone.ERROR,
                        primaryActionLabel = t("retry", "Retry"),
                        onPrimaryAction = vm::refresh
                    )
                }
                sortedItems.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(MatreeDesign.spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    MatreeStatePanel(
                        title = t("no_profiles_found", "No profiles found"),
                        message = localizedEmptyStateMessage(emptyState, ui.filter),
                        icon = Icons.Filled.SearchOff,
                        primaryActionLabel = localizedEmptyStateAction(emptyState),
                        onPrimaryAction = when (emptyState.action) {
                            DiscoveryEmptyState.Action.RESET -> {
                                { vm.clearFilters(); Unit }
                            }
                            DiscoveryEmptyState.Action.INCLUDE_NO_PHOTO -> {
                                { vm.setFilter(ui.filter.copy(withPhotoOnly = false)); Unit }
                            }
                            DiscoveryEmptyState.Action.RELAX_MUTUAL_MATCH -> {
                                { vm.setFilter(ui.filter.copy(minMutualMatchPercent = 0)); Unit }
                            }
                            DiscoveryEmptyState.Action.NONE -> null
                        }
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("matches_list"),
                    contentPadding = PaddingValues(MatreeDesign.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
                ) {
                    item {
                        Text(
                            t("result_count", mapOf("count" to sortedItems.size), "{count} profiles in this result set"),
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
                label = { Text(t("active_today", "Active today")) },
                leadingIcon = { Icon(Icons.Filled.Circle, null, Modifier.size(9.dp), tint = MatreeDesign.colors.online) }
            )
        }
        item {
            FilterChip(
                selected = filter.minMutualMatchPercent >= 90,
                onClick = {
                    onChange(
                        filter.copy(
                            minMutualMatchPercent =
                                if (filter.minMutualMatchPercent >= 90) 0 else 90
                        )
                    )
                },
                label = { Text(t("mutual_90_plus", "90%+ mutual")) }
            )
        }
        item {
            FilterChip(
                selected = filter.verifiedOnly,
                onClick = { onChange(filter.copy(verifiedOnly = !filter.verifiedOnly)) },
                label = { Text(t("verified", "Verified")) }
            )
        }
        item {
            FilterChip(
                selected = filter.withPhotoOnly,
                onClick = { onChange(filter.copy(withPhotoOnly = !filter.withPhotoOnly)) },
                label = { Text(t("with_photo", "With photo")) }
            )
        }
        item {
            FilterChip(
                selected = filter.nriOnly,
                onClick = { onChange(filter.copy(nriOnly = !filter.nriOnly)) },
                label = { Text(t("nri", "NRI")) }
            )
        }
        item { AssistChip(onClick = onAllFilters, label = { Text(t("all_filters", "All filters")) }, leadingIcon = { Icon(Icons.Filled.Tune, null) }) }
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
            t("saved_searches", "Saved searches"),
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
                            Icon(Icons.Filled.Close, t("delete_saved_search", mapOf("name" to preset.name), "Delete {name}"), Modifier.size(15.dp))
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
        if (filter.ageMin != 18 || filter.ageMax != 70) add("${filter.ageMin}-${filter.ageMax} ${t("years_suffix", "yrs")}")
        if (filter.heightMinCm != 90 || filter.heightMaxCm != 250) {
            add("${filter.heightMinCm}-${filter.heightMaxCm} cm")
        }
        if (filter.lastActiveWithinDays > 0) add(t("active_within_days", mapOf("days" to filter.lastActiveWithinDays), "Active ≤ {days}d"))
    }
    if (summary.isEmpty() && count == 0) return
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                summary.take(5).joinToString(" • ").ifBlank { t("filters_active", mapOf("count" to count), "{count} filters active") },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
            TextButton(onClick = onClear) { Text(t("reset", "Reset")) }
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
        item { FilterChip(selected == MatchMode.ADVANCED, { onSelect(MatchMode.ADVANCED) }, { Text(t("balanced", "Balanced")) }) }
        item { FilterChip(selected == MatchMode.QUESTIONNAIRE, { onSelect(MatchMode.QUESTIONNAIRE) }, { Text(t("values", "Values")) }) }
        if (astrologyApplicable) {
            item { FilterChip(selected == MatchMode.ASTROLOGY, { onSelect(MatchMode.ASTROLOGY) }, { Text(t("astrology", "Astrology")) }) }
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
    val primaryLine = listOf(p.city, p.state, p.profession)
        .filter { it.isNotBlank() }
        .joinToString(" • ")
    val secondaryLine = listOf(p.religion, p.caste, p.subCaste, p.motherTongue)
        .filter { it.isNotBlank() }
        .joinToString(" • ")
    val supporting = buildList {
        result.mutualPreferenceScore?.let { score ->
            val percent = (score.coerceIn(0f, 1f) * 100f).toInt()
            val evidenceReady = result.mutualPreferenceCriteria >= 5
            add(
                if (percent >= 90 && evidenceReady) {
                    t("strong_mutual_match", mapOf("percent" to percent, "count" to result.mutualPreferenceCriteria), "{percent}% strong mutual match • {count} criteria")
                } else {
                    t("mutual_preferences", mapOf("percent" to percent, "count" to result.mutualPreferenceCriteria), "{percent}% mutual preferences • {count} criteria")
                }
            )
        }
        if (result.forwardPreferenceScore != null && result.reversePreferenceScore != null) {
            val theyFitYou = (result.forwardPreferenceScore.coerceIn(0f, 1f) * 100f).toInt()
            val youFitThem = (result.reversePreferenceScore.coerceIn(0f, 1f) * 100f).toInt()
            add(t("reciprocal_fit", mapOf("they" to theyFitYou, "you" to youFitThem), "They fit you {they}% • You fit them {you}%"))
        }
        if (p.education.isNotBlank()) add(p.education)
        if (p.maritalStatus.isNotBlank()) add(p.maritalStatus)
        if (p.heightCm > 0) add("${p.heightCm} cm")
    }

    MatreeProfileCard(
        name = p.displayName,
        age = p.age.takeIf { it > 0 },
        username = p.username,
        primaryLine = primaryLine,
        secondaryLine = secondaryLine,
        photoModel = p.photoUrl.takeIf { it.isNotBlank() },
        isVerified = p.isVerified,
        isPremium = p.isPremium,
        activityLabel = activity.label.takeIf { p.showLastActive },
        isOnline = activity.isOnline,
        supportingLabels = supporting,
        variant = MatreeProfileCardVariant.STANDARD,
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MatreePrimaryButton(
                text = if (liked) t("interested", "Interested") else t("send_interest", "Send interest"),
                icon = if (liked) Icons.Filled.Favorite else Icons.AutoMirrored.Filled.Send,
                onClick = onInterest,
                modifier = Modifier.weight(1f)
            )
            OutlinedIconButton(
                onClick = onShortlist,
                modifier = Modifier.size(MatreeDesign.sizes.touchTarget)
            ) {
                Icon(
                    if (shortlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (shortlisted) t("remove_shortlist", "Remove from shortlist") else t("add_shortlist", "Add to shortlist")
                )
            }
            OutlinedIconButton(
                onClick = onOpen,
                modifier = Modifier.size(MatreeDesign.sizes.touchTarget)
            ) {
                Icon(Icons.Filled.Visibility, t("view_profile", "View profile"))
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
        title = { Text(t("save_this_search", "Save this search")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t("save_search_body", "Save the current filters to your account and reuse them on your other signed-in devices."))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(t("name", "Name")) },
                    placeholder = { Text(t("save_search_example", "e.g. Karnataka Telugu, Chennai professionals")) },
                    singleLine = true
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(t("save", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("cancel", "Cancel")) } }
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
                    Text(t("search_preferences", "Search preferences"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(t("search_preferences_subtitle", "State → language → religion → community, plus any other preference you choose."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { f = MatchFilter() }) { Text(t("reset", "Reset")) }
            }
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                FilterBlock(t("basic", "Basic")) {
                    Text(t("age_range", mapOf("min" to f.ageMin, "max" to f.ageMax), "Age {min}–{max}"))
                    RangeSlider(
                        value = f.ageMin.toFloat()..f.ageMax.toFloat(),
                        onValueChange = { f = f.copy(ageMin = it.start.toInt(), ageMax = it.endInclusive.toInt()) },
                        valueRange = 18f..70f,
                        steps = 51
                    )
                    Text(t("height_range", mapOf("min" to f.heightMinCm, "max" to f.heightMaxCm), "Height {min}–{max} cm"))
                    RangeSlider(
                        value = f.heightMinCm.toFloat()..f.heightMaxCm.toFloat(),
                        onValueChange = {
                            f = f.copy(
                                heightMinCm = it.start.toInt(),
                                heightMaxCm = it.endInclusive.toInt()
                            )
                        },
                        valueRange = 90f..250f,
                        steps = 159
                    )
                    OptionDropdown(t("marital_status", "Marital status"), f.maritalStatus, listOf("Any") + IndiaProfileCatalog.maritalStatuses) { f = f.copy(maritalStatus = anyToBlank(it)) }
                    OptionDropdown(t("last_active", "Last active"), activityLabel(f.lastActiveWithinDays), listOf("Any time", "Online / today", "Last 7 days", "Last 30 days")) {
                        f = f.copy(lastActiveWithinDays = when (it) { "Online / today" -> 1; "Last 7 days" -> 7; "Last 30 days" -> 30; else -> 0 })
                    }
                    OptionDropdown(t("recently_joined", "Recently joined"), recentlyJoinedLabel(f.recentlyJoinedDays), listOf("Any time", "Last 7 days", "Last 30 days", "Last 90 days")) {
                        f = f.copy(recentlyJoinedDays = when (it) { "Last 7 days" -> 7; "Last 30 days" -> 30; "Last 90 days" -> 90; else -> 0 })
                    }
                    ToggleRow(t("verified_profiles_only", "Verified profiles only"), f.verifiedOnly) { f = f.copy(verifiedOnly = it) }
                    ToggleRow(t("profiles_with_photo", "Profiles with photo"), f.withPhotoOnly) { f = f.copy(withPhotoOnly = it) }
                    ToggleRow(t("paid_members_only", "Paid members only"), f.premiumOnly) { f = f.copy(premiumOnly = it) }
                }

                FilterBlock(t("location", "Location")) {
                    OptionDropdown(t("state_union_territory", "State / union territory"), f.state, listOf("Any") + IndiaProfileCatalog.statesAndUnionTerritories) { selected ->
                        f = f.copy(state = anyToBlank(selected), motherTongue = if (selected == "Any") f.motherTongue else f.motherTongue)
                    }
                    OutlinedTextField(f.city, { f = f.copy(city = it.take(80)) }, label = { Text(t("city", "City")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OptionDropdown(t("native_state", "Native state"), f.nativeState, listOf("Any") + IndiaProfileCatalog.statesAndUnionTerritories) { f = f.copy(nativeState = anyToBlank(it)) }
                    OutlinedTextField(f.countryOfResidence, { f = f.copy(countryOfResidence = it.take(80)) }, label = { Text(t("country_of_residence", "Country of residence")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.citizenship, { f = f.copy(citizenship = it.take(80)) }, label = { Text(t("citizenship", "Citizenship")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OptionDropdown(t("residential_status", "Residential status"), f.residentialStatus, listOf("Any") + IndiaProfileCatalog.residentialStatuses) { f = f.copy(residentialStatus = anyToBlank(it)) }
                    OutlinedTextField(f.visaStatus, { f = f.copy(visaStatus = it.take(80)) }, label = { Text(t("visa_permit_status", "Visa / permit status")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    ToggleRow(t("nri_only", "NRI only"), f.nriOnly) { f = f.copy(nriOnly = it) }
                    ToggleRow(t("willing_to_relocate", "Willing to relocate"), f.willingToRelocate) { f = f.copy(willingToRelocate = it) }
                }

                FilterBlock(t("language_religion_community", "Language, religion & community")) {
                    val suggestedLanguages = (IndiaProfileCatalog.languageSuggestionsForState(f.state) + IndiaProfileCatalog.indianLanguages).distinct()
                    OptionDropdown(t("mother_tongue", "Mother tongue"), f.motherTongue, listOf("Any") + suggestedLanguages) { f = f.copy(motherTongue = anyToBlank(it)) }
                    OptionDropdown(t("religion", "Religion"), f.religion, listOf("Any") + IndiaProfileCatalog.religions) { f = f.copy(religion = anyToBlank(it), caste = "", subCaste = "") }
                    Text(t("common_communities", "Common communities"), style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(IndiaProfileCatalog.communitySuggestions(f.religion)) { community ->
                            SuggestionChip(onClick = { if (community != "Other") f = f.copy(caste = community) }, label = { Text(community) })
                        }
                    }
                    OutlinedTextField(f.caste, { f = f.copy(caste = it.take(80)) }, label = { Text(t("community_caste", "Community / caste")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.subCaste, { f = f.copy(subCaste = it.take(80)) }, label = { Text(t("subcommunity_subcaste", "Sub-community / sub-caste")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.gothra, { f = f.copy(gothra = it.take(80)) }, label = { Text(t("gothra_clan", "Gothra / clan")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.faithTradition, { f = f.copy(faithTradition = it.take(80)) }, label = { Text(t("faith_tradition_denomination", "Faith tradition / denomination")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.faithSubTradition, { f = f.copy(faithSubTradition = it.take(80)) }, label = { Text(t("faith_subtradition", "Faith sub-tradition")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.faithInstitution, { f = f.copy(faithInstitution = it.take(100)) }, label = { Text(t("faith_institution_community", "Faith institution / community")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                FilterBlock(t("education_career", "Education & career")) {
                    OptionDropdown(t("education", "Education"), f.educationLevel, listOf("Any") + IndiaProfileCatalog.educationLevels) { f = f.copy(educationLevel = anyToBlank(it)) }
                    OptionDropdown(t("occupation_category", "Occupation category"), f.occupationCategory, listOf("Any") + IndiaProfileCatalog.occupationCategories) { f = f.copy(occupationCategory = anyToBlank(it)) }
                    OptionDropdown(t("employer_type", "Employer type"), f.employerType, listOf("Any") + IndiaProfileCatalog.employerTypes) { f = f.copy(employerType = anyToBlank(it)) }
                    OutlinedTextField(f.educationField, { f = f.copy(educationField = it.take(100)) }, label = { Text(t("field_of_study", "Field of study")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(f.incomeMin, { f = f.copy(incomeMin = it.take(80)) }, label = { Text(t("income_range_keyword", "Income range keyword")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                FilterBlock(t("lifestyle_family", "Lifestyle & family")) {
                    OptionDropdown(t("diet", "Diet"), f.diet, listOf("Any") + IndiaProfileCatalog.diets) { f = f.copy(diet = anyToBlank(it)) }
                    OptionDropdown(t("smoking", "Smoking"), f.smoking, listOf("Any") + IndiaProfileCatalog.habitOptions) { f = f.copy(smoking = anyToBlank(it)) }
                    OptionDropdown(t("drinking", "Drinking"), f.drinking, listOf("Any") + IndiaProfileCatalog.habitOptions) { f = f.copy(drinking = anyToBlank(it)) }
                    OptionDropdown(t("family_type", "Family type"), f.familyType, listOf("Any") + IndiaProfileCatalog.familyTypes) { f = f.copy(familyType = anyToBlank(it)) }
                    OptionDropdown(t("family_status", "Family status"), f.familyStatus, listOf("Any") + IndiaProfileCatalog.familyStatuses) { f = f.copy(familyStatus = anyToBlank(it)) }
                    OptionDropdown(t("children", "Children"), f.hasChildrenFilter.ifBlank { "Don't mind" }, listOf("Don't mind", "No children", "Has children")) {
                        f = f.copy(hasChildrenFilter = if (it == "Don't mind") "" else it)
                    }
                    OptionDropdown(t("physical_status", "Physical status"), f.physicalStatus, listOf("Any") + IndiaProfileCatalog.physicalStatuses) { f = f.copy(physicalStatus = anyToBlank(it)) }
                    OutlinedTextField(f.hobbies, { f = f.copy(hobbies = it.take(200)) }, label = { Text(t("hobby_interest_keyword", "Hobby / interest keyword")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                if (astrologyApplicable) {
                    FilterBlock(t("astrology_kundali", "Astrology / Kundali")) {
                        OptionDropdown(t("horoscope", "Horoscope"), f.hasHoroscope.ifBlank { "Any" }, listOf("Any", "Yes", "No")) { f = f.copy(hasHoroscope = anyToBlank(it)) }
                        OutlinedTextField(f.rasi, { f = f.copy(rasi = it.take(60)) }, label = { Text(t("rasi_moon_sign", "Rasi / moon sign")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(f.nakshatra, { f = f.copy(nakshatra = it.take(60)) }, label = { Text(t("nakshatra_birth_star", "Nakshatra / birth star")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OptionDropdown(t("manglik", "Manglik"), f.manglik.ifBlank { "Any" }, listOf("Any", "Yes", "No", "Partial (Anshik)", "Don't know")) { f = f.copy(manglik = anyToBlank(it)) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = { onApply(f) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text(t("apply_filters", "Apply filters"))
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
            Text(localizedDiscoveryOption(value.ifBlank { "Any" }), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.distinct().forEach { option ->
                DropdownMenuItem(text = { Text(localizedDiscoveryOption(option)) }, onClick = { onSelect(option); expanded = false })
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


@Composable
private fun discoverySortLabel(sort: DiscoverySort): String = when (sort) {
    DiscoverySort.BEST -> t("sort_best_match", "Best match")
    DiscoverySort.NEWEST -> t("sort_newest", "Newest")
    DiscoverySort.AGE_LOW -> t("sort_age_low", "Age: low to high")
    DiscoverySort.AGE_HIGH -> t("sort_age_high", "Age: high to low")
}

@Composable
private fun localizedEmptyStateMessage(
    state: DiscoveryEmptyState,
    filter: MatchFilter
): String = when (state.action) {
    DiscoveryEmptyState.Action.RELAX_MUTUAL_MATCH -> t(
        "no_mutual_threshold_profiles",
        mapOf("percent" to filter.minMutualMatchPercent),
        "No profiles currently meet your {percent}%+ reciprocal preference threshold. Your strict preferences have not been changed."
    )
    DiscoveryEmptyState.Action.RESET -> t(
        "no_filter_profiles",
        "No profiles matched your current search settings. Reset or widen them to search a broader eligible set."
    )
    DiscoveryEmptyState.Action.INCLUDE_NO_PHOTO -> t(
        "no_photo_profiles",
        "No eligible profiles with a photo are available in this result set. You can include profiles without a photo."
    )
    DiscoveryEmptyState.Action.NONE -> t(
        "no_eligible_profiles",
        "No eligible profiles are available in this result set right now. Try refreshing later."
    )
}

@Composable
private fun localizedEmptyStateAction(state: DiscoveryEmptyState): String? = when (state.action) {
    DiscoveryEmptyState.Action.RELAX_MUTUAL_MATCH ->
        t("show_other_eligible_matches", "Show other eligible matches")
    DiscoveryEmptyState.Action.RESET ->
        t("reset_search_settings", "Reset search settings")
    DiscoveryEmptyState.Action.INCLUDE_NO_PHOTO ->
        t("include_profiles_without_photos", "Include profiles without photos")
    DiscoveryEmptyState.Action.NONE -> null
}

@Composable
private fun localizedDiscoveryOption(value: String): String = when (value) {
    "Any" -> t("any", "Any")
    "Any time" -> t("any_time", "Any time")
    "Online / today" -> t("online_today", "Online / today")
    "Last 7 days" -> t("last_7_days", "Last 7 days")
    "Last 30 days" -> t("last_30_days", "Last 30 days")
    "Last 90 days" -> t("last_90_days", "Last 90 days")
    "Don't mind" -> t("dont_mind", "Don't mind")
    "No children" -> t("no_children", "No children")
    "Has children" -> t("has_children", "Has children")
    "Yes" -> t("yes", "Yes")
    "No" -> t("no", "No")
    "Partial (Anshik)" -> t("partial_anshik", "Partial (Anshik)")
    "Don't know" -> t("dont_know", "Don't know")
    else -> value
}

private fun anyToBlank(value: String) = if (value == "Any" || value == "Any time") "" else value
private fun activityLabel(days: Int) = when (days) { 1 -> "Online / today"; 7 -> "Last 7 days"; 30 -> "Last 30 days"; else -> "Any time" }
private fun recentlyJoinedLabel(days: Int) = when (days) { 7 -> "Last 7 days"; 30 -> "Last 30 days"; 90 -> "Last 90 days"; else -> "Any time" }

private fun activeFilterCount(f: MatchFilter): Int = listOf(
    f.ageMin != 18 || f.ageMax != 70,
    f.heightMinCm != 90 || f.heightMaxCm != 250,
    f.minMutualMatchPercent > 0,
    f.city.isNotBlank(), f.state.isNotBlank(), f.caste.isNotBlank(), f.subCaste.isNotBlank(),
    f.religion.isNotBlank(), f.faithTradition.isNotBlank(), f.faithSubTradition.isNotBlank(),
    f.faithInstitution.isNotBlank(), f.motherTongue.isNotBlank(), f.maritalStatus.isNotBlank(),
    f.verifiedOnly, f.incomeMin.isNotBlank(), f.incomeMax.isNotBlank(), f.educationLevel.isNotBlank(),
    f.diet.isNotBlank(), f.residentialStatus.isNotBlank(), f.gothra.isNotBlank(), f.nativeState.isNotBlank(),
    f.countryOfResidence.isNotBlank(), f.visaStatus.isNotBlank(), f.nriOnly,
    f.willingToRelocate, f.recentlyJoinedDays > 0,
    f.smoking.isNotBlank(), f.drinking.isNotBlank(), f.familyType.isNotBlank(), f.familyStatus.isNotBlank(),
    f.hasChildrenFilter.isNotBlank(), f.physicalStatus.isNotBlank(), f.citizenship.isNotBlank(), f.educationField.isNotBlank(),
    f.occupationCategory.isNotBlank(), f.employerType.isNotBlank(), f.nakshatra.isNotBlank(),
    f.rasi.isNotBlank(), f.manglik.isNotBlank(), f.hobbies.isNotBlank(), !f.withPhotoOnly,
    f.verifiedLevel > 0, f.premiumOnly, f.lastActiveWithinDays > 0, f.hasHoroscope.isNotBlank(), f.keyword.isNotBlank()
).count { it }
