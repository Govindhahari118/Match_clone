package com.match.app.data.remote

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileAnalyticsService @Inject constructor() {
    data class Snapshot(
        val profileViews: Int,
        val interestsReceived: Int,
        val interestsSent: Int,
        val mutualMatches: Int,
        val conversations: Int,
        val shortlistedBy: Int,
        val isVerified: Boolean
    )

    private val functions = FirebaseFunctions.getInstance()

    suspend fun load(): Snapshot {
        val result = functions.getHttpsCallable("getProfileAnalytics").call().await()
        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid analytics response")
        return Snapshot(
            profileViews = data.count("profileViews"),
            interestsReceived = data.count("interestsReceived"),
            interestsSent = data.count("interestsSent"),
            mutualMatches = data.count("mutualMatches"),
            conversations = data.count("conversations"),
            shortlistedBy = data.count("shortlistedBy"),
            isVerified = data["isVerified"] as? Boolean ?: false
        )
    }

    private fun Map<String, Any?>.count(key: String): Int {
        return ((this[key] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }
}
