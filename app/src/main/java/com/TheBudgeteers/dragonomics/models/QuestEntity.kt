package com.TheBudgeteers.dragonomics.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.TheBudgeteers.dragonomics.data.QuestType

// Represents a quest stored in the database.
// Tracks quest progress, type, and completion.
// Used to persist quest state for a given user.

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,           // Which user this quest belongs to
    val questType: QuestType,   // Type of quest
    val title: String,          // Quest title
    val description: String?,   // Optional description
    val iconRes: String,        // Icon name stored as string
    val rewardXp: Int,          // XP reward for completing quest
    val targetValue: Int,       // Goal value for quest progress
    val currentValue: Int = 0,  // Current progress value
    val completed: Boolean = false, // Whether quest is completed
    val completedDate: Long? = null // Timestamp when completed
)



