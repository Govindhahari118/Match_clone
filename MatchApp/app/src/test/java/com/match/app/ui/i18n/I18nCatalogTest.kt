package com.match.app.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [I18nCatalog] text resolution and interpolation.
 * Runs on JVM — no Android dependencies needed.
 */
class I18nCatalogTest {

    private val english = mapOf(
        "hello" to "Hello",
        "greeting" to "Hi {name}!",
        "items" to "{count} items",
        "items_plural" to "{count} items",
        "item" to "{count} item"
    )

    private val hindi = mapOf(
        "hello" to "नमस्ते",
        "greeting" to "नमस्ते {name}!"
    )

    @Test
    fun `text returns current locale value`() {
        val catalog = I18nCatalog(hindi, english)
        assertEquals("नमस्ते", catalog.text("hello", "fallback"))
    }

    @Test
    fun `text falls back to English when key missing in current locale`() {
        val catalog = I18nCatalog(hindi, english)
        // "items" is not in hindi map, should fall back to english
        assertEquals("{count} items", catalog.text("items", "fallback"))
    }

    @Test
    fun `text returns fallback when key missing in both locales`() {
        val catalog = I18nCatalog(hindi, english)
        assertEquals("My Fallback", catalog.text("nonexistent_key", "My Fallback"))
    }

    @Test
    fun `text with args interpolates variables`() {
        val catalog = I18nCatalog(english, english)
        val result = catalog.text("greeting", mapOf("name" to "Lakshmi"), "fallback")
        assertEquals("Hi Lakshmi!", result)
    }

    @Test
    fun `text with args in non-English locale`() {
        val catalog = I18nCatalog(hindi, english)
        val result = catalog.text("greeting", mapOf("name" to "राम"), "fallback")
        assertEquals("नमस्ते राम!", result)
    }

    @Test
    fun `empty catalog returns fallback`() {
        val catalog = I18nCatalog(emptyMap(), emptyMap())
        assertEquals("Default", catalog.text("any_key", "Default"))
    }
}
