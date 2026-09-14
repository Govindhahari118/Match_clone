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

/** Reusable, truthful paywall for server-backed membership features. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    feature: Feature,
    minimumPlan: Plan,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val priceLabel = if (minimumPlan.amountPaise <= 0) {
        minimumPlan.displayName
    } else {
        val rupees = minimumPlan.amountPaise / 100
        val duration = if (minimumPlan.durationMonths == 1) "month" else "${minimumPlan.durationMonths} months"
        "${minimumPlan.displayName} • ₹$rupees / $duration"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(72.dp).background(
                    Brush.radialGradient(listOf(Color(0xFF8B1A1A), Color(0xFFD4A017))),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(featureIcon(feature), null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Text(featureTitle(feature), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                featureDescription(feature),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFD4A017), modifier = Modifier.size(20.dp))
                    Text(priceLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }

            planBenefits(minimumPlan).forEach { benefit ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp), tint = Color(0xFF4CAF50))
                    Text(benefit, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onUpgrade(); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1A1A)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Rocket, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(t("upgrade_now", "Upgrade to ${minimumPlan.displayName}"), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onDismiss) { Text(t("maybe_later", "Maybe later")) }
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
    Feature.SEE_WHO_VIEWED -> "See who viewed you"
    Feature.REVEAL_CONTACT -> "Unlock contact details"
    Feature.KUNDALI_MATCH -> "Kundali compatibility report"
    Feature.PROFILE_BOOST -> "Boost your profile"
    Feature.VIDEO_CALL -> "Video calling"
    Feature.RM_ASSISTANCE -> "Relationship manager"
    Feature.PRIORITY_SUPPORT -> "Priority support"
    Feature.ADVANCED_FILTERS -> "Advanced search filters"
    Feature.READ_RECEIPTS -> "Read receipts"
    Feature.STEALTH_BROWSE -> "Stealth browse mode"
    Feature.SEND_INTEREST -> "Send more interests"
}

private fun featureDescription(feature: Feature): String = when (feature) {
    Feature.SEE_WHO_VIEWED -> "See the profiles that viewed you when your plan includes this feature."
    Feature.REVEAL_CONTACT -> "Reveal contact details through the secure server-controlled contact flow."
    Feature.KUNDALI_MATCH -> "Open the detailed astrology compatibility report when available."
    Feature.PROFILE_BOOST -> "Increase profile visibility during an active boost period."
    Feature.VIDEO_CALL -> "Use supported in-app calling features included with your membership."
    Feature.RM_ASSISTANCE -> "Access relationship-manager assistance when included with your plan."
    Feature.PRIORITY_SUPPORT -> "Get priority support when included with your membership."
    Feature.ADVANCED_FILTERS -> "Use the extended matrimony discovery filters."
    Feature.READ_RECEIPTS -> "See message read status where supported by the conversation."
    Feature.STEALTH_BROWSE -> "Browse without creating viewer-history entries when stealth mode is active."
    Feature.SEND_INTEREST -> "You've reached the free-plan interest allowance. Upgrade for a higher limit."
}

private fun planBenefits(plan: Plan): List<String> = buildList {
    if (plan.interestsPerDay == Int.MAX_VALUE) add("Unlimited daily interests") else if (plan.interestsPerDay > 0) add("${plan.interestsPerDay} interests per day")
    if (plan.canSeeWhoViewed) add("See who viewed your profile")
    if (plan.canRevealContact) add("Reveal eligible contact details (${plan.contactLimit}/period)")
    if (plan.canUseKundali) add("Kundali compatibility reports")
    if (plan.canBoost) add("Profile boost access")
    if (plan.canVideoCall) add("Video calling")
    if (plan.hasRMAssistance) add("Relationship-manager assistance")
    if (plan.prioritySupport) add("Priority support")
    if (plan.advancedFilters) add("Advanced discovery filters")
    if (plan.readReceipts) add("Chat read receipts")
    if (plan.stealthBrowse) add("Stealth browsing")
}