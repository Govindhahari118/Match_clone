package com.match.app.ui.events

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private val PURPLE = Color(0xFF6A1B9A)
private val AMBER  = Color(0xFFF57F17)

/* ── Data models ──────────────────────────────────────────────────────── */

private enum class EventType(val label: String, val icon: ImageVector, val color: Color) {
    SPEED_DATE("Speed Dating",   Icons.Filled.Timer,     Color(0xFFE91E63)),
    MIXER("Digital Mixer",       Icons.Filled.Groups,    Color(0xFF1565C0)),
    WORKSHOP("Workshop",         Icons.Filled.School,    Color(0xFF2E7D32)),
    GAMES("Ice Breaker Games",   Icons.Filled.SportsEsports, Color(0xFFFF6F00)),
    COMMUNITY("Community Meet",  Icons.Filled.Diversity3, Color(0xFF6A1B9A))
}

private data class LiveEvent(
    val id: Int,
    val title: String,
    val type: EventType,
    val date: String,
    val time: String,
    val attendees: Int,
    val maxAttendees: Int,
    val host: String,
    val description: String,
    val isFree: Boolean = false,
    val price: String = "₹199"
)

private val UPCOMING_EVENTS = listOf(
    LiveEvent(1, "Sunday Speed Dating — 25-32 yrs", EventType.SPEED_DATE,
        "Sun, 27 Apr", "7:00 PM", 42, 60, "Team Match",
        "5-minute video rounds. Meet 8 potential matches in 40 minutes. Most popular event!", isFree = false, price = "₹299"),
    LiveEvent(2, "Tamil Professionals Mixer", EventType.MIXER,
        "Sat, 26 Apr", "6:00 PM", 28, 40, "Chennai Circle",
        "Exclusive mixer for Tamil-speaking professionals. Structured icebreakers + free chat time.", isFree = false, price = "₹199"),
    LiveEvent(3, "Relationship 101 — Communication Skills", EventType.WORKSHOP,
        "Fri, 25 Apr", "8:00 PM", 65, 100, "Dr. Anita Verma",
        "Learn active listening, conflict resolution, and how to express needs constructively.", isFree = true),
    LiveEvent(4, "Would You Rather — Matrimony Edition", EventType.GAMES,
        "Thu, 24 Apr", "9:00 PM", 55, 80, "Match Events Team",
        "Fun icebreaker game where your answers reveal compatibility. Matched in pairs after!", isFree = true),
    LiveEvent(5, "Jain Community Virtual Meet", EventType.COMMUNITY,
        "Sun, 27 Apr", "11:00 AM", 18, 30, "Jain Sangam Group",
        "Curated introductions within the Jain community. Families welcome.", isFree = false, price = "₹149"),
    LiveEvent(6, "Second Innings Speed Dating — 35+ yrs", EventType.SPEED_DATE,
        "Sat, 3 May", "7:00 PM", 12, 30, "Team Match",
        "Designed for divorced, widowed, or separated individuals seeking a fresh start.", isFree = false, price = "₹249"),
    LiveEvent(7, "Telugu NRI Mixer — US/UK/Canada", EventType.MIXER,
        "Sun, 4 May", "10:00 AM IST", 22, 50, "NRI Connect",
        "Connect with Telugu-speaking NRIs from USA, UK, and Canada. Timezone-friendly.", isFree = false, price = "₹399"),
    LiveEvent(8, "Pre-Marriage Prep Workshop", EventType.WORKSHOP,
        "Wed, 30 Apr", "7:30 PM", 38, 60, "Rahul Menon",
        "Setting expectations, financial planning, and building a strong foundation.", isFree = true)
)

