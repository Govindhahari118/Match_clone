package com.match.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.match.app.data.local.entity.SavedSearchEntity
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.SavedSearchRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.AppPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val session: SessionStore,
    private val savedSearchRepo: SavedSearchRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    val filter: StateFlow<MatchFilter> = session.filter.stateIn(viewModelScope, SharingStarted.Eagerly, MatchFilter())
    val darkMode: StateFlow<Boolean> = session.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val paletteKey: StateFlow<String> = session.paletteKey.stateIn(viewModelScope, SharingStarted.Eagerly, "ROSE")
    val apiBaseUrl: StateFlow<String> = session.apiBaseUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "http://10.0.2.2:8000")
    val biometricLock: StateFlow<Boolean> = session.biometricLock.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val planKey: StateFlow<String> = session.subscriptionPlan.stateIn(viewModelScope, SharingStarted.Eagerly, "FREE")

    val savedSearches: StateFlow<List<SavedSearchEntity>> = session.userId
        .filterNotNull()
        .flatMapLatest { savedSearchRepo.observeForUser(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiLanguage: StateFlow<String> = session.uiLanguage.stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun saveFilter(f: MatchFilter) = viewModelScope.launch { session.setFilter(f) }
    fun setDarkMode(v: Boolean) = viewModelScope.launch { session.setDarkMode(v) }
    fun setPalette(p: AppPalette) = viewModelScope.launch { session.setPalette(p.name) }
    fun setApiBase(url: String) = viewModelScope.launch { session.setApiBaseUrl(url) }
    fun setBiometricLock(v: Boolean) = viewModelScope.launch { session.setBiometricLock(v) }

    fun saveSearch(name: String, filter: MatchFilter) = viewModelScope.launch {
        val userId = session.userId.first() ?: return@launch
        savedSearchRepo.save(userId, name, filter)
    }

    fun deleteSearch(id: Long) = viewModelScope.launch {
        savedSearchRepo.delete(id)
    }

    fun applySearch(entity: SavedSearchEntity) = viewModelScope.launch {
        session.setFilter(savedSearchRepo.toFilter(entity))
    }

    sealed class AccountState {
        data object Idle : AccountState()
        data object Deleting : AccountState()
        data object Deleted : AccountState()
        data class Error(val msg: String) : AccountState()
    }

    private val _accountState = kotlinx.coroutines.flow.MutableStateFlow<AccountState>(AccountState.Idle)
    val accountState: StateFlow<AccountState> = _accountState

    fun deleteAccount() = viewModelScope.launch {
        _accountState.value = AccountState.Deleting
        val userId = session.userId.first()
        if (userId == null) {
            _accountState.value = AccountState.Error("Not signed in")
            return@launch
        }
        val result = authRepo.deleteAccount(userId)
        _accountState.value = when (result) {
            is com.match.app.data.repo.AuthResult.Success -> AccountState.Deleted
            is com.match.app.data.repo.AuthResult.Error -> AccountState.Error(result.message)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onGoLanguage: () -> Unit = {}, onUpgrade: () -> Unit = {}, onAccountDeleted: () -> Unit = {}, vm: SettingsViewModel = hiltViewModel()) {
    val filter by vm.filter.collectAsState()
    val darkMode by vm.darkMode.collectAsState()
    val paletteKey by vm.paletteKey.collectAsState()
    val apiBaseUrl by vm.apiBaseUrl.collectAsState()
    val savedSearches by vm.savedSearches.collectAsState(initial = emptyList())
    val biometricLock by vm.biometricLock.collectAsState()
    val uiLanguage by vm.uiLanguage.collectAsState()
    val planKey by vm.planKey.collectAsState()
    val hasAdvancedFilters = com.match.app.domain.subscription.SubscriptionPlans.canAccess(
        planKey, com.match.app.domain.subscription.SubscriptionPlans.Feature.ADVANCED_FILTERS)
    val accountState by vm.accountState.collectAsState()

    // Navigate away once account is deleted
    LaunchedEffect(accountState) {
        if (accountState is SettingsViewModel.AccountState.Deleted) onAccountDeleted()
    }

    var ageMin by remember(filter) { mutableStateOf(filter.ageMin.toFloat()) }
    var ageMax by remember(filter) { mutableStateOf(filter.ageMax.toFloat()) }
    var city by remember(filter) { mutableStateOf(filter.city) }
    var state by remember(filter) { mutableStateOf(filter.state) }
    var caste by remember(filter) { mutableStateOf(filter.caste) }
    var religion by remember(filter) { mutableStateOf(filter.religion) }
    var motherTongue by remember(filter) { mutableStateOf(filter.motherTongue) }
    var maritalStatus by remember(filter) { mutableStateOf(filter.maritalStatus) }
    var verifiedOnly by remember(filter) { mutableStateOf(filter.verifiedOnly) }
    var diet by remember(filter) { mutableStateOf(filter.diet) }
    var educationLevel by remember(filter) { mutableStateOf(filter.educationLevel) }
    var residentialStatus by remember(filter) { mutableStateOf(filter.residentialStatus) }
    var gothra by remember(filter) { mutableStateOf(filter.gothra) }
    var hasChildren by remember(filter) { mutableStateOf(filter.hasChildren) }
    var nativeState by remember(filter) { mutableStateOf(filter.nativeState) }
    var countryOfResidence by remember(filter) { mutableStateOf(filter.countryOfResidence) }
    var nriOnly by remember(filter) { mutableStateOf(filter.nriOnly) }
    var willingToRelocate by remember(filter) { mutableStateOf(filter.willingToRelocate) }
    var recentlyJoinedDays by remember(filter) { mutableStateOf(filter.recentlyJoinedDays) }
    // Sprint-10 extended filters
    var smoking by remember(filter) { mutableStateOf(filter.smoking) }
    var drinking by remember(filter) { mutableStateOf(filter.drinking) }
    var familyType by remember(filter) { mutableStateOf(filter.familyType) }
    var familyStatus by remember(filter) { mutableStateOf(filter.familyStatus) }
    var physicalStatus by remember(filter) { mutableStateOf(filter.physicalStatus) }
    var educationField by remember(filter) { mutableStateOf(filter.educationField) }
    var occupationCategory by remember(filter) { mutableStateOf(filter.occupationCategory) }
    var manglik by remember(filter) { mutableStateOf(filter.manglik) }
    var rasi by remember(filter) { mutableStateOf(filter.rasi) }
    var withPhotoOnly by remember(filter) { mutableStateOf(filter.withPhotoOnly) }
    var premiumOnly by remember(filter) { mutableStateOf(filter.premiumOnly) }
    var lastActiveWithinDays by remember(filter) { mutableStateOf(filter.lastActiveWithinDays) }
    var minPoruthamScore by remember(filter) { mutableStateOf(filter.minPoruthamScore) }
    var dirty by remember { mutableStateOf(false) }

    fun markDirty() { dirty = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("settings", "Settings")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // â”€â”€ Account details â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text(t("account", "Account"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Customize your account details and save changes instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            var firstName by remember { mutableStateOf("") }
            var lastName  by remember { mutableStateOf("") }
            var phone     by remember { mutableStateOf("") }
            var email     by remember { mutableStateOf("") }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it; markDirty() },
                    label = { Text("First name") }, singleLine = true,
                    modifier = Modifier.weight(1f).testTag("settings_first_name"))
                OutlinedTextField(value = lastName, onValueChange = { lastName = it; markDirty() },
                    label = { Text("Last name") }, singleLine = true,
                    modifier = Modifier.weight(1f).testTag("settings_last_name"))
            }
            OutlinedTextField(value = phone, onValueChange = { phone = it; markDirty() },
                label = { Text("Phone") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_phone"))
            OutlinedTextField(value = email, onValueChange = { email = it; markDirty() },
                label = { Text("Email") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_email"))

            HorizontalDivider()

            // â”€â”€ Appearance â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text(t("appearance", "Appearance"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (darkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode, null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("dark_mode", "Dark mode"), style = MaterialTheme.typography.bodyMedium)
                    Text(if (darkMode) "On" else "Off", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = darkMode,
                    onCheckedChange = { vm.setDarkMode(it) },
                    modifier = Modifier.testTag("settings_dark_mode")
                )
            }

            // â”€â”€ Theme palette â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text(t("theme_palette", "Theme palette"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("Switch the brand palette across the entire app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().testTag("settings_palette_row"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppPalette.entries.forEach { p ->
                    val selected = p.name == paletteKey
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_palette_${p.name.lowercase()}")
                    ) {
                        androidx.compose.material3.Surface(
                            onClick = { vm.setPalette(p) },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = p.swatch,
                            border = if (selected)
                                androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                            else null,
                            modifier = Modifier.size(40.dp)
                        ) { Box(Modifier) {} }
                        Spacer(Modifier.height(4.dp))
                        Text(p.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider()

            if (com.match.app.BuildConfig.DEBUG) {
            // â”€â”€ Backend connection â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text("Backend connection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Point the app at your local API. Use 10.0.2.2 from the Android emulator to reach your host's localhost.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            var apiUrlField by remember(apiBaseUrl) { mutableStateOf(apiBaseUrl) }
            OutlinedTextField(
                value = apiUrlField,
                onValueChange = { apiUrlField = it },
                singleLine = true,
                label = { Text("API base URL") },
                modifier = Modifier.fillMaxWidth().testTag("settings_api_base")
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { apiUrlField = "http://10.0.2.2:4000" },
                    modifier = Modifier.weight(1f)
                ) { Text("Emulator (10.0.2.2:4000)") }
                Button(
                    onClick = { vm.setApiBase(apiUrlField.trim()) },
                    enabled = apiUrlField.isNotBlank() && apiUrlField != apiBaseUrl,
                    modifier = Modifier.weight(1f).testTag("settings_api_save")
                ) { Text("Save URL") }
            }

            HorizontalDivider()
            } // end DEBUG

            // â”€â”€ Match filters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text(t("match_filters", "Match filters"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Text("Age range: ${ageMin.toInt()} â€“ ${ageMax.toInt()}", style = MaterialTheme.typography.bodyMedium)
            RangeSlider(
                value = ageMin..ageMax,
                onValueChange = { ageMin = it.start; ageMax = it.endInclusive; markDirty() },
                valueRange = 18f..80f,
                modifier = Modifier.fillMaxWidth().testTag("settings_age_range")
            )

            OutlinedTextField(
                value = city, onValueChange = { city = it; markDirty() },
                label = { Text("City (leave blank for all)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_city")
            )
            OutlinedTextField(
                value = state, onValueChange = { state = it; markDirty() },
                label = { Text("State (leave blank for all)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_state")
            )
            DropdownField(
                label = "Religion",
                value = religion,
                options = listOf("", "Hindu", "Muslim", "Christian", "Sikh", "Jain", "Buddhist", "Parsi", "Jewish", "Other"),
                onSelect = { religion = it; markDirty() },
                tag = "settings_religion"
            )
            OutlinedTextField(
                value = caste, onValueChange = { caste = it; markDirty() },
                label = { Text("Caste (leave blank for all)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_caste")
            )
            DropdownField(
                label = "Mother tongue",
                value = motherTongue,
                options = listOf("", "Tamil", "Telugu", "Kannada", "Malayalam", "Hindi", "Bengali", "Marathi", "Gujarati", "Punjabi", "Odia", "Urdu", "Other"),
                onSelect = { motherTongue = it; markDirty() },
                tag = "settings_tongue"
            )
            DropdownField(
                label = "Marital status",
                value = maritalStatus,
                options = listOf("", "Never Married", "Divorced", "Widowed", "Awaiting Divorce"),
                onSelect = { maritalStatus = it; markDirty() },
                tag = "settings_marital"
            )
            DropdownField(
                label = "Diet preference",
                value = diet,
                options = listOf("", "Veg", "Non-Veg", "Eggetarian", "Jain"),
                onSelect = { diet = it; markDirty() },
                tag = "settings_diet"
            )
            DropdownField(
                label = "Education level",
                value = educationLevel,
                options = listOf("", "Graduate", "Post-Graduate", "Doctorate", "Diploma", "Under Graduate"),
                onSelect = { educationLevel = it; markDirty() },
                tag = "settings_education"
            )
            DropdownField(
                label = "Residential status",
                value = residentialStatus,
                options = listOf("", "Citizen", "PR", "Work Permit", "Student"),
                onSelect = { residentialStatus = it; markDirty() },
                tag = "settings_residential"
            )
            OutlinedTextField(
                value = gothra, onValueChange = { gothra = it; markDirty() },
                label = { Text("Gothra (leave blank for all)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_gothra")
            )
            DropdownField(
                label = "Has children",
                value = hasChildren,
                options = listOf("", "Yes", "No", "Any"),
                onSelect = { hasChildren = it; markDirty() },
                tag = "settings_children"
            )
            OutlinedTextField(
                value = nativeState, onValueChange = { nativeState = it; markDirty() },
                label = { Text("Native State (origin)") },
                placeholder = { Text("e.g. Andhra Pradesh") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_native_state")
            )

            // ── NRI / International Filters ──────────────────────────────
            Spacer(Modifier.height(4.dp))
            Text("NRI / International", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = countryOfResidence, onValueChange = { countryOfResidence = it; markDirty() },
                label = { Text("Country of Residence") },
                placeholder = { Text("e.g. USA, UK, UAE") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings_country_of_residence")
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NRI Profiles Only", style = MaterialTheme.typography.bodyMedium)
                    Text("Show profiles based outside India", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = nriOnly, onCheckedChange = { nriOnly = it; markDirty() },
                    modifier = Modifier.testTag("settings_nri_only"))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Willing to Relocate", style = MaterialTheme.typography.bodyMedium)
                    Text("Only show profiles open to relocation", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = willingToRelocate, onCheckedChange = { willingToRelocate = it; markDirty() },
                    modifier = Modifier.testTag("settings_relocate"))
            }
            DropdownField(
                label = "Recently Joined",
                value = when (recentlyJoinedDays) { 7 -> "Last 7 days"; 30 -> "Last 30 days"; else -> "All time" },
                options = listOf("All time", "Last 7 days", "Last 30 days"),
                onSelect = { recentlyJoinedDays = when (it) { "Last 7 days" -> 7; "Last 30 days" -> 30; else -> 0 }; markDirty() },
                tag = "settings_recently_joined"
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("verified_only", "Verified profiles only"), style = MaterialTheme.typography.bodyMedium)
                    Text(t("verified_only_desc", "Show only verified users"), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = verifiedOnly,
                    onCheckedChange = { verifiedOnly = it; markDirty() },
                    modifier = Modifier.testTag("settings_verified_only")
                )
            }

            // ── Sprint-10: Lifestyle filters ─────────────────────────
            if (!hasAdvancedFilters) {
                Surface(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Advanced Filters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Unlock lifestyle, career, astrology & activity filters",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = onUpgrade) { Text("Upgrade") }
                    }
                }
            }
            if (hasAdvancedFilters) {
            Text("Lifestyle & Values", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            DropdownField(
                label = "Smoking",
                value = smoking.ifBlank { "Any" },
                options = listOf("Any", "Never", "Occasionally", "Regularly"),
                onSelect = { smoking = if (it == "Any") "" else it; markDirty() },
                tag = "settings_smoking"
            )
            DropdownField(
                label = "Drinking",
                value = drinking.ifBlank { "Any" },
                options = listOf("Any", "Never", "Occasionally", "Socially"),
                onSelect = { drinking = if (it == "Any") "" else it; markDirty() },
                tag = "settings_drinking"
            )
            DropdownField(
                label = "Family Type",
                value = familyType.ifBlank { "Any" },
                options = listOf("Any", "Joint", "Nuclear", "Either"),
                onSelect = { familyType = if (it == "Any") "" else it; markDirty() },
                tag = "settings_family_type"
            )
            DropdownField(
                label = "Family Status",
                value = familyStatus.ifBlank { "Any" },
                options = listOf("Any", "Middle Class", "Upper Middle Class", "Affluent", "Rich"),
                onSelect = { familyStatus = if (it == "Any") "" else it; markDirty() },
                tag = "settings_family_status"
            )
            DropdownField(
                label = "Physical Status",
                value = physicalStatus.ifBlank { "Any" },
                options = listOf("Any", "Normal", "Differently Abled"),
                onSelect = { physicalStatus = if (it == "Any") "" else it; markDirty() },
                tag = "settings_physical_status"
            )

            // ── Sprint-10: Career filters ─────────────────────────────
            Text("Education & Career", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            DropdownField(
                label = "Education Field",
                value = educationField.ifBlank { "Any" },
                options = listOf("Any", "Engineering", "Medicine", "Law", "Commerce", "Arts", "Science", "Management"),
                onSelect = { educationField = if (it == "Any") "" else it; markDirty() },
                tag = "settings_education_field"
            )
            DropdownField(
                label = "Occupation Category",
                value = occupationCategory.ifBlank { "Any" },
                options = listOf("Any", "Private Sector", "Government", "Business", "Self-employed", "Defence", "Not Working"),
                onSelect = { occupationCategory = if (it == "Any") "" else it; markDirty() },
                tag = "settings_occupation_category"
            )

            // ── Sprint-10: Astrology filters ─────────────────────────
            Text("Astrology Preferences", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            DropdownField(
                label = "Manglik",
                value = manglik.ifBlank { "Any" },
                options = listOf("Any", "Manglik", "Non-Manglik"),
                onSelect = { manglik = if (it == "Any") "" else it; markDirty() },
                tag = "settings_manglik"
            )
            DropdownField(
                label = "Rasi",
                value = rasi.ifBlank { "Any" },
                options = listOf("Any", "Mesha", "Vrishabha", "Mithuna", "Karka", "Simha", "Kanya",
                    "Tula", "Vrischika", "Dhanu", "Makara", "Kumbha", "Meena"),
                onSelect = { rasi = if (it == "Any") "" else it; markDirty() },
                tag = "settings_rasi"
            )
            Text("Min. Porutham Score: $minPoruthamScore/10",
                style = MaterialTheme.typography.bodySmall)
            Slider(
                value = minPoruthamScore.toFloat(),
                onValueChange = { minPoruthamScore = it.toInt(); markDirty() },
                valueRange = 0f..10f, steps = 9,
                modifier = Modifier.testTag("settings_porutham")
            )

            // ── Sprint-10: Activity & quality filters ─────────────────
            Text("Activity & Quality", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            DropdownField(
                label = "Last Active",
                value = when (lastActiveWithinDays) { 1 -> "Today"; 7 -> "This week"; 30 -> "This month"; else -> "Any time" },
                options = listOf("Any time", "Today", "This week", "This month"),
                onSelect = { lastActiveWithinDays = when (it) { "Today" -> 1; "This week" -> 7; "This month" -> 30; else -> 0 }; markDirty() },
                tag = "settings_last_active"
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("With Photo Only", style = MaterialTheme.typography.bodyMedium)
                    Text("Exclude profiles without a photo", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = withPhotoOnly, onCheckedChange = { withPhotoOnly = it; markDirty() },
                    modifier = Modifier.testTag("settings_with_photo"))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Premium Members Only", style = MaterialTheme.typography.bodyMedium)
                    Text("Only show paying members", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = premiumOnly, onCheckedChange = { premiumOnly = it; markDirty() },
                    modifier = Modifier.testTag("settings_premium_only"))
            }
            } // end if (hasAdvancedFilters)

            Button(
                enabled = dirty,
                onClick = {
                    vm.saveFilter(MatchFilter(
                        ageMin = ageMin.toInt(), ageMax = ageMax.toInt(),
                        city = city, state = state, caste = caste,
                        religion = religion, motherTongue = motherTongue,
                        maritalStatus = maritalStatus, verifiedOnly = verifiedOnly,
                        diet = diet, educationLevel = educationLevel,
                        residentialStatus = residentialStatus, gothra = gothra,
                        hasChildren = hasChildren,
                        nativeState = nativeState,
                        countryOfResidence = countryOfResidence,
                        nriOnly = nriOnly,
                        willingToRelocate = willingToRelocate,
                        recentlyJoinedDays = recentlyJoinedDays,
                        smoking = smoking, drinking = drinking,
                        familyType = familyType, familyStatus = familyStatus,
                        physicalStatus = physicalStatus,
                        educationField = educationField,
                        occupationCategory = occupationCategory,
                        manglik = manglik, rasi = rasi,
                        withPhotoOnly = withPhotoOnly, premiumOnly = premiumOnly,
                        lastActiveWithinDays = lastActiveWithinDays,
                        minPoruthamScore = minPoruthamScore
                    ))
                    dirty = false
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("settings_save")
) { Text(t("save_filters", "Save filters")) }

            // ── Save current filter as preset ──────────────────────────
            var showSaveDialog by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("settings_save_search")
            ) {
                Icon(Icons.Filled.Bookmark, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save current filters as a preset")
            }

            if (showSaveDialog) {
                var searchName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    title = { Text("Save Search Preset") },
                    text = {
                        OutlinedTextField(
                            value = searchName,
                            onValueChange = { searchName = it },
                            label = { Text("Preset name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.saveSearch(searchName, MatchFilter(
                                    ageMin = ageMin.toInt(), ageMax = ageMax.toInt(),
                                    city = city, state = state, caste = caste,
                                    religion = religion, motherTongue = motherTongue,
                                    maritalStatus = maritalStatus, verifiedOnly = verifiedOnly,
                                    diet = diet, educationLevel = educationLevel,
                                    residentialStatus = residentialStatus, gothra = gothra,
                                    hasChildren = hasChildren
                                ))
                                showSaveDialog = false
                            },
                            enabled = searchName.isNotBlank()
                        ) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
                    }
                )
            }

            // ── Saved search presets list ──────────────────────────────
            if (savedSearches.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Saved presets", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary)
                savedSearches.forEach { saved ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.BookmarkBorder, null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(saved.name, style = MaterialTheme.typography.bodyMedium)
                            Text("Age ${saved.minAge}–${saved.maxAge}" +
                                    if (saved.religions.isNotBlank()) " · ${saved.religions}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { vm.applySearch(saved) }) { Text("Apply") }
                        IconButton(onClick = { vm.deleteSearch(saved.id) }) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            HorizontalDivider()

            // â”€â”€ Notifications â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text(t("notifications", "Notifications"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            var notifInterests by remember { mutableStateOf(true) }
            var notifMessages  by remember { mutableStateOf(true) }
            var notifPromos    by remember { mutableStateOf(false) }

            NotifToggle("New interests received", "Alert when someone sends you an interest", notifInterests) { notifInterests = it }
            NotifToggle("New messages", "Alert on incoming chat messages", notifMessages) { notifMessages = it }
            NotifToggle("Tips & promotions", "Occasional tips and plan offers", notifPromos) { notifPromos = it }

            HorizontalDivider()

            // â”€â”€ Referral â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text(t("invite_friends", "Invite friends"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Share your referral code to earn rewards when friends subscribe.",
                        style = MaterialTheme.typography.bodyMedium)
                    var referralCode by remember { mutableStateOf("MC-INVITE") }
                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Your referral code") },
                        trailingIcon = {
                            IconButton(onClick = {}) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share referral link")
                    }
                }
            }

            HorizontalDivider()

            // â”€â”€ Privacy controls â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Text(t("privacy", "Privacy"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Control who can see your profile and contact details.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            var hideProfile by remember { mutableStateOf(false) }
            var photoVisibility by remember { mutableStateOf("Visible to all") }
            var showLastSeen by remember { mutableStateOf(true) }
            var showShortlists by remember { mutableStateOf(true) }
            var contactNumberVisible by remember { mutableStateOf(false) }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VisibilityOff, null, Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("hide_profile", "Hide profile"), style = MaterialTheme.typography.bodyMedium)
                    Text(t("hide_profile_desc", "Temporarily invisible to everyone"), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = hideProfile,
                    onCheckedChange = { hideProfile = it },
                    modifier = Modifier.testTag("settings_hide_profile")
                )
            }

            DropdownField(
                label = "Photo visibility",
                value = photoVisibility,
                options = listOf("Visible to all", "Visible to premium", "Visible to matches only", "Hidden"),
                onSelect = { photoVisibility = it },
                tag = "settings_photo_vis"
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("show_last_seen", "Show last seen"), style = MaterialTheme.typography.bodyMedium)
                    Text(t("show_last_seen_desc", "Let others see when you were last active"), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = showLastSeen, onCheckedChange = { showLastSeen = it })
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("show_profile_views", "Show profile views"), style = MaterialTheme.typography.bodyMedium)
                    Text(t("show_profile_views_desc", "Let others know you viewed their profile"), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = showShortlists, onCheckedChange = { showShortlists = it })
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("show_contact_number", "Show contact number"), style = MaterialTheme.typography.bodyMedium)
                    Text(t("show_contact_desc", "Display your phone to accepted matches"), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = contactNumberVisible,
                    onCheckedChange = { contactNumberVisible = it },
                    modifier = Modifier.testTag("settings_contact_vis")
                )
            }

            HorizontalDivider()

            // ── Language preference ──────────────────────────────────────────
            Text("Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val langDisplayName = remember(uiLanguage) {
                when (uiLanguage) {
                    "te" -> "తెలుగు (Telugu)"; "hi" -> "हिन्दी (Hindi)"; "ta" -> "தமிழ் (Tamil)"
                    "kn" -> "ಕನ್ನಡ (Kannada)"; "mr" -> "मराठी (Marathi)"; "bn" -> "বাংলা (Bengali)"
                    "gu" -> "ગુજરાતી (Gujarati)"; "ml" -> "മലയാളം (Malayalam)"; "pa" -> "ਪੰਜਾਬੀ (Punjabi)"
                    "ur" -> "اردو (Urdu)"; "es" -> "Español"; "pt" -> "Português"
                    "ru" -> "Pусский"; "ar" -> "العربية"; "de" -> "Deutsch"
                    "fr" -> "Français"; "it" -> "Italiano"; "ja" -> "日本語"
                    "ko" -> "한국어"; "zh" -> "简体中文"; "id" -> "Bahasa Indonesia"
                    "tr" -> "Türkçe"; "sw" -> "Kiswahili"; "vi" -> "Tiếng Việt"; "th" -> "ไทย"
                    else -> "English"
                }
            }
            OutlinedCard(
                onClick = onGoLanguage,
                modifier = Modifier.fillMaxWidth().testTag("settings_language")
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t("app_language", "App language"), style = MaterialTheme.typography.bodyMedium)
                        Text(langDisplayName, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.Filled.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            // ── Security ──────────────────────────────────────────────────
            Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, null, Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("biometric_lock", "Biometric app lock"), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Require fingerprint or face unlock on every open",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = biometricLock,
                    onCheckedChange = { vm.setBiometricLock(it) },
                    modifier = Modifier.testTag("settings_biometric_lock")
                )
            }

            HorizontalDivider()

            // ── Danger zone ───────────────────────────────────────────────
            var showDeleteDialog by remember { mutableStateOf(false) }
            var confirmText by remember { mutableStateOf("") }

            Text("Danger Zone", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            Text("These actions are permanent and cannot be reversed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("settings_delete_account"),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete my account permanently")
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false; confirmText = "" },
                    icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text("Delete Account?", color = MaterialTheme.colorScheme.error) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("This is permanent and cannot be undone. Your profile, photos, messages, and all data will be erased immediately.")
                            Text("Type DELETE to confirm:", style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = confirmText,
                                onValueChange = { confirmText = it.uppercase() },
                                singleLine = true,
                                placeholder = { Text("DELETE") },
                                isError = confirmText.isNotEmpty() && confirmText != "DELETE",
                                modifier = Modifier.fillMaxWidth().testTag("settings_delete_confirm_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { vm.deleteAccount(); showDeleteDialog = false; confirmText = "" },
                            enabled = confirmText == "DELETE" && accountState !is SettingsViewModel.AccountState.Deleting,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("settings_delete_confirm_btn")
                        ) {
                            if (accountState is SettingsViewModel.AccountState.Deleting)
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onError)
                            else Text("Yes, delete everything")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false; confirmText = "" }) { Text("Cancel") }
                    }
                )
            }

            HorizontalDivider()

            // ── App info ──────────────────────────────────────────────────
            Text(t("app_info", "App info"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Version 1.0.0 • Offline-first • All data on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    tag: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = if (value.isBlank()) "All" else value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().testTag(tag)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(if (opt.isBlank()) "All" else opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun NotifToggle(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

