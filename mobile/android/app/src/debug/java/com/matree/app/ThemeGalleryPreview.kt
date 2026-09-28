package com.matree.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.matree.app.design.AppearanceTheme
import com.matree.app.design.MatreeBackground
import com.matree.app.design.MatreeTheme
import com.matree.app.ui.components.MatreeCard
import com.matree.app.ui.components.MatreeHeroHeader
import com.matree.app.ui.components.MatreePrimaryButton
import com.matree.app.ui.components.MatreeSearchField
import com.matree.app.ui.components.MatreeSecondaryButton

@Preview(name = "Default", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DefaultPreview() = ThemePreview(AppearanceTheme.UNIVERSAL)

@Preview(name = "Hindu", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HinduPreview() = ThemePreview(AppearanceTheme.HINDU)

@Preview(name = "Muslim", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun MuslimPreview() = ThemePreview(AppearanceTheme.MUSLIM)

@Preview(name = "Christian", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ChristianPreview() = ThemePreview(AppearanceTheme.CHRISTIAN)

@Preview(name = "Sikh", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SikhPreview() = ThemePreview(AppearanceTheme.SIKH)

@Composable
private fun ThemePreview(theme: AppearanceTheme) {
    MatreeTheme(appearanceTheme = theme) {
        val tokens = MatreeTheme.tokens
        MatreeBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaddingValues(tokens.spacing.md)),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
            ) {
                MatreeHeroHeader(
                    eyebrow = theme.displayName,
                    title = "A consistent Matree experience",
                    body = "Shared structure, theme-specific atmosphere, production Android behavior.",
                )
                MatreeSearchField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Search",
                )
                MatreeCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    ) {
                        MatreePrimaryButton(
                            text = "Primary action",
                            onClick = {},
                        )
                        MatreeSecondaryButton(
                            text = "Secondary action",
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}
