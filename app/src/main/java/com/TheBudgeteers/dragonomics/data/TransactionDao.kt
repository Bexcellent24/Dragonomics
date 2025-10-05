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
// filters by user ID for multi-user support

@Dao
interface TransactionDao {

    // begin code attribution
    // DAO structure and annotations are based on:
    // Android Developers official guide to Room DAO interfaces


    // ---------- INSERT ----------
    @Insert
    suspend fun insert(transaction: Transaction)



    // ---------- BASIC QUERIES (FILTERED BY USER) ----------
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAll(userId: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :start AND :end ORDER BY date DESC")
    suspend fun getByDateRange(userId: Long, start: Long, end: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND categoryId = :nestId")
    suspend fun getByCategoryId(userId: Long, nestId: Long): List<Transaction>



    // ---------- REACTIVE QUERIES (FILTERED BY USER) ----------
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllFlow(userId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRangeFlow(userId: Long, start: Long, end: Long): Flow<List<Transaction>>



    // ---------- AGGREGATE QUERIES (FILTERED BY USER) ----------
    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND categoryId = :nestId")
    suspend fun getTotalIncomeForNest(userId: Long, nestId: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND fromCategoryId = :nestId")
    fun getSpentAmountFromNestFlow(userId: Long, nestId: Long): Flow<Double?>

    @Query("""SELECT IFNULL(SUM(amount), 0) FROM transactions 
              WHERE userId = :userId AND categoryId = :nestId AND date BETWEEN :start AND :end """)
    fun getSpentForNestInRange(userId: Long, nestId: Long, start: Long, end: Long): Flow<Double>

    @Query(""" SELECT categoryId AS nestId, IFNULL(SUM(amount), 0) AS spent 
               FROM transactions 
               WHERE userId = :userId AND date BETWEEN :start AND :end 
               GROUP BY categoryId """)
    fun getSpentAmountsInRangeFlow(userId: Long, start: Long, end: Long): Flow<List<NestSpent>>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE userId = :userId AND categoryId = :nestId")
    fun getSpentInCategoryFlow(userId: Long, nestId: Long): Flow<Double>

    // end code attribution (Android Developers, 2020)
}

// reference list
// Android Developers, 2020. Access data using Room DAOs. [online] Available at: <https://developer.android.com/training/data-storage/room/accessing-data> [Accessed 16 September 2025].