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

// TransactionViewModel manages transaction data for the UI.

class TransactionViewModel(private val repository: Repository) : ViewModel() {


    // Current user ID for filtering transactions.
    // When this changes, reactive flows automatically update.
    private val _userId = MutableStateFlow<String?>(null)

    // Set the current user ID.
    fun setUserId(userId: String) {
        _userId.value = userId
    }


    // Add a new transaction to the database.
    fun addTransaction(userId: String, transaction: Transaction, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.addTransaction(userId, transaction)  // This now triggers achievement
                onSuccess?.invoke()
            } catch (e: Exception) {
                android.util.Log.e("TransactionViewModel", "Error adding transaction", e)
            }
        }
    }

    // Get all transactions for a user.
    // Results returned via callback when ready.
    fun getTransactions(userId: String, callback: (List<Transaction>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getTransactions(userId))
        }
    }

    // Get transactions with their nest (category) information.
    fun getTransactionsWithNests(userId: String, callback: (List<TransactionWithNest>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getTransactionsWithNests(userId))
        }
    }


    // Flow that automatically updates when transactions change.
     //Switches to correct user's data when userId is updated.
     // Returns empty list if no user is set.
    val transactionsWithNestsFlow: Flow<List<TransactionWithNest>> =
        _userId.flatMapLatest { userId ->
            if (userId != null) {
                repository.getTransactionsWithNestsFlow(userId)
            } else {
                flowOf(emptyList())
            }
        }
}