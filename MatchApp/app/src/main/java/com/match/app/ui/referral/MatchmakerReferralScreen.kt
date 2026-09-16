package com.match.app.ui.referral

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.match.app.ui.components.MatreeScreen
import com.match.app.ui.components.MatreeUnavailableFeature

@Composable
fun MatchmakerReferralScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    MatreeScreen(title = "Referrals", onBack = onBack) { padding ->
        MatreeUnavailableFeature(
            modifier = Modifier.padding(padding),
            title = "Referral rewards are not active yet",
            description = "The previous prototype generated local referral codes, rewards and a leaderboard without trusted attribution records. Those simulations are removed from production UI.",
            icon = Icons.Filled.CardGiftcard,
            requirements = listOf(
                "Issue unique referral identifiers from the backend and prevent self-referral or duplicate attribution.",
                "Define the qualifying event, reward amount, expiry and anti-abuse rules.",
                "Record referral attribution and reward status on trusted server data.",
                "Connect rewards to an auditable server-side fulfillment mechanism.",
                "Show leaderboards only when based on real, privacy-reviewed program data."
            ),
            onHelp = onOpenHelp
        )
    }
}
