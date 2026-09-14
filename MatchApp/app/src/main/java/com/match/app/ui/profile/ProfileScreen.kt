package com.match.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.UserProfile
import com.match.app.ui.common.ProfileCompletenessBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    val likeCount: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { likeDao.observeIncomingCount(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val savedCount: StateFlow<Int> = session.userId.filterNotNull()
        .flatMapLatest { shortlistDao.observeCount(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun importPhoto(uri: Uri) = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        photoRepo.import(uid, uri)
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

    val p = profile
    if (p == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.testTag("profile_loading"))
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileHero(p)

        ProfileCompletenessBar(
            profile = p,
            onComplete = onGoSettings,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        ReligionExperienceCard(profileReligion = p.religion)

        ProfileStats(p.profileViewCount, likeCount, savedCount, onGoWhoViewed, onGoInterests, onGoShortlists)

        TrustAndVerificationCard(p, photos.isNotEmpty(), onGoVerification)

        ProfileSection("Personal details") {
            InfoRow(Icons.Filled.Person, "${p.age} years • ${p.maritalStatus}")
            InfoRow(Icons.Filled.Height, if (p.heightCm > 0) "${p.heightCm} cm" else "")
            InfoRow(Icons.Filled.Public, listOf(p.city, p.state, p.countryOfResidence).filter { it.isNotBlank() }.joinToString(", "))
            InfoRow(Icons.Filled.Translate, p.motherTongue)
        }

        ProfileSection("Religion & community") {
            InfoRow(Icons.Filled.Public, p.religion)
            InfoRow(Icons.Filled.Groups, p.caste)
            InfoRow(Icons.Filled.People, p.subCaste)
            if (ReligionCategory.fromReligion(p.religion) == ReligionCategory.HINDU) {
                InfoRow(Icons.Filled.AccountTree, p.gothra)
            }
        }

        if (ReligionCategory.fromReligion(p.religion) == ReligionCategory.HINDU) {
            ProfileSection("Birth & astrology") {
                InfoRow(Icons.Filled.Star, listOf(p.rasi, p.nakshatra).filter { it.isNotBlank() }.joinToString(" • "))
                InfoRow(Icons.Filled.Description, p.dateOfBirth)
                InfoRow(Icons.Filled.Public, p.birthPlace)
                if (p.birthTime.isNotBlank()) InfoRow(Icons.Filled.Star, "Birth time: ${p.birthTime}")
                if (p.manglik.isNotBlank()) InfoRow(Icons.Filled.Star, "Manglik/Dosham: ${p.manglik}")
                OutlinedButton(onClick = onGoKundli, modifier = Modifier.fillMaxWidth()) { Text("Open Kundali matching") }
            }
        }

        ProfileSection("Education & career") {
            InfoRow(Icons.Filled.School, listOf(p.education, p.educationField, p.institution).filter { it.isNotBlank() }.joinToString(" • "))
            InfoRow(Icons.Filled.BusinessCenter, listOf(p.profession, p.occupationCategory, p.employer).filter { it.isNotBlank() }.joinToString(" • "))
            if (p.incomeBand.isNotBlank()) InfoRow(Icons.Filled.BusinessCenter, "Income: ${p.incomeBand}")
        }

        ProfileSection("Family & lifestyle") {
            InfoRow(Icons.Filled.Home, listOf(p.familyType, p.familyStatus, p.familyValues).filter { it.isNotBlank() }.joinToString(" • "))
            InfoRow(Icons.Filled.FamilyRestroom, p.aboutFamily)
            InfoRow(Icons.Filled.Restaurant, p.diet)
            if (p.hobbies.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    p.hobbies.take(8).forEach { hobby -> AssistChip(onClick = {}, label = { Text(hobby) }) }
                }
            }
        }

        if (p.bio.isNotBlank()) {
            ProfileSection("About me") { Text(p.bio, style = MaterialTheme.typography.bodyMedium) }
        }

        PhotosSection(photos, picker = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, vm = vm)

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onGoPrivacy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Filled.PrivacyTip, null)
                Spacer(Modifier.size(8.dp))
                Text("Privacy & visibility")
            }
            OutlinedButton(onClick = onGoBiodata, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Filled.Description, null)
                Spacer(Modifier.size(8.dp))
                Text("View biodata")
            }
            OutlinedButton(onClick = onGoSettings, modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_settings")) {
                Icon(Icons.Filled.Settings, null)
                Spacer(Modifier.size(8.dp))
                Text("Settings")
            }
            OutlinedButton(
                onClick = vm::signOut,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_sign_out"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                Spacer(Modifier.size(8.dp))
                Text("Sign out")
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ProfileHero(p: UserProfile) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(84.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(p.displayName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (p.isVerified) {
                    Spacer(Modifier.size(5.dp))
                    Icon(Icons.Filled.Verified, "Verified", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("${p.age} • ${p.city} • ${p.profession}", style = MaterialTheme.typography.bodyMedium)
            Text("${p.religion}${p.caste.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(p.matrimonyId.ifBlank { "Profile ID: M${p.id}" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ProfileStats(views: Int, likes: Int, saved: Int, onViews: () -> Unit, onLikes: () -> Unit, onSaved: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(Icons.Filled.Visibility, "Views", views.toString(), Modifier.weight(1f), onViews)
        StatCard(Icons.Filled.Favorite, "Interests", likes.toString(), Modifier.weight(1f), onLikes)
        StatCard(Icons.Filled.Bookmark, "Saved", saved.toString(), Modifier.weight(1f), onSaved)
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TrustAndVerificationCard(p: UserProfile, hasPhoto: Boolean, onVerify: () -> Unit) {
    val checks = listOf(
        "Profile photo" to hasPhoto,
        "Profile details" to (p.bio.isNotBlank() && p.city.isNotBlank()),
        "Phone/identity baseline" to (p.verificationLevel >= 1),
        "Identity verified" to p.isVerified
    )
    val complete = checks.count { it.second }
    val progress = complete / checks.size.toFloat()
    ElevatedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Trust & verification", fontWeight = FontWeight.Bold)
                    Text("Only completed server-backed checks count here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("$complete/${checks.size}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            checks.forEach { (label, done) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, Modifier.size(16.dp), tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.size(7.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!p.isVerified) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onVerify, modifier = Modifier.fillMaxWidth()) { Text("Continue verification") }
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    if (text.isBlank()) return
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PhotosSection(photos: List<PhotoEntity>, picker: () -> Unit, vm: ProfileViewModel) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Photos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = picker, modifier = Modifier.testTag("btn_add_photo")) {
                Icon(Icons.Filled.AddAPhoto, null, Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("Add")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (photos.isEmpty()) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PhotoCamera, null)
                    Text("Add clear recent photos to improve trust.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 330.dp).testTag("photos_grid")
            ) {
                items(photos, key = { it.id }) { photo ->
                    PhotoCell(photo, vm::setPrimary, vm::delete, vm::setPrivacy)
                }
            }
        }
    }
}

@Composable
private fun PhotoCell(photo: PhotoEntity, onPrimary: (PhotoEntity) -> Unit, onDelete: (PhotoEntity) -> Unit, onSetPrivacy: (PhotoEntity, String) -> Unit) {
    var showPrivacyMenu by remember { mutableStateOf(false) }
    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).testTag("photo_${photo.id}")) {
        AsyncImage(model = File(photo.path), contentDescription = "Profile photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        FilledIconButton(onClick = { onPrimary(photo) }, modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(28.dp)) {
            Icon(if (photo.isPrimary) Icons.Filled.Star else Icons.Filled.StarBorder, "Set primary", Modifier.size(14.dp))
        }
        Row(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
            FilledIconButton(onClick = { showPrivacyMenu = true }, modifier = Modifier.size(28.dp)) {
                Icon(when (photo.privacy) { "HIDDEN" -> Icons.Filled.VisibilityOff; "ACCEPTED_ONLY" -> Icons.Filled.People; else -> Icons.Filled.Public }, "Photo privacy", Modifier.size(14.dp))
            }
            DropdownMenu(expanded = showPrivacyMenu, onDismissRequest = { showPrivacyMenu = false }) {
                DropdownMenuItem(text = { Text("Public") }, leadingIcon = { Icon(Icons.Filled.Public, null) }, onClick = { onSetPrivacy(photo, "PUBLIC"); showPrivacyMenu = false })
                DropdownMenuItem(text = { Text("Accepted only") }, leadingIcon = { Icon(Icons.Filled.People, null) }, onClick = { onSetPrivacy(photo, "ACCEPTED_ONLY"); showPrivacyMenu = false })
                DropdownMenuItem(text = { Text("Hidden") }, leadingIcon = { Icon(Icons.Filled.VisibilityOff, null) }, onClick = { onSetPrivacy(photo, "HIDDEN"); showPrivacyMenu = false })
            }
            FilledIconButton(onClick = { onDelete(photo) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, "Delete photo", Modifier.size(14.dp))
            }
        }
    }
}
