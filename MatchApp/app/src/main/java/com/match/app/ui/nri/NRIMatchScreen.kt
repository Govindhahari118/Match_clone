package com.match.app.ui.nri

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.match.app.ui.components.MatreeInfoCard
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeStatusTone

/**
 * NRI discovery is intentionally unavailable until it is backed by real server discovery,
 * reciprocal preference filtering, privacy authorization and production inventory.
 *
 * This screen must never contain synthetic member names, fabricated profile counts or invented
 * verification percentages. Route promotion is governed by docs/production-readiness/route-inventory.md.
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun NRIMatchScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NRI discovery") },
                navigationIcon = {
                    IconButton(onClick = onBack, Modifier.testTag("nri_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .testTag("nri_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "NRI discovery is not available yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "The account model can store residence, citizenship and relocation preferences, " +
                    "but Matree will not present an NRI catalogue until the server-side discovery " +
                    "contract and real production inventory have passed release validation.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            MatreeInfoCard {
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "No synthetic profiles or invented market statistics",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Only real, authorized member data may appear when this feature is promoted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            MatreeInlineNotice(
                message = "This feature remains hidden from the production shell until its end-to-end release gate passes.",
                icon = Icons.Filled.Public,
                tone = MatreeStatusTone.NEUTRAL,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
