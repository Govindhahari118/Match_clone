package com.match.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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


@Composable
fun MatreeVerificationBadge(
    verified: Boolean,
    modifier: Modifier = Modifier,
    verifiedLabel: String = "Verified",
    pendingLabel: String = "Not verified"
) {
    MatreeStatusChip(
        text = if (verified) verifiedLabel else pendingLabel,
        tone = if (verified) MatreeStatusTone.VERIFIED else MatreeStatusTone.NEUTRAL,
        modifier = modifier.semantics { role = Role.Image }
    )
}

@Composable
fun MatreeCoverageBadge(
    complete: Int,
    total: Int,
    modifier: Modifier = Modifier,
    label: String = "Profile"
) {
    val safeTotal = total.coerceAtLeast(1)
    val safeComplete = complete.coerceIn(0, safeTotal)
    val tone = when {
        safeComplete >= safeTotal -> MatreeStatusTone.SUCCESS
        safeComplete > 0 -> MatreeStatusTone.WARNING
        else -> MatreeStatusTone.NEUTRAL
    }
    MatreeStatusChip(
        text = label + " " + safeComplete + "/" + safeTotal,
        tone = tone,
        modifier = modifier
    )
}

@Composable
fun MatreeMatchSignal(
    label: String,
    modifier: Modifier = Modifier,
    score: Float? = null,
    supportingText: String? = null
) {
    val normalized = score?.coerceIn(0f, 1f)
    MatreeInfoCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                supportingText?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            normalized?.let {
                Text(
                    ((it * 100).toInt()).toString() + "%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        normalized?.let {
            LinearProgressIndicator(
                progress = { it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MatreeIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(MatreeDesign.sizes.touchTarget)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(MatreeDesign.sizes.icon)
        )
    }
}

@Composable
fun MatreeChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        leadingIcon = icon?.let { image ->
            {
                Icon(
                    image,
                    contentDescription = null,
                    modifier = Modifier.size(MatreeDesign.sizes.iconSmall)
                )
            }
        },
        label = { Text(text) }
    )
}

/**
 * Shared photo pager. Models are always supplied by the caller's authorized repository state;
 * this component never invents sample people or fallback network photos.
 */
@Composable
fun MatreePhotoPager(
    photoModels: List<Any>,
    profileName: String,
    modifier: Modifier = Modifier,
    fallbackInitial: String = profileName.firstOrNull()?.uppercase() ?: "?",
    aspectRatio: Float = MatreeDesign.profilePhotoAspectRatio
) {
    val models = remember(photoModels) {
        photoModels.filter { model -> model !is String || model.isNotBlank() }
    }
    val pageCount = models.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
        ) { page ->
            val model = models.getOrNull(page)
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = profileName + " profile photo " + (page + 1) + " of " + models.size,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            fallbackInitial,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (models.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MatreeDesign.spacing.xs),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                models.indices.forEach { index ->
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = MatreeDesign.spacing.xxs)
                            .size(if (pagerState.currentPage == index) 8.dp else 6.dp),
                        shape = RoundedCornerShape(percent = 50),
                        color = if (pagerState.currentPage == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ) {}
                }
            }
        }
    }
}

/**
 * Canonical photo-first profile header used by profile and profile-detail surfaces.
 */
@Composable
fun MatreeProfileHeader(
    name: String,
    age: Int?,
    photoModels: List<Any>,
    modifier: Modifier = Modifier,
    username: String = "",
    primaryLine: String = "",
    secondaryLine: String = "",
    isVerified: Boolean = false,
    isPremium: Boolean = false,
    managedBy: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MatreeDesign.radii.large),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = MatreeDesign.elevation.card)
    ) {
        Column {
            MatreePhotoPager(
                photoModels = photoModels,
                profileName = name,
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                Modifier.padding(MatreeDesign.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildString {
                            append(name)
                            age?.takeIf { value -> value > 0 }?.let { value -> append(", ").append(value) }
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    trailingContent?.invoke(this)
                }
                if (username.isNotBlank()) {
                    Text(
                        "@$username",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (primaryLine.isNotBlank()) {
                    Text(primaryLine, style = MaterialTheme.typography.bodyMedium)
                }
                if (secondaryLine.isNotBlank()) {
                    Text(
                        secondaryLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isVerified) MatreeVerificationBadge(verified = true)
                    if (isPremium) MatreeStatusChip("Premium", MatreeStatusTone.PREMIUM)
                    managedBy?.takeIf { it.isNotBlank() }?.let {
                        MatreeStatusChip("Managed by $it", MatreeStatusTone.NEUTRAL)
                    }
                }
            }
        }
    }
}

@Composable
fun MatreeProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MatreeDesign.radii.card)
    ) {
        Column(
            Modifier.padding(MatreeDesign.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}
