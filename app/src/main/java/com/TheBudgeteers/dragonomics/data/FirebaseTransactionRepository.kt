package com.TheBudgeteers.dragonomics.data

import com.TheBudgeteers.dragonomics.models.NestSpent
import com.TheBudgeteers.dragonomics.models.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Firebase Firestore repository for Transaction operations.
 * Replaces TransactionDao with Firestore implementation.
 *
 * Firestore structure:
 * /users/{userId}/transactions/{transactionId}
 *   - title: String
 *   - amount: Double
 *   - date: Long (timestamp)
 *   - photoPath: String?
 *   - description: String?
 *   - categoryId: String (Firestore nest ID)
 *   - fromCategoryId: String? (Firestore nest ID)
 */
class FirebaseTransactionRepository {

    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Get reference to user's transactions collection
     */
    private fun transactionsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("transactions")

    /**
     * Insert a new transaction into Firestore.
     * Returns the generated transaction ID.
     */
    suspend fun insert(userId: String, transaction: Transaction): String {
        val transactionData = hashMapOf(
            "title" to transaction.title,
            "amount" to transaction.amount,
            "date" to transaction.date.time, // Store as timestamp (Long)
            "photoPath" to transaction.photoPath,
            "description" to transaction.description,
            "categoryId" to transaction.categoryId,
            "fromCategoryId" to transaction.fromCategoryId,
            "createdAt" to System.currentTimeMillis()
        )

        val docRef = transactionsCollection(userId).add(transactionData).await()
        return docRef.id
    }

    /**
     * Get all transactions for a user, ordered by date descending.
     */
    suspend fun getAll(userId: String): List<Transaction> {
        val snapshot = transactionsCollection(userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            documentToTransaction(userId, doc.id, doc.data)
        }
    }

