package com.match.app.data.repo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class TrustSummary(
    val score: Int,
    val tier: String,
    val factors: List<String>,
    val formulaVersion: String
) {
    val tierLabel: String
        get() = when (tier.uppercase()) {
            "HIGH" -> "High trust"
            "STRONG" -> "Strong trust"
            "BUILDING" -> "Building trust"
            else -> "Basic trust"
        }
}

/**
 * Trusted boundary for Matree Trust Score.
 *
 * The Android client never calculates or supplies score inputs. The backend derives identity,
 * account-age, profile-completeness and reviewed safety state from authoritative sources and
 * returns only a privacy-safe public summary.
 */
@Singleton
class TrustRepository @Inject constructor() {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun load(targetUid: String? = null): TrustSummary {
        val payload = targetUid?.takeIf { it.isNotBlank() }?.let { mapOf("targetUid" to it) } ?: emptyMap()
        val result = functions.getHttpsCallable("getTrustSummary")
            .call(payload)
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: error("Invalid trust response")
        val score = (data["score"] as? Number)?.toInt()?.coerceIn(0, 100)
            ?: error("Missing trust score")
        val tier = data["tier"] as? String ?: "BASIC"
        val factors = (data["factors"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter { it.isNotBlank() }
            ?.take(8)
            ?: emptyList()
        val formulaVersion = data["formulaVersion"] as? String ?: ""

        return TrustSummary(
            score = score,
            tier = tier,
            factors = factors,
            formulaVersion = formulaVersion
        )
    }
}
