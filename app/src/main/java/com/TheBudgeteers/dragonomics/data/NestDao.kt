package com.TheBudgeteers.dragonomics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.TheBudgeteers.dragonomics.models.Nest


// NestDao.kt
// This is the DAO (Data Access Object) for nests.
// It defines how we insert and query nests in the database.
// Works with AppDatabase so we can get nests and their details for the app.

@Dao
interface NestDao {

    // Inserts a nest into the database
    @Insert
    suspend fun insert(nest: Nest)

    // Gets all nests stored in the database
    @Query("SELECT * FROM nests")
    suspend fun getAll(): List<Nest>

    // Gets a specific nest by its ID
    @Query("SELECT * FROM nests WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Nest
}