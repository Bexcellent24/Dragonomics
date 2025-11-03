package com.TheBudgeteers.dragonomics.gamify


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


// Utility object for triggering achievement checks.
// These are convenience methods that can be called from anywhere in the app.

object AchievementTriggers {

    private val scope = CoroutineScope(Dispatchers.IO)

   // Track user login for streak achievements.
    fun trackLogin(userId: String) {
        scope.launch {
            try {
                val manager = AchievementManager()
                manager.onUserLogin(userId)
            } catch (e: Exception) {
                android.util.Log.e("AchievementTriggers", "Error tracking login", e)
            }
        }
    }

}