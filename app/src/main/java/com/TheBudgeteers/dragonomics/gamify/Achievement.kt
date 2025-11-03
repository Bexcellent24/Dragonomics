package com.TheBudgeteers.dragonomics.gamify

//Represents an achievement definition.

data class Achievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val medalRes: Int = 0, // R.drawable resource ID
    val goldReward: Int = 0,
    val type: AchievementType = AchievementType.ONE_TIME,
    val targetValue: Int = 1, // For cumulative achievements
    val order: Int = 0 // Display order
)

// Tracks a user's progress on a specific achievement.
data class UserAchievement(
    val achievementId: String = "",
    val userId: String = "",
    val achieved: Boolean = false,
    val progress: Int = 0, // Current progress
    val unlockedAt: Long? = null // Timestamp when unlocked
)

// Types of achievements
enum class AchievementType {
    ONE_TIME,      // Complete once
    CUMULATIVE,    // Reach a target count
    STREAK         // Consecutive actions
}
