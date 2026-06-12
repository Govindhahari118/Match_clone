package com.match.app.ui.counselling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.ui.i18n.t

private val TEAL = Color(0xFF00796B)

private data class Counsellor(val name: String, val title: String, val rating: Double,
                              val sessions: Int, val langs: String, val emoji: String)
private val COUNSELLORS = listOf(
    Counsellor("Dr. Anita Verma", "Relationship Therapist", 4.9, 1240, "Hindi, English", "👩‍⚕️"),
    Counsellor("Rahul Menon", "Pre-marriage Coach", 4.8, 720, "English, Tamil", "🧑‍⚕️"),
    Counsellor("Dr. Priya Nair", "Divorce Recovery Specialist", 4.9, 950, "English, Malayalam", "👩‍⚕️"),
    Counsellor("Advocate Suresh K.", "Legal & Family Law", 4.7, 430, "Hindi, English", "🧑‍⚖️")
)
private data class SessionType(val title: String, val desc: String, val icon: ImageVector, val freeFirst: Boolean)
private val SESSIONS = listOf(
    SessionType("Pre-Marriage", "Compatibility, expectations, communication",
        Icons.Filled.Favorite, true),
    SessionType("Post-Loss", "Widow/Widower emotional support",
        Icons.Filled.SelfImprovement, true),
    SessionType("Divorce Recovery", "Healing after separation, rebuilding trust",
        Icons.Filled.Psychology, true),
    SessionType("Family Pressure", "Handling family expectations & boundaries",
        Icons.Filled.Groups, false),
    SessionType("Legal Checklist", "Second-marriage legal, custody, property",
        Icons.Filled.Gavel, false),
    SessionType("Communication", "Talking to potential matches with confidence",
        Icons.AutoMirrored.Filled.Chat, false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounsellingScreen(
    onBack: () -> Unit = {},
    vm: CounsellingViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    var selectedSession by remember { mutableStateOf<SessionType?>(null) }
    var mode by remember { mutableStateOf("Video") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("counselling", "Counselling")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("counsel_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        if (ui.booked) {
            Box(Modifier.padding(pad).fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = Color(0xFF2E7D32).copy(alpha = 0.1f), modifier = Modifier.size(100.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CheckCircle, null, Modifier.size(60.dp), tint = Color(0xFF2E7D32))
                        }
                    }
                    Text("Session Booked!", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("Your counsellor will reach out within 2 hours to confirm the slot.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { vm.resetBooking(); onBack() },
                        colors = ButtonDefaults.buttonColors(containerColor = TEAL)) {
                        Text(t("back_home", "Back to Home"))
                    }
                }
            }
        } else {
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(16.dp).testTag("counselling_screen"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = TEAL.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SupportAgent, null, tint = TEAL, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Talk to a certified counsellor",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                color = TEAL)
                        }
                        Text("Your first session is free. 100% confidential. Judgement-free.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text("What would you like help with?",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SESSIONS.forEach { s ->
                    val sel = selectedSession == s
                    ElevatedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                            .testTag("session_${s.title.replace(' ', '_')}"),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (sel) TEAL.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        onClick = { selectedSession = s }
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(s.icon, null, tint = TEAL, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.title, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleSmall)
                                    if (s.freeFirst) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2E7D32)) {
                                            Text("1st FREE", style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                Text(s.desc, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (sel) Icon(Icons.Filled.CheckCircle, null, tint = TEAL)
                        }
                    }
                }

                Text("Our Counsellors",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                COUNSELLORS.forEach { c ->
                    ElevatedCard(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = TEAL.copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(c.emoji, style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleSmall)
                                Text(c.title, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFB300))
                                    Text(" ${c.rating} • ${c.sessions} sessions • ${c.langs}",
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Text("Session mode",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Video", "Phone", "Chat").forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m },
                            label = { Text(m) }, modifier = Modifier.weight(1f))
                    }
                }

                if (ui.error != null) {
                    Text(ui.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = {
                        vm.bookSession(
                            counsellor = COUNSELLORS.first().name, // Simplified for now
                            sessionType = selectedSession?.title ?: "General",
                            mode = mode
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("book_session_btn"),
                    enabled = selectedSession != null && !ui.loading,
                    colors = ButtonDefaults.buttonColors(containerColor = TEAL)
                ) {
                    if (ui.loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else {
                        Icon(Icons.Filled.EventAvailable, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (selectedSession?.freeFirst == true) "Book FREE session"
                             else "Book session (₹999)")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
