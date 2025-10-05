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

// TransactionViewModel manages transaction data for the UI
// Handles adding new transactions and retrieving transaction lists
// Provides both callback-based and Flow-based access to transaction data
// Keeps all database operations off the main thread using coroutines

class TransactionViewModel(private val repository: Repository) : ViewModel() {

    // begin code attribution
    // MutableStateFlow usage adapted from Kotlin Coroutines documentation

    // Current user ID for filtering transactions
    // When this changes, reactive flows automatically update
    private val _userId = MutableStateFlow<Long?>(null)

    // end code attribution (Kotlin Documentation, 2021)

    fun setUserId(userId: Long) {
        _userId.value = userId
    }


    // Add a new transaction to the database
    // Runs in background thread so UI doesn't freeze
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }


    // Get all transactions for a user
    // Results returned via callback when ready
    fun getTransactions(userId: Long, callback: (List<Transaction>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getTransactions(userId))
        }
    }

    // Get transactions with their nest (category) information
    // Results returned via callback when ready
    fun getTransactionsWithNests(userId: Long, callback: (List<TransactionWithNest>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getTransactionsWithNests(userId))
        }
    }

    // begin code attribution
    // flatMapLatest usage adapted from Kotlin Coroutines documentation

    // Flow that automatically updates when transactions change
    // Switches to correct user's data when userId is updated
    // Returns empty list if no user is set
    val transactionsWithNestsFlow: Flow<List<TransactionWithNest>> =
        _userId.flatMapLatest { userId ->
            if (userId != null) {
                repository.getTransactionsWithNestsFlow(userId)
            } else {
                flowOf(emptyList())
            }
        }

    // end code attribution (Kotlin Documentation, 2021)
}

// reference list
// Kotlin Documentation, 2021. flatMapLatest and StateFlow. [online] Available at: <https://kotlinlang.org/docs/flow.html#flatmaplatest> [Accessed 4 October 2025].