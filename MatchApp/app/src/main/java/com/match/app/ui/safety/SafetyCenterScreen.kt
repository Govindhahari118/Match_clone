package com.match.app.ui.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

private data class SafetyControl(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val SAFETY_CONTROLS = listOf(
    SafetyControl(
        Icons.Filled.Block,
        "Block",
        "Blocking is enforced by the backend across discovery, profile access, interactions, chat, contact access, protected media and notifications."
    ),
    SafetyControl(
        Icons.Filled.Report,
        "Report",
        "Reports create a real moderation record. Serious reports are routed for review rather than being silently treated as resolved."
    ),
    SafetyControl(
        Icons.Filled.Lock,
        "Contact and photo privacy",
        "Paying for a plan does not override another member's contact or protected-photo permissions."
    ),
    SafetyControl(
        Icons.Filled.LocationOff,
        "Location privacy",
        "Nearby uses foreground location only and does not expose another member's raw coordinates."
    ),
    SafetyControl(
        Icons.Filled.Verified,
        "Granular verification",
        "Phone, email, photo and identity signals are shown separately when actually verified; verification is not presented as a guarantee of honesty or compatibility."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCenterScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("safety_center", "Safety Center")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("safety_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("safety_center_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Safety controls backed by real platform state",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Matree does not publish invented safety scores, fake fraud-detection statistics, or claim end-to-end encryption when it is not implemented.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SAFETY_CONTROLS.forEach { control ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(control.icon, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(control.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                control.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text(
                "Safety reminder: verify the person independently before sending money or sharing sensitive information. Use Block or Report from the relevant profile or conversation when needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
