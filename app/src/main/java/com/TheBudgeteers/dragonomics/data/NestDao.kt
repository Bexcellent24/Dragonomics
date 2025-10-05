package com.TheBudgeteers.dragonomics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.TheBudgeteers.dragonomics.models.Nest
import kotlinx.coroutines.flow.Flow

// DAO for the Nest table.
// Handles inserting and fetching nests from the database.
// Used by the repository and ViewModels for reactive data access.
// Filters user by ID for multi-user support

@Dao
interface NestDao {

    // Inserts a new nest entry
    @Insert
    suspend fun insert(nest: Nest)

    // Returns all nests for a specific user as a list (non-reactive)
    @Query("SELECT * FROM nests WHERE userId = :userId")
    suspend fun getAll(userId: Long): List<Nest>

    // Returns a single nest by ID (no userId filter needed since ID is unique)
    @Query("SELECT * FROM nests WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Nest

    // Returns all nests for a specific user as a reactive Flow
    @Query("SELECT * FROM nests WHERE userId = :userId")
    fun getAllFlow(userId: Long): Flow<List<Nest>>

    // Returns reactive list of nests filtered by type (income/outgoing) for a user
    @Query("""SELECT DISTINCT nests.* FROM nests 
              LEFT JOIN transactions ON transactions.categoryId = nests.id 
              WHERE nests.userId = :userId AND nests.type = :type """)
    fun getAllFlowByType(userId: Long, type: String): Flow<List<Nest>>
}