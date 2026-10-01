package com.match.app.security

import android.view.WindowManager
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.match.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ScreenCaptureProtectionTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun screenCaptureFlagTracksPersistedPrivacyPreference() {
        runBlocking { compose.activity.session.setScreenshotProtection(true) }
        compose.waitUntil(timeoutMillis = 5_000) { secureFlagEnabled() }
        assertTrue("FLAG_SECURE must be set when protection is enabled", secureFlagEnabled())

        runBlocking { compose.activity.session.setScreenshotProtection(false) }
        compose.waitUntil(timeoutMillis = 5_000) { !secureFlagEnabled() }
        assertFalse("FLAG_SECURE must clear only after an explicit opt-out", secureFlagEnabled())

        // Restore the privacy-preserving default for subsequent instrumentation tests.
        runBlocking { compose.activity.session.setScreenshotProtection(true) }
        compose.waitUntil(timeoutMillis = 5_000) { secureFlagEnabled() }
    }

    private fun secureFlagEnabled(): Boolean =
        (compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
}
