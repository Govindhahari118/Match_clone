package com.match.app.core.matching

import com.match.app.domain.model.ReligionCategory
import com.match.app.domain.model.UserProfile
import kotlin.math.abs

/**
 * Explainable, symmetric compatibility score used by production discovery.
 *
 * v4 keeps applicability-aware astrology and mutual trust, and uses the backend-derived reciprocal
 * partner-preference fit as an optional pair-specific factor. Raw partner preferences never enter
 * this client scorer; only the trusted 0..1 pair aggregate is accepted.
 *
 * Missing/inapplicable dimensions are excluded and the remaining configured weights are
 * renormalized. This prevents absent astrology/questionnaire data from becoming a fabricated
 * compatibility penalty or neutral bonus.
 */
object MatchScorer {

    const val FORMULA_VERSION = "match-v4-reciprocal-preferences"

    data class Factor(
        val key: String,
        val score: Float,
        val configuredWeight: Float
    )

    data class Result(
        val percentage: Int,
        val formulaVersion: String,
        val factors: List<Factor>,
        val agePenalty: Float
    )

    fun calculate(me: UserProfile, peer: UserProfile): Int = explain(me, peer).percentage

    fun explain(
        me: UserProfile,
        peer: UserProfile,
        bilateralPreferenceFit: Float? = null
    ): Result {
        val factors = mutableListOf<Factor>()

        bilateralPreferenceFit?.let {
            factors += Factor("bilateral_preferences", it.coerceIn(0f, 1f), 0.35f)
        }

        questionnaireScore(me, peer)?.let {
            factors += Factor("questionnaire", it, 0.25f)
        }

        if (astrologyApplicable(me, peer)) {
            Astrology.scoreOrNull(me.rasi, me.nakshatra, peer.rasi, peer.nakshatra)?.let {
                factors += Factor("astrology", it, 0.15f)
            }
        }

        demographicsScore(me, peer)?.let {
            factors += Factor("demographics_lifestyle", it, 0.15f)
        }

        val trust = (
            verificationScore(me.verificationLevel) +
                verificationScore(peer.verificationLevel)
            ) / 2f
        factors += Factor("mutual_trust", trust, 0.10f)

        val activeWeight = factors.sumOf { it.configuredWeight.toDouble() }.toFloat()
        val weighted = if (activeWeight > 0f) {
            factors.sumOf {
                (it.score.coerceIn(0f, 1f) * it.configuredWeight).toDouble()
            }.toFloat() / activeWeight
        } else {
            0f
        }

        val agePenalty = if (abs(me.age - peer.age) > 10) 0.10f else 0f
        val normalized = (weighted - agePenalty).coerceIn(0f, 1f)
        return Result(
            percentage = (normalized * 100f).toInt().coerceIn(0, 100),
            formulaVersion = FORMULA_VERSION,
            factors = factors.toList(),
            agePenalty = agePenalty
        )
    }

    private fun questionnaireScore(me: UserProfile, peer: UserProfile): Float? {
        val meSelf = me.selfVector ?: return null
        val mePartner = me.partnerVector ?: return null
        val peerSelf = peer.selfVector ?: return null
        val peerPartner = peer.partnerVector ?: return null
        return Vectors.questionnaireScore(meSelf, mePartner, peerSelf, peerPartner)
    }

    private fun astrologyApplicable(me: UserProfile, peer: UserProfile): Boolean {
        return ReligionCategory.fromReligion(me.religion) == ReligionCategory.HINDU &&
            ReligionCategory.fromReligion(peer.religion) == ReligionCategory.HINDU &&
            Astrology.isValid(me.rasi, me.nakshatra) &&
            Astrology.isValid(peer.rasi, peer.nakshatra)
    }

    /**
     * Uses only dimensions known for both profiles. The old unconditional 0.5 base was removed:
     * unknown demographic data is now omitted rather than pretending to be a 50% match.
     */
    private fun demographicsScore(me: UserProfile, peer: UserProfile): Float? {
        var weighted = 0f
        var weight = 0f

        if (me.education.isNotBlank() && peer.education.isNotBlank()) {
            weighted += if (me.education.equals(peer.education, true)) 0.40f else 0f
            weight += 0.40f
        }
        if (me.occupationCategory.isNotBlank() && peer.occupationCategory.isNotBlank()) {
            weighted += if (me.occupationCategory.equals(peer.occupationCategory, true)) 0.40f else 0f
            weight += 0.40f
        }
        if (me.diet.isNotBlank() && peer.diet.isNotBlank()) {
            weighted += if (me.diet.equals(peer.diet, true)) 0.20f else 0f
            weight += 0.20f
        }

        return if (weight > 0f) (weighted / weight).coerceIn(0f, 1f) else null
    }

    private fun verificationScore(level: Int): Float =
        (level.toFloat() / 5f).coerceIn(0f, 1f)
}
