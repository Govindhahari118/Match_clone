package com.match.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
import com.match.app.data.local.dao.UserDao
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
    @Inject lateinit var userDao: UserDao
    @Inject lateinit var subscriptionRepo: SubscriptionRepository
    @Inject lateinit var inAppUpdateManager: InAppUpdateManager
    @Inject lateinit var remoteConfig: RemoteConfigManager

    private var isPermissionPromptInFlight = false
    private var isBiometricPromptShowing = false

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isPermissionPromptInFlight = false
        }

    var pendingDeepLink: String? = null
        private set

    /** Consume the pending deep link — clears it so it only fires once. */
    fun consumeDeepLink(): String? {
        val link = pendingDeepLink
        pendingDeepLink = null
        return link
    }

    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // window.setFlags(
        //     WindowManager.LayoutParams.FLAG_SECURE,
        //     WindowManager.LayoutParams.FLAG_SECURE
        // )
        enableEdgeToEdge()
        // Keep splash on screen until session state is resolved
        splash.setKeepOnScreenCondition { !isReady }
        lifecycleScope.launch {
            try {
                kotlinx.coroutines.withTimeout(500) {
                    session.userId.first()
                }
            } catch (e: Exception) {
                // Ignore timeout
            }
            isReady = true
        }
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)
        setContent { MatchRoot() }
    }

    override fun onResume() {
        super.onResume()
        if (isPermissionPromptInFlight || isBiometricPromptShowing) return
        // Record activity for session expiry tracking
        lifecycleScope.launch { session.touchActivity() }
        // Trigger biometric lock if enabled
        lifecycleScope.launch {
            if (session.biometricLock.first()) showBiometricPrompt()
        }
        // Check for app updates via Play Store
        inAppUpdateManager.checkForUpdate(
            activity = this,
            forceUpdateVersionCode = remoteConfig.forceUpdateVersionCode
        )
    }

    private fun showBiometricPrompt() {
        if (isPermissionPromptInFlight || isBiometricPromptShowing) return
        val canAuthenticate = BiometricManager.from(this)
            .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) return
        isBiometricPromptShowing = true

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    isBiometricPromptShowing = false
                    // Close app if user cancels/fails
                    if (code == BiometricPrompt.ERROR_USER_CANCELED ||
                        code == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) finishAffinity()
                }
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isBiometricPromptShowing = false
                    // App already shown — nothing to do
                }
                override fun onAuthenticationFailed() {
                    // Keep prompt active; system manages retries.
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MatrimonyConnect")
            .setSubtitle("Verify your identity to continue")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: run {
            val notifType   = intent?.getStringExtra("notif_type")
            val fromUserId  = intent?.getLongExtra("from_user_id", -1L).takeIf { it != null && it > 0 }
            val chatPeerId  = intent?.getLongExtra("peer_id", -1L).takeIf { it != null && it > 0 }
            pendingDeepLink = when (notifType) {
                "message"           -> chatPeerId?.let { "chat/$it" } ?: "chat_list"
                "interest_received" -> fromUserId?.let { "detail/$it" } ?: "interests"
                "new_match",
                "mutual_match"      -> fromUserId?.let { "detail/$it" } ?: "matches"
                "profile_viewed"    -> "who_viewed"
                "like"              -> "interests"
                "notification"      -> "notifications"
                "boost_expiring"    -> "profile_boost"
                "verification"      -> "verification"
                "reward"            -> "daily_rewards"
                else                -> null
            }
            return
        }
        pendingDeepLink = when (uri.host) {
            "match"         -> uri.pathSegments.firstOrNull()?.let { "detail/$it" }
            "chat"          -> uri.pathSegments.firstOrNull()?.let { "chat/$it" }
            "notifications" -> "notifications"
            "interests"     -> "interests"
            "matches"       -> "matches"
            "who_viewed"    -> "who_viewed"
            "pricing"       -> "pricing"
            else            -> null
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                isPermissionPromptInFlight = true
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        Toast.makeText(this, "Payment successful! Verifying...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val orderId = paymentData?.orderId ?: ""
            val payId = razorpayPaymentId ?: ""
            val signature = paymentData?.signature ?: ""
            // Read plan from the repository set by PricingScreen before checkout.open()
            val planType = subscriptionRepo.pendingPlanType
            val amount = subscriptionRepo.pendingAmountPaise
            val result = subscriptionRepo.verifyAndActivatePremium(
                orderId = orderId,
                paymentId = payId,
                signature = signature,
                planType = planType,
                amount = amount
            )
            result.onSuccess {
                Toast.makeText(this@MainActivity, "Welcome to Premium!", Toast.LENGTH_LONG).show()
            }.onFailure {
                // Fallback: activate locally if server verification fails
                val userId = session.userId.first()
                if (userId != null && userId > 0) {
                    val user = userDao.findById(userId)
                    if (user != null) userDao.update(user.copy(isPremium = true))
                }
                Toast.makeText(this@MainActivity, "Premium activated (offline).", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPaymentError(errorCode: Int, errorDescription: String?, paymentData: PaymentData?) {
        val msg = when (errorCode) {
            0    -> "Network error. Please try again."
            1    -> "Payment cancelled."
            2    -> "Invalid options."
            else -> "Payment failed: $errorDescription"
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}

