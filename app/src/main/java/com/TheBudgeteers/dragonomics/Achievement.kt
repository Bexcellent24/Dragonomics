package com.TheBudgeteers.dragonomics

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val medalRes: Int,
    val achieved: Boolean
)
