package com.match.app.ui.assisted

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.ui.i18n.t

private val MAROON = Color(0xFF8B1A1A)
private val GOLD   = Color(0xFFB8860B)

private data class RmPlan(
    val name: String, val price: String, val duration: String,
    val features: List<String>, val highlighted: Boolean = false
)

private val RM_PLANS = listOf(
    RmPlan(
        "Silver RM", "₹14,999", "3 Months",
        listOf("Dedicated Relationship Manager", "30 curated matches", "Profile optimisation",
               "Weekly progress calls", "Priority shortlisting")
    ),
    RmPlan(
        "Gold RM", "₹24,999", "6 Months",
        listOf("Senior Relationship Manager", "75 curated matches", "Full profile makeover",
               "Bi-weekly calls + WhatsApp", "Meet & Greet co-ordination", "Background verification"),
        highlighted = true
    ),
    RmPlan(
        "Platinum RM", "₹44,999", "12 Months",
        listOf("Elite Matchmaking Expert", "Unlimited curated matches", "Professional photo shoot",
               "Dedicated 24×7 support", "Invitation + venue shortlisting", "Legal & documentation help",
               "Counsellor pre-marriage sessions")
    )
)

private data class HowStep(val step: Int, val icon: ImageVector, val title: String, val desc: String)
private val HOW_STEPS = listOf(
    HowStep(1, Icons.Filled.Person, "Share Your Preferences",
        "Tell your RM exactly what you're looking for — family background, education, personality, and more."),
    HowStep(2, Icons.Filled.Search, "Expert Curation",
        "Your RM hand-picks and screens profiles that best fit your preferences from our verified database."),
    HowStep(3, Icons.Filled.Forum, "Facilitated Introductions",
        "Your RM sets up introductions, mediates conversations, and helps navigate family discussions."),
    HowStep(4, Icons.Filled.Favorite, "Walk Down the Aisle",
        "With professional guidance every step of the way — from first meeting to final confirmation.")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistedServiceScreen(
    onBack: () -> Unit = {},
    vm: AssistedViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("assisted_matchmaking", "Assisted Matchmaking")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("assisted_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        if (ui.submitted) {
            ConfirmationView(
                name = ui.leadName,
                plan = ui.selectedPlan,
                onBack = { vm.resetSubmission(); onBack() }
            )
        } else {
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                    .testTag("assisted_screen"),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Hero ───────────────────────────────────────────────
                Surface(Modifier.fillMaxWidth(), color = MAROON) {
                    Column(
                        Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SupportAgent, null, Modifier.size(48.dp), tint = Color.White)
                        Text("Find Your Perfect Match", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                        Text("Let our expert Relationship Managers do the searching, screening, and facilitating — so you can focus on building a connection.",
                            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center)
                        Surface(shape = RoundedCornerShape(20.dp), color = GOLD) {
                            Text("  10× Faster than Self-Search  ",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
                        }
                    }
                }

                // ── Stats bar ──────────────────────────────────────────
                Surface(Modifier.fillMaxWidth(), color = MAROON.copy(alpha = 0.07f)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        RmStat("8,500+", "Couples Matched")
                        RmStat("98%", "Satisfaction Rate")
                        RmStat("47 days", "Avg Match Time")
                    }
                }

                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {

                    // ── How it works ──────────────────────────────────
                    Text("How Assisted Matchmaking Works",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    HOW_STEPS.forEach { step ->
                        HowItWorksStep(step)
                    }

                    // ── Plans ─────────────────────────────────────────
                    Text("Choose Your Plan",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    RM_PLANS.forEach { plan ->
                        RmPlanCard(plan, ui.selectedPlan == plan.name) { vm.onPlanSelect(plan.name) }
                    }

                    // ── Lead form ─────────────────────────────────────
                    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Request a Call Back",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Our senior RM will call you within 24 hours to understand your requirements.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(value = ui.leadName, onValueChange = vm::onNameChange,
                                label = { Text(t("full_name", "Full Name")) }, modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Filled.Person, null) })
                            OutlinedTextField(value = ui.leadPhone, onValueChange = vm::onPhoneChange,
                                label = { Text(t("mobile_number", "Mobile Number")) }, modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Text("+91  ", style = MaterialTheme.typography.bodyMedium) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                            OutlinedTextField(value = ui.leadPreference, onValueChange = vm::onPreferenceChange,
                                label = { Text("What are you looking for? (Optional)") },
                                modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                            
                            if (ui.error != null) {
                                Text(ui.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = vm::submitRequest,
                                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("assisted_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = MAROON),
                                enabled = ui.leadName.isNotBlank() && ui.leadPhone.length == 10 && !ui.loading
                            ) {
                                if (ui.loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                else {
                                    Icon(Icons.Filled.Phone, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(t("request_callback_free", "Request Call Back — Free"))
                                }
                            }
                        }
                    }

                    // ── Testimonials ──────────────────────────────────
                    Text("What Our Clients Say",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    listOf(
                        Triple("Priya & Arjun", "Gold RM plan",
                            "\"Our RM Sunita aunty was amazing. She found Arjun in just 6 weeks. Best investment of my life!\""),
                        Triple("Vikram & Kavitha", "Platinum RM plan",
                            "\"The Platinum plan was worth every rupee. Our RM handled everything from photos to family meetings.\"")
                    ).forEach { (names, plan, quote) ->
                        RmTestimonial(names, plan, quote)
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RmStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MAROON)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HowItWorksStep(step: HowStep) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = MAROON, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text("${step.step}", style = MaterialTheme.typography.labelLarge,
                    color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(step.icon, null, Modifier.size(18.dp), tint = MAROON)
                Spacer(Modifier.width(6.dp))
                Text(step.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(step.desc, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RmPlanCard(plan: RmPlan, selected: Boolean, onSelect: () -> Unit) {
    ElevatedCard(
        onClick = onSelect,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("rm_plan_${plan.name}"),
        colors = if (plan.highlighted || selected)
            CardDefaults.elevatedCardColors(containerColor = MAROON.copy(alpha = 0.06f))
        else CardDefaults.elevatedCardColors(),
        elevation = if (selected) CardDefaults.elevatedCardElevation(8.dp)
                    else CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(plan.name, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = MAROON)
                        if (plan.highlighted) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = GOLD) {
                                Text("MOST POPULAR", style = MaterialTheme.typography.labelSmall,
                                    color = Color.White, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("${plan.price} / ${plan.duration}",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RadioButton(selected = selected, onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = MAROON))
            }
            HorizontalDivider()
            plan.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(8.dp))
                    Text(feature, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RmTestimonial(names: String, plan: String, quote: String) {
    Surface(shape = RoundedCornerShape(14.dp),
        color = MAROON.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, null, Modifier.size(16.dp), tint = MAROON)
                Spacer(Modifier.width(6.dp))
                Text(names, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(4.dp), color = GOLD.copy(alpha = 0.15f)) {
                    Text(plan, style = MaterialTheme.typography.labelSmall, color = GOLD,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(quote, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmationView(name: String, plan: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = Color(0xFF2E7D32).copy(alpha = 0.1f),
            modifier = Modifier.size(100.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.CheckCircle, null, Modifier.size(60.dp), tint = Color(0xFF2E7D32))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Request Submitted!", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Spacer(Modifier.height(12.dp))
        Text("Thank you, $name! Our senior RM from the $plan will call you within 24 hours.",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("In the meantime, keep your profile updated for the best matches.",
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MAROON)) {
            Text("Back to Home")
        }
    }
}
