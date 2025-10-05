package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.HistoryListItem
import com.TheBudgeteers.dragonomics.data.MonthlyStats
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.models.TransactionWithNest
import com.TheBudgeteers.dragonomics.utils.DateUtils
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.Date

// HistoryViewModel manages the transaction history screen
// Shows transactions for a specific month or custom date range
// Groups transactions by date and provides monthly spending stats
// Handles month navigation (prev/next) and custom date filtering

class HistoryViewModel(
    private val repository: Repository,
    private val userId: Long
) : ViewModel() {


    private var currentYear: Int
    private var currentMonth: Int

    // Start and end dates for filtering transactions (as timestamps)
    private val _startDate = MutableStateFlow(0L)
    private val _endDate = MutableStateFlow(0L)

    val startDate: StateFlow<Long> = _startDate.asStateFlow()
    val endDate: StateFlow<Long> = _endDate.asStateFlow()

    init {
        // Initialize to current month
        val cal = Calendar.getInstance()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH)
        setMonth(currentYear, currentMonth)
    }

    // begin code attribution
    // combine() and flatMapLatest() usage adapted from:
    // Kotlin Coroutines documentation: Combining flows

    // Basic transactions between start and end dates
    val transactions: StateFlow<List<Transaction>> =
        combine(_startDate, _endDate) { start, end ->
            Pair(start, end)
        }.flatMapLatest { (start, end) ->
            repository.getTransactionsBetweenFlow(userId, start, end)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Transactions with their nest (category) information attached
    val transactionsWithNest: StateFlow<List<TransactionWithNest>> =
        combine(_startDate, _endDate) { start, end ->
            Pair(start, end)
        }.flatMapLatest { (start, end) ->
            repository.getTransactionsWithNestBetweenFlow(userId, start, end)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // end code attribution (Kotlin Documentation, 2020)


    // Monthly statistics: total income, expenses, and balance
    val monthlyStats: StateFlow<MonthlyStats> =
        combine(_startDate, _endDate) { start, end ->
            Pair(start, end)
        }.flatMapLatest { (start, end) ->
            repository.getMonthlyStatsFlow(userId, start, end)
        }.stateIn(viewModelScope, SharingStarted.Lazily, MonthlyStats(0.0, 0.0, 0.0))


    // begin code attribution
    // groupBy() for grouping list items adapted from:
    // Kotlin Collections documentation

    // Transactions grouped by date with headers for each day
    // Format: [Header(date), Transaction, Transaction, Header(date), Transaction...]
    val groupedTransactions: StateFlow<List<HistoryListItem>> =
        transactionsWithNest.map { list ->
            list.sortedByDescending { it.transaction.date.time }
                .groupBy { stripTime(it.transaction.date) }
                .flatMap { (date, transactionsForDate) ->
                    listOf(HistoryListItem.Header(date.time)) +
                            transactionsForDate.map { HistoryListItem.TransactionItem(it) }
                }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // end code attribution (Kotlin Documentation, 2020)

    // Set a specific month and year
    fun setMonth(year: Int, month: Int) {
        currentYear = year
        currentMonth = month
        val (start, end) = DateUtils.getMonthRange(year, month)
        _startDate.value = start
        _endDate.value = end
    }

    // Go to previous month
    fun prevMonth() {
        if (currentMonth == 0) {
            currentMonth = 11
            currentYear--
        } else {
            currentMonth--
        }
        setMonth(currentYear, currentMonth)
    }

    // Go to next month
    fun nextMonth() {
        if (currentMonth == 11) {
            currentMonth = 0
            currentYear++
        } else {
            currentMonth++
        }
        setMonth(currentYear, currentMonth)
    }

    // Set a custom date range (for advanced filtering)
    fun setCustomRange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end
    }

    fun getMonthDisplayName(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
        }
        // Short format: "Oct 24"
        val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.getDefault())
        val year = (currentYear % 100).toString().padStart(2, '0') // Last 2 digits of year
        return "$monthName $year"
    }


    // Remove time component from date to group transactions by day only
    private fun stripTime(date: Date): Date {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }
}

// reference list
// Kotlin Documentation, 2020. Combining Flows. [online] Available at: <https://kotlinlang.org/docs/flow.html#combine> [Accessed 5 October 2025].
// Kotlin Documentation, 2020. groupBy. [online] Available at: <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/group-by.html> [Accessed 5 October 2025].