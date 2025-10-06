package com.TheBudgeteers.dragonomics

import org.junit.Assert.assertEquals
import org.junit.Test

/*
  Desired rules:
  - Base XP per expense: 5
  - Photo bonus: +2 if a photo is attached
  - Mood flat modifier: HAPPY +3, NEUTRAL +0, ANGRY -1
  - Level size: 50 XP per level
 */
class DragonXpTests {

    @Test
    fun angry_noPhoto_gives4xp() {
        val gained = XpCalculator.gain(mood = Mood.ANGRY, hasPhoto = false)
        assertEquals(4, gained)
    }

    @Test
    fun happy_noPhoto_gives8xp() {
        val gained = XpCalculator.gain(mood = Mood.HAPPY, hasPhoto = false)
        assertEquals(8, gained)
    }

    @Test
    fun happy_withPhoto_gives10xp() {
        val gained = XpCalculator.gain(mood = Mood.HAPPY, hasPhoto = true)
        assertEquals(10, gained)
    }

    @Test
    fun level_rollover_from_48_plus4_makes_lvl1_with2xp() {
        val result = XpCalculator.applyGain(
            currentLevel = 0,
            currentXp = 48,
            gained = 4,
            xpPerLevel = 50
        )
        assertEquals(1 to 2, result)
    }

    enum class Mood { HAPPY, NEUTRAL, ANGRY }
}

object XpCalculator {
    private const val BASE = 5
    private const val PHOTO = 2
    private const val XP_PER_LEVEL_DEFAULT = 50

    fun gain(mood: DragonXpTests.Mood, hasPhoto: Boolean): Int {
        val moodFlat = when (mood) {
            DragonXpTests.Mood.HAPPY -> 3
            DragonXpTests.Mood.NEUTRAL -> 0
            DragonXpTests.Mood.ANGRY -> -1
        }
        return BASE + (if (hasPhoto) PHOTO else 0) + moodFlat
    }


    fun applyGain(
        currentLevel: Int,
        currentXp: Int,
        gained: Int,
        xpPerLevel: Int = XP_PER_LEVEL_DEFAULT
    ): Pair<Int, Int> {
        var total = currentXp + gained
        var level = currentLevel
        while (total >= xpPerLevel) {
            total -= xpPerLevel
            level += 1
        }
        return level to total
    }
}
