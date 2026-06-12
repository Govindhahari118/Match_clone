package com.match.app.ui.regions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.remote.RegionPresetDto
import com.match.app.data.remote.RegionPresetFiltersDto
import com.match.app.data.repo.CatalogRepository
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

// ── Quick-select preset (local icons) ────────────────────────────────────────
private data class QuickPreset(
    val label: String,
    val icon: ImageVector,
    val accentColor: Color,
    val religion: String = "",
    val city: String = "",
    val motherTongue: String = "",
    val caste: String = "",
    val state: String = ""
)

private val RELIGION_PRESETS = listOf(
    QuickPreset("Hindu",     Icons.Filled.TempleHindu,     Color(0xFFE65100), religion = "Hindu"),
    QuickPreset("Muslim",    Icons.Filled.Mosque,           Color(0xFF1B5E20), religion = "Muslim"),
    QuickPreset("Christian", Icons.Filled.Church,           Color(0xFF1A237E), religion = "Christian"),
    QuickPreset("Sikh",      Icons.Filled.Star,             Color(0xFF4A148C), religion = "Sikh"),
    QuickPreset("Jain",      Icons.Filled.Diamond,          Color(0xFF880E4F), religion = "Jain"),
    QuickPreset("Buddhist",  Icons.Filled.SelfImprovement,  Color(0xFF33691E), religion = "Buddhist"),
)

private val LANGUAGE_PRESETS = listOf(
    QuickPreset("Telugu",    Icons.Filled.Translate, Color(0xFF1565C0), motherTongue = "Telugu"),
    QuickPreset("Hindi",     Icons.Filled.Translate, Color(0xFF880E4F), motherTongue = "Hindi"),
    QuickPreset("Tamil",     Icons.Filled.Translate, Color(0xFF6A1B9A), motherTongue = "Tamil"),
    QuickPreset("Marathi",   Icons.Filled.Translate, Color(0xFF1B5E20), motherTongue = "Marathi"),
    QuickPreset("Gujarati",  Icons.Filled.Translate, Color(0xFFE65100), motherTongue = "Gujarati"),
    QuickPreset("Kannada",   Icons.Filled.Translate, Color(0xFF4A148C), motherTongue = "Kannada"),
    QuickPreset("Malayalam", Icons.Filled.Translate, Color(0xFF004D40), motherTongue = "Malayalam"),
    QuickPreset("Bengali",   Icons.Filled.Translate, Color(0xFF0277BD), motherTongue = "Bengali"),
)

private val CITY_PRESETS = listOf(
    QuickPreset("Hyderabad", Icons.Filled.LocationCity, Color(0xFF1A237E), city = "Hyderabad"),
    QuickPreset("Mumbai",    Icons.Filled.LocationCity, Color(0xFF37474F), city = "Mumbai"),
    QuickPreset("Bengaluru", Icons.Filled.LocationCity, Color(0xFF1B5E20), city = "Bengaluru"),
    QuickPreset("Chennai",   Icons.Filled.LocationCity, Color(0xFF880E4F), city = "Chennai"),
    QuickPreset("Delhi",     Icons.Filled.LocationCity, Color(0xFF4E342E), city = "Delhi"),
    QuickPreset("Pune",      Icons.Filled.LocationCity, Color(0xFF00695C), city = "Pune"),
    QuickPreset("Kolkata",   Icons.Filled.LocationCity, Color(0xFF4A148C), city = "Kolkata"),
    QuickPreset("Vijayawada",Icons.Filled.LocationCity, Color(0xFF0277BD), city = "Vijayawada"),
)

