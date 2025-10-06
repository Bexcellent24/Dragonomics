package com.TheBudgeteers.dragonomics.utils

import android.util.Patterns

/*
Purpose:
  - Reusable input validation for username, email, password, and confirmation.
  - Each function returns an error message or null when the input is valid.
*/

object Validators {

    // begin code attribution
    // Simple alphanumeric-only validation using a Kotlin Regex pattern.
    // Adapted from:
    // Kotlin, 2024. Regular expressions. [online]
    // Available at: <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/> [Accessed 6 October 2025].

    // Alphanumeric only, at least 6 characters
    private val ALNUM_6PLUS = Regex("^[A-Za-z0-9]{6,}$")
    // end code attribution (Kotlin, 2024)

    // Validate username format and length
    fun username(u: String): String? {
        if (u.isBlank()) return "Username is required"
        if (!ALNUM_6PLUS.matches(u)) {
            return if (u.length < 6) "Username must be at least 6 characters"
            else "Username may only contain letters and numbers"
        }
        return null
    }

    // begin code attribution
    // Validate email format using Android’s built-in EMAIL_ADDRESS regex pattern.
    // Adapted from:
    // Android Developers, 2020. android.util.Patterns.EMAIL_ADDRESS. [online]
    // Available at: <https://developer.android.com/reference/android/util/Patterns#EMAIL_ADDRESS>
    // [Accessed 6 October 2025].

    // Validate email format using Android's built-in EMAIL_ADDRESS heuristic.
    fun email(e: String): String? {
        if (e.isBlank()) return "Email is required"
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches())
            return "Not a correct email format"
        return null
    }
    // end code attribution (Android Developers, 2020)


    // Validate password format and length
    fun password(p: String): String? {
        if (p.isBlank()) return "Password is required"
        if (!ALNUM_6PLUS.matches(p)) {
            return if (p.length < 6) "Password must be at least 6 characters"
            else "Password may only contain letters and numbers"
        }
        return null
    }

    // Validate password confirmation
    fun confirmPassword(p: String, c: String): String? {
        if (c.isBlank()) return "Please confirm your password"
        if (p != c) return "Passwords do not match"
        return null
    }
}

// reference list
// Android Developers, 2020. android.util.Patterns.EMAIL_ADDRESS. [online]
// Available at: <https://developer.android.com/reference/android/util/Patterns#EMAIL_ADDRESS> [Accessed 6 October 2025].
// Kotlin, 2024. Regular expressions. [online]
// Available at: <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/> [Accessed 6 October 2025].
