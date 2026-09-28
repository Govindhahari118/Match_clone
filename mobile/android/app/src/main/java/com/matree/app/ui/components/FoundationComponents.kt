package com.matree.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.matree.app.design.MatreeTheme

@Composable
fun MatreeTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tokens = MatreeTheme.tokens
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = tokens.colors.surfacePrimary.copy(alpha = 0.96f),
        shadowElevation = tokens.elevation.low,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(
                    horizontal = tokens.spacing.md,
                    vertical = tokens.spacing.sm,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            navigation?.invoke()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = tokens.typography.title,
                    color = tokens.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = tokens.typography.caption,
                        color = tokens.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            actions()
        }
    }
}

@Composable
fun MatreeHeroHeader(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val tokens = MatreeTheme.tokens
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radii.card))
            .background(Brush.linearGradient(tokens.gradients.hero))
            .padding(tokens.spacing.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = tokens.typography.label,
                    color = tokens.colors.brandPrimary,
                )
            }
            Text(
                text = title,
                style = tokens.typography.heading1,
                color = tokens.colors.textPrimary,
            )
            Text(
                text = body,
                style = tokens.typography.body,
                color = tokens.colors.textSecondary,
            )
            trailingContent?.invoke()
        }
    }
}

@Composable
fun MatreePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val tokens = MatreeTheme.tokens
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(tokens.radii.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = tokens.colors.actionPrimary,
            contentColor = tokens.colors.textOnAccent,
            disabledContainerColor = tokens.colors.surfaceSubtle,
            disabledContentColor = tokens.colors.textDisabled,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(tokens.spacing.xs))
        }
        Text(text = text, style = tokens.typography.label)
    }
}

@Composable
fun MatreeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = MatreeTheme.tokens
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(tokens.radii.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = tokens.colors.actionSecondary,
            contentColor = tokens.colors.actionPrimary,
            disabledContainerColor = tokens.colors.surfaceSubtle,
            disabledContentColor = tokens.colors.textDisabled,
        ),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
    ) {
        Text(text = text, style = tokens.typography.label)
    }
}

@Composable
fun MatreeTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = MatreeTheme.tokens
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
    ) {
        Text(
            text = text,
            style = tokens.typography.label,
            color = if (enabled) tokens.colors.actionPrimary else tokens.colors.textDisabled,
        )
    }
}

@Composable
fun MatreeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val tokens = MatreeTheme.tokens
    IconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tokens.colors.textPrimary else tokens.colors.textDisabled,
        )
    }
}

@Composable
fun MatreeCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    content: @Composable () -> Unit,
) {
    val tokens = MatreeTheme.tokens
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radii.card),
        colors = CardDefaults.cardColors(containerColor = tokens.colors.surfaceElevated),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (elevated) tokens.elevation.medium else tokens.elevation.low,
        ),
    ) {
        Box(Modifier.padding(tokens.spacing.md)) {
            content()
        }
    }
}

@Composable
fun MatreeListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = MatreeTheme.tokens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(vertical = tokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.x2s),
        ) {
            Text(
                text = title,
                style = tokens.typography.body,
                color = tokens.colors.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = tokens.typography.bodySecondary,
                    color = tokens.colors.textSecondary,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun MatreeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val tokens = MatreeTheme.tokens
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = tokens.typography.heading3,
            color = tokens.colors.textPrimary,
        )
        action?.invoke()
    }
}

@Composable
fun MatreeBadge(
    text: String,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
) {
    val tokens = MatreeTheme.tokens
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = if (emphasis) tokens.colors.actionSecondary else tokens.colors.surfaceSubtle,
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = tokens.typography.caption,
            color = if (emphasis) tokens.colors.actionPrimary else tokens.colors.textSecondary,
        )
    }
}

@Composable
fun MatreeAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    background: Color? = null,
) {
    val tokens = MatreeTheme.tokens
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background ?: tokens.colors.actionSecondary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.take(2).uppercase(),
            style = tokens.typography.label,
            color = tokens.colors.actionPrimary,
        )
    }
}

@Composable
fun MatreeContentCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
) {
    MatreeCard(modifier = modifier) {
        val tokens = MatreeTheme.tokens
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Text(
                text = title,
                style = tokens.typography.title,
                color = tokens.colors.textPrimary,
            )
            Text(
                text = body,
                style = tokens.typography.bodySecondary,
                color = tokens.colors.textSecondary,
            )
            footer?.invoke()
        }
    }
}

/**
 * Generic content surface for devotional/religion-aware experiences.
 * It deliberately contains no generated scripture or sacred symbols.
 */
@Composable
fun MatreeRespectfulContentCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    MatreeContentCard(
        title = title,
        body = body,
        modifier = modifier,
        footer = content,
    )
}

@Composable
fun MatreeMediaControls(
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MatreeTheme.tokens
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radii.large),
        color = tokens.colors.surfaceSubtle,
    ) {
        Row(
            modifier = Modifier.padding(tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            MatreeIconButton(
                icon = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                onClick = onTogglePlayback,
            )
        }
    }
}