private val CASTE_PRESETS = listOf(
    QuickPreset("Reddy",    Icons.Filled.Groups, Color(0xFF6A1B9A), caste = "Reddy"),
    QuickPreset("Kamma",    Icons.Filled.Groups, Color(0xFF00695C), caste = "Kamma"),
    QuickPreset("Brahmin",  Icons.Filled.Groups, Color(0xFFE65100), caste = "Brahmin"),
    QuickPreset("Kapu",     Icons.Filled.Groups, Color(0xFF1A237E), caste = "Kapu"),
    QuickPreset("Velama",   Icons.Filled.Groups, Color(0xFF4A148C), caste = "Velama"),
    QuickPreset("Naidu",    Icons.Filled.Groups, Color(0xFF880E4F), caste = "Naidu"),
    QuickPreset("Yadav",    Icons.Filled.Groups, Color(0xFF1B5E20), caste = "Yadav"),
    QuickPreset("Maratha",  Icons.Filled.Groups, Color(0xFF37474F), caste = "Maratha"),
    QuickPreset("Rajput",   Icons.Filled.Groups, Color(0xFF4E342E), caste = "Rajput"),
    QuickPreset("Nair",     Icons.Filled.Groups, Color(0xFF004D40), caste = "Nair"),
    QuickPreset("Iyer",     Icons.Filled.Groups, Color(0xFF0277BD), caste = "Iyer"),
    QuickPreset("Iyengar",  Icons.Filled.Groups, Color(0xFF1565C0), caste = "Iyengar"),
)

private val STATE_PRESETS = listOf(
    QuickPreset("Telangana",    Icons.Filled.Map, Color(0xFF1A237E), state = "Telangana"),
    QuickPreset("Andhra Pradesh",Icons.Filled.Map, Color(0xFF0277BD), state = "Andhra Pradesh"),
    QuickPreset("Tamil Nadu",   Icons.Filled.Map, Color(0xFF6A1B9A), state = "Tamil Nadu"),
    QuickPreset("Karnataka",    Icons.Filled.Map, Color(0xFF1B5E20), state = "Karnataka"),
    QuickPreset("Kerala",       Icons.Filled.Map, Color(0xFF004D40), state = "Kerala"),
    QuickPreset("Maharashtra",  Icons.Filled.Map, Color(0xFFE65100), state = "Maharashtra"),
    QuickPreset("Gujarat",      Icons.Filled.Map, Color(0xFF880E4F), state = "Gujarat"),
    QuickPreset("Rajasthan",    Icons.Filled.Map, Color(0xFF4E342E), state = "Rajasthan"),
    QuickPreset("Uttar Pradesh",Icons.Filled.Map, Color(0xFF37474F), state = "Uttar Pradesh"),
    QuickPreset("West Bengal",  Icons.Filled.Map, Color(0xFF4A148C), state = "West Bengal"),
    QuickPreset("Punjab",       Icons.Filled.Map, Color(0xFF00695C), state = "Punjab"),
    QuickPreset("Bihar",        Icons.Filled.Map, Color(0xFF33691E), state = "Bihar"),
)

private val NRI_PRESETS = listOf(
    QuickPreset("USA",       Icons.Filled.Flight, Color(0xFF1A237E), state = "United States"),
    QuickPreset("UK",        Icons.Filled.Flight, Color(0xFF880E4F), state = "United Kingdom"),
    QuickPreset("Canada",    Icons.Filled.Flight, Color(0xFFE65100), state = "Canada"),
    QuickPreset("Australia", Icons.Filled.Flight, Color(0xFF1B5E20), state = "Australia"),
    QuickPreset("Germany",   Icons.Filled.Flight, Color(0xFF37474F), state = "Germany"),
    QuickPreset("UAE/Gulf",  Icons.Filled.Flight, Color(0xFF4E342E), state = "UAE"),
    QuickPreset("Singapore", Icons.Filled.Flight, Color(0xFF6A1B9A), state = "Singapore"),
)

