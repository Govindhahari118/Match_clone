package com.match.app.data.remote

import android.content.Context
import com.google.firebase.functions.FirebaseFunctions
import com.match.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the private installation identity used only for server-side FCM registration.
 *
 * The identifier is random app-install state, not advertising ID, Android ID or user identity.
 * Server callables bind it to the currently authenticated Firebase UID and atomically transfer
 * ownership during an account switch.
 */
@Singleton
class FcmDeviceRegistry @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val functions = FirebaseFunctions.getInstance()

    val deviceId: String
        get() {
            prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.matches(DEVICE_ID_REGEX) }?.let { return it }
            val generated = UUID.randomUUID().toString().replace("-", "")
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            return generated
        }

    suspend fun register(token: String) {
        require(token.isNotBlank())
        functions.getHttpsCallable("registerFcmDevice")
            .call(
                mapOf(
                    "deviceId" to deviceId,
                    "token" to token,
                    "appVersion" to BuildConfig.VERSION_NAME
                )
            )
            .await()
    }

    suspend fun revoke() {
        functions.getHttpsCallable("revokeFcmDevice")
            .call(mapOf("deviceId" to deviceId))
            .await()
    }

    private companion object {
        const val PREFS_NAME = "fcm_prefs"
        const val KEY_DEVICE_ID = "installation_id"
        val DEVICE_ID_REGEX = Regex("^[A-Za-z0-9_-]{16,128}$")
    }
}
