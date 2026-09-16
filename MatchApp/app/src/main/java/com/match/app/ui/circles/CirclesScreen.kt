package com.match.app.ui.circles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val FALLBACK_CIRCLES = listOf(
    CommunityGroupDto("local_telugu", "Telugu", "Filter by Telugu mother tongue", "Language", null, motherTongue = "Telugu"),
    CommunityGroupDto("local_tamil", "Tamil", "Filter by Tamil mother tongue", "Language", null, motherTongue = "Tamil"),
    CommunityGroupDto("local_hindi", "Hindi", "Filter by Hindi mother tongue", "Language", null, motherTongue = "Hindi"),
    CommunityGroupDto("local_hindu", "Hindu", "Filter by Hindu religion", "Religion", null, religion = "Hindu"),
    CommunityGroupDto("local_muslim", "Muslim", "Filter by Muslim religion", "Religion", null, religion = "Muslim"),
    CommunityGroupDto("local_christian", "Christian", "Filter by Christian religion", "Religion", null, religion = "Christian"),
    CommunityGroupDto("local_sikh", "Sikh", "Filter by Sikh religion", "Religion", null, religion = "Sikh"),
    CommunityGroupDto("local_buddhist", "Buddhist", "Filter by Buddhist religion", "Religion", null, religion = "Buddhist"),
    CommunityGroupDto("local_jain", "Jain", "Filter by Jain religion", "Religion", null, religion = "Jain"),
    CommunityGroupDto("local_parsi", "Parsi / Zoroastrian", "Filter by Parsi / Zoroastrian religion", "Religion", null, religion = "Parsi / Zoroastrian"),
    CommunityGroupDto("local_hyd", "Hyderabad", "Filter by Hyderabad city", "City", null, city = "Hyderabad"),
    CommunityGroupDto("local_mumbai", "Mumbai", "Filter by Mumbai city", "City", null, city = "Mumbai"),
    CommunityGroupDto("local_bengaluru", "Bengaluru", "Filter by Bengaluru city", "City", null, city = "Bengaluru"),
    CommunityGroupDto("local_chennai", "Chennai", "Filter by Chennai city", "City", null, city = "Chennai"),
    CommunityGroupDto("local_delhi", "Delhi", "Filter by Delhi city", "City", null, city = "Delhi"),
    CommunityGroupDto("local_verified", "Verified profiles", "Show profiles with platform verification", "Trust", null)
)

