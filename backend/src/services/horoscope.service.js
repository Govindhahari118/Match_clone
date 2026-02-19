const calculateCompatibility = (profileA, profileB) => {
    // Mock Gun Milan logic
    // In a real app, this would use an astrological library or API like Prokerala/Astrosage

    if (!profileA.zodiacSign || !profileB.zodiacSign) {
        return { score: 0, outOf: 36, tier: "Incomplete Data" };
    }

    // Mock scores based on zodiac compatibility matrix
    const zodiacs = ["Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"];

    // Generate a consistent pseudo-random score based on names/signs so it doesn't change on refresh
    const seed = (profileA.firstName.length + profileB.firstName.length + zodiacs.indexOf(profileA.zodiacSign)) % 36;
    let score = 18 + (seed % 18); // Ensuring score between 18 and 36 for demo purposes

    // Enhance score if Nakshatras match (demo logic)
    if (profileA.nakshatra && profileB.nakshatra && profileA.nakshatra === profileB.nakshatra) {
        score = Math.min(36, score + 5);
    }

    let tier = "Average";
    if (score > 28) tier = "Excellent Match";
    else if (score > 18) tier = "Good Match";
    else tier = "Average Match";

    return {
        score,
        outOf: 36,
        tier,
        details: {
            varna: "Good",
            vasya: "Compatible",
            tara: "Excellent",
            yoni: "Average",
            grahaMaitri: "Good",
            gana: "Excellent",
            bhakoot: "Average",
            nadi: "Warning (Nadi Dosha Check Required)" // Classic Indian match warning for realism
        }
    };
};

module.exports = { calculateCompatibility };
