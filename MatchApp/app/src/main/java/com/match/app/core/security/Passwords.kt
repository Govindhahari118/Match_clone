package com.match.app.core.security

import org.mindrot.jbcrypt.BCrypt

/** BCrypt-based password hashing. Cost 11 ≈ ~100ms on modern phones — strong without lag. */
object Passwords {
    private const val COST = 11
    fun hash(plain: String): String = BCrypt.hashpw(plain, BCrypt.gensalt(COST))
    fun verify(plain: String, hashed: String): Boolean =
        runCatching { BCrypt.checkpw(plain, hashed) }.getOrDefault(false)
}
