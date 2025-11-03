package com.TheBudgeteers.dragonomics.data

class Enums {
}

enum class NestLayoutType {
    GRID, LIST, HISTORY
}

enum class NestType {
    INCOME,
    EXPENSE
}

// Possible moods for a nest based on budget progress
enum class Mood {
    POSITIVE, // On track
    NEUTRAL,  // Borderline
    NEGATIVE  // Overspent
}

