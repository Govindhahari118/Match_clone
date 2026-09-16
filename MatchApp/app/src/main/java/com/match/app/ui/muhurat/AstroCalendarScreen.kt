package com.match.app.ui.muhurat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.match.app.ui.components.MatreeScreen
import com.match.app.ui.components.MatreeUnavailableFeature

/** Muhurat dates must come from an explicit current source, never stale hard-coded fixtures. */
@Composable
fun AstroCalendarScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    MatreeScreen(title = "Muhurat Calendar", onBack = onBack) { padding ->
        MatreeUnavailableFeature(
            modifier = Modifier.padding(padding),
            title = "Muhurat calendar data is not active yet",
            description = "The previous prototype embedded fixed 2026 dates and could become stale or incorrect. Matree now waits for a current, attributable calendar source before presenting dates as auspicious guidance.",
            icon = Icons.Filled.CalendarMonth,
            requirements = listOf(
                "Connect a maintained Panchang/astrology source with location and timezone support.",
                "Store the source, calculation method and last-updated time with every calendar result.",
                "Automatically exclude past dates and clearly distinguish general dates from personalised calculations.",
                "Show this optional tool only as applicable to the member's chosen tradition/preferences.",
                "Avoid presenting calendar guidance as a guarantee or universal requirement."
            ),
            onHelp = onOpenHelp
        )
    }
}