    /**
     * Get transactions within a date range.
     */
    suspend fun getByDateRange(userId: String, start: Long, end: Long): List<Transaction> {
        val snapshot = transactionsCollection(userId)
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            documentToTransaction(userId, doc.id, doc.data)
        }
    }

    /**
     * Get transactions for a specific nest/category.
     */
    suspend fun getByCategoryId(userId: String, nestId: String): List<Transaction> {
        val snapshot = transactionsCollection(userId)
            .whereEqualTo("categoryId", nestId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            documentToTransaction(userId, doc.id, doc.data)
        }
    }

    /**
     * Get all transactions as a reactive Flow, ordered by date.
     */
    fun getAllFlow(userId: String): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection(userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    documentToTransaction(userId, doc.id, doc.data)
                } ?: emptyList()

                trySend(transactions)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get transactions within date range as reactive Flow.
     */
    fun getByDateRangeFlow(userId: String, start: Long, end: Long): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection(userId)
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    documentToTransaction(userId, doc.id, doc.data)
                } ?: emptyList()

                trySend(transactions)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get total amount spent from a specific nest (sum of fromCategoryId).
     * Note: Firestore doesn't support aggregation queries natively,
     * so we calculate client-side.
     */
    fun getSpentAmountFromNestFlow(userId: String, nestId: String): Flow<Double> = callbackFlow {
        val listener = transactionsCollection(userId)
            .whereEqualTo("fromCategoryId", nestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Filter to only current period (transactions without budgetPeriod field)
                val total = snapshot?.documents?.filter { doc ->
                    !doc.contains("budgetPeriod") || doc.get("budgetPeriod") == null
                }?.sumOf { doc ->
                    (doc.getDouble("amount") ?: 0.0)
                } ?: 0.0

                trySend(total)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get total spent in a category within date range.
     */
    fun getSpentForNestInRange(userId: String, nestId: String, start: Long, end: Long): Flow<Double> = callbackFlow {
        val listener = transactionsCollection(userId)
            .whereEqualTo("categoryId", nestId)
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val total = snapshot?.documents?.sumOf { doc ->
                    (doc.getDouble("amount") ?: 0.0)
                } ?: 0.0

                trySend(total)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get spent amounts grouped by nest within date range.
     * Returns Flow<List<NestSpent>>.
     *
     * Note: Firestore doesn't support GROUP BY, so we group client-side.
     */
    fun getSpentAmountsInRangeFlow(userId: String, start: Long, end: Long): Flow<List<NestSpent>> = callbackFlow {
        val listener = transactionsCollection(userId)
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Filter to only current period and group by category
                val grouped = mutableMapOf<String, Double>()
                snapshot?.documents?.filter { doc ->
                    !doc.contains("budgetPeriod") || doc.get("budgetPeriod") == null
                }?.forEach { doc ->
                    val categoryId = doc.getString("categoryId") ?: return@forEach
                    val amount = doc.getDouble("amount") ?: 0.0
                    grouped[categoryId] = (grouped[categoryId] ?: 0.0) + amount
                }

                val result = grouped.map { (nestId, spent) ->
                    NestSpent(nestId = nestId, spent = spent)
                }

                trySend(result)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get total spent in a category (all time).
     */
    fun getSpentInCategoryFlow(userId: String, nestId: String): Flow<Double> = callbackFlow {
        val listener = transactionsCollection(userId)
            .whereEqualTo("categoryId", nestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Filter to only current period (transactions without budgetPeriod field)
                val total = snapshot?.documents?.filter { doc ->
                    !doc.contains("budgetPeriod") || doc.get("budgetPeriod") == null
                }?.sumOf { doc ->
                    (doc.getDouble("amount") ?: 0.0)
                } ?: 0.0

                trySend(total)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get total income for a specific nest (all time).
     */
    suspend fun getTotalIncomeForNest(userId: String, nestId: String): Double {
        val snapshot = transactionsCollection(userId)
            .whereEqualTo("categoryId", nestId)
            .get()
            .await()

        // Filter to only current period
        return snapshot.documents.filter { doc ->
            !doc.contains("budgetPeriod") || doc.get("budgetPeriod") == null
        }.sumOf { doc ->
            doc.getDouble("amount") ?: 0.0
        }
    }

    /**
     * Update a transaction.
     */
    suspend fun update(userId: String, transactionId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            transactionsCollection(userId)
                .document(transactionId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a transaction.
     */
    suspend fun delete(userId: String, transactionId: String): Result<Unit> {
        return try {
            transactionsCollection(userId)
                .document(transactionId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert Firestore document to Transaction model.
     */
    private fun documentToTransaction(userId: String, docId: String, data: Map<String, Any?>?): Transaction? {
        if (data == null) return null

        return try {
            Transaction(
                id = docId,
                userId = userId,
                title = data["title"] as? String ?: return null,
                amount = data["amount"] as? Double ?: return null,
                date = Date(data["date"] as? Long ?: return null),
                photoPath = data["photoPath"] as? String,
                description = data["description"] as? String,
                categoryId = data["categoryId"] as? String ?: return null,
                fromCategoryId = data["fromCategoryId"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun closeCurrentBudgetPeriod(userId: String): Result<Unit> {
        return try {
            val currentPeriod = getCurrentMonthPeriod()

            // Get all transactions - we'll filter client-side
            val snapshot = transactionsCollection(userId)
                .get()
                .await()

            // Mark only transactions that don't have a budgetPeriod field yet
            snapshot.documents.filter { doc ->
                !doc.contains("budgetPeriod") || doc.get("budgetPeriod") == null
            }.forEach { doc ->
                doc.reference.update("budgetPeriod", currentPeriod).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current month period as a string (e.g., "2024-11")
     */
    private fun getCurrentMonthPeriod(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        return "$year-${month.toString().padStart(2, '0')}"
    }

    suspend fun getByCategoryIdCurrentPeriod(userId: String, nestId: String): List<Transaction> {
        val snapshot = transactionsCollection(userId)
            .whereEqualTo("categoryId", nestId)
            .get()
            .await()

        // Filter to only current period (no budgetPeriod field)
        return snapshot.documents.filter { doc ->
            !doc.contains("budgetPeriod") || doc.get("budgetPeriod") == null
        }.mapNotNull { doc ->
            documentToTransaction(userId, doc.id, doc.data)
        }
    }

    fun getCurrentPeriodTransactionsFlow(userId: String, start: Long, end: Long): Flow<List<Transaction>> = callbackFlow {
        val listener = transactionsCollection(userId)
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Filter to only current period (transactions without budgetPeriod field)
                val transactions = snapshot?.documents?.filter { doc ->
                    !doc.contains("budgetPeriod") || doc.get("budgetPeriod") == null
                }?.mapNotNull { doc ->
                    documentToTransaction(userId, doc.id, doc.data)
                } ?: emptyList()

                trySend(transactions)
            }

        awaitClose { listener.remove() }
    }

}