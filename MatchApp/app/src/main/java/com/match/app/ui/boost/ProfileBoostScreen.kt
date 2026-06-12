package com.match.app.ui.boost

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
import com.match.app.data.repo.AuthRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.i18n.t
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class BoostPlanUi(
    val name: String,
    val duration: String,
    val price: String,
    val multiplier: String,
    val tag: String = "",
    val durationMs: Long
)

private val PLANS = listOf(
    BoostPlanUi("Quick Boost",  "3 Hours",  "₹99",  "5× more views",  "Try it out",    3 * 60 * 60 * 1000L),
    BoostPlanUi("Power Boost",  "24 Hours", "₹249", "10× more views", "Most Popular",  24 * 60 * 60 * 1000L),
    BoostPlanUi("Super Boost",  "7 Days",   "₹799", "15× more views", "Best Value",    7L * 24 * 60 * 60 * 1000L)
)

@HiltViewModel
class ProfileBoostViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository
) : ViewModel() {
    var isBoostActive by mutableStateOf(false)
        private set
    var boostExpiresInMs by mutableLongStateOf(0L)
        private set
    var boosted by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val uid = session.userId.first() ?: return@launch
            isBoostActive = auth.isBoostActive(uid)
            boostExpiresInMs = (auth.getBoostExpiryMs(uid) - System.currentTimeMillis()).coerceAtLeast(0L)
        }
    }

    fun activateBoost(durationMs: Long) = viewModelScope.launch {
        val uid = session.userId.first() ?: return@launch
        auth.activateBoost(uid, durationMs)
        isBoostActive = true
        boostExpiresInMs = durationMs
        boosted = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBoostScreen(onBack: () -> Unit = {}, vm: ProfileBoostViewModel = hiltViewModel()) {
    var selectedPlan by remember { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("profile_boost", "Profile Boost")) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("boost_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("boost_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active boost banner
            if (vm.isBoostActive) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFF8E1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, null, Modifier.size(28.dp), tint = Color(0xFFFFB300))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(t("boost_active", "Boost Active! 🚀"), fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall)
                            val hours = TimeUnit.MILLISECONDS.toHours(vm.boostExpiresInMs)
                            val mins  = TimeUnit.MILLISECONDS.toMinutes(vm.boostExpiresInMs) % 60
                            Text("Expires in ${hours}h ${mins}m", style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100))
                        }
                    }
                }
            }

            // Hero
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
                    Icon(Icons.Filled.Bolt, null, Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text(t("boost_more_views", "Get 10× More Views"), style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Boosted profiles appear at the top of search results and daily recommendations.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // How it works
            Text(t("how_boost_works", "How Boost Works"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            listOf(
                Triple(Icons.Filled.ArrowUpward, "Top placement", "Your profile appears first in all matches & searches"),
                Triple(Icons.Filled.Visibility, "More visibility", "Shown to 10× more potential matches"),
                Triple(Icons.Filled.Notifications, "Instant alerts", "You're notified immediately when someone views you"),
                Triple(Icons.Filled.Star, "Boost badge", "A special ⚡ badge makes your profile stand out")
            ).forEach { (icon, title, desc) ->
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(desc, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider()

            // Plan cards
            Text(t("choose_boost_plan", "Choose a Boost Plan"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            PLANS.forEachIndexed { idx, plan ->
                val selected = selectedPlan == idx
                ElevatedCard(
                    onClick = { selectedPlan = idx },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("boost_plan_$idx"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                         else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(if (selected) 6.dp else 2.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(plan.name, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall)
                                if (plan.tag.isNotBlank()) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (plan.tag == "Most Popular") Color(0xFFFFB300).copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(plan.tag, style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (plan.tag == "Most Popular") Color(0xFFE65100)
                                                    else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(plan.duration, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(plan.multiplier, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                        Text(plan.price, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = selected, onClick = { selectedPlan = idx })
                    }
                }
            }

            // CTA
            if (vm.boosted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text("Boost activated! Your profile is now featured. 🚀",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1B5E20))
                    }
                }
            } else {
                Button(
                    onClick = { vm.activateBoost(PLANS[selectedPlan].durationMs) },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("boost_activate"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Bolt, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Boost Now — ${PLANS[selectedPlan].price}",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
