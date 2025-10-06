package com.TheBudgeteers.dragonomics.gamify

import android.content.Context
import android.content.SharedPreferences
import com.TheBudgeteers.dragonomics.models.Mood as NestMood

/*
Purpose:
  - Helper for the dragon’s overall mood (POSITIVE / NEUTRAL / NEGATIVE).
  - Stores the current overall mood in SharedPreferences and exposes a simple XP multiplier.
 */

object DragonMoodManager {
    private const val PREF = "dragon_mood_prefs"
    private const val KEY_OVERALL = "overall_mood"

    // Accessor for the prefs file.
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // begin code attribution
    // Store the enum as its stable String name in SharedPreferences.
    // Adapted from:
    // Android Developers, 2020. SharedPreferences. [online]
    // Available at: <https://developer.android.com/reference/android/content/SharedPreferences> [Accessed 6 October 2025].
    // Persist the overall mood as the enum name
    fun setOverallMood(ctx: Context, mood: NestMood) {
        prefs(ctx).edit().putString(KEY_OVERALL, mood.name).apply()
    }
    // end code attribution (Android Developers, 2020)
}
// reference list
// Android Developers, 2020. SharedPreferences. [online]
// Available at: <https://developer.android.com/reference/android/content/SharedPreferences> [Accessed 6 October 2025].
