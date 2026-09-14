package com.match.app.ui.detail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.match.app.core.analytics.AnalyticsManager
import com.match.app.core.matching.Astrology
import com.match.app.core.matching.CombinedMatcher
import com.match.app.core.matching.Vectors
import com.match.app.data.local.Vec
import com.match.app.data.local.dao.QuestionnaireDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.PhotoEntity
import com.match.app.data.remote.MatchApiProvider
import com.match.app.data.remote.ReportProfileRequest
import com.match.app.data.repo.*
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.UserProfile
import com.match.app.ui.common.ActivityStatusChip
import com.match.app.ui.common.ContactUnlockSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
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
    val privateNote: String = "",
    val meIsPremium: Boolean = false,
    val showContactUnlock: Boolean = false,
    val revealedPhone: String = "",
    val contactsUsed: Int = 0,
    val contactsLimit: Int = 0,
    val contactLoading: Boolean = false,
    val contactError: String? = null,
    val showReportDialog: Boolean = false,
    val reportMessage: String? = null,
    val loading: Boolean = true
)

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
    private val subscriptionRepo: SubscriptionRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(DetailUi())
    val ui: StateFlow<DetailUi> = _ui.asStateFlow()
    private var meId = 0L
    private var targetId = 0L

    fun load(userId: Long) = viewModelScope.launch {
        targetId = userId
        meId = session.userId.first() ?: run {
            _ui.value = _ui.value.copy(loading = false)
            return@launch
        }
        val profile = auth.currentProfile(userId)
        if (profile == null) {
            _ui.value = _ui.value.copy(loading = false)
            return@launch
        }
        val photos = photoRepo.observe(userId).first()
        val meLiked = social.isLiked(meId, userId)
        val theyLiked = social.isLiked(userId, meId)
        val me = auth.currentProfile(meId)
        _ui.value = _ui.value.copy(
            profile = profile,
            photos = photos,
            liked = meLiked,
            blocked = social.isBlocked(meId, userId),
            shortlisted = shortlistRepo.isSaved(meId, userId),
            isMutual = meLiked && theyLiked,
            meIsPremium = me?.isPremium == true,
            loading = false
        )

        launch {
            noteRepo.observe(meId, userId).collect { note ->
                _ui.update { it.copy(privateNote = note?.note.orEmpty()) }
            }
        }

        runCatching { whoViewed.record(meId, userId) }
        analytics.logProfileView(userId)

        val seeker = userDao.findById(meId)
        val target = userDao.findById(userId)
        if (seeker != null && target != null) {
            val seekerQ = qDao.forUser(meId)
            val targetQ = qDao.forUser(userId)
            val qScore = if (seekerQ != null && targetQ != null) {
                Vectors.questionnaireScore(
                    Vec.decode(seekerQ.selfVector), Vec.decode(seekerQ.partnerVector),
                    Vec.decode(targetQ.selfVector), Vec.decode(targetQ.partnerVector)
                )
            } else 0f
            val astro = if (
                ReligionCategory.fromReligion(profile.religion) == ReligionCategory.HINDU &&
                seeker.rasi.isNotBlank() && seeker.nakshatra.isNotBlank() &&
                target.rasi.isNotBlank() && target.nakshatra.isNotBlank()
            ) Astrology.score(seeker.rasi, seeker.nakshatra, target.rasi, target.nakshatra) else 0f
            _ui.update { it.copy(qScore = qScore, astroScore = astro, combinedScore = CombinedMatcher.combine(qScore, astro)) }
        }
    }

    fun toggleLike() = viewModelScope.launch {
        if (_ui.value.blocked) return@launch
        val liked = social.toggleLike(meId, targetId)
        val mutual = liked && social.isLiked(targetId, meId)
        _ui.update { it.copy(liked = liked, isMutual = mutual, contactError = null) }
        if (liked) analytics.logInterestSent(targetId)
    }

    fun toggleShortlist() = viewModelScope.launch {
        val saved = shortlistRepo.toggle(meId, targetId)
        _ui.update { it.copy(shortlisted = saved) }
        analytics.logShortlistToggled(targetId, saved)
    }

    fun toggleBlock() = viewModelScope.launch {
        val wasBlocked = _ui.value.blocked
        if (wasBlocked) social.unblock(meId, targetId) else social.block(meId, targetId)
        _ui.update {
            it.copy(
                blocked = !wasBlocked,
                showContactUnlock = false,
                revealedPhone = if (!wasBlocked) "" else it.revealedPhone
            )
        }
        analytics.logBlockToggled(!wasBlocked)
    }

    fun saveNote(text: String) = viewModelScope.launch {
        noteRepo.save(meId, targetId, text.trim().take(1000))
    }

    fun showContactUnlock() {
        if (!_ui.value.blocked) _ui.update { it.copy(showContactUnlock = true, contactError = null) }
    }

    fun dismissContactUnlock() = _ui.update { it.copy(showContactUnlock = false, contactError = null) }

    fun revealContact() = viewModelScope.launch {
        val targetUid = _ui.value.profile?.firebaseUid.orEmpty()
        if (targetUid.isBlank()) {
            _ui.update { it.copy(contactError = "Contact is unavailable for this profile.") }
            return@launch
        }
        _ui.update { it.copy(contactLoading = true, contactError = null) }
        subscriptionRepo.revealContact(targetUid)
            .onSuccess { result ->
                _ui.update {
                    it.copy(
                        contactLoading = false,
                        revealedPhone = result.phoneNumber,
                        contactsUsed = result.contactsUsed,
                        contactsLimit = result.contactsLimit,
                        contactError = null
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        contactLoading = false,
                        contactError = error.message?.take(180) ?: "Unable to reveal contact right now."
                    )
                }
            }
    }

    fun showReportDialog() = _ui.update { it.copy(showReportDialog = true, reportMessage = null) }
    fun dismissReportDialog() = _ui.update { it.copy(showReportDialog = false) }

    fun submitReport(reason: String) = viewModelScope.launch {
        if (reason.isBlank()) return@launch
        analytics.logProfileReported(reason)
        val outcome = runCatching {
            apiProvider.api().reportProfile(ReportProfileRequest(reporterId = meId, targetId = targetId, reason = reason.take(120)))
        }
        _ui.update {
            it.copy(
                showReportDialog = false,
                reportMessage = if (outcome.isSuccess) "Report submitted. Thank you for helping keep the community safe."
                else "Report could not be submitted. Check your connection and try again."
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    userId: Long,
    onBack: () -> Unit,
    onChat: () -> Unit,
    onPricing: () -> Unit = {},
    vm: MatchDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) { vm.load(userId) }
    val ui by vm.ui.collectAsState()
    val p = ui.profile
    val context = LocalContext.current
    var showNote by remember { mutableStateOf(false) }
    var noteText by remember(ui.privateNote) { mutableStateOf(ui.privateNote) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.reportMessage) {
        ui.reportMessage?.let { snackbar.showSnackbar(it) }
    }

    if (ui.showContactUnlock && p != null) {
        ContactUnlockSheet(
            matchName = p.displayName,
            isPremium = ui.meIsPremium,
            isMutual = ui.isMutual,
            revealedPhone = ui.revealedPhone,
            contactsUsed = ui.contactsUsed,
            contactsLimit = ui.contactsLimit,
            isLoading = ui.contactLoading,
            errorMessage = ui.contactError,
            onReveal = vm::revealContact,
            onUpgrade = onPricing,
            onMessage = onChat,
            onDismiss = vm::dismissContactUnlock
        )
    }

    if (showNote) {
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text("Private note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it.take(1000) },
                    label = { Text("Only you can see this") },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { vm.saveNote(noteText); showNote = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showNote = false }) { Text("Cancel") } }
        )
    }

    if (ui.showReportDialog) {
        val reasons = listOf("Fake profile", "Inappropriate content", "Harassment", "Spam or scam", "Under age", "Other")
        var selected by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = vm::dismissReportDialog,
            title = { Text("Report profile") },
            text = {
                Column {
                    Text("Choose the reason that best describes the issue.")
                    reasons.forEach { reason ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected == reason, onClick = { selected = reason })
                            Text(reason)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = selected != null, onClick = { selected?.let(vm::submitReport) }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = vm::dismissReportDialog) { Text("Cancel") } }
        )
    }

    fun shareProfile() {
        val profile = p ?: return
        val text = buildString {
            append("${profile.displayName}, ${profile.age}\n")
            append(listOf(profile.city, profile.religion, profile.profession).filter { it.isNotBlank() }.joinToString(" • "))
            append("\nmatrimonyconnect://match/${profile.id}")
        }
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Share profile"))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(p?.displayName ?: "Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { showNote = true }) { Icon(Icons.Filled.EditNote, "Private note") }
                    IconButton(onClick = vm::toggleShortlist) {
                        Icon(if (ui.shortlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Shortlist")
                    }
                    IconButton(onClick = ::shareProfile) { Icon(Icons.Filled.Share, "Share") }
                    IconButton(onClick = vm::showReportDialog) { Icon(Icons.Filled.Flag, "Report") }
                    IconButton(onClick = vm::toggleBlock) {
                        Icon(Icons.Filled.Block, if (ui.blocked) "Unblock" else "Block", tint = if (ui.blocked) MaterialTheme.colorScheme.error else LocalContentColor.current)
                    }
                }
            )
        },
        bottomBar = {
            if (p != null && !ui.blocked) {
                Surface(shadowElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = vm::showContactUnlock, modifier = Modifier.weight(1f).height(50.dp)) {
                            Icon(Icons.Filled.Phone, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Contact")
                        }
                        Button(
                            onClick = { if (!ui.liked) vm.toggleLike(); onChat() },
                            modifier = Modifier.weight(1.4f).height(50.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (ui.isMutual) "Message" else "Interest & chat")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            ui.loading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            p == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { Text("This profile is unavailable.") }
            else -> MatchDetailContent(p, ui, Modifier.padding(padding))
        }
    }
}

@Composable
private fun MatchDetailContent(p: UserProfile, ui: DetailUi, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("detail_content"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfilePhoto(p, ui.photos, ui.isMutual)

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${p.displayName}, ${p.age}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (p.isVerified) Icon(Icons.Filled.Verified, "Verified", tint = MaterialTheme.colorScheme.primary)
                }
                Text(listOf(p.city, p.state, p.profession).filter { it.isNotBlank() }.joinToString(" • "))
                Text("${p.religion}${p.caste.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (p.showLastActive) ActivityStatusChip(lastActiveAt = p.lastActiveAt)
                if (p.bio.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Text(p.bio)
                }
            }
        }

        DetailSection("Personal & community") {
            DetailRow("Marital status", p.maritalStatus)
            DetailRow("Mother tongue", p.motherTongue)
            DetailRow("Height", if (p.heightCm > 0) "${p.heightCm} cm" else "")
            DetailRow("Religion", p.religion)
            DetailRow("Community", p.caste)
            DetailRow("Sub-community", p.subCaste)
            if (ReligionCategory.fromReligion(p.religion) == ReligionCategory.HINDU) DetailRow("Gothra", p.gothra)
        }

        if (ReligionCategory.fromReligion(p.religion) == ReligionCategory.HINDU && p.showHoroscope) {
            DetailSection("Birth & astrology") {
                DetailRow("Rasi", p.rasi)
                DetailRow("Nakshatra", p.nakshatra)
                DetailRow("Manglik / Dosham", p.manglik)
                DetailRow("Birth place", p.birthPlace)
            }
        }

        DetailSection("Education & career") {
            DetailRow("Education", p.education)
            DetailRow("Field", p.educationField)
            DetailRow("Institution", p.institution)
            DetailRow("Profession", p.profession)
            DetailRow("Occupation", p.occupationCategory)
            DetailRow("Employer", p.employer)
            if (p.incomeDisclosure != "hidden") DetailRow("Income", p.incomeBand)
        }

        DetailSection("Family & lifestyle") {
            DetailRow("Family type", p.familyType)
            DetailRow("Family status", p.familyStatus)
            DetailRow("Family values", p.familyValues)
            DetailRow("Diet", p.diet)
            DetailRow("Smoking", p.smoking)
            DetailRow("Drinking", p.drinking)
            if (p.aboutFamily.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(p.aboutFamily, style = MaterialTheme.typography.bodyMedium)
            }
        }

        DetailSection("Compatibility") {
            ScoreRow("Questionnaire", ui.qScore)
            if (ReligionCategory.fromReligion(p.religion) == ReligionCategory.HINDU && p.showHoroscope) {
                ScoreRow("Astrology", ui.astroScore)
            }
            ScoreRow("Combined", ui.combinedScore, bold = true)
            Text(
                "Compatibility scores are guidance signals, not guarantees.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (ui.blocked) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text("You blocked this member. Messaging and contact access are disabled until you unblock them.", Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ProfilePhoto(p: UserProfile, photos: List<PhotoEntity>, isMutual: Boolean) {
    val primary = photos.firstOrNull { it.isPrimary } ?: photos.firstOrNull()
    val canUseLocal = primary != null && primary.privacy != "HIDDEN" && (primary.privacy == "PUBLIC" || (primary.privacy == "ACCEPTED_ONLY" && isMutual))
    val model: Any? = when {
        p.photoUrl.isNotBlank() -> p.photoUrl
        canUseLocal -> File(primary!!.path)
        else -> null
    }
    Surface(Modifier.fillMaxWidth().height(300.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        if (model != null) {
            AsyncImage(model = model, contentDescription = "Profile photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(p.displayName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ScoreRow(label: String, score: Float, bold: Boolean = false) {
    val safe = score.coerceIn(0f, 1f)
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        LinearProgressIndicator(progress = { safe }, modifier = Modifier.width(100.dp).height(7.dp))
        Spacer(Modifier.width(8.dp))
        Text("${(safe * 100).toInt()}%", fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}
