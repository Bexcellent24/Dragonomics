package com.TheBudgeteers.dragonomics.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITER = 120_000
    private const val KEY_LEN = 256 // bits
    private const val ALGO = "PBKDF2WithHmacSHA256"
    private val rng = SecureRandom()

    fun newSalt(bytes: Int = 16): String {
        val b = ByteArray(bytes); rng.nextBytes(b)
        return Base64.encodeToString(b, Base64.NO_WRAP)
    }

    fun hash(password: CharArray, saltB64: String): String {
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val spec = PBEKeySpec(password, salt, ITER, KEY_LEN)
        val skf = SecretKeyFactory.getInstance(ALGO)
        val key = skf.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    fun verify(password: CharArray, saltB64: String, expectedHashB64: String): Boolean {
        val h = hash(password, saltB64)
        if (h.length != expectedHashB64.length) return false
        var diff = 0
        for (i in h.indices) diff = diff or (h[i].code xor expectedHashB64[i].code)
        return diff == 0
    }
}
