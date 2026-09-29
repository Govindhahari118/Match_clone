package com.match.app

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Repository-safe production entry smoke tests.
 *
 * This class deliberately does not fake authentication, seed production inventory, bypass Firebase,
 * or claim to replace the required two-user/two-device release matrix. Authenticated production E2E
 * requires the exact release Firebase/Play/App Check environment and real test identities, so that
 * evidence belongs to the external release gate rather than a demo-backed instrumentation test.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UserJourneyTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hilt.inject()
        FirebaseAuth.getInstance().signOut()
    }

    @Test
    fun freshLaunchShowsProductionAuthEntryOnly() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("signin_screen")).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("signin_screen").assertIsDisplayed()
        compose.onNodeWithTag("signin_email").assertIsDisplayed()
        compose.onNodeWithTag("signin_password").assertIsDisplayed()
        compose.onNodeWithTag("signin_submit").assertIsDisplayed()
        compose.onNodeWithTag("signin_go_signup").assertIsDisplayed()
        compose.onNodeWithTag("signin_phone").assertIsDisplayed()
        compose.onNodeWithTag("bottom_bar").assertDoesNotExist()
    }

    @Test
    fun phoneOtpEntryIsReachableWithoutDemoIdentity() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("signin_phone")).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("signin_phone").performClick()

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("phone_auth_screen")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("phone_auth_screen").assertIsDisplayed()
        compose.onNodeWithTag("phone_auth_number").assertIsDisplayed()
        compose.onNodeWithTag("phone_auth_send").assertIsDisplayed()
    }

    @Test
    fun authEntryDoesNotExposeRetiredDemoNavigation() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag("signin_screen")).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithText("Demo", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("drawer_regions").assertDoesNotExist()
        compose.onNodeWithTag("drawer_circles").assertDoesNotExist()
        compose.onNodeWithTag("drawer_stories").assertDoesNotExist()
    }
}
