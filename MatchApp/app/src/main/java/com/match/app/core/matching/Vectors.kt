package com.match.app.core.matching

import kotlin.math.abs
import kotlin.math.sqrt

/** Vector utilities for interest/personality matching. */
object Vectors {

    /** Cosine similarity in [-1, 1], returning 0 for zero-length or zero-norm vectors. */
    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "vector size mismatch ${a.size} vs ${b.size}" }
        require(a.all { it.isFinite() } && b.all { it.isFinite() }) {
            "vectors must contain only finite values"
        }
        // Scale each input independently to avoid overflow when large questionnaire
        // values are squared. Double precision avoids float accumulation overflow.
        val scaleA = a.maxOfOrNull { abs(it.toDouble()) } ?: 0.0
        val scaleB = b.maxOfOrNull { abs(it.toDouble()) } ?: 0.0
        if (scaleA == 0.0 || scaleB == 0.0) return 0f

        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            val x = a[i].toDouble() / scaleA
            val y = b[i].toDouble() / scaleB
            dot += x * y
            normA += x * x
            normB += y * y
        }
        return (dot / (sqrt(normA) * sqrt(normB))).coerceIn(-1.0, 1.0).toFloat()
    }

    /** Mutual compatibility between each person's preferences and the other's self-description. */
    fun questionnaireScore(
        seekerSelf: FloatArray,
        seekerPartner: FloatArray,
        candidateSelf: FloatArray,
        candidatePartner: FloatArray
    ): Float {
        val aToB = cosine(seekerPartner, candidateSelf)
        val bToA = cosine(candidatePartner, seekerSelf)
        val s1 = (aToB + 1f) / 2f
        val s2 = (bToA + 1f) / 2f
        return ((s1 + s2) / 2f).coerceIn(0f, 1f)
    }
}
