package com.match.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import com.match.app.data.repo.SubscriptionRepository
import com.match.app.data.session.SessionStore
import com.match.app.ui.MatchRoot
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity(), PaymentResultWithDataListener {

    @Inject lateinit var session: SessionStore
    @Inject lateinit var subscriptionRepo: SubscriptionRepository
    @Inject lateinit var inAppUpdateManager: InAppUpdateManager
    @Inject lateinit var remoteConfig: RemoteConfigManager

    private var isPermissionPromptInFlight = false
    private var isBiometricPromptShowing = false
    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isPermissionPromptInFlight = false }

    var pendingDeepLink: String? = null
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
        lifecycleScope.launch { if (session.biometricLock.first()) showBiometricPrompt() }
        inAppUpdateManager.checkForUpdate(activity = this, forceUpdateVersionCode = remoteConfig.forceUpdateVersionCode)
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

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: run {
            val type = intent?.getStringExtra("notif_type")
            val fromUserId = intent?.getLongExtra("from_user_id", -1L).takeIf { it != null && it > 0 }
            val chatPeerId = intent?.getLongExtra("peer_id", -1L).takeIf { it != null && it > 0 }
            pendingDeepLink = when (type) {
                "message" -> chatPeerId?.let { "chat/$it" } ?: "chat_list"
                "interest_received" -> fromUserId?.let { "detail/$it" } ?: "interests"
                "new_match", "mutual_match" -> fromUserId?.let { "detail/$it" } ?: "matches"
                "profile_viewed" -> "who_viewed"
                "like" -> "interests"
                "notification" -> "notifications"
                "boost_expiring" -> "profile_boost"
                "verification", "verification_update" -> "verification"
                "reward" -> "daily_rewards"
                else -> null
            }
            return
        }
        pendingDeepLink = when (uri.host) {
            "match" -> uri.pathSegments.firstOrNull()?.let { "detail/$it" }
            "chat" -> uri.pathSegments.firstOrNull()?.let { "chat/$it" }
            "notifications" -> "notifications"
            "interests" -> "interests"
            "matches" -> "matches"
            "who_viewed" -> "who_viewed"
            "pricing" -> "pricing"
            else -> null
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            isPermissionPromptInFlight = true
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val orderId = paymentData?.orderId.orEmpty()
        val paymentId = razorpayPaymentId.orEmpty()
        val signature = paymentData?.signature.orEmpty()
        if (orderId.isBlank() || paymentId.isBlank() || signature.isBlank()) {
            Toast.makeText(this, "Payment response incomplete. Do not pay again; contact support.", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Payment received. Verifying securely…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            subscriptionRepo.verifyAndActivatePremium(orderId, paymentId, signature)
                .onSuccess { Toast.makeText(this@MainActivity, "Membership activated.", Toast.LENGTH_LONG).show() }
                .onFailure {
                    // Never grant local premium on verification failure. A captured payment can
                    // still be recovered by the signed Razorpay webhook on the server.
                    subscriptionRepo.checkPremiumStatus()
                    Toast.makeText(
                        this@MainActivity,
                        "Payment verification is pending. Do not pay again; membership will update after server confirmation.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    override fun onPaymentError(errorCode: Int, errorDescription: String?, paymentData: PaymentData?) {
        val msg = when (errorCode) {
            0 -> "Network error. Please try again."
            1 -> "Payment cancelled."
            2 -> "Invalid payment options."
            else -> "Payment failed. Please try again."
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
