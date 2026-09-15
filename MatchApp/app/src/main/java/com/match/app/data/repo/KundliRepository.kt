package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KundliRepository @Inject constructor() {
    data class SharedHoroscope(
        val available: Boolean,
        val reason: String = "",
        val targetName: String = "",
        val myRasi: String = "",
        val myNakshatra: String = "",
        val targetRasi: String = "",
        val targetNakshatra: String = ""
    )

    private val functions = FirebaseFunctions.getInstance()

    /**
     * Requests only the minimal horoscope attributes the backend currently permits this signed-in
     * viewer to use. The backend rechecks block/hide/stealth/request state on every call.
     */
    suspend fun getSharedHoroscope(targetUid: String): Result<SharedHoroscope> = runCatching {
        require(targetUid.isNotBlank()) { "Missing target profile" }
        val response = functions.getHttpsCallable("getSharedHoroscope")
            .call(mapOf("targetUid" to targetUid))
            .await()
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any?> ?: error("Invalid horoscope response")
        SharedHoroscope(
            available = data["available"] as? Boolean ?: false,
            reason = data["reason"] as? String ?: "",
            targetName = data["targetName"] as? String ?: "",
            myRasi = data["myRasi"] as? String ?: "",
            myNakshatra = data["myNakshatra"] as? String ?: "",
            targetRasi = data["targetRasi"] as? String ?: "",
            targetNakshatra = data["targetNakshatra"] as? String ?: ""
        )
    }
}
