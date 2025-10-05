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


// TransactionViewModel.kt
// This ViewModel manages transaction data for the UI.
// It talks to the Repository to get and add transactions.
// Keeps database work off the main thread using coroutines.
// Works with Repository.kt and TransactionDao.kt.

class TransactionViewModel(private val repository: Repository) : ViewModel() {

    private val _userId = MutableStateFlow<Long?>(null)

    // Adds a transaction — runs in background so UI doesn’t freeze
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    // Gets all transactions and passes them back via a callback
    fun getTransactions(userId: Long, callback: (List<Transaction>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getTransactions(userId))
        }
    }

    fun getTransactionsWithNests(userId: Long, callback: (List<TransactionWithNest>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getTransactionsWithNests(userId))
        }
    }

    val transactionsWithNestsFlow: Flow<List<TransactionWithNest>> =
        _userId.flatMapLatest { userId ->
            if (userId != null) {
                repository.getTransactionsWithNestsFlow(userId)
            } else {
                flowOf(emptyList())
            }
        }

    fun setUserId(userId: Long) {
        _userId.value = userId
    }

}