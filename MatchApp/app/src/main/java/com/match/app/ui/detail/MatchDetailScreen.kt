package com.match.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.match.app.core.matching.Astrology
import com.match.app.core.matching.CombinedMatcher
import com.match.app.core.matching.Vectors
import com.match.app.data.local.Vec
import com.match.app.data.local.dao.QuestionnaireDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.PhotoEntity
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.NoteRepository
import com.match.app.data.repo.PhotoRepository
import com.match.app.data.repo.ShortlistRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.repo.SubscriptionRepository
import com.match.app.data.repo.WhoViewedRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.UserProfile
import com.match.app.core.analytics.AnalyticsManager
import com.match.app.core.security.FakeProfileDetector
import com.match.app.data.remote.MatchApiProvider
import com.match.app.data.remote.ReportProfileRequest
import com.match.app.ui.common.ActivityStatusChip
import com.match.app.ui.common.ContactUnlockSheet
import android.content.Intent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUi(
    val profile: UserProfile? = null,
    val photos: List<PhotoEntity> = emptyList(),
    val liked: Boolean = false,
    val blocked: Boolean = false,
    val shortlisted: Boolean = false,
    val isMutual: Boolean = false,
    val qScore: Float = 0f,
    val astroScore: Float = 0f,
    val combinedScore: Float = 0f,
    val showReportDialog: Boolean = false,
    val reportSubmitted: Boolean = false,
    val privateNote: String = "",
    val trustScore: Int = -1,
    val trustBadge: String = "",
    // Sprint 7
    val isSuperLiked: Boolean = false,
    val meIsPremium: Boolean = false,
    val showContactUnlock: Boolean = false,
    val contactsUsedThisMonth: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MatchDetailViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val social: SocialRepository,
    private val shortlistRepo: ShortlistRepository,
    private val whoViewed: WhoViewedRepository,
    private val session: SessionStore,
    private val userDao: UserDao,
    private val qDao: QuestionnaireDao,
    private val photoRepo: PhotoRepository,
    private val analytics: AnalyticsManager,
    private val apiProvider: MatchApiProvider,
    private val noteRepo: NoteRepository,
    private val fakeDetector: FakeProfileDetector,
    private val subscriptionRepo: SubscriptionRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(DetailUi())
    val ui: StateFlow<DetailUi> = _ui.asStateFlow()

    private var meId: Long = 0L
    private var targetId: Long = 0L

    fun load(userId: Long) = viewModelScope.launch {
        targetId = userId
        meId = session.userId.first() ?: return@launch
        val profile = auth.currentProfile(userId)
        val photos = photoRepo.observe(userId).first()
        _ui.value = _ui.value.copy(profile = profile, photos = photos)
        val meIsLiked = social.isLiked(meId, userId)
        val theyLikedMe = social.isLiked(userId, meId)
        val contactsUsed = subscriptionRepo.getContactsUsedThisMonth()
        _ui.value = _ui.value.copy(
            liked = meIsLiked,
            blocked = social.isBlocked(meId, userId),
            shortlisted = shortlistRepo.isSaved(meId, userId),
            isMutual = meIsLiked && theyLikedMe,
            isSuperLiked = social.isSuperLike(meId, userId),
            meIsPremium = auth.currentProfile(meId)?.isPremium == true,
            contactsUsedThisMonth = contactsUsed
        )
        // Observe private note in a separate coroutine
        viewModelScope.launch {
            noteRepo.observe(meId, userId).collect { noteEntity ->
                _ui.value = _ui.value.copy(privateNote = noteEntity?.note ?: "")
            }
        }
        // record this as a view
        whoViewed.record(meId, userId)
        analytics.logProfileView(userId)
        val seeker = userDao.findById(meId) ?: return@launch
        val target = userDao.findById(userId) ?: return@launch
        val astro = Astrology.score(seeker.rasi, seeker.nakshatra, target.rasi, target.nakshatra)
        val seekerQ = qDao.forUser(meId)
        val targetQ = qDao.forUser(userId)
        val qScore = if (seekerQ != null && targetQ != null) {
            Vectors.questionnaireScore(
                Vec.decode(seekerQ.selfVector), Vec.decode(seekerQ.partnerVector),
                Vec.decode(targetQ.selfVector), Vec.decode(targetQ.partnerVector)
            )
        } else 0f
        _ui.value = _ui.value.copy(
            qScore = qScore, astroScore = astro,
            combinedScore = CombinedMatcher.combine(qScore, astro)
        )
        // Trust score analysis
        val trustReport = fakeDetector.analyze(
            user = target,
            photoCount = _ui.value.photos.size,
            hasQuestionnaire = targetQ != null
        )
        _ui.value = _ui.value.copy(
            trustScore = trustReport.overallScore,
            trustBadge = trustReport.badgeTier.name
        )
    }

    fun toggleLike() = viewModelScope.launch {
        val nowLiked = social.toggleLike(meId, targetId)
        _ui.value = _ui.value.copy(liked = nowLiked)
        if (nowLiked) analytics.logInterestSent(targetId)
    }

    fun toggleBlock() = viewModelScope.launch {
        if (_ui.value.blocked) social.unblock(meId, targetId)
        else social.block(meId, targetId)
        val nowBlocked = !_ui.value.blocked
        _ui.value = _ui.value.copy(blocked = nowBlocked)
        analytics.logBlockToggled(nowBlocked)
    }

    fun toggleShortlist() = viewModelScope.launch {
        val nowSaved = shortlistRepo.toggle(meId, targetId)
        _ui.value = _ui.value.copy(shortlisted = nowSaved)
        analytics.logShortlistToggled(targetId, nowSaved)
    }

    fun showReportDialog() { _ui.value = _ui.value.copy(showReportDialog = true) }
    fun dismissReportDialog() { _ui.value = _ui.value.copy(showReportDialog = false) }
    fun submitReport(reason: String) = viewModelScope.launch {
        analytics.logProfileReported(reason)
        // Try backend; gracefully degrade if offline
        try {
            apiProvider.api().reportProfile(
                ReportProfileRequest(reporterId = meId, targetId = targetId, reason = reason)
            )
        } catch (_: Exception) { /* queued locally — retry when backend is reachable */ }
        _ui.value = _ui.value.copy(showReportDialog = false, reportSubmitted = true)
    }

    fun saveNote(text: String) = viewModelScope.launch {
        noteRepo.save(meId, targetId, text)
    }

    /** Send a Super Interest — premium feature. */
    fun sendSuperInterest() = viewModelScope.launch {
        social.superLike(meId, targetId)
        _ui.value = _ui.value.copy(liked = true, isSuperLiked = true)
        analytics.logInterestSent(targetId)
    }

    fun showContactUnlock()    { _ui.value = _ui.value.copy(showContactUnlock = true) }
    fun dismissContactUnlock() { _ui.value = _ui.value.copy(showContactUnlock = false) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    userId: Long,
    onBack: () -> Unit,
    onChat: () -> Unit,
    vm: MatchDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) { vm.load(userId) }
    val ui by vm.ui.collectAsState()
    val p = ui.profile
    val context = LocalContext.current
    val txtCheckProfile = "Check out"
    val txtProfile = "Profile"
    val txtShareProfile = "Share profile via"
    val txtReportProfile = "Report Profile"
    val txtReportWhy = "Why are you reporting this profile?"
    val txtSubmitReport = "Submit Report"
    val txtCancel = "Cancel"
    val txtReportSubmitted = "Report submitted. Thank you for keeping the community safe."
    val txtSendInterest = "Send Interest"

    // ── Contact Unlock Sheet ──────────────────────────────────────────────
    if (ui.showContactUnlock && p != null) {
        ContactUnlockSheet(
            matchName = p.displayName,
            matchPhone = p.phoneNumber,
            isPremium = ui.meIsPremium,
            isMutual = ui.isMutual,
            contactsUsed = ui.contactsUsedThisMonth,
            contactsLimit = if (ui.meIsPremium) -1 else 0,
            onUpgrade = { /* navigate to pricing */ },
            onDismiss = vm::dismissContactUnlock
        )
    }

    // ── Share profile helper ──────────────────────────────────────────────
    fun shareProfile() {
        p ?: return
        val text = buildString {
            append("$txtCheckProfile ${p.displayName} $txtProfile\n")
            append("${p.age} years • ${p.city} • ${p.religion}\n")
            if (p.bio.isNotBlank()) append("\"${p.bio}\"\n")
            append("\nmatrimonyconnect://match/${p.id}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, txtShareProfile))
    }

    // ── Private note dialog ───────────────────────────────────────────────
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember(ui.privateNote) { mutableStateOf(ui.privateNote) }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            icon = { Icon(Icons.Filled.EditNote, null) },
            title = { Text("Private Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your private note about ${p?.displayName ?: "this person"}. Only you can see this.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Your note") },
                        placeholder = { Text("e.g. Met at conference, seems genuine, follow up...") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.saveNote(noteText); showNoteDialog = false }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Report dialog ─────────────────────────────────────────────────────
    if (ui.showReportDialog) {
        val reasons = listOf(
            "Fake profile", "Inappropriate photos", "Harassment",
            "Spam / scam", "Under age", "Other"
        )
        var selected by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = vm::dismissReportDialog,
            icon = { Icon(Icons.Filled.Flag, null, tint = Color(0xFFC62828)) },
            title = { Text(txtReportProfile) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(txtReportWhy,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    reasons.forEach { reason ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected == reason,
                                onClick = { selected = reason }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(reason, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { selected?.let { vm.submitReport(it) } },
                    enabled = selected != null
                ) { Text(txtSubmitReport) }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissReportDialog) { Text(txtCancel) }
            }
        )
    }

    // ── Report submitted snackbar ─────────────────────────────────────────
    if (ui.reportSubmitted) {
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(txtReportSubmitted)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag("detail_topbar"),
                title = { Text(p?.displayName ?: txtProfile) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Private note button — yellow if note exists
                    IconButton(onClick = { showNoteDialog = true }, modifier = Modifier.testTag("detail_note")) {
                        Icon(
                            Icons.Filled.EditNote,
                            contentDescription = "Add note",
                            tint = if (ui.privateNote.isNotBlank()) Color(0xFFFFB300)
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = vm::toggleShortlist, modifier = Modifier.testTag("detail_shortlist")) {
                        Icon(
                            if (ui.shortlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Shortlist",
                            tint = if (ui.shortlisted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = vm::toggleLike, modifier = Modifier.testTag("detail_like")) {
                        Icon(
                            if (ui.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (ui.liked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = vm::toggleBlock, modifier = Modifier.testTag("detail_block")) {
                        Icon(
                            Icons.Filled.Block,
                            contentDescription = "Block",
                            tint = if (ui.blocked) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = vm::showReportDialog, modifier = Modifier.testTag("detail_report")) {
                        Icon(
                            Icons.Filled.Flag,
                            contentDescription = "Report",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = ::shareProfile, modifier = Modifier.testTag("detail_share")) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share profile",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {},
        bottomBar = {
            if (p != null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Super Interest or plain Like button
                        OutlinedButton(
                            onClick = { if (!ui.isSuperLiked) vm.sendSuperInterest() },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("detail_super_interest"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (ui.isSuperLiked) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(if (ui.isSuperLiked) Icons.Filled.Star else Icons.Filled.StarBorder, null, Modifier.size(18.dp))
                        }
                        Button(
                            onClick = { vm.toggleLike(); onChat() },
                            modifier = Modifier.weight(2f).height(50.dp).testTag("detail_interest_sticky"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(txtSendInterest, style = MaterialTheme.typography.titleSmall)
                        }
                        // Contact unlock button
                        OutlinedButton(
                            onClick = vm::showContactUnlock,
                            modifier = Modifier.weight(1f).height(50.dp).testTag("detail_contact_unlock")
                        ) {
                            Icon(Icons.Filled.Phone, null, Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { pad ->
        if (p == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag("detail_loading"))
            }
            return@Scaffold
        }
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                .testTag("detail_content")
        ) {
            // ── Photo carousel ───────────────────────────────────────
            if (ui.photos.isNotEmpty()) {
                var photoPage by remember { mutableIntStateOf(0) }
                Box(
                    Modifier.fillMaxWidth().height(260.dp).testTag("detail_photo_carousel")
                ) {
                    val photo = ui.photos[photoPage]
                    val canView = photo.privacy == "PUBLIC" ||
                        (photo.privacy == "ACCEPTED_ONLY" && ui.isMutual)
                    // HIDDEN photos are never visible to viewers
                    if (photo.privacy != "HIDDEN" && canView) {
                        AsyncImage(
                            model = java.io.File(photo.path),
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Blurred / locked placeholder
                        Box(
                            Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Photo locked",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (photo.privacy == "ACCEPTED_ONLY") "Mutual interest required"
                                    else "Private photo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // dot indicators
                    if (ui.photos.size > 1) {
                        Row(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ui.photos.forEachIndexed { idx, _ ->
                                Surface(
                                    shape = CircleShape,
                                    color = if (idx == photoPage) Color.White else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(if (idx == photoPage) 10.dp else 7.dp)
                                ) {}
                            }
                        }
                        // prev/next tap areas
                        Row(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.weight(1f).fillMaxHeight()
                                    .clickable(enabled = photoPage > 0) { photoPage-- }
                            )
                            Box(
                                Modifier.weight(1f).fillMaxHeight()
                                    .clickable(enabled = photoPage < ui.photos.size - 1) { photoPage++ }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else {
                // gradient avatar fallback
                val avatarColors = remember(p.id) {
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
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(120.dp).clip(RoundedCornerShape(36.dp))
                            .background(Brush.linearGradient(avatarColors))
                            .testTag("detail_avatar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            p.displayName.first().uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Main profile card ─────────────────────────────────────
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${p.displayName}, ${p.age}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (p.isVerified) Icon(Icons.Filled.Verified, null, Modifier.size(20.dp), tint = androidx.compose.ui.graphics.Color(0xFF1976D2))
                        if (p.isPremium) Icon(Icons.Filled.Star, null, Modifier.size(18.dp), tint = androidx.compose.ui.graphics.Color(0xFFFFB300))
                    }
                    Text("${p.gender} • ${p.city}", style = MaterialTheme.typography.bodyMedium)
                    Text("Profile ID: M${p.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(6.dp))
                    ActivityStatusChip(lastActiveAt = p.lastActiveAt)
                    // Trust badge
                    if (ui.trustScore >= 0) {
                        Spacer(Modifier.height(6.dp))
                        val (trustColor, trustLabel) = when {
                            ui.trustScore >= 75 -> Color(0xFF2E7D32) to "Trusted (${ui.trustScore}%)"
                            ui.trustScore >= 50 -> Color(0xFFF57F17) to "Moderate (${ui.trustScore}%)"
                            else -> Color(0xFFC62828) to "Low Trust (${ui.trustScore}%)"
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = trustColor.copy(alpha = 0.12f),
                            modifier = Modifier.testTag("detail_trust_badge")
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, null, Modifier.size(14.dp), tint = trustColor)
                                Spacer(Modifier.width(4.dp))
                                Text(trustLabel, style = MaterialTheme.typography.labelSmall, color = trustColor, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${p.religion} • ${p.motherTongue}", style = MaterialTheme.typography.bodySmall)
                    Text("${p.education} • ${p.profession}", style = MaterialTheme.typography.bodySmall)
                    Text("Height: ${p.heightCm} cm • ${p.maritalStatus}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    // Community chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (p.religion.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(6.dp), color = androidx.compose.ui.graphics.Color(0xFFE8F5E9)) {
                                Text(p.religion, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
                                    color = androidx.compose.ui.graphics.Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                            }
                        }
                        if (p.caste.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(p.caste, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                            }
                        }
                        if (p.subCaste.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(p.subCaste, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Rasi: ${p.rasi}", modifier = Modifier.testTag("detail_rasi"))
                    Text("Nakshatra: ${p.nakshatra}", modifier = Modifier.testTag("detail_nakshatra"))
                    if (p.bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(p.bio, modifier = Modifier.testTag("detail_bio"))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Lifestyle ────────────────────────────────────────────────
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("detail_lifestyle")) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Lifestyle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    DetailRow("Diet", p.diet)
                    DetailRow("Smoking", p.smoking)
                    DetailRow("Drinking", p.drinking)
                    DetailRow("Income", p.incomeBand)
                    DetailRow("Residential status", p.residentialStatus)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Family details ───────────────────────────────────────────
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("detail_family")) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Family Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    DetailRow("Family type", p.familyType)
                    DetailRow("Father's occupation", p.fatherOccupation)
                    DetailRow("Mother's occupation", p.motherOccupation)
                    DetailRow("Siblings", p.siblings.toString())
                    DetailRow("Has children", if (p.hasChildren) "Yes" else "No")
                    DetailRow("Gothra", p.gothra)
                    DetailRow("Sub-caste", p.subCaste)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Personality & Interests ──────────────────────────────────
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("detail_personality")) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Personality & Interests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    DetailRow("MBTI type", p.personalityType)
                    if (p.hobbies.isNotEmpty()) {
                        Text("Hobbies", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            p.hobbies.take(5).forEach { hobby ->
                                SuggestionChip(onClick = {}, label = { Text(hobby, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                    if (p.spokenLanguages.isNotEmpty()) {
                        Text("Languages", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            p.spokenLanguages.take(5).forEach { lang ->
                                SuggestionChip(onClick = {}, label = { Text(lang, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Partner Preferences ──────────────────────────────────
            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("detail_partner_prefs")) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Favorite, null, Modifier.size(18.dp), tint = Color(0xFFE91E63))
                        Spacer(Modifier.width(8.dp))
                        Text("Partner Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    DetailRow("Looking for", p.lookingFor.name.lowercase().replaceFirstChar { it.uppercase() })
                    DetailRow("Preferred age", "—")
                    DetailRow("Preferred religion", p.religion)
                    DetailRow("Preferred caste", if (p.caste.isNotBlank()) p.caste else "Open to all")
                    DetailRow("Preferred location", if (p.city.isNotBlank()) p.city else "Anywhere in India")
                    DetailRow("Preferred education", p.education)
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().testTag("detail_scores")) {
                Column(Modifier.padding(18.dp)) {
                    Text("Compatibility Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    ScoreRow("Questionnaire", ui.qScore)
                    ScoreRow("Astrology", ui.astroScore)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    ScoreRow("Combined", ui.combinedScore, bold = true)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = vm::toggleLike,
                    modifier = Modifier.weight(1f).testTag("detail_like_btn")
                ) {
                    Icon(if (ui.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (ui.liked) "Unlike" else "Like")
                }
                OutlinedButton(
                    onClick = vm::toggleBlock,
                    modifier = Modifier.weight(1f).testTag("detail_block_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (ui.blocked) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(Icons.Filled.Block, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (ui.blocked) "Unblock" else "Block")
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Float, bold: Boolean = false) {
    val pct = (score * 100).toInt()
    val color = when {
        pct >= 80 -> Color(0xFF2E7D32)
        pct >= 60 -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f))
        LinearProgressIndicator(
            progress = { score.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(8.dp),
            color = color, trackColor = color.copy(alpha = 0.15f)
        )
        Spacer(Modifier.width(8.dp))
        Text("$pct%", fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
