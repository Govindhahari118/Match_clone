package com.match.app.domain.questionnaire

/**
 * The canonical questionnaire template. EVERY user answers the exact same 15 questions:
 * — 10 personality/lifestyle self-description questions (answered twice: self + desired partner).
 * — 5 hobby/interest multi-select questions (answered twice: self + desired partner).
 *
 * Each Likert answer encodes to a float in [-1, +1] producing a fixed-length vector used for cosine similarity.
 */
object Questionnaire {

    data class DecodedAnswers(
        val likert: Map<Int, Int>,
        val interests: Map<Int, Set<String>>
    )

    /** 10 single-choice Likert-style questions. */
    data class LikertQuestion(val id: Int, val prompt: String)

    /** 5 multi-select interest buckets. */
    data class InterestQuestion(val id: Int, val prompt: String, val options: List<String>)

    val LIKERT: List<LikertQuestion> = listOf(
        LikertQuestion(1,  "How introverted vs. extroverted are you?"),
        LikertQuestion(2,  "How traditional vs. modern is your outlook on family?"),
        LikertQuestion(3,  "How career-driven are you right now?"),
        LikertQuestion(4,  "How important is religion/spirituality in daily life?"),
        LikertQuestion(5,  "How adventurous are you (travel, risk, change)?"),
        LikertQuestion(6,  "How important is fitness and healthy living?"),
        LikertQuestion(7,  "How financially conservative vs. spending-oriented are you?"),
        LikertQuestion(8,  "How important is living close to extended family?"),
        LikertQuestion(9,  "How much do you value intellectual/academic pursuits?"),
        LikertQuestion(10, "How emotionally expressive vs. reserved are you?")
    )

    val INTERESTS: List<InterestQuestion> = listOf(
        InterestQuestion(11, "Music genres you love",
            listOf("Classical","Pop","Rock","Hip-Hop","Jazz","Folk","Indie","Electronic")),
        InterestQuestion(12, "How do you like to spend weekends?",
            listOf("Outdoors","Reading","Movies","Cooking","Gaming","Socializing","Fitness","Art")),
        InterestQuestion(13, "Food preferences",
            listOf("Vegetarian","Vegan","Non-veg","Eggetarian","Jain","Halal","Kosher","Anything")),
        InterestQuestion(14, "Travel style",
            listOf("Beaches","Mountains","Cities","Countryside","Adventure","Luxury","Budget","Spiritual")),
        InterestQuestion(15, "Hobbies",
            listOf("Sports","Photography","Writing","Dancing","Yoga","Coding","Gardening","Volunteering"))
    )

    /** Total flattened vector length = 10 (likert) + sum(|options|) = 10 + 40 = 50. */
    val VECTOR_LENGTH: Int = LIKERT.size + INTERESTS.sumOf { it.options.size }

    /**
     * Encode answers to a fixed-length FloatArray.
     * @param likert  map of likertQuestionId -> Int in 1..5   (3 = neutral)
     * @param interests map of interestQuestionId -> set of chosen option strings
     */
    fun encode(
        likert: Map<Int, Int>,
        interests: Map<Int, Set<String>>
    ): FloatArray {
        val vec = FloatArray(VECTOR_LENGTH)
        var idx = 0
        // 1) likert — map 1..5 to -1..+1
        for (q in LIKERT) {
            val raw = likert[q.id] ?: 3
            vec[idx++] = ((raw.coerceIn(1, 5) - 3) / 2f)
        }
        // 2) interests — one-hot 0/1 per option (ordered consistently)
        for (q in INTERESTS) {
            val chosen = interests[q.id].orEmpty()
            for (opt in q.options) {
                vec[idx++] = if (opt in chosen) 1f else 0f
            }
        }
        return vec
    }

    /** Reverse the canonical vector so owner-only server state can restore the editing form. */
    fun decode(vector: FloatArray): DecodedAnswers? {
        if (vector.size != VECTOR_LENGTH) return null
        var index = 0
        val likert = linkedMapOf<Int, Int>()
        LIKERT.forEach { question ->
            val value = vector[index++]
            if (!value.isFinite() || value !in -1f..1f) return null
            likert[question.id] = (value * 2f + 3f).toInt().coerceIn(1, 5)
        }

        val interests = linkedMapOf<Int, Set<String>>()
        INTERESTS.forEach { question ->
            val selected = linkedSetOf<String>()
            question.options.forEach { option ->
                val value = vector[index++]
                if (!value.isFinite() || value !in -1f..1f) return null
                if (value > 0.5f) selected += option
            }
            interests[question.id] = selected
        }
        return DecodedAnswers(likert = likert, interests = interests)
    }
}
