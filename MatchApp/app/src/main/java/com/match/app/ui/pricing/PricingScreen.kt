package com.match.app.ui.pricing

import android.app.Activity
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.testTag
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
import com.match.app.ui.i18n.t
import com.razorpay.Checkout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

// ── Plan model ──────────────────────────────────────────────────────────────
private data class Plan(
    val name: String,
    val tagline: String,
    val price: String,
    val originalPrice: String?,
    val period: String,
    val duration: String,
    val contacts: String,
    val badge: String?,
    val badgeColor: Color,
    val features: List<String>,
    val lockedFeatures: List<String>,
    val cta: String,
    val highlighted: Boolean,
    val icon: ImageVector,
    val amountPaise: Int = 0
)

private val PLANS = listOf(
    Plan(
        name = "Free",
        tagline = "Start your journey",
        price = "₹0",
        originalPrice = null,
        period = "ALWAYS FREE",
        duration = "Unlimited",
        contacts = "5 per day",
        badge = null,
        badgeColor = Color.Transparent,
        features = listOf(
            "Create & complete profile",
            "Browse daily matches",
            "Send up to 5 interests/day",
            "Shortlist profiles",
            "Receive interests",
            "Basic questionnaire matching"
        ),
        lockedFeatures = listOf(
            "View contact details",
            "Unlimited messaging",
            "Kundli compatibility report"
        ),
        cta = "Get started free",
        highlighted = false,
        icon = Icons.Filled.Person,
        amountPaise = 0
    ),
    Plan(
        name = "Silver",
        tagline = "Serious matchmaking",
        price = "₹2,999",
        originalPrice = "₹4,999",
        period = "3 MONTHS",
        duration = "3 months",
        contacts = "75 contacts",
        badge = null,
        badgeColor = Color.Transparent,
        features = listOf(
            "Everything in Free",
            "View 75 contact details",
            "Priority profile listing",
            "Advanced search filters",
            "Unlimited messaging",
            "See who viewed you",
            "Hide profile from non-members",
            "Kundli compatibility report"
        ),
        lockedFeatures = listOf(
            "Profile spotlight",
            "Dedicated relationship manager"
        ),
        cta = "Upgrade to Silver",
        highlighted = false,
        icon = Icons.Filled.WorkspacePremium,
        amountPaise = 299900
    ),
    Plan(
        name = "Gold",
        tagline = "Most chosen plan",
        price = "₹4,999",
        originalPrice = "₹8,999",
        period = "6 MONTHS",
        duration = "6 months",
        contacts = "150 contacts",
        badge = "Most Popular",
        badgeColor = Color(0xFFE91E63),
        features = listOf(
            "Everything in Silver",
            "View 150 contact details",
            "Profile spotlight (2× visibility)",
            "Bold listing in search",
            "Family profile sharing",
            "Dedicated customer support",
            "Export biodata as PDF",
            "SMS match alerts",
            "Premium trust badge"
        ),
        lockedFeatures = listOf(
            "Dedicated relationship manager"
        ),
        cta = "Upgrade to Gold",
        highlighted = true,
        icon = Icons.Filled.Star,
        amountPaise = 499900
    ),
    Plan(
        name = "Platinum",
        tagline = "Best value · Till you marry",
        price = "₹7,499",
        originalPrice = "₹14,999",
        period = "12 MONTHS",
        duration = "12 months",
        contacts = "300 contacts",
        badge = "Best Value",
        badgeColor = Color(0xFF7B1FA2),
        features = listOf(
            "Everything in Gold",
            "View 300 contact details",
            "Top placement in all searches",
            "Profile highlighted to premium members",
            "3× spotlight rotation",
            "Astrology deep-dive report",
            "1 virtual family meet session",
            "Dedicated relationship manager (3 months)"
        ),
        lockedFeatures = emptyList(),
        cta = "Go Platinum",
        highlighted = false,
        icon = Icons.Filled.Diamond,
        amountPaise = 749900
    )
)

private fun planTypeKey(plan: Plan): String = when (plan.name) {
    "Silver"   -> "silver_3m"
    "Gold"     -> "gold_6m"
    "Platinum" -> "platinum_12m"
    else       -> "free"
}

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

    suspend fun createOrder(amount: Int, plan: String): String? {
        _loading.value = true
        val res = subscriptionRepo.createRazorpayOrder(amount, plan)
        _loading.value = false
        return res.getOrNull()
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

    LaunchedEffect(Unit) { Checkout.preload(activity) }

    fun launchPayment(plan: Plan) {
        if (plan.amountPaise == 0 || activity == null) return
        scope.launch {
            val orderId = vm.createOrder(plan.amountPaise, planTypeKey(plan))
            if (orderId == null) {
                snackbarHostState.showSnackbar("Failed to initialize payment. Please try again.")
                return@launch
            }
            vm.subscriptionRepo.pendingPlanType = planTypeKey(plan)
            vm.subscriptionRepo.pendingAmountPaise = plan.amountPaise
            try {
                val checkout = Checkout()
                checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
                val options = JSONObject().apply {
                    put("name", "MatrimonyConnect")
                    put("description", "${plan.name} Membership")
                    put("order_id", orderId)
                    put("amount", plan.amountPaise)
                    put("currency", "INR")
                    put("prefill", JSONObject().apply {
                        put("contact", prefill.phone)
                        put("email", prefill.email)
                    })
                    put("theme", JSONObject().apply { put("color", "#E91E63") })
                }
                checkout.open(activity, options)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("membership_plans", "Membership Plans")) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(t("choose_right_plan", "Choose the right plan"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                
                PLANS.forEach { plan ->
                    PlanCard(plan, onCta = { if (plan.amountPaise == 0) launchPayment(plan) else pendingPlan = plan })
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (loading) {
                Surface(color = Color.Black.copy(alpha = 0.3f), modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }

    pendingPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingPlan = null },
            title = { Text("Confirm Upgrade") },
            text = { Text("Upgrade to ${plan.name} for ${plan.price}?") },
            confirmButton = { Button(onClick = { launchPayment(plan); pendingPlan = null }) { Text("Proceed") } },
            dismissButton = { TextButton(onClick = { pendingPlan = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PlanCard(plan: Plan, onCta: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(plan.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(plan.price, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            plan.features.forEach { f ->
                Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                    Spacer(Modifier.width(8.dp))
                    Text(f, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCta, modifier = Modifier.fillMaxWidth()) { Text(plan.cta) }
        }
    }
}
