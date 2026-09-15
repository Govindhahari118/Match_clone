package com.match.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    suspend fun heartbeat(): Result<Long> = runCatching {
        if (auth.currentUser == null) return@runCatching 0L
        val result = functions.getHttpsCallable("touchPresence").call().await()
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?>
            ?: error("Invalid presence response")
        (payload["lastActiveAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
    }
}
