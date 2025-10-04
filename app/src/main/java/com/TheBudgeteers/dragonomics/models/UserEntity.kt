package com.TheBudgeteers.dragonomics.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Room entity for storing user accounts.
// Holds login credentials and user goal settings.
// Username is unique across users.

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)] // Enforce unique usernames
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // DB primary key
    val username: String,                              // Unique username
    val email: String,                                 // Email address
    val passwordHash: String,                          // Hashed password
    val salt: String,                                  // Salt for password hashing

    val minGoal: Double? = null,                       // Optional minimum goal
    val maxGoal: Double? = null                        // Optional maximum goal
)
