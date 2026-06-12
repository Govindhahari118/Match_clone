package com.match.app.ui.verification

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class VerificationUi(
    val isPhoneVerified: Boolean = false,
    val isIdVerified: Boolean = false,
    val isProfileComplete: Boolean = false,
    val isPhotoAdded: Boolean = false,
    val isPremium: Boolean = false,
    val verificationStatus: String = "none", // none, pending, verified, rejected
    val rejectionReason: String = ""
)

private data class CheckItem(val icon: ImageVector, val title: String, val description: String, val done: Boolean)

private data class VerifMethod(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val badge: String,
    val badgeColor: Color
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val ui: StateFlow<VerificationUi> = session.userId.filterNotNull()
        .map { uid ->
            val p = auth.currentProfile(uid)
            val fbUid = p?.firebaseUid ?: ""
            val currentUser = FirebaseAuth.getInstance().currentUser
            
            // Check verification status from Firestore
            val status = if (fbUid.isNotBlank()) {
                try {
                    val doc = firestore.collection("verificationRequests")
                        .document(fbUid).get().await()
                    doc.getString("status") ?: "none"
                } catch (_: Exception) { "none" }
            } else "none"
            val reason = if (fbUid.isNotBlank()) {
                try {
                    val doc = firestore.collection("verificationRequests")
                        .document(fbUid).get().await()
                    doc.getString("rejectionReason") ?: ""
                } catch (_: Exception) { "" }
            } else ""

            VerificationUi(
                isPhoneVerified  = currentUser?.phoneNumber != null || p?.phoneNumber?.isNotBlank() == true,
                isIdVerified     = (p?.verificationLevel ?: 0) >= 2 || status == "verified",
                isProfileComplete= p != null && p.bio.isNotBlank() && p.city.isNotBlank(),
                isPhotoAdded     = (p?.primaryPhotoPath != null || (p?.photoUrl ?: "").isNotBlank()),
                isPremium        = p?.isPremium ?: false,
                verificationStatus = status,
                rejectionReason = reason
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, VerificationUi())

    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    private val _submitted = MutableStateFlow(false)
    val submitted = _submitted.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /**
     * Uploads the verification document to Firebase Storage and creates
     * a verification request in Firestore for admin review.
     */
    fun submitVerification(docType: String, documentUri: Uri? = null) {
        _submitting.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("Not signed in")

                // Upload document to Firebase Storage if URI provided
                val docUrl = if (documentUri != null) {
                    val ref = storage.reference
                        .child("verification/$uid/${docType}_${System.currentTimeMillis()}")
                    ref.putFile(documentUri).await()
                    ref.downloadUrl.await().toString()
                } else ""

                // Create verification request in Firestore
                val request = hashMapOf(
                    "uid" to uid,
                    "docType" to docType,
                    "docUrl" to docUrl,
                    "status" to "pending",
                    "submittedAt" to com.google.firebase.Timestamp.now(),
                    "reviewedAt" to null,
                    "rejectionReason" to ""
                )
                firestore.collection("verificationRequests")
                    .document(uid)
                    .set(request)
                    .await()

                _submitting.value = false
                _submitted.value = true
            } catch (e: Exception) {
                _submitting.value = false
                _error.value = e.message ?: "Verification submission failed"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    onBack: () -> Unit = {},
    vm: VerificationViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val submitted by vm.submitted.collectAsState()
    var selectedDocType by remember { mutableStateOf("Aadhaar") }
    val checks = listOf(
        CheckItem(Icons.Filled.PhoneAndroid, "Phone verified",  "Your mobile number has been confirmed",               ui.isPhoneVerified),
        CheckItem(Icons.Filled.Badge,        "ID verified",     "Government ID checked and approved",                  ui.isIdVerified),
        CheckItem(Icons.Filled.Person,       "Profile complete","Bio, city, education and other details filled in",    ui.isProfileComplete),
        CheckItem(Icons.Filled.PhotoCamera,  "Photo added",     "At least one profile photo uploaded",                 ui.isPhotoAdded),
        CheckItem(Icons.Filled.Star,         "Premium member",  "Active paid subscription for priority trust signals", ui.isPremium),
    )
    val passed = checks.count { it.done }
    val total  = checks.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("trust_verification", "Trust & Verification")) },
                navigationIcon = { IconButton(onClick = onBack, modifier = Modifier.testTag("verification_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                .testTag("verification_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Trust score circle ───────────────────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(t("trust_score", "Trust Score"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val pct = (passed.toFloat() / total * 100).toInt()
                    Text("$pct%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold,
                        color = if (pct >= 80) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary)
                    Text("$passed of $total checks passed", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { passed.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (pct >= 80) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Checklist ────────────────────────────────────────────────
            Text(t("verification_checklist", "Verification Checklist"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            checks.forEach { (icon, title, desc, done) ->
                VerificationRow(icon, title, desc, done)
            }
            HorizontalDivider()

            // ── Why verify ───────────────────────────────────────────────
            Text(t("why_verify", "Why verify your profile?"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            listOf(
                "Get a ✓ verified badge visible to all matches",
                "Appear higher in search results",
                "Receive 3× more interest requests",
                "Build trust with families before meeting"
            ).forEach { benefit ->
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp).padding(top = 2.dp),
                        tint = Color(0xFF2E7D32))
                    Text(benefit, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Verification status ──────────────────────────────────────
            when {
                submitted || ui.verificationStatus == "pending" -> {
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.HourglassTop, null, Modifier.size(24.dp), tint = Color(0xFFE65100))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(t("verification_pending", "Verification Pending"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(t("verification_pending_desc", "Your document has been submitted. We'll review within 24-48 hours."),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                ui.verificationStatus == "rejected" -> {
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Error, null, Modifier.size(24.dp), tint = Color(0xFFC62828))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(t("verification_rejected", "Verification Rejected"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
                                if (ui.rejectionReason.isNotBlank())
                                    Text("Reason: ${ui.rejectionReason}", style = MaterialTheme.typography.bodySmall)
                                Text(t("verification_rejected_hint", "Please resubmit with a clear document."), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                ui.verificationStatus == "verified" -> {
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, null, Modifier.size(24.dp), tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("ID Verified", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                                Text("Your identity has been verified.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // ── Document type selector ───────────────────────────────────
            if (ui.verificationStatus != "verified" && !submitted) {
                Text(t("select_verification_method", "Select Verification Method"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                // Government ID
                Text("Government ID", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Aadhaar", "Passport", "PAN Card", "Voter ID").forEach { docType ->
                        FilterChip(
                            selected = selectedDocType == docType,
                            onClick = { selectedDocType = docType },
                            label = { Text(docType, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (selectedDocType == docType) {{ Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }} else null
                        )
                    }
                }

                // Professional
                Text("Professional (optional, additional boost)", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LinkedIn", "Work Email").forEach { method ->
                        FilterChip(
                            selected = selectedDocType == method,
                            onClick = { selectedDocType = method },
                            label = { Text(method, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (selectedDocType == method) {{ Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }} else null
                        )
                    }
                }

                // Video
                Text("Video Verification (fastest, highest trust)", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedDocType == "Video Selfie",
                        onClick = { selectedDocType = "Video Selfie" },
                        label = { Text("Video Selfie", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (selectedDocType == "Video Selfie") {{ Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }} else null
                    )
                }

                Button(
                    onClick = { vm.submitVerification(selectedDocType) },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("verification_request_btn")
                ) {
                    if (submitting) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(t("submitting", "Submitting…"))
                    } else {
                        Icon(Icons.Filled.Upload, null)
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
private fun VerificationRow(icon: ImageVector, title: String, description: String, done: Boolean) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (done) Color(0xFF2E7D32).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(20.dp),
                        tint = if (done) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (done) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(20.dp), tint = Color(0xFF2E7D32))
            } else {
                Icon(Icons.Filled.RadioButtonUnchecked, null, Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
