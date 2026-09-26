package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Trusted boundary for confirming canonical matrimonial-profile religion. */
@Singleton
class ReligionProfileRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun confirm(religionLabel: String) {
        val result = functions.getHttpsCallable("confirmReligion")
            .call(mapOf("religion" to religionLabel.trim()))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?>
            ?: error("Invalid religion confirmation response")
        check(data["locked"] == true) { "Religion confirmation was not completed" }
    }
}
