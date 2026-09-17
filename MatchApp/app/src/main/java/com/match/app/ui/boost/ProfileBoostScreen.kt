package com.match.app.ui.boost

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.match.app.data.billing.PlayBillingManager
import com.match.app.data.repo.SubscriptionRepository
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class BoostPlanUi(
    val id: String,
    val name: String,
    val duration: String,
    val tag: String = ""
)

private val BOOST_PLANS = listOf(
    BoostPlanUi(PlayBillingManager.BOOST_3H_ID, "Quick Boost", "3 hours", "Short boost"),
    BoostPlanUi(PlayBillingManager.BOOST_24H_ID, "Power Boost", "24 hours", "Popular"),
    BoostPlanUi(PlayBillingManager.BOOST_7D_ID, "Week Boost", "7 days", "Longest")
)

@HiltViewModel
class ProfileBoostViewModel @Inject constructor(
    val billing: PlayBillingManager,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {
    val offers = billing.offers
    val ready = billing.ready

    var boostUntil by mutableLongStateOf(0L)
        private set
    var refreshing by mutableStateOf(false)
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    init {
        billing.connect()
        refresh()
        viewModelScope.launch {
            billing.events.collect { event ->
                when (event) {
                    is PlayBillingManager.Event.BoostActivated -> {
                        boostUntil = event.boostUntil
                        _messages.emit("Boost activated after secure Google Play verification.")
                    }
                    is PlayBillingManager.Event.Pending -> {
                        if (billing.isBoostProduct(event.productId)) {
                            _messages.emit(
                                "Payment is pending in Google Play. Boost starts only after payment completes."
                            )
                        }
                    }
                    PlayBillingManager.Event.Cancelled -> _messages.emit("Purchase cancelled.")
                    is PlayBillingManager.Event.Error -> _messages.emit(event.message)
                    is PlayBillingManager.Event.Activated -> Unit
                }
            }
        }
    }

    fun refresh() {
        billing.refresh()
        viewModelScope.launch {
            refreshing = true
            boostUntil = subscriptionRepository.getBoostExpiry()
            refreshing = false
        }
    }

    fun purchase(activity: Activity, boostId: String): Int {
        return billing.launchPurchase(activity, boostId).responseCode
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBoostScreen(
    onBack: () -> Unit = {},
    vm: ProfileBoostViewModel = hiltViewModel()
) {
    val activity = LocalContext.current as? Activity
    val offers by vm.offers.collectAsState()
    val ready by vm.ready.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var selectedPlan by remember { mutableIntStateOf(1) }
    var pendingPlan by remember { mutableStateOf<BoostPlanUi?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(vm.boostUntil) {
        while (vm.boostUntil > System.currentTimeMillis()) {
            now = System.currentTimeMillis()
            delay(60_000)
        }
        now = System.currentTimeMillis()
    }

    val remainingMs = (vm.boostUntil - now).coerceAtLeast(0L)
    val boostActive = remainingMs > 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("profile_boost", "Profile Boost")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("boost_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, "Refresh boost and Google Play prices")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("boost_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (boostActive) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                t("boost_active", "Boost Active"),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            val days = TimeUnit.MILLISECONDS.toDays(remainingMs)
                            val hours = TimeUnit.MILLISECONDS.toHours(remainingMs) % 24
                            val mins = TimeUnit.MILLISECONDS.toMinutes(remainingMs) % 60
                            val remaining = if (days > 0) {
                                "${days}d ${hours}h remaining"
                            } else {
                                "${hours}h ${mins}m remaining"
                            }
                            Text(
                                remaining,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Increase Your Discovery Visibility",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "An active boost gives your profile temporary priority among otherwise eligible discovery profiles. It does not change compatibility, privacy, verification, or matching rules.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                t("how_boost_works", "How Boost Works"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            listOf(
                Triple(
                    Icons.Filled.ArrowUpward,
                    "Temporary discovery priority",
                    "Boost is considered only after a profile passes normal discovery and privacy checks."
                ),
                Triple(
                    Icons.Filled.Security,
                    "No bypasses",
                    "Boost cannot bypass blocks, hidden-profile rules, age filters, or mutual partner preferences."
                ),
                Triple(
                    Icons.Filled.VerifiedUser,
                    "Verified purchase",
                    "Paid boost time starts only after the backend verifies the completed Google Play purchase."
                )
            ).forEach { (icon, title, description) ->
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            title,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                t("choose_boost_plan", "Choose Boost Duration"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Prices are loaded directly from Google Play for your account and region.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!ready || vm.refreshing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            BOOST_PLANS.forEachIndexed { index, plan ->
                val selected = selectedPlan == index
                val offer = offers[plan.id]
                ElevatedCard(
                    onClick = { selectedPlan = index },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("boost_plan_$index"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.elevatedCardElevation(if (selected) 6.dp else 2.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    plan.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(Modifier.width(8.dp))
                                AssistChip(onClick = {}, enabled = false, label = { Text(plan.tag) })
                            }
                            Text(
                                plan.duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            when {
                                offer != null -> offer.formattedPrice
                                ready -> "Unavailable"
                                else -> "Loading…"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = selected, onClick = { selectedPlan = index })
                    }
                }
            }

            val selected = BOOST_PLANS[selectedPlan]
            val selectedOffer = offers[selected.id]
            Button(
                onClick = { pendingPlan = selected },
                enabled = selectedOffer != null && activity != null,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("boost_activate"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Bolt, contentDescription = null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        selectedOffer != null && boostActive -> "Add ${selected.duration} — ${selectedOffer.formattedPrice}"
                        selectedOffer != null -> "Boost for ${selected.duration} — ${selectedOffer.formattedPrice}"
                        ready -> "Not available in Google Play"
                        else -> "Loading Google Play…"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "If Google Play reports a pending payment, no boost is granted until payment completes and the backend verifies it. Completed one-time purchases are recovered automatically after reconnecting.",
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
                    "Continue to Google Play to purchase ${plan.duration} of boost for ${offer?.formattedPrice ?: "the price shown by Google Play"}? Active boost time is extended rather than discarded."
                )
            },
            confirmButton = {
                Button(onClick = {
                    pendingPlan = null
                    val host = activity ?: return@Button
                    val code = vm.purchase(host, plan.id)
                    if (code != BillingClient.BillingResponseCode.OK) vm.refresh()
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
