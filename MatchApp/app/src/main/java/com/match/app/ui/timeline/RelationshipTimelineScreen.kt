package com.match.app.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private data class JourneyStage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val SUGGESTED_STAGES = listOf(
    JourneyStage(Icons.Filled.PersonAdd, "Complete your profile", "Keep identity, preferences and important background details accurate and current."),
    JourneyStage(Icons.Filled.Verified, "Build trust", "Use available verification tools and independently verify important information before major decisions."),
    JourneyStage(Icons.Filled.Favorite, "Express mutual interest", "Move forward when both people choose to connect rather than relying on one-sided assumptions."),
    JourneyStage(Icons.Filled.Chat, "Talk about expectations", "Discuss values, family, career, finances, location and future plans at a comfortable pace."),
    JourneyStage(Icons.Filled.VideoCall, "Meet safely", "Use a controlled video or in-person meeting and follow the Safety Center guidance."),
    JourneyStage(Icons.Filled.Groups, "Involve families when appropriate", "Choose the timing together and keep both members' consent at the center."),
    JourneyStage(Icons.Filled.Celebration, "Plan next steps together", "Engagement, marriage and other milestones should reflect decisions you have actually made—not app-generated assumptions.")
)

/**
 * Until a real event/activity repository is connected, this screen is a neutral journey guide.
 * It deliberately does not fabricate completed milestones, dates, partners or meeting locations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipTimelineScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("your_journey", "Relationship Journey"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("timeline_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("timeline_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("A guide, not an invented history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Your real timeline will appear only when it can be built from actual, consented activity. For now, use these stages as a planning guide.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            SUGGESTED_STAGES.forEachIndexed { index, stage ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    ElevatedCard(modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(stage.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(stage.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    stage.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
