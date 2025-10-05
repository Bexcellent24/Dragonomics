package com.TheBudgeteers.dragonomics.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Room entity for storing user accounts.
// Holds login credentials and user goal settings.
// Username is unique across users.


// begin code attribution
// Entity structure and Room annotations adapted from:
// Android Developers official guide to defining entities in Room

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
// end code attribution (Android Developers, 2020)

// reference list
// Android Developers, 2020. Define data using Room entities. [online] Available at: <https://developer.android.com/training/data-storage/room/defining-data> [Accessed 17 September 2025].