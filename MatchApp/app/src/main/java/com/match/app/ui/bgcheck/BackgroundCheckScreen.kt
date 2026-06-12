package com.match.app.ui.bgcheck

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.match.app.ui.i18n.t
import androidx.compose.ui.unit.dp

private val NAVY = Color(0xFF1A237E)

/* ── Data models ──────────────────────────────────────────────────────── */

private data class CheckPackage(
    val name: String,
    val price: String,
    val turnaround: String,
    val checks: List<String>,
    val highlighted: Boolean = false
)

private val PACKAGES = listOf(
    CheckPackage(
        "Basic Verify", "₹999", "3-5 days",
        listOf("Identity document verification", "Address verification", "Court record check (district level)",
               "Marital status verification")
    ),
    CheckPackage(
        "Professional", "₹2,499", "5-7 days",
        listOf("All Basic checks", "Employment verification", "Education verification",
               "Professional license check", "Social media audit", "Credit score range"),
        highlighted = true
    ),
    CheckPackage(
        "Premium 360°", "₹4,999", "7-10 days",
        listOf("All Professional checks", "Family background verification", "Property & asset check",
               "Criminal record check (national)", "Reference interviews (2 contacts)",
               "Detailed written report with score")
    )
)

private data class CheckStatus(
    val category: String,
    val icon: ImageVector,
    val status: String, // pending, passed, flagged, not_started
    val detail: String
)

/* ── Screen ───────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundCheckScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("bg_check", Context.MODE_PRIVATE) }
    var selectedPkg by remember { mutableStateOf("Professional") }
    var requested by remember { mutableStateOf(false) }
    var targetProfileId by remember { mutableStateOf("") }
    var showMyReport by remember { mutableStateOf(false) }
    val totalChecksRequested = prefs.getInt("totalChecks", 0)

    // Simulated check statuses for "My Report"
    val myChecks = remember {
        listOf(
            CheckStatus("Identity", Icons.Filled.Badge, "passed", "Aadhaar verified — name, DOB match"),
            CheckStatus("Address", Icons.Filled.Home, "passed", "Current address confirmed via utility bill"),
            CheckStatus("Education", Icons.Filled.School, "passed", "B.Tech from VIT verified"),
            CheckStatus("Employment", Icons.Filled.Work, "passed", "Currently employed at Infosys — confirmed"),
            CheckStatus("Court Records", Icons.Filled.Gavel, "passed", "No civil or criminal cases found"),
            CheckStatus("Marital Status", Icons.Filled.Favorite, "passed", "Never married — confirmed via affidavit"),
            CheckStatus("Family", Icons.Filled.Groups, "not_started", "Not yet requested"),
            CheckStatus("Credit Score", Icons.Filled.AccountBalance, "not_started", "Not yet requested")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("background_verification", "Background Verification")) },
                navigationIcon = {
                    IconButton(onClick = onBack, Modifier.testTag("bgcheck_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(onClick = { showMyReport = !showMyReport }) {
                        Text(if (showMyReport) "Request Check" else "My Report")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("bg_check_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showMyReport) {
                // ── My Verification Report ──
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFE8F5E9))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, null, Modifier.size(28.dp), tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(t("your_verification_report", "Your Verification Report"), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                Text("Last updated: 18 Apr 2026", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        val passed = myChecks.count { it.status == "passed" }
                        Text("$passed of ${myChecks.size} checks completed",
                            style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(
                            progress = { passed.toFloat() / myChecks.size },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                myChecks.forEach { check ->
                    CheckStatusRow(check)
                }

                OutlinedButton(onClick = { showMyReport = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Request check on a match")
                }
            } else {
                // ── Hero ──
                Surface(shape = RoundedCornerShape(20.dp), color = NAVY, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Shield, null, Modifier.size(40.dp), tint = Color.White)
                        Text(t("verify_before_you_trust", "Verify Before You Trust"), style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        Text("Get thorough background checks on your matches. Powered by certified verification agencies.",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f),
                            textAlign = TextAlign.Center)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("25,000+", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                                Text(t("checks_done", "Checks done"), style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("99.2%", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Accuracy", style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("3-10 days", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Turnaround", style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.7f))
                            }
                        }
                    }
                }

                // ── Why ──
                Text(t("why_bg_checks", "Why Background Checks?"), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                listOf(
                    "Verify identity, education, and employment claims",
                    "Check for criminal records and court cases",
                    "Confirm marital status with official records",
                    "Ensure financial stability and creditworthiness",
                    "Peace of mind for you and your family"
                ).forEach { reason ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp).padding(top = 2.dp),
                            tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text(reason, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // ── Profile ID input ──
                OutlinedTextField(
                    value = targetProfileId,
                    onValueChange = { targetProfileId = it.filter { c -> c.isDigit() }.take(10) },
                    label = { Text("Match Profile ID (e.g. M12345)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    prefix = { Text("M") }
                )

                // ── Packages ──
                Text(t("choose_package", "Choose a Package"), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                PACKAGES.forEach { pkg ->
                    PackageCard(pkg, selectedPkg == pkg.name) { selectedPkg = pkg.name }
                }

                // ── Request button ──
                if (!requested) {
                    Button(
                        onClick = {
                            if (targetProfileId.isNotBlank()) {
                                prefs.edit()
                                    .putInt("totalChecks", totalChecksRequested + 1)
                                    .putString("lastTarget", "M$targetProfileId")
                                    .putString("lastPackage", selectedPkg)
                                    .apply()
                                requested = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("request_bgcheck_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NAVY),
                        enabled = targetProfileId.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Shield, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Request $selectedPkg Check")
                    }
                } else {
                    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFF3E0))) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.HourglassTop, null, Modifier.size(36.dp), tint = Color(0xFFE65100))
                            Text("Check Initiated!", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("$selectedPkg check for profile M$targetProfileId has been submitted.\nYou'll receive the report within ${PACKAGES.first { it.name == selectedPkg }.turnaround}.",
                                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { requested = false; targetProfileId = "" },
                                colors = ButtonDefaults.buttonColors(containerColor = NAVY)) {
                                Text("Request Another")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/* ── Sub-composables ──────────────────────────────────────────────────── */

@Composable
private fun PackageCard(pkg: CheckPackage, selected: Boolean, onSelect: () -> Unit) {
    ElevatedCard(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected || pkg.highlighted)
            CardDefaults.elevatedCardColors(containerColor = NAVY.copy(alpha = 0.06f))
        else CardDefaults.elevatedCardColors(),
        elevation = if (selected) CardDefaults.elevatedCardElevation(8.dp)
                    else CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(pkg.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                            color = NAVY)
                        if (pkg.highlighted) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFF6F00)) {
                                Text("RECOMMENDED", style = MaterialTheme.typography.labelSmall,
                                    color = Color.White, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("${pkg.price} • ${pkg.turnaround}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RadioButton(selected = selected, onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = NAVY))
            }
            HorizontalDivider()
            pkg.checks.forEach { check ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(8.dp))
                    Text(check, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CheckStatusRow(check: CheckStatus) {
    val (bgColor, statusColor, statusText) = when (check.status) {
        "passed"  -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Passed")
        "flagged" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Flagged")
        "pending" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pending")
        else      -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Not started")
    }
    ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = bgColor, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(check.icon, null, Modifier.size(20.dp), tint = statusColor)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(check.category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(check.detail, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(6.dp), color = bgColor) {
                Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}
