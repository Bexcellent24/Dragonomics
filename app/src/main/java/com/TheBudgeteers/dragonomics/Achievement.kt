package com.TheBudgeteers.dragonomics

/*
Achievement

Purpose:
  - Immutable value object representing a single achievement row.
  - Consumed by AchievementsAdapter to render medal/title/description and completion state.

References:
  - Kotlin language: Data classes.
      * https://kotlinlang.org/docs/data-classes.html

Author: Kotlin | Date: 2025-10-05
*/

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val medalRes: Int,
    val achieved: Boolean
)
