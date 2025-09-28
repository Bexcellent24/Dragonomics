package com.TheBudgeteers.dragonomics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.TheBudgeteers.dragonomics.models.Transaction

// TransactionDao.kt
// This is the DAO (Data Access Object) for transactions.
// It defines how we insert and query transactions in the database.
// Works with AppDatabase to give the ViewModel access to transaction data.

@Dao
interface TransactionDao {

    // Inserts a transaction into the database
    @Insert
    suspend fun insert(transaction: Transaction)

    // Gets all transactions, ordered from newest to oldest
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAll(): List<Transaction>

    // Gets transactions within a specific date range
    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    suspend fun getByDateRange(start: Long, end: Long): List<Transaction>

    // Gets all transactions for a specific nest/category
    @Query("SELECT * FROM transactions WHERE categoryId = :nestId")
    suspend fun getByCategoryId(nestId: Long): List<Transaction>
}