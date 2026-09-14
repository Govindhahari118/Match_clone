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
import androidx.compose.ui.graphics.Color
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
import com.match.app.data.session.SessionStore
import com.match.app.core.network.ConnectivityObserver
import com.match.app.domain.model.ReligionExperiencePreference
import com.match.app.ui.auth.SignInScreen
import com.match.app.ui.auth.SignUpScreen
import com.match.app.ui.i18n.LocalI18n
import com.match.app.ui.i18n.rememberI18nCatalog
import com.match.app.ui.main.MainShell
import com.match.app.ui.onboarding.OnboardingScreen
import com.match.app.ui.theme.AppPalette
import com.match.app.ui.theme.MatchTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
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
    private val connectivity: ConnectivityObserver
) : ViewModel() {
    val userId    = session.userId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val onboarded = session.onboarded.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val darkMode  = session.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val palette   = session.paletteKey.stateIn(viewModelScope, SharingStarted.Eagerly, "VIVAH")
    val religionExperience = session.religionExperience.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ReligionExperiencePreference()
    )
    val uiLanguage = session.uiLanguage.stateIn(viewModelScope, SharingStarted.Eagerly, "en")
    val isOnline   = connectivity.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun touchActivity() = viewModelScope.launch { session.touchActivity() }

    fun checkSessionExpiry() = viewModelScope.launch {
        if (session.isSessionExpired()) session.clear()
    }
}

/** Root decision: Onboarding → Auth → Main. Only one is composed at a time. */
@Composable
fun MatchRoot(vm: RootViewModel = hiltViewModel()) {
    val darkMode by vm.darkMode.collectAsState()
    val paletteKey by vm.palette.collectAsState()
    val religionExperience by vm.religionExperience.collectAsState()
    val uiLanguage by vm.uiLanguage.collectAsState()
    val catalog = rememberI18nCatalog(uiLanguage)

    LaunchedEffect(Unit) {
        vm.checkSessionExpiry()
        vm.touchActivity()
    }

    val selectedReligion = religionExperience.selected.singleOrNull()
    val effectivePalette = if (religionExperience.religionThemeEnabled && selectedReligion != null) {
        AppPalette.forReligion(selectedReligion)
    } else {
        AppPalette.fromKey(paletteKey)
    }

    val layoutDir = if (uiLanguage in setOf("ar", "ur")) LayoutDirection.Rtl else LayoutDirection.Ltr
    MatchTheme(darkMode = darkMode, palette = effectivePalette) {
        CompositionLocalProvider(
            LocalI18n provides catalog,
            androidx.compose.ui.platform.LocalLayoutDirection provides layoutDir
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val userId    by vm.userId.collectAsState()
                val onboarded by vm.onboarded.collectAsState()
                val isOnline  by vm.isOnline.collectAsState()
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
                            !onboarded -> OnboardingScreen(onDone = {})
                            !loggedIn  -> AuthNav()
                            else       -> MainShell()
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
