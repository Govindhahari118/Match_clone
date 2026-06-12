package com.match.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "match_session")

class SessionStore(private val context: Context) {

    private val KEY_USER_ID   = longPreferencesKey("user_id")
    private val KEY_FIREBASE_UID = stringPreferencesKey("firebase_uid")
    private val KEY_MODE      = longPreferencesKey("match_mode")
    private val KEY_ONBOARD   = booleanPreferencesKey("onboarded")
    private val KEY_AGE_MIN   = intPreferencesKey("filter_age_min")
    private val KEY_AGE_MAX   = intPreferencesKey("filter_age_max")
    private val KEY_CITY      = stringPreferencesKey("filter_city")
    private val KEY_STATE     = stringPreferencesKey("filter_state")
    private val KEY_CASTE     = stringPreferencesKey("filter_caste")
    private val KEY_MIN_SCORE = floatPreferencesKey("filter_min_score")
    private val KEY_RELIGION  = stringPreferencesKey("filter_religion")
    private val KEY_TONGUE    = stringPreferencesKey("filter_tongue")
    private val KEY_MARITAL   = stringPreferencesKey("filter_marital")
    private val KEY_VERIFIED  = booleanPreferencesKey("filter_verified")
    private val KEY_DARK      = booleanPreferencesKey("dark_mode")
    private val KEY_PALETTE   = stringPreferencesKey("palette_key")
    private val KEY_API_BASE  = stringPreferencesKey("api_base_url")
    private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_lock")
    private val KEY_INCOME_MIN = stringPreferencesKey("filter_income_min")
    private val KEY_INCOME_MAX = stringPreferencesKey("filter_income_max")
    private val KEY_EDUCATION  = stringPreferencesKey("filter_education")
    private val KEY_DIET       = stringPreferencesKey("filter_diet")
    private val KEY_RESIDENTIAL = stringPreferencesKey("filter_residential")
    private val KEY_CHILDREN   = stringPreferencesKey("filter_children")
    private val KEY_KEYWORD    = stringPreferencesKey("filter_keyword")
    private val KEY_GOTHRA     = stringPreferencesKey("filter_gothra")
    private val KEY_NATIVE_STATE = stringPreferencesKey("filter_native_state")
    private val KEY_COUNTRY    = stringPreferencesKey("filter_country")
    private val KEY_NRI_ONLY   = booleanPreferencesKey("filter_nri_only")
    private val KEY_RELOCATE   = booleanPreferencesKey("filter_relocate")
    private val KEY_RECENT_DAYS = intPreferencesKey("filter_recent_days")
    private val KEY_UI_LANG    = stringPreferencesKey("ui_language")
    private val KEY_COMMUNITY_SETUP_DONE = booleanPreferencesKey("community_setup_done")
    private val KEY_LAST_ACTIVE = longPreferencesKey("last_active_at")
    private val KEY_INCOGNITO   = booleanPreferencesKey("incognito_mode")
    private val KEY_SUB_PLAN   = stringPreferencesKey("subscription_plan")
    private val KEY_DPDP_CONSENT = booleanPreferencesKey("dpdp_consent_given")
    private val KEY_DPDP_CONSENT_AT = longPreferencesKey("dpdp_consent_at")

    companion object {
        /** Auto-logout after 30 days of inactivity. */
        private const val SESSION_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000
    }

