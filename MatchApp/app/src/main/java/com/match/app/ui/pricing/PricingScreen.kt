package com.match.app.ui.pricing

import android.app.Activity
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.match.app.data.billing.PlayBillingManager
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private data class Plan(
    val id: String,
    val name: String,
    val tagline: String,
    val period: String,
    val contacts: String,
    val features: List<String>,
    val cta: String,
    val icon: ImageVector
)

private val PLANS = listOf(
    Plan("FREE", "Free", "Start your journey", "ALWAYS FREE", "No paid contact reveals", listOf(
        "Create and complete your profile",
        "Browse eligible profiles",
        "Send and receive interests",
        "Shortlist profiles"
    ), "Free plan", Icons.Filled.Person),
    Plan("SILVER_3M", "Silver", "Verified paid membership", "3 MONTHS", "75 contact reveals", listOf(
        "75 matched-member contact reveals during this entitlement",
        "3-month server-verified membership entitlement",
        "Purchase and restore handled through Google Play"
    ), "Choose Silver", Icons.Filled.WorkspacePremium),
    Plan("GOLD_6M", "Gold", "Verified paid membership", "6 MONTHS", "150 contact reveals", listOf(
        "150 matched-member contact reveals during this entitlement",
        "6-month server-verified membership entitlement",
        "Purchase and restore handled through Google Play"
    ), "Choose Gold", Icons.Filled.Star),
    Plan("PLATINUM_12M", "Platinum", "Verified paid membership", "12 MONTHS", "300 contact reveals", listOf(
        "300 matched-member contact reveals during this entitlement",
        "12-month server-verified membership entitlement",
        "Purchase and restore handled through Google Play"
    ), "Choose Platinum", Icons.Filled.Diamond)
)

@HiltViewModel
class PricingViewModel @Inject constructor(
    val billing: PlayBillingManager,
    session: SessionStore
) : ViewModel() {
    val offers = billing.offers
    val ready = billing.ready
    val events = billing.events
    val currentPlan: StateFlow<String> = session.subscriptionPlan
        .stateIn(viewModelScope, SharingStarted.Eagerly, "FREE")

    init {
        billing.connect()
    }

    fun refresh() = billing.refresh()

    fun purchase(activity: Activity, planId: String): Int {
        return billing.launchPurchase(activity, planId).responseCode
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(onBack: () -> Unit = {}, vm: PricingViewModel = hiltViewModel()) {
    val activity = LocalContext.current as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingPlan by remember { mutableStateOf<Plan?>(null) }
    val offers by vm.offers.collectAsState()
    val ready by vm.ready.collectAsState()
    val currentPlan by vm.currentPlan.collectAsState()

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is PlayBillingManager.Event.Activated -> snackbarHostState.showSnackbar(
                    "${event.planId.replace('_', ' ')} membership activated."
                )
                is PlayBillingManager.Event.BoostActivated -> Unit
                is PlayBillingManager.Event.Pending -> snackbarHostState.showSnackbar(
                    "Payment is pending in Google Play. Access starts only after payment completes."
                )
                PlayBillingManager.Event.Cancelled -> snackbarHostState.showSnackbar("Purchase cancelled.")
                is PlayBillingManager.Event.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("membership_plans", "Membership Plans")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, "Refresh Google Play prices")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                t("choose_right_plan", "Choose the right plan"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Paid memberships in this Play Store build are purchased through Google Play. Prices below come directly from Google Play for your account and region.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (!ready) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    "Connecting to Google Play…",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            PLANS.forEach { plan ->
                val offer = offers[plan.id]
                val isFree = plan.id == "FREE"
                val isCurrent = currentPlan == plan.id
                val price = when {
                    isFree -> "₹0"
                    offer != null -> offer.formattedPrice
                    ready -> "Unavailable"
                    else -> "Loading…"
                }

                ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(plan.icon, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    plan.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    plan.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isCurrent) {
                                AssistChip(onClick = {}, enabled = false, label = { Text("Current") })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            price,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(plan.period, style = MaterialTheme.typography.labelMedium)
                        Text(plan.contacts, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        plan.features.forEach { feature ->
                            Row(
                                Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    Modifier.size(16.dp),
                                    tint = Color(0xFF2E7D32)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(feature, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { pendingPlan = plan },
                            enabled = !isFree && !isCurrent && offer != null && activity != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when {
                                    isCurrent -> "Current membership"
                                    isFree -> "Free plan"
                                    offer == null && ready -> "Not available in Google Play"
                                    offer == null -> "Loading Google Play…"
                                    else -> plan.cta
                                }
                            )
                        }
                    }
                }
            }

            Text(
                "Google Play may support pending payment methods. No paid access is granted while a purchase is pending; the app securely verifies completed purchases with Google Play on the server before activating membership.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    pendingPlan?.let { plan ->
        val offer = offers[plan.id]
        AlertDialog(
            onDismissRequest = { pendingPlan = null },
            title = { Text("Confirm ${plan.name}") },
            text = {
                Text(
                    "Continue to Google Play to purchase ${plan.name} for ${offer?.formattedPrice ?: "the price shown by Google Play"}?"
                )
            },
            confirmButton = {
                Button(onClick = {
                    pendingPlan = null
                    val host = activity ?: return@Button
                    val code = vm.purchase(host, plan.id)
                    if (code != BillingClient.BillingResponseCode.OK) {
                        vm.refresh()
                    }
                }) {
                    Text("Continue to Google Play")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPlan = null }) { Text("Cancel") }
            }
        )
    }
}
