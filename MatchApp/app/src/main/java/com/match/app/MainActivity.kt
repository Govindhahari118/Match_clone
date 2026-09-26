package com.match.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.match.app.core.config.RemoteConfigManager
import com.match.app.core.update.InAppUpdateManager
import com.match.app.data.billing.PlayBillingManager
import com.match.app.data.repo.PresenceRepository
import com.match.app.data.session.SessionStore
import com.match.app.navigation.DeepLinkRouteResolver
import com.match.app.ui.MatchRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var session: SessionStore
    @Inject lateinit var playBilling: PlayBillingManager
    @Inject lateinit var inAppUpdateManager: InAppUpdateManager
    @Inject lateinit var remoteConfig: RemoteConfigManager
    @Inject lateinit var presenceRepository: PresenceRepository

    private var isPermissionPromptInFlight = false
    private var isBiometricPromptShowing = false
    private var presenceJob: Job? = null
    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isPermissionPromptInFlight = false }

    /** Compose-observable so a deep link received by a warm singleTask Activity is not dropped. */
    var pendingDeepLink: String? by mutableStateOf(null)
        private set

    fun consumeDeepLink(): String? = pendingDeepLink.also { pendingDeepLink = null }

    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { !isReady }
        lifecycleScope.launch {
            try { kotlinx.coroutines.withTimeout(500) { session.userId.first() } } catch (_: Exception) {}
            isReady = true
        }
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)
        setContent { MatchRoot() }
    }

    override fun onResume() {
        super.onResume()
        if (isPermissionPromptInFlight || isBiometricPromptShowing) return
        lifecycleScope.launch { session.touchActivity() }
        startPresenceHeartbeat()
        lifecycleScope.launch { if (session.biometricLock.first()) showBiometricPrompt() }
        playBilling.connect()
        inAppUpdateManager.checkForUpdate(activity = this, forceUpdateVersionCode = remoteConfig.forceUpdateVersionCode)
    }

    override fun onPause() {
        presenceJob?.cancel()
        presenceJob = null
        super.onPause()
    }

    private fun startPresenceHeartbeat() {
        if (presenceJob?.isActive == true) return
        presenceJob = lifecycleScope.launch {
            while (isActive) {
                presenceRepository.heartbeat()
                delay(PRESENCE_HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun showBiometricPrompt() {
        if (isPermissionPromptInFlight || isBiometricPromptShowing) return
        if (BiometricManager.from(this).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) return
        isBiometricPromptShowing = true
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                isBiometricPromptShowing = false
                if (code == BiometricPrompt.ERROR_USER_CANCELED || code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) finishAffinity()
            }
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { isBiometricPromptShowing = false }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MatrimonyConnect")
            .setSubtitle("Verify your identity to continue")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri == null) {
            // These extras are generated by our immutable notification PendingIntents. Route
            // arguments still use typed positive local ids, while the destination repositories
            // re-authorize profile/chat data before exposing remote content.
            val type = intent?.getStringExtra("notif_type")
            val fromUserId = intent?.getLongExtra("from_user_id", -1L).takeIf { it != null && it > 0 }
            val chatPeerId = intent?.getLongExtra("peer_id", -1L).takeIf { it != null && it > 0 }
            pendingDeepLink = DeepLinkRouteResolver.fromNotification(
                type = type,
                fromUserId = fromUserId,
                chatPeerId = chatPeerId
            )
            return
        }

        // Only accept the documented custom scheme. Never concatenate arbitrary URI text into a
        // Navigation route: malformed path values can otherwise bypass typed route assumptions or
        // crash argument parsing. External profile/chat links currently use a positive local id;
        // profile/chat repositories remain the authorization boundary for the destination data.
        pendingDeepLink = DeepLinkRouteResolver.fromUri(
            scheme = uri.scheme,
            userInfo = uri.userInfo,
            host = uri.host,
            pathSegments = uri.pathSegments,
            hasQueryOrFragment = !uri.query.isNullOrBlank() || !uri.fragment.isNullOrBlank()
        )

        if (pendingDeepLink == null) rejectDeepLink(uri.toString())
    }

    private fun rejectDeepLink(raw: String) {
        pendingDeepLink = null
        Log.w("MainActivity", "Rejected malformed or unsupported deep link: ${raw.take(160)}")
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            isPermissionPromptInFlight = true
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val PRESENCE_HEARTBEAT_INTERVAL_MS = 2L * 60L * 1000L
    }
}
