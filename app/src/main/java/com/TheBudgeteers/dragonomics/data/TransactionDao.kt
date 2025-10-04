package com.TheBudgeteers.dragonomics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.TheBudgeteers.dragonomics.models.NestSpent
import com.TheBudgeteers.dragonomics.models.Transaction
import kotlinx.coroutines.flow.Flow

// DAO for Transaction table.
// Contains methods for inserting and querying transactions,
// including flows for reactive updates and aggregation queries.

@Dao
interface TransactionDao {

    // ---------- INSERT ----------
    @Insert
    suspend fun insert(transaction: Transaction)

    // ---------- BASIC QUERIES ----------
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAll(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    suspend fun getByDateRange(start: Long, end: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE categoryId = :nestId")
    suspend fun getByCategoryId(nestId: Long): List<Transaction>

    // ---------- REACTIVE QUERIES ----------
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRangeFlow(start: Long, end: Long): Flow<List<Transaction>>

    // ---------- AGGREGATE QUERIES ----------
    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :nestId")
    suspend fun getTotalIncomeForNest(nestId: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE fromCategoryId = :nestId")
    fun getSpentAmountFromNestFlow(nestId: Long): Flow<Double?>

    @Query("""SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE categoryId = :nestId AND date BETWEEN :start AND :end """)
    fun getSpentForNestInRange(nestId: Long, start: Long, end: Long): Flow<Double>

    @Query(""" SELECT categoryId AS nestId, IFNULL(SUM(amount), 0) AS spent FROM transactions WHERE date BETWEEN :start AND :end GROUP BY categoryId """)
    fun getSpentAmountsInRangeFlow(start: Long, end: Long): Flow<List<NestSpent>>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE categoryId = :nestId")
    fun getSpentInCategoryFlow(nestId: Long): Flow<Double>
}
