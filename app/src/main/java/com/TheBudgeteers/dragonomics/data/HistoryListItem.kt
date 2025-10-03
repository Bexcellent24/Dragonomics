package com.TheBudgeteers.dragonomics.data

import com.TheBudgeteers.dragonomics.models.TransactionWithNest

sealed class HistoryListItem {
    data class Header(val dateMillis: Long) : HistoryListItem()
    data class TransactionItem(val transactionWithNest: TransactionWithNest) : HistoryListItem()
}
