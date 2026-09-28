package com.matree.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.matree.app.R
import com.matree.app.design.AppearanceTheme
import com.matree.app.design.MatreeTheme
import com.matree.app.design.ReferenceStatus
import com.matree.app.design.ThemeRegistry
import com.matree.app.ui.components.MatreeAdaptiveContent
import com.matree.app.ui.components.MatreeCard
import com.matree.app.ui.components.MatreeHeroHeader
import com.matree.app.ui.components.MatreeSectionHeader
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    currentAppearance: AppearanceTheme,
    onAppearanceSelected: suspend (AppearanceTheme) -> Unit,
) {
    val tokens = MatreeTheme.tokens
    val scope = rememberCoroutineScope()

    MatreeAdaptiveContent(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            item {
                MatreeHeroHeader(
                    title = "Your Matree",
                    body = "Appearance is a device preference. It does not change or infer your saved religion, verification, profile attributes, or match data.",
                )
            }

            item {
                MatreeSectionHeader(title = stringResource(R.string.appearance))
            }

            item {
                Text(
                    text = stringResource(R.string.appearance_subtitle),
                    style = tokens.typography.bodySecondary,
                    color = tokens.colors.textSecondary,
                )
            }

            ThemeRegistry.descriptors().forEach { descriptor ->
                val enabled = descriptor.referenceStatus == ReferenceStatus.APPROVED_DIRECTION
                item(key = descriptor.appearanceTheme.name) {
                    MatreeCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(descriptor.gradients.selected)),
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(tokens.spacing.x2s),
                            ) {
                                Text(
                                    text = descriptor.appearanceTheme.displayName,
                                    style = tokens.typography.title,
                                    color = tokens.colors.textPrimary,
                                )
                                Text(
                                    text = if (enabled) {
                                        "Approved visual direction"
                                    } else {
                                        stringResource(R.string.reference_required_body)
                                    },
                                    style = tokens.typography.caption,
                                    color = tokens.colors.textSecondary,
                                )
                            }

                            RadioButton(
                                selected = currentAppearance == descriptor.appearanceTheme,
                                enabled = enabled,
                                onClick = {
                                    scope.launch {
                                        onAppearanceSelected(descriptor.appearanceTheme)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            item {
                ReferencePolicyNote()
            }
        }
    }
}

@Composable
private fun ReferencePolicyNote() {
    val tokens = MatreeTheme.tokens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.radii.large),
        color = tokens.colors.surfaceSubtle,
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            Text(
                text = "Reference fidelity",
                style = tokens.typography.label,
                color = tokens.colors.textPrimary,
            )
            Text(
                text = "Buddhist, Jain, Parsi, and Other are intentionally not given invented religious artwork. Their theme slots become selectable after approved canonical references define their design DNA.",
                style = tokens.typography.bodySecondary,
                color = tokens.colors.textSecondary,
            )
        }
    }
}
