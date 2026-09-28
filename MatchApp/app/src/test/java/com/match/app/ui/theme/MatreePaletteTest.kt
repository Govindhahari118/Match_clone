package com.match.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class MatreePaletteTest {

    private val productionPalettes = listOf(
        AppPalette.VIVAH,
        AppPalette.HINDU,
        AppPalette.MUSLIM,
        AppPalette.CHRISTIAN,
        AppPalette.SIKH,
        AppPalette.BUDDHIST,
        AppPalette.JAIN,
        AppPalette.PARSI
    )

    @Test
    fun `Matree Signature is warm premium and matrimonial`() {
        val scheme = colorSchemeFor(AppPalette.VIVAH, dark = false)

        assertEquals(Color(0xFF6E2F4B), scheme.primary)
        assertEquals(Color(0xFFFFF9F6), scheme.background)
        assertEquals(Color.White, scheme.surface)
        assertEquals(Color(0xFFF8EEE9), scheme.surfaceVariant)
    }

    @Test
    fun `religion palettes have distinct light surface treatment`() {
        val backgrounds = productionPalettes
            .map { colorSchemeFor(it, dark = false).background }
            .toSet()

        assertTrue("Expected visually distinct religion-aware light surfaces", backgrounds.size >= 6)
        assertNotEquals(
            colorSchemeFor(AppPalette.MUSLIM, dark = false).surfaceVariant,
            colorSchemeFor(AppPalette.HINDU, dark = false).surfaceVariant
        )
        assertNotEquals(
            colorSchemeFor(AppPalette.CHRISTIAN, dark = false).surfaceVariant,
            colorSchemeFor(AppPalette.SIKH, dark = false).surfaceVariant
        )
    }

    @Test
    fun `religion palettes retain restrained identity in dark mode`() {
        val backgrounds = productionPalettes
            .map { colorSchemeFor(it, dark = true).background }
            .toSet()
        val surfaces = productionPalettes
            .map { colorSchemeFor(it, dark = true).surfaceVariant }
            .toSet()

        assertTrue("Expected distinct dark backgrounds across Matree families", backgrounds.size >= 6)
        assertTrue("Expected distinct dark card surfaces across Matree families", surfaces.size >= 6)
    }

    @Test
    fun `primary actions meet normal-text AA contrast in every production palette`() {
        productionPalettes.forEach { palette ->
            listOf(false, true).forEach { dark ->
                val scheme = colorSchemeFor(palette, dark)
                val ratio = contrastRatio(scheme.primary, scheme.onPrimary)
                assertTrue(
                    "${palette.name} primary/onPrimary contrast was $ratio in dark=$dark",
                    ratio >= 4.5
                )
            }
        }
    }

    @Test
    fun `danger semantics do not change with religion palette`() {
        listOf(false, true).forEach { dark ->
            val baseline = colorSchemeFor(AppPalette.VIVAH, dark)
            productionPalettes.forEach { palette ->
                val scheme = colorSchemeFor(palette, dark)
                assertEquals(baseline.error, scheme.error)
                assertEquals(baseline.onError, scheme.onError)
                assertEquals(baseline.errorContainer, scheme.errorContainer)
                assertEquals(baseline.onErrorContainer, scheme.onErrorContainer)
            }
        }
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val l1 = relativeLuminance(a)
        val l2 = relativeLuminance(b)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double =
        0.2126 * linear(color.red.toDouble()) +
            0.7152 * linear(color.green.toDouble()) +
            0.0722 * linear(color.blue.toDouble())

    private fun linear(value: Double): Double =
        if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}
