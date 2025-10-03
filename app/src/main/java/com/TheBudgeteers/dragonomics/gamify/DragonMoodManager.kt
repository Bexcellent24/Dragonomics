package com.TheBudgeteers.dragonomics.gamify

import android.content.Context
import android.content.SharedPreferences
import com.TheBudgeteers.dragonomics.models.Mood as NestMood

object DragonMoodManager {
    private const val PREF = "dragon_mood_prefs"
    private const val KEY_OVERALL = "overall_mood"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun setOverallMood(ctx: Context, mood: NestMood) {
        prefs(ctx).edit().putString(KEY_OVERALL, mood.name).apply()
    }

    fun getOverallMood(ctx: Context): NestMood {
        val s = prefs(ctx).getString(KEY_OVERALL, NestMood.NEUTRAL.name)!!
        return runCatching { enumValueOf<NestMood>(s) }.getOrDefault(NestMood.NEUTRAL)
    }

    fun xpMultiplierFor(mood: NestMood): Float = when (mood) {
        NestMood.POSITIVE -> 1.25f
        NestMood.NEUTRAL  -> 1.00f
        NestMood.NEGATIVE -> 0.75f
    }
}
