package com.match.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.match.app.core.network.ConnectivityObserver
import com.match.app.data.local.dao.UserDao
import com.match.app.data.local.entity.UserEntity
import com.match.app.data.repo.AppearancePreferenceRepository
import com.match.app.data.repo.AuthRepository
import com.match.app.data.repo.PartnerPreferenceRepository
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.AppearancePreference
import com.match.app.domain.model.ThemePreference
import com.match.app.ui.auth.SignInScreen
import com.match.app.ui.auth.SignUpScreen
import com.match.app.ui.auth.PhoneAuthScreen
import com.match.app.ui.i18n.LocalI18n
import com.match.app.ui.i18n.rememberI18nCatalog
import com.match.app.ui.main.MainShell
import com.match.app.ui.onboarding.OnboardingScreen
import com.match.app.ui.onboarding.ProfileWizardScreen
import com.match.app.ui.preferences.PartnerPreferencesScreen
import com.match.app.ui.theme.AppPalette
import com.match.app.ui.theme.AppearanceThemeResolver
import com.match.app.ui.theme.MatchTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

object Routes {
    const val AUTH_GRAPH  = "auth"
    const val SIGN_IN     = "sign_in"
    const val SIGN_UP     = "sign_up"
    const val PHONE_AUTH  = "phone_auth"
    const val ONBOARDING  = "onboarding"
    const val MAIN_GRAPH  = "main"
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val session: SessionStore,
    private val connectivity: ConnectivityObserver,
    private val authRepository: AuthRepository,
    private val partnerPreferenceRepository: PartnerPreferenceRepository,
    userDao: UserDao,
    appearancePreferenceRepository: AppearancePreferenceRepository
) : ViewModel() {
    val userId = session.userId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val onboarded = session.onboarded.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val appearance = appearancePreferenceRepository.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppearancePreference())
    val uiLanguage = session.uiLanguage.stateIn(viewModelScope, SharingStarted.Eagerly, "en")
    val isOnline = connectivity.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Prevent auth/main route flashes while DataStore restores the existing signed-in session. */
    val sessionReady = combine(session.onboarded, session.userId) { _, _ -> true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Canonical signed-in profile. Appearance may read religion from here, never from discovery filters. */
    val currentUser = session.userId
        .flatMapLatest { id -> if (id == null) flowOf(null) else userDao.observeById(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Profile completion is account-scoped and derived from the signed-in user's actual persisted
     * profile. It intentionally does not use the old device-global communitySetupDone preference,
     * which could be inherited by a different account on the same phone.
     */
    val profileSetupComplete = currentUser
        .map { it?.isRequiredProfileComplete() == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    enum class PartnerPreferenceGate {
        UNKNOWN,
        CHECKING,
        REQUIRED,
        COMPLETE,
        UNAVAILABLE
    }

    private val _partnerPreferenceGate =
        kotlinx.coroutines.flow.MutableStateFlow(PartnerPreferenceGate.UNKNOWN)
    val partnerPreferenceGate = _partnerPreferenceGate
        .stateIn(viewModelScope, SharingStarted.Eagerly, PartnerPreferenceGate.UNKNOWN)

    private var partnerPreferenceCheckedUid: Long? = null

    fun refreshPartnerPreferenceGate(force: Boolean = false) = viewModelScope.launch {
        val id = userId.value ?: run {
            partnerPreferenceCheckedUid = null
            _partnerPreferenceGate.value = PartnerPreferenceGate.UNKNOWN
            return@launch
        }
        if (!profileSetupComplete.value) {
            _partnerPreferenceGate.value = PartnerPreferenceGate.UNKNOWN
            return@launch
        }
        if (!force && partnerPreferenceCheckedUid == id &&
            _partnerPreferenceGate.value in setOf(
                PartnerPreferenceGate.REQUIRED,
                PartnerPreferenceGate.COMPLETE
            )
        ) return@launch

        _partnerPreferenceGate.value = PartnerPreferenceGate.CHECKING
        runCatching { partnerPreferenceRepository.load() }
            .onSuccess { preferences ->
                partnerPreferenceCheckedUid = id
                _partnerPreferenceGate.value = if (preferences.configured) {
                    PartnerPreferenceGate.COMPLETE
                } else {
                    PartnerPreferenceGate.REQUIRED
                }
            }
            .onFailure {
                // Do not brick startup when Firebase/Functions is unavailable. Main remains usable
                // offline; the check retries when connectivity returns.
                _partnerPreferenceGate.value = PartnerPreferenceGate.UNAVAILABLE
            }
    }

    fun markPartnerPreferencesConfigured() {
        partnerPreferenceCheckedUid = userId.value
        _partnerPreferenceGate.value = PartnerPreferenceGate.COMPLETE
    }

    fun touchActivity() = viewModelScope.launch { session.touchActivity() }

    fun checkSessionExpiry() = viewModelScope.launch {
        if (session.isSessionExpired()) authRepository.signOut()
    }

    fun validateRemoteSession() = viewModelScope.launch {
        authRepository.validateRemoteSession()
    }

    private fun UserEntity.isRequiredProfileComplete(): Boolean =
        username.isNotBlank() &&
            displayName.trim().length >= 2 &&
            dateOfBirth.isNotBlank() &&
            state.isNotBlank() &&
            city.trim().length >= 2 &&
            motherTongue.isNotBlank() &&
            religion.isNotBlank() &&
            education.isNotBlank() &&
            profession.trim().length >= 2 &&
            maritalStatus.isNotBlank() &&
            heightCm in 90..250
}

/** Root decision: app intro → auth → required account profile → main. Only one is composed at a time. */
@Composable
fun MatchRoot(vm: RootViewModel = hiltViewModel()) {
    val appearance by vm.appearance.collectAsState()
    val currentUser by vm.currentUser.collectAsState()
    val userId by vm.userId.collectAsState()
    val onboarded by vm.onboarded.collectAsState()
    val sessionReady by vm.sessionReady.collectAsState()
    val profileSetupComplete by vm.profileSetupComplete.collectAsState()
    val partnerPreferenceGate by vm.partnerPreferenceGate.collectAsState()
    val loggedIn = userId != null
    val uiLanguage by vm.uiLanguage.collectAsState()
    val catalog = rememberI18nCatalog(uiLanguage)
    val systemDark = isSystemInDarkTheme()
    val isOnline by vm.isOnline.collectAsState()

    LaunchedEffect(Unit) {
        vm.checkSessionExpiry()
        vm.touchActivity()
    }
    LaunchedEffect(isOnline) {
        if (isOnline) vm.validateRemoteSession()
    }
    LaunchedEffect(loggedIn, profileSetupComplete, isOnline) {
        if (loggedIn && profileSetupComplete && isOnline) {
            vm.refreshPartnerPreferenceGate()
        }
    }

    val effectivePalette = when {
        // Intro/onboarding and incomplete profile setup stay visually stable and universal.
        !sessionReady || !onboarded || (loggedIn && !profileSetupComplete) -> AppPalette.VIVAH

        // Signed-out auth may honor only an explicit stored manual appearance. Automatic cannot
        // derive identity without an authenticated confirmed profile.
        !loggedIn -> if (appearance.themePreference == ThemePreference.MANUAL) {
            AppearanceThemeResolver.resolve(
                themePreference = ThemePreference.MANUAL,
                manualPaletteKey = appearance.manualThemeKey,
                profileReligion = null
            )
        } else {
            AppPalette.VIVAH
        }

        else -> AppearanceThemeResolver.resolve(
            themePreference = appearance.themePreference,
            manualPaletteKey = appearance.manualThemeKey,
            profileReligion = currentUser?.religion
        )
    }
    val darkMode = AppearanceThemeResolver.resolveDarkMode(
        displayMode = appearance.displayMode,
        systemDark = systemDark
    )

    val layoutDir = if (uiLanguage in setOf("ar", "ur")) LayoutDirection.Rtl else LayoutDirection.Ltr
    MatchTheme(darkMode = darkMode, palette = effectivePalette) {
        CompositionLocalProvider(
            LocalI18n provides catalog,
            androidx.compose.ui.platform.LocalLayoutDirection provides layoutDir
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = !isOnline,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(
                                    horizontal = com.match.app.ui.theme.MatreeDesign.spacing.md,
                                    vertical = com.match.app.ui.theme.MatreeDesign.spacing.xs
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.CloudOff, null,
                                modifier = Modifier.size(com.match.app.ui.theme.MatreeDesign.sizes.iconSmall),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(com.match.app.ui.theme.MatreeDesign.spacing.xs))
                            Text(
                                "You're offline — some features may be limited",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Box(Modifier.weight(1f)) {
                        when {
                            !sessionReady -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                            !onboarded -> OnboardingScreen(onDone = {})
                            !loggedIn -> AuthNav()
                            !profileSetupComplete -> ProfileWizardScreen(onComplete = {})
                            partnerPreferenceGate == RootViewModel.PartnerPreferenceGate.REQUIRED ->
                                PartnerPreferencesScreen(
                                    onBack = null,
                                    onSaved = vm::markPartnerPreferencesConfigured
                                )
                            partnerPreferenceGate == RootViewModel.PartnerPreferenceGate.CHECKING ->
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            else -> MainShell()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthNav() {
    val nav: NavHostController = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SIGN_IN) {
        composable(Routes.SIGN_IN) {
            SignInScreen(
                onGoSignUp = { nav.navigate(Routes.SIGN_UP) },
                onGoPhone = { nav.navigate(Routes.PHONE_AUTH) }
            )
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.PHONE_AUTH) {
            PhoneAuthScreen(onBack = { nav.popBackStack() })
        }
    }
}
