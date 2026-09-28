package com.matree.app.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ThemeRegistryTest {
    @Test
    fun approvedDirectionsHaveIndependentPrimaryPalettes() {
        val approved = listOf(
            AppearanceTheme.UNIVERSAL,
            AppearanceTheme.HINDU,
            AppearanceTheme.MUSLIM,
            AppearanceTheme.CHRISTIAN,
            AppearanceTheme.SIKH,
        ).map(ThemeRegistry::tokensFor)

        assertEquals(
            approved.size,
            approved.map { it.colors.actionPrimary }.distinct().size,
        )
    }

    @Test
    fun unreferencedReligionsRemainNeutralUntilCanonicalReferencesExist() {
        val universal = ThemeRegistry.tokensFor(AppearanceTheme.UNIVERSAL)

        listOf(
            AppearanceTheme.BUDDHIST,
            AppearanceTheme.JAIN,
            AppearanceTheme.PARSI,
            AppearanceTheme.OTHER,
        ).forEach { theme ->
            val candidate = ThemeRegistry.tokensFor(theme)
            assertEquals(ReferenceStatus.REFERENCE_REQUIRED, candidate.referenceStatus)
            assertEquals(universal.colors, candidate.colors)
            assertEquals(universal.gradients, candidate.gradients)
            assertEquals(universal.atmosphere, candidate.atmosphere)
        }
    }

    @Test
    fun approvedThemesMeetCoreTextAndPrimaryActionContrast() {
        val approved = listOf(
            AppearanceTheme.UNIVERSAL,
            AppearanceTheme.HINDU,
            AppearanceTheme.MUSLIM,
            AppearanceTheme.CHRISTIAN,
            AppearanceTheme.SIKH,
        )

        approved.forEach { theme ->
            val tokens = ThemeRegistry.tokensFor(theme)
            assertTrue(
                "${theme.name} primary text/background contrast",
                contrast(tokens.colors.textPrimary, tokens.colors.backgroundPrimary) >= 4.5,
            )
            assertTrue(
                "${theme.name} on-primary contrast",
                contrast(tokens.colors.textOnAccent, tokens.colors.actionPrimary) >= 4.5,
            )
        }
    }

    @Test
    fun approvedReligiousFamiliesDoNotCollapseToUniversalPrimaryAction() {
        val universal = ThemeRegistry.tokensFor(AppearanceTheme.UNIVERSAL).colors.actionPrimary
        listOf(
            AppearanceTheme.HINDU,
            AppearanceTheme.MUSLIM,
            AppearanceTheme.CHRISTIAN,
            AppearanceTheme.SIKH,
        ).forEach { theme ->
            assertNotEquals(universal, ThemeRegistry.tokensFor(theme).colors.actionPrimary)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    private fun luminance(color: Color): Double {
        val argb = color.toArgb()
        val r = (argb shr 16 and 0xFF) / 255.0
        val g = (argb shr 8 and 0xFF) / 255.0
        val b = (argb and 0xFF) / 255.0

        fun linear(channel: Double): Double =
            if (channel <= 0.04045) channel / 12.92
            else ((channel + 0.055) / 1.055).pow(2.4)

        return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
    }
}
