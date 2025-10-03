package com.TheBudgeteers.dragonomics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.TheBudgeteers.dragonomics.models.UserEntity
import kotlinx.coroutines.flow.Flow

// This is the DAO (Data Access Object) for users.
// It defines how we compare and query user input checks in the database.
// Works with AppDatabase so we can get compare usernames and passwords for Login / Sign up.
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserFlow(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("UPDATE users SET minGoal = :minGoal, maxGoal = :maxGoal WHERE id = :userId")
    suspend fun updateGoals(userId: Long, minGoal: Double?, maxGoal: Double?)
}
