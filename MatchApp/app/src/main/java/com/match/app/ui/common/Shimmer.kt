package com.match.app.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Shimmer brush ─────────────────────────────────────────────────────────────

@Composable
fun shimmerBrush(widthPx: Float = 1000f): Brush {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue  = widthPx * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors    = shimmerColors,
        start     = Offset(translateAnim - widthPx, 0f),
        end       = Offset(translateAnim, 0f)
    )
}

// ── Reusable shimmer shapes ───────────────────────────────────────────────────

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 8.dp
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

@Composable
fun ShimmerCircle(size: Dp = 48.dp) {
    val brush = shimmerBrush()
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(brush)
    )
}

// ── Match card shimmer ────────────────────────────────────────────────────────

@Composable
fun MatchCardShimmer(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    androidx.compose.material3.ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(brush)
            )
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBox(Modifier.fillMaxWidth(0.6f), height = 18.dp)
                ShimmerBox(Modifier.fillMaxWidth(0.85f), height = 14.dp)
                ShimmerBox(Modifier.fillMaxWidth(0.7f), height = 14.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(Modifier.width(60.dp), height = 24.dp, cornerRadius = 12.dp)
                    ShimmerBox(Modifier.width(80.dp), height = 24.dp, cornerRadius = 12.dp)
                }
            }
        }
    }
}

// ── Chat list shimmer ─────────────────────────────────────────────────────────

@Composable
fun ChatListItemShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerCircle(48.dp)
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(Modifier.width(120.dp), height = 16.dp)
                ShimmerBox(Modifier.width(40.dp), height = 12.dp)
            }
            ShimmerBox(Modifier.fillMaxWidth(0.75f), height = 14.dp)
        }
    }
}
