package com.match.app.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.core.telemetry.MatreeTelemetry
import com.match.app.data.repo.SupportRepository
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class Faq(val question: String, val answer: String)

private val FAQS = listOf(
    Faq(
        "What can I do on the free plan?",
        "You can create and maintain your profile, browse matches, shortlist profiles, receive interests, and send up to the free daily interest limit. Paid capabilities are shown on the Membership screen."
    ),
    Faq(
        "Who can message me?",
        "Messaging is available after both members have expressed interest and formed a mutual match. Blocking either member prevents new messages."
    ),
    Faq(
        "Who can see my profile photos?",
        "Uploaded profile photos are part of your member profile and can be shown to signed-in members who can access your profile. Delete a photo from Profile if you no longer want it shown."
    ),
    Faq(
        "Are my phone number and email public?",
        "No. Contact data is kept outside the public profile document. Phone reveal is a server-controlled paid feature and requires a mutual match plus an available contact allowance."
    ),
    Faq(
        "How does verification work?",
        "You can submit the supported identity-verification information from the Verification screen. A verified badge is only granted by the server-side verification workflow; the app cannot self-assign verification."
    ),
    Faq(
        "How are memberships purchased?",
        "In the Google Play version, paid memberships are purchased through Google Play. The app shows the localized price returned by Google Play and activates access only after the server verifies the completed purchase."
    ),
    Faq(
        "What happens if a Google Play payment is pending?",
        "No paid access is granted while a payment is pending. When Google Play marks it purchased, reopening the app automatically retries secure server verification. Do not pay a second time for the same pending purchase."
    ),
    Faq(
        "How do refunds work?",
        "Refund eligibility depends on the published refund policy and the Google Play purchase state. Use the Refund Policy in the app and your Google Play order/support options for a purchase-related request."
    ),
    Faq(
        "How do I report a suspicious profile?",
        "Open the member profile, choose Report, select the most accurate reason, and submit it. Reports are stored for moderation; blocking is available separately if you need to stop interaction immediately."
    ),
    Faq(
        "How do I delete my account?",
        "Open Settings and choose Delete account. The authenticated server cleanup removes the account and associated application data before the local session is cleared."
    )
)

private val SUPPORT_CATEGORIES = listOf(
    "Account", "Membership", "Verification", "Safety", "Technical issue", "Other"
)

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val supportRepository: SupportRepository,
    private val telemetry: MatreeTelemetry
) : ViewModel() {
    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun submit(category: String, text: String) = viewModelScope.launch {
        if (_submitting.value) return@launch
        _submitting.value = true
        _message.value = null
        supportRepository.submitSupportTicket(category, text)
            .onSuccess { ticketId ->
                telemetry.supportSubmitted(category)
                _message.value = "Support request submitted. Ticket: ${ticketId.take(12)}"
            }
            .onFailure { error ->
                telemetry.recordFailure(MatreeTelemetry.Operation.FUNCTIONS, error)
                _message.value = "Could not submit the support request. Check your connection and try again."
            }
        _submitting.value = false
    }

    fun consumeMessage() { _message.value = null }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HelpScreen(onBack: () -> Unit = {}, vm: HelpViewModel = hiltViewModel()) {
    val submitting by vm.submitting.collectAsState()
    val resultMessage by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var category by remember { mutableStateOf("Technical issue") }
    var supportText by remember { mutableStateOf("") }

    LaunchedEffect(resultMessage) {
        resultMessage?.let {
            snackbar.showSnackbar(it)
            if (it.startsWith("Support request submitted")) supportText = ""
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(t("help_support", "Help & Support")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("help_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(20.dp).testTag("help_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Help that matches the live app", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "These answers describe features currently enforced by the app and backend. For an account-specific issue, submit a support ticket below.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text("Frequently asked questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FAQS.forEachIndexed { index, faq -> FaqItem(faq, index) }

            HorizontalDivider()
            Text("Contact support", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Choose a category and describe what happened. The request is sent through the authenticated backend and linked to your signed-in account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SUPPORT_CATEGORIES.forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item) }
                    )
                }
            }

            OutlinedTextField(
                value = supportText,
                onValueChange = { supportText = it.take(2000) },
                modifier = Modifier.fillMaxWidth().testTag("support_message"),
                label = { Text("Describe the issue") },
                supportingText = { Text("${supportText.length}/2000 · minimum 10 characters") },
                minLines = 4,
                maxLines = 8,
                enabled = !submitting
            )

            Button(
                onClick = { vm.submit(category, supportText.trim()) },
                enabled = supportText.trim().length >= 10 && !submitting,
                modifier = Modifier.fillMaxWidth().testTag("submit_support_ticket")
            ) {
                if (submitting) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Filled.SupportAgent, null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (submitting) "Submitting…" else "Submit support request")
            }

            Text(
                "For immediate personal safety or an emergency, contact the appropriate local emergency service. In-app reporting is for platform moderation and is not an emergency service.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                Text(faq.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(faq.answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
