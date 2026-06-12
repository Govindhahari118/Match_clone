package com.match.app.ui.consent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * DPDP Act 2023 consent dialog.
 * Shown once on first app launch. User must accept before proceeding.
 *
 * Covers:
 *  - Purpose of data collection (matchmaking only)
 *  - Types of data collected
 *  - User rights (access, correction, erasure)
 *  - Grievance officer contact
 *  - Cross-border transfer disclosure (Firebase US servers)
 */
@Composable
fun DpdpConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss */ },
        confirmButton = {
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("I Agree & Continue")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDecline,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Decline")
            }
        },
        title = {
            Text(
                "Data Protection Consent",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "As per the Digital Personal Data Protection Act, 2023 (DPDP Act), " +
                    "we need your consent to collect and process your personal data.",
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Purpose of Data Collection")
                BulletPoint("To match you with compatible life partners")
                BulletPoint("To display your profile to other registered users")
                BulletPoint("To verify your identity for safety")
                BulletPoint("To send relevant match notifications")

                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Data We Collect")
                BulletPoint("Name, age, gender, contact details")
                BulletPoint("Photos, education, occupation, family details")
                BulletPoint("Religion, caste, horoscope information")
                BulletPoint("Location (city-level only)")
                BulletPoint("Device information for security")

                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Your Rights")
                BulletPoint("Right to access your data (Settings → Export Data)")
                BulletPoint("Right to correct your data (Edit Profile anytime)")
                BulletPoint("Right to delete your account (Settings → Delete Account)")
                BulletPoint("Right to withdraw consent (stop using the app)")

                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Data Storage")
                BulletPoint("Data stored on Google Firebase servers (US/Mumbai)")
                BulletPoint("All data encrypted in transit and at rest")
                BulletPoint("Cross-border transfer: India ↔ US (Firebase infrastructure)")

                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Grievance Officer")
                Text(
                    "For any data-related concerns, contact:\n" +
                    "Email: grievance@matrimonyconnect.app\n" +
                    "Response within 72 hours as per IT Rules 2021",
                    fontSize = 13.sp
                )
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)) {
        Text("• ", fontSize = 13.sp)
        Text(text, fontSize = 13.sp)
    }
}
