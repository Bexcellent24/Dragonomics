package com.TheBudgeteers.dragonomics.gamify

// begin code attribution
// Use a Kotlin data class to model an immutable value object with auto-generated
// equals/hashCode/toString and copy semantics.
// Adapted from:
// Kotlin, 2024. Data classes. [online]
// Available at: <https://kotlinlang.org/docs/data-classes.html> [Accessed 6 October 2025].
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val medalRes: Int,
    val achieved: Boolean
)