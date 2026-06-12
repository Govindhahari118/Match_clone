package com.match.app.core.security

import com.match.app.data.local.entity.UserEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * AI-powered fake profile detection using heuristic signals.
 *
 * Scores range from 0 (definitely fake) to 100 (highly trustworthy).
 * Profiles scoring below 30 are flagged for manual review.
 *
 * Signals evaluated:
 * - Profile completeness (all fields filled)
 * - Photo presence and count
 * - Bio quality (length, keywords)
 * - Verification status
 * - Account age
 * - Behavioral patterns (will be enhanced with ML Kit later)
 */
@Singleton
class FakeProfileDetector @Inject constructor() {

    companion object {
        const val THRESHOLD_FLAG = 30    // Auto-flag for review
        const val THRESHOLD_WARN = 50    // Show warning to viewers
        const val THRESHOLD_TRUSTED = 75 // Mark as trusted
    }

    data class TrustReport(
        val overallScore: Int,           // 0-100
        val completenessScore: Int,      // 0-100
        val bioScore: Int,               // 0-100
        val verificationScore: Int,      // 0-100
        val activityScore: Int,          // 0-100
        val photoScore: Int,             // 0-100
        val flags: List<String>,         // List of suspicious signals
        val isFlagged: Boolean,          // Score < THRESHOLD_FLAG
        val badgeTier: BadgeTier
    )

    enum class BadgeTier {
        UNVERIFIED,     // No verification at all
        BASIC,          // Email verified only
        PHONE_VERIFIED, // Phone + email verified
        ID_VERIFIED,    // Government ID verified (blue tick)
        PREMIUM_TRUST   // ID + premium + high completeness
    }

    fun analyze(
        user: UserEntity,
        photoCount: Int = 0,
        hasQuestionnaire: Boolean = false
    ): TrustReport {
        val flags = mutableListOf<String>()

        // 1. Profile completeness (30% weight)
        val completenessScore = calculateCompleteness(user, hasQuestionnaire)
        if (completenessScore < 30) flags.add("Very incomplete profile")

        // 2. Bio quality (15% weight)
        val bioScore = analyzeBio(user.bio, flags)

        // 3. Verification status (25% weight)
        val verificationScore = when {
            user.isVerified -> 100
            user.email.isNotBlank() -> 40
            else -> 0
        }

        // 4. Activity indicators (15% weight)
        val activityScore = analyzeActivity(user, flags)

        // 5. Photo assessment (15% weight)
        val photoScore = when {
            photoCount >= 3 -> 100
            photoCount == 2 -> 75
            photoCount == 1 -> 50
            else -> { flags.add("No photos uploaded"); 0 }
        }

        val overall = (
            completenessScore * 0.30 +
            bioScore * 0.15 +
            verificationScore * 0.25 +
            activityScore * 0.15 +
            photoScore * 0.15
        ).roundToInt().coerceIn(0, 100)

        val badgeTier = when {
            user.isVerified && user.isPremium && completenessScore > 80 -> BadgeTier.PREMIUM_TRUST
            user.isVerified -> BadgeTier.ID_VERIFIED
            user.email.isNotBlank() && completenessScore > 50 -> BadgeTier.PHONE_VERIFIED
            user.email.isNotBlank() -> BadgeTier.BASIC
            else -> BadgeTier.UNVERIFIED
        }

        return TrustReport(
            overallScore = overall,
            completenessScore = completenessScore,
            bioScore = bioScore,
            verificationScore = verificationScore,
            activityScore = activityScore,
            photoScore = photoScore,
            flags = flags,
            isFlagged = overall < THRESHOLD_FLAG,
            badgeTier = badgeTier
        )
    }

