package com.match.app.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsernameRepository @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    fun normalize(raw: String): String = raw.trim().lowercase().removePrefix("@")

    fun validate(raw: String): String? {
        val value = normalize(raw)
        if (value.length !in 3..30) return "Username must be 3-30 characters."
        if (!value.first().isLetterOrDigit()) return "Username must start with a letter or number."
        if (value.any { !(it.isLetterOrDigit() || it == '.' || it == '_') }) {
            return "Use only letters, numbers, dot or underscore."
        }
        if (".." in value) return "Username cannot contain consecutive dots."
        return null
    }

    suspend fun reserve(raw: String): Result<String> = runCatching {
        check(auth.currentUser != null) { "Sign in required" }
        validate(raw)?.let { error(it) }
        val normalized = normalize(raw)
        val response = functions.getHttpsCallable("setUsername")
            .call(mapOf("username" to normalized))
            .await()
        @Suppress("UNCHECKED_CAST")
        val payload = response.data as? Map<String, Any?>
            ?: error("Invalid username response")
        payload["username"] as? String ?: normalized
    }
}
