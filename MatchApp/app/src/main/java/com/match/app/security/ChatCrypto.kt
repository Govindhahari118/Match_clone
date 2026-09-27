package com.match.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption using the Android KeyStore.
 *
 * Keys are created inside Android Keystore and are not exported as raw key bytes to app code.
 * Hardware backing depends on the device/OS configuration. A unique IV is generated for every
 * encrypt call and is prepended to the ciphertext so decrypt can reconstruct the GCMParameterSpec.
 *
 * Cipher payload (Base64-encoded): [12-byte IV][N-byte ciphertext+tag]
 */
object ChatCrypto {

    private const val KEY_ALIAS    = "match_chat_key_v1"
    private const val PROVIDER     = "AndroidKeyStore"
    private const val ALGORITHM    = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE   = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING      = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val CIPHER_XFORM = "AES/GCM/NoPadding"
    private const val IV_LENGTH    = 12   // 96-bit IV recommended for GCM
    private const val TAG_LENGTH   = 128  // bits
    private const val STORAGE_PREFIX = "matree-chat:v1:"

    // ── Key management ───────────────────────────────────────────────────────

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        ks.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(ALGORITHM, PROVIDER)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // flip to true for biometric gate
                .build()
        )
        return keyGen.generateKey()
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Encrypt [plaintext] and return a Base64 string safe to store or transmit.
     * Returns null if encryption fails (e.g. KeyStore unavailable during tests).
     */
    fun encrypt(plaintext: String): String? = runCatching {
        val cipher = Cipher.getInstance(CIPHER_XFORM)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv         = cipher.iv                                   // 12 bytes
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + ciphertext.size)
        iv.copyInto(combined)
        ciphertext.copyInto(combined, iv.size)

        Base64.encodeToString(combined, Base64.NO_WRAP)
    }.getOrNull()

    /**
     * Prefix new local-storage ciphertext so retry/migration code can distinguish protected rows
     * from historical plaintext pending-outbox rows without guessing.
     */
    fun encryptForStorage(plaintext: String): String? =
        encrypt(plaintext)?.let { STORAGE_PREFIX + it }

    fun isVersionedStorage(value: String): Boolean = value.startsWith(STORAGE_PREFIX)

    /**
     * Decrypt a value written by [encryptForStorage]. Historical unprefixed ciphertext produced by
     * older app versions is also accepted so existing encrypted Room rows remain readable.
     */
    fun decryptFromStorage(value: String): String? =
        decrypt(if (isVersionedStorage(value)) value.removePrefix(STORAGE_PREFIX) else value)

    /**
     * Decrypt a Base64 string produced by [encrypt].
     * Returns the original plaintext, or null on failure.
     */
    fun decrypt(encoded: String): String? = runCatching {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_LENGTH) { "Ciphertext too short" }

        val iv         = combined.copyOfRange(0, IV_LENGTH)
        val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(CIPHER_XFORM)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH, iv))

        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()
}
