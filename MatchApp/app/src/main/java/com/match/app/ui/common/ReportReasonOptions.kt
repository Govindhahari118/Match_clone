package com.match.app.ui.common

import androidx.compose.runtime.Composable
import com.match.app.ui.i18n.t

data class ReportReasonOption(
    val code: String,
    val label: String
)

@Composable
fun reportReasonOptions(): List<ReportReasonOption> = listOf(
    ReportReasonOption("FAKE_PROFILE", t("fake_profile", "Fake profile")),
    ReportReasonOption("SCAM_OR_MONEY_REQUEST", t("spam_or_scam", "Spam or scam")),
    ReportReasonOption("HARASSMENT", t("harassment", "Harassment")),
    ReportReasonOption("INAPPROPRIATE_CONTENT", t("inappropriate_content", "Inappropriate content")),
    ReportReasonOption("UNDERAGE_CONCERN", t("under_age", "Under age")),
    ReportReasonOption("OTHER", t("other", "Other"))
)