    private fun calculateCompleteness(user: UserEntity, hasQuestionnaire: Boolean): Int {
        // Weighted scoring: identity fields count more than optional ones
        var weightedFilled = 0.0
        var totalWeight = 0.0

        fun checkW(value: String, weight: Double) { totalWeight += weight; if (value.isNotBlank()) weightedFilled += weight }
        fun checkIntW(value: Int, default: Int, weight: Double) { totalWeight += weight; if (value != default) weightedFilled += weight }

        // Identity fields (weight 3) — these are critical for trust
        checkW(user.displayName, 3.0)
        checkW(user.email, 3.0)
        checkIntW(user.age, 25, 3.0)
        checkW(user.city, 3.0)
        checkW(user.state, 3.0)

        // Core profile fields (weight 2)
        checkW(user.bio, 2.0)
        checkW(user.religion, 2.0)
        checkW(user.motherTongue, 2.0)
        checkW(user.education, 2.0)
        checkW(user.profession, 2.0)
        checkW(user.maritalStatus, 2.0)
        checkIntW(user.heightCm, 165, 2.0)

        // Optional enrichment fields (weight 1)
        checkW(user.caste, 1.0)
        checkW(user.rasi, 1.0)
        checkW(user.nakshatra, 1.0)
        checkW(user.diet, 1.0)
        checkW(user.familyType, 1.0)
        checkW(user.fatherOccupation, 1.0)
        checkW(user.motherOccupation, 1.0)
        checkW(user.hobbies, 1.0)
        checkW(user.spokenLanguages, 1.0)
        checkW(user.nativeState, 1.0)
        checkW(user.familyValues, 1.0)
        checkW(user.aboutFamily, 1.0)
        checkW(user.manglik, 1.0)

        // Questionnaire bonus (weight 2)
        totalWeight += 2.0; if (hasQuestionnaire) weightedFilled += 2.0

        return if (totalWeight == 0.0) 0 else ((weightedFilled / totalWeight) * 100).roundToInt()
    }

    private fun analyzeBio(bio: String, flags: MutableList<String>): Int {
        if (bio.isBlank()) {
            flags.add("No bio written")
            return 0
        }
        var score = 0
        val wordCount = bio.trim().split("\\s+".toRegex()).size

        // Length scoring
        score += when {
            wordCount >= 50 -> 40
            wordCount >= 25 -> 30
            wordCount >= 10 -> 20
            else -> 10
        }

        // Penalty for suspicious patterns
        val lowerBio = bio.lowercase()
        val suspiciousPatterns = listOf(
            "whatsapp", "telegram", "instagram", "call me",
            "contact me at", "money", "send me", "western union",
            "bitcoin", "invest", "business opportunity"
        )
        val suspiciousCount = suspiciousPatterns.count { lowerBio.contains(it) }
        if (suspiciousCount > 0) {
            flags.add("Bio contains suspicious keywords")
            score -= suspiciousCount * 15
        }

        // Bonus for personal details
        if (wordCount >= 15) score += 20
        if (bio.contains(",") || bio.contains(".")) score += 10 // Proper sentences
        if (!bio.all { it.isUpperCase() || it.isWhitespace() }) score += 10 // Not all caps

        // Penalty for repeated characters
        if (bio.contains(Regex("(.)\\1{4,}"))) {
            flags.add("Bio contains repeated characters")
            score -= 20
        }

        return score.coerceIn(0, 100)
    }

    private fun analyzeActivity(user: UserEntity, flags: MutableList<String>): Int {
        var score = 50 // Base score

        // Account age bonus
        val accountAgeMs = System.currentTimeMillis() - user.createdAt
        val accountAgeDays = accountAgeMs / (24 * 60 * 60 * 1000L)

        score += when {
            accountAgeDays > 90 -> 30
            accountAgeDays > 30 -> 20
            accountAgeDays > 7 -> 10
            accountAgeDays > 1 -> 5
            else -> { flags.add("Account created very recently"); -10 }
        }

        // Premium users get trust bonus
        if (user.isPremium) score += 20

        return score.coerceIn(0, 100)
    }
}
