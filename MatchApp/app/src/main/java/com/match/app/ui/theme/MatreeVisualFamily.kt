package com.match.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Presentation-only identity for a Matree appearance family.
 *
 * Nothing in this model represents member identity, matching eligibility or religious practice.
 * It exists only to keep the approved color-led + culturally rich visual system data-driven.
 */
enum class MatreeMotif {
    NEUTRAL,
    LOTUS_ARCH,
    GEOMETRIC_ARCH,
    STAINED_ARCH,
    GOLDEN_ARCH,
    LOTUS_STUPA,
    MARBLE_LOTUS,
    HERITAGE_DECO
}

@Immutable
data class MatreeVisualFamily(
    val palette: AppPalette,
    val displayName: String,
    val shortDescription: String,
    val heroEyebrow: String,
    val heroTitle: String,
    val heroSubtitle: String,
    val motif: MatreeMotif,
    val accent: Color,
    val accentSecondary: Color,
    val ornament: Color,
    val ornamentAlpha: Float,
    val visualKeywords: List<String>
)

private val visualFamilies = mapOf(
    AppPalette.VIVAH to MatreeVisualFamily(
        palette = AppPalette.VIVAH,
        displayName = "Matree Signature",
        shortDescription = "Warm ivory, deep plum and champagne-inspired matrimonial elegance",
        heroEyebrow = "MATREE",
        heroTitle = "A beautiful beginning, designed around what matters",
        heroSubtitle = "Matree's flagship matrimonial experience: warm, refined and focused on real preferences, trust and meaningful connection.",
        motif = MatreeMotif.NEUTRAL,
        accent = Color(0xFF6E2F4B),
        accentSecondary = Color(0xFFB8897E),
        ornament = Color(0xFFD7B98E),
        ornamentAlpha = 0.30f,
        visualKeywords = listOf("Warm", "Refined", "Modern", "Welcoming")
    ),
    AppPalette.HINDU to MatreeVisualFamily(
        palette = AppPalette.HINDU,
        displayName = "Hindu",
        shortDescription = "Saffron, marigold and warm heritage accents",
        heroEyebrow = "MATREE HINDU",
        heroTitle = "Warm heritage, modern connections",
        heroSubtitle = "A saffron-led Matree experience with restrained lotus, arch and festive details.",
        motif = MatreeMotif.LOTUS_ARCH,
        accent = Color(0xFFFF8A00),
        accentSecondary = Color(0xFFD7261E),
        ornament = Color(0xFFD4AF37),
        ornamentAlpha = 0.30f,
        visualKeywords = listOf("Saffron", "Marigold", "Gold", "Heritage")
    ),
    AppPalette.MUSLIM to MatreeVisualFamily(
        palette = AppPalette.MUSLIM,
        displayName = "Muslim",
        shortDescription = "Emerald, teal and elegant geometric accents",
        heroEyebrow = "MATREE MUSLIM",
        heroTitle = "Elegant connections, thoughtfully presented",
        heroSubtitle = "An emerald-led Matree experience with restrained arches, geometry and warm gold detail.",
        motif = MatreeMotif.GEOMETRIC_ARCH,
        accent = Color(0xFF0B6B53),
        accentSecondary = Color(0xFF1E7F62),
        ornament = Color(0xFFD4AF37),
        ornamentAlpha = 0.28f,
        visualKeywords = listOf("Emerald", "Teal", "Gold", "Geometry")
    ),
    AppPalette.CHRISTIAN to MatreeVisualFamily(
        palette = AppPalette.CHRISTIAN,
        displayName = "Christian",
        shortDescription = "White, chapel blue and soft-gold accents",
        heroEyebrow = "MATREE CHRISTIAN",
        heroTitle = "Bright, graceful connections",
        heroSubtitle = "A white-and-blue Matree experience with restrained chapel arches, luminous lines and soft gold.",
        motif = MatreeMotif.STAINED_ARCH,
        accent = Color(0xFF1E4DB7),
        accentSecondary = Color(0xFF6A8ED8),
        ornament = Color(0xFFD4AF37),
        ornamentAlpha = 0.25f,
        visualKeywords = listOf("White", "Blue", "Gold", "Light")
    ),
    AppPalette.SIKH to MatreeVisualFamily(
        palette = AppPalette.SIKH,
        displayName = "Sikh",
        shortDescription = "Gold, saffron and deep-navy accents",
        heroEyebrow = "MATREE SIKH",
        heroTitle = "Strong values, warm connections",
        heroSubtitle = "A gold-led Matree experience with deep navy contrast and restrained heritage architecture.",
        motif = MatreeMotif.GOLDEN_ARCH,
        accent = Color(0xFFD4AF37),
        accentSecondary = Color(0xFFFF8A00),
        ornament = Color(0xFF0B2D5B),
        ornamentAlpha = 0.22f,
        visualKeywords = listOf("Gold", "Saffron", "Navy", "Heritage")
    ),
    AppPalette.BUDDHIST to MatreeVisualFamily(
        palette = AppPalette.BUDDHIST,
        displayName = "Buddhist",
        shortDescription = "Saffron, lotus pink and calm earth accents",
        heroEyebrow = "MATREE BUDDHIST",
        heroTitle = "Calm design for meaningful journeys",
        heroSubtitle = "A mindful Matree experience with generous space, lotus forms and quiet natural warmth.",
        motif = MatreeMotif.LOTUS_STUPA,
        accent = Color(0xFFE19A2F),
        accentSecondary = Color(0xFFF3B4C6),
        ornament = Color(0xFF8BAE8F),
        ornamentAlpha = 0.24f,
        visualKeywords = listOf("Saffron", "Lotus", "Earth", "Calm")
    ),
    AppPalette.JAIN to MatreeVisualFamily(
        palette = AppPalette.JAIN,
        displayName = "Jain",
        shortDescription = "Ivory, muted gold and leaf-green accents",
        heroEyebrow = "MATREE JAIN",
        heroTitle = "Pure, restrained and value-led",
        heroSubtitle = "An airy Matree experience with marble-like surfaces, clean symmetry and subtle lotus detail.",
        motif = MatreeMotif.MARBLE_LOTUS,
        accent = Color(0xFF537F46),
        accentSecondary = Color(0xFFD4AF7C),
        ornament = Color(0xFFD4AF7C),
        ornamentAlpha = 0.23f,
        visualKeywords = listOf("Ivory", "Gold", "Sage", "Symmetry")
    ),
    AppPalette.PARSI to MatreeVisualFamily(
        palette = AppPalette.PARSI,
        displayName = "Parsi",
        shortDescription = "Teal, antique gold and heritage-navy accents",
        heroEyebrow = "MATREE PARSI",
        heroTitle = "Timeless character, modern connections",
        heroSubtitle = "A refined Matree experience with heritage teal, antique gold and restrained Art Deco geometry.",
        motif = MatreeMotif.HERITAGE_DECO,
        accent = Color(0xFF0F6B6A),
        accentSecondary = Color(0xFFC9A96B),
        ornament = Color(0xFF1E3A5F),
        ornamentAlpha = 0.22f,
        visualKeywords = listOf("Teal", "Gold", "Navy", "Deco")
    )
)

internal fun visualFamilyFor(palette: AppPalette): MatreeVisualFamily =
    visualFamilies[palette] ?: visualFamilies.getValue(AppPalette.VIVAH)
