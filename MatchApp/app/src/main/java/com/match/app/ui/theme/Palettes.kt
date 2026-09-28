package com.match.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.match.app.domain.model.ReligionCategory

/**
 * Brand palettes available to the user. Religion palettes are optional visual
 * treatments only; the default remains the premium Matree Signature matrimonial theme.
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
    VIVAH   ("Matree Signature", Color(0xFF6E2F4B)),
    HINDU   ("Hindu",    Color(0xFFFF8A00)),
    CHRISTIAN("Christian", Color(0xFF1E4DB7)),
    MUSLIM  ("Muslim",   Color(0xFF0B6B53)),
    SIKH    ("Sikh",     Color(0xFFD4AF37)),
    BUDDHIST("Buddhist", Color(0xFFE19A2F)),
    JAIN    ("Jain",     Color(0xFF537F46)),
    PARSI   ("Parsi",    Color(0xFF0F6B6A)),
    COMMUNITY("Community", Color(0xFF6D4C7D));

    companion object {
        val productionThemes: List<AppPalette> = listOf(
            VIVAH, HINDU, MUSLIM, CHRISTIAN, SIKH, BUDDHIST, JAIN, PARSI
        )

        fun fromKey(key: String?): AppPalette =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: VIVAH

        /** Only themes intentionally exposed by the current Matree appearance contract. */
        fun fromProductionKey(key: String?): AppPalette =
            productionThemes.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: VIVAH

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
    lightBackground: Color = Color(0xFFFDF7F7),
    lightSurfaceVariant: Color = Color(0xFFFFF8F6),
    darkBackground: Color = Color(0xFF0B0B14),
    darkSurface: Color = Color(0xFF141423),
    darkSurfaceVariant: Color = Color(0xFF1E1E30),
    dark: Boolean
): ColorScheme {
    val ink   = darkBackground
    val cloud = lightBackground
    val cream = lightSurfaceVariant
    val midGray = Color(0xFF64607A)
    val darkCard = darkSurfaceVariant
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
        primary = Color(0xFF6E2F4B), primaryContainer = Color(0xFFF5DDE7), onPrimaryContainer = Color(0xFF3A1426),
        secondary = Color(0xFF8B5E57), secondaryContainer = Color(0xFFF3E4DE), onSecondaryContainer = Color(0xFF3D2721),
        tertiary = Color(0xFF7A6434),
        lightBackground = Color(0xFFFFF9F6),
        lightSurfaceVariant = Color(0xFFF8EEE9),
        darkBackground = Color(0xFF150E12),
        darkSurface = Color(0xFF21161C),
        darkSurfaceVariant = Color(0xFF2D2027),
        dark = dark
    )
    AppPalette.HINDU -> scheme(
        primary = Color(0xFFB85B00), primaryContainer = Color(0xFFFFE8C2), onPrimaryContainer = Color(0xFF4A2500),
        secondary = Color(0xFF8C4A3A), secondaryContainer = Color(0xFFFFDED6), onSecondaryContainer = Color(0xFF4B1710),
        tertiary = Color(0xFF7C6A00),
        lightBackground = Color(0xFFFFFBF5),
        lightSurfaceVariant = Color(0xFFFFF4E2),
        darkBackground = Color(0xFF17110B),
        darkSurface = Color(0xFF211810),
        darkSurfaceVariant = Color(0xFF2C2116),
        dark = dark
    )
    AppPalette.CHRISTIAN -> scheme(
        primary = Color(0xFF1E4DB7), primaryContainer = Color(0xFFE6F0FF), onPrimaryContainer = Color(0xFF0A2A6B),
        secondary = Color(0xFF8A6A13), secondaryContainer = Color(0xFFFFF0C2), onSecondaryContainer = Color(0xFF3C2C00),
        tertiary = Color(0xFF5D6F7F),
        lightBackground = Color(0xFFFFFEFC),
        lightSurfaceVariant = Color(0xFFF4F7FB),
        darkBackground = Color(0xFF0B131B),
        darkSurface = Color(0xFF101D29),
        darkSurfaceVariant = Color(0xFF172838),
        dark = dark
    )
    AppPalette.MUSLIM -> scheme(
        primary = Color(0xFF0B6B53), primaryContainer = Color(0xFFD9F2E9), onPrimaryContainer = Color(0xFF053B2E),
        secondary = Color(0xFF8A6A13), secondaryContainer = Color(0xFFF5E8C5), onSecondaryContainer = Color(0xFF3B2B00),
        tertiary = Color(0xFF0F4D4A),
        lightBackground = Color(0xFFFAFCF8),
        lightSurfaceVariant = Color(0xFFEEF7F2),
        darkBackground = Color(0xFF091512),
        darkSurface = Color(0xFF10211C),
        darkSurfaceVariant = Color(0xFF173029),
        dark = dark
    )
    AppPalette.SIKH -> scheme(
        primary = Color(0xFF8C6100), primaryContainer = Color(0xFFFFE8A6), onPrimaryContainer = Color(0xFF352300),
        secondary = Color(0xFF0B2D5B), secondaryContainer = Color(0xFFDCE7F5), onSecondaryContainer = Color(0xFF0A2344),
        tertiary = Color(0xFF4F6D58),
        lightBackground = Color(0xFFFFFBF2),
        lightSurfaceVariant = Color(0xFFFFF5DF),
        darkBackground = Color(0xFF151108),
        darkSurface = Color(0xFF211A0D),
        darkSurfaceVariant = Color(0xFF2D2412),
        dark = dark
    )
    AppPalette.BUDDHIST -> scheme(
        primary = Color(0xFF9A4D16), primaryContainer = Color(0xFFFFE3C7), onPrimaryContainer = Color(0xFF431B00),
        secondary = Color(0xFF6B7457), secondaryContainer = Color(0xFFE6EDD9), onSecondaryContainer = Color(0xFF27301F),
        tertiary = Color(0xFF8A5A6D),
        lightBackground = Color(0xFFFFFAF2),
        lightSurfaceVariant = Color(0xFFF7EFE2),
        darkBackground = Color(0xFF17100B),
        darkSurface = Color(0xFF24170F),
        darkSurfaceVariant = Color(0xFF302016),
        dark = dark
    )
    AppPalette.JAIN -> scheme(
        primary = Color(0xFF537F46), primaryContainer = Color(0xFFE1EFD9), onPrimaryContainer = Color(0xFF173415),
        secondary = Color(0xFF8A6A2D), secondaryContainer = Color(0xFFF3E3BE), onSecondaryContainer = Color(0xFF3A2909),
        tertiary = Color(0xFF8A6E52),
        lightBackground = Color(0xFFFFFDF8),
        lightSurfaceVariant = Color(0xFFF8F3E9),
        darkBackground = Color(0xFF160C0F),
        darkSurface = Color(0xFF231216),
        darkSurfaceVariant = Color(0xFF30191F),
        dark = dark
    )
    AppPalette.PARSI -> scheme(
        primary = Color(0xFF0F6B6A), primaryContainer = Color(0xFFD8EFEC), onPrimaryContainer = Color(0xFF043938),
        secondary = Color(0xFF8A6A2A), secondaryContainer = Color(0xFFF1E2BE), onSecondaryContainer = Color(0xFF392900),
        tertiary = Color(0xFF1E3A5F),
        lightBackground = Color(0xFFFFFBF5),
        lightSurfaceVariant = Color(0xFFF4EFE8),
        darkBackground = Color(0xFF0E1020),
        darkSurface = Color(0xFF161A2B),
        darkSurfaceVariant = Color(0xFF20263A),
        dark = dark
    )
    AppPalette.COMMUNITY -> scheme(
        primary = Color(0xFF6D4C7D), primaryContainer = Color(0xFFEEDDF5), onPrimaryContainer = Color(0xFF351A42),
        secondary = Color(0xFF8B5E66), secondaryContainer = Color(0xFFF8DDE1), onSecondaryContainer = Color(0xFF461F26),
        tertiary = Color(0xFF4C6658),
        dark = dark
    )
}
