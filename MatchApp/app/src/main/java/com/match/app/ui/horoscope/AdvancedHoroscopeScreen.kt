package com.match.app.ui.horoscope

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.match.app.ui.components.MatreeScreen
import com.match.app.ui.components.MatreeUnavailableFeature

/** Advanced astrology is optional and must be calculated from real birth data/provider output. */
@Composable
fun AdvancedHoroscopeScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    MatreeScreen(title = "Advanced Horoscope", onBack = onBack) { padding ->
        MatreeUnavailableFeature(
            modifier = Modifier.padding(padding),
            title = "Advanced horoscope calculation is not active yet",
            description = "This optional compatibility tool is relevant only for members and traditions that choose astrology. The old prototype used fixed Rasi, Nakshatra and compatibility results; those fabricated personal readings are removed.",
            icon = Icons.Filled.AutoAwesome,
            requirements = listOf(
                "Use the member's explicitly provided birth date, time and place only when astrology is applicable and enabled.",
                "Integrate a documented calculation engine or provider rather than hard-coded planetary/compatibility output.",
                "Show source/method and calculation timestamp so results are auditable.",
                "Keep astrology optional and separate from universal compatibility dimensions.",
                "Do not infer or fabricate missing birth details."
            ),
            onHelp = onOpenHelp
        )
    }
}
