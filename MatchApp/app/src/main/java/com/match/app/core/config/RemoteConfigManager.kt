package com.match.app.core.config

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.match.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production feature flags come from Firebase Remote Config.
 *
 * Defaults are deliberately safe: optional/risky surfaces stay off unless the trusted production
 * project explicitly enables them. Fetch failures never silently enable an optional feature.
 */
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

        private val DEFAULTS: Map<String, Any> = mapOf(
            KEY_FREE_MSG_LIMIT to 5L,
            KEY_BOOST_DURATION_HOURS to 24L,
            KEY_MAX_DAILY_LIKES to 20L,
            KEY_SHOW_VIDEO_PROFILES to false,
            KEY_ENABLE_VOICE_CALLS to false,
            KEY_ENABLE_COMMUNITY to false,
            KEY_MIN_PHOTOS_FOR_BOOST to 2L,
            KEY_MAINTENANCE_MODE to false,
            KEY_MAINTENANCE_MSG to "We're performing maintenance. Please try again shortly.",
            KEY_FORCE_UPDATE_VERSION to 0L,
            KEY_RECOMMENDED_UPDATE_VERSION to 0L,
            KEY_REFERRAL_BONUS_DAYS to 0L,
            KEY_ENABLE_NRI_FEATURES to false,
            KEY_MAX_PROFILE_PHOTOS to 6L,
            KEY_ENABLE_AI_ICEBREAKERS to false,
            KEY_DAILY_REWARD_ENABLED to false
        )
    }

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance().apply {
        setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
                .build()
        )
        setDefaultsAsync(DEFAULTS)
    }

    fun fetchAndActivate(onComplete: (Boolean) -> Unit = {}) {
        remoteConfig.fetchAndActivate()
            .addOnSuccessListener { activated -> onComplete(activated) }
            .addOnFailureListener { error ->
                Log.w("RemoteConfig", "Remote Config fetch failed; safe defaults/last activated values remain in effect", error)
                onComplete(false)
            }
    }

    val freeMessageLimit: Int get() = remoteConfig.getLong(KEY_FREE_MSG_LIMIT).toInt().coerceAtLeast(0)
    val boostDurationHours: Int get() = remoteConfig.getLong(KEY_BOOST_DURATION_HOURS).toInt().coerceAtLeast(0)
    val maxDailyLikes: Int get() = remoteConfig.getLong(KEY_MAX_DAILY_LIKES).toInt().coerceAtLeast(0)
    val showVideoProfiles: Boolean get() = remoteConfig.getBoolean(KEY_SHOW_VIDEO_PROFILES)
    val enableVoiceCalls: Boolean get() = remoteConfig.getBoolean(KEY_ENABLE_VOICE_CALLS)
    val enableCommunity: Boolean get() = remoteConfig.getBoolean(KEY_ENABLE_COMMUNITY)
    val minPhotosForBoost: Int get() = remoteConfig.getLong(KEY_MIN_PHOTOS_FOR_BOOST).toInt().coerceAtLeast(0)
    val isMaintenanceMode: Boolean get() = remoteConfig.getBoolean(KEY_MAINTENANCE_MODE)
    val maintenanceMessage: String get() = remoteConfig.getString(KEY_MAINTENANCE_MSG)
        .ifBlank { DEFAULTS.getValue(KEY_MAINTENANCE_MSG) as String }
    val forceUpdateVersionCode: Int get() = remoteConfig.getLong(KEY_FORCE_UPDATE_VERSION).toInt().coerceAtLeast(0)
    val recommendedUpdateVersionCode: Int get() = remoteConfig.getLong(KEY_RECOMMENDED_UPDATE_VERSION).toInt().coerceAtLeast(0)
    val referralBonusDays: Int get() = remoteConfig.getLong(KEY_REFERRAL_BONUS_DAYS).toInt().coerceAtLeast(0)
    val enableNriFeatures: Boolean get() = remoteConfig.getBoolean(KEY_ENABLE_NRI_FEATURES)
    val maxProfilePhotos: Int get() = remoteConfig.getLong(KEY_MAX_PROFILE_PHOTOS).toInt().coerceAtLeast(1)
    val enableAiIcebreakers: Boolean get() = remoteConfig.getBoolean(KEY_ENABLE_AI_ICEBREAKERS)
    val dailyRewardEnabled: Boolean get() = remoteConfig.getBoolean(KEY_DAILY_REWARD_ENABLED)
}
