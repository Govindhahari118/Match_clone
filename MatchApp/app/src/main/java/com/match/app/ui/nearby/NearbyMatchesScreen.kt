package com.match.app.ui.nearby

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.match.app.data.repo.LocationRepository
import com.match.app.data.repo.NearbyProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NearbyUiState(
    val matches: List<NearbyProfile> = emptyList(),
    val loading: Boolean = false,
    val statusLoading: Boolean = true,
    val sharingLocation: Boolean = false,
    val lastSharedAt: Long = 0L,
    val permissionGranted: Boolean = false,
    val precisePermission: Boolean = false,
    val locationServicesEnabled: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(
        NearbyUiState(
            permissionGranted = locationRepository.hasLocationPermission(),
            precisePermission = locationRepository.hasPreciseLocationPermission(),
            locationServicesEnabled = locationRepository.isLocationServiceEnabled()
        )
    )
    val ui: StateFlow<NearbyUiState> = _ui.asStateFlow()

    fun syncPermissionState() {
        _ui.update {
            it.copy(
                permissionGranted = locationRepository.hasLocationPermission(),
                precisePermission = locationRepository.hasPreciseLocationPermission(),
                locationServicesEnabled = locationRepository.isLocationServiceEnabled()
            )
        }
    }

    /**
     * Restores server-side opt-in state without acquiring a new device location. A member who
     * previously tapped Stop sharing stays stopped after navigation, restart and sign-in.
     */
    fun loadStatus(radiusKm: Int) = viewModelScope.launch {
        syncPermissionState()
        _ui.update { it.copy(statusLoading = true, error = null) }
        runCatching { locationRepository.getSharingStatus() }
            .onSuccess { status ->
                _ui.update {
                    it.copy(
                        statusLoading = false,
                        sharingLocation = status.sharing,
                        lastSharedAt = status.updatedAtMillis,
                        error = null
                    )
                }
                if (status.sharing) searchStored(radiusKm)
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        statusLoading = false,
                        error = error.message?.take(200) ?: "Unable to check Nearby sharing status."
                    )
                }
            }
    }

    /** Explicit opt-in. This is the only path that can re-create a deleted location document. */
    fun enableSharing(radiusKm: Int) = viewModelScope.launch {
        syncPermissionState()
        if (!_ui.value.permissionGranted) {
            _ui.update { it.copy(error = "Allow location to enable Nearby.") }
            return@launch
        }
        if (!_ui.value.locationServicesEnabled) {
            _ui.update { it.copy(error = "Turn on device location to enable Nearby.") }
            return@launch
        }
        refreshInternal(radiusKm, enabling = true)
    }

    fun refresh(radiusKm: Int) = viewModelScope.launch {
        syncPermissionState()
        if (!_ui.value.sharingLocation) {
            _ui.update { it.copy(error = "Enable Nearby before refreshing your location.") }
            return@launch
        }
        if (!_ui.value.permissionGranted) {
            _ui.update { it.copy(error = "Location permission is required to refresh your position. You can still stop sharing.") }
            return@launch
        }
        if (!_ui.value.locationServicesEnabled) {
            _ui.update { it.copy(error = "Turn on device location, then refresh again.") }
            return@launch
        }
        refreshInternal(radiusKm, enabling = false)
    }

    /** Radius changes query the already-shared point and do not acquire a new location. */
    fun search(radiusKm: Int) = viewModelScope.launch {
        if (!_ui.value.sharingLocation) return@launch
        searchStored(radiusKm)
    }

    private suspend fun refreshInternal(radiusKm: Int, enabling: Boolean) {
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { locationRepository.refreshAndFindNearby(radiusKm) }
            .onSuccess { profiles ->
                _ui.update {
                    it.copy(
                        matches = profiles,
                        loading = false,
                        sharingLocation = true,
                        lastSharedAt = System.currentTimeMillis(),
                        error = null,
                        precisePermission = locationRepository.hasPreciseLocationPermission()
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        loading = false,
                        sharingLocation = if (enabling) false else it.sharingLocation,
                        error = error.message?.take(200) ?: "Unable to load nearby profiles."
                    )
                }
            }
    }

    private suspend fun searchStored(radiusKm: Int) {
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { locationRepository.findNearby(radiusKm) }
            .onSuccess { profiles -> _ui.update { it.copy(matches = profiles, loading = false, error = null) } }
            .onFailure { error ->
                // If the server expired the location, return to explicit opt-in instead of silently
                // obtaining a fresh coordinate.
                val message = error.message?.take(200) ?: "Unable to load nearby profiles."
                val expired = message.contains("expired", ignoreCase = true) || message.contains("Enable Nearby", ignoreCase = true)
                _ui.update {
                    it.copy(
                        matches = if (expired) emptyList() else it.matches,
                        loading = false,
                        sharingLocation = if (expired) false else it.sharingLocation,
                        error = message
                    )
                }
            }
    }

    fun stopSharing() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { locationRepository.stopSharingLocation() }
            .onSuccess {
                _ui.update {
                    it.copy(
                        matches = emptyList(),
                        loading = false,
                        sharingLocation = false,
                        lastSharedAt = 0L,
                        error = null
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(loading = false, error = error.message?.take(200) ?: "Unable to stop location sharing.")
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyMatchesScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: NearbyViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    var radius by rememberSaveable { mutableFloatStateOf(25f) }
    var requestedPermission by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        requestedPermission = true
        vm.syncPermissionState()
        val granted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) vm.enableSharing(radius.toInt())
    }

    LaunchedEffect(Unit) { vm.loadStatus(radius.toInt()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("nearby_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().testTag("nearby_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Matches near you", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Nearby is opt-in. A fresh foreground location is collected only when you enable or refresh this feature. Other members receive distance only, never your coordinates.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (ui.statusLoading) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        } else {
                            AssistChip(
                                onClick = {},
                                label = { Text(if (ui.sharingLocation) "Nearby sharing is on" else "Nearby sharing is off") },
                                leadingIcon = {
                                    Icon(
                                        if (ui.sharingLocation) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                                        null,
                                        Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        if (ui.permissionGranted) {
                            Text(
                                if (ui.precisePermission) "Device permission: precise location" else "Device permission: approximate location",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (!ui.statusLoading && !ui.sharingLocation) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Enable Nearby", fontWeight = FontWeight.Bold)
                            Text(
                                "Approximate location is enough. Precise location improves distance accuracy but is optional. Nothing is shared until you tap Enable Nearby.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (ui.permissionGranted) {
                                Button(
                                    onClick = { vm.enableSharing(radius.toInt()) },
                                    enabled = !ui.loading && ui.locationServicesEnabled,
                                    modifier = Modifier.fillMaxWidth().testTag("nearby_enable")
                                ) {
                                    Icon(Icons.Filled.MyLocation, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Enable Nearby")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        )
                                    },
                                    enabled = !ui.loading,
                                    modifier = Modifier.fillMaxWidth().testTag("nearby_enable_location")
                                ) {
                                    Icon(Icons.Filled.MyLocation, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Allow location & enable")
                                }
                                if (requestedPermission) {
                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(
                                                Intent(
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Open app settings") }
                                }
                            }
                        }
                    }
                }
            }

            if (!ui.locationServicesEnabled && ui.permissionGranted) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationDisabled, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Device location is turned off. Existing Nearby sharing can still be stopped, but enabling or refreshing needs device location.",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Open location settings") }
                        }
                    }
                }
            }

            if (ui.sharingLocation) {
                item {
                    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Search radius: ${radius.toInt()} km", fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = radius,
                                onValueChange = { radius = it },
                                onValueChangeFinished = { vm.search(radius.toInt()) },
                                valueRange = 5f..100f,
                                steps = 18,
                                enabled = !ui.loading
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("5 km", style = MaterialTheme.typography.labelSmall)
                                Text("100 km", style = MaterialTheme.typography.labelSmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { vm.refresh(radius.toInt()) },
                                    enabled = !ui.loading && ui.permissionGranted && ui.locationServicesEnabled,
                                    modifier = Modifier.weight(1f).testTag("nearby_refresh")
                                ) {
                                    Icon(Icons.Filled.Refresh, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Refresh position")
                                }
                                OutlinedButton(
                                    onClick = vm::stopSharing,
                                    enabled = !ui.loading,
                                    modifier = Modifier.weight(1f).testTag("nearby_stop_sharing")
                                ) {
                                    Icon(Icons.Filled.LocationOff, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Stop sharing")
                                }
                            }
                            if (!ui.permissionGranted) {
                                Text(
                                    "Location permission is currently off. Your previously shared point remains available until you stop sharing or it expires; Refresh position is disabled.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            ui.error?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(message, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            if (ui.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (ui.sharingLocation && ui.matches.isEmpty() && ui.error == null) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.LocationSearching, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(8.dp))
                        Text("No profiles found within ${radius.toInt()} km", fontWeight = FontWeight.SemiBold)
                        Text("Try a larger radius later.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (ui.matches.isNotEmpty()) {
                item {
                    Text("${ui.matches.size} nearby profiles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(ui.matches, key = { it.firebaseUid }) { profile ->
                    ElevatedCard(
                        onClick = { onOpenProfile(profile.userId) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (profile.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = profile.photoUrl,
                                    contentDescription = "${profile.displayName} profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp)
                                )
                            } else {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(profile.displayName.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(profile.displayName, fontWeight = FontWeight.SemiBold)
                                    if (profile.isVerified) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Filled.Verified, "Verified", Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text(
                                    listOf("${profile.age}", profile.profession, profile.city).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    if (profile.distanceKm < 1.0) "Less than 1 km away" else "${profile.distanceKm} km away",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (profile.isPremium) {
                                Icon(Icons.Filled.WorkspacePremium, "Premium", tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Exact coordinates are never readable by other clients. Stop sharing deletes your location immediately. Otherwise, the backend automatically deletes stored Nearby coordinates after 30 days without a refresh.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
