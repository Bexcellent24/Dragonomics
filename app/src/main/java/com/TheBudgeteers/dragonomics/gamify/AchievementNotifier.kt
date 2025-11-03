package com.TheBudgeteers.dragonomics.gamify

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.TheBudgeteers.dragonomics.viewmodel.AchievementDisplay
import com.TheBudgeteers.dragonomics.viewmodel.AchievementsViewModel
import kotlinx.coroutines.launch


// Handles achievement unlock notifications (toasts).
// Tracks which achievements were previously shown to avoid duplicate toasts.

class AchievementNotifier(private val context: Context, private val userId: String) {

    private val previouslyUnlockedAchievements = mutableSetOf<String>()

    companion object {
        private const val PREFS_NAME = "achievement_notifications"
        private const val KEY_PREFIX = "unlocked_"
    }

    init {
        loadPreviouslyUnlockedAchievements()
    }

    // Start observing achievements and show toasts for new unlocks.
    fun observeAndNotify(lifecycleOwner: LifecycleOwner, viewModel: AchievementsViewModel) {
        lifecycleOwner.lifecycleScope.launch {
            viewModel.achievements.collect { achievements ->
                android.util.Log.d("AchievementNotifier", "Checking ${achievements.size} achievements")

                // Find newly unlocked achievements
                val newlyUnlocked = achievements.filter {
                    it.achieved && isNewlyUnlocked(it)
                }

                android.util.Log.d("AchievementNotifier", "Found ${newlyUnlocked.size} new unlocks")

                // Show toast for each newly unlocked achievement
                newlyUnlocked.forEach { achievement ->
                    showUnlockNotification(achievement)
                    markAsShown(achievement.id)
                }
            }
        }
    }

    // Check if an achievement was newly unlocked
    private fun isNewlyUnlocked(achievement: AchievementDisplay): Boolean {
        return !previouslyUnlockedAchievements.contains(achievement.id)
    }

   // Show a toast notification for an unlocked achievement.
    private fun showUnlockNotification(achievement: AchievementDisplay) {
        android.util.Log.d(
            "AchievementNotifier",
            "🏆 Showing toast: ${achievement.title} (+${achievement.goldReward}g)"
        )

        Toast.makeText(
            context,
            "🏆 Achievement Unlocked: ${achievement.title} (+${achievement.goldReward}g)",
            Toast.LENGTH_LONG
        ).show()
    }

    // Mark an achievement as shown so we don't display it again.
    private fun markAsShown(achievementId: String) {
        previouslyUnlockedAchievements.add(achievementId)
        saveUnlockedAchievement(achievementId)
    }

    // Load previously shown achievements from SharedPreferences.
    private fun loadPreviouslyUnlockedAchievements() {
        val prefs = context.getSharedPreferences(
            "${PREFS_NAME}_$userId",
            Context.MODE_PRIVATE
        )

        val unlockedSet = prefs.getStringSet(KEY_PREFIX + userId, emptySet()) ?: emptySet()
        previouslyUnlockedAchievements.clear()
        previouslyUnlockedAchievements.addAll(unlockedSet)

        android.util.Log.d(
            "AchievementNotifier",
            "Loaded ${previouslyUnlockedAchievements.size} previously shown achievements"
        )
    }

    // Save newly shown achievement to SharedPreferences.
    private fun saveUnlockedAchievement(achievementId: String) {
        val prefs = context.getSharedPreferences(
            "${PREFS_NAME}_$userId",
            Context.MODE_PRIVATE
        )

        val currentSet = prefs.getStringSet(KEY_PREFIX + userId, emptySet())?.toMutableSet()
            ?: mutableSetOf()

        currentSet.add(achievementId)
        prefs.edit().putStringSet(KEY_PREFIX + userId, currentSet).apply()

        android.util.Log.d("AchievementNotifier", "Saved shown achievement: $achievementId")
    }

    // Clear all notification history
    fun clearNotificationHistory() {
        val prefs = context.getSharedPreferences(
            "${PREFS_NAME}_$userId",
            Context.MODE_PRIVATE
        )
        prefs.edit().clear().apply()
        previouslyUnlockedAchievements.clear()

        android.util.Log.d("AchievementNotifier", "Cleared all notification history")
    }
}