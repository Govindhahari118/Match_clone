package com.match.app.ui.bgcheck

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.match.app.ui.components.MatreeScreen
import com.match.app.ui.components.MatreeUnavailableFeature

@Composable
fun BackgroundCheckScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    MatreeScreen(title = "Background Verification", onBack = onBack) { padding ->
        MatreeUnavailableFeature(
            modifier = Modifier.padding(padding),
            title = "Background verification is not active yet",
            description = "The previous screen simulated verification packages and reports locally. A real background-check product must use a contracted provider, explicit member consent and auditable result handling.",
            icon = Icons.Filled.FactCheck,
            requirements = listOf(
                "Integrate an approved verification provider and document exactly which checks are performed.",
                "Collect explicit consent before sending member information to the provider.",
                "Store provider status and report metadata on the trusted backend instead of local demo state.",
                "Connect any paid verification package to real Google Play/payment entitlements and refund handling.",
                "Define dispute, correction, retention and access controls for sensitive verification results."
            ),
            onHelp = onOpenHelp
        )
    }
}
