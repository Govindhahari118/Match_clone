package com.match.app.ui.assisted

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.match.app.ui.components.MatreeScreen
import com.match.app.ui.components.MatreeUnavailableFeature

@Composable
fun AssistedServiceScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    MatreeScreen(title = "Assisted Matchmaking", onBack = onBack) { padding ->
        MatreeUnavailableFeature(
            modifier = Modifier.padding(padding),
            title = "Assisted matchmaking is not active yet",
            description = "Relationship-manager packages should not be sold until real staff assignment, service levels, communication records and billing entitlements are connected to the account.",
            icon = Icons.Filled.SupportAgent,
            requirements = listOf(
                "Create a server-backed service request and relationship-manager assignment workflow.",
                "Define service scope, response times, escalation and cancellation rules before advertising packages.",
                "Connect purchases to verified entitlements instead of local package-selection state.",
                "Expose only real manager identity, availability and communication history to the member.",
                "Add support/audit tooling for reassignment, complaints and refund handling."
            ),
            onHelp = onOpenHelp
        )
    }
}
