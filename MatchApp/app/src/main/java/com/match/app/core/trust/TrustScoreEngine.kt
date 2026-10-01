package com.match.app.core.trust

import com.match.app.domain.model.UserProfile

/**
 * Legacy local profile-quality preview retained for compatibility with older screens/tests.
 *
 * Production trust decisions must use TrustRepository/getTrustSummary, whose inputs are derived
 * by trusted backend code. Payment or premium membership must never increase trust.
 *
 * Local preview breakdown:
 *   - ID verified: 35 points
 *   - Photo added: 20 points
 *   - Bio filled: 10 points
 *   - Questionnaire completed: 15 points
 *   - Profession filled: 10 points
 *   - Native state filled: 10 points
 */
object TrustScoreEngine {

    data class TrustBreakdown(
        val score: Int,
        val label: String,
        val verified: Boolean,
        val hasPhoto: Boolean,
        val hasBio: Boolean,
        val hasQuestionnaire: Boolean,
        val hasProfession: Boolean
    )

    fun compute(profile: UserProfile): TrustBreakdown {
        var score = 0

        val verified = profile.isVerified
        val hasPhoto = profile.primaryPhotoPath != null
        val hasBio   = profile.bio.isNotBlank()
        val hasQ     = profile.hasQuestionnaire
        val hasPro   = profile.profession.isNotBlank()

        if (verified) score += 35
        if (hasPhoto) score += 20
        if (hasBio)   score += 10
        if (hasQ)     score += 15
        if (hasPro)   score += 10
        if (profile.nativeState.isNotBlank()) score += 10

        val label = when {
            score >= 80 -> "Highly Trusted"
            score >= 60 -> "Trusted"
            score >= 40 -> "Partially Verified"
            else        -> "Low Trust"
        }

        return TrustBreakdown(
            score           = score.coerceIn(0, 100),
            label           = label,
            verified        = verified,
            hasPhoto        = hasPhoto,
            hasBio          = hasBio,
            hasQuestionnaire = hasQ,
            hasProfession   = hasPro
        )
    }
}
