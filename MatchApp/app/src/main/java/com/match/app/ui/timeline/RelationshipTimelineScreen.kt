package com.match.app.ui.timeline

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private val PINK = Color(0xFFC2185B)

private data class Milestone(val icon: ImageVector, val title: String, val date: String, val desc: String, val done: Boolean, val color: Color = PINK)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipTimelineScreen(onBack: () -> Unit = {}) {
    val milestones = remember { listOf(
        Milestone(Icons.Filled.PersonAdd, "Profile Created", "15 Mar 2026", "You joined Match and completed your profile", true, Color(0xFF2E7D32)),
        Milestone(Icons.Filled.Verified, "Profile Verified", "16 Mar 2026", "ID verification completed — Trust badge earned", true, Color(0xFF2E7D32)),
        Milestone(Icons.Filled.PhotoCamera, "Photos Uploaded", "16 Mar 2026", "Added 5 profile photos and a video intro", true, Color(0xFF2E7D32)),
        Milestone(Icons.Filled.Quiz, "Questionnaire Done", "17 Mar 2026", "Completed 50-question personality assessment", true, Color(0xFF2E7D32)),
        Milestone(Icons.Filled.Favorite, "First Interest Sent", "18 Mar 2026", "Sent your first interest request", true, Color(0xFF2E7D32)),
        Milestone(Icons.AutoMirrored.Filled.Chat, "First Conversation", "20 Mar 2026", "Started chatting with your first mutual match", true, Color(0xFF2E7D32)),
        Milestone(Icons.Filled.Phone, "First Call", "25 Mar 2026", "Had a 15-minute secure call with Priya", true, Color(0xFF2E7D32)),
        Milestone(Icons.Filled.Videocam, "Video Meet", "28 Mar 2026", "30-minute virtual meet — great connection!", true, Color(0xFF2E7D32)),
        Milestone(Icons.Filled.People, "Family Introduction", "5 Apr 2026", "Both families connected over video call", true, Color(0xFF6A1B9A)),
        Milestone(Icons.Filled.Restaurant, "First In-Person Meet", "12 Apr 2026", "Met at a café in Jubilee Hills, Hyderabad", true, Color(0xFF6A1B9A)),
        Milestone(Icons.Filled.FavoriteBorder, "Mutual Decision", "", "Both agree to proceed — families aligned", false),
        Milestone(Icons.Filled.EventAvailable, "Engagement", "", "Ring ceremony with both families", false),
        Milestone(Icons.Filled.Celebration, "Wedding", "", "The big day!", false),
        Milestone(Icons.Filled.AutoAwesome, "Share Your Story", "", "Inspire others by sharing your success story", false)
    )}
    val doneCount = milestones.count { it.done }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("your_journey", "Your Journey")) },
                navigationIcon = { IconButton(onClick = onBack, Modifier.testTag("timeline_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("timeline_screen"),
            verticalArrangement = Arrangement.spacedBy(0.dp)) {

            // Hero
            Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.horizontalGradient(listOf(PINK, Color(0xFF880E4F)))).padding(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("💕", style = MaterialTheme.typography.displayMedium)
                        Text(t("your_match_journey", "Your Match Journey"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        Text("$doneCount of ${milestones.size} milestones reached", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f))
                        LinearProgressIndicator(progress = { doneCount.toFloat() / milestones.size }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Color.White, trackColor = Color.White.copy(0.3f))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            // Timeline
            milestones.forEachIndexed { idx, m ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    // Timeline line + dot
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                        Surface(shape = CircleShape, color = if (m.done) m.color else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                if (m.done) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = Color.White)
                                else Text("${idx + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (idx < milestones.size - 1) {
                            Box(Modifier.width(2.dp).height(60.dp).background(if (m.done) m.color.copy(0.3f) else MaterialTheme.colorScheme.outlineVariant))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    // Content
                    ElevatedCard(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).padding(bottom = if (idx < milestones.size - 1) 8.dp else 0.dp),
                        colors = if (m.done) CardDefaults.elevatedCardColors() else CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(m.icon, null, Modifier.size(22.dp), tint = if (m.done) m.color else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(m.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (m.date.isNotBlank()) Text(m.date, style = MaterialTheme.typography.labelSmall, color = m.color)
                                Text(m.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