private val FALLBACK_PRESETS = listOf(
    RegionPresetDto("nearby", "Nearby Matches",
        description = "Profiles from your city and surrounding areas.",
        filters = RegionPresetFiltersDto(),
        tags = listOf("Nearby", "Local")),
    RegionPresetDto("hyderabad-telugu", "Hyderabad Telugu Matches",
        description = "Telugu mother tongue profiles in Hyderabad, Telangana.",
        filters = RegionPresetFiltersDto(city = "Hyderabad", state = "Telangana", motherTongue = "Telugu"),
        tags = listOf("Telangana", "City", "Language")),
    RegionPresetDto("vijayawada-reddy", "Reddy Community - Vijayawada",
        description = "Reddy community profiles around Vijayawada, Andhra Pradesh.",
        filters = RegionPresetFiltersDto(city = "Vijayawada", caste = "Reddy", state = "Andhra Pradesh"),
        tags = listOf("Community", "Andhra Pradesh", "Caste")),
    RegionPresetDto("usa-telugu-nri", "Telugu NRI - United States",
        description = "US-based Telugu profiles for NRI matrimonial search.",
        filters = RegionPresetFiltersDto(motherTongue = "Telugu", country = "United States"),
        tags = listOf("NRI", "USA", "Telugu")),
    RegionPresetDto("bangalore-it", "Bengaluru IT Professionals",
        description = "Profiles in Bengaluru working in IT and Software.",
        filters = RegionPresetFiltersDto(city = "Bangalore", occupationCategory = "IT/Software"),
        tags = listOf("Career", "Karnataka")),
    RegionPresetDto("chennai-tamil", "Chennai Tamil Matches",
        description = "Tamil mother tongue profiles in Chennai, Tamil Nadu.",
        filters = RegionPresetFiltersDto(city = "Chennai", state = "Tamil Nadu", motherTongue = "Tamil"),
        tags = listOf("Tamil Nadu", "City", "Language")),
    // NRI popular destinations
    RegionPresetDto("uk-hindu-nri", "Hindu NRI - United Kingdom",
        description = "UK-based Hindu profiles seeking matches from India.",
        filters = RegionPresetFiltersDto(religion = "Hindu", country = "United Kingdom"),
        tags = listOf("NRI", "UK", "Hindu")),
    RegionPresetDto("canada-punjabi-nri", "Punjabi NRI - Canada",
        description = "Canadian Punjabi profiles for NRI matrimonial search.",
        filters = RegionPresetFiltersDto(motherTongue = "Punjabi", country = "Canada"),
        tags = listOf("NRI", "Canada", "Punjabi")),
    RegionPresetDto("gulf-muslim", "Muslim Professionals - Gulf",
        description = "Muslim profiles working in UAE, Saudi Arabia, and Gulf countries.",
        filters = RegionPresetFiltersDto(religion = "Muslim", country = "UAE"),
        tags = listOf("NRI", "Gulf", "Muslim")),
    // Profession-based
    RegionPresetDto("doctors-medical", "Doctors & Medical Professionals",
        description = "MBBS, MD, Dental, and allied medical professionals across India.",
        filters = RegionPresetFiltersDto(occupationCategory = "Medical/Healthcare"),
        tags = listOf("Career", "Doctors", "Medical")),
    RegionPresetDto("iit-iim-elite", "IIT / IIM / Premier Institute Alumni",
        description = "Graduates from India's top engineering and management institutes.",
        filters = RegionPresetFiltersDto(occupationCategory = "Engineering/Technology"),
        tags = listOf("Career", "Elite", "Education")),
    // Second marriage
    RegionPresetDto("second-marriage", "Second Marriage / Divorcee",
        description = "Profiles open to second marriage — divorcee, widowed, awaiting divorce.",
        filters = RegionPresetFiltersDto(maritalStatus = "Divorced"),
        tags = listOf("Second Marriage", "Divorcee")),
)

// ── Backend status ───────────────────────────────────────────────────────────
enum class BackendStatus { Unknown, Loading, Live, Offline }

// ── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class RegionsViewModel @Inject constructor(
    private val session: SessionStore,
    private val catalog: CatalogRepository,
    private val matchingRepo: MatchingRepository
) : ViewModel() {

    val currentFilter = session.filter.stateIn(viewModelScope, SharingStarted.Eagerly, MatchFilter())

    private val _presets = MutableStateFlow<List<RegionPresetDto>>(FALLBACK_PRESETS)
    val presets = _presets.asStateFlow()

    val backendStatus = MutableStateFlow(BackendStatus.Unknown)

    /** Currently selected preset (null = browse mode) */
    private val _selectedPreset = MutableStateFlow<RegionPresetDto?>(null)
    val selectedPreset = _selectedPreset.asStateFlow()

    /** Inline match results for the selected preset */
    private val _presetResults = MutableStateFlow<List<MatchResult>>(emptyList())
    val presetResults = _presetResults.asStateFlow()

    private val _loadingResults = MutableStateFlow(false)
    val loadingResults = _loadingResults.asStateFlow()

    /** Quick filter inline results */
    private val _quickResults = MutableStateFlow<List<MatchResult>>(emptyList())
    val quickResults = _quickResults.asStateFlow()

    private val _quickLabel = MutableStateFlow("")
    val quickLabel = _quickLabel.asStateFlow()

    private val _loadingQuick = MutableStateFlow(false)
    val loadingQuick = _loadingQuick.asStateFlow()

    init { refreshFromBackend() }

    fun refreshFromBackend() = viewModelScope.launch {
        backendStatus.value = BackendStatus.Loading
        catalog.fetchPresets()
            .onSuccess { list ->
                _presets.value = list.ifEmpty { FALLBACK_PRESETS }
                backendStatus.value = if (list.isNotEmpty()) BackendStatus.Live else BackendStatus.Offline
            }
            .onFailure {
                _presets.value = FALLBACK_PRESETS
                backendStatus.value = BackendStatus.Offline
            }
    }

    /** Tap a backend preset → show its matches inline */
    fun selectPreset(p: RegionPresetDto) = viewModelScope.launch {
        _selectedPreset.value = p
        _quickLabel.value = ""
        _quickResults.value = emptyList()
        loadPresetResults(p)
    }

    fun clearSelection() {
        _selectedPreset.value = null
        _presetResults.value = emptyList()
    }

    /** Tap a quick chip → show its matches inline */
    fun selectQuick(label: String, religion: String = "", city: String = "",
                    motherTongue: String = "", caste: String = "", state: String = "") = viewModelScope.launch {
        _selectedPreset.value = null
        _presetResults.value = emptyList()
        _quickLabel.value = label
        loadQuickResults(religion, city, motherTongue, caste, state)
    }

    fun clearQuick() {
        _quickLabel.value = ""
        _quickResults.value = emptyList()
    }

    /** Also apply the preset as a global filter across the whole app */
    fun applyAsGlobalFilter(p: RegionPresetDto) = viewModelScope.launch {
        val existing = currentFilter.value
        val f = p.filters
        session.setFilter(existing.copy(
            city         = f.city?.takeIf { it.isNotBlank() } ?: existing.city,
            state        = f.state?.takeIf { it.isNotBlank() } ?: existing.state,
            caste        = f.caste?.takeIf { it.isNotBlank() } ?: existing.caste,
            religion     = f.religion?.takeIf { it.isNotBlank() } ?: existing.religion,
            motherTongue = f.motherTongue?.takeIf { it.isNotBlank() } ?: existing.motherTongue
        ))
    }

    fun clearAll() = viewModelScope.launch { session.setFilter(MatchFilter()) }

    fun clearSingle(field: String) = viewModelScope.launch {
        val cur = currentFilter.value
        session.setFilter(when (field) {
            "religion"     -> cur.copy(religion = "")
            "city"         -> cur.copy(city = "")
            "state"        -> cur.copy(state = "")
            "caste"        -> cur.copy(caste = "")
            "motherTongue" -> cur.copy(motherTongue = "")
            else           -> cur
        })
    }

    private suspend fun loadPresetResults(p: RegionPresetDto) {
        _loadingResults.value = true
        val uid = session.userId.first() ?: run { _loadingResults.value = false; return }
        val f = p.filters
        val filter = MatchFilter(
            city = f.city ?: "", state = f.state ?: "",
            caste = f.caste ?: "", religion = f.religion ?: "",
            motherTongue = f.motherTongue ?: ""
        )
        _presetResults.value = matchingRepo.recommendations(uid, MatchMode.ADVANCED, filter)
        _loadingResults.value = false
    }

    private suspend fun loadQuickResults(religion: String, city: String,
                                         motherTongue: String, caste: String, state: String) {
        _loadingQuick.value = true
        val uid = session.userId.first() ?: run { _loadingQuick.value = false; return }
        val filter = MatchFilter(
            religion = religion, city = city, motherTongue = motherTongue,
            caste = caste, state = state
        )
        _quickResults.value = matchingRepo.recommendations(uid, MatchMode.ADVANCED, filter)
        _loadingQuick.value = false
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionsScreen(
    onBack: () -> Unit = {},
    onGoMatches: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: RegionsViewModel = hiltViewModel()
) {
    val filter         by vm.currentFilter.collectAsState()
    val presets        by vm.presets.collectAsState()
    val backendStatus  by vm.backendStatus.collectAsState()
    val selectedPreset by vm.selectedPreset.collectAsState()
    val presetResults  by vm.presetResults.collectAsState()
    val loadingResults by vm.loadingResults.collectAsState()
    val quickResults   by vm.quickResults.collectAsState()
    val quickLabel     by vm.quickLabel.collectAsState()
    val loadingQuick   by vm.loadingQuick.collectAsState()

    val activeChips = buildList {
        if (filter.religion.isNotBlank())     add("Religion: ${filter.religion}" to "religion")
        if (filter.caste.isNotBlank())         add("Caste: ${filter.caste}" to "caste")
        if (filter.motherTongue.isNotBlank()) add("Language: ${filter.motherTongue}" to "motherTongue")
        if (filter.city.isNotBlank())         add("City: ${filter.city}" to "city")
        if (filter.state.isNotBlank())        add("State: ${filter.state}" to "state")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        selectedPreset != null ->
                            Text(selectedPreset!!.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        quickLabel.isNotBlank() ->
                            Text("$quickLabel Matches", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        else ->
                            Text(t("regions_community", "Regions & Community"))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            selectedPreset != null -> vm.clearSelection()
                            quickLabel.isNotBlank() -> vm.clearQuick()
                            else -> onBack()
                        }
                    }, modifier = Modifier.testTag("regions_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (selectedPreset == null && quickLabel.isBlank()) {
                        BackendPill(backendStatus, onRefresh = vm::refreshFromBackend)
                    }
                    if (selectedPreset != null) {
                        TextButton(onClick = {
                            vm.applyAsGlobalFilter(selectedPreset!!)
                            onGoMatches()
                        }) {
                            Icon(Icons.Filled.FilterAlt, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(t("apply_globally", "Apply globally"))
                        }
                    }
                    if (activeChips.isNotEmpty() && selectedPreset == null && quickLabel.isBlank()) {
                        TextButton(onClick = vm::clearAll, modifier = Modifier.testTag("regions_clear_all")) {
                            Icon(Icons.Filled.FilterAltOff, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }
            )
        }
    ) { pad ->
        when {
            selectedPreset != null -> PresetDetailView(
                preset = selectedPreset!!,
                results = presetResults,
                loading = loadingResults,
                onOpenProfile = onOpenProfile,
                onApplyGlobal = { vm.applyAsGlobalFilter(selectedPreset!!); onGoMatches() },
                modifier = Modifier.padding(pad)
            )
            quickLabel.isNotBlank() -> QuickResultsView(
                label = quickLabel,
                results = quickResults,
                loading = loadingQuick,
                onOpenProfile = onOpenProfile,
                modifier = Modifier.padding(pad)
            )
            else -> BrowseView(
                presets = presets,
                backendStatus = backendStatus,
                activeChips = activeChips,
                onSelectPreset = vm::selectPreset,
                onSelectQuick = { q -> vm.selectQuick(q.label, q.religion, q.city, q.motherTongue, q.caste, q.state) },
                onClearChip = vm::clearSingle,
                onClearAll = vm::clearAll,
                onGoMatches = onGoMatches,
                modifier = Modifier.padding(pad)
            )
        }
    }
}

// ── Browse View ──────────────────────────────────────────────────────────────
@Composable
private fun BrowseView(
    presets: List<RegionPresetDto>,
    backendStatus: BackendStatus,
    activeChips: List<Pair<String, String>>,
    onSelectPreset: (RegionPresetDto) -> Unit,
    onSelectQuick: (QuickPreset) -> Unit,
    onClearChip: (String) -> Unit,
    onClearAll: () -> Unit,
    onGoMatches: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier.fillMaxSize().testTag("regions_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (activeChips.isNotEmpty()) {
            item { ActiveFilterBanner(activeChips, onClearChip, onClearAll, onGoMatches) }
        }

        // ── Quick chip rows FIRST ──────────────────────────────────────
        item { SectionHeader(icon = Icons.Filled.Groups, title = "Browse by caste",
            subtitle = "Tap to view caste-based matches", count = CASTE_PRESETS.size) }
        item { QuickChipRow(CASTE_PRESETS, onSelectQuick) }

        item { SectionHeader(icon = Icons.Filled.TempleHindu, title = "Browse by religion",
            subtitle = "Tap to view religion-based matches", count = RELIGION_PRESETS.size) }
        item { QuickChipRow(RELIGION_PRESETS, onSelectQuick) }

        item { SectionHeader(icon = Icons.Filled.Translate, title = "Browse by language",
            subtitle = "Tap to view language-based matches", count = LANGUAGE_PRESETS.size) }
        item { QuickChipRow(LANGUAGE_PRESETS, onSelectQuick) }

        item { SectionHeader(icon = Icons.Filled.LocationCity, title = "Browse by city",
            subtitle = "Tap to view city-based matches", count = CITY_PRESETS.size) }
        item { QuickChipRow(CITY_PRESETS, onSelectQuick) }

        item { SectionHeader(icon = Icons.Filled.Map, title = "Browse by state",
            subtitle = "Tap to view state-based matches", count = STATE_PRESETS.size) }
        item { QuickChipRow(STATE_PRESETS, onSelectQuick) }

        item { SectionHeader(icon = Icons.Filled.Flight, title = "NRI Matches",
            subtitle = "Profiles in popular NRI destinations", count = NRI_PRESETS.size) }
        item { QuickChipRow(NRI_PRESETS, onSelectQuick) }

        // ── Community presets BELOW ────────────────────────────────────
        item {
            SectionHeader(
                icon = if (backendStatus == BackendStatus.Live) Icons.Filled.CloudDone else Icons.Filled.Groups,
                title = "Community presets",
                subtitle = "Tap a preset to see matching profiles",
                count = presets.size
            )
        }
        items(presets, key = { it.slug ?: it.title }) { preset ->
            BackendPresetCard(preset) { onSelectPreset(preset) }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Preset Detail View ───────────────────────────────────────────────────────
@Composable
private fun PresetDetailView(
    preset: RegionPresetDto,
    results: List<MatchResult>,
    loading: Boolean,
    onOpenProfile: (Long) -> Unit,
    onApplyGlobal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val f = preset.filters
    val filterParts = buildList {
        f.religion?.takeIf { it.isNotBlank() }?.let { add("Religion: $it") }
        f.caste?.takeIf { it.isNotBlank() }?.let { add("Caste: $it") }
        f.motherTongue?.takeIf { it.isNotBlank() }?.let { add("Language: $it") }
        f.city?.takeIf { it.isNotBlank() }?.let { add("City: $it") }
        f.state?.takeIf { it.isNotBlank() }?.let { add("State: $it") }
        f.country?.takeIf { it.isNotBlank() }?.let { add("Country: $it") }
        f.occupationCategory?.takeIf { it.isNotBlank() }?.let { add("Occupation: $it") }
    }

    LazyColumn(
        modifier.fillMaxSize().testTag("regions_detail"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(preset.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    preset.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (filterParts.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filterParts) { part ->
                                SuggestionChip(onClick = {}, label = { Text(part, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                    if (preset.tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            preset.tags.take(4).forEach { tag ->
                                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(tag, style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
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

        item {
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Loading matches...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("${results.size} match${if (results.size != 1) "es" else ""} found",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }

        if (!loading && results.isEmpty()) {
            item { EmptyResultCard("No matches found for this preset", "Try a different region or community filter") }
        }

        items(results, key = { it.user.id }) { r -> InlineMatchCard(r, onOpenProfile) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Quick Results View ───────────────────────────────────────────────────────
@Composable
private fun QuickResultsView(
    label: String, results: List<MatchResult>, loading: Boolean,
    onOpenProfile: (Long) -> Unit, modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier.fillMaxSize().testTag("regions_quick_results"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Loading $label matches...", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("${results.size} match${if (results.size != 1) "es" else ""} for $label",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }

        if (!loading && results.isEmpty()) {
            item { EmptyResultCard("No matches found for $label", "Try a different filter category") }
        }

        items(results, key = { it.user.id }) { r -> InlineMatchCard(r, onOpenProfile) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Inline Match Card ────────────────────────────────────────────────────────
@Composable
private fun InlineMatchCard(r: MatchResult, onOpen: (Long) -> Unit) {
    val p = r.user
    ElevatedCard(
        onClick = { onOpen(p.id) }, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("region_match_${p.id}")
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(50.dp)) {
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
                    if (p.isPremium)  Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = Color(0xFFFFB300))
                }
                Text("${p.city} • ${p.profession}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val attrs = buildList {
                    if (p.religion.isNotBlank()) add(p.religion)
                    if (p.caste.isNotBlank()) add(p.caste)
                    if (p.motherTongue.isNotBlank()) add(p.motherTongue)
                    if (p.state.isNotBlank()) add(p.state)
                }
                if (attrs.isNotEmpty()) {
                    Text(attrs.joinToString(" • "), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            ScorePill(r.displayScore)
        }
    }
}

@Composable
private fun ScorePill(score: Int) {
    val bg = when { score >= 90 -> Color(0xFF2E7D32); score >= 70 -> Color(0xFF1565C0)
        else -> MaterialTheme.colorScheme.surfaceVariant }
    val fg = when { score >= 90 -> Color(0xFF2E7D32); score >= 70 -> Color(0xFF1565C0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant }
    Surface(shape = RoundedCornerShape(20.dp), color = bg.copy(alpha = 0.15f)) {
        Text("$score%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
            color = fg, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun EmptyResultCard(title: String, subtitle: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.SearchOff, null, Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Composables ──────────────────────────────────────────────────────────────
@Composable
private fun BackendPresetCard(preset: RegionPresetDto, onClick: () -> Unit) {
    val f = preset.filters
    val filterParts = buildList {
        f.religion?.takeIf { it.isNotBlank() }?.let { add(it) }
        f.caste?.takeIf { it.isNotBlank() }?.let { add(it) }
        f.motherTongue?.takeIf { it.isNotBlank() }?.let { add(it) }
        f.city?.takeIf { it.isNotBlank() }?.let { add(it) }
        f.state?.takeIf { it.isNotBlank() }?.let { add(it) }
        f.country?.takeIf { it.isNotBlank() }?.let { add(it) }
        f.occupationCategory?.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("region_preset_${preset.slug}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(preset.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    preset.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Filled.ChevronRight, null, Modifier.size(20.dp).padding(2.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (filterParts.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filterParts) { part ->
                        SuggestionChip(onClick = onClick, label = { Text(part, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
            if (preset.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    preset.tags.take(4).forEach { tag ->
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(tag, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterBanner(
    chips: List<Pair<String, String>>, onClearChip: (String) -> Unit, onClearAll: () -> Unit, onBrowse: () -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FilterAlt, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("Active global filter", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(chips) { (label, field) ->
                    InputChip(selected = true, onClick = { onClearChip(field) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(14.dp).clickable { onClearChip(field) }) },
                        modifier = Modifier.testTag("active_chip_$field"))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClearAll, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.FilterAltOff, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text(t("clear_all", "Clear all"))
                }
                Button(onClick = onBrowse, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Favorite, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Browse matches")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, subtitle: String = "", count: Int = 0) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (count > 0) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text("$count", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickChipRow(presets: List<QuickPreset>, onSelect: (QuickPreset) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
        items(presets) { p ->
            ElevatedCard(shape = RoundedCornerShape(14.dp),
                modifier = Modifier.width(90.dp).clip(RoundedCornerShape(14.dp)).clickable { onSelect(p) }
                    .testTag("quick_preset_${p.label.lowercase()}")) {
                Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(50), color = p.accentColor.copy(alpha = 0.13f), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(p.icon, null, Modifier.size(20.dp), tint = p.accentColor) }
                    }
                    Text(p.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun BackendPill(status: BackendStatus, onRefresh: () -> Unit) {
    val (label, tint, icon) = when (status) {
        BackendStatus.Live    -> Triple("Live",   Color(0xFF2E7D32), Icons.Filled.CloudDone)
        BackendStatus.Loading -> Triple("Sync",   MaterialTheme.colorScheme.primary, Icons.Filled.Sync)
        BackendStatus.Offline -> Triple("Offline",Color(0xFFB71C1C), Icons.Filled.CloudOff)
        BackendStatus.Unknown -> Triple("Idle",   MaterialTheme.colorScheme.onSurfaceVariant, Icons.Filled.Cloud)
    }
    AssistChip(onClick = onRefresh, label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = { Icon(icon, null, Modifier.size(14.dp), tint = tint) },
        modifier = Modifier.testTag("regions_backend_pill"))
}
