package com.match.app.ui.consent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Legacy entry-point retained for binary/source compatibility.
 *
 * This dialog deliberately avoids hard-coded legal, residency, retention or response-time claims.
 * Current purpose-specific consent is recorded by ConsentRepository against backend-owned notice
 * versions and can be reviewed/withdrawn from the Privacy Dashboard.
 */
@Composable
fun DpdpConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(onClick = onAccept, shape = RoundedCornerShape(8.dp)) {
                Text("Review choices & continue")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDecline, shape = RoundedCornerShape(8.dp)) {
                Text("Not now")
            }
        },
        title = { Text("Privacy choices", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Matree uses purpose-specific privacy choices instead of one blanket approval. " +
                        "Sensitive features such as identity verification, location, media processing " +
                        "and personalization ask for the current notice before processing starts."
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "You can review or withdraw optional choices from Privacy & visibility. " +
                        "Withdrawing a choice stops the corresponding optional processing where the " +
                        "feature supports it; for example, withdrawing location consent removes the " +
                        "stored Nearby location."
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "The app's Privacy Policy and in-product notices are the source for current " +
                        "operator, retention and legal disclosures."
                )
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
