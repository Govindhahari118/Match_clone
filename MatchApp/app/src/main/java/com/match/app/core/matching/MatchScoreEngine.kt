package com.match.app.core.matching

import com.match.app.data.local.entity.UserEntity

/**
 * Multi-dimensional Match Score™ engine.
 *
 * Computes a 0.0–1.0 compatibility score across 9 weighted dimensions:
 *   1. Astrology (10-Porutham)       — 20%
 *   2. Religion & caste              — 15%
 *   3. Education & career level      — 10%
 *   4. Location proximity            — 10%
 *   5. Age compatibility             — 10%
 *   6. Family values alignment       — 10%
 *   7. Lifestyle compatibility       — 10%
 *   8. Physical preferences          — 10%
 *   9. Personality type              — 5%
 *
 * All computation is on-device (no API calls). Deterministic.
 */
object MatchScoreEngine {

    data class ScoreBreakdown(
        val total: Float,
        val astrology: Float,
        val religionCaste: Float,
        val educationCareer: Float,
        val location: Float,
        val age: Float,
        val familyValues: Float,
        val lifestyle: Float,
        val physical: Float,
        val personality: Float
    )

    fun compute(me: UserEntity, candidate: UserEntity): ScoreBreakdown {
        val astrology = computeAstrology(me, candidate)
        val religionCaste = computeReligionCaste(me, candidate)
        val educationCareer = computeEducationCareer(me, candidate)
        val location = computeLocation(me, candidate)
        val age = computeAge(me, candidate)
        val familyValues = computeFamilyValues(me, candidate)
        val lifestyle = computeLifestyle(me, candidate)
        val physical = computePhysical(me, candidate)
        val personality = computePersonality(me, candidate)

        val total = (
            astrology * 0.20f +
            religionCaste * 0.15f +
            educationCareer * 0.10f +
            location * 0.10f +
            age * 0.10f +
            familyValues * 0.10f +
            lifestyle * 0.10f +
            physical * 0.10f +
            personality * 0.05f
        ).coerceIn(0f, 1f)

        return ScoreBreakdown(
            total = total,
            astrology = astrology,
            religionCaste = religionCaste,
            educationCareer = educationCareer,
            location = location,
            age = age,
            familyValues = familyValues,
            lifestyle = lifestyle,
            physical = physical,
            personality = personality
        )
    }

    private fun computeAstrology(me: UserEntity, candidate: UserEntity): Float {
        if (me.nakshatra.isBlank() || candidate.nakshatra.isBlank()) return 0.5f
        if (me.rasi.isBlank() || candidate.rasi.isBlank()) return 0.5f
        val result = TenPorutham.calculate(
            nakshatraBride = me.nakshatra, rasiBride = me.rasi,
            nakshatraGroom = candidate.nakshatra, rasiGroom = candidate.rasi
        )
        return result.score.toFloat() / 10f
    }

    private fun computeReligionCaste(me: UserEntity, candidate: UserEntity): Float {
        var score = 0f
        if (me.religion.equals(candidate.religion, ignoreCase = true)) score += 0.5f
        if (me.caste.isNotBlank() && me.caste.equals(candidate.caste, ignoreCase = true)) score += 0.3f
        if (me.motherTongue.equals(candidate.motherTongue, ignoreCase = true)) score += 0.2f
        return score
    }

    private fun computeEducationCareer(me: UserEntity, candidate: UserEntity): Float {
        var score = 0f
        // Same education level
        if (me.education.isNotBlank() && me.education.equals(candidate.education, ignoreCase = true)) score += 0.4f
        // Same field
        if (me.educationField.isNotBlank() && me.educationField.equals(candidate.educationField, ignoreCase = true)) score += 0.2f
        // Same occupation category
        if (me.occupationCategory.isNotBlank() && me.occupationCategory.equals(candidate.occupationCategory, ignoreCase = true)) score += 0.2f
        // Similar income (within same bracket)
        if (me.incomeBand.isNotBlank() && me.incomeBand.equals(candidate.incomeBand, ignoreCase = true)) score += 0.2f
        return score
    }

    private fun computeLocation(me: UserEntity, candidate: UserEntity): Float {
        var score = 0f
        if (me.city.isNotBlank() && me.city.equals(candidate.city, ignoreCase = true)) score += 0.6f
        else if (me.state.isNotBlank() && me.state.equals(candidate.state, ignoreCase = true)) score += 0.3f
        if (me.countryOfResidence.isNotBlank() && me.countryOfResidence.equals(candidate.countryOfResidence, ignoreCase = true)) score += 0.4f
        return score.coerceAtMost(1f)
    }

    private fun computeAge(me: UserEntity, candidate: UserEntity): Float {
        val diff = kotlin.math.abs(me.age - candidate.age)
        return when {
            diff <= 2 -> 1.0f
            diff <= 4 -> 0.85f
            diff <= 6 -> 0.7f
            diff <= 8 -> 0.5f
            diff <= 10 -> 0.3f
            else -> 0.1f
        }
    }

    private fun computeFamilyValues(me: UserEntity, candidate: UserEntity): Float {
        var score = 0f
        if (me.familyValues.isNotBlank() && me.familyValues.equals(candidate.familyValues, ignoreCase = true)) score += 0.4f
        if (me.familyType.isNotBlank() && me.familyType.equals(candidate.familyType, ignoreCase = true)) score += 0.3f
        if (me.familyStatus.isNotBlank() && me.familyStatus.equals(candidate.familyStatus, ignoreCase = true)) score += 0.3f
        return score
    }

    private fun computeLifestyle(me: UserEntity, candidate: UserEntity): Float {
        var score = 0f
        if (me.diet.isNotBlank() && me.diet.equals(candidate.diet, ignoreCase = true)) score += 0.3f
        if (me.smoking.isNotBlank() && me.smoking.equals(candidate.smoking, ignoreCase = true)) score += 0.25f
        if (me.drinking.isNotBlank() && me.drinking.equals(candidate.drinking, ignoreCase = true)) score += 0.25f
        // Overlapping hobbies
        if (me.hobbies.isNotBlank() && candidate.hobbies.isNotBlank()) {
            val myHobbies = me.hobbies.split(",").map { it.trim().lowercase() }.toSet()
            val theirHobbies = candidate.hobbies.split(",").map { it.trim().lowercase() }.toSet()
            val overlap = myHobbies.intersect(theirHobbies).size
            if (overlap > 0) score += (0.2f * (overlap.toFloat() / myHobbies.size.coerceAtLeast(1))).coerceAtMost(0.2f)
        }
        return score
    }

    private fun computePhysical(me: UserEntity, candidate: UserEntity): Float {
        // Physical preferences are subjective; score based on completeness and matching criteria
        var score = 0.5f // Base: neutral when no data
        if (me.heightCm > 0 && candidate.heightCm > 0) {
            val diff = kotlin.math.abs(me.heightCm - candidate.heightCm)
            score = when {
                diff <= 10 -> 0.9f
                diff <= 20 -> 0.7f
                else -> 0.5f
            }
        }
        return score
    }

    private fun computePersonality(me: UserEntity, candidate: UserEntity): Float {
        if (me.personalityType.isBlank() || candidate.personalityType.isBlank()) return 0.5f
        // Same personality = good
        return if (me.personalityType.equals(candidate.personalityType, ignoreCase = true)) 0.9f else 0.5f
    }
}
