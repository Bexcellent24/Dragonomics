package com.TheBudgeteers.dragonomics.data

class Enums {
}

enum class NestLayoutType {
    GRID, LIST, HISTORY
}

enum class QuestType {
    DAILY_STREAK,      // Login X days in a row
    LOG_EXPENSES,      // Log X transactions
    HIT_MIN_GOAL,      // Stay above min goal
    HIT_MAX_GOAL,      // Reach max goal
    SAVE_AMOUNT,       // Save X amount of money
    CATEGORIZE_ALL,    // Categorize all transactions in a period
    PHOTO_RECEIPTS,    // Add photos to X receipts
    WEEKLY_REVIEW      // Review spending weekly
}

