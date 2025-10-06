package com.TheBudgeteers.dragonomics.gamify

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import com.TheBudgeteers.dragonomics.R
import java.util.Calendar

/*
 Purpose:
  - Central place for all tunable gameplay numbers and mapping rules
    (XP curve, moods, icon selection).
 */

object DragonRules {

    //Tunable progression values
    const val XP_PER_LEVEL = 25
    const val XP_EXPENSE = 25
    const val XP_PHOTO_BONUS = 2

    //Tunable dragon mood bonuses
    const val HAPPY_BONUS = 3
    const val NEUTRAL_BONUS = 0
    const val ANGRY_BONUS = -1

    //Guard so an action always grants at least 1 XP
    const val MIN_XP_PER_ACTION = 1

    fun levelFromXp(totalXp: Int): Int = totalXp / XP_PER_LEVEL
    fun xpIntoLevel(totalXp: Int): Int = totalXp % XP_PER_LEVEL

    //Mood type enums
    enum class Mood { HAPPY, NEUTRAL, ANGRY }

    //Maps numeric int to the associated mood
    fun moodFromScore(score: Int): Mood =
        when {
            score >= 2  -> Mood.HAPPY
            score <= -2 -> Mood.ANGRY
            else        -> Mood.NEUTRAL
        }

    // Apply flat mood bonus to base XP
    fun applyMoodBonus(base: Int, mood: Mood): Int {
        val bonus = when (mood) {
            Mood.HAPPY   -> HAPPY_BONUS
            Mood.NEUTRAL -> NEUTRAL_BONUS
            Mood.ANGRY   -> ANGRY_BONUS
        }
        return maxOf(MIN_XP_PER_ACTION, base + bonus)
    }

    // begin code attribution
    // Annotate functions that return drawable resource IDs with @DrawableRes for Lint/type safety.
    // Adapted from:
    // Android Developers, 2020. androidx.annotation.DrawableRes. [online]
    // Available at: <https://developer.android.com/reference/androidx/annotation/DrawableRes> [Accessed 6 October 2025].

    //Displays the relevant dragon image based on dragon's level and mood
    @DrawableRes
    fun dragonImageFor(level: Int, mood: Mood): Int = when {
        level >= 10 -> when (mood) { // Adult from (Level >= 10)
            // these will be replaced with gifs
            Mood.HAPPY   -> R.drawable.adult_happy
            Mood.NEUTRAL -> R.drawable.adult_neutral
            Mood.ANGRY   -> R.drawable.adult_angry
        }
        level >= 5 -> when (mood) {   // Teen from 5 to 9
            Mood.HAPPY   -> R.drawable.teen_happy
            Mood.NEUTRAL -> R.drawable.teen_neutral
            Mood.ANGRY   -> R.drawable.teen_angry /// just double checking this works
        }
        else       -> when (mood) {    // baby start
            Mood.HAPPY   -> R.drawable.baby_happy
            Mood.NEUTRAL -> R.drawable.baby_neutral
            Mood.ANGRY   -> R.drawable.baby_sad
        }
    }
    // end code attribution (Android Developers, 2020)

    //Displays the relevant icons based on dragon's mood
    @DrawableRes
    fun moodIconFor(mood: Mood): Int = when (mood) {
        Mood.HAPPY   -> R.drawable.happy_mood
        Mood.NEUTRAL -> R.drawable.neutral_mood
        Mood.ANGRY   -> R.drawable.angry_mood
    }
}

//Dragon's current game state
data class DragonState(
    val totalXp: Int = 0,
    val level: Int = 0,
    val xpIntoLevel: Int = 0,
    val moodScore: Int = 0,
    val mood: DragonRules.Mood = DragonRules.Mood.NEUTRAL,
    val lastLoginYmd: Int = 0
)


class DragonStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dragon_store", Context.MODE_PRIVATE)

    //Load persisted values and rebuild a consistent DragonState.
    fun load(): DragonState {
        val xp = prefs.getInt("xp", 0)
        val moodScore = prefs.getInt("moodScore", 0)
        val mood = DragonRules.moodFromScore(moodScore)
        val level = DragonRules.levelFromXp(xp)
        val into = DragonRules.xpIntoLevel(xp)
        val last = prefs.getInt("lastLoginYmd", 0)
        return DragonState(xp, level, into, moodScore, mood, last)
    }
    //Persist only the authoritative fields
    fun save(state: DragonState) {
        prefs.edit()
            .putInt("xp", state.totalXp)
            .putInt("moodScore", state.moodScore)
            .putInt("lastLoginYmd", state.lastLoginYmd)
            .apply()
    }
}

class DragonGame(private val store: DragonStore) {

    var state: DragonState = store.load()
        private set


     //Centralised recomputation: Accepts XP, moodScore, lastLogin and rebuilds a new state
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
        // Broadcast to any collectors
        DragonGameEvents.notifyChanged(state)
    }

    // Call once per real day on app open
    fun onDailyLogin() {
        val today = todayYmd()
        if (state.lastLoginYmd == today) return
        recomputeAndPersist(moodScore = state.moodScore + 1, lastLoginYmd = today)
    }

    //Award XP for logging an expense then re-evaluate mood state
    fun onExpenseLogged(addedPhoto: Boolean) {
        var base = DragonRules.XP_EXPENSE
        if (addedPhoto) base += DragonRules.XP_PHOTO_BONUS
        val effective = DragonRules.applyMoodBonus(base, state.mood)

        //Debugging
        android.util.Log.d("DragonXP",
            "Expense BEFORE: base=$base mood=${state.mood} effective=$effective " +
                    "total=${state.totalXp}"
        )

        recomputeAndPersist(totalXp = state.totalXp + effective)

        //Debugging
        android.util.Log.d("DragonXP",
            "Expense AFTER:  L${state.level} ${state.xpIntoLevel}/${DragonRules.XP_PER_LEVEL} " +
                    "total=${state.totalXp}"
        )
    }

    //Update moodScore based on nest mood averages
    //Positive nests brings mood up, overspending brings it down.
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

    // begin code attribution
    // Construct an integer date stamp (YYYYMMDD) using java.util.Calendar (note: MONTH is zero-based).
    // Adapted from:
    // Oracle, 2024. java.util.Calendar. [online]
    // Available at: <https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html> [Accessed 6 October 2025].

    //Current date as YYYYMMDD
    private fun todayYmd(): Int {
        val c = Calendar.getInstance()
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return y * 10000 + m * 100 + d
    }
    // end code attribution (Oracle, 2024)

    //Force a specific mood band and refresh dependent visuals.
    fun setOverallMood(m: DragonRules.Mood) {
        //pick representative band edges so moodFromScore() returns exactly this mood
        val targetScore = when (m) {
            DragonRules.Mood.HAPPY   ->  2
            DragonRules.Mood.NEUTRAL ->  0
            DragonRules.Mood.ANGRY   -> -2
        }
        recomputeAndPersist(moodScore = targetScore)
    }
}
// reference list
// Android Developers, 2020. androidx.annotation.DrawableRes. [online]
// Available at: <https://developer.android.com/reference/androidx/annotation/DrawableRes> [Accessed 6 October 2025].
// Oracle, 2024. java.util.Calendar. [online]
// Available at: <https://docs.oracle.com/javase/8/docs/api/java/util/Calendar.html> [Accessed 6 October 2025].
