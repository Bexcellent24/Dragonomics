package com.TheBudgeteers.dragonomics.data


import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.TransactionWithNest

// Repository.kt
// This is the main data layer for the app.
// It connects the DAOs (TransactionDao, NestDao) to the ViewModels.
// Keeps database logic in one place so ViewModels don’t have to worry about queries.
// Acts as a single point to fetch, insert and manage data.

class Repository(private val db: AppDatabase) {

    // Shortcuts to DAOs
    val transactionDao = db.transactionDao()
    val nestDao = db.nestDao()

    val users = db.userDao()

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

    suspend fun getTransactionsWithNests(): List<TransactionWithNest> {
        val transactions = transactionDao.getAll()
        return transactions.map { transaction ->
            val categoryNest = nestDao.getById(transaction.categoryId)
            val fromNest = transaction.fromCategoryId?.let { nestDao.getById(it) }
            TransactionWithNest(transaction, categoryNest, fromNest)
        }
    }




    // ---------- USERS LOGIN / SIGN UP ----------
    suspend fun signUpUser(username: String, email: String, password: String): Result<Long> {
        val u = username.trim(); val e = email.trim(); val p = password.toCharArray()
        if (u.isBlank() || e.isBlank() || p.isEmpty())
            return Result.failure(IllegalArgumentException("All fields required"))

        if (users.findByUsername(u) != null)
            return Result.failure(IllegalStateException("Username taken"))

        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash(p, salt)
        p.fill('*')

        val id = users.insert(UserEntity(username = u, email = e, passwordHash = hash, salt = salt))
        return Result.success(id)
    }

    suspend fun loginUser(username: String, password: String): Result<UserEntity> {
        val user = users.findByUsername(username.trim())
            ?: return Result.failure(Exception("Invalid credentials"))
        val ok = PasswordHasher.verify(password.toCharArray(), user.salt, user.passwordHash)
        return if (ok) Result.success(user) else Result.failure(Exception("Invalid credentials"))
    }
}