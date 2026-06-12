package com.match.app.ui.safety

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.match.app.ui.i18n.t
import androidx.compose.ui.unit.dp

private val RED = Color(0xFFC62828)

private data class SafetyFeature(val icon: ImageVector, val title: String, val desc: String, val enabled: Boolean, val auto: Boolean = false)
private data class FraudSignal(val label: String, val risk: String, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCenterScreen(onBack: () -> Unit = {}) {
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    val safetyScore = 85
    val features = remember { listOf(
        SafetyFeature(Icons.Filled.Shield, "AI Fraud Detection", "Automatically scans profiles for fake photos, inconsistent info, and known scam patterns", true, true),
        SafetyFeature(Icons.Filled.PhotoCamera, "Photo Verification", "Cross-references uploaded photos with video selfie to ensure authenticity", true, true),
        SafetyFeature(Icons.Filled.Block, "Auto-Block Scammers", "Instantly blocks users who send suspicious links or request money", true, true),
        SafetyFeature(Icons.Filled.Warning, "Suspicious Activity Alerts", "Get notified when a match exhibits red-flag behaviour patterns", true),
        SafetyFeature(Icons.Filled.LocationOff, "Location Privacy", "Your exact location is never shared — only city-level info", true),
        SafetyFeature(Icons.Filled.Security, "End-to-End Chat Encryption", "All messages are encrypted in transit and at rest", true, true),
        SafetyFeature(Icons.Filled.Screenshot, "Screenshot Protection", "FLAG_SECURE prevents screenshots on all profile screens", true, true),
        SafetyFeature(Icons.Filled.Fingerprint, "Biometric Lock", "Fingerprint/face authentication required to open app", true),
        SafetyFeature(Icons.Filled.Report, "One-Tap Report", "Report any profile in 2 taps — reviewed within 24 hours", true),
        SafetyFeature(Icons.Filled.Timer, "Auto-Delete Messages", "Option to auto-delete chat history after 30 days", false)
    )}

    val fraudSignals = remember { listOf(
        FraudSignal("Profiles flagged this month", "142 blocked", Color(0xFFC62828)),
        FraudSignal("Photo verification success rate", "99.4%", Color(0xFF2E7D32)),
        FraudSignal("Average scam detection time", "< 2 hours", Color(0xFF2E7D32)),
        FraudSignal("False positive rate", "0.3%", Color(0xFF2E7D32)),
        FraudSignal("User-reported profiles actioned", "100%", Color(0xFF2E7D32))
    )}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("safety_center", "Safety Center")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("safety_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("safety_center_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Hero
            Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.horizontalGradient(listOf(RED, Color(0xFF880E4F)))).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.HealthAndSafety, null, Modifier.size(40.dp), tint = Color.White)
                        Text("Your Safety Score: $safetyScore%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("We use AI, biometrics, and encryption to keep you safe.\nYour safety is our #1 priority.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), textAlign = TextAlign.Center)
                        LinearProgressIndicator(progress = { safetyScore / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Color.White, trackColor = Color.White.copy(0.3f))
                    }
                }
            }

            // AI Fraud Detection stats
            Text(t("ai_fraud_detection", "AI Fraud Detection"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    fraudSignals.forEach { s ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Surface(shape = RoundedCornerShape(6.dp), color = s.color.copy(0.12f)) {
                                Text(s.risk, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = s.color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
            }

            // Safety features
            Text(t("active_safety_features", "Active Safety Features"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            features.forEach { f ->
                ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(f.icon, null, Modifier.size(22.dp), tint = if (f.enabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(f.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                if (f.auto) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E7D32).copy(0.12f)) {
                                        Text("AUTO", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }
                            Text(f.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (f.enabled) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, Modifier.size(20.dp), tint = if (f.enabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Emergency contact
            Text(t("emergency_contact", "Emergency Contact"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Add a trusted contact who can be notified if you feel unsafe during a meeting.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = emergencyName, onValueChange = { emergencyName = it }, label = { Text("Contact Name") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Person, null) })
                    OutlinedTextField(value = emergencyPhone, onValueChange = { emergencyPhone = it.filter { c -> c.isDigit() }.take(10) }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Text("+91  ") })
                    Button(onClick = { saved = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RED), enabled = emergencyName.isNotBlank() && emergencyPhone.length == 10 && !saved) {
                        Text(if (saved) "Saved ✓" else "Save Emergency Contact")
                    }
                }
            }

            // Safety tips
            Text(t("safety_tips", "Safety Tips for Meeting"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf(
                "Always meet in a public place for the first time",
                "Inform a friend or family member about your meeting",
                "Use our Secure Call feature — never share your real number",
                "Trust your instincts — if something feels off, leave",
                "Report any suspicious behaviour immediately",
                "Never send money to someone you haven't met in person"
            ).forEachIndexed { i, tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Surface(shape = CircleShape, color = RED, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(tip, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
