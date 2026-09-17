package com.match.app.ui.assisted

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.match.app.ui.i18n.t

private data class RmPlan(
    val name: String,
    val duration: String,
    val description: String
)

private val RM_PLANS = listOf(
    RmPlan(
        "Silver RM",
        "3 months",
        "Request a dedicated assisted-matchmaking engagement for a shorter search period."
    ),
    RmPlan(
        "Gold RM",
        "6 months",
        "Request a longer assisted-matchmaking engagement with relationship-manager support."
    ),
    RmPlan(
        "Platinum RM",
        "12 months",
        "Request the longest assisted-matchmaking engagement currently listed in the app."
    )
)

private data class HowStep(
    val step: Int,
    val title: String,
    val description: String
)

private val HOW_STEPS = listOf(
    HowStep(1, "Share your requirements", "Tell the team the partner preferences that matter to you."),
    HowStep(2, "Request a callback", "The request is stored on the backend for operations follow-up."),
    HowStep(3, "Confirm service details", "Availability, scope, pricing, and payment are confirmed before any paid service begins."),
    HowStep(4, "Start only after confirmation", "The app does not mark a relationship-manager service active from this form alone.")
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("assisted_screen"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.SupportAgent,
                        contentDescription = null,
                        modifier = Modifier.size(46.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Request Human Matchmaking Support",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Submit your interest in an assisted package. This form does not purchase a service, assign an RM, or promise a match timeline. An operations representative must confirm the actual service before activation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Text(
                "How the request works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HOW_STEPS.forEach { step ->
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                step.step.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(step.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            step.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (ui.submitted) {
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Request submitted",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Package interest: ${ui.selectedPlan}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "This confirms only that your callback/service request was recorded. It is not proof of payment, assignment, or an active paid engagement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Text(
                    "Choose the engagement you want to discuss",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                RM_PLANS.forEach { plan ->
                    val selected = ui.selectedPlan == plan.name
                    ElevatedCard(
                        onClick = { vm.onPlanSelect(plan.name) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    plan.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    plan.duration,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    plan.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = selected,
                                onClick = { vm.onPlanSelect(plan.name) }
                            )
                        }
                    }
                }

                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Request a callback",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Pricing and included services are intentionally not hard-coded here. They must be confirmed from the current service catalogue before you pay.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = ui.leadName,
                            onValueChange = vm::onNameChange,
                            label = { Text(t("full_name", "Full Name")) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ui.leadPhone,
                            onValueChange = vm::onPhoneChange,
                            label = { Text(t("mobile_number", "Mobile Number")) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Text("+91") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ui.leadPreference,
                            onValueChange = vm::onPreferenceChange,
                            label = { Text("What are you looking for? (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        if (ui.error != null) {
                            Text(
                                ui.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = vm::submitRequest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("assisted_submit"),
                            enabled = ui.leadName.trim().length >= 2 &&
                                ui.leadPhone.length == 10 &&
                                !ui.loading
                        ) {
                            if (ui.loading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Phone, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Submit Callback Request")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
