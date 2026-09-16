package com.match.app.ui.stories

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.components.EmptyState
import com.match.app.ui.i18n.t

/**
 * Production-safe success-story surface.
 *
 * The previous implementation shipped fictional couples and invented aggregate success metrics as
 * though they were verified platform data. Until a consented story feed is connected, the correct
 * UI is an honest empty state with publication standards rather than synthetic social proof.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessStoriesScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("success_stories", "Success Stories"), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("stories_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).testTag("success_stories_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary)
                        Text("Real stories only", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Stories should be published only with the couple's permission and a verified source. Matree does not display invented marriage counts, ratings or success statistics here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            EmptyState(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                icon = Icons.Filled.Favorite,
                title = "No published stories yet",
                subtitle = "Consented, verified member stories will appear here when the production story feed is available."
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PublicationRule(Icons.Filled.Verified, "Source verified before publication")
                    PublicationRule(Icons.Filled.PrivacyTip, "Explicit permission before names, photos or personal details are shown")
                }
            }
        }
    }
}

@Composable
private fun PublicationRule(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
