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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.match.app.core.activity.ActivityStatusHelper
import com.match.app.domain.model.UserProfile

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
        score >= 90 -> Color(0xFF2E7D32) to "Excellent!"
        score >= 70 -> Color(0xFF1976D2) to "Looking good"
        score >= 50 -> Color(0xFFF57C00) to "Getting there"
        else        -> Color(0xFFE53935) to "Incomplete"
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

            if (missing.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Add: ${missing.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onComplete, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Complete", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Item checklist (first 6 only)
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

    val dotColor = if (status.isOnline) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
    val bgColor  = if (status.isOnline) Color(0xFF4CAF50).copy(alpha = 0.12f) else Color(0xFF9E9E9E).copy(alpha = 0.10f)

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
            color = if (status.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
