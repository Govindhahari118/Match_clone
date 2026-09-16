package com.match.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.SavedSearchEntity
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.AuthResult
import com.match.app.data.repo.SavedSearchRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.ReligionExperiencePreference
import com.match.app.ui.theme.AppPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao,
    private val authRepo: AuthRepository,
    private val savedSearchRepo: SavedSearchRepository
) : ViewModel() {
    val darkMode = session.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val paletteKey = session.paletteKey.stateIn(viewModelScope, SharingStarted.Eagerly, "VIVAH")
    val religionExperience = session.religionExperience.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ReligionExperiencePreference()
    )
    val biometricLock = session.biometricLock.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val planKey = session.subscriptionPlan.stateIn(viewModelScope, SharingStarted.Eagerly, "FREE")
    val uiLanguage = session.uiLanguage.stateIn(viewModelScope, SharingStarted.Eagerly, "en")
    val currentFilter = session.filter.stateIn(viewModelScope, SharingStarted.Eagerly, MatchFilter())

    val user: StateFlow<UserEntity?> = session.userId
        .flatMapLatest { id -> if (id == null) flowOf(null) else userDao.observeById(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val savedSearches: StateFlow<List<SavedSearchEntity>> = session.userId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else savedSearchRepo.observeForUser(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    sealed class AccountState {
        data object Idle : AccountState()
        data object Deleting : AccountState()
        data object Deleted : AccountState()
        data class Error(val message: String) : AccountState()
    }

    private val _accountState = MutableStateFlow<AccountState>(AccountState.Idle)
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    private val _searchMessage = MutableStateFlow<String?>(null)
    val searchMessage: StateFlow<String?> = _searchMessage.asStateFlow()

    fun setDarkMode(value: Boolean) = viewModelScope.launch { session.setDarkMode(value) }

    /** Selecting a manual palette explicitly leaves automatic religion-following mode. */
    fun setPalette(value: AppPalette) = viewModelScope.launch {
        session.setPalette(value.name)
        session.setReligionThemeEnabled(false)
    }

    fun setAutomaticReligionTheme(enabled: Boolean) = viewModelScope.launch {
        session.setReligionThemeEnabled(enabled)
    }

    fun setBiometricLock(value: Boolean) = viewModelScope.launch { session.setBiometricLock(value) }

    fun saveCurrentSearch(name: String) = viewModelScope.launch {
        val id = session.userId.first() ?: return@launch
        savedSearchRepo.save(id, name, currentFilter.value)
        _searchMessage.value = "Search saved."
    }

    fun applySavedSearch(search: SavedSearchEntity) = viewModelScope.launch {
        session.setFilter(savedSearchRepo.toFilter(search))
        _searchMessage.value = "${search.name} applied to Matches."
    }

    fun deleteSavedSearch(search: SavedSearchEntity) = viewModelScope.launch {
        savedSearchRepo.delete(search.id)
        _searchMessage.value = "Saved search deleted."
    }

    fun savedSearchToFilter(search: SavedSearchEntity): MatchFilter = savedSearchRepo.toFilter(search)
    fun consumeSearchMessage() { _searchMessage.value = null }

    fun deleteAccount() = viewModelScope.launch {
        val id = session.userId.first()
        if (id == null) {
            _accountState.value = AccountState.Error("You are not signed in.")
            return@launch
        }
        _accountState.value = AccountState.Deleting
        _accountState.value = when (val result = authRepo.deleteAccount(id)) {
            is AuthResult.Success -> AccountState.Deleted
            is AuthResult.Error -> AccountState.Error(result.message)
        }
    }

    fun resetError() {
        if (_accountState.value is AccountState.Error) _accountState.value = AccountState.Idle
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onGoLanguage: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    onAccountDeleted: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsState()
    val darkMode by vm.darkMode.collectAsState()
    val paletteKey by vm.paletteKey.collectAsState()
    val religionExperience by vm.religionExperience.collectAsState()
    val biometric by vm.biometricLock.collectAsState()
    val plan by vm.planKey.collectAsState()
    val language by vm.uiLanguage.collectAsState()
    val accountState by vm.accountState.collectAsState()
    val currentFilter by vm.currentFilter.collectAsState()
    val savedSearches by vm.savedSearches.collectAsState()
    val searchMessage by vm.searchMessage.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by remember { mutableStateOf(false) }
    var saveSearchDialog by remember { mutableStateOf(false) }
    var searchName by remember { mutableStateOf("") }

    val manualThemePalettes = remember {
        listOf(
            AppPalette.VIVAH,
            AppPalette.HINDU,
            AppPalette.MUSLIM,
            AppPalette.CHRISTIAN,
            AppPalette.SIKH,
            AppPalette.BUDDHIST,
            AppPalette.JAIN,
            AppPalette.PARSI
        )
    }

    LaunchedEffect(accountState) {
        when (val state = accountState) {
            SettingsViewModel.AccountState.Deleted -> onAccountDeleted()
            is SettingsViewModel.AccountState.Error -> {
                snackbar.showSnackbar(state.message)
                vm.resetError()
            }
            else -> Unit
        }
    }
    LaunchedEffect(searchMessage) {
        searchMessage?.let {
            snackbar.showSnackbar(it)
            vm.consumeSearchMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(46.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(user?.displayName?.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user?.displayName ?: "Account", fontWeight = FontWeight.SemiBold)
                            user?.username?.takeIf { it.isNotBlank() }?.let { Text("@$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                            Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (user?.isVerified == true) Icon(Icons.Filled.Verified, "Verified", tint = MaterialTheme.colorScheme.primary)
                    }
                    user?.phoneNumber?.takeIf { it.isNotBlank() }?.let { SettingInfoRow(Icons.Filled.Phone, "Phone", maskPhone(it)) }
                    SettingInfoRow(Icons.Filled.Badge, "Profile ID", user?.matrimonyId?.ifBlank { user?.id?.let { id -> "M$id" } ?: "" }.orEmpty())
                }
            }

            Text("Membership", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(plan.ifBlank { "FREE" }.replace('_', ' '), fontWeight = FontWeight.SemiBold)
                        Text("Membership status is synchronized from the server.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onUpgrade) { Text("Plans") }
                }
            }

            Text("Saved searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current filters", fontWeight = FontWeight.SemiBold)
                    Text(searchFilterSummary(currentFilter), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { searchName = ""; saveSearchDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.BookmarkAdd, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save current search")
                    }
                    if (savedSearches.isEmpty()) {
                        Text("No saved searches yet. Set filters in Matches, then save them here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        savedSearches.forEach { search ->
                            HorizontalDivider()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(search.name, fontWeight = FontWeight.Medium)
                                    Text(searchFilterSummary(vm.savedSearchToFilter(search)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                }
                                TextButton(onClick = { vm.applySavedSearch(search) }) { Text("Apply") }
                                IconButton(onClick = { vm.deleteSavedSearch(search) }) { Icon(Icons.Filled.DeleteOutline, "Delete saved search") }
                            }
                        }
                    }
                }
            }

            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SettingToggle(
                icon = Icons.Filled.AutoAwesome,
                title = "Automatic religion theme",
                subtitle = "Follow your confirmed profile religion. This changes presentation only.",
                checked = religionExperience.religionThemeEnabled,
                onCheckedChange = vm::setAutomaticReligionTheme
            )
            SettingToggle(
                icon = if (darkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                title = "Dark mode",
                subtitle = if (darkMode) "Dark appearance enabled" else "Use the light appearance",
                checked = darkMode,
                onCheckedChange = vm::setDarkMode
            )

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Manual theme", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (religionExperience.religionThemeEnabled) {
                            "Automatic mode is active. Choose any theme below to switch to a manual override."
                        } else {
                            "Matree Neutral is always available. A manual visual theme never changes your profile religion or matching preferences."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        manualThemePalettes.forEach { palette ->
                            FilterChip(
                                selected = !religionExperience.religionThemeEnabled && palette.name == paletteKey,
                                onClick = { vm.setPalette(palette) },
                                label = { Text(palette.label) },
                                leadingIcon = {
                                    Surface(
                                        shape = CircleShape,
                                        color = palette.swatch,
                                        modifier = Modifier.size(16.dp)
                                    ) { Box(Modifier) {} }
                                }
                            )
                        }
                    }
                }
            }

            Text("Language & security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(onClick = onGoLanguage, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("App language", fontWeight = FontWeight.SemiBold)
                        Text(language.uppercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null)
                }
            }
            SettingToggle(
                icon = Icons.Filled.Fingerprint,
                title = "Biometric app lock",
                subtitle = "Require device biometrics or device credential when returning to the app.",
                checked = biometric,
                onCheckedChange = vm::setBiometricLock
            )

            HorizontalDivider()
            Text("Account deletion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Permanently delete your account and start the authenticated server cleanup flow. This action cannot be undone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { confirmDelete = true },
                enabled = accountState !is SettingsViewModel.AccountState.Deleting,
                modifier = Modifier.fillMaxWidth().testTag("settings_delete_account"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                if (accountState is SettingsViewModel.AccountState.Deleting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text(if (accountState is SettingsViewModel.AccountState.Deleting) "Deleting account…" else "Delete account")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (saveSearchDialog) {
        AlertDialog(
            onDismissRequest = { saveSearchDialog = false },
            title = { Text("Save current search") },
            text = {
                OutlinedTextField(
                    value = searchName,
                    onValueChange = { searchName = it.take(60) },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Hyderabad Telugu 25–30") },
                    singleLine = true
                )
            },
            confirmButton = { Button(onClick = { vm.saveCurrentSearch(searchName); saveSearchDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { saveSearchDialog = false }) { Text("Cancel") } }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete account permanently?") },
            text = { Text("Your profile will be removed and the server will clean up associated account data. Payment/audit records may be retained only where legally required.") },
            confirmButton = {
                Button(onClick = { confirmDelete = false; vm.deleteAccount() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete permanently") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun searchFilterSummary(f: MatchFilter): String {
    val parts = mutableListOf("Age ${f.ageMin}–${f.ageMax}")
    if (f.state.isNotBlank()) parts += f.state
    if (f.city.isNotBlank()) parts += f.city
    if (f.motherTongue.isNotBlank()) parts += f.motherTongue
    if (f.religion.isNotBlank()) parts += f.religion
    if (f.caste.isNotBlank()) parts += f.caste
    if (f.educationLevel.isNotBlank()) parts += f.educationLevel
    if (f.occupationCategory.isNotBlank()) parts += f.occupationCategory
    if (f.lastActiveWithinDays > 0) parts += "Active ≤${f.lastActiveWithinDays}d"
    if (f.verifiedOnly || f.verifiedLevel > 0) parts += "Verified"
    if (f.keyword.isNotBlank()) parts += "“${f.keyword.take(24)}”"
    return parts.take(7).joinToString(" • ")
}

private fun maskPhone(value: String): String {
    val trimmed = value.trim()
    if (trimmed.length <= 4) return "••••"
    return "•".repeat((trimmed.length - 4).coerceAtMost(8)) + trimmed.takeLast(4)
}
