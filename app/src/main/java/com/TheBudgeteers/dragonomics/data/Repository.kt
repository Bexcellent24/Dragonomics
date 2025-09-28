package com.TheBudgeteers.dragonomics.data


import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.models.Nest

// Repository.kt
// This is the main data layer for the app.
// It connects the DAOs (TransactionDao, NestDao) to the ViewModels.
// Keeps database logic in one place so ViewModels don’t have to worry about queries.
// Acts as a single point to fetch, insert and manage data.

class Repository(private val db: AppDatabase) {

    // Shortcuts to DAOs
    val transactionDao = db.transactionDao()
    val nestDao = db.nestDao()

    // Adds a transaction to the database
    suspend fun addTransaction(transaction: Transaction) = transactionDao.insert(transaction)

    // Gets all transactions
    suspend fun getTransactions() = transactionDao.getAll()

    // Gets transactions between two timestamps
    suspend fun getTransactionsBetween(start: Long, end: Long) =
        transactionDao.getByDateRange(start, end)

    // Adds a new nest to the database
    suspend fun addNest(nest: Nest) = nestDao.insert(nest)

    // Gets all nests
    suspend fun getNests() = nestDao.getAll()

    // Gets a specific nest by its ID
    suspend fun getNestById(id: Long) = nestDao.getById(id)

    // Gets all transactions for a specific nest
    suspend fun getTransactionsByNestId(nestId: Long) = transactionDao.getByCategoryId(nestId)
}