@HiltViewModel
class CirclesViewModel @Inject constructor(
    private val session: SessionStore,
    private val catalog: CatalogRepository,
    private val matchingRepo: MatchingRepository
) : ViewModel() {
    val currentFilter = session.filter.stateIn(viewModelScope, SharingStarted.Eagerly, MatchFilter())

    private val _circles = MutableStateFlow(FALLBACK_CIRCLES)
    val circles = _circles.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _liveCatalog = MutableStateFlow(false)
    val liveCatalog = _liveCatalog.asStateFlow()
    private val _selectedCircle = MutableStateFlow<CommunityGroupDto?>(null)
    val selectedCircle = _selectedCircle.asStateFlow()
    private val _results = MutableStateFlow<List<MatchResult>>(emptyList())
    val results = _results.asStateFlow()
    private val _loadingResults = MutableStateFlow(false)
    val loadingResults = _loadingResults.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init { refreshCatalog() }

    fun refreshCatalog() = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        catalog.fetchCommunityGroups()
            .onSuccess { list ->
                if (list.isNotEmpty()) {
                    _circles.value = list
                    _liveCatalog.value = true
                } else {
                    _circles.value = FALLBACK_CIRCLES
                    _liveCatalog.value = false
                }
            }
            .onFailure {
                _circles.value = FALLBACK_CIRCLES
                _liveCatalog.value = false
            }
        _loading.value = false
    }

    fun selectCircle(circle: CommunityGroupDto) = viewModelScope.launch {
        _selectedCircle.value = circle
        _loadingResults.value = true
        _error.value = null
        runCatching {
            val uid = session.userId.first() ?: error("You are not signed in.")
            matchingRepo.recommendations(uid, MatchMode.ADVANCED, circleFilter(circle))
        }.onSuccess { _results.value = it }
            .onFailure {
                _results.value = emptyList()
                _error.value = it.message?.take(180) ?: "Could not load matches for this filter."
            }
        _loadingResults.value = false
    }

    fun clearSelection() {
        _selectedCircle.value = null
        _results.value = emptyList()
        _error.value = null
    }

    fun applyAsGlobalFilter(circle: CommunityGroupDto, onApplied: () -> Unit) = viewModelScope.launch {
        val existing = currentFilter.value
        val scoped = circleFilter(circle)
        session.setFilter(
            existing.copy(
                religion = scoped.religion,
                caste = scoped.caste,
                city = scoped.city,
                motherTongue = scoped.motherTongue,
                verifiedOnly = scoped.verifiedOnly
            )
        )
        onApplied()
    }

    fun clearError() { _error.value = null }

    private fun circleFilter(circle: CommunityGroupDto) = MatchFilter(
        religion = circle.religion.orEmpty(),
        caste = circle.caste.orEmpty(),
        city = circle.city.orEmpty(),
        motherTongue = circle.motherTongue.orEmpty(),
        verifiedOnly = circle.category.equals("Trust", ignoreCase = true) &&
            circle.name.contains("Verified", ignoreCase = true)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirclesScreen(
    onBack: () -> Unit = {},
    onGoMatches: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: CirclesViewModel = hiltViewModel()
) {
    val circles by vm.circles.collectAsState()
    val loading by vm.loading.collectAsState()
    val liveCatalog by vm.liveCatalog.collectAsState()
    val selected by vm.selectedCircle.collectAsState()
    val results by vm.results.collectAsState()
    val loadingResults by vm.loadingResults.collectAsState()
    val error by vm.error.collectAsState()
    var category by remember { mutableStateOf("All") }
    val snackbar = remember { SnackbarHostState() }

    val categories = remember(circles) { listOf("All") + circles.map { it.category ?: "Other" }.distinct() }
    val visible = remember(circles, category) {
        if (category == "All") circles else circles.filter { (it.category ?: "Other") == category }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        selected?.name ?: "Discovery Circles",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { if (selected != null) vm.clearSelection() else onBack() },
                        modifier = Modifier.testTag("circles_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selected == null) {
                        IconButton(onClick = vm::refreshCatalog, enabled = !loading) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh circles")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (selected != null) {
            CircleResults(
                modifier = Modifier.padding(padding),
                circle = checkNotNull(selected),
                results = results,
                loading = loadingResults,
                onApply = { vm.applyAsGlobalFilter(checkNotNull(selected), onGoMatches) },
                onOpenProfile = onOpenProfile
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).testTag("circles_screen"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.FilterAlt, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Reusable discovery shortcuts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (liveCatalog) "These categories came from Matree's current catalog. Member counts are not displayed unless a reviewed live metric is available."
                                    else "Using built-in filter shortcuts because no live circle catalog is available. These are not membership statistics.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { item ->
                            FilterChip(
                                selected = category == item,
                                onClick = { category = item },
                                label = { Text(item) }
                            )
                        }
                    }
                }

                if (loading) {
                    item {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }

                items(visible, key = { it.id }) { circle ->
                    ElevatedCard(
                        onClick = { vm.selectCircle(circle) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(iconForCategory(circle.category), null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(circle.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                circle.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Text(
                                    circle.category ?: "Filter",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Open ${circle.name}")
                        }
                    }
                }

                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun CircleResults(
    modifier: Modifier,
    circle: CommunityGroupDto,
    results: List<MatchResult>,
    loading: Boolean,
    onApply: () -> Unit,
    onOpenProfile: (Long) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("circle_results"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ElevatedCard(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(circle.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        circle.description?.takeIf { it.isNotBlank() } ?: "Preview profiles matching this shortcut.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Button(onClick = onApply, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.FilterAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Use this filter in Matches")
                    }
                }
            }
        }

        if (loading) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        } else if (results.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No visible matches for this shortcut", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Try another circle or broaden your filters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                Text(
                    "${results.size} currently loaded match${if (results.size == 1) "" else "es"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(results, key = { it.user.id }) { result ->
                ElevatedCard(
                    onClick = { onOpenProfile(result.user.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(result.user.displayName.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(result.user.displayName, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(result.user.age.toString(), result.user.city, result.user.profession).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (result.displayScore > 0) {
                            Text("${result.displayScore}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun iconForCategory(category: String?): ImageVector = when (category?.lowercase()) {
    "language" -> Icons.Filled.Translate
    "religion" -> Icons.Filled.Diversity3
    "caste", "community" -> Icons.Filled.Groups
    "city", "region" -> Icons.Filled.LocationCity
    "trust" -> Icons.Filled.Verified
    else -> Icons.Filled.FilterAlt
}
