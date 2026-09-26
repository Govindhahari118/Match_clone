package com.match.app.ui.detail

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
import androidx.compose.ui.layout.ContentScale
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
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.NoteRepository
import com.match.app.data.repo.PhotoRepository
import com.match.app.data.repo.ShortlistRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.repo.SubscriptionRepository
import com.match.app.data.repo.SupportRepository
import com.match.app.data.repo.WhoViewedRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.UserProfile
import com.match.app.ui.common.ContactUnlockSheet
import com.match.app.ui.components.MatreeProfileHeader
import com.match.app.ui.components.MatreeProfileSection
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
    val reportSubmitting: Boolean = false,
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
    private val noteRepo: NoteRepository,
    private val subscriptionRepo: SubscriptionRepository,
    private val supportRepo: SupportRepository
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
        if (profile == null || userId == meId) {
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
                profile.showHoroscope && ReligionCategory.fromReligion(profile.religion) == ReligionCategory.HINDU &&
                seeker.rasi.isNotBlank() && seeker.nakshatra.isNotBlank() &&
                target.rasi.isNotBlank() && target.nakshatra.isNotBlank()
            ) {
                Astrology.score(seeker.rasi, seeker.nakshatra, target.rasi, target.nakshatra)
            } else 0f
            _ui.update {
                it.copy(
                    qScore = qScore,
                    astroScore = astro,
                    combinedScore = CombinedMatcher.combine(qScore, astro)
                )
            }
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
        if (_ui.value.blocked) return@launch
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
                liked = if (!wasBlocked) false else it.liked,
                isMutual = if (!wasBlocked) false else it.isMutual,
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
        if (!_ui.value.blocked && _ui.value.isMutual) {
            _ui.update { it.copy(showContactUnlock = true, contactError = null) }
        }
    }

    fun dismissContactUnlock() = _ui.update { it.copy(showContactUnlock = false, contactError = null) }

    fun revealContact() = viewModelScope.launch {
        val targetUid = _ui.value.profile?.firebaseUid.orEmpty()
        if (!_ui.value.isMutual || targetUid.isBlank()) {
            _ui.update { it.copy(contactError = "Contact reveal is available only for a valid mutual match.") }
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
    fun consumeReportMessage() = _ui.update { it.copy(reportMessage = null) }

    fun submitReport(reason: String) = viewModelScope.launch {
        val targetUid = _ui.value.profile?.firebaseUid.orEmpty()
        if (targetUid.isBlank() || reason.isBlank() || _ui.value.reportSubmitting) return@launch
        _ui.update { it.copy(reportSubmitting = true) }
        supportRepo.submitProfileReport(targetUid, reason)
            .onSuccess {
                analytics.logProfileReported(reason)
                _ui.update {
                    it.copy(
                        showReportDialog = false,
                        reportSubmitting = false,
                        reportMessage = "Report submitted for moderation."
                    )
                }
            }
            .onFailure {
                _ui.update {
                    it.copy(
                        reportSubmitting = false,
                        reportMessage = "Report could not be submitted. Check your connection and try again."
                    )
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MatchDetailScreen(
    userId: Long,
    onBack: () -> Unit,
    onChat: () -> Unit,
    onPricing: () -> Unit = {},
    onKundli: () -> Unit = {},
    vm: MatchDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(userId) { vm.load(userId) }
    val ui by vm.ui.collectAsState()
    val p = ui.profile
    var showNote by remember { mutableStateOf(false) }
    var noteText by remember(ui.privateNote) { mutableStateOf(ui.privateNote) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.reportMessage) {
        ui.reportMessage?.let {
            snackbar.showSnackbar(it)
            vm.consumeReportMessage()
        }
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
                    label = { Text("Only you can see this on this app account") },
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
            onDismissRequest = { if (!ui.reportSubmitting) vm.dismissReportDialog() },
            title = { Text("Report profile") },
            text = {
                Column {
                    Text("Choose the reason that best describes the issue.")
                    reasons.forEach { reason ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selected == reason, onClick = { selected = reason }, enabled = !ui.reportSubmitting)
                            Text(reason)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selected?.let(vm::submitReport) }, enabled = selected != null && !ui.reportSubmitting) {
                    if (ui.reportSubmitting) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (ui.reportSubmitting) "Submitting…" else "Submit report")
                }
            },
            dismissButton = { TextButton(onClick = vm::dismissReportDialog, enabled = !ui.reportSubmitting) { Text("Cancel") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(p?.displayName ?: "Profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = onKundli, enabled = p?.showHoroscope == true && !ui.blocked) {
                        Icon(Icons.Filled.AutoAwesome, "Check Kundali")
                    }
                    IconButton(onClick = { showNote = true }, enabled = p != null) { Icon(Icons.Filled.Note, "Private note") }
                    IconButton(onClick = vm::showReportDialog, enabled = p != null) { Icon(Icons.Filled.Flag, "Report profile") }
                }
            )
        }
    ) { pad ->
        when {
            ui.loading -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            p == null -> Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { Text("This profile is unavailable.") }
            else -> Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("match_detail_screen"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProfileHero(p, ui.photos)

                if (ui.blocked) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            "You blocked this member. Interests, messaging and contact reveal stay unavailable until you unblock them.",
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::toggleLike, enabled = !ui.blocked, modifier = Modifier.weight(1f)) {
                        Icon(if (ui.liked) Icons.Filled.Favorite else Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (ui.liked) "Interest sent" else "Send interest")
                    }
                    OutlinedButton(onClick = vm::toggleShortlist, enabled = !ui.blocked, modifier = Modifier.weight(1f)) {
                        Icon(if (ui.shortlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (ui.shortlisted) "Saved" else "Shortlist")
                    }
                }

                if (p.showHoroscope && !ui.blocked) {
                    OutlinedButton(onClick = onKundli, modifier = Modifier.fillMaxWidth().testTag("profile_check_kundli")) {
                        Icon(Icons.Filled.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check Kundali compatibility")
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onChat, enabled = ui.isMutual && !ui.blocked, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (ui.isMutual) "Message" else "Message after match")
                    }
                    OutlinedButton(onClick = vm::showContactUnlock, enabled = ui.isMutual && !ui.blocked, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Phone, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Contact")
                    }
                }

                if (!ui.isMutual && !ui.blocked) {
                    Text(
                        "Messaging and contact reveal unlock only after both members express interest. You can review this profile and Kundali before accepting or sending interest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ActualCompatibilityCard(ui)
                ProfileFacts(p)

                if (p.bio.isNotBlank()) SectionCard("About") { Text(p.bio, style = MaterialTheme.typography.bodyMedium) }
                if (ui.privateNote.isNotBlank()) SectionCard("Your private note") { Text(ui.privateNote, style = MaterialTheme.typography.bodySmall) }

                OutlinedButton(
                    onClick = vm::toggleBlock,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (ui.blocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                ) {
                    Icon(if (ui.blocked) Icons.Filled.LockOpen else Icons.Filled.Block, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (ui.blocked) "Unblock member" else "Block member")
                }
                TextButton(onClick = vm::showReportDialog, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Flag, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Report profile")
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ProfileHero(profile: UserProfile, photos: List<PhotoEntity>) {
    val photoModels = remember(profile.id, profile.photoUrl, profile.primaryPhotoPath, photos) {
        buildList<Any> {
            photos
                .sortedWith(compareByDescending<PhotoEntity> { it.isPrimary }.thenBy { it.id })
                .forEach { photo ->
                    add(
                        if (photo.path.startsWith("https://") || photo.path.startsWith("http://")) {
                            photo.path
                        } else {
                            File(photo.path)
                        }
                    )
                }
            if (isEmpty()) {
                when {
                    profile.photoUrl.startsWith("https://") || profile.photoUrl.startsWith("http://") ->
                        add(profile.photoUrl)
                    !profile.primaryPhotoPath.isNullOrBlank() ->
                        add(File(profile.primaryPhotoPath))
                }
            }
        }
    }

    MatreeProfileHeader(
        name = profile.displayName,
        age = profile.age.takeIf { it > 0 },
        username = profile.username,
        photoModels = photoModels,
        primaryLine = listOf(profile.city, profile.profession)
            .filter { it.isNotBlank() }
            .joinToString(" • "),
        secondaryLine = listOf(profile.religion, profile.motherTongue)
            .filter { it.isNotBlank() }
            .joinToString(" • "),
        isVerified = profile.isVerified,
        isPremium = profile.isPremium
    )
}

@Composable
private fun ActualCompatibilityCard(ui: DetailUi) {
    val available = ui.qScore > 0f || ui.astroScore > 0f
    if (!available) return
    SectionCard("Compatibility") {
        if (ui.qScore > 0f) ScoreRow("Questionnaire", ui.qScore)
        if (ui.astroScore > 0f) ScoreRow("Astrology", ui.astroScore)
        ScoreRow("Combined", ui.combinedScore)
        Text(
            "Compatibility scores are decision-support signals from the information available in the app; they are not predictions or guarantees about a relationship.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScoreRow(label: String, score: Float) {
    val pct = (score.coerceIn(0f, 1f) * 100).toInt()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(105.dp), style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(progress = { score.coerceIn(0f, 1f) }, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text("$pct%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileFacts(p: UserProfile) {
    SectionCard("Profile details") {
        Fact("Marital status", p.maritalStatus)
        Fact("Height", if (p.heightCm > 0) "${p.heightCm} cm" else "")
        Fact("Education", p.education)
        Fact("Profession", p.profession)
        Fact("Religion", p.religion)
        Fact("Community", p.caste)
        Fact("Mother tongue", p.motherTongue)
        Fact("Family type", p.familyType)
        Fact("Family values", p.familyValues)
        Fact("Diet", p.diet)
        Fact("Country", p.countryOfResidence)
        if (p.showHoroscope && ReligionCategory.fromReligion(p.religion) == ReligionCategory.HINDU) {
            Fact("Rasi / Nakshatra", listOf(p.rasi, p.nakshatra).filter { it.isNotBlank() }.joinToString(" • "))
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, modifier = Modifier.weight(0.42f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(0.58f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    MatreeProfileSection(title = title, content = content)
}
