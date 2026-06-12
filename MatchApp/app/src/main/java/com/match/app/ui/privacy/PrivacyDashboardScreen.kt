package com.match.app.ui.privacy

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(private val session: SessionStore) : ViewModel() {
    val incognito = session.incognitoMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    fun setIncognito(v: Boolean) = viewModelScope.launch { session.setIncognitoMode(v) }
}

private val INDIGO = Color(0xFF283593)

/* ── Data ─────────────────────────────────────────────────────────────── */

private data class PrivacyToggle(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val key: String,
    val isPremium: Boolean = false
)

private val PHOTO_PRIVACY = listOf(
    PrivacyToggle(Icons.Filled.BlurOn, "Blur Photos for Non-Matches",
        "Only accepted interests can see clear photos", "blur_photos"),
    PrivacyToggle(Icons.Filled.Visibility, "Show Photos to Premium Only",
        "Free users see a blurred placeholder", "photos_premium_only", isPremium = true),
    PrivacyToggle(Icons.Filled.Security, "Watermark All Photos",
        "Add 'Shared via Match' watermark to prevent misuse", "watermark"),
    PrivacyToggle(Icons.Filled.Screenshot, "Block Screenshot",
        "Prevent screenshots of your profile & photos", "block_screenshot")
)

private val PROFILE_PRIVACY = listOf(
    PrivacyToggle(Icons.Filled.VisibilityOff, "Hide Profile from Search",
        "Only people you shortlist can see your profile", "hide_search", isPremium = true),
    PrivacyToggle(Icons.Filled.PersonOff, "Incognito Browsing",
        "Browse profiles without appearing in 'Who Viewed'", "incognito", isPremium = true),
    PrivacyToggle(Icons.Filled.PhoneDisabled, "Hide Contact Info",
        "Phone and email visible only after mutual acceptance", "hide_contact"),
    PrivacyToggle(Icons.Filled.LocationOff, "Hide City/Location",
        "Show only state/region instead of exact city", "hide_city")
)

private val COMMUNICATION_PRIVACY = listOf(
    PrivacyToggle(Icons.Filled.DoNotDisturb, "Limit Daily Messages",
        "Max 10 messages per day to prevent spam", "msg_limit"),
    PrivacyToggle(Icons.Filled.Block, "Auto-Block Suspicious",
        "Automatically block users with reported behaviour", "auto_block"),
    PrivacyToggle(Icons.Filled.FilterAlt, "Interest Request Filter",
        "Only verified or premium users can send interest", "interest_filter", isPremium = true),
    PrivacyToggle(Icons.Filled.Timer, "Read Receipt Control",
        "Don't show when you've read messages", "read_receipts")
)

/* ── Screen ───────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    onBack: () -> Unit = {},
    onGoSettings: () -> Unit = {},
    vm: PrivacyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val incognitoState by vm.incognito.collectAsState()
    val toggleStates = remember {
        mutableStateMapOf(
            "blur_photos" to true,
            "watermark" to true,
            "block_screenshot" to true,
            "hide_contact" to true,
            "msg_limit" to false,
            "auto_block" to true,
            "read_receipts" to false
        )
    }
    // Keep incognito in sync with persisted state
    LaunchedEffect(incognitoState) { toggleStates["incognito"] = incognitoState }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("privacy_safety", "Privacy & Safety")) },
                navigationIcon = {
                    IconButton(onClick = onBack, Modifier.testTag("privacy_dash_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("privacy_dashboard_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Privacy score ──
            val enabledCount = toggleStates.count { it.value }
            val totalCount = PHOTO_PRIVACY.size + PROFILE_PRIVACY.size + COMMUNICATION_PRIVACY.size
            val score = ((enabledCount.toFloat() / totalCount) * 100).toInt().coerceIn(0, 100)

            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = INDIGO.copy(alpha = 0.06f))) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Shield, null, Modifier.size(36.dp), tint = INDIGO)
                    Text(t("privacy_score", "Privacy Score"), style = MaterialTheme.typography.titleMedium)
                    Text("$score%", style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            score >= 70 -> Color(0xFF2E7D32)
                            score >= 40 -> Color(0xFFE65100)
                            else -> Color(0xFFC62828)
                        })
                    LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = when {
                            score >= 70 -> Color(0xFF2E7D32)
                            score >= 40 -> Color(0xFFE65100)
                            else -> Color(0xFFC62828)
                        }
                    )
                    Text("$enabledCount of $totalCount privacy controls enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Photo Privacy ──
            PrivacySection("Photo Privacy", Icons.Filled.PhotoCamera, PHOTO_PRIVACY, toggleStates)

            // ── Profile Privacy ──
            PrivacySection("Profile Visibility", Icons.Filled.Person, PROFILE_PRIVACY, toggleStates,
                onSpecialToggle = { key, value -> if (key == "incognito") vm.setIncognito(value) })

            // ── Communication Privacy ──
            PrivacySection("Communication Safety", Icons.AutoMirrored.Filled.Chat, COMMUNICATION_PRIVACY, toggleStates)

            // ── Blocked users ──
            ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Block, null, tint = Color(0xFFC62828))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("blocked_users", "Blocked Users"), style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text("3 users blocked", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Report abuse ──
            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
            ) {
                Icon(Icons.Filled.Flag, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(t("report_abuse", "Report Abuse or Harassment"))
            }

            // ── GDPR / DPDP Rights ──────────────────────────────────────
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = INDIGO.copy(alpha = 0.06f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Gavel, null, Modifier.size(22.dp), tint = INDIGO)
                        Spacer(Modifier.width(10.dp))
                        Text("Your Data Rights (GDPR / DPDP Act)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Under GDPR and India's Digital Personal Data Protection Act (DPDP) 2023, " +
                        "you have the right to access, correct, export, and delete your personal data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Export my data
                    OutlinedButton(
                        onClick = {
                            // Build a plain-text data summary and share via system share sheet
                            val shareText = buildString {
                                appendLine("=== MatrimonyConnect Data Export ===")
                                appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
                                appendLine()
                                appendLine("Your profile, messages, and activity data are stored locally on this device.")
                                appendLine("To request a full server-side export, please email: privacy@matrimonyconnect.com")
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "My MatrimonyConnect Data Export")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export my data"))
                        },
                        modifier = Modifier.fillMaxWidth().testTag("privacy_export_data")
                    ) {
                        Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export my data")
                    }
                    // Delete account link
                    TextButton(
                        onClick = onGoSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828))
                    ) {
                        Icon(Icons.Filled.DeleteForever, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Request account deletion → Settings")
                    }
                    Text(
                        "Data retention: Your data is deleted within 30 days of account deletion request.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ── Section composable ───────────────────────────────────────────────── */

@Composable
private fun PrivacySection(
    title: String,
    @Suppress("UNUSED_PARAMETER") sectionIcon: ImageVector,
    toggles: List<PrivacyToggle>,
    states: MutableMap<String, Boolean>,
    onSpecialToggle: (key: String, value: Boolean) -> Unit = { _, _ -> }
) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    toggles.forEach { toggle ->
        val enabled = states[toggle.key] ?: false
        ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(toggle.icon, null, Modifier.size(22.dp),
                    tint = if (enabled) INDIGO else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(toggle.title, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium)
                        if (toggle.isPremium) {
                            Spacer(Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFD600)) {
                                Text("PRO", style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold, color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                            }
                        }
                    }
                    Text(toggle.subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { newVal ->
                        states[toggle.key] = newVal
                        onSpecialToggle(toggle.key, newVal)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = INDIGO)
                )
            }
        }
    }
}
