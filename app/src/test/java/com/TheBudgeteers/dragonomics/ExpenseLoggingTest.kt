package com.TheBudgeteers.dragonomics.gamify

import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class ExpenseLoggingDragonGameTest {

    @Before
    fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun newGameWithState(start: DragonState): DragonGame {
        val store = mockk<DragonStore>(relaxed = true)
        every { store.load() } returns start
        return DragonGame(store)
    }

    @Test
    fun expense_neutral_noPhoto_gains25_levelsUpExactly() {
        val start = DragonState(
            totalXp = 0, level = 0, xpIntoLevel = 0,
            moodScore = 0, mood = DragonRules.Mood.NEUTRAL, lastLoginYmd = 0
        )
        val game = newGameWithState(start)

        game.onExpenseLogged(addedPhoto = false)

        assertThat(game.state.totalXp).isEqualTo(25)
        assertThat(game.state.level).isEqualTo(1)
        assertThat(game.state.xpIntoLevel).isEqualTo(0)
    }

    @Test
    fun expense_happy_withPhoto_gains30_levelsAndRemainder5() {
        val start = DragonState(
            totalXp = 0, level = 0, xpIntoLevel = 0,
            moodScore = 2,
            mood = DragonRules.Mood.HAPPY, lastLoginYmd = 0
        )
        val game = newGameWithState(start)

        game.onExpenseLogged(addedPhoto = true)

        assertThat(game.state.totalXp).isEqualTo(30)
        assertThat(game.state.level).isEqualTo(1)
        assertThat(game.state.xpIntoLevel).isEqualTo(5)
        assertThat(game.state.mood).isEqualTo(DragonRules.Mood.HAPPY)
    }

    @Test
    fun expense_angry_noPhoto_gains24_rolloverMathCorrect() {
        val start = DragonState(
            totalXp = 23, level = 0, xpIntoLevel = 23,
            moodScore = -2, // ensures ANGRY
            mood = DragonRules.Mood.ANGRY, lastLoginYmd = 0
        )
        val game = newGameWithState(start)

        game.onExpenseLogged(addedPhoto = false)

        assertThat(game.state.totalXp).isEqualTo(47)
        assertThat(game.state.level).isEqualTo(1)
        assertThat(game.state.xpIntoLevel).isEqualTo(22)
        assertThat(game.state.mood).isEqualTo(DragonRules.Mood.ANGRY)
    }

    @Test
    fun setOverallMood_forcesExactBand_thenExpenseUsesThatBonus() {
        val game = newGameWithState(
            DragonState(
                totalXp = 0, level = 0, xpIntoLevel = 0, moodScore = 0,
                mood = DragonRules.Mood.NEUTRAL, lastLoginYmd = 0
            )
        )

        game.setOverallMood(DragonRules.Mood.HAPPY)
        assertThat(game.state.mood).isEqualTo(DragonRules.Mood.HAPPY)

        game.onExpenseLogged(addedPhoto = false)
        assertThat(game.state.totalXp).isEqualTo(28)
        assertThat(game.state.level).isEqualTo(1)
        assertThat(game.state.xpIntoLevel).isEqualTo(3)
    }
}
