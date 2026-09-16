package com.match.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class MemberPresence(
    val online: Boolean = false,
    val lastActiveAt: Long = 0L
)

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

    suspend fun memberPresence(targetUid: String): Result<MemberPresence> = runCatching {
        require(auth.currentUser != null) { "Sign in required" }
        require(targetUid.isNotBlank()) { "Missing target profile" }
        val result = functions.getHttpsCallable("getMemberPresence")
            .call(mapOf("targetUid" to targetUid))
            .await()
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?>
            ?: error("Invalid presence response")
        MemberPresence(
            online = payload["online"] as? Boolean ?: false,
            lastActiveAt = (payload["lastActiveAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
