package com.match.app.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class MatreeAccessibilityTokensTest {

    @Test
    fun `interactive size tokens meet 48dp minimum target`() {
        val sizes = MatreeSizes()
        assertTrue(sizes.touchTarget.value >= 48f)
        assertTrue(sizes.buttonHeight.value >= 48f)
        assertTrue(sizes.avatarSmall.value >= 40f)
    }

    @Test
    fun `semantic status containers meet normal text AA contrast`() {
        listOf(false, true).forEach { dark ->
            val colors = semanticColorsFor(dark)
            val pairs = listOf(
                "success" to (colors.successContainer to colors.onSuccessContainer),
                "warning" to (colors.warningContainer to colors.onWarningContainer),
                "interest" to (colors.interestContainer to colors.onInterestContainer)
            )
            pairs.forEach { (label, pair) ->
                val ratio = contrastRatio(pair.first, pair.second)
                assertTrue("$label contrast was $ratio in dark=$dark", ratio >= 4.5)
            }
        }
    }

    private fun contrastRatio(
        a: androidx.compose.ui.graphics.Color,
        b: androidx.compose.ui.graphics.Color
    ): Double {
        val l1 = relativeLuminance(a)
        val l2 = relativeLuminance(b)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: androidx.compose.ui.graphics.Color): Double =
        0.2126 * linear(color.red.toDouble()) +
            0.7152 * linear(color.green.toDouble()) +
            0.0722 * linear(color.blue.toDouble())

    private fun linear(value: Double): Double =
        if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}
