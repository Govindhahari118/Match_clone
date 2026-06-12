package com.match.app.core.matching

import com.match.app.domain.model.UserProfile
import kotlin.math.abs

/**
 * Advanced multi-dimensional match scoring engine.
 * Calculates compatibility across:
 * 1. Personality (Cosine similarity of vectors)
 * 2. Astrology (10 Poruthams)
 * 3. Demographics (Education, Profession, Income)
 * 4. Verification & Trust
 */
object MatchScorer {

    fun calculate(me: UserProfile, peer: UserProfile): Int {
        var score = 0f
        
        // 1. Questionnaire/Personality (40% weight)
        val selfV = me.selfVector
        val partnerV = me.partnerVector
        val peerSelfV = peer.selfVector
        val peerPartnerV = peer.partnerVector
        
        val personalityScore = if (selfV != null && partnerV != null && peerSelfV != null && peerPartnerV != null) {
            Vectors.questionnaireScore(selfV, partnerV, peerSelfV, peerPartnerV)
        } else 0f
        score += personalityScore * 0.40f

        // 2. Astrology (30% weight)
        val astrologyScore = Astrology.score(
            me.rasi, me.nakshatra,
            peer.rasi, peer.nakshatra
        )
        score += astrologyScore * 0.30f

        // 3. Demographics & Lifestyle (20% weight)
        var demoScore = 0.5f
        if (me.education == peer.education) demoScore += 0.2f
        if (me.occupationCategory == peer.occupationCategory) demoScore += 0.2f
        if (me.diet == peer.diet) demoScore += 0.1f
        score += demoScore.coerceIn(0f, 1f) * 0.20f

        // 4. Trust & Verification (10% weight)
        val trustScore = (peer.verificationLevel.toFloat() / 5f).coerceIn(0f, 1f)
        score += trustScore * 0.10f

        // Age proximity penalty (if outside preferred range)
        val ageDiff = abs(me.age - peer.age)
        if (ageDiff > 10) score -= 0.1f

        return (score * 100).toInt().coerceIn(0, 100)
    }
}
