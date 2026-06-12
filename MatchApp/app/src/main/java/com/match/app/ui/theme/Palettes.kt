package com.match.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Brand palettes available to the user, mirroring the web app's theme picker
 * (rose, lavender, solar, ocean, mono).  Each palette ships a light & dark
 * Material 3 [ColorScheme] so the entire app re-themes consistently.
 */
enum class AppPalette(val label: String, val swatch: Color) {
    ROSE    ("Rose",     Color(0xFFFF5A7D)),
    LAVENDER("Lavender", Color(0xFF7A4E9C)),
    SOLAR   ("Solar",    Color(0xFFFF8F00)),
    OCEAN   ("Ocean",    Color(0xFF0277BD)),
    MONO    ("Mono",     Color(0xFF424242)),
    GLACIER ("Glacier",  Color(0xFF4FC3F7)),
    TELUGU  ("Telugu",   Color(0xFFD4A017)),
    VIVAH   ("Vivah",    Color(0xFF8B1A1A));   // Maroon + Gold — industry standard (Shaadi/BharatMatrimony style)

    companion object {
        fun fromKey(key: String?): AppPalette =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: ROSE
    }
}

private fun scheme(
    primary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color = Color(0xFF2E7D32),
    dark: Boolean
): ColorScheme {
    val ink   = Color(0xFF0B0B14)
    val cloud = Color(0xFFFDF7F7)
    val cream = Color(0xFFFFF8F6)
    val midGray = Color(0xFF64607A)
    val darkSurface = Color(0xFF141423)
    val darkCard    = Color(0xFF1E1E30)
    return if (dark) darkColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = onPrimaryContainer, onPrimaryContainer = primaryContainer,
        secondary = secondary, onSecondary = Color.White,
        secondaryContainer = onSecondaryContainer, onSecondaryContainer = secondaryContainer,
        tertiary = tertiary, onTertiary = Color.Black,
        background = ink, onBackground = cloud,
        surface = darkSurface, onSurface = cloud,
        surfaceVariant = darkCard, onSurfaceVariant = Color(0xFFB0ABB8),
        outline = Color(0xFF4A4458), outlineVariant = Color(0xFF36304A)
    ) else lightColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = Color.White,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD7F5DA), onTertiaryContainer = Color(0xFF0A3A0D),
        background = cloud, onBackground = ink,
        surface = Color.White, onSurface = ink,
        surfaceVariant = cream, onSurfaceVariant = midGray,
        outline = Color(0xFFD4CEDB), outlineVariant = Color(0xFFECE7F0)
    )
}

internal fun colorSchemeFor(palette: AppPalette, dark: Boolean): ColorScheme = when (palette) {
    AppPalette.ROSE -> scheme(
        primary = Color(0xFFFF5A7D), primaryContainer = Color(0xFFFFE0E6), onPrimaryContainer = Color(0xFF5C0023),
        secondary = Color(0xFF7A4E9C), secondaryContainer = Color(0xFFEDE0F5), onSecondaryContainer = Color(0xFF33175A),
        dark = dark
    )
    AppPalette.LAVENDER -> scheme(
        primary = Color(0xFF7A4E9C), primaryContainer = Color(0xFFEDE0F5), onPrimaryContainer = Color(0xFF33175A),
        secondary = Color(0xFFFF5A7D), secondaryContainer = Color(0xFFFFE0E6), onSecondaryContainer = Color(0xFF5C0023),
        dark = dark
    )
    AppPalette.SOLAR -> scheme(
        primary = Color(0xFFFF8F00), primaryContainer = Color(0xFFFFE7B0), onPrimaryContainer = Color(0xFF4A2A00),
        secondary = Color(0xFFE65100), secondaryContainer = Color(0xFFFFD2A8), onSecondaryContainer = Color(0xFF3F1A00),
        dark = dark
    )
    AppPalette.OCEAN -> scheme(
        primary = Color(0xFF0277BD), primaryContainer = Color(0xFFCFE7F5), onPrimaryContainer = Color(0xFF002B45),
        secondary = Color(0xFF00838F), secondaryContainer = Color(0xFFB8E5EA), onSecondaryContainer = Color(0xFF002429),
        dark = dark
    )
    AppPalette.MONO -> scheme(
        primary = Color(0xFF424242), primaryContainer = Color(0xFFE0E0E0), onPrimaryContainer = Color(0xFF1B1B1B),
        secondary = Color(0xFF616161), secondaryContainer = Color(0xFFEEEEEE), onSecondaryContainer = Color(0xFF1B1B1B),
        dark = dark
    )
    AppPalette.GLACIER -> scheme(
        primary = Color(0xFF4FC3F7), primaryContainer = Color(0xFFE1F5FE), onPrimaryContainer = Color(0xFF01579B),
        secondary = Color(0xFF80DEEA), secondaryContainer = Color(0xFFE0F7FA), onSecondaryContainer = Color(0xFF004D40),
        dark = dark
    )
    AppPalette.TELUGU -> scheme(
        primary = Color(0xFFD4A017), primaryContainer = Color(0xFFFFF8E1), onPrimaryContainer = Color(0xFF5D4037),
        secondary = Color(0xFFC62828), secondaryContainer = Color(0xFFFFCDD2), onSecondaryContainer = Color(0xFF4A0000),
        tertiary = Color(0xFF1B5E20),
        dark = dark
    )
    AppPalette.VIVAH -> scheme(
        primary = Color(0xFF8B1A1A), primaryContainer = Color(0xFFFFE8E8), onPrimaryContainer = Color(0xFF3E0000),
        secondary = Color(0xFFB8860B), secondaryContainer = Color(0xFFFFF8DC), onSecondaryContainer = Color(0xFF4A3000),
        tertiary = Color(0xFF1B5E20),
        dark = dark
    )
}
