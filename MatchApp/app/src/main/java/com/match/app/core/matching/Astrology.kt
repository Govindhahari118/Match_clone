package com.match.app.core.matching

/**
 * Astrology compatibility engine.
 *
 * Uses Rasi (moon sign, 12) and Nakshatra (27). We combine three classical factors,
 * each normalized to 0..1, and average them:
 *   - Rasi element compatibility (fire/earth/air/water).
 *   - Rasi polarity & modality alignment.
 *   - Nakshatra gana / yoni compatibility (simplified tiered matrix).
 *
 * This is a deterministic, fast, on-device scorer — no network, no third-party API.
 */
object Astrology {

    val RASIS = listOf(
        "Aries","Taurus","Gemini","Cancer","Leo","Virgo",
        "Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"
    )

    val NAKSHATRAS = listOf(
        "Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra","Punarvasu",
        "Pushya","Ashlesha","Magha","Purva Phalguni","Uttara Phalguni","Hasta",
        "Chitra","Swati","Vishakha","Anuradha","Jyeshtha","Mula","Purva Ashadha",
        "Uttara Ashadha","Shravana","Dhanishta","Shatabhisha","Purva Bhadrapada",
        "Uttara Bhadrapada","Revati"
    )

    private enum class Element { FIRE, EARTH, AIR, WATER }
    private enum class Modality { CARDINAL, FIXED, MUTABLE }

    private val rasiMeta: Map<String, Pair<Element, Modality>> = mapOf(
        "Aries" to (Element.FIRE to Modality.CARDINAL),
        "Taurus" to (Element.EARTH to Modality.FIXED),
        "Gemini" to (Element.AIR to Modality.MUTABLE),
        "Cancer" to (Element.WATER to Modality.CARDINAL),
        "Leo" to (Element.FIRE to Modality.FIXED),
        "Virgo" to (Element.EARTH to Modality.MUTABLE),
        "Libra" to (Element.AIR to Modality.CARDINAL),
        "Scorpio" to (Element.WATER to Modality.FIXED),
        "Sagittarius" to (Element.FIRE to Modality.MUTABLE),
        "Capricorn" to (Element.EARTH to Modality.CARDINAL),
        "Aquarius" to (Element.AIR to Modality.FIXED),
        "Pisces" to (Element.WATER to Modality.MUTABLE),
    )

    /** Gana classification for each Nakshatra. */
    private val gana: Map<String, String> = mapOf(
        "Ashwini" to "Deva", "Mrigashira" to "Deva", "Punarvasu" to "Deva",
        "Pushya" to "Deva", "Hasta" to "Deva", "Swati" to "Deva",
        "Anuradha" to "Deva", "Shravana" to "Deva", "Revati" to "Deva",
        "Bharani" to "Manushya", "Rohini" to "Manushya", "Ardra" to "Manushya",
        "Purva Phalguni" to "Manushya", "Uttara Phalguni" to "Manushya",
        "Purva Ashadha" to "Manushya", "Uttara Ashadha" to "Manushya",
        "Purva Bhadrapada" to "Manushya", "Uttara Bhadrapada" to "Manushya",
        "Krittika" to "Rakshasa", "Ashlesha" to "Rakshasa", "Magha" to "Rakshasa",
        "Chitra" to "Rakshasa", "Vishakha" to "Rakshasa", "Jyeshtha" to "Rakshasa",
        "Mula" to "Rakshasa", "Dhanishta" to "Rakshasa", "Shatabhisha" to "Rakshasa"
    )

    private fun elementScore(a: Element, b: Element): Float = when {
        a == b -> 1.0f
        (a == Element.FIRE && b == Element.AIR) || (a == Element.AIR && b == Element.FIRE) -> 0.85f
        (a == Element.EARTH && b == Element.WATER) || (a == Element.WATER && b == Element.EARTH) -> 0.85f
        (a == Element.FIRE && b == Element.WATER) || (a == Element.WATER && b == Element.FIRE) -> 0.25f
        (a == Element.EARTH && b == Element.AIR) || (a == Element.AIR && b == Element.EARTH) -> 0.35f
        else -> 0.55f
    }

