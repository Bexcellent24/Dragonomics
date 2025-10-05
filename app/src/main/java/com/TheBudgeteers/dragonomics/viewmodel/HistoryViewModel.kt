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

class HistoryViewModel(private val repository: Repository, private val userId: Long) : ViewModel() {

    private var currentYear: Int
    private var currentMonth: Int

    private val _startDate = MutableStateFlow(0L)
    private val _endDate = MutableStateFlow(0L)

    val startDate: StateFlow<Long> = _startDate.asStateFlow()
    val endDate: StateFlow<Long> = _endDate.asStateFlow()

    val transactions: StateFlow<List<Transaction>> =
        combine(_startDate, _endDate) { start, end ->
            Pair(start, end)
        }.flatMapLatest { (start, end) ->
            repository.getTransactionsBetweenFlow(userId, start, end)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val transactionsWithNest: StateFlow<List<TransactionWithNest>> =
        combine(_startDate, _endDate) { start, end ->
            Pair(start, end)
        }.flatMapLatest { (start, end) ->
            repository.getTransactionsWithNestBetweenFlow(userId, start, end)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val monthlyStats: StateFlow<MonthlyStats> =
        combine(_startDate, _endDate) { start, end ->
            Pair(start, end)
        }.flatMapLatest { (start, end) ->
            repository.getMonthlyStatsFlow(userId, start, end)
        }.stateIn(viewModelScope, SharingStarted.Lazily, MonthlyStats(0.0, 0.0, 0.0))

    init {
        val cal = Calendar.getInstance()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH)
        setMonth(currentYear, currentMonth)
    }

    fun setMonth(year: Int, month: Int) {
        currentYear = year
        currentMonth = month
        val (start, end) = DateUtils.getMonthRange(year, month)
        _startDate.value = start
        _endDate.value = end
    }

    fun prevMonth() {
        if (currentMonth == 0) {
            currentMonth = 11
            currentYear--
        } else {
            currentMonth--
        }
        setMonth(currentYear, currentMonth)
    }

    fun nextMonth() {
        if (currentMonth == 11) {
            currentMonth = 0
            currentYear++
        } else {
            currentMonth++
        }
        setMonth(currentYear, currentMonth)
    }

    fun setCustomRange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end
    }

    fun getMonthDisplayName(): String {
        return DateUtils.getMonthName(currentYear, currentMonth)
    }

    val groupedTransactions: StateFlow<List<HistoryListItem>> =
        transactionsWithNest.map { list ->
            list.sortedByDescending { it.transaction.date.time }
                .groupBy { stripTime(it.transaction.date) }
                .flatMap { (date, transactionsForDate) ->
                    listOf(HistoryListItem.Header(date.time)) +
                            transactionsForDate.map { HistoryListItem.TransactionItem(it) }
                }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
