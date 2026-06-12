package com.match.app.core.matching

/** Fuses questionnaire and astrology scores. */
object CombinedMatcher {
    /**
     * @param questionnaire 0..1
     * @param astrology     0..1
     * @param wQ            weight on questionnaire (default 0.6)
     * @param wA            weight on astrology     (default 0.4)
     */
    fun combine(questionnaire: Float, astrology: Float, wQ: Float = 0.6f, wA: Float = 0.4f): Float {
        val sum = wQ + wA
        return ((questionnaire * wQ + astrology * wA) / sum).coerceIn(0f, 1f)
    }
}
