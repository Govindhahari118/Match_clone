package com.match.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.match.app.data.session.SessionStore
import com.match.app.domain.model.AppearancePreference
import com.match.app.domain.model.ThemePreference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account-scoped visual appearance preferences.
 *
 * Religion is deliberately not stored here: the canonical profile remains the only religion
 * authority. DisplayMode is deliberately not stored here either because light/dark/system is a
 * device/accessibility preference. Firestore synchronizes only the visual theme mode and the
 * last manual palette choice across the member's own devices.
 */
@Singleton
class AppearancePreferenceRepository @Inject constructor(
    private val session: SessionStore
) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private sealed interface RemoteState {
        data class Missing(val uid: String) : RemoteState
        data class Value(
            val uid: String,
            val themePreference: ThemePreference,
            val manualThemeKey: String
        ) : RemoteState
    }

    fun observe(): Flow<AppearancePreference> {
        val remoteChanges = session.firebaseUid
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) flowOf<RemoteState>()
                else observeRemote(uid)
            }
            .mapNotNull { remote ->
                when (remote) {
                    is RemoteState.Missing -> {
                        val local = session.appearancePreference.first()
                        runCatching { writeRemote(remote.uid, local) }
                        local
                    }
                    is RemoteState.Value -> {
                        val local = session.appearancePreference.first()
                        if (
                            local.themePreference != remote.themePreference ||
                            local.manualThemeKey != remote.manualThemeKey
                        ) {
                            session.setThemePreference(
                                value = remote.themePreference,
                                manualThemeKey = remote.manualThemeKey
                            )
                        }
                        session.appearancePreference.first()
                    }
                }
            }

        return merge(session.appearancePreference, remoteChanges).distinctUntilChanged()
    }

    /**
     * Applies immediately to the local Compose state, then queues a private Firestore write so the
     * same account receives the choice on another device. Firestore's offline persistence keeps a
     * disconnected write pending without inventing a server-success state.
     */
    suspend fun setThemePreference(value: ThemePreference, manualThemeKey: String? = null) {
        val safeManual = sanitizeManualThemeKey(
            manualThemeKey ?: session.appearancePreference.first().manualThemeKey
        )
        session.setThemePreference(value, safeManual)

        val uid = auth.currentUser?.uid ?: return
        val local = session.appearancePreference.first()
        writeRemote(uid, local)
    }

    private fun observeRemote(uid: String): Flow<RemoteState> = callbackFlow {
        val registration = firestore.collection(COLLECTION).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (!snapshot.exists()) {
                    trySend(RemoteState.Missing(uid))
                    return@addSnapshotListener
                }

                val preference = ThemePreference.fromStorage(snapshot.getString("themePreference"))
                val manualKey = sanitizeManualThemeKey(snapshot.getString("manualThemeKey"))
                val normalizedPreference =
                    if (preference == ThemePreference.MANUAL && manualKey == "VIVAH") {
                        ThemePreference.NEUTRAL
                    } else {
                        preference
                    }
                trySend(RemoteState.Value(uid, normalizedPreference, manualKey))
            }
        awaitClose { registration.remove() }
    }

    private suspend fun writeRemote(uid: String, preference: AppearancePreference) {
        if (auth.currentUser?.uid != uid) return
        firestore.collection(COLLECTION).document(uid)
            .set(
                mapOf(
                    "themePreference" to preference.themePreference.name,
                    "manualThemeKey" to sanitizeManualThemeKey(preference.manualThemeKey),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    private fun sanitizeManualThemeKey(value: String?): String {
        val normalized = value.orEmpty().trim().uppercase()
        return normalized.takeIf { it in ACCOUNT_THEME_KEYS } ?: "VIVAH"
    }

    private companion object {
        const val COLLECTION = "appearancePrefs"
        val ACCOUNT_THEME_KEYS = setOf(
            "VIVAH", "HINDU", "MUSLIM", "CHRISTIAN", "SIKH", "BUDDHIST", "JAIN", "PARSI"
        )
    }
}
