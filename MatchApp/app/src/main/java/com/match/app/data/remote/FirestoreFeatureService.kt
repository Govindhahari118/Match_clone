package com.match.app.data.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compatibility bridge retained only for the deployed assisted-matchmaking backend.
 *
 * Historical prototype methods for events, counselling, referrals, background checks, secure-call
 * requests, communities and rewards were removed because their corresponding callable/provider
 * contracts are not deployed. Hidden screens must fail closed instead of compiling against phantom
 * backend APIs.
 */
@Singleton
class FirestoreFeatureService @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun requestRM(
        uid: String,
        plan: String,
        preferences: String,
        name: String = "",
        phone: String = ""
    ): String {
        require(uid.isNotBlank())
        val result = functions.getHttpsCallable("requestRelationshipManager").call(
            mapOf(
                "plan" to plan,
                "preferences" to preferences,
                "name" to name,
                "phone" to phone
            )
        ).await()
        return result.stringField("requestId")
    }

    suspend fun getRMRequest(uid: String): Map<String, Any?>? {
        if (uid.isBlank()) return null
        val result = functions.getHttpsCallable("getMyRelationshipManagerRequest")
            .call()
            .await()
        @Suppress("UNCHECKED_CAST")
        val payload = result.data as? Map<String, Any?> ?: return null
        @Suppress("UNCHECKED_CAST")
        return payload["request"] as? Map<String, Any?>
    }

    suspend fun cancelRMRequest(uid: String) {
        require(uid.isNotBlank())
        functions.getHttpsCallable("cancelMyRelationshipManagerRequest")
            .call()
            .await()
    }

    private fun com.google.firebase.functions.HttpsCallableResult.stringField(name: String): String {
        @Suppress("UNCHECKED_CAST")
        val payload = data as? Map<String, Any?> ?: error("Invalid server response")
        return payload[name] as? String ?: error("Missing $name")
    }
}
