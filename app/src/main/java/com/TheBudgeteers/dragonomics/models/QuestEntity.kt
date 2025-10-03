package com.TheBudgeteers.dragonomics.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
  Quest entity for storing user quests in the database.
  Tracks quest progress and completion status.
*/

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val questType: QuestType,
    val title: String,
    val description: String?,
    val iconRes: String, // Store icon name as string, convert to resource ID in UI
    val rewardXp: Int,
    val targetValue: Int, // e.g., "log 3 expenses" -> targetValue = 3
    val currentValue: Int = 0, // Current progress
    val completed: Boolean = false,
    val completedDate: Long? = null
)


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