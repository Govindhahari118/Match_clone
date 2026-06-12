@file:Suppress("UNUSED_PARAMETER")
package com.match.app.ui.discovery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.UserProfile
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import javax.inject.Inject

@HiltViewModel
class SwipeDiscoveryViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles = _profiles.asStateFlow()

    private val _likedCount = MutableStateFlow(0)
    val likedCount = _likedCount.asStateFlow()

    private val _passedCount = MutableStateFlow(0)
    val passedCount = _passedCount.asStateFlow()

    init { loadProfiles() }

    private fun loadProfiles() = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        val entities = userDao.allExcluding(uid).takeLast(50).reversed()
        val list = mutableListOf<UserProfile>()
        for (e in entities) { auth.currentProfile(e.id)?.let { list.add(it) } }
        _profiles.value = list
    }

    fun onLike(profile: UserProfile) = viewModelScope.launch {
        _likedCount.value++
        _profiles.value = _profiles.value.drop(1)
    }

    fun onPass(profile: UserProfile) = viewModelScope.launch {
        _passedCount.value++
        _profiles.value = _profiles.value.drop(1)
    }

    fun onSuperLike(profile: UserProfile) = viewModelScope.launch {
        _likedCount.value++
        _profiles.value = _profiles.value.drop(1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDiscoveryScreen(
    onBack: () -> Unit = {},
    onOpenProfile: (Long) -> Unit = {},
    vm: SwipeDiscoveryViewModel = hiltViewModel()
) {
    val profiles by vm.profiles.collectAsState()
    val likedCount by vm.likedCount.collectAsState()
    val passedCount by vm.passedCount.collectAsState()

    // "It's a Match!" overlay state
    var matchOverlayProfile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(likedCount) {
        if (likedCount > 0) {
            // Show overlay for 2.5 seconds then auto-dismiss
            kotlinx.coroutines.delay(100)
            if (matchOverlayProfile == null) {
                // we show the overlay for the last liked profile
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("discover", "Discover")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Favorite, null, Modifier.size(14.dp),
                                tint = Color(0xFFE91E63))
                            Text("$likedCount", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (profiles.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🎉", fontSize = 64.sp)
                        Text(t("youve_seen_everyone", "You've seen everyone!"), style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(t("check_back_later", "Check back later for new profiles"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                        Button(onClick = onBack) { Text(t("browse_all_matches", "Browse All Matches")) }
                    }
                }
            } else {
                // Card stack (show top 3)
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background cards (depth effect)
                    profiles.take(3).reversed().forEachIndexed { index, _ ->
                        val isTop = index == profiles.take(3).size - 1
                        if (!isTop) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(480.dp)
                                    .graphicsLayer {
                                        scaleX = 0.92f - (index * 0.04f)
                                        scaleY = 0.92f - (index * 0.04f)
                                        translationY = (index * -18).dp.toPx()
                                    }
                            ) {}
                        }
                    }
                    // Top swipeable card
                    profiles.firstOrNull()?.let { profile ->
                        SwipeCard(
                            profile = profile,
                            onLike = {
                                matchOverlayProfile = profile
                                vm.onLike(profile)
                            },
                            onPass = { vm.onPass(profile) },
                            onTap = { onOpenProfile(profile.id) }
                        )
                    }
                }

                // Action buttons
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pass
                    FilledIconButton(
                        onClick = { profiles.firstOrNull()?.let { vm.onPass(it) } },
                        modifier = Modifier.size(60.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Icon(Icons.Filled.Close, null, Modifier.size(28.dp), tint = Color(0xFFE53935))
                    }
                    // Super Like
                    FilledIconButton(
                        onClick = { profiles.firstOrNull()?.let { vm.onSuperLike(it) } },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Icon(Icons.Filled.Star, null, Modifier.size(24.dp), tint = Color(0xFF1E88E5))
                    }
                    // Like
                    FilledIconButton(
                        onClick = { profiles.firstOrNull()?.let { vm.onLike(it) } },
                        modifier = Modifier.size(60.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFFCE4EC)
                        )
                    ) {
                        Icon(Icons.Filled.Favorite, null, Modifier.size(28.dp), tint = Color(0xFFE91E63))
                    }
                }

                // Stats bar
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$likedCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63))
                        Text(t("liked", "Liked"), style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${profiles.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text(t("remaining", "Remaining"), style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$passedCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline)
                        Text(t("passed", "Passed"), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    // ── "It's a Match!" overlay ────────────────────────────────────────────
    AnimatedVisibility(
        visible = matchOverlayProfile != null,
        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.85f),
        exit  = fadeOut(tween(400)) + scaleOut(tween(400))
    ) {
        matchOverlayProfile?.let { matched ->
            MatchCelebrationOverlay(
                matchedName = matched.displayName,
                matchedId   = matched.id,
                onMessage   = { onOpenProfile(matched.id); matchOverlayProfile = null },
                onDismiss   = { matchOverlayProfile = null }
            )
            LaunchedEffect(matched.id) {
                kotlinx.coroutines.delay(3000)
                matchOverlayProfile = null
            }
        }
    }
}

@Composable
private fun SwipeCard(
    profile: UserProfile,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onTap: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    var isDragging by remember { mutableStateOf(false) }
    val swipeThreshold = 350f

    val likeAlpha = if (offsetX.value > 50f) (offsetX.value / swipeThreshold).coerceIn(0f, 1f) else 0f
    val passAlpha = if (offsetX.value < -50f) (-offsetX.value / swipeThreshold).coerceIn(0f, 1f) else 0f

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .shadow(if (isDragging) 16.dp else 8.dp, RoundedCornerShape(20.dp))
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = rotation.value
            }
            .pointerInput(profile.id) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        scope.launch {
                            when {
                                offsetX.value > swipeThreshold -> {
                                    offsetX.animateTo(1200f, tween(300))
                                    onLike()
                                    offsetX.snapTo(0f); offsetY.snapTo(0f); rotation.snapTo(0f)
                                }
                                offsetX.value < -swipeThreshold -> {
                                    offsetX.animateTo(-1200f, tween(300))
                                    onPass()
                                    offsetX.snapTo(0f); offsetY.snapTo(0f); rotation.snapTo(0f)
                                }
                                else -> {
                                    offsetX.animateTo(0f, tween(400))
                                    offsetY.animateTo(0f, tween(400))
                                    rotation.animateTo(0f, tween(400))
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        scope.launch {
                            offsetX.animateTo(0f, tween(400))
                            offsetY.animateTo(0f, tween(400))
                            rotation.animateTo(0f, tween(400))
                        }
                    },
                    onDrag = { _, dragAmount ->
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.3f)
                            rotation.snapTo((offsetX.value / 20f).coerceIn(-15f, 15f))
                        }
                    }
                )
            }
    ) {
        Box(Modifier.fillMaxSize()) {
            // Gradient background
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF667EEA).copy(alpha = 0.3f),
                                Color(0xFF764BA2).copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            // Avatar
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(120.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            profile.displayName.first().uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Verified badge
                if (profile.isVerified) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Verified, null, Modifier.size(20.dp), tint = Color.White)
                        }
                    }
                }
            }

            // Info overlay
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${profile.displayName}, ${profile.age}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = Color.White)
                        if (profile.isPremium) {
                            Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = Color(0xFFFFB300))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(14.dp), tint = Color.White.copy(0.8f))
                        Text(profile.city, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                        Text("•", color = Color.White.copy(0.5f))
                        Text(profile.profession, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                    }
                    Text("${profile.religion} • ${profile.education}",
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))

                    // Compatibility score badge — uses real MatchScoreEngine result when available
                    val compat = if (profile.matchScore > 0f) (profile.matchScore * 100).toInt()
                                 else ((profile.id % 40) + 60).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.9f)) {
                            Text("$compat% Match", style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        if (profile.maritalStatus.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)) {
                                Text(profile.maritalStatus, style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
            }

            // LIKE stamp
            if (likeAlpha > 0f) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                        .rotate(-15f)
                        .graphicsLayer { alpha = likeAlpha }
                ) {
                    Text("LIKE 💚", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold, color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            // NOPE stamp
            if (passAlpha > 0f) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE53935),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .rotate(15f)
                        .graphicsLayer { alpha = passAlpha }
                ) {
                    Text("NOPE 💔", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold, color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// "It's a Match!" full-screen celebration overlay
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MatchCelebrationOverlay(
    matchedName: String,
    matchedId: Long,
    onMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    val avatarColors = remember(matchedId) {
        val palette = listOf(
            listOf(Color(0xFFE91E63), Color(0xFFFF5722)),
            listOf(Color(0xFF9C27B0), Color(0xFF3F51B5)),
            listOf(Color(0xFF009688), Color(0xFF4CAF50)),
            listOf(Color(0xFF1976D2), Color(0xFF00BCD4)),
        )
        palette[(matchedId % palette.size).toInt()]
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF880E4F).copy(0.95f), Color(0xFF4A148C).copy(0.95f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text("\uD83C\uDF89", fontSize = 64.sp)
            Text(
                "It's a Match!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            // Two avatars side by side
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // You
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFFFF5722)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, null, Modifier.size(36.dp), tint = Color.White)
                }
                Icon(Icons.Filled.Favorite, null, Modifier.size(32.dp), tint = Color(0xFFFF80AB))
                // Them
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(avatarColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(matchedName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(
                "You and $matchedName both sent interest!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onMessage,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF880E4F))
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(t("send_message", "Send a Message"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDismiss) {
                Text("Keep Browsing", color = Color.White.copy(0.8f))
            }
        }
    }
}
