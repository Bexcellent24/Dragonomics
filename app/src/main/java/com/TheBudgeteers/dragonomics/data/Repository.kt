package com.TheBudgeteers.dragonomics.data

import com.TheBudgeteers.dragonomics.models.*
import com.TheBudgeteers.dragonomics.utils.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Central repository for app data.
// Handles database access and logic combining multiple DAOs.
// Keeps data retrieval logic separate from UI/ViewModels.

class Repository(private val db: AppDatabase) {

    private val transactionDao = db.transactionDao()
    private val nestDao = db.nestDao()
    private val users = db.userDao()

    // ---------- NEST OPERATIONS ----------

    suspend fun addNest(nest: Nest) = nestDao.insert(nest)

    suspend fun getNests() = nestDao.getAll()

    suspend fun getNestById(id: Long) = nestDao.getById(id)

    fun getReactiveNestsFlowByType(type: NestType) =
        nestDao.getAllFlowByType(type.name)

    fun getNestsFlowByType(type: NestType) =
        nestDao.getAllFlow().map { list -> list.filter { it.type == type } }

    fun getSpentAmountFromNestFlow(nestId: Long) =
        transactionDao.getSpentAmountFromNestFlow(nestId)

    fun getSpentAmountForNestInRange(nestId: Long, start: Long, end: Long): Flow<Double> =
        transactionDao.getSpentForNestInRange(nestId, start, end)

    fun getSpentAmountsInRange(start: Long, end: Long): Flow<List<NestSpent>> =
        transactionDao.getSpentAmountsInRangeFlow(start, end)

    // ---------- TRANSACTION OPERATIONS ----------

    suspend fun addTransaction(transaction: Transaction) =
        transactionDao.insert(transaction)

    suspend fun getTransactions() =
        transactionDao.getAll()

    suspend fun getTransactionsByNestId(nestId: Long) =
        transactionDao.getByCategoryId(nestId)

    fun getTransactionsBetweenFlow(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRangeFlow(start, end)

    fun getSpentInCategoryFlow(nestId: Long): Flow<Double> =
        transactionDao.getSpentInCategoryFlow(nestId)

    // Returns transactions with their related nest details
    suspend fun getTransactionsWithNests(): List<TransactionWithNest> =
        transactionDao.getAll().map { mapTransaction(it) }

    fun getTransactionsWithNestsFlow(): Flow<List<TransactionWithNest>> =
        transactionDao.getAllFlow().map { list -> list.map { mapTransaction(it) } }

    fun getTransactionsWithNestBetweenFlow(start: Long, end: Long): Flow<List<TransactionWithNest>> =
        transactionDao.getByDateRangeFlow(start, end).map { list -> list.map { mapTransaction(it) } }

    // Helper to map a transaction to TransactionWithNest
    private suspend fun mapTransaction(transaction: Transaction): TransactionWithNest {
        val categoryNest = nestDao.getById(transaction.categoryId)
        val fromNest = transaction.fromCategoryId?.let { nestDao.getById(it) }
        return TransactionWithNest(transaction, categoryNest, fromNest)
    }

    // ---------- USER OPERATIONS ----------

    suspend fun signUpUser(username: String, email: String, password: String): Result<Long> {
        val u = username.trim()
        val e = email.trim()
        val p = password.toCharArray()

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

    suspend fun updateUserGoals(userId: Long, minGoal: Double?, maxGoal: Double?) =
        users.updateGoals(userId, minGoal, maxGoal)

    suspend fun getUserById(userId: Long): UserEntity? =
        users.getUserById(userId)

    fun getUserFlow(userId: Long): Flow<UserEntity?> =
        users.getUserFlow(userId)

    // ---------- STATS ----------

    fun getMonthlyStatsFlow(start: Long, end: Long): Flow<MonthlyStats> =
        transactionDao.getByDateRangeFlow(start, end).map { transactions ->
            var income = 0.0
            var expenses = 0.0

            transactions.forEach { t ->
                val nest = nestDao.getById(t.categoryId)
                if (nest.type == NestType.INCOME) income += t.amount
                else if (nest.type == NestType.EXPENSE) expenses += t.amount
            }

            MonthlyStats(income = income, expenses = expenses, remaining = income - expenses)
        }
}