    private fun modalityScore(a: Modality, b: Modality): Float = when {
        a == b && a == Modality.FIXED -> 0.6f        // fixed+fixed → stubborn
        a == b -> 0.8f
        else -> 0.9f                                 // complementary
    }

    private fun ganaScore(a: String, b: String): Float {
        val ga = gana[a] ?: return 0.5f
        val gb = gana[b] ?: return 0.5f
        return when {
            ga == gb && ga == "Deva" -> 1.0f
            ga == gb -> 0.85f
            (ga == "Deva" && gb == "Manushya") || (gb == "Deva" && ga == "Manushya") -> 0.8f
            (ga == "Manushya" && gb == "Rakshasa") || (gb == "Manushya" && ga == "Rakshasa") -> 0.3f
            (ga == "Deva" && gb == "Rakshasa") || (gb == "Deva" && ga == "Rakshasa") -> 0.2f
            else -> 0.5f
        }
    }

    /** Returns compatibility in 0..1 based on classical 10 Poruthams. */
    fun score(
        rasiA: String, nakshatraA: String,
        rasiB: String, nakshatraB: String
    ): Float {
        // Find indices
        val rA = RASIS.indexOf(rasiA).takeIf { it >= 0 } ?: return 0.5f
        val rB = RASIS.indexOf(rasiB).takeIf { it >= 0 } ?: return 0.5f
        val nA = NAKSHATRAS.indexOf(nakshatraA).takeIf { it >= 0 } ?: return 0.5f
        val nB = NAKSHATRAS.indexOf(nakshatraB).takeIf { it >= 0 } ?: return 0.5f

        var points = 0f
        var totalPossible = 36f

        // 1. Dina Porutham (Health/Longevity) - 3 pts
        val diff = (nB - nA + 27) % 9
        if (listOf(2, 4, 6, 8, 9, 0).contains(diff)) points += 3f

        // 2. Gana Porutham (Temperament) - 6 pts
        val ga = gana[nakshatraA] ?: ""
        val gb = gana[nakshatraB] ?: ""
        points += when {
            ga == gb -> 6f
            (ga == "Deva" && gb == "Manushya") || (gb == "Deva" && ga == "Manushya") -> 4f
            else -> 0f
        }

        // 3. Mahendra Porutham (Progeny) - 1 pt (Binary)
        val mDiff = (nB - nA + 27) % 27
        if (listOf(4, 7, 10, 13, 16, 19, 22, 25).contains(mDiff)) points += 1f

        // 4. Stree Deergha (Prosperity) - 1 pt
        if ((nB - nA + 27) % 27 > 13) points += 1f

        // 5. Yoni Porutham (Physical compatibility) - 4 pts
        // Simplified matrix
        points += if (nA % 4 == nB % 4) 4f else 2f

        // 6. Rasi Porutham (Unity) - 7 pts
        val rDiff = (rB - rA + 12) % 12
        if (listOf(0, 1, 6, 7, 8, 9, 10, 11).contains(rDiff)) points += 7f

        // 7. Rasi Adhipathi (Friendship) - 5 pts
        val (ea, _) = rasiMeta[rasiA]!!
        val (eb, _) = rasiMeta[rasiB]!!
        points += elementScore(ea, eb) * 5f

        // 8. Vasya Porutham (Attraction) - 2 pts
        if (rDiff == 6) points += 2f

        // 9. Rajju Porutham (Husband's longevity) - 5 pts
        // Very important in South India, simplified to avoid complex group mapping
        if (nA % 5 != nB % 5) points += 5f

        // 10. Vedha Porutham (Affliction) - 2 pts
        if (nA % 3 != nB % 3) points += 2f

        return (points / totalPossible).coerceIn(0f, 1f)
    }
}
