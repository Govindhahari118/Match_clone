package com.match.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand palette ───────────────────────────────────────────────────────────
private val Rose       = Color(0xFFFF5A7D)
private val RoseLight  = Color(0xFFFFE0E6)
private val Plum       = Color(0xFF7A4E9C)
private val PlumLight  = Color(0xFFEDE0F5)
private val Ink        = Color(0xFF0B0B14)
private val Cloud      = Color(0xFFFDF7F7)
private val Cream      = Color(0xFFFFF8F6)
private val MidGray    = Color(0xFF64607A)
private val GreenOk    = Color(0xFF2E7D32)
private val DarkSurface = Color(0xFF141423)
private val DarkCard    = Color(0xFF1E1E30)

private val LightScheme = lightColorScheme(
    primary = Rose,              onPrimary = Color.White,
    primaryContainer = RoseLight,onPrimaryContainer = Color(0xFF5C0023),
    secondary = Plum,            onSecondary = Color.White,
    secondaryContainer = PlumLight, onSecondaryContainer = Color(0xFF33175A),
    tertiary = GreenOk,          onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7F5DA), onTertiaryContainer = Color(0xFF0A3A0D),
    background = Cloud,          onBackground = Ink,
    surface = Color.White,       onSurface = Ink,
    surfaceVariant = Cream,      onSurfaceVariant = MidGray,
    outline = Color(0xFFD4CEDB), outlineVariant = Color(0xFFECE7F0)
)
private val DarkScheme = darkColorScheme(
    primary = Rose,              onPrimary = Color.White,
    primaryContainer = Color(0xFF5C0023), onPrimaryContainer = RoseLight,
    secondary = Plum,            onSecondary = Color.White,
    secondaryContainer = Color(0xFF33175A), onSecondaryContainer = PlumLight,
    tertiary = Color(0xFF81C784),onTertiary = Color.Black,
    background = Ink,            onBackground = Cloud,
    surface = DarkSurface,       onSurface = Cloud,
    surfaceVariant = DarkCard,   onSurfaceVariant = Color(0xFFB0ABB8),
    outline = Color(0xFF4A4458), outlineVariant = Color(0xFF36304A)
)

// ── Typography ──────────────────────────────────────────────────────────────
private val AppTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Bold,    fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold,    fontSize = 30.sp, lineHeight = 38.sp),
    displaySmall  = TextStyle(fontWeight = FontWeight.Bold,    fontSize = 26.sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,    fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 22.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.SemiBold,fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,  fontSize = 11.sp, lineHeight = 14.sp)
)

// ── Theme ───────────────────────────────────────────────────────────────────
@Composable
fun MatchTheme(
    darkMode: Boolean? = null,
    palette: AppPalette = AppPalette.ROSE,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = darkMode ?: systemDark
    val colorScheme = colorSchemeFor(palette, dark)
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
