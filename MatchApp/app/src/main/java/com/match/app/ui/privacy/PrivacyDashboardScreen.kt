package com.match.app.ui.privacy

import androidx.compose.foundation.layout.*
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
import com.match.app.data.remote.FirestoreProfileService
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao,
    private val firestoreProfile: FirestoreProfileService
) : ViewModel() {
    val incognito = session.incognitoMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val user: StateFlow<UserEntity?> = session.userId
        .flatMapLatest { id -> if (id == null) flowOf(null) else userDao.observeById(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setIncognito(value: Boolean) = viewModelScope.launch {
        session.setIncognitoMode(value)
    }

    fun setLastActiveVisible(value: Boolean) = updateUser { it.copy(showLastActive = value) }
    fun setHoroscopeVisible(value: Boolean) = updateUser { it.copy(showHoroscope = value) }
    fun setIncomeDisclosure(value: String) = updateUser { it.copy(incomeDisclosure = value) }

    private fun updateUser(transform: (UserEntity) -> UserEntity) = viewModelScope.launch {
        val current = user.value ?: return@launch
        val updated = transform(current)
        _saving.value = true
        _error.value = null
        try {
            userDao.update(updated)
            if (updated.firebaseUid.isNotBlank()) firestoreProfile.pushProfile(updated)
        } catch (e: Exception) {
            userDao.update(current)
            _error.value = "Could not save this privacy setting. Please try again."
        } finally {
            _saving.value = false
        }
    }

    fun dismissError() { _error.value = null }
}

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun PrivacyDashboardScreen(
    onBack: () -> Unit = {},
    onGoSettings: () -> Unit = {},
    vm: PrivacyViewModel = hiltViewModel()
) {
    val incognito by vm.incognito.collectAsState()
    val user by vm.user.collectAsState()
    val saving by vm.saving.collectAsState()
    val error by vm.error.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            vm.dismissError()
        }
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
                            "Only controls that are actually enforced by the app are shown here.",
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
                subtitle = "Controls whether Rasi, Nakshatra and related astrology fields are published to other members.",
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Profile photos", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Uploaded profile photos are visible to signed-in members who can access your profile. Delete a photo from your Profile if you no longer want it shown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhoneLocked, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Contact details", fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Your phone number and email are not stored in the public profile document. Phone reveal is handled by a server-side entitlement and mutual-match check.",
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
                        "Use Block or Report from a member profile or conversation. Blocking prevents new interests and messages between the two accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()
            Text("Account data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Account deletion is available in Settings. The deletion flow removes the account through the authenticated server cleanup path before the local session is cleared.",
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
