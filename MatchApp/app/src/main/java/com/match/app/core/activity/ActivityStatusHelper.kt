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
            return ActivityStatus("Activity unavailable", isOnline = false, isRecent = false, daysAgo = -1)
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

    /**
     * Display only the backend-authoritative weighted aggregate. The server includes approved
     * photo, partner-preference and verification evidence that is not all duplicated into this
     * local profile object.
     */
    fun profileCompleteness(p: UserProfile): Int =
        (p.profileCompleteness.coerceIn(0f, 1f) * 100f).toInt().coerceIn(0, 100)

    /**
     * Visible section hints only. These are not a second scoring formula; the progress percentage
     * remains server-owned.
     */
    fun completenessItems(p: UserProfile): List<Pair<String, Boolean>> = listOf(
        "Basics" to (
            p.username.isNotBlank() &&
                p.displayName.isNotBlank() &&
                p.dateOfBirth.isNotBlank() &&
                p.state.isNotBlank() &&
                p.city.isNotBlank() &&
                p.motherTongue.isNotBlank() &&
                p.religion.isNotBlank() &&
                p.heightCm > 0 &&
                p.maritalStatus.isNotBlank()
            ),
        "Photo" to p.photoUrl.isNotBlank(),
        "Education" to (p.education.isNotBlank() && p.profession.isNotBlank()),
        "Family" to (
            p.familyType.isNotBlank() &&
                p.familyValues.isNotBlank() &&
                p.aboutFamily.isNotBlank()
            ),
        "Lifestyle" to (
            p.diet.isNotBlank() &&
                p.smoking.isNotBlank() &&
                p.drinking.isNotBlank()
            ),
        "About me" to p.bio.isNotBlank(),
        "Verification" to p.isVerified
    )

}
