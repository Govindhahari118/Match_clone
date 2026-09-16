package com.match.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Canonical spacing/touch tokens used by the final Matree UI. */
object MatreeDimens {
    val Space2 = 2.dp
    val Space4 = 4.dp
    val Space8 = 8.dp
    val Space12 = 12.dp
    val Space16 = 16.dp
    val Space20 = 20.dp
    val Space24 = 24.dp
    val Space32 = 32.dp
    val ScreenHorizontal = 16.dp
    val MinTouchTarget = 48.dp
}

/** Standard destination scaffold used to keep navigation, spacing and semantics consistent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatreeScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = actions
            )
        }
    ) { content(it) }
}

@Composable
fun MatreeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MatreeDimens.Space12)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

/** Hero card using semantic theme tokens, so every religion/light/dark palette remains legible. */
@Composable
fun MatreeHeroCard(
    title: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val base = modifier.fillMaxWidth()
    Card(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            Modifier.padding(MatreeDimens.Space20),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MatreeDimens.Space16)
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MatreeDimens.Space4)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/** Reusable 48dp+ feature target for home, drawer adjuncts and feature hubs. */
@Composable
fun MatreeFeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 76.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            Modifier.padding(MatreeDimens.Space16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MatreeDimens.Space12)
        ) {
            Surface(
                modifier = Modifier.size(MatreeDimens.MinTouchTarget),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

enum class MatreeStatusTone { NEUTRAL, POSITIVE, WARNING, ERROR }

/** Non-interactive status label. Never use a clickable chip for display-only status. */
@Composable
fun MatreeStatusChip(
    label: String,
    tone: MatreeStatusTone = MatreeStatusTone.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val container = when (tone) {
        MatreeStatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        MatreeStatusTone.POSITIVE -> MaterialTheme.colorScheme.tertiaryContainer
        MatreeStatusTone.WARNING -> MaterialTheme.colorScheme.secondaryContainer
        MatreeStatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (tone) {
        MatreeStatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        MatreeStatusTone.POSITIVE -> MaterialTheme.colorScheme.onTertiaryContainer
        MatreeStatusTone.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
        MatreeStatusTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = container) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Canonical production state for a service whose backend/provider is not enabled yet.
 * It replaces fake balances, simulated purchases, fabricated providers and dead action buttons.
 */
@Composable
fun MatreeUnavailableFeature(
    title: String,
    description: String,
    icon: ImageVector,
    requirements: List<String>,
    modifier: Modifier = Modifier,
    onHelp: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                MatreeStatusChip("Not enabled in production", MatreeStatusTone.WARNING)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Text("Required before launch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        requirements.forEach { requirement ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        requirement,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (onHelp != null) {
            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onHelp,
                modifier = Modifier.fillMaxWidth().heightIn(min = MatreeDimens.MinTouchTarget),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.HelpOutline, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Contact support")
            }
        }
    }
}
