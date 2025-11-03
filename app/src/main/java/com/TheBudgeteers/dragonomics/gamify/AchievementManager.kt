package com.TheBudgeteers.dragonomics.gamify

import android.util.Log
import com.TheBudgeteers.dragonomics.data.FirebaseAchievementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar


// Manages achievement checking and unlocking.
// Call methods when user performs actions to automatically check and unlock achievements.

class AchievementManager(
    private val repository: FirebaseAchievementRepository = FirebaseAchievementRepository()
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // ---------- USER ACTION TRACKING ----------

    // Called when user creates a nest.
    fun onNestCreated(userId: String) {
        scope.launch {
            try {
                // Track total nests created
                val stats = repository.getUserStats(userId)
                val nestsCreated = (stats["nestsCreated"] as? Long)?.toInt() ?: 0
                repository.updateUserStats(
                    userId,
                    mapOf("nestsCreated" to nestsCreated + 1)
                )

                // Check "first nest" achievement
                checkAndUnlock(userId, "first_nest")
            } catch (e: Exception) {
                Log.e("AchievementManager", "Error in onNestCreated", e)
            }
        }
    }

    // Called when user logs an expense transaction.
    fun onExpenseLogged(userId: String) {
        scope.launch {
            try {
                // Track total expenses logged
                val stats = repository.getUserStats(userId)
                val expensesLogged = (stats["expensesLogged"] as? Long)?.toInt() ?: 0
                val newTotal = expensesLogged + 1

                repository.updateUserStats(
                    userId,
                    mapOf("expensesLogged" to newTotal)
                )

                // Update progress for first expense (one-time)
                checkAndUnlock(userId, "first_expense")

                //Update progress for cumulative achievements
                updateProgress(userId, "five_expenses", newTotal, 5)
                updateProgress(userId, "ten_expenses", newTotal, 10)

            } catch (e: Exception) {
                Log.e("AchievementManager", "Error in onExpenseLogged", e)
            }
        }
    }

    // Update progress and unlock if target reached
    private suspend fun updateProgress(userId: String, achievementId: String, currentProgress: Int, targetValue: Int) {
        try {
            val userAchievement = repository.getUserAchievement(userId, achievementId)

            // Don't update if already achieved
            if (userAchievement?.achieved == true) return

            // Update progress
            repository.updateUserAchievement(
                userId,
                achievementId,
                currentProgress,
                achieved = currentProgress >= targetValue
            )

            // If target reached, unlock it
            if (currentProgress >= targetValue) {
                checkAndUnlock(userId, achievementId)
            }
        } catch (e: Exception) {
            Log.e("AchievementManager", "Error updating progress for $achievementId", e)
        }
    }

    // Call when user logs an income transaction.
    fun onIncomeLogged(userId: String) {
        scope.launch {
            try {
                // Track total income logged
                val stats = repository.getUserStats(userId)
                val incomeLogged = (stats["incomeLogged"] as? Long)?.toInt() ?: 0

                repository.updateUserStats(
                    userId,
                    mapOf("incomeLogged" to incomeLogged + 1)
                )

                // Check achievement
                checkAndUnlock(userId, "first_income")
            } catch (e: Exception) {
                Log.e("AchievementManager", "Error in onIncomeLogged", e)
            }
        }
    }

    // Call when user logs in.
    // Handles login streak tracking.
    fun onUserLogin(userId: String) {
        scope.launch {
            try {
                val stats = repository.getUserStats(userId).toMutableMap()
                val today = getTodayDateString()
                val lastLoginDate = stats["lastLoginDate"] as? String
                val currentStreak = (stats["loginStreak"] as? Long)?.toInt() ?: 0

                // Check if this is a new day
                if (lastLoginDate != today) {
                    val yesterday = getYesterdayDateString()
                    val newStreak = if (lastLoginDate == yesterday) {
                        // Continuing streak
                        currentStreak + 1
                    } else {
                        // Streak broken, start new
                        1
                    }

                    stats["lastLoginDate"] = today
                    stats["loginStreak"] = newStreak
                    stats["totalLogins"] = ((stats["totalLogins"] as? Long)?.toInt() ?: 0) + 1

                    repository.updateUserStats(userId, stats)

                    //heck day one achievement
                    checkAndUnlock(userId, "day_one")

                    //Update progress for streak achievements
                    updateProgress(userId, "day_two_streak", newStreak, 2)
                    updateProgress(userId, "week_streak", newStreak, 7)
                    updateProgress(userId, "month_streak", newStreak, 30)
                }
            } catch (e: Exception) {
                Log.e("AchievementManager", "Error in onUserLogin", e)
            }
        }
    }

    // ---------- ACHIEVEMENT CHECKING ----------

    // Check and unlock an achievement if not already unlocked.
    //Returns the gold reward if newly unlocked, 0 otherwise.
    private suspend fun checkAndUnlock(userId: String, achievementId: String): Int {
        return try {
            val userAchievement = repository.getUserAchievement(userId, achievementId)

            // Only unlock if not already achieved
            if (userAchievement?.achieved != true) {
                val result = repository.unlockAchievement(userId, achievementId)
                if (result.isSuccess) {
                    val goldReward = result.getOrNull() ?: 0
                    val achievement = repository.getAchievement(achievementId)

                    Log.d("AchievementManager", "✅ Unlocked: ${achievement?.title} (+${goldReward}g)")

                    return goldReward
                }
            }
            0
        } catch (e: Exception) {
            Log.e("AchievementManager", "Error checking achievement $achievementId", e)
            0
        }
    }

    // Get combined achievement data (definition + user progress) for display.
    suspend fun getAchievementsWithProgress(userId: String): List<AchievementWithProgress> {
        return try {
            val achievements = repository.getAllAchievements()

            achievements.map { achievement ->
                val userProgress = repository.getUserAchievement(userId, achievement.id)

                AchievementWithProgress(
                    achievement = achievement,
                    progress = userProgress?.progress ?: 0,
                    achieved = userProgress?.achieved ?: false,
                    unlockedAt = userProgress?.unlockedAt
                )
            }
        } catch (e: Exception) {
            Log.e("AchievementManager", "Error getting achievements with progress", e)
            emptyList()
        }
    }

    // ---------- HELPER METHODS ----------

    private fun getTodayDateString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }
}

// Combined data for displaying achievements with user progress.
data class AchievementWithProgress(
    val achievement: Achievement,
    val progress: Int,
    val achieved: Boolean,
    val unlockedAt: Long?
)