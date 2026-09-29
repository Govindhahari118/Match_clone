package com.match.app.ui.deepcompat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.repo.MatchingRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompatFactorUi(
    val key: String,
    val label: String,
    val scorePct: Int,
    val configuredWeightPct: Int
)

data class CompatScoreState(
    val isLoading: Boolean = true,
    val totalPct: Int = 0,
    val formulaVersion: String = "",
    val factors: List<CompatFactorUi> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CompatibilityDeepDiveViewModel @Inject constructor(
    private val matching: MatchingRepository,
    private val session: SessionStore
) : ViewModel() {

    private val _state = MutableStateFlow(CompatScoreState())
    val state = _state.asStateFlow()

    val planKey: StateFlow<String> = session.subscriptionPlan
        .stateIn(viewModelScope, SharingStarted.Eagerly, "FREE")

    fun load(candidateId: Long) = viewModelScope.launch {
        val myId = session.userId.first()
        if (myId == null || candidateId <= 0) {
            _state.value = CompatScoreState(isLoading = false, error = "Open a valid match profile.")
            return@launch
        }

        _state.value = CompatScoreState(isLoading = true)
        runCatching {
            matching.recommendations(
                seekerId = myId,
                mode = MatchMode.ADVANCED,
                filter = MatchFilter(withPhotoOnly = false)
            ).firstOrNull { it.user.id == candidateId }
                ?: error("This profile is no longer available for compatibility details.")
        }.onSuccess { result ->
            _state.value = CompatScoreState(
                isLoading = false,
                totalPct = (result.combinedScore.coerceIn(0f, 1f) * 100).toInt(),
                formulaVersion = result.formulaVersion,
                factors = result.factors.map { factor ->
                    CompatFactorUi(
                        key = factor.key,
                        label = when (factor.key) {
                            "bilateral_preferences" -> "Mutual partner preferences"
                            "questionnaire" -> "Questionnaire"
                            "astrology" -> "Astrology signal (beta)"
                            "demographics_lifestyle" -> "Profile & lifestyle"
                            "mutual_trust" -> "Mutual verification"
                            else -> factor.key.replace('_', ' ').replaceFirstChar { it.uppercase() }
                        },
                        scorePct = (factor.score.coerceIn(0f, 1f) * 100).toInt(),
                        configuredWeightPct = (factor.configuredWeight.coerceIn(0f, 1f) * 100).toInt()
                    )
                }
            )
        }.onFailure { error ->
            _state.value = CompatScoreState(
                isLoading = false,
                error = error.message?.take(180)
                    ?: "Compatibility details are unavailable."
            )
        }
    }
}
