package com.match.app.ui.verification

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.components.MatreeChoiceChip
import com.match.app.ui.components.MatreeInfoCard
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.components.MatreeProgressCard
import com.match.app.ui.components.MatreeStatusTone
import com.match.app.ui.components.MatreeTopBar
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val MAX_VERIFICATION_BYTES = 5L * 1024L * 1024L

enum class VerificationStatus {
    NOT_STARTED, PENDING, VERIFIED, REQUIRES_ACTION;

    companion object {
        fun fromWire(value: String?): VerificationStatus = when (value?.lowercase()) {
            "pending" -> PENDING
            "verified" -> VERIFIED
            "rejected", "requires_action" -> REQUIRES_ACTION
            else -> NOT_STARTED
        }
    }
}

data class VerificationUi(
    val isPhoneVerified: Boolean = false,
    val isIdVerified: Boolean = false,
    val isProfileComplete: Boolean = false,
    val isPhotoAdded: Boolean = false,
    val verificationStatus: VerificationStatus = VerificationStatus.NOT_STARTED,
    val rejectionReason: String = ""
)

data class VerificationOptionsUi(
    val genericDocumentTypes: List<String> = emptyList(),
    val identityConsentVersion: String = "",
    val aadhaarOfflineAvailable: Boolean = false,
    val selfieVerificationAvailable: Boolean = false,
    val livenessAvailable: Boolean = false,
    val faceSimilarityAvailable: Boolean = false,
    val loaded: Boolean = false
)

private data class RemoteVerification(
    val status: VerificationStatus = VerificationStatus.NOT_STARTED,
    val rejectionReason: String = ""
)

