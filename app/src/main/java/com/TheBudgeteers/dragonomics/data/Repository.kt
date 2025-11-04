package com.TheBudgeteers.dragonomics.data

import android.util.Log
import com.TheBudgeteers.dragonomics.gamify.AchievementManager
import com.TheBudgeteers.dragonomics.models.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await


// Central repository for app data using Firebase.
 // Handles Firebase Authentication, Firestore access, and business logic.
// Replaces Room-based Repository with Firebase implementation.
// All data operations filtered by userId (Firebase UID).

class Repository {

    private val authRepo = FirebaseAuthRepository()
    private val nestRepo = FirebaseNestRepository()
    private val transactionRepo = FirebaseTransactionRepository()
    private val achievementRepo = FirebaseAchievementRepository()
    private val firestore = FirebaseFirestore.getInstance()

    // Achievement manager for auto-tracking
    private val achievementManager = AchievementManager(achievementRepo)

    // ---------- AUTHENTICATION ----------

    suspend fun signUpUser(username: String, email: String, password: String): Result<String> =
        authRepo.signUpUser(username, email, password)

    suspend fun loginUser(username: String, password: String): Result<String> =
        authRepo.loginUser(username, password)

    suspend fun updateUserGoals(userId: String, minGoal: Double?, maxGoal: Double?) =
        authRepo.updateUserGoals(userId, minGoal, maxGoal)

    fun getCurrentUserId(): String? = authRepo.getCurrentUserId()

    fun signOut() = authRepo.signOut()

    // ---------- USER PROFILE ----------

    // Get user profile as a reactive Flow.
    fun getUserFlow(userId: String): Flow<UserProfile?> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val profile = snapshot?.let { doc ->
                    if (doc.exists()) {
                        UserProfile(
                            userId = userId,
                            username = doc.getString("username") ?: "",
                            email = doc.getString("email") ?: "",
                            minGoal = doc.getDouble("minGoal"),
                            maxGoal = doc.getDouble("maxGoal")
                        )
                    } else null
                }

