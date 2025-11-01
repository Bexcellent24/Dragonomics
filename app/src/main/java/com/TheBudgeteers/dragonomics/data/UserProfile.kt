package com.TheBudgeteers.dragonomics.data

/**
 * Firebase-compatible user profile model.
 * Replaces UserEntity for Firebase implementation.
 *
 * Stored in Firestore at: /users/{userId}/
 */
data class UserProfile(
    val userId: String,      // Firebase UID
    val username: String,    // Display name
    val email: String,       // User's email
    val minGoal: Double?,    // Minimum savings goal
    val maxGoal: Double?     // Maximum savings goal
)