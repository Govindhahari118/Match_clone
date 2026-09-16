package com.match.app.core.matching

import com.match.app.domain.model.ReligionId
import com.match.app.domain.model.UserProfile
import com.match.app.domain.profile.ReligionFieldKey
import com.match.app.domain.profile.ReligionFieldRegistry

/**
 * Availability-aware compatibility signal for [UserProfile] recommendation results.
 *
 * This scorer intentionally excludes verification/premium status and does not reward missing data.
 * Astrology is an optional dimension only when it applies to both profiles and both profiles have
 * the required fields. All available dimensions are normalized by their participating weights.
 */
object MatchScorer {

    private data class Dimension(val weight: Float, val score: Float?)

    fun calculate(me: UserProfile, peer: UserProfile): Int {
        val astrologyApplicable = supportsAstrology(me) && supportsAstrology(peer)
        val questionnaire = questionnaireScore(me, peer)
        val astrology = astrologyScore(me, peer, astrologyApplicable)
        val religionCommunity = weightedAverage(
            0.50f to compare(me.religion, peer.religion),
            0.25f to compare(me.caste, peer.caste),
            0.25f to compare(me.motherTongue, peer.motherTongue)
        )
        val educationCareer = weightedAverage(
            0.45f to compare(me.education, peer.education),
            0.20f to compare(me.educationField, peer.educationField),
            0.20f to compare(me.occupationCategory, peer.occupationCategory),
            0.15f to compare(me.employerType, peer.employerType)
        )
        val location = weightedAverage(
            0.45f to compare(me.city, peer.city),
            0.25f to compare(me.state, peer.state),
            0.30f to compare(me.countryOfResidence, peer.countryOfResidence)
        )
        val family = weightedAverage(
            0.50f to compare(me.familyValues, peer.familyValues),
            0.30f to compare(me.familyType, peer.familyType),
            0.20f to compare(me.familyStatus, peer.familyStatus)
        )
        val lifestyle = weightedAverage(
            0.30f to compare(me.diet, peer.diet),
            0.25f to compare(me.smoking, peer.smoking),
            0.25f to compare(me.drinking, peer.drinking),
            0.20f to overlap(me.hobbies, peer.hobbies)
        )

        val dimensions = listOf(
            Dimension(0.40f, questionnaire),
            Dimension(if (astrologyApplicable) 0.20f else 0f, astrology),
            Dimension(0.15f, religionCommunity),
            Dimension(0.10f, educationCareer),
            Dimension(0.10f, location),
            Dimension(0.10f, family),
            Dimension(0.10f, lifestyle)
        )

        val availableWeight = dimensions.sumOf { if (it.score != null) it.weight.toDouble() else 0.0 }.toFloat()
        if (availableWeight <= 0f) return 0
        val weighted = dimensions.sumOf { ((it.score ?: 0f) * it.weight).toDouble() }.toFloat()
        return ((weighted / availableWeight).coerceIn(0f, 1f) * 100f).toInt()
    }

    private fun questionnaireScore(me: UserProfile, peer: UserProfile): Float? {
        val self = me.selfVector ?: return null
        val desired = me.partnerVector ?: return null
        val peerSelf = peer.selfVector ?: return null
        val peerDesired = peer.partnerVector ?: return null
        return Vectors.questionnaireScore(self, desired, peerSelf, peerDesired).coerceIn(0f, 1f)
    }

    private fun supportsAstrology(profile: UserProfile): Boolean {
        val religion = ReligionId.fromProfileValue(profile.religion) ?: return false
        val schema = ReligionFieldRegistry.schemaFor(religion)
        return schema.supports(ReligionFieldKey.RASHI) && schema.supports(ReligionFieldKey.NAKSHATRA)
    }

    private fun astrologyScore(me: UserProfile, peer: UserProfile, applicable: Boolean): Float? {
        if (!applicable) return null
        if (me.rasi.isBlank() || me.nakshatra.isBlank() || peer.rasi.isBlank() || peer.nakshatra.isBlank()) return null
        return Astrology.score(me.rasi, me.nakshatra, peer.rasi, peer.nakshatra).coerceIn(0f, 1f)
    }

    private fun compare(left: String, right: String): Float? {
        if (left.isBlank() || right.isBlank()) return null
        return if (left.trim().equals(right.trim(), ignoreCase = true)) 1f else 0f
    }

    private fun overlap(left: List<String>, right: List<String>): Float? {
        val a = left.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val b = right.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        if (a.isEmpty() || b.isEmpty()) return null
        val union = a union b
        return if (union.isEmpty()) null else (a intersect b).size.toFloat() / union.size.toFloat()
    }

    private fun weightedAverage(vararg pairs: Pair<Float, Float?>): Float? {
        val available = pairs.filter { it.second != null }
        if (available.isEmpty()) return null
        val totalWeight = available.sumOf { it.first.toDouble() }.toFloat()
        if (totalWeight <= 0f) return null
        val sum = available.sumOf { (weight, score) -> (weight * (score ?: 0f)).toDouble() }.toFloat()
        return (sum / totalWeight).coerceIn(0f, 1f)
    }
}
