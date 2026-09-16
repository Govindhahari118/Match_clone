package com.match.app.ui.boost

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.match.app.ui.components.MatreeScreen
import com.match.app.ui.components.MatreeUnavailableFeature

/** Boost purchase is hidden until ranking, entitlement and billing are server-authoritative. */
@Composable
fun ProfileBoostScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    MatreeScreen(title = "Profile Boost", onBack = onBack) { padding ->
        MatreeUnavailableFeature(
            modifier = Modifier.padding(padding),
            title = "Profile Boost is not active yet",
            description = "The prototype activated boosts locally and displayed invented view multipliers without a paid entitlement or ranking guarantee. Production UI now waits for a real boost service.",
            icon = Icons.Filled.Bolt,
            requirements = listOf(
                "Define how boosted ranking works and ensure it does not bypass privacy or safety filters.",
                "Create server-authoritative boost start/expiry records rather than changing a local timestamp.",
                "Connect every paid plan to verified Google Play/payment entitlements and restore/refund flows.",
                "Measure real uplift before displaying any 'more views' performance claim.",
                "Make boost badges and search placement derive from the trusted backend."
            ),
            onHelp = onOpenHelp
        )
    }
}
