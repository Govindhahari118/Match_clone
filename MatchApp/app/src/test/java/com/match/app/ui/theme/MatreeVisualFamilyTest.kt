package com.match.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatreeVisualFamilyTest {

    private val production = AppPalette.productionThemes

    @Test
    fun `every production palette has a dedicated visual family`() {
        val families = production.map(::visualFamilyFor)
        assertEquals(production.size, families.size)
        assertEquals(production.toSet(), families.map { it.palette }.toSet())
        assertTrue(families.all { it.displayName.isNotBlank() && it.shortDescription.isNotBlank() })
        assertTrue(families.all { it.heroTitle.isNotBlank() && it.heroSubtitle.isNotBlank() })
    }

    @Test
    fun `universal production family is named Matree Neutral`() {
        assertEquals("Matree Neutral", AppPalette.VIVAH.label)
        assertEquals("Matree Neutral", visualFamilyFor(AppPalette.VIVAH).displayName)
        assertEquals(MatreeMotif.NEUTRAL, visualFamilyFor(AppPalette.VIVAH).motif)
    }

    @Test
    fun `religion families do not collapse to the neutral motif`() {
        production.filter { it != AppPalette.VIVAH }.forEach { palette ->
            assertNotEquals(MatreeMotif.NEUTRAL, visualFamilyFor(palette).motif)
        }
    }

    @Test
    fun `approved color led identities remain distinct`() {
        assertEquals(Color(0xFFFF8A00), visualFamilyFor(AppPalette.HINDU).accent)
        assertEquals(Color(0xFF0B6B53), visualFamilyFor(AppPalette.MUSLIM).accent)
        assertEquals(Color(0xFF1E4DB7), visualFamilyFor(AppPalette.CHRISTIAN).accent)
        assertEquals(Color(0xFFD4AF37), visualFamilyFor(AppPalette.SIKH).accent)
        assertEquals(Color(0xFFE19A2F), visualFamilyFor(AppPalette.BUDDHIST).accent)
        assertEquals(Color(0xFF537F46), visualFamilyFor(AppPalette.JAIN).accent)
        assertEquals(Color(0xFF0F6B6A), visualFamilyFor(AppPalette.PARSI).accent)
    }

    @Test
    fun `visual family copy describes presentation not member identity`() {
        production.forEach { palette ->
            val copy = buildString {
                append(visualFamilyFor(palette).heroTitle)
                append(' ')
                append(visualFamilyFor(palette).heroSubtitle)
            }.lowercase()
            assertTrue("Visual copy must not claim verification", "verified member" !in copy)
            assertTrue("Visual copy must not claim activity", "active now" !in copy)
            assertTrue("Visual copy must not claim personal observance", "practicing member" !in copy)
        }
    }
}
