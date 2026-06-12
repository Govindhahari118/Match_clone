package com.match.app.core.matching

import kotlin.math.sqrt

/** Vector utilities for interest/personality matching. */
object Vectors {

    /** Cosine similarity in [-1, 1]; returns 0 for zero vectors. */
    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "vector size mismatch ${a.size} vs ${b.size}" }
        var dot = 0f; var na = 0f; var nb = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            na  += a[i] * a[i]
            nb  += b[i] * b[i]
        }
        if (na == 0f || nb == 0f) return 0f
        return (dot / (sqrt(na) * sqrt(nb))).coerceIn(-1f, 1f)
    }

    /** Similarity between a seeker's self-profile and the partner they're looking for, vs. a candidate. */
    fun questionnaireScore(
        seekerSelf: FloatArray,
        seekerPartner: FloatArray,
        candidateSelf: FloatArray,
        candidatePartner: FloatArray
    ): Float {
        val aToB = cosine(seekerPartner, candidateSelf)   // does candidate match what seeker wants?
        val bToA = cosine(candidatePartner, seekerSelf)   // does seeker match what candidate wants?
        // map [-1,1] -> [0,1] and average both directions
        val s1 = (aToB + 1f) / 2f
        val s2 = (bToA + 1f) / 2f
        return ((s1 + s2) / 2f).coerceIn(0f, 1f)
    }
}
