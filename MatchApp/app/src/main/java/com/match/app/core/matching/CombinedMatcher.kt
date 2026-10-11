package com.match.app.core.matching

/**
 * Fuses questionnaire and optional astrology compatibility into a bounded score.
 *
 * An unknown dimension is neutral rather than an automatic mismatch. Weights must
 * be non-negative and finite; a zero-weight configuration returns neutral.
 */
object CombinedMatcher {
    /**
     * @param questionnaire compatibility score from 0 to 1
     * @param astrology optional compatibility score from 0 to 1
     * @param wQ relative questionnaire weight
     * @param wA relative astrology weight
     */
    fun combine(questionnaire: Float, astrology: Float, wQ: Float = 0.6f, wA: Float = 0.4f): Float {
        require(wQ.isFinite() && wQ >= 0f) { "Questionnaire weight must be non-negative and finite" }
        require(wA.isFinite() && wA >= 0f) { "Astrology weight must be non-negative and finite" }

        // Normalize before multiplication so very large finite weights cannot overflow.
        val scale = maxOf(wQ, wA)
        if (scale == 0f) return 0.5f
        val qWeight = wQ / scale
        val aWeight = wA / scale
        val qScore = if (questionnaire.isFinite()) questionnaire.coerceIn(0f, 1f) else 0.5f
        val aScore = if (astrology.isFinite()) astrology.coerceIn(0f, 1f) else 0.5f
        return ((qScore * qWeight + aScore * aWeight) / (qWeight + aWeight))
            .coerceIn(0f, 1f)
    }
}
