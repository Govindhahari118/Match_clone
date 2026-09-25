package com.match.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class NotificationPreferences(
    val interests: Boolean = true,
    val matches: Boolean = true,
    val messages: Boolean = true,
    val system: Boolean = true
)

@Singleton
class NotificationPreferenceRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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
                        system = snapshot?.getBoolean("system") ?: true
                    )
                )
            }
        awaitClose { registration.remove() }
    }

    suspend fun update(key: String, enabled: Boolean) {
        require(key in setOf("interests", "matches", "messages", "system"))
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        firestore.collection("notificationPrefs").document(uid)
            .set(mapOf(key to enabled), SetOptions.merge())
            .await()
    }
}
