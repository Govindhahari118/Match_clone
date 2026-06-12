package com.match.app.domain.questionnaire

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionnaireTest {

    @Test fun `vector length is deterministic`() {
        val expected = Questionnaire.LIKERT.size + Questionnaire.INTERESTS.sumOf { it.options.size }
        assertEquals(expected, Questionnaire.VECTOR_LENGTH)
    }

    @Test fun `encoding neutral produces zero-ish vector for likert`() {
        val likert = Questionnaire.LIKERT.associate { it.id to 3 }
        val interests = emptyMap<Int, Set<String>>()
        val v = Questionnaire.encode(likert, interests)
        for (i in 0 until Questionnaire.LIKERT.size) assertEquals(0f, v[i], 1e-6f)
    }

    @Test fun `encoding picks one-hot for chosen interests`() {
        val likert = Questionnaire.LIKERT.associate { it.id to 3 }
        val q = Questionnaire.INTERESTS.first()
        val chosen = setOf(q.options.first(), q.options.last())
        val interests = mapOf(q.id to chosen)
        val v = Questionnaire.encode(likert, interests)
        // one-hot entries exist after Likert section
        val offset = Questionnaire.LIKERT.size
        assertEquals(1f, v[offset], 1e-6f)                               // first option
        assertEquals(1f, v[offset + q.options.size - 1], 1e-6f)          // last option
        assertTrue(v.sum() >= 2f)
    }
}
