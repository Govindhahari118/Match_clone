package com.match.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.util.TimeZone
import javax.inject.Singleton

data class NotificationPreferences(
    val interests: Boolean = true,
    val matches: Boolean = true,
    val messages: Boolean = true,
    val system: Boolean = true,
    val quietHours: Boolean = false
)

@Singleton
class NotificationPreferenceRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    @Volatile
    private var lastTimeZoneSyncKey: String? = null

    private fun currentTimeZoneId(): String =
        TimeZone.getDefault().id.trim().take(64).ifBlank { "UTC" }

    fun observe(): Flow<NotificationPreferences> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(NotificationPreferences())
            close()
            return@callbackFlow
        }
        val registration = firestore.collection("notificationPrefs").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                trySend(
                    NotificationPreferences(
                        interests = snapshot?.getBoolean("interests") ?: true,
                        matches = snapshot?.getBoolean("matches") ?: true,
                        messages = snapshot?.getBoolean("messages") ?: true,
                        system = snapshot?.getBoolean("system") ?: true,
                        quietHours = snapshot?.getBoolean("quietHours") ?: false
                    )
                )
            }
        awaitClose { registration.remove() }
    }

    /**
     * Refreshes the server-side timezone used for quiet-hours delivery.
     *
     * This is intentionally called from the foreground lifecycle so travel / DST changes do not
     * depend on the member opening Settings. The in-process key prevents repeated writes on every
     * resume while still syncing once per account/process and whenever the timezone actually changes.
     */
    suspend fun syncCurrentTimeZone() {
        val uid = auth.currentUser?.uid ?: return
        val timeZone = currentTimeZoneId()
        val syncKey = "$uid|$timeZone"
        if (lastTimeZoneSyncKey == syncKey) return

        firestore.collection("notificationPrefs").document(uid)
            .set(mapOf("timeZone" to timeZone), SetOptions.merge())
            .await()
        lastTimeZoneSyncKey = syncKey
    }

    suspend fun update(key: String, enabled: Boolean) {
        require(key in setOf("interests", "matches", "messages", "system", "quietHours"))
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val timeZone = currentTimeZoneId()
        firestore.collection("notificationPrefs").document(uid)
            .set(
                mapOf(
                    key to enabled,
                    "timeZone" to timeZone
                ),
                SetOptions.merge()
            )
            .await()
        lastTimeZoneSyncKey = "$uid|$timeZone"
    }
}
