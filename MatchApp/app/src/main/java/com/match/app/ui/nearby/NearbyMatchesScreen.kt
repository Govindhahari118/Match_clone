package com.match.app.ui.nearby

import android.Manifest
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
    val sharingLocation: Boolean = false,
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

    fun refresh(radiusKm: Int) = viewModelScope.launch {
        syncPermissionState()
        if (!_ui.value.permissionGranted) {
            _ui.update { it.copy(error = "Location permission is required to find nearby profiles.") }
            return@launch
        }
        if (!_ui.value.locationServicesEnabled) {
            _ui.update { it.copy(error = "Turn on device location and try again.") }
            return@launch
        }

        _ui.update { it.copy(loading = true, error = null) }
        runCatching { locationRepository.refreshAndFindNearby(radiusKm) }
            .onSuccess { profiles ->
                _ui.update {
                    it.copy(
                        matches = profiles,
                        loading = false,
                        sharingLocation = true,
                        error = null,
                        precisePermission = locationRepository.hasPreciseLocationPermission()
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        loading = false,
                        error = error.message?.take(200) ?: "Unable to load nearby profiles."
                    )
                }
            }
    }

    fun stopSharing() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { locationRepository.stopSharingLocation() }
            .onSuccess {
                _ui.update { it.copy(matches = emptyList(), loading = false, sharingLocation = false) }
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
    var radius by remember { mutableFloatStateOf(25f) }
    var requestedOnce by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        requestedOnce = true
        vm.syncPermissionState()
        val granted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) vm.refresh(radius.toInt())
    }

    LaunchedEffect(Unit) {
        vm.syncPermissionState()
        if (ui.permissionGranted && ui.locationServicesEnabled && !requestedOnce) {
            vm.refresh(radius.toInt())
            requestedOnce = true
        }
    }

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
                            "Nearby uses your current foreground location only when this feature is opened. Other members never receive your exact coordinates; they only see distance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (ui.permissionGranted) {
                            AssistChip(
                                onClick = {},
                                label = { Text(if (ui.precisePermission) "Precise location allowed" else "Approximate location allowed") },
                                leadingIcon = { Icon(Icons.Filled.PrivacyTip, null, Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            if (!ui.permissionGranted) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Enable location for Nearby", fontWeight = FontWeight.Bold)
                            Text(
                                "You can allow approximate location. Precise location improves distance accuracy but is optional.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("nearby_enable_location")
                            ) {
                                Icon(Icons.Filled.MyLocation, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Enable location")
                            }
                        }
                    }
                }
            } else {
                item {
                    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Search radius: ${radius.toInt()} km", fontWeight = FontWeight.SemiBold)
                            Slider(
                                value = radius,
                                onValueChange = { radius = it },
                                onValueChangeFinished = { vm.refresh(radius.toInt()) },
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
                                    enabled = !ui.loading,
                                    modifier = Modifier.weight(1f).testTag("nearby_refresh")
                                ) {
                                    Icon(Icons.Filled.Refresh, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Refresh")
                                }
                                OutlinedButton(
                                    onClick = vm::stopSharing,
                                    enabled = !ui.loading && ui.sharingLocation,
                                    modifier = Modifier.weight(1f).testTag("nearby_stop_sharing")
                                ) {
                                    Icon(Icons.Filled.LocationOff, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Stop sharing")
                                }
                            }
                        }
                    }
                }
            }

            if (!ui.locationServicesEnabled && ui.permissionGranted) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationDisabled, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text("Device location is turned off. Turn it on, then tap Refresh.", color = MaterialTheme.colorScheme.onErrorContainer)
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
            } else if (ui.permissionGranted && ui.sharingLocation && ui.matches.isEmpty() && ui.error == null) {
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
                    "Location is retained only to power Nearby and is excluded after 30 days without a refresh. Use Stop sharing to remove it immediately.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
