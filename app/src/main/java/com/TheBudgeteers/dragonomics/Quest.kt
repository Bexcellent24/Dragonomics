package com.TheBudgeteers.dragonomics

data class Quest(
    val id: String,
    val title: String,
    val iconRes: Int,
    val rewardText: String?,
    val completed: Boolean
)
