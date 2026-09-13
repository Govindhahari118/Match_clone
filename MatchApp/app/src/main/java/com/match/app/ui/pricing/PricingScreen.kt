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
import com.match.app.BuildConfig
import com.match.app.data.local.dao.UserDao
import com.match.app.data.repo.SubscriptionRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.subscription.SubscriptionPlans
import com.match.app.ui.i18n.t
import com.razorpay.Checkout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

private data class Plan(
    val id: String,
    val name: String,
    val tagline: String,
    val price: String,
    val period: String,
    val contacts: String,
    val features: List<String>,
    val cta: String,
    val highlighted: Boolean,
    val icon: ImageVector,
    val amountPaise: Int
)

private val PLANS = listOf(
    Plan("FREE", "Free", "Start your journey", "₹0", "ALWAYS FREE", "No contact reveal", listOf(
        "Create & complete profile", "Browse daily matches", "Send up to 5 interests/day", "Shortlist profiles", "Receive interests"
    ), "Get started free", false, Icons.Filled.Person, 0),
    Plan("SILVER_3M", "Silver", "Serious matchmaking", "₹2,999", "3 MONTHS", "75 contacts", listOf(
        "Everything in Free", "View 75 contact details", "Advanced search filters", "Unlimited messaging", "See who viewed you", "Kundli compatibility report"
    ), "Upgrade to Silver", false, Icons.Filled.WorkspacePremium, SubscriptionPlans.Plan.SILVER_3M.amountPaise),
    Plan("GOLD_6M", "Gold", "Most chosen plan", "₹4,999", "6 MONTHS", "150 contacts", listOf(
        "Everything in Silver", "View 150 contact details", "Profile boost", "Family profile sharing", "Priority support", "Export biodata as PDF"
    ), "Upgrade to Gold", true, Icons.Filled.Star, SubscriptionPlans.Plan.GOLD_6M.amountPaise),
    Plan("PLATINUM_12M", "Platinum", "Maximum access", "₹7,499", "12 MONTHS", "300 contacts", listOf(
        "Everything in Gold", "View 300 contact details", "Priority placement", "Astrology deep-dive", "Relationship manager eligibility"
    ), "Go Platinum", false, Icons.Filled.Diamond, SubscriptionPlans.Plan.PLATINUM_12M.amountPaise)
)

@HiltViewModel
class PricingViewModel @Inject constructor(
    private val session: SessionStore,
    private val userDao: UserDao,
    val subscriptionRepo: SubscriptionRepository
) : ViewModel() {
    data class UserPrefill(val email: String, val phone: String)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val prefill: StateFlow<UserPrefill> = session.userId.filterNotNull()
        .flatMapLatest { uid -> userDao.observeById(uid).map { u -> UserPrefill(u?.email ?: "", u?.phoneNumber ?: "") } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPrefill("", ""))

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    suspend fun createOrder(planId: String): Result<SubscriptionRepository.CheckoutOrder> {
        _loading.value = true
        return try { subscriptionRepo.createRazorpayOrder(planId) } finally { _loading.value = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(onBack: () -> Unit = {}, vm: PricingViewModel = hiltViewModel()) {
    val activity = LocalContext.current as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingPlan by remember { mutableStateOf<Plan?>(null) }
    val prefill by vm.prefill.collectAsState()
    val loading by vm.loading.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { if (activity != null) Checkout.preload(activity) }

    fun launchPayment(plan: Plan) {
        if (plan.amountPaise == 0 || activity == null) return
        scope.launch {
            val order = vm.createOrder(plan.id).getOrElse {
                snackbarHostState.showSnackbar("Unable to initialize payment. Please try again.")
                return@launch
            }
            // The server is authoritative, but never charge an amount different from what the
            // current UI disclosed. A mismatch means the app catalogue needs refreshing.
            if (order.planId != plan.id || order.amount != plan.amountPaise || order.currency != "INR") {
                snackbarHostState.showSnackbar("Membership pricing changed. Please update the app before paying.")
                return@launch
            }
            try {
                Checkout().apply { setKeyID(BuildConfig.RAZORPAY_KEY_ID) }.open(activity, JSONObject().apply {
                    put("name", "MatrimonyConnect")
                    put("description", "${plan.name} Membership")
                    put("order_id", order.id)
                    put("amount", order.amount)
                    put("currency", order.currency)
                    put("prefill", JSONObject().apply {
                        put("contact", prefill.phone)
                        put("email", prefill.email)
                    })
                    put("theme", JSONObject().apply { put("color", "#E91E63") })
                })
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Unable to open secure checkout. Please try again.")
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text(t("membership_plans", "Membership Plans")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        ) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(t("choose_right_plan", "Choose the right plan"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                PLANS.forEach { plan ->
                    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(plan.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Text(plan.price, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(plan.period, style = MaterialTheme.typography.labelMedium)
                            Text(plan.contacts, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            plan.features.forEach { f -> Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = Color(0xFF2E7D32)); Spacer(Modifier.width(8.dp)); Text(f, style = MaterialTheme.typography.bodySmall)
                            } }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { if (plan.amountPaise > 0) pendingPlan = plan }, enabled = plan.amountPaise > 0, modifier = Modifier.fillMaxWidth()) { Text(plan.cta) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            if (loading) Surface(color = Color.Black.copy(alpha = 0.3f), modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }

    pendingPlan?.let { plan -> AlertDialog(
        onDismissRequest = { pendingPlan = null },
        title = { Text("Confirm Upgrade") },
        text = { Text("Upgrade to ${plan.name} for ${plan.price}?") },
        confirmButton = { Button(onClick = { pendingPlan = null; launchPayment(plan) }) { Text("Proceed") } },
        dismissButton = { TextButton(onClick = { pendingPlan = null }) { Text("Cancel") } }
    ) }
}
