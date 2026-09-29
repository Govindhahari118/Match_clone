package com.match.app.ui.kundli

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.core.matching.Astrology
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.KundliRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.components.MatreeHero
import com.match.app.ui.components.MatreeInfoCard
import com.match.app.ui.components.MatreeInlineNotice
import com.match.app.ui.components.MatreeLoadingState
import com.match.app.ui.components.MatreeMatchSignal
import com.match.app.ui.components.MatreeStatusTone
import com.match.app.ui.components.MatreeTopBar
import com.match.app.ui.theme.MatreeDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KundliUi(
    val loading: Boolean = true,
    val myName: String = "",
    val myRasi: String = "",
    val myNakshatra: String = "",
    val targetName: String = "",
    val targetRasi: String = "",
    val targetNakshatra: String = "",
    val targetId: Long? = null,
    val targetAllowsHoroscope: Boolean = true,
    val astrologyScore: Float? = null,
    val message: String? = null
)

@HiltViewModel
class KundliViewModel @Inject constructor(
    private val session: SessionStore,
    private val auth: AuthRepository,
    private val kundliRepository: KundliRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(KundliUi())
    val ui: StateFlow<KundliUi> = _ui.asStateFlow()
    private var loadedTarget: Long? = Long.MIN_VALUE

    fun load(targetId: Long?) {
        if (loadedTarget == targetId) return
        loadedTarget = targetId
        viewModelScope.launch {
            _ui.value = KundliUi(loading = true, targetId = targetId)
            val meId = session.userId.first()
            if (meId == null) {
                _ui.value = KundliUi(loading = false, targetId = targetId, message = "Sign in to view Kundali compatibility.")
                return@launch
            }
            val me = auth.currentProfile(meId)
            if (me == null) {
                _ui.value = KundliUi(loading = false, targetId = targetId, message = "Complete your profile first.")
                return@launch
            }

            if (targetId == null) {
                _ui.value = KundliUi(
                    loading = false,
                    myName = me.displayName,
                    myRasi = me.rasi,
                    myNakshatra = me.nakshatra,
                    message = if (me.rasi.isBlank() || me.nakshatra.isBlank()) "Add Rasi and Nakshatra to your profile to use compatibility." else null
                )
                return@launch
            }

            if (targetId == meId) {
                _ui.value = KundliUi(
                    loading = false,
                    myName = me.displayName,
                    myRasi = me.rasi,
                    myNakshatra = me.nakshatra,
                    targetId = targetId,
                    message = "Choose another member to compare."
                )
                return@launch
            }

            // This read itself is server-reauthorized by AuthRepository. It prevents a stale Room
            // record from becoming a way to access a profile the member has since hidden/blocked.
            val target = auth.currentProfile(targetId)
            if (target == null || target.firebaseUid.isBlank()) {
                _ui.value = KundliUi(
                    loading = false,
                    myName = me.displayName,
                    myRasi = me.rasi,
                    myNakshatra = me.nakshatra,
                    targetId = targetId,
                    message = "This profile is unavailable."
                )
                return@launch
            }

            kundliRepository.getSharedHoroscope(target.firebaseUid)
                .onSuccess { shared ->
                    val score = if (shared.available) {
                        Astrology.scoreOrNull(
                            shared.myRasi,
                            shared.myNakshatra,
                            shared.targetRasi,
                            shared.targetNakshatra
                        )
                    } else null
                    _ui.value = KundliUi(
                        loading = false,
                        myName = me.displayName,
                        myRasi = shared.myRasi.ifBlank { me.rasi },
                        myNakshatra = shared.myNakshatra.ifBlank { me.nakshatra },
                        targetName = shared.targetName.ifBlank { target.displayName },
                        targetRasi = if (shared.available) shared.targetRasi else "",
                        targetNakshatra = if (shared.available) shared.targetNakshatra else "",
                        targetId = targetId,
                        targetAllowsHoroscope = shared.reason != "not_shared",
                        astrologyScore = score,
                        message = when (shared.reason) {
                            "not_shared" -> "This member chose not to share horoscope details."
                            "not_applicable" -> "Kundali comparison is shown only when it is applicable to both members."
                            "viewer_incomplete" -> "Add your Rasi and Nakshatra before comparing."
                            "target_incomplete" -> "This member has not added enough horoscope information for comparison."
                            else -> when {
                                !shared.available -> "Horoscope compatibility is not available for this profile."
                                score == null -> "Horoscope information is outside the supported Rasi/Nakshatra catalogue."
                                else -> null
                            }
                        }
                    )
                }
                .onFailure {
                    _ui.value = KundliUi(
                        loading = false,
                        myName = me.displayName,
                        myRasi = me.rasi,
                        myNakshatra = me.nakshatra,
                        targetName = target.displayName,
                        targetId = targetId,
                        targetAllowsHoroscope = false,
                        message = "Kundali details are unavailable for this member right now."
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KundliScreen(
    targetId: Long? = null,
    onBack: () -> Unit = {},
    vm: KundliViewModel = hiltViewModel()
) {
    LaunchedEffect(targetId) { vm.load(targetId) }
    val ui by vm.ui.collectAsState()

    Scaffold(
        topBar = {
            MatreeTopBar(
                title = if (targetId == null) "Kundali" else "Kundali comparison",
                onBack = onBack
            )
        }
    ) { pad ->
        when {
            ui.loading -> Box(
                Modifier.padding(pad).fillMaxSize().padding(MatreeDesign.spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                MatreeLoadingState(message = "Loading Kundali…", rows = 2)
            }
            targetId == null -> OwnAstrologyContent(ui, Modifier.padding(pad))
            else -> PairCompatibilityContent(ui, Modifier.padding(pad))
        }
    }
}

@Composable
private fun OwnAstrologyContent(ui: KundliUi, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(MatreeDesign.spacing.lg)
            .testTag("kundli_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.md)
    ) {
        MatreeHero(
            title = "Your astrology profile",
            subtitle = "Astrology is shown only when it is applicable and enough information is available.",
            leadingIcon = Icons.Filled.AutoAwesome
        )
        if (ui.myRasi.isNotBlank()) FactCard("Rasi", ui.myRasi)
        if (ui.myNakshatra.isNotBlank()) FactCard("Nakshatra", ui.myNakshatra)
        ui.message?.let { InfoCard(it) }
        InfoCard("Open a member from Discover or Received Interests and choose Check Kundali to compare the information both profiles have chosen to share.")
    }
}

@Composable
private fun PairCompatibilityContent(ui: KundliUi, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(MatreeDesign.spacing.lg)
            .testTag("kundli_pair_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MatreeDesign.spacing.sm)
    ) {
        Text(
            if (ui.targetName.isBlank()) "Compatibility" else "${ui.myName} & ${ui.targetName}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        ui.astrologyScore?.let { score ->
            MatreeMatchSignal(
                label = "Rasi / Nakshatra compatibility signal",
                score = score,
                supportingText = "A decision-support signal from the shared horoscope information; not a prediction or guarantee."
            )
        }

        if (ui.myRasi.isNotBlank() || ui.myNakshatra.isNotBlank()) {
            ProfileAstrologyCard(ui.myName.ifBlank { "You" }, ui.myRasi, ui.myNakshatra)
        }
        if (ui.targetAllowsHoroscope && (ui.targetRasi.isNotBlank() || ui.targetNakshatra.isNotBlank())) {
            ProfileAstrologyCard(ui.targetName.ifBlank { "Member" }, ui.targetRasi, ui.targetNakshatra)
        }
        ui.message?.let { InfoCard(it) }
        InfoCard("This comparison uses only the Rasi and Nakshatra information the member has chosen to share. Birth date, time and place remain private here. It is not a complete birth-chart consultation or a prediction/guarantee.")
    }
}

@Composable
private fun ProfileAstrologyCard(name: String, rasi: String, nakshatra: String) {
    MatreeInfoCard {
        Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (rasi.isNotBlank()) Text("Rasi: $rasi")
        if (nakshatra.isNotBlank()) Text("Nakshatra: $nakshatra")
    }
}

@Composable
private fun FactCard(label: String, value: String) {
    MatreeInfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoCard(message: String) {
    MatreeInlineNotice(
        message = message,
        icon = Icons.Filled.Info,
        tone = MatreeStatusTone.NEUTRAL
    )
}
