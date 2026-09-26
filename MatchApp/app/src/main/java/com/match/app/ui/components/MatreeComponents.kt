package com.match.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.match.app.ui.theme.MatreeDesign

enum class MatreeStatusTone { NEUTRAL, SUCCESS, WARNING, ERROR, VERIFIED, PREMIUM }
enum class MatreeProfileCardVariant { HERO, STANDARD, COMPACT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatreeTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(MatreeDesign.sizes.touchTarget)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions
    )
}

@Composable
fun MatreeScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        snackbarHost = snackbarHost,
        content = content
    )
}

@Composable
fun MatreeHero(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val spacing = MatreeDesign.spacing
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MatreeDesign.radii.hero),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            Modifier.padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            if (leadingIcon != null) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(MatreeDesign.sizes.iconLarge),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content?.invoke(this)
        }
    }
}

@Composable
fun MatreeSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = MatreeDesign.spacing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
fun MatreeInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MatreeDesign.radii.card),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = MatreeDesign.elevation.card)
    ) {
        Column(
            Modifier.padding(MatreeDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm),
            content = content
        )
    }
}

@Composable
fun MatreeActionCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
        shape = RoundedCornerShape(MatreeDesign.radii.card),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = MatreeDesign.elevation.card)
    ) {
        Column(
            Modifier.padding(MatreeDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm),
            content = content
        )
    }
}

@Composable
fun MatreePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = MatreeDesign.sizes.buttonHeight),
        shape = RoundedCornerShape(MatreeDesign.radii.medium)
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(MatreeDesign.sizes.iconSmall))
            Spacer(Modifier.width(MatreeDesign.spacing.xs))
        }
        Text(text)
    }
}

@Composable
fun MatreeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = MatreeDesign.sizes.buttonHeight),
        shape = RoundedCornerShape(MatreeDesign.radii.medium)
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(MatreeDesign.sizes.iconSmall))
            Spacer(Modifier.width(MatreeDesign.spacing.xs))
        }
        Text(text)
    }
}

@Composable
fun MatreeStatusChip(
    text: String,
    tone: MatreeStatusTone = MatreeStatusTone.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val semantic = MatreeDesign.colors
    val (container, content) = when (tone) {
        MatreeStatusTone.SUCCESS -> semantic.successContainer to semantic.onSuccessContainer
        MatreeStatusTone.WARNING -> semantic.warningContainer to semantic.onWarningContainer
        MatreeStatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        MatreeStatusTone.VERIFIED -> MaterialTheme.colorScheme.primaryContainer to semantic.verified
        MatreeStatusTone.PREMIUM -> MaterialTheme.colorScheme.secondaryContainer to semantic.premium
        MatreeStatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = container,
        contentColor = content
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = MatreeDesign.spacing.sm, vertical = MatreeDesign.spacing.xxs),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun MatreeLoadingSkeleton(
    modifier: Modifier = Modifier,
    height: Dp? = null
) {
    val actualHeight = height ?: MatreeDesign.sizes.avatarStandard
    Surface(
        modifier = modifier.fillMaxWidth().height(actualHeight),
        shape = RoundedCornerShape(MatreeDesign.radii.card),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {}
}

/**
 * Canonical profile-card family for discovery/interest/profile-adjacent lists.
 * Callers provide authoritative state and actions; this component owns visual consistency only.
 */
@Composable
fun MatreeProfileCard(
    name: String,
    age: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MatreeProfileCardVariant = MatreeProfileCardVariant.STANDARD,
    username: String = "",
    primaryLine: String = "",
    secondaryLine: String = "",
    photoModel: Any? = null,
    isVerified: Boolean = false,
    isPremium: Boolean = false,
    activityLabel: String? = null,
    isOnline: Boolean = false,
    supportingLabels: List<String> = emptyList(),
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    val spacing = MatreeDesign.spacing
    val avatarSize = when (variant) {
        MatreeProfileCardVariant.HERO -> MatreeDesign.sizes.avatarHero
        MatreeProfileCardVariant.STANDARD -> MatreeDesign.sizes.avatarStandard
        MatreeProfileCardVariant.COMPACT -> MatreeDesign.sizes.avatarCompact
    }
    val imageShape = RoundedCornerShape(
        when (variant) {
            MatreeProfileCardVariant.HERO -> MatreeDesign.radii.large
            else -> MatreeDesign.radii.card
        }
    )
    val usablePhoto = photoModel != null && (photoModel !is String || photoModel.isNotBlank())

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { role = Role.Button },
        shape = RoundedCornerShape(MatreeDesign.radii.card),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = MatreeDesign.elevation.card)
    ) {
        Column(Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (usablePhoto) {
                    AsyncImage(
                        model = photoModel,
                        contentDescription = "$name profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(avatarSize).clip(imageShape)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(avatarSize),
                        shape = imageShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                name.firstOrNull()?.uppercase() ?: "?",
                                style = if (variant == MatreeProfileCardVariant.HERO) {
                                    MaterialTheme.typography.headlineMedium
                                } else {
                                    MaterialTheme.typography.titleLarge
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.width(spacing.sm))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            buildString {
                                append(name)
                                age?.takeIf { it > 0 }?.let { append(", ").append(it) }
                            },
                            style = if (variant == MatreeProfileCardVariant.HERO) {
                                MaterialTheme.typography.titleLarge
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isVerified) {
                            Spacer(Modifier.width(spacing.xxs))
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                modifier = Modifier.size(MatreeDesign.sizes.iconSmall),
                                tint = MatreeDesign.colors.verified
                            )
                        }
                        if (isPremium) {
                            Spacer(Modifier.width(spacing.xxs))
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Premium member",
                                modifier = Modifier.size(MatreeDesign.sizes.iconSmall),
                                tint = MatreeDesign.colors.premium
                            )
                        }
                    }
                    if (username.isNotBlank()) {
                        Text("@$username", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (primaryLine.isNotBlank()) {
                        Text(primaryLine, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (secondaryLine.isNotBlank()) {
                        Text(
                            secondaryLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!activityLabel.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(spacing.xs),
                                shape = RoundedCornerShape(percent = 50),
                                color = if (isOnline) MatreeDesign.colors.online else MaterialTheme.colorScheme.outline
                            ) {}
                            Spacer(Modifier.width(spacing.xs))
                            Text(
                                activityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) MatreeDesign.colors.online else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (supportingLabels.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    supportingLabels.filter { it.isNotBlank() }.take(3).forEach { label ->
                        MatreeStatusChip(text = label)
                    }
                }
            }

            actions()
        }
    }
}
