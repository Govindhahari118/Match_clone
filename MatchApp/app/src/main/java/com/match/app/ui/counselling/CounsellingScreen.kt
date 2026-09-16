package com.match.app.ui.counselling

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.match.app.ui.components.MatreeScreen
import com.match.app.ui.components.MatreeUnavailableFeature

@Composable
fun CounsellingScreen(
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {}
) {
    MatreeScreen(title = "Counselling", onBack = onBack) { padding ->
        MatreeUnavailableFeature(
            modifier = Modifier.padding(padding),
            title = "Counselling booking is not active yet",
            description = "The old prototype displayed fictional counsellors, slots and bookings. A production counselling surface must list real professionals and availability from an accountable service source.",
            icon = Icons.Filled.Psychology,
            requirements = listOf(
                "Onboard real counselling professionals with verified credentials and service scope.",
                "Load live availability and appointment state from a trusted scheduling backend.",
                "Add informed-consent, privacy and cancellation terms appropriate to counselling services.",
                "Connect payment, refunds and appointment reminders to real records.",
                "Avoid presenting counselling as emergency or clinical care unless the service is actually licensed for that purpose."
            ),
            onHelp = onOpenHelp
        )
    }
}
