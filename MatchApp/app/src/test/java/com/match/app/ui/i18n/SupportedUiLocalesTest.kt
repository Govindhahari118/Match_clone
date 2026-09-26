package com.match.app.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedUiLocalesTest {

    @Test
    fun `only production translation packs are exposed`() {
        assertEquals(linkedSetOf("en", "te", "hi"), SupportedUiLocales.codes)
        assertTrue("en" in SupportedUiLocales.codes)
        assertTrue("te" in SupportedUiLocales.codes)
        assertTrue("hi" in SupportedUiLocales.codes)
        assertFalse("ta" in SupportedUiLocales.codes)
        assertFalse("kn" in SupportedUiLocales.codes)
        assertFalse("mr" in SupportedUiLocales.codes)
    }

    @Test
    fun `requested supported language wins`() {
        assertEquals("te", SupportedUiLocales.normalize(" TE ", systemLanguage = "en"))
        assertEquals("hi", SupportedUiLocales.normalize("hi", systemLanguage = "te"))
    }

    @Test
    fun `unsupported requested language falls back to supported system language`() {
        assertEquals("te", SupportedUiLocales.normalize("ta", systemLanguage = "te"))
        assertEquals("hi", SupportedUiLocales.normalize(null, systemLanguage = "HI"))
    }

    @Test
    fun `unsupported requested and system language fall back to English`() {
        assertEquals("en", SupportedUiLocales.normalize("fr", systemLanguage = "de"))
        assertEquals("en", SupportedUiLocales.normalize("", systemLanguage = ""))
    }
}
