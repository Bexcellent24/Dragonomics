package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.models.TransactionWithNest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * TransactionViewModel manages transaction data for the UI.
 * UPDATED FOR FIREBASE: Uses String userId instead of Long.
 */
class TransactionViewModel(private val repository: Repository) : ViewModel() {

    /**
     * Current user ID for filtering transactions.
     * When this changes, reactive flows automatically update.
     * UPDATED: Now uses String (Firebase UID) instead of Long.
     */
    private val _userId = MutableStateFlow<String?>(null)

    /**
     * Set the current user ID.
     * UPDATED: Now accepts String (Firebase UID).
     */
    fun setUserId(userId: String) {
        _userId.value = userId
    }

    /**
     * Add a new transaction to the database.
     * Runs in background thread so UI doesn't freeze.
     * UPDATED: Now accepts String userId.
     */
    fun addTransaction(userId: String, transaction: Transaction) {
        viewModelScope.launch {
            repository.addTransaction(userId, transaction)
        }
    }

    /**
     * Get all transactions for a user.
     * Results returned via callback when ready.
     * UPDATED: Now accepts String userId.
     */
    fun getTransactions(userId: String, callback: (List<Transaction>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getTransactions(userId))
        }
    }

    /**
     * Get transactions with their nest (category) information.
     * Results returned via callback when ready.
     * UPDATED: Now accepts String userId.
     */
    fun getTransactionsWithNests(userId: String, callback: (List<TransactionWithNest>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getTransactionsWithNests(userId))
        }
    }

    /**
     * Flow that automatically updates when transactions change.
     * Switches to correct user's data when userId is updated.
     * Returns empty list if no user is set.
     * UPDATED: Now uses String userId (Firebase UID).
     */
    val transactionsWithNestsFlow: Flow<List<TransactionWithNest>> =
        _userId.flatMapLatest { userId ->
            if (userId != null) {
                repository.getTransactionsWithNestsFlow(userId)
            } else {
                flowOf(emptyList())
            }
        }
}