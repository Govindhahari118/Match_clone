package com.match.app.ui.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.remote.ContactVisibility
import com.match.app.data.remote.FirestorePrivacyService
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.remote.MemberPrivacyRelation
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyMemberUi(
    val uid: String,
    val displayName: String,
    val city: String,
    val profileHidden: Boolean,
    val contactHidden: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao,
    private val firestoreProfile: FirestoreProfileService,
    private val privacy: FirestorePrivacyService
) : ViewModel() {
    val incognito = session.incognitoMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val user: StateFlow<UserEntity?> = session.userId
        .flatMapLatest { id -> if (id == null) flowOf(null) else userDao.observeById(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val candidateProfiles: Flow<List<UserEntity>> = session.userId.filterNotNull().map { myId ->
        userDao.allExcluding(myId)
            .filter { !it.isSeed && it.firebaseUid.isNotBlank() }
            .distinctBy { it.firebaseUid }
            .sortedBy { it.displayName.lowercase() }
    }

    private val relations: Flow<List<MemberPrivacyRelation>> = session.firebaseUid.filterNotNull()
        .flatMapLatest(privacy::observeRelations)

    val members: StateFlow<List<PrivacyMemberUi>> = combine(candidateProfiles, relations) { candidates, relationList ->
        val byUid = relationList.associateBy { it.memberUid }
        val candidateMap = candidates.associateBy { it.firebaseUid }
        val allUids = (candidateMap.keys + byUid.keys).sortedBy { candidateMap[it]?.displayName?.lowercase() ?: it }
        allUids.map { uid ->
            val candidate = candidateMap[uid]
            val relation = byUid[uid]
            PrivacyMemberUi(
                uid = uid,
                displayName = candidate?.displayName?.takeIf { it.isNotBlank() } ?: "Member",
                city = candidate?.city.orEmpty(),
                profileHidden = relation?.profileHidden == true,
                contactHidden = relation?.contactHidden == true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contactVisibility: StateFlow<ContactVisibility> = session.firebaseUid.filterNotNull()
        .flatMapLatest(privacy::observeContactVisibility)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContactVisibility.MUTUAL_MATCHES)

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setIncognito(value: Boolean) = viewModelScope.launch { session.setIncognitoMode(value) }
    fun setLastActiveVisible(value: Boolean) = updateUser { it.copy(showLastActive = value) }
    fun setHoroscopeVisible(value: Boolean) = updateUser { it.copy(showHoroscope = value) }
    fun setIncomeDisclosure(value: String) = updateUser { it.copy(incomeDisclosure = value) }

    fun setContactVisibility(value: ContactVisibility) = privacyUpdate {
        val uid = session.firebaseUid.first() ?: error("Sign in required")
        privacy.setContactVisibility(uid, value)
    }

    fun setProfileHidden(memberUid: String, hidden: Boolean) = privacyUpdate {
        val uid = session.firebaseUid.first() ?: error("Sign in required")
        privacy.setProfileHidden(uid, memberUid, hidden)
    }

    fun setContactHidden(memberUid: String, hidden: Boolean) = privacyUpdate {
        val uid = session.firebaseUid.first() ?: error("Sign in required")
        privacy.setContactHidden(uid, memberUid, hidden)
    }

    private fun privacyUpdate(action: suspend () -> Unit) = viewModelScope.launch {
        _saving.value = true
        _error.value = null
        runCatching { action() }
            .onFailure { _error.value = "Could not save this privacy setting. Please try again." }
        _saving.value = false
    }

    private fun updateUser(transform: (UserEntity) -> UserEntity) = viewModelScope.launch {
        val current = user.value ?: return@launch
        val updated = transform(current)
        _saving.value = true
        _error.value = null
        try {
            userDao.update(updated)
            if (updated.firebaseUid.isNotBlank()) firestoreProfile.pushProfile(updated)
        } catch (_: Exception) {
            userDao.update(current)
            _error.value = "Could not save this privacy setting. Please try again."
        } finally {
            _saving.value = false
        }
    }

    fun dismissError() { _error.value = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    onBack: () -> Unit = {},
    onGoSettings: () -> Unit = {},
    vm: PrivacyViewModel = hiltViewModel()
) {
    val incognito by vm.incognito.collectAsState()
    val user by vm.user.collectAsState()
    val members by vm.members.collectAsState()
    val contactVisibility by vm.contactVisibility.collectAsState()
    val saving by vm.saving.collectAsState()
    val error by vm.error.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var manageVisibility by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            vm.dismissError()
        }
    }

    if (manageVisibility) {
        MemberVisibilityDialog(
            members = members,
            saving = saving,
            onProfileHidden = vm::setProfileHidden,
            onContactHidden = vm::setContactHidden,
            onDismiss = { manageVisibility = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Privacy & visibility") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("privacy_dash_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("privacy_dashboard_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Your privacy choices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Visibility is enforced in profile reads, discovery, Nearby and private contact reveal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            PrivacyToggleCard(
                icon = Icons.Filled.VisibilityOff,
                title = "Private browsing on this device",
                subtitle = "When enabled, opening another profile does not add a local 'Who Viewed' record from this device.",
                checked = incognito,
                enabled = !saving,
                onCheckedChange = vm::setIncognito
            )

            PrivacyToggleCard(
                icon = Icons.Filled.Schedule,
                title = "Show last active",
                subtitle = "Controls whether your last-active time is published in your discoverable profile data.",
                checked = user?.showLastActive ?: true,
                enabled = user != null && !saving,
                onCheckedChange = vm::setLastActiveVisible
            )

            PrivacyToggleCard(
                icon = Icons.Filled.AutoAwesome,
                title = "Show horoscope details",
                subtitle = "Controls whether Rasi, Nakshatra and related astrology fields are shown to members who can view your profile.",
                checked = user?.showHoroscope ?: true,
                enabled = user != null && !saving,
                onCheckedChange = vm::setHoroscopeVisible
            )

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CurrencyRupee, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Income visibility", fontWeight = FontWeight.SemiBold)
                            Text("Choose how your income band appears to other members.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    val current = user?.incomeDisclosure ?: "range"
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("range" to "Show range", "hidden" to "Hide").forEach { (value, label) ->
                            FilterChip(
                                selected = current.equals(value, true),
                                onClick = { vm.setIncomeDisclosure(value) },
                                enabled = user != null && !saving,
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhoneLocked, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Who can reveal my contact?", fontWeight = FontWeight.SemiBold)
                            Text(
                                "A paid member still needs a mutual match. You can additionally stop all phone reveals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = contactVisibility == ContactVisibility.MUTUAL_MATCHES,
                            onClick = { vm.setContactVisibility(ContactVisibility.MUTUAL_MATCHES) },
                            enabled = !saving,
                            label = { Text("Mutual matches") }
                        )
                        FilterChip(
                            selected = contactVisibility == ContactVisibility.NOBODY,
                            onClick = { vm.setContactVisibility(ContactVisibility.NOBODY) },
                            enabled = !saving,
                            label = { Text("Nobody") }
                        )
                    }
                }
            }

            val profileHiddenCount = members.count { it.profileHidden }
            val contactHiddenCount = members.count { it.contactHidden }
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PersonOff, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Hide from selected members", fontWeight = FontWeight.SemiBold)
                            Text(
                                "$profileHiddenCount profile exceptions • $contactHiddenCount contact exceptions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "Like WhatsApp visibility exceptions: selected members can be excluded from seeing your matrimonial profile and/or from revealing your phone. This is one-way and is not the same as blocking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { manageVisibility = true },
                        modifier = Modifier.fillMaxWidth().testTag("manage_visibility_exceptions")
                    ) {
                        Icon(Icons.Filled.ManageAccounts, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manage member exceptions")
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Profile photos", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Profile photos follow profile visibility. A member excluded from your profile cannot fetch the profile document through the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Block, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Block & report", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Use Block when you want to stop interaction entirely. Privacy exceptions only control what that member can see.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()
            Text("Account data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Account deletion is available in Settings. The authenticated deletion flow removes server data before the local session is cleared.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onGoSettings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.ManageAccounts, null)
                Spacer(Modifier.width(8.dp))
                Text("Open account settings")
            }

            if (saving) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Saving privacy setting…", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MemberVisibilityDialog(
    members: List<PrivacyMemberUi>,
    saving: Boolean,
    onProfileHidden: (String, Boolean) -> Unit,
    onContactHidden: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(members, query) {
        members.filter {
            query.isBlank() || it.displayName.contains(query, ignoreCase = true) || it.city.contains(query, ignoreCase = true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Member visibility exceptions") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    label = { Text("Search members") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text(
                        "No interacted/discovered members are available yet. Open profiles from Discover or Interests first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filtered, key = { it.uid }) { member ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(member.displayName, fontWeight = FontWeight.SemiBold)
                                    if (member.city.isNotBlank()) {
                                        Text(member.city, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Hide my profile", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                        Switch(
                                            checked = member.profileHidden,
                                            onCheckedChange = { onProfileHidden(member.uid, it) },
                                            enabled = !saving
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Hide my contact", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                        Switch(
                                            checked = member.contactHidden,
                                            onCheckedChange = { onContactHidden(member.uid, it) },
                                            enabled = !saving
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun PrivacyToggleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
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
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}
