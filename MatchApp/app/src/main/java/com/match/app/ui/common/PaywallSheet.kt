package com.match.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.match.app.domain.subscription.SubscriptionPlans.Feature
import com.match.app.domain.subscription.SubscriptionPlans.Plan
import com.match.app.ui.i18n.t

/**
 * A reusable paywall bottom sheet shown when a free/lower-tier user
 * attempts to access a gated feature.
 *
 * Displays the feature benefit, minimum required plan, and an upgrade CTA.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    feature: Feature,
    minimumPlan: Plan,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF8B1A1A), Color(0xFFD4A017))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    featureIcon(feature),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                featureTitle(feature),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                featureDescription(feature),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Plan badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFD4A017), modifier = Modifier.size(20.dp))
                    Text(
                        "Available from ${minimumPlan.displayName} plan (₹${minimumPlan.priceMonthly}/mo)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Benefits list
            val benefits = planBenefits(minimumPlan)
            benefits.forEach { benefit ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp),
                        tint = Color(0xFF4CAF50))
                    Text(benefit, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(8.dp))

            // CTA
            Button(
                onClick = { onUpgrade(); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1A1A)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Rocket, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    t("upgrade_now", "Upgrade to ${minimumPlan.displayName}"),
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = onDismiss) {
                Text(t("maybe_later", "Maybe later"))
            }
        }
    }
}

private fun featureIcon(feature: Feature): ImageVector = when (feature) {
    Feature.SEE_WHO_VIEWED -> Icons.Filled.RemoveRedEye
    Feature.REVEAL_CONTACT -> Icons.Filled.Phone
    Feature.KUNDALI_MATCH -> Icons.Filled.Star
    Feature.PROFILE_BOOST -> Icons.Filled.Bolt
    Feature.VIDEO_CALL -> Icons.Filled.VideoCall
    Feature.RM_ASSISTANCE -> Icons.Filled.SupportAgent
    Feature.PRIORITY_SUPPORT -> Icons.Filled.HeadsetMic
    Feature.ADVANCED_FILTERS -> Icons.Filled.FilterList
    Feature.READ_RECEIPTS -> Icons.Filled.DoneAll
    Feature.STEALTH_BROWSE -> Icons.Filled.VisibilityOff
    Feature.SEND_INTEREST -> Icons.Filled.Favorite
}

private fun featureTitle(feature: Feature): String = when (feature) {
    Feature.SEE_WHO_VIEWED -> "See Who Viewed You"
    Feature.REVEAL_CONTACT -> "Unlock Contact Details"
    Feature.KUNDALI_MATCH -> "Kundali Compatibility Report"
    Feature.PROFILE_BOOST -> "Boost Your Profile"
    Feature.VIDEO_CALL -> "Video Calling"
    Feature.RM_ASSISTANCE -> "Relationship Manager"
    Feature.PRIORITY_SUPPORT -> "Priority Support"
    Feature.ADVANCED_FILTERS -> "Advanced Search Filters"
    Feature.READ_RECEIPTS -> "Read Receipts"
    Feature.STEALTH_BROWSE -> "Stealth Browse Mode"
    Feature.SEND_INTEREST -> "Send More Interests"
}

private fun featureDescription(feature: Feature): String = when (feature) {
    Feature.SEE_WHO_VIEWED -> "Know exactly who checked your profile. See full list with photos and details."
    Feature.REVEAL_CONTACT -> "Get direct phone/WhatsApp contact of your mutual matches instantly."
    Feature.KUNDALI_MATCH -> "View detailed 10-Porutham astrology compatibility with Nakshatra analysis."
    Feature.PROFILE_BOOST -> "Get 10x more profile views for 24 hours. Appear at the top of search results."
    Feature.VIDEO_CALL -> "Have face-to-face conversations before meeting in person. Safe & secure."
    Feature.RM_ASSISTANCE -> "Get a dedicated relationship manager to help find your perfect match."
    Feature.PRIORITY_SUPPORT -> "Skip the queue. Get priority responses from our support team."
    Feature.ADVANCED_FILTERS -> "Filter by lifestyle, career, astrology, and 20+ advanced criteria."
    Feature.READ_RECEIPTS -> "Know when your messages are read. Never wonder if they saw your message."
    Feature.STEALTH_BROWSE -> "Browse profiles invisibly. Your visits won't appear in their viewer list."
    Feature.SEND_INTEREST -> "You've reached your daily interest limit. Upgrade for more."
}

private fun planBenefits(plan: Plan): List<String> = when (plan) {
    Plan.STANDARD -> listOf(
        "15 interests per day",
        "See who viewed your profile",
        "Advanced search filters",
        "Read receipts in chat"
    )
    Plan.PREMIUM -> listOf(
        "Unlimited interests",
        "Reveal contact details",
        "Kundali match reports",
        "Profile boost (3x/month)",
        "Stealth browse mode"
    )
    Plan.PLATINUM -> listOf(
        "Everything in Premium",
        "Video calling",
        "Dedicated relationship manager",
        "Priority support",
        "Unlimited boosts"
    )
    else -> emptyList()
}
