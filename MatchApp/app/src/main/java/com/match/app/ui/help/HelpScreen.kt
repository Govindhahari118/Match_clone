package com.match.app.ui.help

import com.match.app.ui.i18n.t
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Faq(val question: String, val answer: String, val category: String)
private data class HelpSection(val icon: ImageVector, val title: String, val description: String)

private val FAQS = listOf(
    // Getting Started
    Faq("Is it free to create a profile?",
        "Yes! Creating a profile, browsing matches, sending interests, and shortlisting profiles are all free. Premium plans unlock priority listing, advanced filters, and more.",
        "Getting Started"),
    Faq("How do I register on Match?",
        "Tap 'Create account' and enter your phone number for OTP verification. Then fill in basic details — name, age, gender, city, and a short bio. You can complete the rest later.",
        "Getting Started"),
    Faq("How do I set my partner preferences?",
        "Go to Settings → Match Filters. You can set age range, religion, caste, mother tongue, city, education, diet, and more. Changes reflect immediately in your recommendations.",
        "Getting Started"),

    // Profile & Photos
    Faq("How are profiles verified?",
        "Every profile is phone-verified using OTP. Identity verification (Aadhaar, PAN, Passport, or Voter ID) is optional but adds a ✓ badge that increases trust and response rates by 3×.",
        "Profile & Photos"),
    Faq("Can I upload multiple photos?",
        "Yes. Go to your Profile → Photos section. You can upload up to 10 photos and set any as your primary. Photos are never shared without your consent.",
        "Profile & Photos"),
    Faq("How do I improve my profile visibility?",
        "Complete your profile 100%, add photos, get verified, and upgrade to a paid plan. Premium members get priority listing and up to 3× more profile views.",
        "Profile & Photos"),

    // Search & Matching
    Faq("How do I find matches from my community?",
        "Use the Regions page to pick community, city, or language presets. Or go to Settings → Match Filters to set religion, mother tongue, caste, and other preferences.",
        "Search & Matching"),
    Faq("What makes this different from other matrimony sites?",
        "We combine Astrology + Questionnaire + AI matching for richer compatibility insights. Privacy-first, no brokers, no hidden fees. 200+ community support.",
        "Search & Matching"),
    Faq("How does the Kundli compatibility work?",
        "We use your Rasi and Nakshatra to compute a Guna Milan-inspired score, including checks for Gana Dosh, Nadi Dosh, and Mangal Dosh. View detailed reports in the Kundli section.",
        "Search & Matching"),

    // Family & Privacy
    Faq("Can my family help manage my profile?",
        "Yes. Under Family settings, you can add family members as co-managers. They can browse matches, shortlist profiles, and communicate on your behalf with your permission.",
        "Family & Privacy"),
    Faq("How do you protect my privacy?",
        "Your photos and contact details are hidden from everyone by default. You control who sees what. We never sell your data or share it with third parties. All data is encrypted.",
        "Family & Privacy"),
    Faq("Can I hide my profile temporarily?",
        "Yes. Go to Settings → Privacy → Hide Profile. Your profile becomes invisible to others but your data is preserved. You can unhide anytime.",
        "Family & Privacy"),

    // Membership & Payments
    Faq("What do premium plans include?",
        "Premium plans unlock: contact viewing (75-300 contacts), unlimited messaging, priority listing, profile spotlight, kundli analysis, biodata export, and dedicated support. Plans start at ₹2,999 for 3 months.",
        "Membership & Payments"),
    Faq("Is there a refund policy?",
        "Yes. All plans include a 7-day refund policy if you're not satisfied. Contact support within 7 days of purchase for a full refund.",
        "Membership & Payments"),
    Faq("What payment methods are accepted?",
        "We accept UPI, credit/debit cards (Visa, Mastercard, Amex), net banking, and wallet payments. All transactions are secured with bank-grade encryption.",
        "Membership & Payments"),

    // Safety & Support
    Faq("How do I report a fake or abusive profile?",
        "On any profile, tap the ⋮ menu → Report. Select the reason (fake, abusive, spam, inappropriate). Our moderation team reviews within 24 hours and takes action.",
        "Safety & Support"),
    Faq("How do I delete my account?",
        "Go to Settings → Account → Delete Account. All your data is permanently removed within 30 days per our privacy policy. This action cannot be undone.",
        "Safety & Support"),
    Faq("How do I contact customer support?",
        "Go to Help → Contact Support. You can reach us via in-app chat (24/7), email at support@matchapp.in, or call our helpline. Premium members get priority support.",
        "Safety & Support"),
)

private val HELP_SECTIONS = listOf(
    HelpSection(Icons.AutoMirrored.Filled.MenuBook,       "Help Center",        "Guides, FAQs, and tips to find your perfect match faster."),
    HelpSection(Icons.Filled.Security,       "Safety Guidelines",  "Stay safe with verified profiles, reporting tools, and privacy controls."),
    HelpSection(Icons.Filled.Feedback,       "Share Feedback",     "We're always improving. Let us know how we can serve you better."),
    HelpSection(Icons.Filled.ReportProblem,  "Report an Issue",    "Something wrong? Our team responds within 24 hours."),
    HelpSection(Icons.AutoMirrored.Filled.ContactSupport, "Contact Support",    "Speak directly with our support team for urgent matters."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("help_support", "Help & Support")) },
                navigationIcon = { IconButton(onClick = onBack, modifier = Modifier.testTag("help_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                .testTag("help_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Hero ────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(t("here_to_help", "We're here to help you succeed"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Find answers, get support, and report issues — all in one place.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // ── Quick links ─────────────────────────────────────────────
            Text(t("quick_links", "Quick Links"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            HELP_SECTIONS.forEach { s ->
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(s.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(s.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider()

            // ── FAQ ─────────────────────────────────────────────────────
            Text(t("faq", "Frequently asked questions"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            // Category filter chips
            val categories = FAQS.map { it.category }.distinct()
            var selectedCategory by remember { mutableStateOf<String?>(null) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                )
                categories.take(4).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.split(" ").first(), style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            val filteredFaqs = if (selectedCategory == null) FAQS else FAQS.filter { it.category == selectedCategory }
            filteredFaqs.forEachIndexed { i, faq ->
                FaqItem(faq, i)
            }

            Spacer(Modifier.height(8.dp))

            // ── Contact support card ─────────────────────────────────────
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SupportAgent, null, Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(t("still_need_help", "Still need help?"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(t("support_24_7", "Our support team is available 24/7"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Email, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t("email", "Email"))
                        }
                        Button(onClick = {}, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )) {
                            Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t("live_chat", "Live Chat"))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FaqItem(faq: Faq, index: Int) {
    var expanded by remember { mutableStateOf(false) }
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.testTag("faq_$index")
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(faq.question, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(faq.answer, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
