package com.match.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.match.app.domain.model.AppearancePreference
import com.match.app.domain.model.DisplayMode
import com.match.app.domain.model.MatchFilter
import com.match.app.domain.model.MatchMode
import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.ReligionExperiencePreference
import com.match.app.domain.model.ThemePreference
import com.match.app.ui.i18n.SupportedUiLocales
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
    private val KEY_SUB_CASTE = stringPreferencesKey("filter_sub_caste")
    private val KEY_MIN_SCORE = floatPreferencesKey("filter_min_score")
    private val KEY_RELIGION  = stringPreferencesKey("filter_religion")
    private val KEY_TONGUE    = stringPreferencesKey("filter_tongue")
    private val KEY_MARITAL   = stringPreferencesKey("filter_marital")
    private val KEY_VERIFIED  = booleanPreferencesKey("filter_verified")
    private val KEY_DARK      = booleanPreferencesKey("dark_mode")
    private val KEY_PALETTE   = stringPreferencesKey("palette_key")
    private val KEY_THEME_PREFERENCE = stringPreferencesKey("appearance_theme_preference")
    private val KEY_MANUAL_THEME = stringPreferencesKey("appearance_manual_theme")
    private val KEY_DISPLAY_MODE = stringPreferencesKey("appearance_display_mode")
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
    private val KEY_SMOKING = stringPreferencesKey("filter_smoking")
    private val KEY_DRINKING = stringPreferencesKey("filter_drinking")
    private val KEY_FAMILY_TYPE = stringPreferencesKey("filter_family_type")
    private val KEY_FAMILY_STATUS = stringPreferencesKey("filter_family_status")
    private val KEY_PHYSICAL_STATUS = stringPreferencesKey("filter_physical_status")
    private val KEY_CHILDREN_FILTER = stringPreferencesKey("filter_children_extended")
    private val KEY_CITIZENSHIP = stringPreferencesKey("filter_citizenship")
    private val KEY_NRI_STATUS = stringPreferencesKey("filter_nri_status")
    private val KEY_EDUCATION_FIELD = stringPreferencesKey("filter_education_field")
    private val KEY_OCCUPATION_CATEGORY = stringPreferencesKey("filter_occupation_category")
    private val KEY_EMPLOYER_TYPE = stringPreferencesKey("filter_employer_type")
    private val KEY_NAKSHATRA = stringPreferencesKey("filter_nakshatra")
    private val KEY_RASI = stringPreferencesKey("filter_rasi")
    private val KEY_MANGLIK = stringPreferencesKey("filter_manglik")
    private val KEY_HOBBIES = stringPreferencesKey("filter_hobbies")
    private val KEY_PHOTO_ONLY = booleanPreferencesKey("filter_photo_only")
    private val KEY_VERIFIED_LEVEL = intPreferencesKey("filter_verified_level")
    private val KEY_PREMIUM_ONLY = booleanPreferencesKey("filter_premium_only")
    private val KEY_LAST_ACTIVE_DAYS = intPreferencesKey("filter_last_active_days")
    private val KEY_MIN_PORUTHAM = intPreferencesKey("filter_min_porutham")
    private val KEY_HAS_HOROSCOPE = stringPreferencesKey("filter_has_horoscope")
    private val KEY_UI_LANG    = stringPreferencesKey("ui_language")
    private val KEY_COMMUNITY_SETUP_DONE = booleanPreferencesKey("community_setup_done")
    private val KEY_LAST_ACTIVE = longPreferencesKey("last_active_at")
    private val KEY_INCOGNITO   = booleanPreferencesKey("incognito_mode")
    private val KEY_SUB_PLAN   = stringPreferencesKey("subscription_plan")
    private val KEY_DPDP_CONSENT = booleanPreferencesKey("dpdp_consent_given")
    private val KEY_DPDP_CONSENT_AT = longPreferencesKey("dpdp_consent_at")
    private val KEY_WIZARD_STEP = intPreferencesKey("profile_wizard_step")

    // Discovery experience only. These values never redefine the member's declared religion.
    private val KEY_RELIGION_LENSES = stringPreferencesKey("religion_lenses")
    private val KEY_RELIGION_LOCKED = booleanPreferencesKey("religion_lens_locked")
    // Legacy appearance key retained only for migration/compatibility with older installs.
    private val KEY_RELIGION_THEME = booleanPreferencesKey("religion_theme_enabled")

    companion object {
        private const val SESSION_EXPIRY_MS = 30L * 24 * 60 * 60 * 1000
    }

    private fun normalizeUiLanguage(value: String?): String =
        SupportedUiLocales.normalize(value)

    val userId: Flow<Long?> = context.dataStore.data.map { it[KEY_USER_ID]?.takeIf { id -> id > 0 } }
    val firebaseUid: Flow<String?> = context.dataStore.data.map { it[KEY_FIREBASE_UID]?.takeIf { uid -> uid.isNotBlank() } }
    val mode: Flow<MatchMode> = context.dataStore.data.map {
        when (it[KEY_MODE] ?: 2L) { 0L -> MatchMode.QUESTIONNAIRE; 1L -> MatchMode.ASTROLOGY; else -> MatchMode.ADVANCED }
    }
    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARD] ?: false }
    val filter: Flow<MatchFilter> = context.dataStore.data.map {
        MatchFilter(
            ageMin = it[KEY_AGE_MIN] ?: 18,
            ageMax = it[KEY_AGE_MAX] ?: 70,
            city = it[KEY_CITY] ?: "",
            state = it[KEY_STATE] ?: "",
            caste = it[KEY_CASTE] ?: "",
            subCaste = it[KEY_SUB_CASTE] ?: "",
            minScore = it[KEY_MIN_SCORE] ?: 0f,
            religion = it[KEY_RELIGION] ?: "",
            motherTongue = it[KEY_TONGUE] ?: "",
            maritalStatus = it[KEY_MARITAL] ?: "",
            verifiedOnly = it[KEY_VERIFIED] ?: false,
            incomeMin = it[KEY_INCOME_MIN] ?: "",
            incomeMax = it[KEY_INCOME_MAX] ?: "",
            educationLevel = it[KEY_EDUCATION] ?: "",
            diet = it[KEY_DIET] ?: "",
            residentialStatus = it[KEY_RESIDENTIAL] ?: "",
            hasChildren = it[KEY_CHILDREN] ?: "",
            keyword = it[KEY_KEYWORD] ?: "",
            gothra = it[KEY_GOTHRA] ?: "",
            nativeState = it[KEY_NATIVE_STATE] ?: "",
            countryOfResidence = it[KEY_COUNTRY] ?: "",
            nriOnly = it[KEY_NRI_ONLY] ?: false,
            willingToRelocate = it[KEY_RELOCATE] ?: false,
            recentlyJoinedDays = it[KEY_RECENT_DAYS] ?: 0,
            smoking = it[KEY_SMOKING] ?: "",
            drinking = it[KEY_DRINKING] ?: "",
            familyType = it[KEY_FAMILY_TYPE] ?: "",
            familyStatus = it[KEY_FAMILY_STATUS] ?: "",
            physicalStatus = it[KEY_PHYSICAL_STATUS] ?: "",
            hasChildrenFilter = it[KEY_CHILDREN_FILTER] ?: "",
            citizenship = it[KEY_CITIZENSHIP] ?: "",
            nriStatus = it[KEY_NRI_STATUS] ?: "",
            educationField = it[KEY_EDUCATION_FIELD] ?: "",
            occupationCategory = it[KEY_OCCUPATION_CATEGORY] ?: "",
            employerType = it[KEY_EMPLOYER_TYPE] ?: "",
            nakshatra = it[KEY_NAKSHATRA] ?: "",
            rasi = it[KEY_RASI] ?: "",
            manglik = it[KEY_MANGLIK] ?: "",
            hobbies = it[KEY_HOBBIES] ?: "",
            withPhotoOnly = it[KEY_PHOTO_ONLY] ?: true,
            verifiedLevel = it[KEY_VERIFIED_LEVEL] ?: 0,
            premiumOnly = it[KEY_PREMIUM_ONLY] ?: false,
            lastActiveWithinDays = it[KEY_LAST_ACTIVE_DAYS] ?: 0,
            minPoruthamScore = it[KEY_MIN_PORUTHAM] ?: 0,
            hasHoroscope = it[KEY_HAS_HOROSCOPE] ?: ""
        )
    }

    /** Presentation state is independent from profile religion and discovery preferences. */
    val appearancePreference: Flow<AppearancePreference> = context.dataStore.data.map { prefs ->
        val legacyPalette = prefs[KEY_PALETTE]
        val themePreference = prefs[KEY_THEME_PREFERENCE]?.let { ThemePreference.fromStorage(it) } ?: when {
            prefs[KEY_RELIGION_THEME] == true -> ThemePreference.AUTOMATIC
            prefs[KEY_RELIGION_THEME] == false -> if ((legacyPalette ?: "VIVAH") == "VIVAH") ThemePreference.NEUTRAL else ThemePreference.MANUAL
            legacyPalette != null -> if (legacyPalette == "VIVAH") ThemePreference.NEUTRAL else ThemePreference.MANUAL
            else -> ThemePreference.AUTOMATIC
        }
        val displayMode = prefs[KEY_DISPLAY_MODE]?.let { DisplayMode.fromStorage(it) } ?: when (prefs[KEY_DARK]) {
            true -> DisplayMode.DARK
            false -> DisplayMode.LIGHT
            null -> DisplayMode.SYSTEM
        }
        AppearancePreference(
            themePreference = themePreference,
            manualThemeKey = prefs[KEY_MANUAL_THEME] ?: legacyPalette ?: "VIVAH",
            displayMode = displayMode
        )
    }

    // Compatibility flows retained for older call sites while migration is completed.
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK] ?: false }
    val paletteKey: Flow<String> = context.dataStore.data.map { it[KEY_PALETTE] ?: "VIVAH" }
    val religionExperience: Flow<ReligionExperiencePreference> = context.dataStore.data.map { prefs ->
        val selected = prefs[KEY_RELIGION_LENSES]
            .orEmpty()
            .split(',')
            .mapNotNull { ReligionCategory.fromStorageKey(it.trim()) }
            .toSet()
        ReligionExperiencePreference(
            selected = selected,
            locked = prefs[KEY_RELIGION_LOCKED] ?: false,
            religionThemeEnabled = prefs[KEY_RELIGION_THEME] ?: false
        )
    }
    val apiBaseUrl: Flow<String> = context.dataStore.data.map { it[KEY_API_BASE] ?: "" }
    val biometricLock: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC] ?: false }
    val subscriptionPlan: Flow<String> = context.dataStore.data.map { it[KEY_SUB_PLAN] ?: "FREE" }
    val uiLanguage: Flow<String> = context.dataStore.data.map { prefs -> normalizeUiLanguage(prefs[KEY_UI_LANG]) }
    val communitySetupDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_COMMUNITY_SETUP_DONE] ?: false }
    val hasQuestionnaire: Flow<Boolean> = context.dataStore.data.map { it[booleanPreferencesKey("has_questionnaire")] ?: false }
    val incognitoMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_INCOGNITO] ?: false }
    val dpdpConsentGiven: Flow<Boolean> = context.dataStore.data.map { it[KEY_DPDP_CONSENT] ?: false }
    val profileWizardStep: Flow<Int> = context.dataStore.data.map { (it[KEY_WIZARD_STEP] ?: 0).coerceIn(0, 7) }

    suspend fun setUser(id: Long) = context.dataStore.edit { it[KEY_USER_ID] = id }
    suspend fun setFirebaseUid(uid: String) = context.dataStore.edit { it[KEY_FIREBASE_UID] = uid }
    suspend fun setSubscriptionPlan(plan: String) = context.dataStore.edit { it[KEY_SUB_PLAN] = plan }
    /**
     * Clears all account-scoped local state during sign-out/account switch.
     *
     * Only device-scoped presentation/diagnostic preferences are preserved. Filters, onboarding,
     * subscription state, religion lenses/theme choice, consent, incognito and account progress
     * must never bleed from one signed-in member to another.
     */
    suspend fun clear() = context.dataStore.edit { prefs ->
        val uiLanguage = prefs[KEY_UI_LANG]
        val displayMode = prefs[KEY_DISPLAY_MODE]
        val legacyDarkMode = prefs[KEY_DARK]
        val biometricLock = prefs[KEY_BIOMETRIC]
        val apiBaseUrl = prefs[KEY_API_BASE]

        prefs.clear()

        uiLanguage?.let { prefs[KEY_UI_LANG] = it }
        displayMode?.let { prefs[KEY_DISPLAY_MODE] = it }
        legacyDarkMode?.let { prefs[KEY_DARK] = it }
        biometricLock?.let { prefs[KEY_BIOMETRIC] = it }
        apiBaseUrl?.let { prefs[KEY_API_BASE] = it }
    }
    suspend fun setMode(m: MatchMode) = context.dataStore.edit {
        it[KEY_MODE] = when (m) { MatchMode.QUESTIONNAIRE -> 0L; MatchMode.ASTROLOGY -> 1L; MatchMode.ADVANCED -> 2L }
    }
    suspend fun setOnboarded(v: Boolean) = context.dataStore.edit { it[KEY_ONBOARD] = v }
    suspend fun setFilter(f: MatchFilter) = context.dataStore.edit {
        it[KEY_AGE_MIN] = f.ageMin
        it[KEY_AGE_MAX] = f.ageMax
        it[KEY_CITY] = f.city
        it[KEY_STATE] = f.state
        it[KEY_CASTE] = f.caste
        it[KEY_SUB_CASTE] = f.subCaste
        it[KEY_MIN_SCORE] = f.minScore
        it[KEY_RELIGION] = f.religion
        it[KEY_TONGUE] = f.motherTongue
        it[KEY_MARITAL] = f.maritalStatus
        it[KEY_VERIFIED] = f.verifiedOnly
        it[KEY_INCOME_MIN] = f.incomeMin
        it[KEY_INCOME_MAX] = f.incomeMax
        it[KEY_EDUCATION] = f.educationLevel
        it[KEY_DIET] = f.diet
        it[KEY_RESIDENTIAL] = f.residentialStatus
        it[KEY_CHILDREN] = f.hasChildren
        it[KEY_KEYWORD] = f.keyword
        it[KEY_GOTHRA] = f.gothra
        it[KEY_NATIVE_STATE] = f.nativeState
        it[KEY_COUNTRY] = f.countryOfResidence
        it[KEY_NRI_ONLY] = f.nriOnly
        it[KEY_RELOCATE] = f.willingToRelocate
        it[KEY_RECENT_DAYS] = f.recentlyJoinedDays
        it[KEY_SMOKING] = f.smoking
        it[KEY_DRINKING] = f.drinking
        it[KEY_FAMILY_TYPE] = f.familyType
        it[KEY_FAMILY_STATUS] = f.familyStatus
        it[KEY_PHYSICAL_STATUS] = f.physicalStatus
        it[KEY_CHILDREN_FILTER] = f.hasChildrenFilter
        it[KEY_CITIZENSHIP] = f.citizenship
        it[KEY_NRI_STATUS] = f.nriStatus
        it[KEY_EDUCATION_FIELD] = f.educationField
        it[KEY_OCCUPATION_CATEGORY] = f.occupationCategory
        it[KEY_EMPLOYER_TYPE] = f.employerType
        it[KEY_NAKSHATRA] = f.nakshatra
        it[KEY_RASI] = f.rasi
        it[KEY_MANGLIK] = f.manglik
        it[KEY_HOBBIES] = f.hobbies
        it[KEY_PHOTO_ONLY] = f.withPhotoOnly
        it[KEY_VERIFIED_LEVEL] = f.verifiedLevel
        it[KEY_PREMIUM_ONLY] = f.premiumOnly
        it[KEY_LAST_ACTIVE_DAYS] = f.lastActiveWithinDays
        it[KEY_MIN_PORUTHAM] = f.minPoruthamScore
        it[KEY_HAS_HOROSCOPE] = f.hasHoroscope
    }

    suspend fun setReligionExperience(value: ReligionExperiencePreference) = context.dataStore.edit { prefs ->
        prefs[KEY_RELIGION_LENSES] = value.selected.joinToString(",") { it.storageKey }
        prefs[KEY_RELIGION_LOCKED] = value.locked
    }
    suspend fun setReligionLenses(values: Set<ReligionCategory>) = context.dataStore.edit { prefs ->
        prefs[KEY_RELIGION_LENSES] = values.joinToString(",") { it.storageKey }
    }
    suspend fun setReligionLocked(value: Boolean) = context.dataStore.edit { it[KEY_RELIGION_LOCKED] = value }

    suspend fun setThemePreference(value: ThemePreference, manualThemeKey: String? = null) = context.dataStore.edit { prefs ->
        prefs[KEY_THEME_PREFERENCE] = value.name
        when (value) {
            ThemePreference.AUTOMATIC -> prefs[KEY_RELIGION_THEME] = true
            ThemePreference.NEUTRAL -> {
                prefs[KEY_RELIGION_THEME] = false
                prefs[KEY_PALETTE] = "VIVAH"
            }
            ThemePreference.MANUAL -> {
                val selected = manualThemeKey ?: prefs[KEY_MANUAL_THEME] ?: prefs[KEY_PALETTE] ?: "VIVAH"
                prefs[KEY_RELIGION_THEME] = false
                prefs[KEY_MANUAL_THEME] = selected
                prefs[KEY_PALETTE] = selected
            }
        }
    }

    suspend fun setDisplayMode(value: DisplayMode) = context.dataStore.edit { prefs ->
        prefs[KEY_DISPLAY_MODE] = value.name
        when (value) {
            DisplayMode.DARK -> prefs[KEY_DARK] = true
            DisplayMode.LIGHT -> prefs[KEY_DARK] = false
            DisplayMode.SYSTEM -> prefs.remove(KEY_DARK)
        }
    }

    // Legacy setters retain coherent new-model state for any remaining old call sites.
    suspend fun setReligionThemeEnabled(value: Boolean) = context.dataStore.edit { prefs ->
        prefs[KEY_RELIGION_THEME] = value
        if (value) prefs[KEY_THEME_PREFERENCE] = ThemePreference.AUTOMATIC.name
        else if (ThemePreference.fromStorage(prefs[KEY_THEME_PREFERENCE]) == ThemePreference.AUTOMATIC) {
            prefs[KEY_THEME_PREFERENCE] = ThemePreference.NEUTRAL.name
            prefs[KEY_PALETTE] = "VIVAH"
        }
    }
    suspend fun setDarkMode(v: Boolean) = context.dataStore.edit { prefs ->
        prefs[KEY_DARK] = v
        prefs[KEY_DISPLAY_MODE] = if (v) DisplayMode.DARK.name else DisplayMode.LIGHT.name
    }
    suspend fun setPalette(key: String) = context.dataStore.edit { prefs ->
        prefs[KEY_PALETTE] = key
        prefs[KEY_MANUAL_THEME] = key
    }
    suspend fun setApiBaseUrl(url: String) = context.dataStore.edit { it[KEY_API_BASE] = url }
    suspend fun setBiometricLock(v: Boolean) = context.dataStore.edit { it[KEY_BIOMETRIC] = v }
    suspend fun setUiLanguage(lang: String) = context.dataStore.edit { it[KEY_UI_LANG] = normalizeUiLanguage(lang) }
    suspend fun setCommunitySetupDone(v: Boolean) = context.dataStore.edit { it[KEY_COMMUNITY_SETUP_DONE] = v }
    suspend fun setHasQuestionnaire(v: Boolean) = context.dataStore.edit { it[booleanPreferencesKey("has_questionnaire")] = v }
    suspend fun setIncognitoMode(v: Boolean) = context.dataStore.edit { it[KEY_INCOGNITO] = v }
    suspend fun setProfileWizardStep(step: Int) = context.dataStore.edit { it[KEY_WIZARD_STEP] = step.coerceIn(0, 7) }
    suspend fun setDpdpConsent(accepted: Boolean) = context.dataStore.edit {
        it[KEY_DPDP_CONSENT] = accepted
        it[KEY_DPDP_CONSENT_AT] = System.currentTimeMillis()
    }

    suspend fun touchActivity() = context.dataStore.edit { it[KEY_LAST_ACTIVE] = System.currentTimeMillis() }

    suspend fun isSessionExpired(): Boolean {
        val prefs = context.dataStore.data.map { it[KEY_LAST_ACTIVE] }.first()
        val last = prefs ?: return false
        return System.currentTimeMillis() - last > SESSION_EXPIRY_MS
    }
}
