package com.match.app.ui.rewards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

data class DailyTask(val id: String, val icon: ImageVector, val title: String, val coins: Int, val done: Boolean)
data class RewardBadge(val id: String, val emoji: String, val name: String, val desc: String, val earned: Boolean)
data class MarketReward(val id: String, val coins: Int, val title: String, val desc: String)

data class RewardsUiState(
    val loading: Boolean = true,
    val totalCoins: Int = 0,
    val streak: Int = 0,
    val claimedToday: Boolean = false,
    val tasks: List<DailyTask> = emptyList(),
    val badges: List<RewardBadge> = emptyList(),
    val rewards: List<MarketReward> = emptyList()
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

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true) }
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        
        try {
            val state = featureService.getRewardsState(uid)
            val completedTasks = featureService.getCompletedTasks(uid)
            
            val coins = (state["coins"] as? Long)?.toInt() ?: 0
            val streak = (state["streak"] as? Long)?.toInt() ?: 0
            val lastClaimed = state["lastClaimed"] as? com.google.firebase.Timestamp
            
            val today = LocalDate.now()
            val lastDate = lastClaimed?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
            val isClaimedToday = lastDate == today

            _ui.update { it.copy(
                loading = false,
                totalCoins = coins,
                streak = streak,
                claimedToday = isClaimedToday,
                tasks = buildTasks(completedTasks),
                badges = buildBadges(),
                rewards = buildRewards()
            )}
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false) }
        }
    }

    fun claimDaily() = viewModelScope.launch {
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        val currentStreak = _ui.value.streak
        val newStreak = currentStreak + 1
        val rewardAmount = 10 + currentStreak * 5
        
        featureService.claimDailyReward(uid, newStreak, rewardAmount)
        refresh()
    }

    fun completeTask(taskId: String, coins: Int) = viewModelScope.launch {
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        featureService.completeTask(uid, taskId, coins)
        refresh()
    }

    fun redeemReward(rewardId: String, cost: Int) = viewModelScope.launch {
        val uid = session.firebaseUid.firstOrNull() ?: return@launch
        try {
            featureService.redeemReward(uid, rewardId, cost)
            refresh()
        } catch (e: Exception) {
            // Handle error (e.g., show toast)
        }
    }

    private fun buildTasks(completed: Set<String>) = listOf(
        DailyTask("login", Icons.AutoMirrored.Filled.Login, "Daily login", 10, true),
        DailyTask("view_5", Icons.Filled.Visibility, "View 5 profiles", 15, completed.contains("view_5")),
        DailyTask("send_3", Icons.AutoMirrored.Filled.Send, "Send 3 interests", 20, completed.contains("send_3")),
        DailyTask("chat_reply", Icons.AutoMirrored.Filled.Chat, "Reply to a message", 15, completed.contains("chat_reply")),
        DailyTask("photo_upload", Icons.Filled.PhotoCamera, "Upload a new photo", 25, completed.contains("photo_upload")),
        DailyTask("bio_update", Icons.Filled.Edit, "Update your bio", 20, completed.contains("bio_update")),
        DailyTask("share_profile", Icons.Filled.Share, "Share profile link", 30, completed.contains("share_profile")),
        DailyTask("rate_app", Icons.Filled.Star, "Rate Match app", 50, completed.contains("rate_app"))
    )

    private fun buildBadges() = listOf(
        RewardBadge("rising_star", "🌟", "Rising Star", "Complete profile to 100%", true),
        RewardBadge("streak_7", "🔥", "7-Day Streak", "Login for 7 consecutive days", true),
        RewardBadge("starter", "💬", "Conversation Starter", "Send 50 messages", true),
        RewardBadge("popular", "❤️", "Popular Pick", "Get 100 interests received", false),
        RewardBadge("premium", "👑", "Premium Member", "Subscribe to any plan", false),
        RewardBadge("verified", "✅", "Verified Profile", "Complete ID verification", true)
    )

    private fun buildRewards() = listOf(
        MarketReward("boost", 100, "1 Profile Boost", "Appear at the top of search for 1 hour"),
        MarketReward("visitors", 200, "See Who Viewed (1 day)", "Unlock all profile visitors for 24 hours"),
        MarketReward("super_int", 500, "5 Super Interests", "Stand out with priority interest requests"),
        MarketReward("incognito", 1000, "Incognito Mode (7 days)", "Browse profiles without being seen")
    )
}
