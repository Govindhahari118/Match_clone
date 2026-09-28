package com.matree.app.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private val LocalMatreeThemeTokens = staticCompositionLocalOf {
    ThemeRegistry.tokensFor(AppearanceTheme.UNIVERSAL)
}

object MatreeTheme {
    val tokens: MatreeThemeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalMatreeThemeTokens.current
}

@Composable
fun MatreeTheme(
    appearanceTheme: AppearanceTheme,
    content: @Composable () -> Unit,
) {
    val tokens = ThemeRegistry.tokensFor(appearanceTheme)
    val c = tokens.colors

    val materialColors = lightColorScheme(
        primary = c.actionPrimary,
        onPrimary = c.textOnAccent,
        primaryContainer = c.actionSecondary,
        onPrimaryContainer = c.textPrimary,
        secondary = c.brandSecondary,
        onSecondary = c.textOnAccent,
        secondaryContainer = c.surfaceSubtle,
        onSecondaryContainer = c.textPrimary,
        tertiary = c.brandAccent,
        onTertiary = c.textPrimary,
        background = c.backgroundPrimary,
        onBackground = c.textPrimary,
        surface = c.surfacePrimary,
        onSurface = c.textPrimary,
        surfaceVariant = c.surfaceSubtle,
        onSurfaceVariant = c.textSecondary,
        outline = c.borderDefault,
        outlineVariant = c.borderSubtle,
        error = c.error,
        onError = Color.White,
    )

    val t = tokens.typography
    val typography = Typography(
        displayLarge = t.display,
        headlineLarge = t.heading1,
        headlineMedium = t.heading2,
        headlineSmall = t.heading3,
        titleLarge = t.title,
        bodyLarge = t.body,
        bodyMedium = t.bodySecondary,
        labelLarge = t.label,
        labelSmall = t.caption,
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalMatreeThemeTokens provides tokens,
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = typography,
            content = content,
        )
    }
}

internal fun TextStyle.withColor(color: Color): TextStyle = copy(color = color)
