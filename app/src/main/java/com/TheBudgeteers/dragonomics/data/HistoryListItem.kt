package com.TheBudgeteers.dragonomics.data

import com.TheBudgeteers.dragonomics.models.TransactionWithNest


// Used to group transactions by date in the history list.
// Acts as a parent type for headers (dates) and actual transaction items.

sealed class HistoryListItem {

    // Header item, represents a date section in the list
    data class Header(val dateMillis: Long) : HistoryListItem()

    // Transaction item, holds a transaction and its linked nest
    data class TransactionItem(val transactionWithNest: TransactionWithNest) : HistoryListItem()
}