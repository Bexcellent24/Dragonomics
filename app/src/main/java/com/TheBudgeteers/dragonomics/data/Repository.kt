package com.TheBudgeteers.dragonomics.data

import com.TheBudgeteers.dragonomics.models.*
import com.TheBudgeteers.dragonomics.utils.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Central repository for app data.
// Handles database access and logic combining multiple DAOs.
// Keeps data retrieval logic separate from UI/ViewModels.
// all data operations filtered by userId

class Repository(private val db: AppDatabase) {

    private val transactionDao = db.transactionDao()
    private val nestDao = db.nestDao()
    private val users = db.userDao()



    // begin code attribution
    // Repository pattern structure and coroutine Flow usage adapted from:
    // Android Developers official guide - "Room with a View" (Kotlin)
    // and Android Architecture Components documentation

    // ---------- NEST OPERATIONS ----------

    suspend fun addNest(nest: Nest) = nestDao.insert(nest)

    suspend fun getNests(userId: Long) = nestDao.getAll(userId)

    suspend fun getNestById(id: Long) = nestDao.getById(id)

    fun getReactiveNestsFlowByType(userId: Long, type: NestType) =
        nestDao.getAllFlowByType(userId, type.name)

    fun getNestsFlowByType(userId: Long, type: NestType) =
        nestDao.getAllFlow(userId).map { list -> list.filter { it.type == type } }

    fun getSpentAmountFromNestFlow(userId: Long, nestId: Long) =
        transactionDao.getSpentAmountFromNestFlow(userId, nestId)

    fun getSpentAmountsInRange(userId: Long, start: Long, end: Long): Flow<List<NestSpent>> =
        transactionDao.getSpentAmountsInRangeFlow(userId, start, end)



    // ---------- TRANSACTION OPERATIONS ----------

    suspend fun addTransaction(transaction: Transaction) =
        transactionDao.insert(transaction)

    suspend fun getTransactions(userId: Long) =
        transactionDao.getAll(userId)

    suspend fun getTransactionsByNestId(userId: Long, nestId: Long) =
        transactionDao.getByCategoryId(userId, nestId)

    fun getTransactionsBetweenFlow(userId: Long, start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getByDateRangeFlow(userId, start, end)

    fun getSpentInCategoryFlow(userId: Long, nestId: Long): Flow<Double> =
        transactionDao.getSpentInCategoryFlow(userId, nestId)

    suspend fun getTransactionsWithNests(userId: Long): List<TransactionWithNest> =
        transactionDao.getAll(userId).map { mapTransaction(it) }

    fun getTransactionsWithNestsFlow(userId: Long): Flow<List<TransactionWithNest>> =
        transactionDao.getAllFlow(userId).map { list -> list.map { mapTransaction(it) } }

    fun getTransactionsWithNestBetweenFlow(userId: Long, start: Long, end: Long): Flow<List<TransactionWithNest>> =
        transactionDao.getByDateRangeFlow(userId, start, end).map { list -> list.map { mapTransaction(it) } }

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

    fun getMonthlyStatsFlow(userId: Long, start: Long, end: Long): Flow<MonthlyStats> =
        transactionDao.getByDateRangeFlow(userId, start, end).map { transactions ->
            var income = 0.0
            var expenses = 0.0

            transactions.forEach { t ->
                val nest = nestDao.getById(t.categoryId)
                if (nest.type == NestType.INCOME) income += t.amount
                else if (nest.type == NestType.EXPENSE) expenses += t.amount
            }

            MonthlyStats(income = income, expenses = expenses, remaining = income - expenses)
        }


    // end code attribution (Android Developers, 2020)
}

// reference list
// Android Developers, 2020. Room with a View (Kotlin). [online] Available at: <https://developer.android.com/codelabs/android-room-with-a-view-kotlin> [Accessed 17 September 2025].
// Android Developers, 2020. Guide to App Architecture. [online] Available at: <https://developer.android.com/topic/libraries/architecture> [Accessed  17 September 2025].