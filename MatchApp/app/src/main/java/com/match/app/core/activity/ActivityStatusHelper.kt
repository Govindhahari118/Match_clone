package com.match.app.core.activity

import com.match.app.domain.model.UserProfile

/** Converts trusted server activity timestamps to privacy-aware user-facing labels. */
object ActivityStatusHelper {

    data class ActivityStatus(
        val label: String,
        val isOnline: Boolean,
        val isRecent: Boolean,
        val daysAgo: Int
    )

    fun from(profile: UserProfile): ActivityStatus {
        if (!profile.showLastActive) {
            return ActivityStatus("Last active hidden", isOnline = false, isRecent = false, daysAgo = -1)
        }
        return from(profile.lastActiveAt)
    }

    fun from(lastActiveAt: Long, now: Long = System.currentTimeMillis()): ActivityStatus {
        if (lastActiveAt <= 0L) {
            return ActivityStatus("Active recently", isOnline = false, isRecent = true, daysAgo = -1)
        }
        val diffMs = (now - lastActiveAt).coerceAtLeast(0L)
        val minutes = diffMs / 60_000L
        val hours = diffMs / 3_600_000L
        val days = (diffMs / 86_400_000L).toInt()
        return when {
            minutes < 5 -> ActivityStatus("Online now", isOnline = true, isRecent = true, daysAgo = 0)
            minutes < 60 -> ActivityStatus("Active ${minutes.coerceAtLeast(5)} min ago", isOnline = false, isRecent = true, daysAgo = 0)
            hours < 24 -> ActivityStatus("Active $hours ${if (hours == 1L) "hour" else "hours"} ago", isOnline = false, isRecent = true, daysAgo = 0)
            days == 1 -> ActivityStatus("Active yesterday", isOnline = false, isRecent = true, daysAgo = 1)
            days < 7 -> ActivityStatus("Active $days days ago", isOnline = false, isRecent = true, daysAgo = days)
            days < 30 -> {
                val weeks = (days / 7).coerceAtLeast(1)
                ActivityStatus("Active $weeks ${if (weeks == 1) "week" else "weeks"} ago", isOnline = false, isRecent = true, daysAgo = days)
            }
            days < 90 -> {
                val months = (days / 30).coerceAtLeast(1)
                ActivityStatus("Active $months ${if (months == 1) "month" else "months"} ago", isOnline = false, isRecent = false, daysAgo = days)
            }
            else -> ActivityStatus("Inactive", isOnline = false, isRecent = false, daysAgo = days)
        }
    }

    /** Completeness score (0–100) used for sorting and profile nudges. */
    fun profileCompleteness(p: UserProfile): Int {
        var score = 0
        if (p.displayName.isNotBlank()) score += 8
        if (p.bio.isNotBlank()) score += 6
        if (p.city.isNotBlank()) score += 4
        if (p.primaryPhotoPath != null || p.photoUrl.isNotBlank()) score += 12
        if (p.hasQuestionnaire) score += 10
        if (p.religion.isNotBlank()) score += 3
        if (p.caste.isNotBlank()) score += 2
        if (p.education.isNotBlank()) score += 3
        if (p.profession.isNotBlank()) score += 3
        if (p.incomeBand.isNotBlank()) score += 2
        if (p.heightCm > 0) score += 2
        if (p.maritalStatus.isNotBlank()) score += 3
        if (p.motherTongue.isNotBlank()) score += 2
        if (p.dateOfBirth.isNotBlank()) score += 4
        if (p.rasi.isNotBlank()) score += 3
        if (p.nakshatra.isNotBlank()) score += 3
        if (p.weight > 0f) score += 2
        if (p.employer.isNotBlank()) score += 3
        if (p.familyType.isNotBlank()) score += 2
        if (p.fitnessActivities.isNotBlank()) score += 2
        if (p.voiceBioUrl.isNotBlank()) score += 4
        if (p.manglik.isNotBlank()) score += 2
        if (p.phoneNumber.isNotBlank()) score += 5
        if (p.verificationLevel >= 2) score += 6
        if (p.matrimonyId.isNotBlank()) score += 4
        return score.coerceIn(0, 100)
    }

    fun completenessItems(p: UserProfile): List<Pair<String, Boolean>> = listOf(
        "Profile photo" to (p.primaryPhotoPath != null || p.photoUrl.isNotBlank()),
        "Questionnaire" to p.hasQuestionnaire,
        "Bio" to p.bio.isNotBlank(),
        "City" to p.city.isNotBlank(),
        "Date of birth" to p.dateOfBirth.isNotBlank(),
        "Rasi / Nakshatra" to (p.rasi.isNotBlank() && p.nakshatra.isNotBlank()),
        "Religion" to p.religion.isNotBlank(),
        "Education" to p.education.isNotBlank(),
        "Profession" to p.profession.isNotBlank(),
        "Employer" to p.employer.isNotBlank(),
        "Income" to p.incomeBand.isNotBlank(),
        "Height & weight" to (p.heightCm > 0 && p.weight > 0f),
        "Mother tongue" to p.motherTongue.isNotBlank(),
        "Marital status" to p.maritalStatus.isNotBlank(),
        "Family type" to p.familyType.isNotBlank(),
        "Voice bio" to p.voiceBioUrl.isNotBlank(),
        "Phone number" to p.phoneNumber.isNotBlank(),
        "ID verification" to (p.verificationLevel >= 2)
    )
}
