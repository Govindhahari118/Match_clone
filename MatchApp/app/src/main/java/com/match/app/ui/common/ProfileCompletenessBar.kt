package com.match.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.match.app.core.activity.ActivityStatusHelper
import com.match.app.domain.model.UserProfile
import com.match.app.ui.theme.MatreeDesign

/**
 * Profile completeness progress bar — shown on ProfileScreen and HomeScreen.
 * Helps members understand which profile sections are still incomplete.
 */
@Composable
fun ProfileCompletenessBar(
    profile: UserProfile,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val score   = ActivityStatusHelper.profileCompleteness(profile)
    val items   = ActivityStatusHelper.completenessItems(profile)
    val missing = items.filter { !it.second }.take(3).map { it.first }
    val pct     = score / 100f
    val animPct by animateFloatAsState(pct, animationSpec = tween(800), label = "completeness")

    val (barColor, label) = when {
        score >= 90 -> MatreeDesign.colors.success to "Excellent!"
        score >= 70 -> MatreeDesign.colors.verified to "Looking good"
        score >= 50 -> MatreeDesign.colors.warning to "Getting there"
        else        -> MaterialTheme.colorScheme.error to "Incomplete"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Profile Strength", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(label, style = MaterialTheme.typography.bodySmall, color = barColor, fontWeight = FontWeight.SemiBold)
                }
                Text("$score%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = barColor)
            }

            LinearProgressIndicator(
                progress = { animPct },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            if (score < 100) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (missing.isNotEmpty()) {
                            "Next: ${missing.joinToString(", ")}"
                        } else {
                            "Review partner preferences and verification to strengthen your profile."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onComplete, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Review", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Text(
                "Strength is server-calculated from profile sections, approved photo, partner preferences and verification.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Visible section checklist only; the percentage remains server-authoritative.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items.take(6).forEach { (label, done) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (done) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                            contentDescription = label,
                            tint = if (done) barColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            label.split(" ").first(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

/**
 * Small activity status chip shown on profile cards and detail screen.
 * Green dot for online, grey for recent.
 */
@Composable
fun ActivityStatusChip(
    lastActiveAt: Long,
    modifier: Modifier = Modifier
) {
    val status = remember(lastActiveAt) { ActivityStatusHelper.from(lastActiveAt) }
    if (!status.isRecent) return

    val dotColor = if (status.isOnline) MatreeDesign.colors.online else MaterialTheme.colorScheme.outline
    val bgColor = if (status.isOnline) {
        MatreeDesign.colors.successContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Text(
            status.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (status.isOnline) MatreeDesign.colors.success else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
