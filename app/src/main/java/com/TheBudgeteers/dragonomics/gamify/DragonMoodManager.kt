package com.TheBudgeteers.dragonomics.gamify

import android.content.Context
import android.content.SharedPreferences
import com.TheBudgeteers.dragonomics.models.Mood as NestMood

/*
Purpose:
  - Helper for the dragon’s overall mood (POSITIVE / NEUTRAL / NEGATIVE).
  - Stores the current overall mood in SharedPreferences and exposes a simple XP multiplier.

  References:
  - Kotlin language: Enums, exception-safe parsing, and when-expressions.
     * Enum classes: https://kotlinlang.org/docs/enum-classes.html

      Author: Kotlin | Date: 2025-10-05

 */

object DragonMoodManager {
    private const val PREF = "dragon_mood_prefs"
    private const val KEY_OVERALL = "overall_mood"

    // Accessor for the prefs file.
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    //Persist the overall mood as the enum name
    fun setOverallMood(ctx: Context, mood: NestMood) {
        prefs(ctx).edit().putString(KEY_OVERALL, mood.name).apply()
    }

}
