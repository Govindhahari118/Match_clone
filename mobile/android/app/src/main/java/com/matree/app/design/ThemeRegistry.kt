package com.matree.app.design

import androidx.compose.ui.graphics.Color

object ThemeRegistry {
    private val universal = MatreeThemeTokens(
        appearanceTheme = AppearanceTheme.UNIVERSAL,
        referenceStatus = ReferenceStatus.APPROVED_DIRECTION,
        colors = MatreeColorTokens(
            backgroundPrimary = Color(0xFFFFFAF7),
            backgroundSecondary = Color(0xFFF7F2EF),
            surfacePrimary = Color(0xFFFFFEFC),
            surfaceElevated = Color(0xFFFFFFFF),
            surfaceSubtle = Color(0xFFF4EEEB),
            textPrimary = Color(0xFF251C1A),
            textSecondary = Color(0xFF6C5C58),
            textOnAccent = Color(0xFFFFFFFF),
            textDisabled = Color(0xFFAA9E9A),
            brandPrimary = Color(0xFF8B3F4D),
            brandSecondary = Color(0xFFC47D75),
            brandAccent = Color(0xFFE7B881),
            actionPrimary = Color(0xFF8B3F4D),
            actionSecondary = Color(0xFFF4E6E2),
            actionDestructive = Color(0xFFB3261E),
            success = Color(0xFF2F7D5A),
            warning = Color(0xFF9A6700),
            error = Color(0xFFB3261E),
            info = Color(0xFF356A8B),
            borderDefault = Color(0xFFD8CBC6),
            borderSubtle = Color(0xFFECE2DE),
            divider = Color(0xFFE6DCD8),
        ),
        gradients = MatreeGradientTokens(
            hero = listOf(Color(0xFFFFF4EC), Color(0xFFF8E3DF)),
            header = listOf(Color(0xFFFFFBF8), Color(0xFFF8EFEA)),
            selected = listOf(Color(0xFF9B4D58), Color(0xFF7A3443)),
            background = listOf(Color(0xFFFFFCFA), Color(0xFFF7F1EE)),
        ),
        atmosphere = AtmosphereStyle.NEUTRAL_HALO,
    )

    private val hindu = universal.copy(
        appearanceTheme = AppearanceTheme.HINDU,
        colors = universal.colors.copy(
            backgroundPrimary = Color(0xFFFFF7EB),
            backgroundSecondary = Color(0xFFFFEFD8),
            surfaceSubtle = Color(0xFFFFE9CF),
            textPrimary = Color(0xFF32170F),
            textSecondary = Color(0xFF745043),
            brandPrimary = Color(0xFFB44A1B),
            brandSecondary = Color(0xFF7D2633),
            brandAccent = Color(0xFFC99A43),
            actionPrimary = Color(0xFF9E3F1C),
            actionSecondary = Color(0xFFFFE5C4),
            borderDefault = Color(0xFFE5C7A2),
            borderSubtle = Color(0xFFF4DDC2),
            divider = Color(0xFFEBD4B9),
        ),
        gradients = MatreeGradientTokens(
            hero = listOf(Color(0xFFFFE4B7), Color(0xFFF5B874), Color(0xFF8F2C31)),
            header = listOf(Color(0xFFFFF6E8), Color(0xFFFFE7C0)),
            selected = listOf(Color(0xFFB94E20), Color(0xFF7D2633)),
            background = listOf(Color(0xFFFFFBF4), Color(0xFFFFEBD0)),
        ),
        atmosphere = AtmosphereStyle.WARM_ARCH,
    )

    private val muslim = universal.copy(
        appearanceTheme = AppearanceTheme.MUSLIM,
        colors = universal.colors.copy(
            backgroundPrimary = Color(0xFFF5FAF6),
            backgroundSecondary = Color(0xFFEAF5EF),
            surfacePrimary = Color(0xFFFCFFFD),
            surfaceSubtle = Color(0xFFE7F2EC),
            textPrimary = Color(0xFF132A21),
            textSecondary = Color(0xFF4F6D61),
            brandPrimary = Color(0xFF176B4A),
            brandSecondary = Color(0xFF267A72),
            brandAccent = Color(0xFFC8A552),
            actionPrimary = Color(0xFF176B4A),
            actionSecondary = Color(0xFFDDEFE5),
            borderDefault = Color(0xFFBFD8CB),
            borderSubtle = Color(0xFFDDEBE3),
            divider = Color(0xFFD5E5DC),
        ),
        gradients = MatreeGradientTokens(
            hero = listOf(Color(0xFFE9F6EF), Color(0xFFBCDCCB), Color(0xFF176B4A)),
            header = listOf(Color(0xFFF8FCF9), Color(0xFFE3F2EA)),
            selected = listOf(Color(0xFF227A58), Color(0xFF145A45)),
            background = listOf(Color(0xFFFBFEFC), Color(0xFFEAF5EF)),
        ),
        atmosphere = AtmosphereStyle.GEOMETRIC,
    )

