package com.match.app.ui.securecall

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.match.app.ui.i18n.t
import androidx.compose.ui.unit.dp

private val TEAL = Color(0xFF00695C)

/* ── Data ─────────────────────────────────────────────────────────────── */

private data class RecentCall(
    val name: String, val initial: Char, val duration: String,
    val time: String, val isMissed: Boolean = false
)

private val RECENT_CALLS = listOf(
    RecentCall("Priya Sharma", 'P', "12m 34s", "Today 6:30 PM"),
    RecentCall("Ananya Reddy", 'A', "8m 12s", "Today 3:15 PM"),
    RecentCall("Kavitha M.", 'K', "—", "Yesterday 7:00 PM", isMissed = true),
    RecentCall("Meera Patel", 'M', "22m 08s", "Yesterday 2:45 PM"),
    RecentCall("Deepa Nair", 'D', "5m 50s", "20 Apr, 8:00 PM")
)

private data class CallPlan(
    val name: String, val minutes: Int, val price: String,
    val perMin: String, val highlighted: Boolean = false
)

private val CALL_PLANS = listOf(
    CallPlan("Starter", 30, "₹149", "₹4.97/min"),
    CallPlan("Regular", 90, "₹349", "₹3.88/min", highlighted = true),
    CallPlan("Unlimited", 300, "₹799", "₹2.66/min")
)

/* ── Screen ───────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureCallScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("secure_call", Context.MODE_PRIVATE) }
    var remainingMins by remember { mutableIntStateOf(prefs.getInt("remainingMins", 0)) }
    var selectedPlan by remember { mutableStateOf("Regular") }
    var showDial by remember { mutableStateOf(false) }
    var callActive by remember { mutableStateOf(false) }
    var callTarget by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("secure_calls", "Secure Calls")) },
                navigationIcon = {
                    IconButton(onClick = onBack, Modifier.testTag("securecall_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        if (callActive) {
            // ── Active call UI ──
            Column(
                Modifier.padding(pad).fillMaxSize()
                    .background(Brush.verticalGradient(listOf(TEAL, Color(0xFF004D40))))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(shape = CircleShape, color = Color.White.copy(0.15f),
                    modifier = Modifier.size(100.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(callTarget.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(callTarget.ifBlank { "Match Profile" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(t("secure_call_number_hidden", "Secure Call — Number Hidden"),
                    style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                Spacer(Modifier.height(8.dp))
                Text("00:00", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(0.15f)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, null, Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("End-to-end masked • Your number is hidden",
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f))
                    }
                }
                Spacer(Modifier.height(40.dp))

                // Call controls
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    CallControl(Icons.Filled.MicOff, "Mute", Color.White.copy(0.2f)) {}
                    CallControl(Icons.AutoMirrored.Filled.VolumeUp, "Speaker", Color.White.copy(0.2f)) {}
                    CallControl(Icons.Filled.CallEnd, "End", Color(0xFFD32F2F)) {
                        callActive = false
                    }
                }
            }
        } else {
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(16.dp).testTag("secure_call_screen"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Hero ──
                Surface(shape = RoundedCornerShape(20.dp), color = TEAL, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.PhoneInTalk, null, Modifier.size(40.dp), tint = Color.White)
                        Text(t("talk_without_sharing", "Talk Without Sharing Numbers"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        Text("Make voice calls to matches through our secure relay.\nYour real phone number is never revealed.",
                            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f),
                            textAlign = TextAlign.Center)
                    }
                }

                // ── Balance card ──
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = TEAL.copy(alpha = 0.08f))) {
                    Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Your Balance", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$remainingMins minutes", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = TEAL)
                        }
                        FilledTonalButton(onClick = { showDial = true }) {
                            Icon(Icons.Filled.Phone, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call a Match")
                        }
                    }
                }

                // ── How it works ──
                Text(t("how_secure_calling_works", "How Secure Calling Works"), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                listOf(
                    Triple(Icons.Filled.DialerSip, "Dial via App", "Tap 'Call' on any match profile"),
                    Triple(Icons.Filled.Shield, "Number Masked", "Both parties see a temporary relay number"),
                    Triple(Icons.Filled.PhoneInTalk, "Talk Freely", "Crystal clear voice quality via PSTN relay"),
                    Triple(Icons.Filled.Lock, "Auto-Expire", "Relay number expires after call ends")
                ).forEachIndexed { i, (icon, title, desc) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Surface(shape = CircleShape, color = TEAL, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${i + 1}", style = MaterialTheme.typography.labelLarge,
                                    color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, Modifier.size(16.dp), tint = TEAL)
                                Spacer(Modifier.width(6.dp))
                                Text(title, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                            }
                            Text(desc, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // ── Recent calls ──
                Text(t("recent_calls", "Recent Calls"), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                RECENT_CALLS.forEach { call ->
                    ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = TEAL.copy(alpha = 0.12f),
                                modifier = Modifier.size(42.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${call.initial}", style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold, color = TEAL)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(call.name, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                                Text(call.time, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (call.isMissed)
                                Text("Missed", color = Color(0xFFD32F2F),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            else
                                Text(call.duration, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                callTarget = call.name; callActive = true
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Phone, null, tint = TEAL)
                            }
                        }
                    }
                }

                // ── Buy minutes ──
                Text(t("buy_call_minutes", "Buy Call Minutes"), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                CALL_PLANS.forEach { plan ->
                    ElevatedCard(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (selectedPlan == plan.name)
                            CardDefaults.elevatedCardColors(containerColor = TEAL.copy(alpha = 0.06f))
                        else CardDefaults.elevatedCardColors(),
                        onClick = { selectedPlan = plan.name }
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedPlan == plan.name,
                                onClick = { selectedPlan = plan.name },
                                colors = RadioButtonDefaults.colors(selectedColor = TEAL))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(plan.name, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleSmall)
                                    if (plan.highlighted) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFF6F00)) {
                                            Text("BEST VALUE", style = MaterialTheme.typography.labelSmall,
                                                color = Color.White, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                Text("${plan.minutes} mins • ${plan.perMin}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(plan.price, fontWeight = FontWeight.Bold, color = TEAL,
                                style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }

                Button(
                    onClick = {
                        val plan = CALL_PLANS.first { it.name == selectedPlan }
                        remainingMins += plan.minutes
                        prefs.edit().putInt("remainingMins", remainingMins).apply()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("buy_minutes_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = TEAL)
                ) {
                    Icon(Icons.Filled.ShoppingCart, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buy $selectedPlan — ${CALL_PLANS.first { it.name == selectedPlan }.price}")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ── Dial dialog ──
    if (showDial) {
        var dialId by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDial = false },
            icon = { Icon(Icons.Filled.Phone, null, tint = TEAL) },
            title = { Text("Call a Match") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the profile ID of the match you want to call.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = dialId,
                        onValueChange = { dialId = it.filter { c -> c.isDigit() }.take(10) },
                        label = { Text("Profile ID") },
                        prefix = { Text("M") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Balance: $remainingMins min remaining",
                        style = MaterialTheme.typography.labelSmall, color = TEAL)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        callTarget = "M$dialId"
                        callActive = true
                        showDial = false
                    },
                    enabled = dialId.isNotBlank() && remainingMins > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = TEAL)
                ) { Text("Call Now") }
            },
            dismissButton = { TextButton(onClick = { showDial = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CallControl(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, bg: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = bg)
        ) { Icon(icon, label, Modifier.size(28.dp), tint = Color.White) }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}
