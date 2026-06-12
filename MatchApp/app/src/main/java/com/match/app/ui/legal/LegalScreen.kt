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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.match.app.ui.i18n.t

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
        "MatrimonyConnect is a facilitation tool and does not guarantee marriage outcomes.",
        "Users are responsible for their interactions and decisions."
    )),
    LegalSection("Account Termination", listOf(
        "We may suspend or terminate accounts that violate these terms or pose safety risks to the community."
    )),
    LegalSection("Contact", listOf(
        "Questions about these terms can be sent to support@matrimonyconnect.com."
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
    LegalSection("Your Rights (DPDP Act & GDPR)", listOf(
        "Right to Access — Request a copy of the personal data we hold about you.",
        "Right to Correction — Ask us to correct inaccurate or incomplete data.",
        "Right to Erasure — Request deletion of your personal data via Settings > Account > Delete Account.",
        "Right to Withdraw Consent — Withdraw consent for data processing at any time.",
        "Right to Grievance Redressal — File a complaint with our Data Protection Officer.",
        "Right to Nominate — Nominate another individual to exercise your rights (DPDP Act)."
    )),
    LegalSection("Legal Basis for Processing", listOf(
        "Consent — You provide explicit consent when creating an account.",
        "Contractual necessity — Processing required to deliver the matchmaking service.",
        "Legitimate interest — Safety measures, fraud prevention, and service improvement.",
        "Legal obligation — Compliance with applicable Indian and international laws."
    )),
    LegalSection("Data Protection Officer", listOf(
        "For queries related to your personal data, contact dpo@matrimonyconnect.com."
    )),
    LegalSection("Contact", listOf(
        "For privacy questions, email support@matrimonyconnect.com."
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
        "HTTPS is enforced across the platform.",
        "Strong input validation and safe defaults protect user data.",
        "Security headers reduce common browser attacks."
    )),
    LegalSection("Responsible Disclosure", listOf(
        "If you find a vulnerability, email security@matrimonyconnect.com.",
        "Do not publicly disclose issues before we investigate and fix them."
    ))
)

private val REFUNDS = listOf(
    LegalSection("Eligibility", listOf(
        "Refunds are available within 7 days of plan activation.",
        "Only the original payment method can receive refunds."
    )),
    LegalSection("How to Request", listOf(
        "Email support@matrimonyconnect.com with your order ID and the reason for cancellation."
    )),
    LegalSection("Exceptions", listOf(
        "Refunds may not be granted after 7 days, or if the plan was purchased through a partner marketplace."
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
    "terms" -> "MatrimonyConnect is a matchmaking platform. By using the service, you agree to the terms below."
    "privacy" -> "MatrimonyConnect is built around trust. This policy explains what data we collect, why we collect it, and how you control your privacy."
    "guidelines" -> "MatrimonyConnect is built for serious, respectful matchmaking. These guidelines protect everyone on the platform."
    "security" -> "We take security seriously. This page outlines basic practices and how to report security issues responsibly."
    "refunds" -> "All paid plans include a 7-day money-back guarantee. If you are not satisfied, contact support within 7 days."
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
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("legal_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(20.dp).testTag("legal_screen_$type"),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text(t("effective_date", "Effective March 10, 2026"), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }

            // Sections
            sections.forEach { section ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        section.bullets.forEach { bullet ->
                            Row(Modifier.padding(vertical = 3.dp)) {
                                Text("•", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(16.dp))
                                Text(bullet, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Contact footer
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "© 2026 MatrimonyConnect. All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
