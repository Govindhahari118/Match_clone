package com.match.app.ui.meet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Security
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

private data class MeetRequirement(val icon: ImageVector, val title: String, val description: String)

private val REQUIREMENTS = listOf(
    MeetRequirement(Icons.Filled.Verified, "Mutual consent", "A meeting should be created only after both members have explicitly agreed to connect."),
    MeetRequirement(Icons.Filled.VideoCall, "Provider-backed session", "The call must use a configured video provider and server-issued meeting credentials rather than local UI simulation."),
    MeetRequirement(Icons.Filled.CalendarMonth, "Real scheduling", "Availability, invitations, reminders and cancellations must come from a shared production schedule."),
    MeetRequirement(Icons.Filled.Security, "Privacy validation", "Contact visibility, recording policy, retention and abuse controls must be verified before the service is advertised as private or secure.")
)

/** Honest production state while the real meeting service is not configured. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualMeetScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("virtual_meet", "Virtual Meet"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("meet_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp).testTag("virtual_meet_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.VideoCall, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Text("Virtual meetings are not active yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "The previous prototype sold local 'credits' and simulated scheduling without a real meeting provider or payment record. Those controls are intentionally disabled until the production service exists.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text("What must be ready first", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            REQUIREMENTS.forEach { requirement ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(requirement.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(requirement.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(requirement.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onOpenHelp,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.Help, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Contact support")
            }
        }
    }
}
