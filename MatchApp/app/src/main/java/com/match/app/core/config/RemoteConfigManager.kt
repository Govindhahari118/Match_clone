package com.match.app.core.config

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor() {

    companion object {
        const val KEY_FREE_MSG_LIMIT = "free_message_limit"
        const val KEY_BOOST_DURATION_HOURS = "boost_duration_hours"
        const val KEY_MAX_DAILY_LIKES = "max_daily_likes_free"
        const val KEY_SHOW_VIDEO_PROFILES = "show_video_profiles"
        const val KEY_ENABLE_VOICE_CALLS = "enable_voice_calls"
        const val KEY_ENABLE_COMMUNITY = "enable_community"
        const val KEY_MIN_PHOTOS_FOR_BOOST = "min_photos_for_boost"
        const val KEY_MAINTENANCE_MODE = "maintenance_mode"
        const val KEY_MAINTENANCE_MSG = "maintenance_message"
        const val KEY_FORCE_UPDATE_VERSION = "force_update_version_code"
        const val KEY_RECOMMENDED_UPDATE_VERSION = "recommended_update_version_code"
        const val KEY_REFERRAL_BONUS_DAYS = "referral_bonus_premium_days"
        const val KEY_ENABLE_NRI_FEATURES = "enable_nri_features"
        const val KEY_MAX_PROFILE_PHOTOS = "max_profile_photos"
        const val KEY_ENABLE_AI_ICEBREAKERS = "enable_ai_icebreakers"
        const val KEY_DAILY_REWARD_ENABLED = "daily_reward_enabled"

        private val DEFAULTS = mapOf(
            KEY_FREE_MSG_LIMIT to 5L,
            KEY_BOOST_DURATION_HOURS to 24L,
            KEY_MAX_DAILY_LIKES to 20L,
            KEY_SHOW_VIDEO_PROFILES to true,
            KEY_ENABLE_VOICE_CALLS to false,
            KEY_ENABLE_COMMUNITY to true,
            KEY_MIN_PHOTOS_FOR_BOOST to 2L,
            KEY_MAINTENANCE_MODE to false,
            KEY_MAINTENANCE_MSG to "We're performing maintenance. Please try again shortly.",
            KEY_FORCE_UPDATE_VERSION to 1L,
            KEY_RECOMMENDED_UPDATE_VERSION to 1L,
            KEY_REFERRAL_BONUS_DAYS to 7L,
            KEY_ENABLE_NRI_FEATURES to true,
            KEY_MAX_PROFILE_PHOTOS to 6L,
            KEY_ENABLE_AI_ICEBREAKERS to true,
            KEY_DAILY_REWARD_ENABLED to true
        )
    }

    fun fetchAndActivate(onComplete: (Boolean) -> Unit = {}) {
        Log.d("RemoteConfig", "Bypassing Firebase due to fake API keys. Using local defaults.")
        onComplete(true)
    }

    val freeMessageLimit: Int get() = (DEFAULTS[KEY_FREE_MSG_LIMIT] as Long).toInt()
    val boostDurationHours: Int get() = (DEFAULTS[KEY_BOOST_DURATION_HOURS] as Long).toInt()
    val maxDailyLikes: Int get() = (DEFAULTS[KEY_MAX_DAILY_LIKES] as Long).toInt()
    val showVideoProfiles: Boolean get() = DEFAULTS[KEY_SHOW_VIDEO_PROFILES] as Boolean
    val enableVoiceCalls: Boolean get() = DEFAULTS[KEY_ENABLE_VOICE_CALLS] as Boolean
    val enableCommunity: Boolean get() = DEFAULTS[KEY_ENABLE_COMMUNITY] as Boolean
    val minPhotosForBoost: Int get() = (DEFAULTS[KEY_MIN_PHOTOS_FOR_BOOST] as Long).toInt()
    val isMaintenanceMode: Boolean get() = DEFAULTS[KEY_MAINTENANCE_MODE] as Boolean
    val maintenanceMessage: String get() = DEFAULTS[KEY_MAINTENANCE_MSG] as String
    val forceUpdateVersionCode: Int get() = (DEFAULTS[KEY_FORCE_UPDATE_VERSION] as Long).toInt()
    val recommendedUpdateVersionCode: Int get() = (DEFAULTS[KEY_RECOMMENDED_UPDATE_VERSION] as Long).toInt()
    val referralBonusDays: Int get() = (DEFAULTS[KEY_REFERRAL_BONUS_DAYS] as Long).toInt()
    val enableNriFeatures: Boolean get() = DEFAULTS[KEY_ENABLE_NRI_FEATURES] as Boolean
    val maxProfilePhotos: Int get() = (DEFAULTS[KEY_MAX_PROFILE_PHOTOS] as Long).toInt()
    val enableAiIcebreakers: Boolean get() = DEFAULTS[KEY_ENABLE_AI_ICEBREAKERS] as Boolean
    val dailyRewardEnabled: Boolean get() = DEFAULTS[KEY_DAILY_REWARD_ENABLED] as Boolean
}
