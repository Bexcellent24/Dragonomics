package com.TheBudgeteers.dragonomics.models

// Temporary placeholder model for quest display.
// Will be replaced when full quest logic is implemented.

data class Quest(
    val id: String,
    val title: String,
    val iconRes: Int,       
    val rewardText: String?,
    val completed: Boolean
)
