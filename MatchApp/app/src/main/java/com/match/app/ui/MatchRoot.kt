package com.match.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import com.match.app.data.session.SessionStore
import com.match.app.ui.auth.SignInScreen
import com.match.app.ui.auth.SignUpScreen
import com.match.app.ui.i18n.LocalI18n
import com.match.app.ui.i18n.rememberI18nCatalog
import com.match.app.ui.main.MainShell
import com.match.app.ui.onboarding.OnboardingScreen
import com.match.app.ui.onboarding.ProfileWizardScreen
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
    const val ONBOARDING  = "onboarding"
    const val MAIN_GRAPH  = "main"
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val session: SessionStore,
    private val connectivity: ConnectivityObserver,
    userDao: UserDao
) : ViewModel() {
    val userId = session.userId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val onboarded = session.onboarded.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val darkMode = session.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val palette = session.paletteKey.stateIn(viewModelScope, SharingStarted.Eagerly, "VIVAH")
    val religionThemeEnabled = session.religionExperience
        .map { it.religionThemeEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
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

    fun touchActivity() = viewModelScope.launch { session.touchActivity() }

    fun checkSessionExpiry() = viewModelScope.launch {
        if (session.isSessionExpired()) session.clear()
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
            heightCm in 90..250
}

/** Root decision: app intro → auth → required account profile → main. Only one is composed at a time. */
@Composable
fun MatchRoot(vm: RootViewModel = hiltViewModel()) {
    val darkMode by vm.darkMode.collectAsState()
    val paletteKey by vm.palette.collectAsState()
    val religionThemeEnabled by vm.religionThemeEnabled.collectAsState()
    val currentUser by vm.currentUser.collectAsState()
    val uiLanguage by vm.uiLanguage.collectAsState()
    val catalog = rememberI18nCatalog(uiLanguage)

    LaunchedEffect(Unit) {
        vm.checkSessionExpiry()
        vm.touchActivity()
    }

    val effectivePalette = AppearanceThemeResolver.resolve(
        automaticReligionTheme = religionThemeEnabled,
        manualPaletteKey = paletteKey,
        profileReligion = currentUser?.religion
    )

    val layoutDir = if (uiLanguage in setOf("ar", "ur")) LayoutDirection.Rtl else LayoutDirection.Ltr
    MatchTheme(darkMode = darkMode, palette = effectivePalette) {
        CompositionLocalProvider(
            LocalI18n provides catalog,
            androidx.compose.ui.platform.LocalLayoutDirection provides layoutDir
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val userId by vm.userId.collectAsState()
                val onboarded by vm.onboarded.collectAsState()
                val isOnline by vm.isOnline.collectAsState()
                val sessionReady by vm.sessionReady.collectAsState()
                val profileSetupComplete by vm.profileSetupComplete.collectAsState()
                val loggedIn = userId != null

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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.CloudOff, null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
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
            SignInScreen(onGoSignUp = { nav.navigate(Routes.SIGN_UP) })
        }
        composable(Routes.SIGN_UP) {
            SignUpScreen(onBack = { nav.popBackStack() })
        }
    }
}
