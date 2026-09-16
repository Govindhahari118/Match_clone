package com.match.app.ui.securecall

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PhoneLocked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

/**
 * Production availability state for masked calling.
 *
 * A prior prototype simulated balances, call history, plans and an active call without a telephony
 * provider. This screen now refuses to represent those simulations as a working paid service.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureCallScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("secure_calls", "Secure Calls"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("securecall_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp).testTag("secure_call_screen"),
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
                            Icon(Icons.Filled.PhoneLocked, null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Text("Masked calling is not active yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "A real secure-call experience requires a configured calling provider, server-issued session/relay credentials, metering and verified billing. Matree will not simulate those as completed calls or sell fake minute balances.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text("Production requirements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Requirement(Icons.Filled.Call, "Provider-backed calling", "Calls must be created and terminated by the configured communication provider—not by local UI state.")
            Requirement(Icons.Filled.Security, "Number privacy", "Number masking and retention rules must be validated end to end before the feature is advertised as private.")
            Requirement(Icons.Filled.PhoneLocked, "Real usage and billing", "Balances, duration and pricing must come from trusted server/payment records before purchase controls are enabled.")

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

@Composable
private fun Requirement(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
