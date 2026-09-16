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
import com.match.app.ui.i18n.t
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

    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun clearError() { _error.value = null }

    fun submitVerification(docType: String, documentUri: Uri?) {
        if (_submitting.value) return
        if (documentUri == null) {
            _error.value = "Choose a clear government-ID image or PDF before submitting."
            return
        }
        viewModelScope.launch {
            _submitting.value = true
            _error.value = null
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: error("Sign in required")
                val contentType = context.contentResolver.getType(documentUri)?.lowercase().orEmpty()
                require(contentType.startsWith("image/") || contentType == "application/pdf") {
                    "Choose an image or PDF document."
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
    vm: VerificationViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val error by vm.error.collectAsState()
    var selectedDocType by rememberSaveable { mutableStateOf("Aadhaar") }
    var selectedDocument by remember { mutableStateOf<Uri?>(null) }
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
            TopAppBar(
                title = { Text(t("trust_verification", "Trust & Verification")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("verification_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                .testTag("verification_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t("trust_progress", "Verification progress"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("$passed/${checks.size}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("real account checks completed", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { passed.toFloat() / checks.size }, modifier = Modifier.fillMaxWidth().height(8.dp))
                }
            }

            Text(t("verification_checklist", "Verification Checklist"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            checks.forEach { item -> VerificationRow(item.icon, item.title, item.description, item.done) }

            Card(shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.PrivacyTip, "Private verification document", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Your raw ID file is stored in a protected KYC path and is not readable as profile media. Other members see only the final verification result.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Aadhaar", "Passport", "PAN Card", "Voter ID").forEach { docType ->
                        FilterChip(
                            selected = selectedDocType == docType,
                            onClick = { selectedDocType = docType },
                            enabled = !submitting,
                            label = { Text(docType) },
                            leadingIcon = if (selectedDocType == docType) {
                                { Icon(Icons.Filled.Check, "Selected", Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                OutlinedButton(
                    onClick = { documentPicker.launch(arrayOf("image/*", "application/pdf")) },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().testTag("verification_choose_document")
                ) {
                    Icon(Icons.Filled.AttachFile, "Choose verification document")
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedDocument == null) "Choose image or PDF" else "Document selected")
                }
                selectedDocument?.let { uri ->
                    Text(
                        uri.lastPathSegment?.takeLast(80) ?: "Selected document",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { vm.submitVerification(selectedDocType, selectedDocument) },
                    enabled = !submitting && selectedDocument != null,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("verification_request_btn")
                ) {
                    if (submitting) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Uploading & validating…")
                    } else {
                        Icon(Icons.Filled.Upload, "Submit verification document")
                        Spacer(Modifier.width(8.dp))
                        Text(t("submit_verification", "Submit for Verification"))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusCard(icon: ImageVector, title: String, message: String, isError: Boolean = false) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                Modifier.size(24.dp),
                tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun VerificationRow(icon: ImageVector, title: String, description: String, done: Boolean) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp), tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
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
