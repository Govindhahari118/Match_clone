package com.match.app.core.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorsTest {

    @Test fun `cosine is 1 for identical vectors`() {
        val v = floatArrayOf(1f, 2f, 3f)
        assertEquals(1f, Vectors.cosine(v, v), 1e-5f)
    }

    @Test fun `cosine is 0 for zero vector`() {
        assertEquals(0f, Vectors.cosine(floatArrayOf(0f, 0f), floatArrayOf(1f, 1f)), 1e-5f)
    }

    @Test fun `cosine is -1 for opposite vectors`() {
        val a = floatArrayOf(1f, 0f, 0f); val b = floatArrayOf(-1f, 0f, 0f)
        assertEquals(-1f, Vectors.cosine(a, b), 1e-5f)
    }

    @Test fun `questionnaireScore is in 0 to 1`() {
        val a = FloatArray(10) { 0.3f }
        val b = FloatArray(10) { 0.7f }
        val s = Vectors.questionnaireScore(a, b, a, b)
        assertTrue(s in 0f..1f)
    }

    @Test fun `questionnaireScore peaks when seeker-partner equals candidate-self`() {
        val seekerSelf = FloatArray(5) { 0.1f }
        val seekerPartner = floatArrayOf(1f, 0f, 0f, 0f, 0f)
        val candSelf = floatArrayOf(1f, 0f, 0f, 0f, 0f)         // matches seeker's wish
        val candPartner = FloatArray(5) { 0.1f }                // matches seeker's self
        val s = Vectors.questionnaireScore(seekerSelf, seekerPartner, candSelf, candPartner)
        assertTrue("expected high score, got $s", s > 0.9f)
    }
}
