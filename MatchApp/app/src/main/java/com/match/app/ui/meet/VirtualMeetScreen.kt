package com.match.app.ui.meet

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private val BLUE = Color(0xFF1565C0)

private data class MeetStep(val num: Int, val icon: ImageVector, val title: String, val desc: String)
private val MEET_STEPS = listOf(
    MeetStep(1, Icons.Filled.FavoriteBorder, "Express Interest",
        "Send a Virtual Meet request to a profile that aligns with you."),
    MeetStep(2, Icons.Filled.ThumbUp, "Mutual Acceptance",
        "Once accepted, the connection becomes active for 24 hours."),
    MeetStep(3, Icons.Filled.Verified, "Verification",
        "Both members complete one verification (Gov ID, LinkedIn, Work Email, Video)."),
    MeetStep(4, Icons.Filled.CalendarMonth, "Schedule the Meet",
        "Self-schedule at your convenience. RM can assist if needed."),
    MeetStep(5, Icons.Filled.Videocam, "Meet Privately",
        "Connect securely on video. No contact numbers shared.")
)

private data class CreditPack(
    val credits: Int, val price: String, val perCredit: String, val badge: String?
)
private val CREDIT_PACKS = listOf(
    CreditPack(1, "₹500", "₹500 / meet", null),
    CreditPack(5, "₹2,000", "₹400 / meet", "SAVE 20%"),
    CreditPack(15, "₹5,000", "₹333 / meet", "BEST VALUE")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualMeetScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("virtual_meet", Context.MODE_PRIVATE) }
    var creditsOwned by remember { mutableIntStateOf(prefs.getInt("credits", 0)) }
    var rmAssist by remember { mutableStateOf(true) }
    var selectedSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("virtual_meet", "Virtual Meet")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("meet_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("virtual_meet_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Hero ──────────────────────────────────────────────
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = BLUE.copy(alpha = 0.08f))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.VideoCall, null, Modifier.size(48.dp), tint = BLUE)
                    Text("Meet safely — before sharing contact",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = BLUE, textAlign = TextAlign.Center)
                    Text("A private video call between two mutually interested, verified members. Your phone number is never visible.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrustPill("🔒", "No Contact Sharing")
                        TrustPill("✓", "100% Verified")
                    }
                }
            }

            // ── Credits balance ───────────────────────────────────
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Stars, null, Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("your_meet_credits", "Your Meet Credits"), style = MaterialTheme.typography.labelMedium)
                        Text("$creditsOwned credits available",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── How it works ──────────────────────────────────────
            Text("How a Virtual Meet Works",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            MEET_STEPS.forEach { StepRow(it) }

            // ── Credit packs ─────────────────────────────────────
            Text(t("buy_meet_credits", "Buy Meet Credits"),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Pay only when you're ready to meet — credits refundable if meeting doesn't occur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            CREDIT_PACKS.forEach { pack ->
                CreditPackCard(pack) {
                    creditsOwned += pack.credits
                    prefs.edit().putInt("credits", creditsOwned).apply()
                }
            }

            // ── Schedule a meet ──────────────────────────────────
            Text(t("schedule_a_meet", "Schedule a Meet"),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Surface(shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pick a day & time", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold)
                    val days = listOf("Today", "Tomorrow", "Sat", "Sun", "Mon", "Tue", "Wed")
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        days.forEachIndexed { i, d ->
                            FilterChip(
                                selected = selectedSlot?.first == i,
                                onClick = { selectedSlot = i to (selectedSlot?.second ?: -1) },
                                label = { Text(d) }
                            )
                        }
                    }
                    val times = listOf("10 AM", "1 PM", "5 PM", "8 PM")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        times.forEachIndexed { i, t ->
                            FilterChip(
                                selected = selectedSlot?.second == i,
                                onClick = { selectedSlot = (selectedSlot?.first ?: 0) to i },
                                label = { Text(t) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    // RM assist
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = rmAssist, onCheckedChange = { rmAssist = it })
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Let our RM briefly introduce both sides",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold)
                            Text("Optional ice-breaker at the start of the call",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = {
                            if (creditsOwned > 0) {
                                creditsOwned--
                                prefs.edit().putInt("credits", creditsOwned).apply()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("meet_schedule_btn"),
                        enabled = creditsOwned > 0 && selectedSlot?.first != null && selectedSlot?.second != null && selectedSlot!!.second >= 0,
                        colors = ButtonDefaults.buttonColors(containerColor = BLUE)
                    ) {
                        Icon(Icons.Filled.Videocam, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (creditsOwned > 0) "Schedule Meet (1 credit)" else "Buy credits to schedule")
                    }
                }
            }

            // ── Trust bar ────────────────────────────────────────
            ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Safety & Privacy Promise",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    SafetyLine("Your phone number is never visible")
                    SafetyLine("No time limit on the video call — talk freely")
                    SafetyLine("Post-call feedback is anonymous")
                    SafetyLine("Credit refund if meeting does not happen")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TrustPill(emoji: String, text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(emoji)
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                color = BLUE)
        }
    }
}

@Composable
private fun StepRow(step: MeetStep) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = BLUE, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("${step.num}", style = MaterialTheme.typography.labelLarge,
                    color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(step.icon, null, Modifier.size(16.dp), tint = BLUE)
                Spacer(Modifier.width(6.dp))
                Text(step.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(step.desc, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreditPackCard(pack: CreditPack, onBuy: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${pack.credits} Meet Credit${if (pack.credits > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    pack.badge?.let {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = BLUE) {
                            Text(it, style = MaterialTheme.typography.labelSmall,
                                color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(pack.perCredit, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(pack.price, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = BLUE)
            Spacer(Modifier.width(10.dp))
            FilledTonalButton(onClick = onBuy, modifier = Modifier.testTag("buy_pack_${pack.credits}")) {
                Text("Buy")
            }
        }
    }
}

@Composable
private fun SafetyLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Shield, null, Modifier.size(14.dp), tint = Color(0xFF2E7D32))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

