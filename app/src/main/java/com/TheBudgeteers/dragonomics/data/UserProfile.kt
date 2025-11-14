package com.TheBudgeteers.dragonomics.data

// Firebase-compatible user profile model.
// Replaces UserEntity for Firebase implementation.

data class UserProfile(
    val userId: String,      // Firebase UID
    val username: String,    // Display name
    val email: String,       // User's email
    val firstName: String = "",   // Added: First name
    val lastName: String = "",    // Added: Last name
    val profilePictureUrl: String = "", // Added: Profile picture URL from Firebase Storage
    val minGoal: Double?,    // Minimum savings goal
    val maxGoal: Double?,     // Maximum savings goal
    val gold: Int = 0
)