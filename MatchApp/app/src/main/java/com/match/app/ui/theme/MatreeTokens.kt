package com.match.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MatreeSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val display: Dp = 48.dp
)

@Immutable
data class MatreeRadii(
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val card: Dp = 16.dp,
    val large: Dp = 20.dp,
    val hero: Dp = 28.dp
)

@Immutable
data class MatreeSizes(
    val touchTarget: Dp = 48.dp,
    val buttonHeight: Dp = 52.dp,
    val iconSmall: Dp = 16.dp,
    val icon: Dp = 24.dp,
    val iconLarge: Dp = 32.dp,
    val avatarCompact: Dp = 56.dp,
    val avatarStandard: Dp = 76.dp,
    val avatarHero: Dp = 104.dp,
    val bottomNavigation: Dp = 80.dp
)

@Immutable
data class MatreeElevation(
    val flat: Dp = 0.dp,
    val subtle: Dp = 1.dp,
    val card: Dp = 2.dp,
    val floating: Dp = 6.dp
)

@Immutable
data class MatreeSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val verified: Color,
    val premium: Color,
    val online: Color
)

private val LocalSpacing = staticCompositionLocalOf { MatreeSpacing() }
private val LocalRadii = staticCompositionLocalOf { MatreeRadii() }
private val LocalSizes = staticCompositionLocalOf { MatreeSizes() }
private val LocalElevation = staticCompositionLocalOf { MatreeElevation() }
private val LocalSemanticColors = staticCompositionLocalOf { lightSemanticColors() }

object MatreeDesign {
    val spacing: MatreeSpacing @Composable get() = LocalSpacing.current
    val radii: MatreeRadii @Composable get() = LocalRadii.current
    val sizes: MatreeSizes @Composable get() = LocalSizes.current
    val elevation: MatreeElevation @Composable get() = LocalElevation.current
    val colors: MatreeSemanticColors @Composable get() = LocalSemanticColors.current

    const val profilePhotoAspectRatio: Float = 4f / 5f
}

@Composable
internal fun ProvideMatreeDesignTokens(
    darkMode: Boolean,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSpacing provides MatreeSpacing(),
        LocalRadii provides MatreeRadii(),
        LocalSizes provides MatreeSizes(),
        LocalElevation provides MatreeElevation(),
        LocalSemanticColors provides if (darkMode) darkSemanticColors() else lightSemanticColors(),
        content = content
    )
}

private fun lightSemanticColors() = MatreeSemanticColors(
    success = Color(0xFF176B45),
    onSuccess = Color.White,
    successContainer = Color(0xFFD9F3E5),
    onSuccessContainer = Color(0xFF0B3B26),
    warning = Color(0xFF8A5800),
    onWarning = Color.White,
    warningContainer = Color(0xFFFFE8BD),
    onWarningContainer = Color(0xFF4A2D00),
    verified = Color(0xFF245F9E),
    premium = Color(0xFF7B5B00),
    online = Color(0xFF176B45)
)

private fun darkSemanticColors() = MatreeSemanticColors(
    success = Color(0xFF74D6A0),
    onSuccess = Color(0xFF06351F),
    successContainer = Color(0xFF164D33),
    onSuccessContainer = Color(0xFFD9F3E5),
    warning = Color(0xFFFFC96D),
    onWarning = Color(0xFF4A2D00),
    warningContainer = Color(0xFF5A3A00),
    onWarningContainer = Color(0xFFFFE8BD),
    verified = Color(0xFF9CCBFF),
    premium = Color(0xFFFFD875),
    online = Color(0xFF74D6A0)
)
