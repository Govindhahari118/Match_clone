package com.match.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.ReligionId

/**
 * Brand palettes available to the user. Religion palettes are presentation only;
 * they never alter profile identity, matching, entitlement, privacy or security.
 */
enum class AppPalette(val label: String, val swatch: Color) {
    ROSE("Rose", Color(0xFFFF5A7D)),
    LAVENDER("Lavender", Color(0xFF7A4E9C)),
    SOLAR("Solar", Color(0xFFFF8F00)),
    OCEAN("Ocean", Color(0xFF0277BD)),
    MONO("Mono", Color(0xFF424242)),
    GLACIER("Glacier", Color(0xFF4FC3F7)),
    TELUGU("Telugu", Color(0xFFD4A017)),
    VIVAH("Matree Neutral", Color(0xFF8B1A1A)),
    HINDU("Hindu", Color(0xFFC76B00)),
    MUSLIM("Muslim", Color(0xFF17705A)),
    CHRISTIAN("Christian", Color(0xFF315E8A)),
    SIKH("Sikh", Color(0xFF9A6A00)),
    BUDDHIST("Buddhist", Color(0xFF7A263A)),
    JAIN("Jain", Color(0xFF8A3028)),
    PARSI("Parsi", Color(0xFF365A9C)),
    COMMUNITY("Community", Color(0xFF6D4C7D));

    companion object {
        fun fromKey(key: String?): AppPalette =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: VIVAH

        fun forReligion(category: ReligionCategory): AppPalette = forReligion(category.religionId)

        fun forReligion(religion: ReligionId): AppPalette = when (religion) {
            ReligionId.HINDU -> HINDU
            ReligionId.MUSLIM -> MUSLIM
            ReligionId.CHRISTIAN -> CHRISTIAN
            ReligionId.SIKH -> SIKH
            ReligionId.BUDDHIST -> BUDDHIST
            ReligionId.JAIN -> JAIN
            ReligionId.PARSI_ZOROASTRIAN -> PARSI
            ReligionId.OTHER, ReligionId.PREFER_NOT_TO_SAY -> VIVAH
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
    darkPrimary: Color = primary,
    darkSecondary: Color = secondary,
    dark: Boolean
): ColorScheme {
    val ink = Color(0xFF0B0B14)
    val cloud = Color(0xFFFDF7F7)
    val cream = Color(0xFFFFF8F6)
    val midGray = Color(0xFF64607A)
    val darkSurface = Color(0xFF141423)
    val darkCard = Color(0xFF1E1E30)

    return if (dark) darkColorScheme(
        primary = darkPrimary,
        onPrimary = Color.Black,
        primaryContainer = onPrimaryContainer,
        onPrimaryContainer = primaryContainer,
        secondary = darkSecondary,
        onSecondary = Color.Black,
        secondaryContainer = onSecondaryContainer,
        onSecondaryContainer = secondaryContainer,
        tertiary = Color(0xFF81C784),
        onTertiary = Color.Black,
        background = ink,
        onBackground = cloud,
        surface = darkSurface,
        onSurface = cloud,
        surfaceVariant = darkCard,
        onSurfaceVariant = Color(0xFFD1CAD9),
        outline = Color(0xFF746D82),
        outlineVariant = Color(0xFF4C465A)
    ) else lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD7F5DA),
        onTertiaryContainer = Color(0xFF0A3A0D),
        background = cloud,
        onBackground = ink,
        surface = Color.White,
        onSurface = ink,
        surfaceVariant = cream,
        onSurfaceVariant = midGray,
        outline = Color(0xFFD4CEDB),
        outlineVariant = Color(0xFFECE7F0)
    )
}

