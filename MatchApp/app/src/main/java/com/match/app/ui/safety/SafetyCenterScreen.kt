package com.match.app.ui.safety

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private data class SafetyGuidance(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val BEFORE_CONNECTING = listOf(
    SafetyGuidance(
        Icons.Filled.VerifiedUser,
        "Review the whole profile",
        "Verification badges and complete profile details can add context, but they are not a guarantee. Take time to verify important claims independently."
    ),
    SafetyGuidance(
        Icons.Filled.MoneyOff,
        "Never send money",
        "Do not transfer money, share banking credentials, OTPs, card details or investment access with someone you met through the app."
    ),
    SafetyGuidance(
        Icons.Filled.Call,
        "Keep early communication controlled",
        "Use in-app communication options while you are still deciding whether you trust the other person."
    ),
    SafetyGuidance(
        Icons.Filled.Report,
        "Use report and block controls",
        "Stop the conversation and report behaviour that is threatening, manipulative, fraudulent or persistently unwanted."
    )
)

private val BEFORE_MEETING = listOf(
    SafetyGuidance(Icons.Filled.LocationOn, "Choose a public place", "For an initial meeting, choose a populated venue and arrange your own transport."),
    SafetyGuidance(Icons.Filled.Groups, "Tell someone you trust", "Share your plan, location and expected return time with a family member or trusted friend."),
    SafetyGuidance(Icons.Filled.Lock, "Protect personal information", "Avoid sharing documents, passwords, financial information or your home address before trust is established."),
    SafetyGuidance(Icons.Filled.Block, "Leave when you feel uncomfortable", "You do not owe anyone more time or personal information. End the meeting or conversation when something feels unsafe.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCenterScreen(
    onBack: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("safety_center", "Safety Center"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("safety_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("safety_center_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Security, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Safer choices, clearer controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "This page gives practical safety guidance. It does not invent a safety score or claim protections that have not been independently verified.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            SafetySection("Before connecting", BEFORE_CONNECTING)
            SafetySection("Before meeting in person", BEFORE_MEETING)

            Text("Privacy & support", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Review who can see your information and use support when you need help with an account, profile or interaction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(
                        onClick = onOpenPrivacy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open privacy controls")
                    }
                    OutlinedButton(
                        onClick = onOpenHelp,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Help, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Get help")
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    "If there is an immediate threat to your safety, contact local emergency services or someone you trust. Do not rely on an in-app feature for urgent emergency response.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SafetySection(title: String, items: List<SafetyGuidance>) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
