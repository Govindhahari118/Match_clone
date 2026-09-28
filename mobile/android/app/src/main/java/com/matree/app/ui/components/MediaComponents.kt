package com.matree.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.matree.app.design.MatreeTheme

/**
 * Slot-based image/media card. Callers supply authenticated/authorized media content;
 * this component never invents photos, verification badges, activity, or scores.
 */
@Composable
fun MatreeImageCard(
    modifier: Modifier = Modifier,
    aspectRatio: Float = 4f / 5f,
    media: @Composable BoxScope.() -> Unit,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
) {
    val tokens = MatreeTheme.tokens
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        shape = RoundedCornerShape(tokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = tokens.colors.surfaceSubtle),
        border = BorderStroke(
            width = 1.dp,
            color = tokens.colors.borderSubtle,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.elevation.low),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(tokens.radii.card)),
        ) {
            media()
            overlay?.invoke(this)
        }
    }
}