private data class CheckItem(val icon: ImageVector, val title: String, val description: String, val done: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VerificationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    private val remoteVerification: Flow<RemoteVerification> = session.firebaseUid
        .filterNotNull()
        .flatMapLatest { uid ->
            callbackFlow {
                val registration = firestore.collection("verificationRequests").document(uid)
                    .addSnapshotListener { doc, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(
                            RemoteVerification(
                                status = VerificationStatus.fromWire(doc?.getString("status")),
                                rejectionReason = doc?.getString("rejectionReason").orEmpty()
                            )
                        )
                    }
                awaitClose { registration.remove() }
            }
        }

    val ui: StateFlow<VerificationUi> = combine(
        session.userId.filterNotNull(),
        remoteVerification
    ) { localId, remote ->
        val profile = auth.currentProfile(localId)
        val currentUser = FirebaseAuth.getInstance().currentUser
        VerificationUi(
            isPhoneVerified = !currentUser?.phoneNumber.isNullOrBlank(),
            isIdVerified = remote.status == VerificationStatus.VERIFIED || profile?.isVerified == true,
            isProfileComplete = profile != null && profile.bio.isNotBlank() && profile.city.isNotBlank(),
            isPhotoAdded = profile?.primaryPhotoPath != null || !profile?.photoUrl.isNullOrBlank(),
            verificationStatus = remote.status,
            rejectionReason = remote.rejectionReason
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VerificationUi())

    private val _options = MutableStateFlow(VerificationOptionsUi())
    val options: StateFlow<VerificationOptionsUi> = _options.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val response = functions.getHttpsCallable("getVerificationOptions").call().await()
                @Suppress("UNCHECKED_CAST")
                val data = response.data as? Map<String, Any?> ?: error("Invalid verification-options response")
                VerificationOptionsUi(
                    genericDocumentTypes = (data["genericDocumentTypes"] as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.filter { it.isNotBlank() }
                        .orEmpty(),
                    identityConsentVersion = data["identityConsentVersion"] as? String ?: "",
                    aadhaarOfflineAvailable = data["aadhaarOfflineAvailable"] as? Boolean ?: false,
                    selfieVerificationAvailable =
                        data["selfieVerificationAvailable"] as? Boolean ?: false,
                    livenessAvailable = data["livenessAvailable"] as? Boolean ?: false,
                    faceSimilarityAvailable =
                        data["faceSimilarityAvailable"] as? Boolean ?: false,
                    loaded = true
                )
            }.onSuccess {
                _options.value = it
            }.onFailure {
                _error.value = "Verification options could not be loaded. Please reconnect and try again."
            }
        }
    }

    fun clearError() { _error.value = null }

    fun submitVerification(docType: String, documentUri: Uri?, consentGranted: Boolean) {
        if (_submitting.value) return
        if (documentUri == null) {
            _error.value = "Choose a clear government-ID image or PDF before submitting."
            return
        }
        if (!consentGranted) {
            _error.value = "Review and accept the identity-verification consent before submitting."
            return
        }
        val consentVersion = options.value.identityConsentVersion
        if (!options.value.loaded || consentVersion.isBlank()) {
            _error.value = "Current verification consent could not be loaded. Please retry."
            return
        }
        if (docType !in options.value.genericDocumentTypes) {
            _error.value = "Choose a currently supported government ID type."
            return
        }
        viewModelScope.launch {
            _submitting.value = true
            _error.value = null
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Sign in required")
                functions.getHttpsCallable("recordConsent")
                    .call(
                        mapOf(
                            "purpose" to "identity_verification",
                            "granted" to true,
                            "noticeVersion" to consentVersion,
                            "locale" to "en-IN"
                        )
                    )
                    .await()

                val contentType = context.contentResolver.getType(documentUri)?.lowercase().orEmpty()
                require(
                    contentType in setOf(
                        "image/jpeg",
                        "image/png",
                        "image/webp",
                        "application/pdf"
                    )
                ) {
                    "Choose a JPEG, PNG, WebP or PDF document."
                }
                val knownSize = documentSize(documentUri)
                require(knownSize <= 0L || knownSize <= MAX_VERIFICATION_BYTES) {
                    "Verification document must be 5 MB or smaller."
                }

                val documentPath = "verifications/$uid/document"
                val metadata = StorageMetadata.Builder()
                    .setContentType(contentType)
                    .setCustomMetadata("ownerUid", uid)
                    .setCustomMetadata("docType", docType)
                    .build()
                storage.reference.child(documentPath).putFile(documentUri, metadata).await()

                functions.getHttpsCallable("submitVerificationRequest")
                    .call(mapOf("docType" to docType, "documentPath" to documentPath))
                    .await()
            } catch (error: Exception) {
                _error.value = error.message?.take(220) ?: "Verification submission failed. Please retry."
            } finally {
                _submitting.value = false
            }
        }
    }

    private fun documentSize(uri: Uri): Long {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else -1L
            } else -1L
        } catch (_: Exception) {
            -1L
        } finally {
            cursor?.close()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VerificationScreen(
    onBack: () -> Unit = {},
    onVerifyPhone: () -> Unit = {},
    vm: VerificationViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val options by vm.options.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val error by vm.error.collectAsState()
    var selectedDocType by rememberSaveable { mutableStateOf("Passport") }
    var selectedDocument by remember { mutableStateOf<Uri?>(null) }
    var identityConsentGranted by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedDocument = uri
    }

    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    LaunchedEffect(options.genericDocumentTypes) {
        if (options.genericDocumentTypes.isNotEmpty() && selectedDocType !in options.genericDocumentTypes) {
            selectedDocType = options.genericDocumentTypes.first()
        }
    }

    val checks = listOf(
        CheckItem(Icons.Filled.PhoneAndroid, "Phone verified", "Firebase-authenticated phone number", ui.isPhoneVerified),
        CheckItem(Icons.Filled.Badge, "Identity verified", "Government ID reviewed and approved", ui.isIdVerified),
        CheckItem(Icons.Filled.Person, "Profile complete", "Core matrimonial profile details completed", ui.isProfileComplete),
        CheckItem(Icons.Filled.PhotoCamera, "Photo added", "At least one profile photo uploaded", ui.isPhotoAdded)
    )
    val passed = checks.count { it.done }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            MatreeTopBar(
                title = t("trust_verification", "Trust & Verification"),
                onBack = onBack
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(MatreeDesign.spacing.lg)
                .testTag("verification_screen"),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.md)
        ) {
            MatreeProgressCard(
                title = t("trust_progress", "Verification progress"),
                progress = passed.toFloat() / checks.size,
                supportingText = "Real account checks completed",
                valueLabel = "$passed/${checks.size}"
            )
            Text(t("verification_checklist", "Verification Checklist"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            checks.forEach { item -> VerificationRow(item.icon, item.title, item.description, item.done) }

            if (!ui.isPhoneVerified) {
                MatreePrimaryButton(
                    text = "Verify phone number",
                    icon = Icons.Filled.PhoneAndroid,
                    onClick = onVerifyPhone,
                    modifier = Modifier.fillMaxWidth().testTag("verification_phone_btn")
                )
                MatreeInlineNotice(
                    message = "Phone verification uses Firebase Phone Auth and is a separate account signal. It does not grant government-ID verification or increase your KYC level by itself.",
                    icon = Icons.Filled.Info
                )
            }

            MatreeInlineNotice(
                message = "Your raw ID file is stored in a protected KYC path and is not readable as profile media. Other members see only the final verification result.",
                icon = Icons.Filled.PrivacyTip
            )

            when (ui.verificationStatus) {
                VerificationStatus.PENDING -> StatusCard(
                    icon = Icons.Filled.HourglassTop,
                    title = "Verification pending",
                    message = "Your uploaded document is awaiting backend/admin review. No verified badge is granted until approval."
                )
                VerificationStatus.REQUIRES_ACTION -> StatusCard(
                    icon = Icons.Filled.ErrorOutline,
                    title = "Verification needs action",
                    message = ui.rejectionReason.ifBlank { "The previous submission could not be approved. Choose a clear valid document and resubmit." },
                    isError = true
                )
                VerificationStatus.VERIFIED -> StatusCard(
                    icon = Icons.Filled.Verified,
                    title = "Identity verified",
                    message = "Your government-ID review has been approved."
                )
                VerificationStatus.NOT_STARTED -> Unit
            }

            if (ui.verificationStatus != VerificationStatus.VERIFIED && ui.verificationStatus != VerificationStatus.PENDING) {
                HorizontalDivider()
                Text("Government ID", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Choose the ID type you are actually submitting. Unsupported verification methods stay hidden until they are fully implemented.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!options.loaded) {
                    MatreeInlineNotice(
                        message = "Loading the current verification policy…",
                        icon = Icons.Filled.Sync
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.genericDocumentTypes.forEach { docType ->
                            MatreeChoiceChip(
                                text = docType,
                                selected = selectedDocType == docType,
                                onClick = { selectedDocType = docType },
                                enabled = !submitting,
                                icon = if (selectedDocType == docType) Icons.Filled.Check else null
                            )
                        }
                    }
                }

                if (!options.aadhaarOfflineAvailable) {
                    MatreeInlineNotice(
                        message = "Aadhaar is not accepted as a generic card/image upload. It will only appear after a compliant registered offline-verification provider flow is deployed. Use another supported ID for now.",
                        icon = Icons.Filled.PrivacyTip,
                        tone = MatreeStatusTone.WARNING
                    )
                }

                if (
                    !options.selfieVerificationAvailable ||
                    !options.livenessAvailable ||
                    !options.faceSimilarityAvailable
                ) {
                    MatreeInlineNotice(
                        message = "Selfie verification, liveness and face similarity are not presented as active checks until a validated biometric provider adapter is deployed and server-verified. Government-ID review remains available independently.",
                        icon = Icons.Filled.Face,
                        tone = MatreeStatusTone.NEUTRAL
                    )
                }

                OutlinedButton(
                    onClick = { documentPicker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) },
                    enabled = !submitting && options.loaded && options.genericDocumentTypes.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().testTag("verification_choose_document")
                ) {
                    Icon(Icons.Filled.AttachFile, "Choose verification document")
                    Spacer(Modifier.width(MatreeDesign.spacing.xs))
                    Text(if (selectedDocument == null) "Choose JPEG, PNG, WebP or PDF" else "Document selected")
                }
                selectedDocument?.let { uri ->
                    Text(
                        uri.lastPathSegment?.takeLast(80) ?: "Selected document",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Checkbox(
                        checked = identityConsentGranted,
                        onCheckedChange = { identityConsentGranted = it },
                        enabled = !submitting && options.loaded
                    )
                    Spacer(Modifier.width(MatreeDesign.spacing.xs))
                    Text(
                        "I consent to Matree processing this government-ID document only for identity verification and review. I understand the raw document is protected from other members and is deleted after review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = MatreeDesign.spacing.xs)
                    )
                }

                MatreePrimaryButton(
                    text = if (submitting) "Uploading & validating…" else t("submit_verification", "Submit for Verification"),
                    icon = if (submitting) null else Icons.Filled.Upload,
                    onClick = { vm.submitVerification(selectedDocType, selectedDocument, identityConsentGranted) },
                    enabled = !submitting &&
                        options.loaded &&
                        options.identityConsentVersion.isNotBlank() &&
                        selectedDocument != null &&
                        identityConsentGranted,
                    modifier = Modifier.fillMaxWidth().testTag("verification_request_btn")
                )            }
            Spacer(Modifier.height(MatreeDesign.spacing.md))
        }
    }
}

@Composable
private fun StatusCard(icon: ImageVector, title: String, message: String, isError: Boolean = false) {
    MatreeInlineNotice(
        message = "$title — $message",
        icon = icon,
        tone = if (isError) MatreeStatusTone.ERROR else MatreeStatusTone.SUCCESS
    )
}

@Composable
private fun VerificationRow(icon: ImageVector, title: String, description: String, done: Boolean) {
    MatreeInfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(MatreeDesign.sizes.avatarSmall)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(MatreeDesign.spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                if (done) "$title complete" else "$title incomplete",
                Modifier.size(20.dp),
                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