internal fun colorSchemeFor(palette: AppPalette, dark: Boolean): ColorScheme = when (palette) {
    AppPalette.ROSE -> scheme(
        primary = Color(0xFFFF5A7D), primaryContainer = Color(0xFFFFE0E6), onPrimaryContainer = Color(0xFF5C0023),
        secondary = Color(0xFF7A4E9C), secondaryContainer = Color(0xFFEDE0F5), onSecondaryContainer = Color(0xFF33175A),
        darkPrimary = Color(0xFFFFAFC0), darkSecondary = Color(0xFFD5B1EA), dark = dark
    )
    AppPalette.LAVENDER -> scheme(
        primary = Color(0xFF7A4E9C), primaryContainer = Color(0xFFEDE0F5), onPrimaryContainer = Color(0xFF33175A),
        secondary = Color(0xFFFF5A7D), secondaryContainer = Color(0xFFFFE0E6), onSecondaryContainer = Color(0xFF5C0023),
        darkPrimary = Color(0xFFD5B1EA), darkSecondary = Color(0xFFFFAFC0), dark = dark
    )
    AppPalette.SOLAR -> scheme(
        primary = Color(0xFFB85E00), primaryContainer = Color(0xFFFFE7B0), onPrimaryContainer = Color(0xFF4A2A00),
        secondary = Color(0xFF9A3E00), secondaryContainer = Color(0xFFFFD2A8), onSecondaryContainer = Color(0xFF3F1A00),
        darkPrimary = Color(0xFFFFB95C), darkSecondary = Color(0xFFFFB27A), dark = dark
    )
    AppPalette.OCEAN -> scheme(
        primary = Color(0xFF0277BD), primaryContainer = Color(0xFFCFE7F5), onPrimaryContainer = Color(0xFF002B45),
        secondary = Color(0xFF00838F), secondaryContainer = Color(0xFFB8E5EA), onSecondaryContainer = Color(0xFF002429),
        darkPrimary = Color(0xFF8DCDFA), darkSecondary = Color(0xFF80D8E2), dark = dark
    )
    AppPalette.MONO -> scheme(
        primary = Color(0xFF424242), primaryContainer = Color(0xFFE0E0E0), onPrimaryContainer = Color(0xFF1B1B1B),
        secondary = Color(0xFF616161), secondaryContainer = Color(0xFFEEEEEE), onSecondaryContainer = Color(0xFF1B1B1B),
        darkPrimary = Color(0xFFC9C9C9), darkSecondary = Color(0xFFBDBDBD), dark = dark
    )
    AppPalette.GLACIER -> scheme(
        primary = Color(0xFF0277BD), primaryContainer = Color(0xFFE1F5FE), onPrimaryContainer = Color(0xFF01579B),
        secondary = Color(0xFF007C91), secondaryContainer = Color(0xFFE0F7FA), onSecondaryContainer = Color(0xFF004D40),
        darkPrimary = Color(0xFF82D5FA), darkSecondary = Color(0xFF80DEEA), dark = dark
    )
    AppPalette.TELUGU -> scheme(
        primary = Color(0xFF9A6A00), primaryContainer = Color(0xFFFFF8E1), onPrimaryContainer = Color(0xFF5D4037),
        secondary = Color(0xFFC62828), secondaryContainer = Color(0xFFFFCDD2), onSecondaryContainer = Color(0xFF4A0000),
        tertiary = Color(0xFF1B5E20), darkPrimary = Color(0xFFE7C45D), darkSecondary = Color(0xFFFF8A80), dark = dark
    )
    AppPalette.VIVAH -> scheme(
        primary = Color(0xFF8B1A1A), primaryContainer = Color(0xFFFFE8E8), onPrimaryContainer = Color(0xFF3E0000),
        secondary = Color(0xFF8A6500), secondaryContainer = Color(0xFFFFF8DC), onSecondaryContainer = Color(0xFF4A3000),
        tertiary = Color(0xFF1B5E20), darkPrimary = Color(0xFFFFB3B0), darkSecondary = Color(0xFFE8C66A), dark = dark
    )
    AppPalette.HINDU -> scheme(
        primary = Color(0xFF9A5000), primaryContainer = Color(0xFFFFE8C2), onPrimaryContainer = Color(0xFF4A2500),
        secondary = Color(0xFF7A4A00), secondaryContainer = Color(0xFFFFEDC6), onSecondaryContainer = Color(0xFF3A2500),
        tertiary = Color(0xFF6F6100), darkPrimary = Color(0xFFFFB866), darkSecondary = Color(0xFFE8C56B), dark = dark
    )
    AppPalette.MUSLIM -> scheme(
        primary = Color(0xFF11614D), primaryContainer = Color(0xFFCDEEE4), onPrimaryContainer = Color(0xFF073D30),
        secondary = Color(0xFF66502E), secondaryContainer = Color(0xFFF2E2C2), onSecondaryContainer = Color(0xFF3D2D12),
        tertiary = Color(0xFF386A74), darkPrimary = Color(0xFF82D6BC), darkSecondary = Color(0xFFD8C08D), dark = dark
    )
    AppPalette.CHRISTIAN -> scheme(
        primary = Color(0xFF315E8A), primaryContainer = Color(0xFFD8E9FA), onPrimaryContainer = Color(0xFF0D2B47),
        secondary = Color(0xFF4B6F91), secondaryContainer = Color(0xFFDDECF8), onSecondaryContainer = Color(0xFF16324A),
        tertiary = Color(0xFF476A5A), darkPrimary = Color(0xFFA4C9EF), darkSecondary = Color(0xFFAFCDEA), dark = dark
    )
    AppPalette.SIKH -> scheme(
        primary = Color(0xFF8A6200), primaryContainer = Color(0xFFFFEDB5), onPrimaryContainer = Color(0xFF3F2D00),
        secondary = Color(0xFF273A67), secondaryContainer = Color(0xFFDDE4FA), onSecondaryContainer = Color(0xFF111E40),
        tertiary = Color(0xFF5C6B36), darkPrimary = Color(0xFFE7C75F), darkSecondary = Color(0xFFB5C4F6), dark = dark
    )
    AppPalette.BUDDHIST -> scheme(
        primary = Color(0xFF7A263A), primaryContainer = Color(0xFFF9DDE3), onPrimaryContainer = Color(0xFF3C0C19),
        secondary = Color(0xFF8A5C00), secondaryContainer = Color(0xFFFFE8B5), onSecondaryContainer = Color(0xFF422B00),
        tertiary = Color(0xFF5D6740), darkPrimary = Color(0xFFE8A9B8), darkSecondary = Color(0xFFE6C166), dark = dark
    )
    AppPalette.JAIN -> scheme(
        primary = Color(0xFF8A3028), primaryContainer = Color(0xFFFFE1DC), onPrimaryContainer = Color(0xFF42110D),
        secondary = Color(0xFF806000), secondaryContainer = Color(0xFFFFF0B9), onSecondaryContainer = Color(0xFF3D2D00),
        tertiary = Color(0xFF52634E), darkPrimary = Color(0xFFFFB4AA), darkSecondary = Color(0xFFE5C75E), dark = dark
    )
    AppPalette.PARSI -> scheme(
        primary = Color(0xFF365A9C), primaryContainer = Color(0xFFDDE7FF), onPrimaryContainer = Color(0xFF102A55),
        secondary = Color(0xFF806000), secondaryContainer = Color(0xFFFFF0B9), onSecondaryContainer = Color(0xFF3D2D00),
        tertiary = Color(0xFF4B6658), darkPrimary = Color(0xFFAEC6FF), darkSecondary = Color(0xFFE5C75E), dark = dark
    )
    AppPalette.COMMUNITY -> scheme(
        primary = Color(0xFF6D4C7D), primaryContainer = Color(0xFFEEDDF5), onPrimaryContainer = Color(0xFF351A42),
        secondary = Color(0xFF8B5E66), secondaryContainer = Color(0xFFF8DDE1), onSecondaryContainer = Color(0xFF461F26),
        tertiary = Color(0xFF4C6658), darkPrimary = Color(0xFFD3B5E0), darkSecondary = Color(0xFFE3B8C0), dark = dark
    )
}
