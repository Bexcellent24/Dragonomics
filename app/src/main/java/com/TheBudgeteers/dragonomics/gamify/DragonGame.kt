package com.TheBudgeteers.dragonomics.gamify

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import com.TheBudgeteers.dragonomics.R
import java.util.Calendar

/**
 * Core numbers:
 * - XP per level: 50
 * - Base XP per expense: +5, photo bonus: +2
 * - XP modifier: ONLY the dragon’s mood (HAPPY +3, NEUTRAL +0, ANGRY -1) — flat bonuses
 * - Mood bands from score: >= +2 = HAPPY, <= -2 = ANGRY, else NEUTRAL
 */
object DragonRules {
    const val XP_PER_LEVEL = 25
    const val XP_EXPENSE = 5
    const val XP_PHOTO_BONUS = 2

    // Flat mood bonuses
    const val HAPPY_BONUS = 3
    const val NEUTRAL_BONUS = 0
    const val ANGRY_BONUS = -1

    // Optional guard so an action always grants at least 1 XP
    const val MIN_XP_PER_ACTION = 1

    fun levelFromXp(totalXp: Int): Int = totalXp / XP_PER_LEVEL
    fun xpIntoLevel(totalXp: Int): Int = totalXp % XP_PER_LEVEL

    enum class Mood { HAPPY, NEUTRAL, ANGRY }

    fun moodFromScore(score: Int): Mood =
        when {
            score >= 2  -> Mood.HAPPY
            score <= -2 -> Mood.ANGRY
            else        -> Mood.NEUTRAL
        }

    /** Apply flat mood bonus to base XP (clamped to at least MIN_XP_PER_ACTION). */
    fun applyMoodBonus(base: Int, mood: Mood): Int {
        val bonus = when (mood) {
            Mood.HAPPY   -> HAPPY_BONUS
            Mood.NEUTRAL -> NEUTRAL_BONUS
            Mood.ANGRY   -> ANGRY_BONUS
        }
        return maxOf(MIN_XP_PER_ACTION, base + bonus)
    }

    @DrawableRes
    fun dragonImageFor(level: Int): Int = when {
        level >= 10 -> R.drawable.adult_dragon
        level >= 5 -> R.drawable.teen_dragon
        else       -> R.drawable.baby_dragon
    }

    @DrawableRes
    fun moodIconFor(mood: Mood): Int = when (mood) {
        Mood.HAPPY   -> R.drawable.happy_mood
        Mood.NEUTRAL -> R.drawable.neutral_mood
        Mood.ANGRY   -> R.drawable.angry_mood
    }
}

data class DragonState(
    val totalXp: Int = 0,
    val level: Int = 0,
    val xpIntoLevel: Int = 0,
    val moodScore: Int = 0,
    val mood: DragonRules.Mood = DragonRules.Mood.NEUTRAL,
    val lastLoginYmd: Int = 0 // YYYYMMDD to grant daily login once
)

class DragonStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dragon_store", Context.MODE_PRIVATE)

    fun load(): DragonState {
        val xp = prefs.getInt("xp", 0)
        val moodScore = prefs.getInt("moodScore", 0)
        val mood = DragonRules.moodFromScore(moodScore)
        val level = DragonRules.levelFromXp(xp)
        val into = DragonRules.xpIntoLevel(xp)
        val last = prefs.getInt("lastLoginYmd", 0)
        return DragonState(xp, level, into, moodScore, mood, last)
    }

    fun save(state: DragonState) {
        prefs.edit()
            .putInt("xp", state.totalXp)
            .putInt("moodScore", state.moodScore)
            .putInt("lastLoginYmd", state.lastLoginYmd)
            .apply()
    }
}

/** Facade used by Activities/Fragments */
class DragonGame(private val store: DragonStore) {

    var state: DragonState = store.load()
        private set

    private fun recomputeAndPersist(
        totalXp: Int? = null,
        moodScore: Int? = null,
        lastLoginYmd: Int? = null
    )
    {
        val newXp = totalXp ?: state.totalXp
        val newMoodScore = moodScore ?: state.moodScore
        val newMood = DragonRules.moodFromScore(newMoodScore)
        val newLevel = DragonRules.levelFromXp(newXp)
        val into = DragonRules.xpIntoLevel(newXp)
        val last = lastLoginYmd ?: state.lastLoginYmd
        state = DragonState(newXp, newLevel, into, newMoodScore, newMood, last)
        android.util.Log.d("DragonXP",
            "New state: L${state.level} ${state.xpIntoLevel}/${DragonRules.XP_PER_LEVEL}, total=${state.totalXp}, mood=${state.mood} (score=${state.moodScore})"
        )
        store.save(state)
        DragonGameEvents.notifyChanged(state)


    }

    /** Call once per real day on app open (grants +1 Mood once per day) */
    fun onDailyLogin() {
        val today = todayYmd()
        if (state.lastLoginYmd == today) return
        recomputeAndPersist(moodScore = state.moodScore + 1, lastLoginYmd = today)
    }

    fun onExpenseLogged(addedPhoto: Boolean) {
        var base = DragonRules.XP_EXPENSE
        if (addedPhoto) base += DragonRules.XP_PHOTO_BONUS
        val effective = DragonRules.applyMoodBonus(base, state.mood)

        android.util.Log.d("DragonXP",
            "Expense BEFORE: base=$base mood=${state.mood} effective=$effective total=${state.totalXp}"
        )

        recomputeAndPersist(totalXp = state.totalXp + effective)

        android.util.Log.d("DragonXP",
            "Expense AFTER:  L${state.level} ${state.xpIntoLevel}/${DragonRules.XP_PER_LEVEL} total=${state.totalXp}"
        )
    }


    /**
     * Call whenever you evaluate budget status (daily/weekly/monthly).
     * Inputs are flags derived from your budget calculations.
     */
    fun onBudgetEvaluated(
        under80Percent: Boolean = false,
        between80And100: Boolean = false,
        overBudget: Boolean = false,
        betweenMinAndMaxGoal: Boolean = false,
        aboveMaxGoal: Boolean = false
    ) {
        var delta = 0
        if (under80Percent)        delta += 2
        if (between80And100)       delta += 0
        if (overBudget)            delta -= 3
        if (betweenMinAndMaxGoal)  delta += 2
        if (aboveMaxGoal)          delta -= 3
        recomputeAndPersist(moodScore = state.moodScore + delta)
    }

    private fun todayYmd(): Int {
        val c = Calendar.getInstance()
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return y * 10000 + m * 100 + d
    }

    fun setOverallMood(m: DragonRules.Mood) {
        // pick representative band edges so moodFromScore() returns exactly this mood
        val targetScore = when (m) {
            DragonRules.Mood.HAPPY   ->  2
            DragonRules.Mood.NEUTRAL ->  0
            DragonRules.Mood.ANGRY   -> -2
        }
        // This recompute persists, rebuilds state, and notifies observers.
        recomputeAndPersist(moodScore = targetScore)
    }

}
