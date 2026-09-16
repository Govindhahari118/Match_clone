package com.match.app.core.matching

import com.match.app.data.local.entity.UserEntity
import com.match.app.domain.model.ReligionId
import com.match.app.domain.profile.ReligionFieldKey
import com.match.app.domain.profile.ReligionFieldRegistry

/**
 * Deterministic profile-similarity signal used by detailed compatibility UI.
 *
 * Important invariants:
 * - Missing data never receives a synthetic neutral score.
 * - Astrology contributes only when it is applicable to both profiles and both supplied the
 *   required astrology fields.
 * - The total is normalized across dimensions that can actually be compared.
 * - [ScoreBreakdown.coverage] communicates how much of the potentially applicable comparison
 *   data was available, so a high score based on sparse data is not presented as high confidence.
 * - Physical appearance is not scored until the product has explicit, consented partner
 *   preferences; raw height difference is not treated as compatibility.
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
        val personality: Float,
        val coverage: Float = 0f,
        val astrologyApplicable: Boolean = false
    )

    private data class WeightedDimension(
        val weight: Float,
        val score: Float?
    )

    fun compute(me: UserEntity, candidate: UserEntity): ScoreBreakdown {
        val astrologyApplicable = astrologyApplicable(me, candidate)
        val astrology = computeAstrology(me, candidate, astrologyApplicable)
        val religionCommunity = computeReligionCommunity(me, candidate)
        val educationCareer = computeEducationCareer(me, candidate)
        val location = computeLocation(me, candidate)
        val age = computeAge(me, candidate)
        val familyValues = computeFamilyValues(me, candidate)
        val lifestyle = computeLifestyle(me, candidate)
        val personality = computePersonality(me, candidate)

        val dimensions = listOf(
            WeightedDimension(if (astrologyApplicable) 0.20f else 0f, astrology),
            WeightedDimension(0.15f, religionCommunity),
            WeightedDimension(0.10f, educationCareer),
            WeightedDimension(0.10f, location),
            WeightedDimension(0.10f, age),
            WeightedDimension(0.10f, familyValues),
            WeightedDimension(0.10f, lifestyle),
            // Physical scoring is intentionally disabled until explicit preference data exists.
            WeightedDimension(0f, null),
            WeightedDimension(0.05f, personality)
        )

        val availableWeight = dimensions.sumOf { d -> if (d.score != null) d.weight.toDouble() else 0.0 }.toFloat()
        val potentialWeight = dimensions.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.0001f)
        val weightedScore = dimensions.sumOf { d ->
            ((d.score ?: 0f) * d.weight).toDouble()
        }.toFloat()
        val total = if (availableWeight > 0f) weightedScore / availableWeight else 0f
        val coverage = (availableWeight / potentialWeight).coerceIn(0f, 1f)

        return ScoreBreakdown(
            total = total.coerceIn(0f, 1f),
            astrology = astrology ?: 0f,
            religionCaste = religionCommunity ?: 0f,
            educationCareer = educationCareer ?: 0f,
            location = location ?: 0f,
            age = age,
            familyValues = familyValues ?: 0f,
            lifestyle = lifestyle ?: 0f,
            physical = 0f,
            personality = personality ?: 0f,
            coverage = coverage,
            astrologyApplicable = astrologyApplicable
        )
    }

    private fun astrologyApplicable(me: UserEntity, candidate: UserEntity): Boolean {
        fun supportsAstrology(user: UserEntity): Boolean {
            val religion = ReligionId.fromProfileValue(user.religion) ?: return false
            val schema = ReligionFieldRegistry.schemaFor(religion)
            return schema.supports(ReligionFieldKey.RASHI) &&
                schema.supports(ReligionFieldKey.NAKSHATRA)
        }
        return supportsAstrology(me) && supportsAstrology(candidate)
    }

    private fun computeAstrology(
        me: UserEntity,
        candidate: UserEntity,
        applicable: Boolean
    ): Float? {
        if (!applicable) return null
        if (me.nakshatra.isBlank() || candidate.nakshatra.isBlank()) return null
        if (me.rasi.isBlank() || candidate.rasi.isBlank()) return null
        val result = TenPorutham.calculate(
            nakshatraBride = me.nakshatra,
            rasiBride = me.rasi,
            nakshatraGroom = candidate.nakshatra,
            rasiGroom = candidate.rasi
        )
        return (result.score.toFloat() / 10f).coerceIn(0f, 1f)
    }

    private fun computeReligionCommunity(me: UserEntity, candidate: UserEntity): Float? =
        weightedAverage(
            0.50f to compareStrings(me.religion, candidate.religion),
            0.30f to compareStrings(me.caste, candidate.caste),
            0.20f to compareStrings(me.motherTongue, candidate.motherTongue)
        )

    private fun computeEducationCareer(me: UserEntity, candidate: UserEntity): Float? =
        weightedAverage(
            0.40f to compareStrings(me.education, candidate.education),
            0.20f to compareStrings(me.educationField, candidate.educationField),
            0.20f to compareStrings(me.occupationCategory, candidate.occupationCategory),
            0.20f to compareStrings(me.incomeBand, candidate.incomeBand)
        )

    private fun computeLocation(me: UserEntity, candidate: UserEntity): Float? {
        val cityOrState = when {
            me.city.isNotBlank() && candidate.city.isNotBlank() ->
                if (me.city.equals(candidate.city, ignoreCase = true)) 1f
                else if (me.state.isNotBlank() && candidate.state.isNotBlank() &&
                    me.state.equals(candidate.state, ignoreCase = true)) 0.5f else 0f
            me.state.isNotBlank() && candidate.state.isNotBlank() ->
                if (me.state.equals(candidate.state, ignoreCase = true)) 1f else 0f
            else -> null
        }
        return weightedAverage(
            0.60f to cityOrState,
            0.40f to compareStrings(me.countryOfResidence, candidate.countryOfResidence)
        )
    }

    private fun computeAge(me: UserEntity, candidate: UserEntity): Float {
        val diff = kotlin.math.abs(me.age - candidate.age)
        return when {
            diff <= 2 -> 1.0f
            diff <= 4 -> 0.85f
            diff <= 6 -> 0.70f
            diff <= 8 -> 0.50f
            diff <= 10 -> 0.30f
            else -> 0.10f
        }
    }

    private fun computeFamilyValues(me: UserEntity, candidate: UserEntity): Float? =
        weightedAverage(
            0.40f to compareStrings(me.familyValues, candidate.familyValues),
            0.30f to compareStrings(me.familyType, candidate.familyType),
            0.30f to compareStrings(me.familyStatus, candidate.familyStatus)
        )

    private fun computeLifestyle(me: UserEntity, candidate: UserEntity): Float? {
        val hobbyScore = if (me.hobbies.isBlank() || candidate.hobbies.isBlank()) {
            null
        } else {
            val mine = me.hobbies.split(',').map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            val theirs = candidate.hobbies.split(',').map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
            if (mine.isEmpty() || theirs.isEmpty()) null
            else mine.intersect(theirs).size.toFloat() / mine.union(theirs).size.toFloat().coerceAtLeast(1f)
        }
        return weightedAverage(
            0.30f to compareStrings(me.diet, candidate.diet),
            0.25f to compareStrings(me.smoking, candidate.smoking),
            0.25f to compareStrings(me.drinking, candidate.drinking),
            0.20f to hobbyScore
        )
    }

    private fun computePersonality(me: UserEntity, candidate: UserEntity): Float? =
        compareStrings(me.personalityType, candidate.personalityType)

    private fun compareStrings(left: String, right: String): Float? {
        if (left.isBlank() || right.isBlank()) return null
        return if (left.trim().equals(right.trim(), ignoreCase = true)) 1f else 0f
    }

    private fun weightedAverage(vararg values: Pair<Float, Float?>): Float? {
        val available = values.filter { it.second != null }
        if (available.isEmpty()) return null
        val weight = available.sumOf { it.first.toDouble() }.toFloat()
        if (weight <= 0f) return null
        val weighted = available.sumOf { (w, score) -> (w * (score ?: 0f)).toDouble() }.toFloat()
        return (weighted / weight).coerceIn(0f, 1f)
    }
}
