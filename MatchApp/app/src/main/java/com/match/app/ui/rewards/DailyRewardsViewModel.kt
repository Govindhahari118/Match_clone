package com.match.app.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.match.app.data.remote.FirestoreFeatureService
import com.match.app.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MarketReward(val id: String, val coins: Int, val title: String, val desc: String)

data class RewardsUiState(
    val loading: Boolean = true,
    val working: Boolean = false,
    val totalCoins: Int = 0,
    val streak: Int = 0,
    val claimedToday: Boolean = false,
    val rewards: List<MarketReward> = listOf(
        MarketReward("boost", 100, "1-hour profile boost", "Adds one hour to your server-managed profile boost window.")
    ),
    val message: String? = null
)

@HiltViewModel
class DailyRewardsViewModel @Inject constructor(
    private val session: SessionStore,
    private val featureService: FirestoreFeatureService
) : ViewModel() {

    private val _ui = MutableStateFlow(RewardsUiState())
    val ui: StateFlow<RewardsUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch { loadState(showLoader = true) }

    private suspend fun loadState(showLoader: Boolean, successMessage: String? = null) {
        val uid = session.firebaseUid.firstOrNull()
        if (uid.isNullOrBlank()) {
            _ui.update { it.copy(loading = false, working = false, message = "Sign in to use rewards.") }
            return
        }
        if (showLoader) _ui.update { it.copy(loading = true, message = null) }
        try {
            val state = featureService.getRewardsState(uid)
            val coins = (state["coins"] as? Number)?.toInt() ?: 0
            val streak = (state["streak"] as? Number)?.toInt() ?: 0
            val lastClaimedDay = state["lastClaimedDay"] as? String ?: ""
            _ui.update {
                it.copy(
                    loading = false,
                    working = false,
                    totalCoins = coins.coerceAtLeast(0),
                    streak = streak.coerceAtLeast(0),
                    claimedToday = lastClaimedDay == LocalDate.now().toString(),
                    message = successMessage
                )
            }
        } catch (_: Exception) {
            _ui.update { it.copy(loading = false, working = false, message = "Rewards could not be loaded. Try again.") }
        }
    }

    fun claimDaily() = viewModelScope.launch {
        if (_ui.value.working || _ui.value.claimedToday) return@launch
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        _ui.update { it.copy(working = true, message = null) }
        try {
            // Arguments are compatibility-only; the trusted backend calculates streak and coins.
            featureService.claimDailyReward(uid, 0, 0)
            loadState(showLoader = false, successMessage = "Daily reward claimed.")
        } catch (_: Exception) {
            _ui.update { it.copy(working = false, message = "Daily reward was not claimed. Please retry.") }
        }
    }

    fun redeemReward(rewardId: String, cost: Int) = viewModelScope.launch {
        if (_ui.value.working) return@launch
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        _ui.update { it.copy(working = true, message = null) }
        try {
            // Cost is compatibility-only; the backend owns the catalogue and authoritative price.
            featureService.redeemReward(uid, rewardId, cost)
            loadState(showLoader = false, successMessage = "Reward redeemed successfully.")
        } catch (_: Exception) {
            _ui.update { it.copy(working = false, message = "Reward could not be redeemed. Check your balance and try again.") }
        }
    }

    fun consumeMessage() = _ui.update { it.copy(message = null) }
}
