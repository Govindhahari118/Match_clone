package com.match.app.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.match.app.ui.components.MatreePrimaryButton
import com.match.app.ui.components.MatreeProfileCard
import com.match.app.ui.components.MatreeProfileCardVariant
import com.match.app.ui.components.MatreeSecondaryButton
import com.match.app.ui.components.MatreeStatusChip
import com.match.app.ui.components.MatreeStatusTone

/**
 * Debug/design-time catalog only. The preview identity and data never enter the release source set.
 */
@Composable
private fun ThemeCatalogPreview(palette: AppPalette, dark: Boolean) {
    MatchTheme(darkMode = dark, palette = palette) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MatreeDesign.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.md)
            ) {
                MatreeProfileCard(
                    name = "Theme preview",
                    age = 29,
                    onClick = {},
                    variant = MatreeProfileCardVariant.STANDARD,
                    primaryLine = "Hyderabad • Product professional",
                    secondaryLine = "Preview data only — not a production profile",
                    isVerified = true,
                    isPremium = true,
                    activityLabel = "Active recently",
                    supportingLabels = listOf("Compatible", "Complete profile")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
                    ) {
                        MatreePrimaryButton(
                            text = "Interest",
                            icon = Icons.Filled.Favorite,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                        MatreeSecondaryButton(
                            text = "Shortlist",
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.xs)) {
                    MatreeStatusChip("Verified", MatreeStatusTone.VERIFIED)
                    MatreeStatusChip("Premium", MatreeStatusTone.PREMIUM)
                    MatreeStatusChip("Warning", MatreeStatusTone.WARNING)
                }
            }
        }
    }
}

@Preview(name = "Neutral Light", showBackground = true, widthDp = 390)
@Composable
private fun NeutralLightPreview() = ThemeCatalogPreview(AppPalette.VIVAH, dark = false)

@Preview(name = "Neutral Dark", showBackground = true, widthDp = 390)
@Composable
private fun NeutralDarkPreview() = ThemeCatalogPreview(AppPalette.VIVAH, dark = true)

@Preview(name = "Hindu Light", showBackground = true, widthDp = 390)
@Composable
private fun HinduLightPreview() = ThemeCatalogPreview(AppPalette.HINDU, dark = false)

@Preview(name = "Hindu Dark", showBackground = true, widthDp = 390)
@Composable
private fun HinduDarkPreview() = ThemeCatalogPreview(AppPalette.HINDU, dark = true)

@Preview(name = "Muslim Light", showBackground = true, widthDp = 390)
@Composable
private fun MuslimLightPreview() = ThemeCatalogPreview(AppPalette.MUSLIM, dark = false)

@Preview(name = "Muslim Dark", showBackground = true, widthDp = 390)
@Composable
private fun MuslimDarkPreview() = ThemeCatalogPreview(AppPalette.MUSLIM, dark = true)

@Preview(name = "Christian Light", showBackground = true, widthDp = 390)
@Composable
private fun ChristianLightPreview() = ThemeCatalogPreview(AppPalette.CHRISTIAN, dark = false)

@Preview(name = "Christian Dark", showBackground = true, widthDp = 390)
@Composable
private fun ChristianDarkPreview() = ThemeCatalogPreview(AppPalette.CHRISTIAN, dark = true)

@Preview(name = "Sikh Light", showBackground = true, widthDp = 390)
@Composable
private fun SikhLightPreview() = ThemeCatalogPreview(AppPalette.SIKH, dark = false)

@Preview(name = "Sikh Dark", showBackground = true, widthDp = 390)
@Composable
private fun SikhDarkPreview() = ThemeCatalogPreview(AppPalette.SIKH, dark = true)

@Preview(name = "Buddhist Light", showBackground = true, widthDp = 390)
@Composable
private fun BuddhistLightPreview() = ThemeCatalogPreview(AppPalette.BUDDHIST, dark = false)

@Preview(name = "Buddhist Dark", showBackground = true, widthDp = 390)
@Composable
private fun BuddhistDarkPreview() = ThemeCatalogPreview(AppPalette.BUDDHIST, dark = true)

@Preview(name = "Jain Light", showBackground = true, widthDp = 390)
@Composable
private fun JainLightPreview() = ThemeCatalogPreview(AppPalette.JAIN, dark = false)

@Preview(name = "Jain Dark", showBackground = true, widthDp = 390)
@Composable
private fun JainDarkPreview() = ThemeCatalogPreview(AppPalette.JAIN, dark = true)

@Preview(name = "Parsi Light", showBackground = true, widthDp = 390)
@Composable
private fun ParsiLightPreview() = ThemeCatalogPreview(AppPalette.PARSI, dark = false)

@Preview(name = "Parsi Dark", showBackground = true, widthDp = 390)
@Composable
private fun ParsiDarkPreview() = ThemeCatalogPreview(AppPalette.PARSI, dark = true)

