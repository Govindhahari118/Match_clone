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
import com.match.app.core.matching.MatchScorer
import com.match.app.data.local.Vec
import com.match.app.data.local.dao.QuestionnaireDao
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.PhotoEntity
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.KundliRepository
import com.match.app.data.repo.NoteRepository
import com.match.app.data.repo.PhotoRepository
import com.match.app.data.repo.PhotoRequestRepository
import com.match.app.data.repo.PartnerPreferenceRepository
import com.match.app.data.repo.PartnerPreferenceSummary
import com.match.app.data.repo.PartnerPreferenceMode
import com.match.app.data.repo.ShortlistRepository
import com.match.app.data.repo.SocialRepository
import com.match.app.data.repo.SubscriptionRepository
import com.match.app.data.repo.SupportRepository
import com.match.app.data.repo.TrustRepository
import com.match.app.data.repo.TrustSummary
import com.match.app.data.repo.WhoViewedRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.CompatibilityFactor
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.UserProfile
import com.match.app.ui.common.ContactUnlockSheet
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeLoadingState
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.components.MatreeProfileHeader
import com.match.app.ui.components.MatreeProfileSection
import com.match.app.ui.components.MatreeSecondaryButton
import com.match.app.ui.components.MatreeStatePanel
import com.match.app.ui.components.MatreeStatusTone
import com.match.app.ui.components.MatreeTopBar
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DetailUi(
    val profile: UserProfile? = null,
    val photos: List<PhotoEntity> = emptyList(),
    val photoRequestStatus: String = "NONE",
    val photoRequestRetryAfterMillis: Long = 0L,
    val photoRequestLoading: Boolean = false,
    val photoRequestMessage: String? = null,
    val liked: Boolean = false,
    val blocked: Boolean = false,
    val shortlisted: Boolean = false,
    val isMutual: Boolean = false,
    val qScore: Float = 0f,
    val astroScore: Float = 0f,
    val combinedScore: Float = 0f,
    val compatibilityFactors: List<CompatibilityFactor> = emptyList(),
    val compatibilityFormulaVersion: String = "",
    val compatibilityAgePenalty: Float = 0f,
    val privateNote: String = "",
    val meIsPremium: Boolean = false,
    val showContactUnlock: Boolean = false,
    val revealedPhone: String = "",
    val contactsUsed: Int = 0,
    val contactsLimit: Int = 0,
    val contactLoading: Boolean = false,
    val contactRequestLoading: Boolean = false,
    val contactRequestStatus: String = "",
    val contactRequestMessage: String? = null,
    val contactError: String? = null,
    val showReportDialog: Boolean = false,
    val reportSubmitting: Boolean = false,
    val reportMessage: String? = null,
    val trustSummary: TrustSummary? = null,
    val partnerPreferenceSummary: PartnerPreferenceSummary? = null,
    val showInterestDialog: Boolean = false,
    val interestSending: Boolean = false,
    val interestError: String? = null,
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
    private val kundliRepo: KundliRepository,
    private val photoRepo: PhotoRepository,
    private val photoRequestRepository: PhotoRequestRepository,
    private val analytics: AnalyticsManager,
    private val noteRepo: NoteRepository,
    private val subscriptionRepo: SubscriptionRepository,
    private val supportRepo: SupportRepository,
    private val trustRepository: TrustRepository,
    private val partnerPreferenceRepository: PartnerPreferenceRepository
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
        val trustSummary = profile.firebaseUid
            .takeIf { it.isNotBlank() }
            ?.let { uid -> runCatching { trustRepository.load(uid) }.getOrNull() }
        val preferenceSummary = profile.firebaseUid
            .takeIf { it.isNotBlank() }
            ?.let { uid -> runCatching { partnerPreferenceRepository.loadPublicSummary(uid) }.getOrNull() }
        val photoRequestState = profile.firebaseUid
            .takeIf { it.isNotBlank() && profile.photoUrl.isBlank() }
            ?.let { uid -> photoRequestRepository.status(uid).getOrNull() }
        val photoRequestStatus = photoRequestState?.status
            ?: if (profile.photoUrl.isNotBlank()) "PHOTO_AVAILABLE" else "NONE"
        val meLiked = social.isLiked(meId, userId)
        val theyLiked = social.isLiked(userId, meId)
        val me = auth.currentProfile(meId)
        _ui.value = _ui.value.copy(
            profile = profile,
            photos = photos,
            photoRequestStatus = photoRequestStatus,
            photoRequestRetryAfterMillis = photoRequestState?.retryAfterMillis ?: 0L,
            liked = meLiked,
            blocked = social.isBlocked(meId, userId),
            shortlisted = shortlistRepo.isSaved(meId, userId),
            isMutual = meLiked && theyLiked,
            meIsPremium = me?.isPremium == true,
            trustSummary = trustSummary,
            partnerPreferenceSummary = preferenceSummary,
            loading = false
        )

        launch {
            noteRepo.observe(meId, userId).collect { note ->
                _ui.update { it.copy(privateNote = note?.note.orEmpty()) }
            }
        }

        runCatching { whoViewed.record(meId, userId) }
        analytics.logProfileView(userId)

        if (me != null) {
            val seekerQ = qDao.forUser(meId)
            val targetQ = qDao.forUser(userId)
            var scorerMe = me.copy(
                selfVector = seekerQ?.let { Vec.decode(it.selfVector) },
                partnerVector = seekerQ?.let { Vec.decode(it.partnerVector) }
            )
            var scorerTarget = profile.copy(
                selfVector = targetQ?.let { Vec.decode(it.selfVector) },
                partnerVector = targetQ?.let { Vec.decode(it.partnerVector) },
                // Peer-private astrology must never come from stale Room/profile cache.
                rasi = "",
                nakshatra = ""
            )

            if (profile.showHoroscope && profile.firebaseUid.isNotBlank()) {
                val shared = kundliRepo.getSharedHoroscope(profile.firebaseUid).getOrNull()
                if (shared?.available == true) {
                    scorerMe = scorerMe.copy(
                        rasi = shared.myRasi,
                        nakshatra = shared.myNakshatra
                    )
                    scorerTarget = scorerTarget.copy(
                        rasi = shared.targetRasi,
                        nakshatra = shared.targetNakshatra
                    )
                } else {
                    scorerMe = scorerMe.copy(rasi = "", nakshatra = "")
                }
            } else {
                scorerMe = scorerMe.copy(rasi = "", nakshatra = "")
            }

            val result = MatchScorer.explain(scorerMe, scorerTarget)
            val factors = result.factors.map {
                CompatibilityFactor(
                    key = it.key,
                    score = it.score,
                    configuredWeight = it.configuredWeight
                )
            }
            _ui.update {
                it.copy(
                    qScore = result.factors.firstOrNull { factor -> factor.key == "questionnaire" }?.score ?: 0f,
                    astroScore = result.factors.firstOrNull { factor -> factor.key == "astrology" }?.score ?: 0f,
                    combinedScore = result.percentage.toFloat() / 100f,
                    compatibilityFactors = factors,
                    compatibilityFormulaVersion = result.formulaVersion,
                    compatibilityAgePenalty = result.agePenalty
                )
            }
        }
    }

    fun toggleLike() = viewModelScope.launch {
        if (_ui.value.blocked || _ui.value.interestSending) return@launch
        if (_ui.value.liked) {
            runCatching { social.unlike(meId, targetId) }
                .onSuccess {
                    _ui.update {
                        it.copy(
                            liked = false,
                            isMutual = false,
                            contactError = null,
                            interestError = null
                        )
                    }
                }
                .onFailure { error ->
                    _ui.update {
                        it.copy(interestError = error.message ?: "Could not withdraw this interest.")
                    }
                }
        } else {
            _ui.update { it.copy(showInterestDialog = true, interestError = null) }
        }
    }

    fun dismissInterestDialog() {
        if (!_ui.value.interestSending) {
            _ui.update { it.copy(showInterestDialog = false, interestError = null) }
        }
    }

    fun sendInterest(introNote: String) = viewModelScope.launch {
        if (_ui.value.blocked || _ui.value.interestSending || _ui.value.liked) return@launch
        _ui.update { it.copy(interestSending = true, interestError = null) }
        runCatching {
            social.sendInterest(
                meId,
                targetId,
                isSuperLike = false,
                introNote = introNote.trim()
            )
        }.onSuccess { mutual ->
            _ui.update {
                it.copy(
                    liked = true,
                    isMutual = mutual,
                    showInterestDialog = false,
                    interestSending = false,
                    interestError = null,
                    contactError = null
                )
            }
            analytics.logInterestSent(targetId)
        }.onFailure { error ->
            _ui.update {
                it.copy(
                    interestSending = false,
                    interestError = error.message ?: "Could not send this interest."
                )
            }
        }
    }

    fun requestPhoto() = viewModelScope.launch {
        val current = _ui.value
        val targetUid = current.profile?.firebaseUid.orEmpty()
        if (targetUid.isBlank() || current.blocked || current.photoRequestLoading ||
            current.profile?.photoUrl?.isNotBlank() == true ||
            (current.photoRequestStatus == "PENDING" && current.photoRequestRetryAfterMillis > 0L) ||
            current.photoRequestStatus == "PHOTO_AVAILABLE" ||
            current.photoRequestStatus == "UNAVAILABLE") return@launch

        _ui.update { it.copy(photoRequestLoading = true, photoRequestMessage = null) }
        photoRequestRepository.request(targetUid)
            .onSuccess { result ->
                val message = when (result.status) {
                    "PENDING" -> "photo_request_sent"
                    "PHOTO_AVAILABLE" -> "photo_now_available"
                    else -> null
                }
                _ui.update {
                    it.copy(
                        photoRequestLoading = false,
                        photoRequestStatus = result.status,
                        photoRequestRetryAfterMillis = result.retryAfterMillis,
                        photoRequestMessage = message
                    )
                }
            }
            .onFailure {
                _ui.update {
                    it.copy(
                        photoRequestLoading = false,
                        photoRequestMessage = "photo_request_failed"
                    )
                }
            }
    }

    fun consumePhotoRequestMessage() = _ui.update { it.copy(photoRequestMessage = null) }

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

    fun dismissContactUnlock() = _ui.update {
        it.copy(showContactUnlock = false, contactError = null, contactRequestMessage = null)
    }

    fun requestContactAccess() = viewModelScope.launch {
        val targetUid = _ui.value.profile?.firebaseUid.orEmpty()
        if (!_ui.value.isMutual || targetUid.isBlank() || _ui.value.contactRequestLoading) return@launch
        _ui.update {
            it.copy(contactRequestLoading = true, contactRequestMessage = null, contactError = null)
        }
        subscriptionRepo.requestContactAccess(targetUid)
            .onSuccess { result ->
                val message = when (result.status) {
                    "PENDING" -> "contact_request_pending"
                    "APPROVED", "AUTO_SHARE_ALLOWED" -> "contact_request_approved"
                    else -> "contact_request_sent"
                }
                _ui.update {
                    it.copy(
                        contactRequestLoading = false,
                        contactRequestStatus = result.status,
                        contactRequestMessage = message
                    )
                }
            }
            .onFailure { error ->
                _ui.update {
                    it.copy(
                        contactRequestLoading = false,
                        contactRequestMessage = "contact_request_failed",
                        contactError = error.message?.take(180)
                    )
                }
            }
    }

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
    onSecureCall: () -> Unit = {},
    onPricing: () -> Unit = {},
    onKundli: () -> Unit = {},
    onCompatibilityBreakdown: () -> Unit = {},
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

    LaunchedEffect(ui.photoRequestMessage) {
        val message = when (ui.photoRequestMessage) {
            "photo_request_sent" -> t("photo_request_sent", "Photo request sent.")
            "photo_now_available" -> t("photo_now_available", "A profile photo is now available.")
            "photo_request_failed" -> t("photo_request_failed", "Photo request could not be sent. Please try again.")
            else -> null
        }
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumePhotoRequestMessage()
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
            requestLoading = ui.contactRequestLoading,
            requestStatus = ui.contactRequestStatus,
            requestMessage = ui.contactRequestMessage,
            errorMessage = ui.contactError,
            onReveal = vm::revealContact,
            onRequestAccess = vm::requestContactAccess,
            onUpgrade = onPricing,
            onMessage = onChat,
            onDismiss = vm::dismissContactUnlock
        )
    }

    if (ui.showInterestDialog && p != null) {
        var introNote by remember(p.firebaseUid) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = vm::dismissInterestDialog,
            title = { Text(t("send_interest_title", "Send interest")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                    Text(
                        t("interest_intro_help", "Add an optional personal introduction. Contact details, external links and payment requests are blocked until you both match."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = introNote,
                        onValueChange = { introNote = it.take(280) },
                        label = { Text(t("personal_note_optional", "Personal note (optional)")) },
                        supportingText = { Text("${introNote.length}/280") },
                        minLines = 3,
                        maxLines = 5,
                        enabled = !ui.interestSending,
                        modifier = Modifier.fillMaxWidth().testTag("interest_intro_note")
                    )
                    ui.interestError?.let { error ->
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.sendInterest(introNote) },
                    enabled = !ui.interestSending
                ) {
                    if (ui.interestSending) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (ui.interestSending) t("sending", "Sending…") else t("send_interest", "Send interest"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = vm::dismissInterestDialog,
                    enabled = !ui.interestSending
                ) { Text(t("cancel", "Cancel")) }
            }
        )
    }

    if (showNote) {
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text(t("private_note", "Private note")) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it.take(1000) },
                    label = { Text(t("private_note_only_you", "Only you can see this on this app account")) },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { vm.saveNote(noteText); showNote = false }) { Text(t("save", "Save")) } },
            dismissButton = { TextButton(onClick = { showNote = false }) { Text(t("cancel", "Cancel")) } }
        )
    }

    if (ui.showReportDialog) {
        val reasons = listOf(
            t("fake_profile", "Fake profile"),
            t("inappropriate_content", "Inappropriate content"),
            t("harassment", "Harassment"),
            t("spam_or_scam", "Spam or scam"),
            t("under_age", "Under age"),
            t("other", "Other")
        )
        var selected by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { if (!ui.reportSubmitting) vm.dismissReportDialog() },
            title = { Text(t("report_profile", "Report profile")) },
            text = {
                Column {
                    Text(t("report_choose_reason", "Choose the reason that best describes the issue."))
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
                    Text(if (ui.reportSubmitting) t("submitting", "Submitting…") else t("submit_report", "Submit report"))
                }
            },
            dismissButton = { TextButton(onClick = vm::dismissReportDialog, enabled = !ui.reportSubmitting) { Text(t("cancel", "Cancel")) } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            MatreeTopBar(
                title = p?.displayName ?: t("profile", "Profile"),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onKundli, enabled = p?.showHoroscope == true && !ui.blocked) {
                        Icon(Icons.Filled.AutoAwesome, t("check_kundali_compatibility", "Check Kundali compatibility"))
                    }
                    IconButton(onClick = { showNote = true }, enabled = p != null) {
                        Icon(Icons.Filled.Note, t("private_note", "Private note"))
                    }
                    IconButton(onClick = vm::showReportDialog, enabled = p != null) {
                        Icon(Icons.Filled.Flag, t("report_profile", "Report profile"))
                    }
                }
            )
        }
    ) { pad ->
        when {
            ui.loading -> Box(
                Modifier.padding(pad).fillMaxSize().padding(MatreeDesign.spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                MatreeLoadingState(message = t("loading_profile", "Loading profile…"), rows = 3)
            }
            p == null -> Box(
                Modifier.padding(pad).fillMaxSize().padding(MatreeDesign.spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                MatreeStatePanel(
                    title = t("profile_unavailable", "Profile unavailable"),
                    message = t("profile_unavailable_message", "This profile is no longer available with your current relationship or privacy state."),
                    icon = Icons.Filled.PersonOff,
                    tone = MatreeStatusTone.WARNING
                )
            }
            else -> Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(MatreeDesign.spacing.md)
                    .testTag("match_detail_screen"),
                verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
            ) {
                ProfileHero(p, ui.photos)

                val noDisplayedPhoto = p.photoUrl.isBlank() &&
                    p.primaryPhotoPath.isNullOrBlank() &&
                    ui.photos.isEmpty()
                if (noDisplayedPhoto &&
                    ui.photoRequestStatus != "PHOTO_AVAILABLE" &&
                    ui.photoRequestStatus != "UNAVAILABLE") {
                    MatreeSecondaryButton(
                        text = when {
                            ui.photoRequestLoading -> t("requesting_photo", "Requesting…")
                            ui.photoRequestStatus == "PENDING" && ui.photoRequestRetryAfterMillis > 0L ->
                                t("photo_requested", "Photo requested")
                            else -> t("request_photo", "Request photo")
                        },
                        icon = if (ui.photoRequestStatus == "PENDING" && ui.photoRequestRetryAfterMillis > 0L) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Filled.AddAPhoto
                        },
                        onClick = vm::requestPhoto,
                        enabled = !ui.blocked && !ui.photoRequestLoading &&
                            !(ui.photoRequestStatus == "PENDING" && ui.photoRequestRetryAfterMillis > 0L),
                        modifier = Modifier.fillMaxWidth().testTag("profile_request_photo")
                    )
                    if (ui.photoRequestStatus == "PENDING" && ui.photoRequestRetryAfterMillis > 0L) {
                        Text(
                            t(
                                "photo_request_privacy_note",
                                "This asks the member to add a profile photo. It does not reveal hidden photos or change their privacy settings."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }


                if (ui.blocked) {
                    MatreeInlineNotice(
                        message = t("blocked_member_notice", "You blocked this member. Interests, messaging and contact reveal stay unavailable until you unblock them."),
                        icon = Icons.Filled.Block,
                        tone = MatreeStatusTone.ERROR
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
                ) {
                    MatreePrimaryButton(
                        text = if (ui.liked) t("interest_sent", "Interest sent") else t("send_interest", "Send interest"),
                        icon = if (ui.liked) Icons.Filled.Favorite else Icons.AutoMirrored.Filled.Send,
                        onClick = vm::toggleLike,
                        enabled = !ui.blocked,
                        modifier = Modifier.weight(1f)
                    )
                    MatreeSecondaryButton(
                        text = if (ui.shortlisted) t("saved", "Saved") else t("shortlist", "Shortlist"),
                        icon = if (ui.shortlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        onClick = vm::toggleShortlist,
                        enabled = !ui.blocked,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (p.showHoroscope && !ui.blocked) {
                    MatreeSecondaryButton(
                        text = t("check_kundali_compatibility", "Check Kundali compatibility"),
                        icon = Icons.Filled.AutoAwesome,
                        onClick = onKundli,
                        modifier = Modifier.fillMaxWidth().testTag("profile_check_kundli")
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
                ) {
                    MatreePrimaryButton(
                        text = if (ui.isMutual) t("message", "Message") else t("message_after_match", "Message after match"),
                        icon = Icons.AutoMirrored.Filled.Chat,
                        onClick = onChat,
                        enabled = ui.isMutual && !ui.blocked,
                        modifier = Modifier.weight(1f)
                    )
                    MatreeSecondaryButton(
                        text = t("contact", "Contact"),
                        icon = Icons.Filled.Phone,
                        onClick = vm::showContactUnlock,
                        enabled = ui.isMutual && !ui.blocked,
                        modifier = Modifier.weight(1f)
                    )
                }

                MatreeSecondaryButton(
                    text = if (ui.isMutual) t("secure_call", "Secure Call") else t("secure_call_after_match", "Secure call after match"),
                    icon = Icons.Filled.PhoneInTalk,
                    onClick = onSecureCall,
                    enabled = ui.isMutual && !ui.blocked,
                    modifier = Modifier.fillMaxWidth().testTag("profile_secure_call")
                )

                if (!ui.isMutual && !ui.blocked) {
                    MatreeInlineNotice(
                        message = t("mutual_unlock_notice", "Messaging and contact reveal unlock only after both members express interest. You can review this profile and Kundali before accepting or sending interest."),
                        icon = Icons.Filled.Info
                    )
                }

                PartnerExpectationCard(ui.partnerPreferenceSummary)
                TrustSummaryCard(ui.trustSummary)
                ActualCompatibilityCard(ui)
                MatreeSecondaryButton(
                    text = t("compatibility_breakdown", "Compatibility breakdown"),
                    icon = Icons.Filled.Insights,
                    onClick = onCompatibilityBreakdown,
                    enabled = !ui.blocked,
                    modifier = Modifier.fillMaxWidth().testTag("profile_compat_breakdown")
                )
                ProfileFacts(p)

                if (p.bio.isNotBlank()) SectionCard(t("about", "About")) { Text(p.bio, style = MaterialTheme.typography.bodyMedium) }
                if (ui.privateNote.isNotBlank()) SectionCard(t("your_private_note", "Your private note")) { Text(ui.privateNote, style = MaterialTheme.typography.bodySmall) }

                OutlinedButton(
                    onClick = vm::toggleBlock,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (ui.blocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                ) {
                    Icon(if (ui.blocked) Icons.Filled.LockOpen else Icons.Filled.Block, null)
                    Spacer(Modifier.width(MatreeDesign.spacing.xs))
                    Text(if (ui.blocked) t("unblock_member", "Unblock member") else t("block_member", "Block member"))
                }
                TextButton(onClick = vm::showReportDialog, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Flag, null)
                    Spacer(Modifier.width(MatreeDesign.spacing.xs))
                    Text(t("report_profile", "Report profile"))
                }
                Spacer(Modifier.height(MatreeDesign.spacing.lg))
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
private fun PartnerExpectationCard(summary: PartnerPreferenceSummary?) {
    if (summary?.shared != true || summary.items.isEmpty()) return
    SectionCard(t("what_they_are_looking_for", "What they are looking for")) {
        summary.items.forEach { item ->
            val label = when (item.key) {
                "age" -> t("age", "Age")
                "height" -> t("height", "Height")
                "state" -> t("state", "State")
                "city" -> t("city", "City")
                "country_of_residence" -> t("country", "Country")
                "marital_status" -> t("marital_status", "Marital status")
                "education" -> t("education", "Education")
                "occupation" -> t("occupation", "Occupation")
                "diet" -> t("diet", "Diet")
                "smoking" -> t("smoking", "Smoking")
                "drinking" -> t("drinking", "Drinking")
                "family_values" -> t("family_values", "Family values")
                "relocation" -> t("relocation", "Relocation")
                else -> item.key.replace('_', ' ').replaceFirstChar { it.uppercase() }
            }
            val mode = if (item.mode == PartnerPreferenceMode.STRICT) t("must_match", "Must match") else t("preferred", "Preferred")
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Column(Modifier.weight(0.42f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        mode,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.mode == PartnerPreferenceMode.STRICT) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Text(
                    item.value,
                    modifier = Modifier.weight(0.58f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(
            t("shared_preferences_privacy", "Only the criteria this member chose to share are shown. Sensitive preferences stay private."),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrustSummaryCard(trust: TrustSummary?) {
    if (trust == null) {
        MatreeInlineNotice(
            message = t("trust_unavailable", "Trust Score is temporarily unavailable. Verification badges remain authoritative."),
            icon = Icons.Filled.VerifiedUser
        )
        return
    }
    SectionCard(t("trust_and_verification", "Trust & verification")) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${trust.score}/100",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(MatreeDesign.spacing.sm))
            Column {
                Text(trust.tierLabel, fontWeight = FontWeight.SemiBold)
                Text(
                    t("server_authoritative_trust", "Server-authoritative Trust Score"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        LinearProgressIndicator(
            progress = { trust.score.coerceIn(0, 100) / 100f },
            modifier = Modifier.fillMaxWidth()
        )
        trust.factors.take(6).forEach { factor ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(MatreeDesign.sizes.iconSmall),
                    tint = MatreeDesign.colors.success
                )
                Spacer(Modifier.width(MatreeDesign.spacing.xs))
                Text(factor, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            t("trust_paid_note", "Paid membership does not increase Trust Score. Fraud-detection details stay private so they cannot be gamed."),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActualCompatibilityCard(ui: DetailUi) {
    if (ui.compatibilityFactors.isEmpty()) return
    SectionCard(t("compatibility", "Compatibility")) {
        ui.compatibilityFactors.forEach { factor ->
            val label = when (factor.key) {
                "questionnaire" -> t("questionnaire", "Questionnaire")
                "astrology" -> t("astrology", "Astrology")
                "demographics_lifestyle" -> t("profile_fit", "Profile fit")
                "mutual_trust" -> t("mutual_trust", "Mutual trust")
                else -> factor.key.replace('_', ' ').replaceFirstChar { it.uppercase() }
            }
            ScoreRow(label, factor.score)
        }
        if (ui.compatibilityAgePenalty > 0f) {
            Text(
                "Age-gap adjustment: −${(ui.compatibilityAgePenalty * 100).toInt()} percentage points",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ScoreRow(t("overall", "Overall"), ui.combinedScore)
        Text(
            t("compatibility_disclaimer", "Compatibility scores are decision-support signals from the information available in the app; they are not predictions or guarantees about a relationship."),
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
        Spacer(Modifier.width(MatreeDesign.spacing.xs))
        Text("$pct%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileFacts(p: UserProfile) {
    SectionCard(t("profile_details", "Profile details")) {
        Fact(t("profile_managed_as", "Profile managed as"), p.profileCreatedFor.displayLabel)
        Fact(t("marital_status", "Marital status"), p.maritalStatus)
        Fact(t("height", "Height"), if (p.heightCm > 0) "${p.heightCm} cm" else "")
        Fact(t("education", "Education"), p.education)
        Fact(t("profession", "Profession"), p.profession)
        Fact(t("religion", "Religion"), p.religion)
        Fact(t("community", "Community"), p.caste)
        Fact(t("mother_tongue", "Mother tongue"), p.motherTongue)
        Fact(t("family_type", "Family type"), p.familyType)
        Fact(t("family_values", "Family values"), p.familyValues)
        Fact(t("diet", "Diet"), p.diet)
        Fact(t("country", "Country"), p.countryOfResidence)
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
