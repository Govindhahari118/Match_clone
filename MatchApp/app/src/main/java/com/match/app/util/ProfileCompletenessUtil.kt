package com.match.app.util

import com.match.app.data.local.entity.UserEntity

/**
 * Calculates profile completeness as a Float between 0.0 and 1.0.
 *
 * Weighted sections:
 *  - Basic info (name, age, gender, city): 15%
 *  - Physical (height, weight, complexion): 10%
 *  - Education & career: 15%
 *  - Family: 15%
 *  - Religion & astrology: 15%
 *  - Lifestyle: 10%
 *  - Photo & media: 15%
 *  - About/bio: 5%
 */
object ProfileCompletenessUtil {

    fun calculate(user: UserEntity): Float {
        var score = 0f

        // Basic info (15%) — 5 fields
        score += sectionScore(0.15f, 5,
            user.displayName.isNotBlank(),
            user.age > 0,
            user.gender.isNotBlank(),
            user.city.isNotBlank(),
            user.dateOfBirth.isNotBlank()
        )

        // Physical (10%) — 4 fields
        score += sectionScore(0.10f, 4,
            user.heightCm > 0,
            user.weight > 0f,
            user.complexion.isNotBlank(),
            user.physicalStatus.isNotBlank()
        )

        // Education & career (15%) — 5 fields
        score += sectionScore(0.15f, 5,
            user.education.isNotBlank(),
            user.educationField.isNotBlank(),
            user.profession.isNotBlank(),
            user.occupationCategory.isNotBlank(),
            user.incomeBand.isNotBlank()
        )

        // Family (15%) — 5 fields
        score += sectionScore(0.15f, 5,
            user.familyType.isNotBlank(),
            user.familyStatus.isNotBlank(),
            user.familyValues.isNotBlank(),
            user.fatherOccupation.isNotBlank(),
            user.motherOccupation.isNotBlank()
        )

        // Religion & astrology (15%) — 5 fields
        score += sectionScore(0.15f, 5,
            user.religion.isNotBlank(),
            user.caste.isNotBlank(),
            user.rasi.isNotBlank(),
            user.nakshatra.isNotBlank(),
            user.gothra.isNotBlank()
        )

        // Lifestyle (10%) — 4 fields
        score += sectionScore(0.10f, 4,
            user.diet.isNotBlank(),
            user.smoking.isNotBlank(),
            user.drinking.isNotBlank(),
            user.hobbies.isNotBlank()
        )

        // Photo & media (15%) — 3 fields
        score += sectionScore(0.15f, 3,
            user.photoUrl.isNotBlank(),
            user.videoUrl.isNotBlank(),
            user.voiceBioUrl.isNotBlank()
        )

        // About/bio (5%) — 1 field
        score += sectionScore(0.05f, 1,
            user.bio.isNotBlank()
        )

        return score.coerceIn(0f, 1f)
    }

    private fun sectionScore(weight: Float, totalFields: Int, vararg filled: Boolean): Float {
        val filledCount = filled.count { it }
        return weight * filledCount.toFloat() / totalFields.toFloat()
    }
}
