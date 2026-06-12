package com.match.app.core.matching

/**
 * 10-Porutham (Telugu: పొంతనాలు) traditional compatibility system.
 *
 * The 10 Poruthams are classical South Indian marriage compatibility checks.
 * Each Porutham has a max score; total is 10 points (1 per Porutham).
 * A score of 7+ out of 10 is considered auspicious for marriage.
 *
 * Poruthams:
 *  1. Dina (Star) — Nakshatra day compatibility
 *  2. Gana — Temperament compatibility (Deva/Manushya/Rakshasa)
 *  3. Mahendra — Prosperity and progeny
 *  4. Stree Deergha — Comfortable married life
 *  5. Yoni — Physical and sexual compatibility (animal symbols)
 *  6. Rasi — Rasi (moon sign) compatibility
 *  7. Rasi Adhipathi (Rashyadhipati) — Rasi lord compatibility
 *  8. Vasya — Mutual attraction and control
 *  9. Rajju — Longevity of marriage
 * 10. Vedha — Obstacles / afflictions
 */
object TenPorutham {

    data class PoruthamResult(
        val name: String,
        val teluguName: String,
        val matched: Boolean,
        val description: String
    )

    data class FullResult(
        val poruthams: List<PoruthamResult>,
        val score: Int, // 0-10
        val totalPossible: Int = 10,
        val verdict: String // "Excellent", "Good", "Average", "Poor"
    )

    // Nakshatra to index (0-26)
    private val nakshatraIndex: Map<String, Int> = Astrology.NAKSHATRAS.withIndex()
        .associate { (i, n) -> n to i }

    // Rasi to index (0-11)
    private val rasiIndex: Map<String, Int> = Astrology.RASIS.withIndex()
        .associate { (i, r) -> r to i }

