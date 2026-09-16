package com.match.app.ui.discovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.match.app.data.repo.MatchingRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class SwipeCandidate(val profile: UserProfile, val compatibility: Int)

data class SwipeDiscoveryUiState(
    val loading: Boolean = true,
    val candidates: List<SwipeCandidate> = emptyList(),
    val likedThisSession: Int = 0,
    val skippedThisSession: Int = 0,
    val actionInFlight: Boolean = false,
    val error: String? = null,
    val mutualProfile: UserProfile? = null
)

@HiltViewModel
class SwipeDiscoveryViewModel @Inject constructor(
    private val session: SessionStore,
    private val matching: MatchingRepository,
    private val social: SocialRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(SwipeDiscoveryUiState())
    val ui = _ui.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null, mutualProfile = null) }
        runCatching {
            val me = session.userId.first() ?: error("You are not signed in.")
            val filter = session.filter.first()
            matching.recommendations(me, MatchMode.ADVANCED, filter)
                .map { SwipeCandidate(it.user, it.displayScore.coerceIn(0, 100)) }
        }.onSuccess { candidates ->
            _ui.update { it.copy(loading = false, candidates = candidates) }
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    loading = false,
                    candidates = emptyList(),
                    error = error.message?.take(180) ?: "Could not load discovery profiles."
                )
            }
        }
    }

    fun like(profile: UserProfile, superInterest: Boolean = false) = viewModelScope.launch {
        if (_ui.value.actionInFlight) return@launch
        _ui.update { it.copy(actionInFlight = true, error = null) }
        runCatching {
            val me = session.userId.first() ?: error("You are not signed in.")
            if (superInterest) {
                social.superLike(me, profile.id)
            } else {
                social.like(me, profile.id)
                social.isMutualMatch(me, profile.id)
            }
        }.onSuccess { mutual ->
            _ui.update { current ->
                current.copy(
                    actionInFlight = false,
                    candidates = current.candidates.filterNot { it.profile.id == profile.id },
                    likedThisSession = current.likedThisSession + 1,
                    mutualProfile = profile.takeIf { mutual }
                )
            }
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    actionInFlight = false,
                    error = error.message?.take(180) ?: "Interest could not be sent. Please try again."
                )
            }
        }
    }

    /** Skip is deliberately session-only; it does not create a permanent rejection record. */
    fun skip(profile: UserProfile) {
        if (_ui.value.actionInFlight) return
        _ui.update { current ->
            current.copy(
                candidates = current.candidates.filterNot { it.profile.id == profile.id },
                skippedThisSession = current.skippedThisSession + 1,
                error = null
            )
        }
    }

    fun dismissMutual() = _ui.update { it.copy(mutualProfile = null) }
    fun clearError() = _ui.update { it.copy(error = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDiscoveryScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: SwipeDiscoveryViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val current = ui.candidates.firstOrNull()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Quick Discover", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("swipe_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh, enabled = !ui.loading && !ui.actionInFlight) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh profiles")
                    }
                }
            )
        }
    ) { padding ->
        when {
            ui.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            current == null -> EmptyDiscoveryState(
                modifier = Modifier.padding(padding),
                liked = ui.likedThisSession,
                skipped = ui.skippedThisSession,
                onRefresh = vm::refresh,
                onBack = onBack
            )

            else -> Column(
                Modifier.fillMaxSize().padding(padding).testTag("swipe_discovery_screen"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "Swipe right to send an Interest, left to skip for this session, or use the buttons below.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DiscoveryCard(
                        candidate = current,
                        enabled = !ui.actionInFlight,
                        onOpen = { onOpenProfile(current.profile.id) },
                        onLike = { vm.like(current.profile) },
                        onSkip = { vm.skip(current.profile) }
                    )

                    if (ui.actionInFlight) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shadowElevation = 4.dp
                        ) {
                            CircularProgressIndicator(Modifier.padding(18.dp).size(28.dp))
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { vm.skip(current.profile) },
                        enabled = !ui.actionInFlight,
                        modifier = Modifier.size(60.dp).testTag("swipe_skip")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Skip for now", modifier = Modifier.size(28.dp))
                    }
                    FilledTonalIconButton(
                        onClick = { vm.like(current.profile, superInterest = true) },
                        enabled = !ui.actionInFlight,
                        modifier = Modifier.size(54.dp).testTag("swipe_super_interest")
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "Send Super Interest", modifier = Modifier.size(26.dp))
                    }
                    FilledIconButton(
                        onClick = { vm.like(current.profile) },
                        enabled = !ui.actionInFlight,
                        modifier = Modifier.size(64.dp).testTag("swipe_interest")
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Send Interest", modifier = Modifier.size(30.dp))
                    }
                }

                Text(
                    "${ui.likedThisSession} interests sent • ${ui.skippedThisSession} skipped this session • ${ui.candidates.size} remaining",
                    modifier = Modifier.padding(bottom = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    AnimatedVisibility(
        visible = ui.mutualProfile != null,
        enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.9f),
        exit = fadeOut(tween(180)) + scaleOut(tween(180))
    ) {
        ui.mutualProfile?.let { profile ->
            MutualInterestOverlay(
                profile = profile,
                onViewProfile = {
                    vm.dismissMutual()
                    onOpenProfile(profile.id)
                },
                onContinue = vm::dismissMutual
            )
        }
    }
}

