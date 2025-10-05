package com.TheBudgeteers.dragonomics.utils

import android.util.Patterns

/*
Validators

Purpose:
  - Reusable input validation for username, email, password, and confirmation.
  - Each function returns an error message or null when the input is valid.

References:
 - Android official docs: Text input, error presentation, and email patterns.
     * Text fields (Material): https://developer.android.com/develop/ui/views/components/text-fields
     * TextInputLayout#setError (if used in layout): https://developer.android.com/reference/com/google/android/material/textfield/TextInputLayout
     * android.util.Patterns.EMAIL_ADDRESS: https://developer.android.com/reference/android/util/Patterns#EMAIL_ADDRESS

Author: Android | Date: 2025-10-05
*/

object Validators {

    // Alphanumeric only, at least 6 characters
    private val ALNUM_6PLUS = Regex("^[A-Za-z0-9]{6,}$")

    // Validate username format and length
    fun username(u: String): String? {
        if (u.isBlank()) return "Username is required"
        if (!ALNUM_6PLUS.matches(u)) {
            return if (u.length < 6) "Username must be at least 6 characters"
            else "Username may only contain letters and numbers"
        }
        return null
    }

    // Validate email format using Android's built-in EMAIL_ADDRESS heuristic.
    fun email(e: String): String? {
        if (e.isBlank()) return "Email is required"
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches())
            return "Not a correct email format"
        return null
    }

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