    // Gana classification
    private val ganaMap = mapOf(
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

    // Yoni (animal) for each Nakshatra
    private val yoniMap = mapOf(
        "Ashwini" to "Horse", "Bharani" to "Elephant", "Krittika" to "Goat",
        "Rohini" to "Serpent", "Mrigashira" to "Serpent", "Ardra" to "Dog",
        "Punarvasu" to "Cat", "Pushya" to "Goat", "Ashlesha" to "Cat",
        "Magha" to "Rat", "Purva Phalguni" to "Rat", "Uttara Phalguni" to "Cow",
        "Hasta" to "Buffalo", "Chitra" to "Tiger", "Swati" to "Buffalo",
        "Vishakha" to "Tiger", "Anuradha" to "Deer", "Jyeshtha" to "Deer",
        "Mula" to "Dog", "Purva Ashadha" to "Monkey", "Uttara Ashadha" to "Mongoose",
        "Shravana" to "Monkey", "Dhanishta" to "Lion", "Shatabhisha" to "Horse",
        "Purva Bhadrapada" to "Lion", "Uttara Bhadrapada" to "Cow", "Revati" to "Elephant"
    )

    // Enemy yoni pairs
    private val yoniEnemies = setOf(
        setOf("Horse", "Buffalo"), setOf("Elephant", "Lion"), setOf("Goat", "Monkey"),
        setOf("Serpent", "Mongoose"), setOf("Dog", "Deer"), setOf("Cat", "Rat"),
        setOf("Cow", "Tiger")
    )

    // Vasya classification by rasi
    private val vasyaMap = mapOf(
        "Aries" to "Chatushpada", "Taurus" to "Chatushpada",
        "Leo" to "Chatushpada", "Sagittarius" to "Chatushpada",  // first half
        "Capricorn" to "Chatushpada",  // first half
        "Gemini" to "Dwipada", "Virgo" to "Dwipada",
        "Libra" to "Dwipada", "Aquarius" to "Dwipada",  // first half
        "Cancer" to "Jalchar", "Pisces" to "Jalchar",
        "Scorpio" to "Keeta"
    )

    // Rasi lords
    private val rasiLord = mapOf(
        "Aries" to "Mars", "Taurus" to "Venus", "Gemini" to "Mercury",
        "Cancer" to "Moon", "Leo" to "Sun", "Virgo" to "Mercury",
        "Libra" to "Venus", "Scorpio" to "Mars", "Sagittarius" to "Jupiter",
        "Capricorn" to "Saturn", "Aquarius" to "Saturn", "Pisces" to "Jupiter"
    )

    // Friendly planet pairs
    private val friendlyPlanets = setOf(
        setOf("Sun", "Moon"), setOf("Sun", "Mars"), setOf("Sun", "Jupiter"),
        setOf("Moon", "Mars"), setOf("Moon", "Jupiter"), setOf("Moon", "Mercury"),
        setOf("Mars", "Jupiter"), setOf("Mercury", "Venus"), setOf("Jupiter", "Mars"),
        setOf("Venus", "Saturn"), setOf("Saturn", "Mercury"), setOf("Saturn", "Venus")
    )

    // Rajju classification
    private val rajjuMap = mapOf(
        "Ashwini" to "Paada", "Ashlesha" to "Paada", "Magha" to "Paada",
        "Jyeshtha" to "Paada", "Mula" to "Paada", "Revati" to "Paada",
        "Bharani" to "Ooru", "Pushya" to "Ooru", "Purva Phalguni" to "Ooru",
        "Anuradha" to "Ooru", "Purva Ashadha" to "Ooru", "Uttara Bhadrapada" to "Ooru",
        "Krittika" to "Nabhi", "Punarvasu" to "Nabhi", "Uttara Phalguni" to "Nabhi",
        "Vishakha" to "Nabhi", "Uttara Ashadha" to "Nabhi", "Purva Bhadrapada" to "Nabhi",
        "Rohini" to "Kanta", "Ardra" to "Kanta", "Hasta" to "Kanta",
        "Swati" to "Kanta", "Shravana" to "Kanta", "Shatabhisha" to "Kanta",
        "Mrigashira" to "Siro", "Chitra" to "Siro", "Dhanishta" to "Siro"
    )

    // Vedha (obstruction) pairs — nakshatra indices
    private val vedhaPairs = listOf(
        0 to 17, 1 to 16, 2 to 15, 3 to 14, 4 to 22,
        5 to 21, 6 to 20, 7 to 19, 8 to 18, 9 to 26,
        10 to 25, 11 to 24, 12 to 23
    )

    fun calculate(
        nakshatraBride: String, rasiBride: String,
        nakshatraGroom: String, rasiGroom: String
    ): FullResult {
        val results = mutableListOf<PoruthamResult>()

        // 1. Dina Porutham
        results.add(checkDina(nakshatraBride, nakshatraGroom))
        // 2. Gana Porutham
        results.add(checkGana(nakshatraBride, nakshatraGroom))
        // 3. Mahendra Porutham
        results.add(checkMahendra(nakshatraBride, nakshatraGroom))
        // 4. Stree Deergha
        results.add(checkStreeDeergha(nakshatraBride, nakshatraGroom))
        // 5. Yoni Porutham
        results.add(checkYoni(nakshatraBride, nakshatraGroom))
        // 6. Rasi Porutham
        results.add(checkRasi(rasiBride, rasiGroom))
        // 7. Rasi Adhipathi
        results.add(checkRasiAdhipathi(rasiBride, rasiGroom))
        // 8. Vasya Porutham
        results.add(checkVasya(rasiBride, rasiGroom))
        // 9. Rajju Porutham
        results.add(checkRajju(nakshatraBride, nakshatraGroom))
        // 10. Vedha Porutham
        results.add(checkVedha(nakshatraBride, nakshatraGroom))

        val score = results.count { it.matched }
        val verdict = when {
            score >= 8 -> "Excellent"
            score >= 6 -> "Good"
            score >= 4 -> "Average"
            else -> "Poor"
        }

        return FullResult(poruthams = results, score = score, verdict = verdict)
    }

    private fun checkDina(brideNak: String, groomNak: String): PoruthamResult {
        val bIdx = nakshatraIndex[brideNak] ?: 0
        val gIdx = nakshatraIndex[groomNak] ?: 0
        val diff = ((gIdx - bIdx + 27) % 27) + 1
        // Dina matches if the count from bride to groom's nakshatra (mod 9) is not 2,4,6,8
        val remainder = diff % 9
        val matched = remainder !in listOf(2, 4, 6, 8)
        return PoruthamResult("Dina", "దినం", matched,
            if (matched) "Day compatibility is favorable" else "Day compatibility has dosham")
    }

    private fun checkGana(brideNak: String, groomNak: String): PoruthamResult {
        val gB = ganaMap[brideNak] ?: "Manushya"
        val gG = ganaMap[groomNak] ?: "Manushya"
        val matched = when {
            gB == gG -> true
            gB == "Deva" && gG == "Manushya" -> true
            gG == "Deva" && gB == "Manushya" -> true
            else -> false // Rakshasa with non-Rakshasa = no match
        }
        return PoruthamResult("Gana", "గణం", matched,
            if (matched) "Temperament compatibility present" else "Temperament mismatch (Gana Dosham)")
    }

    private fun checkMahendra(brideNak: String, groomNak: String): PoruthamResult {
        val bIdx = nakshatraIndex[brideNak] ?: 0
        val gIdx = nakshatraIndex[groomNak] ?: 0
        val diff = ((gIdx - bIdx + 27) % 27) + 1
        // Mahendra matches if count is 4,7,10,13,16,19,22,25
        val matched = diff % 3 == 1 && diff > 1
        return PoruthamResult("Mahendra", "మహేంద్ర", matched,
            if (matched) "Prosperity and progeny favorable" else "Mahendra porutham not present")
    }

    private fun checkStreeDeergha(brideNak: String, groomNak: String): PoruthamResult {
        val bIdx = nakshatraIndex[brideNak] ?: 0
        val gIdx = nakshatraIndex[groomNak] ?: 0
        val diff = ((gIdx - bIdx + 27) % 27) + 1
        // Stree Deergha: count from bride's to groom's nakshatra > 13
        val matched = diff > 13
        return PoruthamResult("Stree Deergha", "స్త్రీ దీర్ఘ", matched,
            if (matched) "Long comfortable married life indicated" else "Stree Deergha not met (can be compensated by other poruthams)")
    }

    private fun checkYoni(brideNak: String, groomNak: String): PoruthamResult {
        val yB = yoniMap[brideNak] ?: "Unknown"
        val yG = yoniMap[groomNak] ?: "Unknown"
        val isEnemy = yoniEnemies.any { it == setOf(yB, yG) }
        val matched = !isEnemy
        return PoruthamResult("Yoni", "యోని", matched,
            if (matched) "Physical compatibility present" else "Yoni enemies — physical incompatibility")
    }

    private fun checkRasi(rasiBride: String, rasiGroom: String): PoruthamResult {
        val bIdx = rasiIndex[rasiBride] ?: 0
        val gIdx = rasiIndex[rasiGroom] ?: 0
        val diff = ((gIdx - bIdx + 12) % 12) + 1
        // Rasi matches if groom's is 2,3,4,5,6 signs from bride, or same
        val matched = diff in listOf(1, 2, 3, 4, 5, 6, 7)
        return PoruthamResult("Rasi", "రాశి", matched,
            if (matched) "Moon sign compatibility favorable" else "Rasi not compatible (6-8 position)")
    }

    private fun checkRasiAdhipathi(rasiBride: String, rasiGroom: String): PoruthamResult {
        val lordB = rasiLord[rasiBride] ?: "Unknown"
        val lordG = rasiLord[rasiGroom] ?: "Unknown"
        val matched = when {
            lordB == lordG -> true // Same lord = always compatible
            friendlyPlanets.any { it == setOf(lordB, lordG) } -> true
            else -> false
        }
        return PoruthamResult("Rasi Adhipathi", "రాశ్యాధిపతి", matched,
            if (matched) "Rasi lords are friendly" else "Rasi lords are enemies")
    }

    private fun checkVasya(rasiBride: String, rasiGroom: String): PoruthamResult {
        val vB = vasyaMap[rasiBride] ?: "Dwipada"
        val vG = vasyaMap[rasiGroom] ?: "Dwipada"
        // Vasya matches if same group or compatible groups
        val matched = vB == vG || (vB == "Dwipada" || vG == "Dwipada")
        return PoruthamResult("Vasya", "వశ్య", matched,
            if (matched) "Mutual attraction and harmony present" else "Vasya not compatible")
    }

    private fun checkRajju(brideNak: String, groomNak: String): PoruthamResult {
        val rB = rajjuMap[brideNak] ?: "Nabhi"
        val rG = rajjuMap[groomNak] ?: "Nabhi"
        // Rajju is GOOD when bride and groom are NOT in same rajju
        val matched = rB != rG
        return PoruthamResult("Rajju", "రజ్జు", matched,
            if (matched) "Longevity of marriage is favorable" else "Same Rajju — Rajju Dosham present (important!)")
    }

    private fun checkVedha(brideNak: String, groomNak: String): PoruthamResult {
        val bIdx = nakshatraIndex[brideNak] ?: 0
        val gIdx = nakshatraIndex[groomNak] ?: 0
        // Vedha occurs if bride and groom nakshatras form an obstruction pair
        val hasVedha = vedhaPairs.any { (a, b) ->
            (bIdx == a && gIdx == b) || (bIdx == b && gIdx == a)
        }
        val matched = !hasVedha
        return PoruthamResult("Vedha", "వేధ", matched,
            if (matched) "No obstructions present" else "Vedha Dosham — obstruction pair detected")
    }
}
