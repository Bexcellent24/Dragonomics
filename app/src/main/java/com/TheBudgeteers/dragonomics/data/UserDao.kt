package com.TheBudgeteers.dragonomics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.TheBudgeteers.dragonomics.models.UserEntity
import kotlinx.coroutines.flow.Flow

// DAO for the User table.
// Handles inserting users, retrieving user data, and updating goals.
// Used for login, sign-up, and goal management.

@Dao
interface UserDao {

    // ---------- INSERT ----------
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    // ---------- QUERIES ----------
    // Finds a user by username (used for login)
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    // Gets a user as a reactive Flow
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserFlow(id: Long): Flow<UserEntity?>

    // Gets a user by ID (non-reactive)
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    // ---------- UPDATES ----------
    // Updates a user's goal values
    @Query("UPDATE users SET minGoal = :minGoal, maxGoal = :maxGoal WHERE id = :userId")
    suspend fun updateGoals(userId: Long, minGoal: Double?, maxGoal: Double?)
}