@Composable
private fun DiscoveryCard(
    candidate: SwipeCandidate,
    enabled: Boolean,
    onOpen: () -> Unit,
    onLike: () -> Unit,
    onSkip: () -> Unit
) {
    val offset = remember(candidate.profile.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val threshold = 220f
    val primary = MaterialTheme.colorScheme.primary

    ElevatedCard(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 460.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .graphicsLayer {
                translationX = offset.value
                rotationZ = (offset.value / 28f).coerceIn(-10f, 10f)
            }
            .pointerInput(candidate.profile.id, enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDrag = { change, amount ->
                            change.consume()
                            scope.launch { offset.snapTo(offset.value + amount.x) }
                        },
                        onDragEnd = {
                            val decision = offset.value
                            scope.launch {
                                when {
                                    decision >= threshold -> {
                                        offset.animateTo(900f, tween(180))
                                        onLike()
                                    }
                                    decision <= -threshold -> {
                                        offset.animateTo(-900f, tween(180))
                                        onSkip()
                                    }
                                    else -> offset.animateTo(0f, tween(220))
                                }
                                offset.snapTo(0f)
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f, tween(220)) } }
                    )
                }
            },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().weight(1f).heightIn(min = 320.dp)) {
                val photo = candidate.profile.primaryPhotoPath.orEmpty().ifBlank { candidate.profile.photoUrl }
                if (photo.isNotBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = "Photo of ${candidate.profile.displayName}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            modifier = Modifier.size(116.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    candidate.profile.displayName.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primary
                                )
                            }
                        }
                    }
                }

                if (candidate.profile.isVerified) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)
                    ) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Verified profile",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(8.dp).size(20.dp)
                        )
                    }
                }

                val direction = offset.value
                if (abs(direction) > 45f) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (direction > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .align(if (direction > 0) Alignment.TopStart else Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Text(
                            if (direction > 0) "INTEREST" else "SKIP",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = if (direction > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${candidate.profile.displayName}, ${candidate.profile.age}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (candidate.compatibility > 0) {
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                "${candidate.compatibility}% signal",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                val location = listOf(candidate.profile.city, candidate.profile.state).filter { it.isNotBlank() }.joinToString(", ")
                val work = candidate.profile.profession.takeIf { it.isNotBlank() }
                if (location.isNotBlank() || work != null) {
                    Text(
                        listOfNotNull(location.takeIf { it.isNotBlank() }, work).joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val context = listOf(candidate.profile.religion, candidate.profile.motherTongue, candidate.profile.education)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                if (context.isNotBlank()) {
                    Text(context, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                TextButton(onClick = onOpen, contentPadding = PaddingValues(0.dp)) {
                    Text("View full profile")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyDiscoveryState(
    modifier: Modifier,
    liked: Int,
    skipped: Int,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Explore, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Text("No more profiles in this quick stack", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                "$liked interests sent and $skipped profiles skipped in this session. Refresh to use your current filters again, or return to Matches for the full discovery list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Filled.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun MutualInterestOverlay(
    profile: UserProfile,
    onViewProfile: () -> Unit,
    onContinue: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.76f)),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Favorite, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                Text("Mutual interest", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "You and ${profile.displayName} have both chosen to connect.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onViewProfile, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("View profile")
                }
                TextButton(onClick = onContinue) { Text("Keep discovering") }
            }
        }
    }
}
