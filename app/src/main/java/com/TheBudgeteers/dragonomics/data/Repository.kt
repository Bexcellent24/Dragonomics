package com.TheBudgeteers.dragonomics.data

import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestSpent
import com.TheBudgeteers.dragonomics.models.NestType
import com.TheBudgeteers.dragonomics.models.TransactionWithNest
import com.TheBudgeteers.dragonomics.models.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Repository(private val db: AppDatabase) {

    val transactionDao = db.transactionDao()
    val nestDao = db.nestDao()
    val users = db.userDao()

    suspend fun addTransaction(transaction: Transaction) = transactionDao.insert(transaction)

    suspend fun getTransactions() = transactionDao.getAll()

    suspend fun addNest(nest: Nest) = nestDao.insert(nest)

    suspend fun getNests() = nestDao.getAll()

    suspend fun getNestById(id: Long) = nestDao.getById(id)

    suspend fun getTransactionsByNestId(nestId: Long) = transactionDao.getByCategoryId(nestId)

    suspend fun getTransactionsWithNests(): List<TransactionWithNest> {
        val transactions = transactionDao.getAll()
        return transactions.map { transaction ->
            val categoryNest = nestDao.getById(transaction.categoryId)
            val fromNest = transaction.fromCategoryId?.let { nestDao.getById(it) }
            TransactionWithNest(transaction, categoryNest, fromNest)
        }
    }

    fun getSpentAmountFromNestFlow(nestId: Long) =
        transactionDao.getSpentAmountFromNestFlow(nestId)

    fun getReactiveNestsFlowByType(type: NestType) =
        nestDao.getAllFlowByType(type.name)

    fun getNestsFlowByType(type: NestType) =
        nestDao.getAllFlow().map { list -> list.filter { it.type == type } }

    fun getTransactionsWithNestsFlow(): Flow<List<TransactionWithNest>> =
        transactionDao.getAllFlow().map { transactions ->
            transactions.map { transaction ->
                val categoryNest = nestDao.getById(transaction.categoryId)
                val fromNest = transaction.fromCategoryId?.let { nestDao.getById(it) }
                TransactionWithNest(transaction, categoryNest, fromNest)
            }
        }

    fun getUserFlow(userId: Long): Flow<UserEntity?> {
        return users.getUserFlow(userId)
    }

    fun getMonthlyStatsFlow(start: Long, end: Long): Flow<MonthlyStats> {
        return transactionDao.getByDateRangeFlow(start, end).map { transactions ->
            var income = 0.0
            var expenses = 0.0

            transactions.forEach { t ->
                val nest = nestDao.getById(t.categoryId)
                if (nest.type == NestType.INCOME) {
                    income += t.amount
                } else if (nest.type == NestType.EXPENSE) {
                    expenses += t.amount
                }
            }

            MonthlyStats(
                income = income,
                expenses = expenses,
                remaining = income - expenses
            )
        }
    }

    fun getTransactionsBetweenFlow(start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getByDateRangeFlow(start, end)
    }

    fun getSpentAmountForNestInRange(nestId: Long, start: Long, end: Long): Flow<Double> {
        return transactionDao.getSpentForNestInRange(nestId, start, end)
    }

    fun getSpentAmountsInRange(start: Long, end: Long): Flow<List<NestSpent>> {
        return transactionDao.getSpentAmountsInRangeFlow(start, end)
    }

    fun getTransactionsWithNestBetweenFlow(start: Long, end: Long): Flow<List<TransactionWithNest>> {
        return transactionDao.getByDateRangeFlow(start, end).map { transactions ->
            transactions.map { transaction ->
                val categoryNest = nestDao.getById(transaction.categoryId)
                val fromNest = transaction.fromCategoryId?.let { nestDao.getById(it) }
                TransactionWithNest(transaction, categoryNest, fromNest)
            }
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

    suspend fun updateUserGoals(userId: Long, minGoal: Double?, maxGoal: Double?) {
        users.updateGoals(userId, minGoal, maxGoal)
    }

    suspend fun getUserById(userId: Long): UserEntity? {
        return users.getUserById(userId)
    }
}