    private val christian = universal.copy(
        appearanceTheme = AppearanceTheme.CHRISTIAN,
        colors = universal.colors.copy(
            backgroundPrimary = Color(0xFFF8FBFF),
            backgroundSecondary = Color(0xFFEDF4FC),
            surfacePrimary = Color(0xFFFFFFFF),
            surfaceSubtle = Color(0xFFEAF2FB),
            textPrimary = Color(0xFF17263A),
            textSecondary = Color(0xFF5B6D82),
            brandPrimary = Color(0xFF2D6098),
            brandSecondary = Color(0xFF7CA6D2),
            brandAccent = Color(0xFFD5B56D),
            actionPrimary = Color(0xFF2D6098),
            actionSecondary = Color(0xFFE1ECF8),
            borderDefault = Color(0xFFC8D8E8),
            borderSubtle = Color(0xFFE2ECF5),
            divider = Color(0xFFDCE7F2),
        ),
        gradients = MatreeGradientTokens(
            hero = listOf(Color(0xFFFFFFFF), Color(0xFFDCEBFA), Color(0xFF8EB5DC)),
            header = listOf(Color(0xFFFFFFFF), Color(0xFFEDF5FD)),
            selected = listOf(Color(0xFF487DB2), Color(0xFF2D6098)),
            background = listOf(Color(0xFFFFFFFF), Color(0xFFEEF5FC)),
        ),
        atmosphere = AtmosphereStyle.SOFT_LIGHT,
    )

    private val sikh = universal.copy(
        appearanceTheme = AppearanceTheme.SIKH,
        colors = universal.colors.copy(
            backgroundPrimary = Color(0xFFFFFAF0),
            backgroundSecondary = Color(0xFFF2F3FA),
            surfaceSubtle = Color(0xFFF0F1F8),
            textPrimary = Color(0xFF17203D),
            textSecondary = Color(0xFF59607A),
            brandPrimary = Color(0xFF263E7C),
            brandSecondary = Color(0xFFE89024),
            brandAccent = Color(0xFFC8A152),
            actionPrimary = Color(0xFF263E7C),
            actionSecondary = Color(0xFFE8EBF6),
            borderDefault = Color(0xFFC9CCE0),
            borderSubtle = Color(0xFFE5E6F0),
            divider = Color(0xFFDEDFF0),
        ),
        gradients = MatreeGradientTokens(
            hero = listOf(Color(0xFFF8F0DD), Color(0xFFE6B25D), Color(0xFF304A89)),
            header = listOf(Color(0xFFFFFCF5), Color(0xFFEFF1F8)),
            selected = listOf(Color(0xFF355294), Color(0xFF22356B)),
            background = listOf(Color(0xFFFFFCF5), Color(0xFFF0F2F9)),
        ),
        atmosphere = AtmosphereStyle.INDIGO_WEAVE,
    )

    fun tokensFor(theme: AppearanceTheme): MatreeThemeTokens = when (theme) {
        AppearanceTheme.UNIVERSAL -> universal
        AppearanceTheme.HINDU -> hindu
        AppearanceTheme.MUSLIM -> muslim
        AppearanceTheme.CHRISTIAN -> christian
        AppearanceTheme.SIKH -> sikh
        AppearanceTheme.BUDDHIST,
        AppearanceTheme.JAIN,
        AppearanceTheme.PARSI,
        AppearanceTheme.OTHER,
        -> universal.copy(
            appearanceTheme = theme,
            referenceStatus = ReferenceStatus.REFERENCE_REQUIRED,
        )
    }

    fun descriptors(): List<MatreeThemeTokens> = AppearanceTheme.entries.map(::tokensFor)
}