    val userId: Flow<Long?> = context.dataStore.data.map { it[KEY_USER_ID]?.takeIf { id -> id > 0 } }
    val firebaseUid: Flow<String?> = context.dataStore.data.map { it[KEY_FIREBASE_UID]?.takeIf { uid -> uid.isNotBlank() } }
    val mode: Flow<MatchMode> = context.dataStore.data.map {
        when (it[KEY_MODE] ?: 2L) { 0L -> MatchMode.QUESTIONNAIRE; 1L -> MatchMode.ASTROLOGY; else -> MatchMode.ADVANCED }
    }
    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARD] ?: false }
    val filter: Flow<MatchFilter> = context.dataStore.data.map {
        MatchFilter(
            ageMin       = it[KEY_AGE_MIN]   ?: 18,
            ageMax       = it[KEY_AGE_MAX]   ?: 70,
            city         = it[KEY_CITY]      ?: "",
            state        = it[KEY_STATE]     ?: "",
            caste        = it[KEY_CASTE]     ?: "",
            minScore     = it[KEY_MIN_SCORE] ?: 0f,
            religion     = it[KEY_RELIGION]  ?: "",
            motherTongue = it[KEY_TONGUE]    ?: "",
            maritalStatus= it[KEY_MARITAL]   ?: "",
            verifiedOnly = it[KEY_VERIFIED]  ?: false,
            incomeMin    = it[KEY_INCOME_MIN] ?: "",
            incomeMax    = it[KEY_INCOME_MAX] ?: "",
            educationLevel = it[KEY_EDUCATION] ?: "",
            diet         = it[KEY_DIET]      ?: "",
            residentialStatus = it[KEY_RESIDENTIAL] ?: "",
            hasChildren  = it[KEY_CHILDREN]  ?: "",
            keyword      = it[KEY_KEYWORD]   ?: "",
            gothra       = it[KEY_GOTHRA]    ?: "",
            nativeState  = it[KEY_NATIVE_STATE] ?: "",
            countryOfResidence = it[KEY_COUNTRY] ?: "",
            nriOnly      = it[KEY_NRI_ONLY]  ?: false,
            willingToRelocate = it[KEY_RELOCATE] ?: false,
            recentlyJoinedDays = it[KEY_RECENT_DAYS] ?: 0
        )
    }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK] ?: false }
    val paletteKey: Flow<String> = context.dataStore.data.map { it[KEY_PALETTE] ?: "VIVAH" }
    val apiBaseUrl: Flow<String> = context.dataStore.data.map {
        it[KEY_API_BASE] ?: ""   // No REST server; Firebase IS the backend. Only set in debug.
    }
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC] ?: false }
    val subscriptionPlan: Flow<String> = context.dataStore.data.map { it[KEY_SUB_PLAN] ?: "FREE" }
    val uiLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_UI_LANG] ?: run {
            // Auto-detect system language on first run; fall back to English if unsupported
            val supported = setOf(
                "en","hi","te","ta","kn","mr","bn","gu","ml","pa","ur",
                "es","pt","ru","ar","de","fr","it","ja","ko","zh",
                "id","tr","sw","vi","th"
            )
            val sys = java.util.Locale.getDefault().language
            if (sys in supported) sys else "en"
        }
    }
    val communitySetupDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_COMMUNITY_SETUP_DONE] ?: false }
    val hasQuestionnaire: Flow<Boolean> = context.dataStore.data.map { it[booleanPreferencesKey("has_questionnaire")] ?: false }
    val incognitoMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_INCOGNITO] ?: false }
    val dpdpConsentGiven: Flow<Boolean> = context.dataStore.data.map { it[KEY_DPDP_CONSENT] ?: false }

    suspend fun setUser(id: Long)   = context.dataStore.edit { it[KEY_USER_ID] = id }
    suspend fun setFirebaseUid(uid: String) = context.dataStore.edit { it[KEY_FIREBASE_UID] = uid }
    suspend fun setSubscriptionPlan(plan: String) = context.dataStore.edit { it[KEY_SUB_PLAN] = plan }
    suspend fun clear()              = context.dataStore.edit {
        it.remove(KEY_USER_ID)
        it.remove(KEY_FIREBASE_UID)
        // Keep KEY_ONBOARD so returning users don't re-see onboarding
    }
    suspend fun setMode(m: MatchMode) = context.dataStore.edit {
        it[KEY_MODE] = when (m) { MatchMode.QUESTIONNAIRE -> 0L; MatchMode.ASTROLOGY -> 1L; MatchMode.ADVANCED -> 2L }
    }
    suspend fun setOnboarded(v: Boolean) = context.dataStore.edit { it[KEY_ONBOARD] = v }
    suspend fun setFilter(f: MatchFilter) = context.dataStore.edit {
        it[KEY_AGE_MIN]   = f.ageMin
        it[KEY_AGE_MAX]   = f.ageMax
        it[KEY_CITY]      = f.city
        it[KEY_STATE]     = f.state
        it[KEY_CASTE]     = f.caste
        it[KEY_MIN_SCORE] = f.minScore
        it[KEY_RELIGION]  = f.religion
        it[KEY_TONGUE]    = f.motherTongue
        it[KEY_MARITAL]   = f.maritalStatus
        it[KEY_VERIFIED]  = f.verifiedOnly
        it[KEY_INCOME_MIN] = f.incomeMin
        it[KEY_INCOME_MAX] = f.incomeMax
        it[KEY_EDUCATION]  = f.educationLevel
        it[KEY_DIET]       = f.diet
        it[KEY_RESIDENTIAL] = f.residentialStatus
        it[KEY_CHILDREN]   = f.hasChildren
        it[KEY_KEYWORD]    = f.keyword
        it[KEY_GOTHRA]     = f.gothra
        it[KEY_NATIVE_STATE] = f.nativeState
        it[KEY_COUNTRY]    = f.countryOfResidence
        it[KEY_NRI_ONLY]   = f.nriOnly
        it[KEY_RELOCATE]   = f.willingToRelocate
        it[KEY_RECENT_DAYS] = f.recentlyJoinedDays
    }
    suspend fun setDarkMode(v: Boolean) = context.dataStore.edit { it[KEY_DARK] = v }
    suspend fun setPalette(key: String) = context.dataStore.edit { it[KEY_PALETTE] = key }
    suspend fun setApiBaseUrl(url: String) = context.dataStore.edit { it[KEY_API_BASE] = url }
    suspend fun setBiometricLock(v: Boolean) = context.dataStore.edit { it[KEY_BIOMETRIC] = v }
    suspend fun setUiLanguage(lang: String) = context.dataStore.edit { it[KEY_UI_LANG] = lang }
    suspend fun setCommunitySetupDone(v: Boolean) = context.dataStore.edit { it[KEY_COMMUNITY_SETUP_DONE] = v }
    suspend fun setHasQuestionnaire(v: Boolean) = context.dataStore.edit { it[booleanPreferencesKey("has_questionnaire")] = v }
    suspend fun setIncognitoMode(v: Boolean) = context.dataStore.edit { it[KEY_INCOGNITO] = v }
    suspend fun setDpdpConsent(accepted: Boolean) = context.dataStore.edit {
        it[KEY_DPDP_CONSENT] = accepted
        it[KEY_DPDP_CONSENT_AT] = System.currentTimeMillis()
    }

    /** Record a heartbeat; call from MatchRoot on each resume. */
    suspend fun touchActivity() = context.dataStore.edit { it[KEY_LAST_ACTIVE] = System.currentTimeMillis() }

    /** Returns true if the user hasn't interacted in 30+ days. */
    suspend fun isSessionExpired(): Boolean {
        val prefs = context.dataStore.data.map { it[KEY_LAST_ACTIVE] }.first()
        val last = prefs ?: return false  // no timestamp → fresh install, not expired
        return System.currentTimeMillis() - last > SESSION_EXPIRY_MS
    }
}
