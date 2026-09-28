package com.matree.app.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MatreeColorTokens(
    val backgroundPrimary: Color,
    val backgroundSecondary: Color,
    val surfacePrimary: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textOnAccent: Color,
    val textDisabled: Color,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val brandAccent: Color,
    val actionPrimary: Color,
    val actionSecondary: Color,
    val actionDestructive: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val borderDefault: Color,
    val borderSubtle: Color,
    val divider: Color,
)

data class MatreeGradientTokens(
    val hero: List<Color>,
    val header: List<Color>,
    val selected: List<Color>,
    val background: List<Color>,
)

data class MatreeRadiusTokens(
    val small: Dp = 10.dp,
    val medium: Dp = 14.dp,
    val large: Dp = 20.dp,
    val card: Dp = 24.dp,
    val button: Dp = 18.dp,
    val sheet: Dp = 28.dp,
)

data class MatreeSpacingTokens(
    val x2s: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val x2l: Dp = 40.dp,
)

data class MatreeElevationTokens(
    val low: Dp = 1.dp,
    val medium: Dp = 4.dp,
    val high: Dp = 10.dp,
)

data class MatreeTypographyTokens(
    val display: TextStyle = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold),
    val heading1: TextStyle = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    val heading2: TextStyle = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    val heading3: TextStyle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    val title: TextStyle = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    val body: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    val bodySecondary: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    val caption: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

enum class AtmosphereStyle {
    NEUTRAL_HALO,
    WARM_ARCH,
    GEOMETRIC,
    SOFT_LIGHT,
    INDIGO_WEAVE,
}

data class MatreeThemeTokens(
    val appearanceTheme: AppearanceTheme,
    val referenceStatus: ReferenceStatus,
    val colors: MatreeColorTokens,
    val gradients: MatreeGradientTokens,
    val atmosphere: AtmosphereStyle,
    val radii: MatreeRadiusTokens = MatreeRadiusTokens(),
    val spacing: MatreeSpacingTokens = MatreeSpacingTokens(),
    val elevation: MatreeElevationTokens = MatreeElevationTokens(),
    val typography: MatreeTypographyTokens = MatreeTypographyTokens(),
)
