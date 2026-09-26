package com.match.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.match.app.domain.model.ReligionCategory

/**
 * Brand palettes available to the user. Religion palettes are optional visual
 * treatments only; the default remains the neutral VIVAH matrimony theme.
 * They intentionally avoid sacred text/symbols so the experience stays tasteful
 * and inclusive while still feeling culturally distinct.
 */
enum class AppPalette(val label: String, val swatch: Color) {
    ROSE    ("Rose",     Color(0xFFFF5A7D)),
    LAVENDER("Lavender", Color(0xFF7A4E9C)),
    SOLAR   ("Solar",    Color(0xFFFF8F00)),
    OCEAN   ("Ocean",    Color(0xFF0277BD)),
    MONO    ("Mono",     Color(0xFF424242)),
    GLACIER ("Glacier",  Color(0xFF4FC3F7)),
    TELUGU  ("Telugu",   Color(0xFFD4A017)),
    VIVAH   ("Matree Neutral", Color(0xFF475569)),
    HINDU   ("Hindu",    Color(0xFFC76B00)),
    CHRISTIAN("Christian", Color(0xFF315E8A)),
    MUSLIM  ("Muslim",   Color(0xFF17705A)),
    SIKH    ("Sikh",     Color(0xFFB77900)),
    BUDDHIST("Buddhist", Color(0xFF8A3B12)),
    JAIN    ("Jain",     Color(0xFFA13D4A)),
    PARSI   ("Parsi",    Color(0xFF4B5AA7)),
    COMMUNITY("Community", Color(0xFF6D4C7D));

    companion object {
        fun fromKey(key: String?): AppPalette =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: VIVAH

        fun forReligion(category: ReligionCategory): AppPalette = when (category) {
            ReligionCategory.HINDU -> HINDU
            ReligionCategory.CHRISTIAN -> CHRISTIAN
            ReligionCategory.MUSLIM -> MUSLIM
            ReligionCategory.SIKH -> SIKH
            ReligionCategory.BUDDHIST -> BUDDHIST
            ReligionCategory.JAIN -> JAIN
            ReligionCategory.PARSI -> PARSI
            ReligionCategory.OTHER -> COMMUNITY
        }
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
        primary = Color(0xFF475569), primaryContainer = Color(0xFFE2E8F0), onPrimaryContainer = Color(0xFF0F172A),
        secondary = Color(0xFF64748B), secondaryContainer = Color(0xFFF1F5F9), onSecondaryContainer = Color(0xFF1E293B),
        tertiary = Color(0xFF4F6B5A),
        dark = dark
    )
    AppPalette.HINDU -> scheme(
        primary = Color(0xFFC76B00), primaryContainer = Color(0xFFFFE8C2), onPrimaryContainer = Color(0xFF4A2500),
        secondary = Color(0xFF8C4A3A), secondaryContainer = Color(0xFFFFDED6), onSecondaryContainer = Color(0xFF4B1710),
        tertiary = Color(0xFF7C6A00),
        dark = dark
    )
    AppPalette.CHRISTIAN -> scheme(
        primary = Color(0xFF315E8A), primaryContainer = Color(0xFFD8E9FA), onPrimaryContainer = Color(0xFF0D2B47),
        secondary = Color(0xFF79536B), secondaryContainer = Color(0xFFF3DCE9), onSecondaryContainer = Color(0xFF3B2030),
        tertiary = Color(0xFF476A5A),
        dark = dark
    )
    AppPalette.MUSLIM -> scheme(
        primary = Color(0xFF17705A), primaryContainer = Color(0xFFCDEEE4), onPrimaryContainer = Color(0xFF073D30),
        secondary = Color(0xFF78613A), secondaryContainer = Color(0xFFF2E2C2), onSecondaryContainer = Color(0xFF3D2D12),
        tertiary = Color(0xFF386A74),
        dark = dark
    )
    AppPalette.SIKH -> scheme(
        primary = Color(0xFF9A6700), primaryContainer = Color(0xFFFFE7A8), onPrimaryContainer = Color(0xFF352300),
        secondary = Color(0xFF3D5268), secondaryContainer = Color(0xFFDCE7F1), onSecondaryContainer = Color(0xFF172A3B),
        tertiary = Color(0xFF735C00), dark = dark
    )
    AppPalette.BUDDHIST -> scheme(
        primary = Color(0xFF8A3B12), primaryContainer = Color(0xFFFFDBCA), onPrimaryContainer = Color(0xFF3A1000),
        secondary = Color(0xFF8A6D00), secondaryContainer = Color(0xFFFFEFAF), onSecondaryContainer = Color(0xFF2D2400),
        tertiary = Color(0xFF5C6840), dark = dark
    )
    AppPalette.JAIN -> scheme(
        primary = Color(0xFFA13D4A), primaryContainer = Color(0xFFFFDADD), onPrimaryContainer = Color(0xFF41000A),
        secondary = Color(0xFF826500), secondaryContainer = Color(0xFFFFEFAE), onSecondaryContainer = Color(0xFF2A2100),
        tertiary = Color(0xFF52664B), dark = dark
    )
    AppPalette.PARSI -> scheme(
        primary = Color(0xFF4B5AA7), primaryContainer = Color(0xFFE0E3FF), onPrimaryContainer = Color(0xFF101A58),
        secondary = Color(0xFF8A6500), secondaryContainer = Color(0xFFFFE9A9), onSecondaryContainer = Color(0xFF2D2100),
        tertiary = Color(0xFF4C6658), dark = dark
    )
    AppPalette.COMMUNITY -> scheme(
        primary = Color(0xFF6D4C7D), primaryContainer = Color(0xFFEEDDF5), onPrimaryContainer = Color(0xFF351A42),
        secondary = Color(0xFF8B5E66), secondaryContainer = Color(0xFFF8DDE1), onSecondaryContainer = Color(0xFF461F26),
        tertiary = Color(0xFF4C6658),
        dark = dark
    )
}
