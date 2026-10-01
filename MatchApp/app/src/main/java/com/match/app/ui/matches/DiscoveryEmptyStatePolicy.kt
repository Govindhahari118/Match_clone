package com.match.app.ui.matches

import com.match.app.domain.model.MatchFilter

internal data class DiscoveryEmptyState(
    val message: String,
    val actionLabel: String?,
    val action: Action
) {
    enum class Action { NONE, RESET, INCLUDE_NO_PHOTO, RELAX_MUTUAL_MATCH }
}

internal object DiscoveryEmptyStatePolicy {
    fun resolve(filter: MatchFilter): DiscoveryEmptyState {
        if (filter.minMutualMatchPercent >= 90) {
            return DiscoveryEmptyState(
                message = "No profiles currently meet your ${filter.minMutualMatchPercent}%+ reciprocal preference threshold. Your strict preferences have not been changed.",
                actionLabel = "Show other eligible matches",
                action = DiscoveryEmptyState.Action.RELAX_MUTUAL_MATCH
            )
        }

        val reasons = buildList {
            if (filter.keyword.isNotBlank()) add("keyword")
            if (filter.ageMin != 18 || filter.ageMax != 70) add("age")
            if (filter.city.isNotBlank() || filter.state.isNotBlank() || filter.nativeState.isNotBlank()) {
                add("location")
            }
            if (
                filter.religion.isNotBlank() ||
                filter.caste.isNotBlank() ||
                filter.subCaste.isNotBlank() ||
                filter.motherTongue.isNotBlank()
            ) add("community")
            if (
                filter.educationLevel.isNotBlank() ||
                filter.educationField.isNotBlank() ||
                filter.occupationCategory.isNotBlank() ||
                filter.employerType.isNotBlank()
            ) add("education/career")
            if (filter.verifiedOnly || filter.verifiedLevel > 0) add("verification")
            if (filter.premiumOnly) add("membership")
            if (filter.lastActiveWithinDays > 0) add("activity")
            if (
                filter.incomeMin.isNotBlank() ||
                filter.incomeMax.isNotBlank() ||
                filter.diet.isNotBlank() ||
                filter.smoking.isNotBlank() ||
                filter.drinking.isNotBlank() ||
                filter.familyType.isNotBlank() ||
                filter.familyStatus.isNotBlank() ||
                filter.physicalStatus.isNotBlank()
            ) add("lifestyle/family")
            if (
                filter.rasi.isNotBlank() ||
                filter.nakshatra.isNotBlank() ||
                filter.manglik.isNotBlank() ||
                filter.hasHoroscope.isNotBlank()
            ) add("astrology")
        }

        if (reasons.isNotEmpty()) {
            val shown = reasons.distinct().take(4).joinToString(", ")
            return DiscoveryEmptyState(
                message = "No profiles matched your current $shown settings. Reset or widen them to search a broader eligible set.",
                actionLabel = "Reset search settings",
                action = DiscoveryEmptyState.Action.RESET
            )
        }

        if (filter.withPhotoOnly) {
            return DiscoveryEmptyState(
                message = "No eligible profiles with a photo are available in this result set. You can include profiles without a photo.",
                actionLabel = "Include profiles without photos",
                action = DiscoveryEmptyState.Action.INCLUDE_NO_PHOTO
            )
        }

        return DiscoveryEmptyState(
            message = "No eligible profiles are available in this result set right now. Try refreshing later.",
            actionLabel = null,
            action = DiscoveryEmptyState.Action.NONE
        )
    }
}
