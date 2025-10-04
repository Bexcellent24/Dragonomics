package com.TheBudgeteers.dragonomics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.TheBudgeteers.dragonomics.models.Nest
import kotlinx.coroutines.flow.Flow

// DAO for the Nest table.
// Handles inserting and fetching nests from the database.
// Used by the repository and ViewModels for reactive data access.

@Dao
interface NestDao {

    // Inserts a new nest entry
    @Insert
    suspend fun insert(nest: Nest)

    // Returns all nests as a list (non-reactive)
    @Query("SELECT * FROM nests")
    suspend fun getAll(): List<Nest>

    // Returns a single nest by ID
    @Query("SELECT * FROM nests WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Nest

    // Returns all nests as a reactive Flow
    @Query("SELECT * FROM nests")
    fun getAllFlow(): Flow<List<Nest>>

    // Returns reactive list of nests filtered by type (income/outgoing)
    @Query("""SELECT DISTINCT nests.* FROM nests LEFT JOIN transactions ON transactions.categoryId = nests.id WHERE nests.type = :type """)
    fun getAllFlowByType(type: String): Flow<List<Nest>>
}
