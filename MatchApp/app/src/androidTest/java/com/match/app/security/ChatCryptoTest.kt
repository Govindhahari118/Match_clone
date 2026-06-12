package com.match.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [ChatCrypto] AES-256-GCM encrypt/decrypt round-trip.
 *
 * NOTE: These tests run on an Android device/emulator because ChatCrypto uses
 * Android KeyStore. For pure JVM, mock the KeyStore or use Robolectric.
 */
class ChatCryptoTest {

    @Test
    fun `encrypt returns non-null Base64 string`() {
        val plain = "Hello, World!"
        val encrypted = ChatCrypto.encrypt(plain)
        assertNotNull("Encrypt should not return null", encrypted)
        assertNotEquals("Encrypted text must differ from plaintext", plain, encrypted)
    }

    @Test
    fun `decrypt reverses encrypt`() {
        val original = "Matrimony test message — with Ùnicode: हिन्दी, తెలుగు"
        val encrypted = ChatCrypto.encrypt(original)
        assertNotNull(encrypted)
        val decrypted = ChatCrypto.decrypt(encrypted!!)
        assertEquals(original, decrypted)
    }

    @Test
    fun `decrypt returns null for garbage input`() {
        val result = ChatCrypto.decrypt("not-a-valid-base64-cipher")
        assertNull("Garbage input should return null", result)
    }

    @Test
    fun `encrypt produces different ciphertexts for same plaintext`() {
        val plain = "Same input"
        val a = ChatCrypto.encrypt(plain)
        val b = ChatCrypto.encrypt(plain)
        assertNotNull(a)
        assertNotNull(b)
        // GCM uses a random IV each time, so ciphertexts must differ
        assertNotEquals("Two encryptions of the same text must differ (unique IV)", a, b)
    }

    @Test
    fun `empty string round-trip`() {
        val encrypted = ChatCrypto.encrypt("")
        assertNotNull(encrypted)
        val decrypted = ChatCrypto.decrypt(encrypted!!)
        assertEquals("", decrypted)
    }
}