/* ── Screen ───────────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveEventsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("events", Context.MODE_PRIVATE) }
    var selectedType by remember { mutableStateOf<EventType?>(null) }
    var registeredIds by remember {
        mutableStateOf(
            prefs.getStringSet("registered", emptySet())
                ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        )
    }
    var showConfirm by remember { mutableStateOf<LiveEvent?>(null) }

    val filtered = UPCOMING_EVENTS.filter { selectedType == null || it.type == selectedType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("live_events", "Live Events")) },
                navigationIcon = {
                    IconButton(onClick = onBack, Modifier.testTag("events_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().testTag("live_events_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Hero banner ──
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        Modifier.background(Brush.horizontalGradient(listOf(PURPLE, Color(0xFFAD1457))))
                            .padding(24.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Celebration, null, Modifier.size(32.dp), tint = Color.White)
                                Spacer(Modifier.width(10.dp))
                                Text(t("meet_people_live", "Meet People Live"), style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text("Join virtual speed dating, mixers, workshops & games.\nMeet real verified profiles in real time.",
                                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.9f))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatChip("${UPCOMING_EVENTS.size}", "Events")
                                StatChip("${UPCOMING_EVENTS.sumOf { it.attendees }}+", "Registered")
                                StatChip("4.8★", "Avg Rating")
                            }
                        }
                    }
                }
            }

            // ── Type filter ──
            item {
                Text(t("browse_by_type", "Browse by type"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All") }
                    )
                    EventType.entries.take(3).forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = if (selectedType == t) null else t },
                            label = { Text(t.label, maxLines = 1) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventType.entries.drop(3).forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = if (selectedType == t) null else t },
                            label = { Text(t.label, maxLines = 1) }
                        )
                    }
                }
            }

            // ── Event cards ──
            items(filtered, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    isRegistered = event.id in registeredIds,
                    onRegister = { showConfirm = event }
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text(t("no_events_category", "No events in this category yet"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // ── Registration dialog ──
    showConfirm?.let { event ->
        AlertDialog(
            onDismissRequest = { showConfirm = null },
            icon = { Icon(event.type.icon, null, tint = event.type.color, modifier = Modifier.size(36.dp)) },
            title = { Text(t("register_for_event", "Register for event?"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(event.title, fontWeight = FontWeight.SemiBold)
                    Text("${event.date} at ${event.time}")
                    Text("Host: ${event.host}")
                    if (event.isFree)
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E7D32)) {
                            Text("  FREE  ", color = Color.White, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    else
                        Text("Entry: ${event.price}", fontWeight = FontWeight.SemiBold, color = PURPLE)
                    Text("${event.maxAttendees - event.attendees} spots remaining",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        registeredIds = registeredIds + event.id
                        prefs.edit().putStringSet("registered", registeredIds.map { it.toString() }.toSet()).apply()
                        showConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PURPLE)
                ) { Text(if (event.isFree) "Register — Free" else "Register — ${event.price}") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

/* ── Composables ──────────────────────────────────────────────────────── */

@Composable
private fun EventCard(event: LiveEvent, isRegistered: Boolean, onRegister: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = event.type.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(event.type.icon, null, Modifier.size(22.dp), tint = event.type.color)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(event.type.label, style = MaterialTheme.typography.labelSmall, color = event.type.color)
                }
                if (event.isFree) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E7D32)) {
                        Text("FREE", style = MaterialTheme.typography.labelSmall, color = Color.White,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                } else {
                    Text(event.price, fontWeight = FontWeight.Bold, color = PURPLE,
                        style = MaterialTheme.typography.labelMedium)
                }
            }

            Text(event.description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${event.date}, ${event.time}", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, null, Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("${event.attendees}/${event.maxAttendees}", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { event.attendees.toFloat() / event.maxAttendees },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (event.attendees > event.maxAttendees * 0.8) Color(0xFFE65100) else event.type.color
            )

            if (isRegistered) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text("Registered! You'll receive a reminder before the event.",
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32))
                    }
                }
            } else {
                Button(
                    onClick = onRegister,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = event.type.color)
                ) {
                    Icon(Icons.Filled.EventAvailable, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Register Now")
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}
