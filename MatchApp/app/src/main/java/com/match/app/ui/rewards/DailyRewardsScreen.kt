package com.match.app.ui.rewards

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.match.app.ui.components.MatreeScreen
import com.match.app.ui.components.MatreeUnavailableFeature

/** Rewards stay non-interactive until task completion and redemption are server-authoritative. */
@Composable
fun DailyRewardsScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    MatreeScreen(title = "Rewards & Badges", onBack = onBack) { padding ->
        MatreeUnavailableFeature(
            modifier = Modifier.padding(padding),
            title = "Rewards are not active yet",
            description = "A previous prototype allowed reward tasks to be completed by tapping the UI and displayed hard-coded badges. Production rewards must be awarded from verified server events, not client actions.",
            icon = Icons.Filled.EmojiEvents,
            requirements = listOf(
                "Award coins only from trusted server-observed events such as verified actions or completed milestones.",
                "Derive badge eligibility from real account data instead of hard-coded earned flags.",
                "Make redemption atomic and server-authoritative so a client cannot forge balance or reward state.",
                "Ensure redeemed benefits map to real entitlements such as visitor access, incognito mode or boosts.",
                "Add anti-abuse limits, audit history and clear expiry/terms before launching the program."
            ),
            onHelp = onOpenHelp
        )
    }
}
