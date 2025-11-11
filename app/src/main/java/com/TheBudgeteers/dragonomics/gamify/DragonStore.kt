package com.TheBudgeteers.dragonomics.gamify

import android.content.Context
import android.content.SharedPreferences
import com.TheBudgeteers.dragonomics.data.FirebaseDragonRepository
import kotlinx.coroutines.runBlocking


// Dragon's current game state

data class DragonState(
    val totalXp: Int = 0,
    val level: Int = 0,
    val xpIntoLevel: Int = 0,
    val moodScore: Int = 0,
    val mood: DragonRules.Mood = DragonRules.Mood.NEUTRAL,
    val lastLoginYmd: Int = 0
)


// DragonStore - Now Firebase-backed and user-aware.
//Each user gets their own dragon state stored in Firestore.

class DragonStore(
    context: Context,
    private val userId: String
) {
    private val firebaseRepo = FirebaseDragonRepository()

    // Keep SharedPreferences as a backup/cache
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dragon_store_$userId", Context.MODE_PRIVATE)


     // Load dragon state from Firebase for this specific user.
     // Falls back to SharedPreferences if Firebase fails.

    fun load(): DragonState = runBlocking {
        try {
            // Try to load from Firebase first
            val firebaseData = firebaseRepo.loadDragonState(userId)

            if (firebaseData != null) {
                // Convert Firebase data to DragonState
                val xp = firebaseData.totalXp
                val moodScore = firebaseData.moodScore
                val mood = DragonRules.moodFromScore(moodScore)
                val level = DragonRules.levelFromXp(xp)
                val into = DragonRules.xpIntoLevel(xp)
                val last = firebaseData.lastLoginYmd

                DragonState(xp, level, into, moodScore, mood, last)
            } else {
                // No Firebase data - new user, return default state
                DragonState()
            }
        } catch (e: Exception) {
            android.util.Log.e("DragonStore", "Failed to load from Firebase, using default", e)
            // Return default state if Firebase fails
            DragonState()
        }
    }


     // Save dragon state to Firebase for this user.
     // Also saves to SharedPreferences as backup.

    fun save(state: DragonState) {
        // Save to SharedPreferences as backup
        prefs.edit()
            .putInt("xp", state.totalXp)
            .putInt("moodScore", state.moodScore)
            .putInt("lastLoginYmd", state.lastLoginYmd)
            .apply()

        // Save to Firebase
        runBlocking {
            try {
                firebaseRepo.saveDragonState(
                    userId = userId,
                    totalXp = state.totalXp,
                    moodScore = state.moodScore,
                    lastLoginYmd = state.lastLoginYmd
                )
            } catch (e: Exception) {
                android.util.Log.e("DragonStore", "Failed to save to Firebase", e)
            }
        }
    }
}