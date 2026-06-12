package com.match.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.match.app.data.local.dao.LikeDao
import com.match.app.data.local.dao.ShortlistDao
import com.match.app.data.local.entity.PhotoEntity
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.PhotoRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.UserProfile
import com.match.app.ui.common.ProfileCompletenessBar
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val photoRepo: PhotoRepository,
    private val likeDao: LikeDao,
    private val shortlistDao: ShortlistDao
) : ViewModel() {
    val profile: StateFlow<UserProfile?> = session.userId
        .map { id -> id?.let { auth.currentProfile(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val photos: StateFlow<List<PhotoEntity>> = session.userId.filterNotNull()
        .flatMapLatest { photoRepo.observe(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Count of people who liked me. */
    val likeCount: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { likeDao.observeIncomingCount(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    /** Count of profiles I have shortlisted. */
    val savedCount: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { shortlistDao.observeCount(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun importPhoto(uri: Uri) = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch; photoRepo.import(uid, uri)
    }
    fun setPrimary(photo: PhotoEntity) = viewModelScope.launch { photoRepo.setPrimary(photo.userId, photo.id) }
    fun setPrivacy(photo: PhotoEntity, privacy: String) = viewModelScope.launch { photoRepo.setPrivacy(photo.id, privacy) }
    fun delete(photo: PhotoEntity) = viewModelScope.launch { photoRepo.delete(photo) }
    fun signOut() = viewModelScope.launch { auth.signOut() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onGoSettings: () -> Unit = {},
    onGoNotifications: () -> Unit = {},
    onGoWhoViewed: () -> Unit = {},
    onGoShortlists: () -> Unit = {},
    onGoKundli: () -> Unit = {},
    onGoPricing: () -> Unit = {},
    onGoStories: () -> Unit = {},
    onGoFamily: () -> Unit = {},
    onGoInterests: () -> Unit = {},
    onGoVerification: () -> Unit = {},
    onGoHelp: () -> Unit = {},
    onGoTerms: () -> Unit = {},
    onGoPrivacy: () -> Unit = {},
    onGoGuidelines: () -> Unit = {},
    onGoBiodata: () -> Unit = {},
    unreadNotif: Int = 0,
    vm: ProfileViewModel = hiltViewModel()
) {
    val profile by vm.profile.collectAsState()
    val photos by vm.photos.collectAsState()
    val likeCount by vm.likeCount.collectAsState()
    val savedCount by vm.savedCount.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::importPhoto) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("profile_screen")) {
        profile?.let { p ->
            // ── Hero banner ──────────────────────────────────────────────
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val heroGradient = remember(p.id) {
                        val palette = listOf(
                            listOf(Color(0xFFE91E63), Color(0xFFFF5722)),
                            listOf(Color(0xFF9C27B0), Color(0xFF3F51B5)),
                            listOf(Color(0xFF009688), Color(0xFF4CAF50)),
                            listOf(Color(0xFF1976D2), Color(0xFF00BCD4)),
                            listOf(Color(0xFF795548), Color(0xFF607D8B)),
                            listOf(Color(0xFFFF9800), Color(0xFFFFEB3B)),
                        )
                        palette[(p.id % palette.size).toInt()]
                    }
                    Box(
                        Modifier.size(88.dp).clip(RoundedCornerShape(26.dp))
                            .background(Brush.linearGradient(heroGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(p.displayName.first().uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (p.isVerified) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.Verified, null, Modifier.size(20.dp), tint = Color(0xFF1976D2)) }
                        if (p.isPremium) { Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Star, null, Modifier.size(18.dp), tint = Color(0xFFFFB300)) }
                    }
                    Text("${p.age} • ${p.city} • ${p.profession}", style = MaterialTheme.typography.bodyMedium)
                    Text("${p.religion} • ${p.motherTongue} • ${p.maritalStatus}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${t("profile_id", "Profile ID")}: M${p.id}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline)

                    // Completeness bar — enhanced version with detailed breakdown
                    Spacer(Modifier.height(12.dp))
                    ProfileCompletenessBar(
                        profile = p,
                        onComplete = onGoKundli,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            // ── Quick stats ──────────────────────────────────────────────
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    QuickStat(Icons.Filled.Visibility, t("views", "Views"), p.profileViewCount.toString())
                    VerticalDivider(Modifier.height(36.dp))
                    QuickStat(Icons.Filled.Favorite, t("likes", "Likes"), likeCount.toString())
                    VerticalDivider(Modifier.height(36.dp))
                    QuickStat(Icons.Filled.Bookmark, t("saved", "Saved"), savedCount.toString())
                }
            }

            Spacer(Modifier.height(12.dp))
            // ── Trust Score (LoveVivah Trust Meter pattern) ───────────────
            val trustItems = listOf(
                Triple("Profile photo",       photos.isNotEmpty(),        Icons.Filled.PhotoCamera),
                Triple("Bio / About me",      p.bio.isNotBlank(),         Icons.Filled.EditNote),
                Triple("City & state",        p.city.isNotBlank(),        Icons.Filled.LocationOn),
                Triple("Profession & income", p.profession.isNotBlank(),  Icons.Filled.Work),
                Triple("Questionnaire",       p.hasQuestionnaire,         Icons.Filled.Quiz),
                Triple("Astrology (Rasi)",    p.rasi.isNotBlank(),        Icons.Filled.AutoAwesome),
                Triple("Mobile verified",     true,                       Icons.Filled.PhoneAndroid),
                Triple("ID verified",         p.isVerified,               Icons.Filled.Verified)
            )
            val trustScore = trustItems.count { it.second }
            val trustPct = trustScore.toFloat() / trustItems.size
            val trustLabel = when {
                trustPct >= 0.875f -> "Excellent"
                trustPct >= 0.625f -> "Good"
                trustPct >= 0.375f -> "Fair"
                else               -> "Basic"
            }
            val trustColor = when {
                trustPct >= 0.875f -> Color(0xFF2E7D32)
                trustPct >= 0.625f -> Color(0xFF1565C0)
                trustPct >= 0.375f -> Color(0xFFE65100)
                else               -> Color(0xFFC62828)
            }
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, null, Modifier.size(22.dp), tint = trustColor)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${t("trust_score", "Trust Score")} — $trustLabel",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("$trustScore of ${trustItems.size} trust signals active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${(trustPct * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = trustColor)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { trustPct },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = trustColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    // Trust checklist
                    trustItems.forEach { (label, done, icon) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon, null, Modifier.size(16.dp),
                                tint = if (done) trustColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (done) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                            if (done) {
                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp), tint = trustColor)
                            } else {
                                Icon(Icons.Filled.RadioButtonUnchecked, null, Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                    if (trustScore < trustItems.size) {
                        Spacer(Modifier.height(8.dp))
                        Text("Complete your profile to get ${(trustPct * 100).toInt()}% → 100% trust score and 3× more responses.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(t("browse", "Discover"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HubCard(Icons.Filled.Bookmarks, "Shortlists", Modifier.weight(1f), onClick = onGoShortlists)
                HubCard(Icons.Filled.RemoveRedEye, "Who Viewed", Modifier.weight(1f), onClick = onGoWhoViewed)
                HubCard(Icons.Filled.AutoAwesome, "Kundli", Modifier.weight(1f), onClick = onGoKundli)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HubCard(Icons.Filled.Favorite, "Stories", Modifier.weight(1f), onClick = onGoStories)
                HubCard(Icons.Filled.Star, "Plans", Modifier.weight(1f), badge = if (!p.isPremium) "FREE" else null, onClick = onGoPricing)
                BadgedBox(badge = { if (unreadNotif > 0) Badge { Text("$unreadNotif") } }, modifier = Modifier.weight(1f)) {
                    HubCard(Icons.Filled.Notifications, "Alerts", Modifier.fillMaxWidth(), onClick = onGoNotifications)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HubCard(Icons.AutoMirrored.Filled.Send, "Interests", Modifier.weight(1f), onClick = onGoInterests)
                HubCard(Icons.Filled.Group, "Family", Modifier.weight(1f), onClick = onGoFamily)
                HubCard(Icons.Filled.Verified, "Verify", Modifier.weight(1f), onClick = onGoVerification)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HubCard(Icons.AutoMirrored.Filled.Help, "Help", Modifier.weight(1f), onClick = onGoHelp)
                HubCard(Icons.Filled.Gavel, "Terms", Modifier.weight(1f), onClick = onGoTerms)
                HubCard(Icons.Filled.PrivacyTip, "Privacy", Modifier.weight(1f), onClick = onGoPrivacy)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HubCard(Icons.Filled.Shield, "Guidelines", Modifier.weight(1f), onClick = onGoGuidelines)
                HubCard(Icons.Filled.Description, "Biodata", Modifier.weight(1f), badge = "FREE", onClick = onGoBiodata)
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            // ── Bio & profile info ────────────────────────────────────────
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(t("about", "About"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (p.bio.isNotBlank()) Text(p.bio, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    InfoRow(Icons.Filled.School,     p.education)
                    InfoRow(Icons.Filled.Work,       p.profession)
                    InfoRow(Icons.Filled.Height,     "${p.heightCm} cm")
                    InfoRow(Icons.Filled.TempleHindu,p.religion)
                    InfoRow(Icons.Filled.Translate,  p.motherTongue)
                    InfoRow(Icons.Filled.AutoAwesome,"${p.rasi} / ${p.nakshatra}")
                    Spacer(Modifier.height(8.dp))

                    // ── Hobbies ──────────────────────────────────────
                    if (p.hobbies.isNotEmpty()) {
                        Text("Hobbies & Interests",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            p.hobbies.forEach { hobby ->
                                Surface(shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer) {
                                    Text(hobby, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // ── Spoken languages ─────────────────────────────
                    if (p.spokenLanguages.isNotEmpty()) {
                        Text("Spoken Languages",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            p.spokenLanguages.forEach { lang ->
                                Surface(shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Row(Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Translate, null, Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Spacer(Modifier.width(4.dp))
                                        Text(lang, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // ── Video profile ────────────────────────────────
                    if (p.videoUrl.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1565C0).copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayCircle, null, Modifier.size(24.dp),
                                    tint = Color(0xFF1565C0))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Video Profile Available",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0))
                                    Text("Watch a short video to know this person better",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    AssistChip(onClick = {}, label = { Text(if (p.hasQuestionnaire) "✓ Questionnaire complete" else "Questionnaire pending") })
                }
            }

            // ── Lifestyle section ────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Lifestyle", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    InfoRow(Icons.Filled.Restaurant, if (p.diet.isNotBlank()) p.diet else "Not specified")
                    InfoRow(Icons.Filled.SmokeFree, "Smoking: ${p.smoking.ifBlank { "Not specified" }}")
                    InfoRow(Icons.Filled.LocalBar, "Drinking: ${p.drinking.ifBlank { "Not specified" }}")
                    InfoRow(Icons.Filled.CurrencyRupee, "Income: ${p.incomeBand.ifBlank { "Not specified" }}")
                    InfoRow(Icons.Filled.Public, "Residential: ${p.residentialStatus.ifBlank { "Not specified" }}")
                }
            }

            // ── Family section ───────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Family Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    InfoRow(Icons.Filled.Home, "Family: ${p.familyType.ifBlank { "Not specified" }}")
                    InfoRow(Icons.Filled.Man, "Father: ${p.fatherOccupation.ifBlank { "Not specified" }}")
                    InfoRow(Icons.Filled.Woman, "Mother: ${p.motherOccupation.ifBlank { "Not specified" }}")
                    InfoRow(Icons.Filled.People, "Siblings: ${if (p.siblings > 0) "${p.siblings}" else "None"}")
                    InfoRow(Icons.Filled.ChildCare, "Children: ${if (p.hasChildren) "Yes" else "No"}")
                    if (p.gothra.isNotBlank()) InfoRow(Icons.Filled.AccountTree, "Gothra: ${p.gothra}")
                    if (p.subCaste.isNotBlank()) InfoRow(Icons.Filled.Groups, "Sub-caste: ${p.subCaste}")
                }
            }

            // ── Personality & Interests section ──────────────────────────
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Personality & Interests", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (p.personalityType.isNotBlank()) InfoRow(Icons.Filled.Psychology, "MBTI: ${p.personalityType}")
                    if (p.hobbies.isNotEmpty()) {
                        Text("Hobbies", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            p.hobbies.take(5).forEach { h ->
                                SuggestionChip(onClick = {}, label = { Text(h, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                    if (p.spokenLanguages.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Languages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            p.spokenLanguages.take(5).forEach { l ->
                                SuggestionChip(onClick = {}, label = { Text(l, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // ── Photos ───────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Photos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.testTag("btn_add_photo")) {
                    Icon(Icons.Filled.AddAPhoto, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add")
                }
            }
            Spacer(Modifier.height(8.dp))
            if (photos.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp).padding(horizontal = 16.dp).testTag("photos_empty"),
                    contentAlignment = Alignment.Center) {
                    Text("No photos yet — add your first.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(horizontal = 16.dp)
                        .testTag("photos_grid")) {
                    items(photos, key = { it.id }) { photo ->
                        PhotoCell(photo, vm::setPrimary, vm::delete, vm::setPrivacy)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            // ── Account actions ───────────────────────────────────────────
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onGoSettings, Modifier.fillMaxWidth().height(50.dp).testTag("btn_settings")) {
                    Icon(Icons.Filled.Settings, null); Spacer(Modifier.width(8.dp)); Text("Settings")
                }
                OutlinedButton(onClick = vm::signOut, Modifier.fillMaxWidth().height(50.dp).testTag("btn_sign_out"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null); Spacer(Modifier.width(8.dp)); Text("Sign out")
                }
            }
            Spacer(Modifier.height(32.dp))
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.testTag("profile_loading"))
        }
    }
}

@Composable
private fun QuickStat(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HubCard(icon: ImageVector, label: String, modifier: Modifier = Modifier, badge: String? = null, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            badge?.let { Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }}
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    if (text.isBlank()) return
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PhotoCell(
    photo: PhotoEntity,
    onPrimary: (PhotoEntity) -> Unit,
    onDelete: (PhotoEntity) -> Unit,
    onSetPrivacy: (PhotoEntity, String) -> Unit
) {
    var showPrivacyMenu by remember { mutableStateOf(false) }
    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).testTag("photo_${photo.id}")) {
        AsyncImage(model = File(photo.path), contentDescription = "Photo",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Row(Modifier.align(Alignment.TopStart).padding(4.dp)) {
            FilledIconButton(onClick = { onPrimary(photo) }, modifier = Modifier.size(28.dp).testTag("photo_primary_${photo.id}")) {
                Icon(if (photo.isPrimary) Icons.Filled.Star else Icons.Filled.StarBorder, null, Modifier.size(14.dp))
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            FilledIconButton(
                onClick = { showPrivacyMenu = true },
                modifier = Modifier.size(28.dp).testTag("photo_privacy_${photo.id}")
            ) {
                Icon(
                    when (photo.privacy) {
                        "HIDDEN" -> Icons.Filled.VisibilityOff
                        "ACCEPTED_ONLY" -> Icons.Filled.People
                        else -> Icons.Filled.Public
                    },
                    null, Modifier.size(14.dp)
                )
            }
            DropdownMenu(expanded = showPrivacyMenu, onDismissRequest = { showPrivacyMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Public") },
                    leadingIcon = { Icon(Icons.Filled.Public, null, Modifier.size(16.dp)) },
                    onClick = { onSetPrivacy(photo, "PUBLIC"); showPrivacyMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Accepted only") },
                    leadingIcon = { Icon(Icons.Filled.People, null, Modifier.size(16.dp)) },
                    onClick = { onSetPrivacy(photo, "ACCEPTED_ONLY"); showPrivacyMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Hidden") },
                    leadingIcon = { Icon(Icons.Filled.VisibilityOff, null, Modifier.size(16.dp)) },
                    onClick = { onSetPrivacy(photo, "HIDDEN"); showPrivacyMenu = false }
                )
            }
            FilledIconButton(onClick = { onDelete(photo) }, modifier = Modifier.size(28.dp).testTag("photo_delete_${photo.id}")) {
                Icon(Icons.Filled.Delete, null, Modifier.size(14.dp))
            }
        }
    }
}
