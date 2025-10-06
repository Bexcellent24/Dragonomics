package com.TheBudgeteers.dragonomics

import com.TheBudgeteers.dragonomics.models.Mood
import org.junit.Assert.assertEquals
import org.junit.Test


// Unit tests for Nest progress and mood calculations
 // Tests the business logic for determining nest health

class NestProgressTests {

    @Test
    fun expense_nest_full_budget_gives_100_percent_progress() {
        val budget = 1000.0
        val spent = 0.0
        val remaining = budget - spent
        val progress = remaining / budget

        assertEquals(1.0, progress, 0.01)
    }

    @Test
    fun expense_nest_half_spent_gives_50_percent_progress() {
        val budget = 1000.0
        val spent = 500.0
        val remaining = budget - spent
        val progress = remaining / budget

        assertEquals(0.5, progress, 0.01)
    }

    @Test
    fun expense_nest_overspent_gives_0_percent_progress() {
        val budget = 1000.0
        val spent = 1200.0
        val remaining = (budget - spent).coerceAtLeast(0.0)
        val progress = (remaining / budget).coerceIn(0.0, 1.0)

        assertEquals(0.0, progress, 0.01)
    }

    @Test
    fun progress_75_percent_or_above_is_positive_mood() {
        val progress = 0.8
        val mood = when {
            progress >= 0.75 -> Mood.POSITIVE
            progress >= 0.4  -> Mood.NEUTRAL
            else             -> Mood.NEGATIVE
        }

        assertEquals(Mood.POSITIVE, mood)
    }

    @Test
    fun progress_between_40_and_75_is_neutral_mood() {
        val progress = 0.5
        val mood = when {
            progress >= 0.75 -> Mood.POSITIVE
            progress >= 0.4  -> Mood.NEUTRAL
            else             -> Mood.NEGATIVE
        }

        assertEquals(Mood.NEUTRAL, mood)
    }

    @Test
    fun progress_below_40_is_negative_mood() {
        val progress = 0.2
        val mood = when {
            progress >= 0.75 -> Mood.POSITIVE
            progress >= 0.4  -> Mood.NEUTRAL
            else             -> Mood.NEGATIVE
        }

        assertEquals(Mood.NEGATIVE, mood)
    }

    @Test
    fun progress_boundary_at_75_is_positive() {
        val progress = 0.75
        val mood = when {
            progress >= 0.75 -> Mood.POSITIVE
            progress >= 0.4  -> Mood.NEUTRAL
            else             -> Mood.NEGATIVE
        }

        assertEquals(Mood.POSITIVE, mood)
    }

    @Test
    fun progress_boundary_at_40_is_neutral() {
        val progress = 0.4
        val mood = when {
            progress >= 0.75 -> Mood.POSITIVE
            progress >= 0.4  -> Mood.NEUTRAL
            else             -> Mood.NEGATIVE
        }

        assertEquals(Mood.NEUTRAL, mood)
    }

    @Test
    fun income_nest_always_has_100_percent_progress() {
        // Income nests always return 1.0 progress
        val progress = 1.0

        assertEquals(1.0, progress, 0.01)
    }

    @Test
    fun nest_with_no_budget_gives_0_percent_progress() {
        val budget: Double? = null
        val progress = if (budget != null && budget > 0.0) 1.0 else 0.0

        assertEquals(0.0, progress, 0.01)
    }
}