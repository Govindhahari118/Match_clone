package com.match.app.ui.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.components.MatreeHero
import com.match.app.ui.components.MatreeInfoCard
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeTopBar
import com.match.app.ui.i18n.t
import com.match.app.ui.theme.MatreeDesign

// ── Content definitions ─────────────────────────────────────────────────────

private data class LegalSection(val title: String, val bullets: List<String>)

private val TERMS = listOf(
    LegalSection("Eligibility", listOf(
        "You must be 18+ to create an account.",
        "You are responsible for the accuracy of your profile details."
    )),
    LegalSection("Acceptable Use", listOf(
        "No harassment, impersonation, or fraudulent activity.",
        "No solicitation or commercial spam.",
        "Respect privacy controls and consent boundaries."
    )),
    LegalSection("Service Disclaimer", listOf(
        "Matree is a facilitation tool and does not guarantee marriage outcomes.",
        "Users are responsible for their interactions and decisions."
    )),
    LegalSection("Account Termination", listOf(
        "We may suspend or terminate accounts that violate these terms or pose safety risks to the community."
    )),
    LegalSection("Contact", listOf(
        "Use the authenticated Help & Support flow for questions about these terms."
    ))
)

private val PRIVACY = listOf(
    LegalSection("What We Collect", listOf(
        "Account details: name, contact information, and login identifiers.",
        "Profile details: demographics, preferences, and profile photos you provide.",
        "Usage data: actions taken inside the product to improve stability and safety."
    )),
    LegalSection("How We Use Data", listOf(
        "To provide matchmaking, search, and communication features.",
        "To protect user safety and reduce misuse or fake profiles.",
        "To improve performance, reliability, and user experience."
    )),
    LegalSection("Privacy Controls", listOf(
        "Phone and contact visibility are hidden by default.",
        "You can control profile visibility, last seen, and search indexing.",
        "Privacy settings are available in Settings > Privacy."
    )),
    LegalSection("Data Retention", listOf(
        "We keep your data only as long as necessary to provide the service.",
        "You may request deletion at any time from Settings."
    )),
    LegalSection("Your Controls", listOf(
        "Access — Use the account-data export flow in Settings for a copy of supported account data.",
        "Correction — Edit supported profile information and preferences in the app.",
        "Deletion — Request permanent account deletion from Settings.",
        "Consent — Withdraw supported optional-processing consent from the relevant feature or privacy control.",
        "Privacy or grievance requests — Use the authenticated Help & Support flow so the request is linked to the correct account."
    )),
    LegalSection("Applicable Rights", listOf(
        "Specific legal rights and response obligations depend on the law that applies to you and the final production privacy policy.",
        "Matree does not invent jurisdiction-specific rights, retention periods, officer titles, or response deadlines in product copy."
    )),
    LegalSection("Contact", listOf(
        "Use Help & Support in the app for privacy questions or account-specific requests."
    ))
)

private val GUIDELINES = listOf(
    LegalSection("Be Honest", listOf(
        "Use your real identity and accurate profile details.",
        "Do not misrepresent age, marital status, or profession."
    )),
    LegalSection("Be Respectful", listOf(
        "No harassment, threats, or abusive language.",
        "Respect family involvement and communication preferences."
    )),
    LegalSection("Protect Privacy", listOf(
        "Do not request or share private information too early.",
        "Follow visibility settings and consent before sharing details."
    )),
    LegalSection("Report Concerns", listOf(
        "Use the Report option on any profile or message to flag abuse.",
        "Reports are queued for review by our safety team."
    ))
)

private val SECURITY = listOf(
    LegalSection("Security Basics", listOf(
        "Release builds disable cleartext network traffic; production services must use configured secure transport.",
        "Strong input validation and safe defaults protect user data.",
        "Security headers reduce common browser attacks."
    )),
    LegalSection("Responsible Disclosure", listOf(
        "If you find a vulnerability, use the authenticated Help & Support flow and choose Technical issue or Safety.",
        "Do not publicly disclose issues before we investigate and fix them."
    ))
)

private val REFUNDS = listOf(
    LegalSection("Purchase channel", listOf(
        "Refund and cancellation eligibility depends on the purchase channel, product state, and the policy that applies to that purchase.",
        "For Google Play purchases, the current Google Play purchase/subscription and refund process remains authoritative."
    )),
    LegalSection("Support", listOf(
        "Use the in-app support flow with the relevant order or purchase reference when you need billing help.",
        "Matree does not promise a universal refund window unless that exact policy is displayed for the product and purchase channel."
    )),
    LegalSection("Entitlement changes", listOf(
        "Verified refunds, cancellations, expiries and chargebacks update server-owned entitlement state and are reconciled across devices."
    ))
)

// ── Resolvers ───────────────────────────────────────────────────────────────

private fun titleFor(type: String) = when (type) {
    "terms" -> "Terms of Service"
    "privacy" -> "Privacy Policy"
    "guidelines" -> "Community Guidelines"
    "security" -> "Security Practices"
    "refunds" -> "Refund Policy"
    else -> "Legal"
}

private fun subtitleFor(type: String) = when (type) {
    "terms" -> "Matree is a matchmaking platform. By using the service, you agree to the terms below."
    "privacy" -> "Matree is built around trust. This policy explains what data we collect, why we collect it, and how you control your privacy."
    "guidelines" -> "Matree is built for serious, respectful matchmaking. These guidelines protect everyone on the platform."
    "security" -> "We take security seriously. This page outlines basic practices and how to report security issues responsibly."
    "refunds" -> "Refund and cancellation terms depend on the real purchase channel and the policy shown for that product."
    else -> ""
}

private fun sectionsFor(type: String) = when (type) {
    "terms" -> TERMS
    "privacy" -> PRIVACY
    "guidelines" -> GUIDELINES
    "security" -> SECURITY
    "refunds" -> REFUNDS
    else -> emptyList()
}

// ── Composable ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(type: String, onBack: () -> Unit = {}) {
    val title = titleFor(type)
    val subtitle = subtitleFor(type)
    val sections = sectionsFor(type)

    Scaffold(
        topBar = {
            MatreeTopBar(title = title, onBack = onBack)
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(MatreeDesign.spacing.lg).testTag("legal_screen_$type"),
            verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.lg)
        ) {
            MatreeHero(
                title = title,
                subtitle = subtitle
            ) {
                Text(
                    t("effective_date", "Release-candidate copy · final legal approval required"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sections
            sections.forEach { section ->
                MatreeInfoCard {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(MatreeDesign.spacing.sm))
                    section.bullets.forEach { bullet ->
                        Row(Modifier.padding(vertical = MatreeDesign.spacing.xxs)) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(MatreeDesign.spacing.md)
                            )
                            Text(
                                bullet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Contact footer
            MatreeInlineNotice(
                message = "Release-candidate legal copy · final published policy approval is required before launch."
            )
            Spacer(Modifier.height(MatreeDesign.spacing.md))
        }
    }
}