                trySend(profile)
            }

        awaitClose { listener.remove() }
    }


    // Get user profile once (non-reactive).
    suspend fun getUser(userId: String): UserProfile? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                UserProfile(
                    userId = userId,
                    username = doc.getString("username") ?: "",
                    email = doc.getString("email") ?: "",
                    minGoal = doc.getDouble("minGoal"),
                    maxGoal = doc.getDouble("maxGoal")
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }


    // ---------- NEST OPERATIONS ----------

    suspend fun addNest(userId: String, nest: Nest): String {
        val nestId = nestRepo.insert(userId, nest)
        achievementManager.onNestCreated(userId)
        return nestId
    }

    suspend fun getNests(userId: String): List<Nest> =
        nestRepo.getAll(userId)

    suspend fun getNestById(userId: String, nestId: String): Nest? =
        nestRepo.getById(userId, nestId)

    fun getNestsFlowByType(userId: String, type: NestType): Flow<List<Nest>> =
        nestRepo.getAllFlowByType(userId, type)

    fun getReactiveNestsFlowByType(userId: String, type: NestType): Flow<List<Nest>> =
        nestRepo.getAllFlowByType(userId, type)

    fun getSpentAmountFromNestFlow(userId: String, nestId: String): Flow<Double> =
        transactionRepo.getSpentAmountFromNestFlow(userId, nestId)

    fun getSpentAmountsInRange(userId: String, start: Long, end: Long): Flow<List<NestSpent>> =
        transactionRepo.getSpentAmountsInRangeFlow(userId, start, end)

    // Update a nest
    suspend fun updateNest(userId: String, nestId: String, updates: Map<String, Any?>): Result<Unit> =
        nestRepo.update(userId, nestId, updates)

    // Delete a nest and reassign its transactions
    suspend fun deleteNest(userId: String, nestId: String): Result<Unit> {
        return try {
            // get or create an "Uncategorized" nest
            val uncategorizedNest = getOrCreateUncategorizedNest(userId)

            // Reassign all transactions from this nest to uncategorized
            reassignTransactionsToUncategorized(userId, nestId, uncategorizedNest.id)

            // Now delete the nest
            nestRepo.delete(userId, nestId).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get or create an "Uncategorized" nest for orphaned transactions
    private suspend fun getOrCreateUncategorizedNest(userId: String): Nest {
        // Check if uncategorized nest already exists
        val existingNests = nestRepo.getAll(userId)
        val uncategorized = existingNests.find { it.name == "Uncategorized" }

        if (uncategorized != null) {
            return uncategorized
        }

        // Create new uncategorized nest
        val newNest = Nest(
            userId = userId,
            name = "Uncategorized",
            budget = null,
            icon = "ci_setting",
            colour = "#666666",
            type = NestType.EXPENSE
        )

        val nestId = nestRepo.insert(userId, newNest)
        return newNest.copy(id = nestId)
    }

    // Reassign all transactions from deleted nest to uncategorized
    private suspend fun reassignTransactionsToUncategorized(userId: String, deletedNestId: String, uncategorizedNestId: String) {
        val transactions = transactionRepo.getByCategoryId(userId, deletedNestId)

        transactions.forEach { transaction ->
            transactionRepo.update(
                userId,
                transaction.id,
                mapOf("categoryId" to uncategorizedNestId)
            ).getOrThrow()
        }
    }


    // ---------- TRANSACTION OPERATIONS ----------

    suspend fun addTransaction(userId: String, transaction: Transaction): String {
        val transactionId = transactionRepo.insert(userId, transaction)

        val nest = nestRepo.getById(userId, transaction.categoryId)
        when (nest?.type) {
            NestType.EXPENSE -> achievementManager.onExpenseLogged(userId)
            NestType.INCOME -> achievementManager.onIncomeLogged(userId)
            else -> {}
        }

        return transactionId
    }

    suspend fun getTransactions(userId: String): List<Transaction> =
        transactionRepo.getAll(userId)

    suspend fun getTransactionsByNestId(userId: String, nestId: String): List<Transaction> =
        transactionRepo.getByCategoryId(userId, nestId)

    fun getTransactionsBetweenFlow(userId: String, start: Long, end: Long): Flow<List<Transaction>> =
        transactionRepo.getByDateRangeFlow(userId, start, end)

    fun getSpentInCategoryFlow(userId: String, nestId: String): Flow<Double> =
        transactionRepo.getSpentInCategoryFlow(userId, nestId)

    suspend fun getTransactionsWithNests(userId: String): List<TransactionWithNest> =
        transactionRepo.getAll(userId).map { mapTransaction(userId, it) }

    fun getTransactionsWithNestsFlow(userId: String): Flow<List<TransactionWithNest>> =
        transactionRepo.getAllFlow(userId).map { list ->
            list.map { mapTransaction(userId, it) }
        }

    fun getTransactionsWithNestBetweenFlow(userId: String, start: Long, end: Long): Flow<List<TransactionWithNest>> =
        transactionRepo.getByDateRangeFlow(userId, start, end).map { list ->
            list.map { mapTransaction(userId, it) }
        }

    // Helper to map Transaction to TransactionWithNest.
    private suspend fun mapTransaction(userId: String, transaction: Transaction): TransactionWithNest {
        val categoryNest = nestRepo.getById(userId, transaction.categoryId)
            ?: Nest(
                id = transaction.categoryId,
                userId = userId,
                name = "Unknown",
                budget = null,
                icon = "",
                colour = "#000000",
                type = NestType.EXPENSE
            )

        val fromNest = transaction.fromCategoryId?.let {
            nestRepo.getById(userId, it)
        }

        return TransactionWithNest(transaction, categoryNest, fromNest)
    }


    // ---------- STATS ----------

    fun getMonthlyStatsFlow(userId: String, start: Long, end: Long): Flow<MonthlyStats> =
        transactionRepo.getByDateRangeFlow(userId, start, end).map { transactions ->
            Log.d("Repository", "getMonthlyStatsFlow - Got ${transactions.size} transactions")

            // Fetch ALL nests ONCE (not per transaction!)
            val allNests = nestRepo.getAll(userId).associateBy { it.id }
            Log.d("Repository", "getMonthlyStatsFlow - Got ${allNests.size} nests")

            var income = 0.0
            var expenses = 0.0

            transactions.forEach { transaction ->
                val nest = allNests[transaction.categoryId]
                Log.d("Repository", "Transaction ${transaction.title}: R${transaction.amount} in ${nest?.name} (${nest?.type})")

                when (nest?.type) {
                    NestType.INCOME -> {
                        income += transaction.amount
                        Log.d("Repository", "  Added to INCOME. Total income now: R$income")
                    }
                    NestType.EXPENSE -> {
                        expenses += transaction.amount
                        Log.d("Repository", "  Added to EXPENSES. Total expenses now: R$expenses")
                    }
                    null -> {
                        Log.w("Repository", "  Transaction has unknown nest ID: ${transaction.categoryId}")
                    }
                }
            }

            val stats = MonthlyStats(income = income, expenses = expenses, remaining = income - expenses)
            Log.d("Repository", "Final stats: Income=$income, Expenses=$expenses, Remaining=${income - expenses}")
            stats
        }

    fun getCurrentPeriodStatsFlow(userId: String): Flow<MonthlyStats> {
        // Get current month date range
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)

        val startCal = java.util.Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val endCal = java.util.Calendar.getInstance().apply {
            set(year, month, getActualMaximum(java.util.Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }

        return transactionRepo.getCurrentPeriodTransactionsFlow(userId, startCal.timeInMillis, endCal.timeInMillis)
            .map { transactions ->
                val allNests = nestRepo.getAll(userId).associateBy { it.id }

                var income = 0.0
                var expenses = 0.0

                transactions.forEach { transaction ->
                    val nest = allNests[transaction.categoryId]
                    when (nest?.type) {
                        NestType.INCOME -> income += transaction.amount
                        NestType.EXPENSE -> expenses += transaction.amount
                        null -> {}
                    }
                }

                MonthlyStats(income = income, expenses = expenses, remaining = income - expenses)
            }
    }

    // Reset budgets for a new month period.
    suspend fun resetForNewMonth(userId: String): Result<Unit> {
        return try {
            // Close the current budget period for all transactions
            transactionRepo.closeCurrentBudgetPeriod(userId).getOrThrow()

            // Mark nests as reset
            nestRepo.markBudgetPeriodReset(userId).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionsByNestIdCurrentPeriod(userId: String, nestId: String): List<Transaction> =
        transactionRepo.getByCategoryIdCurrentPeriod(userId, nestId)



    // ---------- ACHIEVEMENT OPERATIONS ----------

    // Initialize default achievements
    suspend fun initializeAchievements(): Result<Unit> =
        achievementRepo.initializeDefaultAchievements()

    // Get all achievements with user's progress.
    suspend fun getAchievementsWithProgress(userId: String) =
        achievementManager.getAchievementsWithProgress(userId)

   // Get user's current gold balance.
    suspend fun getUserGold(userId: String): Int {
        val user = getUser(userId)
        return user?.gold ?: 0
    }
}
