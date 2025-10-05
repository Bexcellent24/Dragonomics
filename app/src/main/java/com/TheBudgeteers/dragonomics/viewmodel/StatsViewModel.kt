package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.MonthlyStats
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// StatsViewModel manages financial statistics and user data for the stats screen
// Loads monthly income/expense summaries for a given date range
// Provides user information for goal comparison and personalization
// Reactive flows automatically update when data changes

class StatsViewModel(private val repository: Repository) : ViewModel() {

    // begin code attribution
    // StateFlow usage adapted from Kotlin Coroutines documentation

    // Monthly financial statistics (income, expenses, balance)
    private val _monthlyStats = MutableStateFlow<MonthlyStats?>(null)
    val monthlyStats: StateFlow<MonthlyStats?> = _monthlyStats

    // User entity for goals and personal information
    private val _userEntity = MutableStateFlow<UserEntity?>(null)
    val userEntity: StateFlow<UserEntity?> = _userEntity.asStateFlow()
    // end code attribution (Kotlin Documentation, 2021)


    // Load monthly statistics for a specific time period
    // Automatically updates when transactions in the range change
    fun loadMonthlyStats(userId: Long, start: Long, end: Long) {
        viewModelScope.launch {
            repository.getMonthlyStatsFlow(userId, start, end).collect { stats ->
                _monthlyStats.value = stats
            }
        }
    }

    // Load user data including savings goals
    // Automatically updates when user information changes
    fun loadUser(userId: Long) {
        viewModelScope.launch {
            repository.getUserFlow(userId).collect { user ->
                _userEntity.value = user
            }
        }
    }
}

// reference list
// Kotlin Documentation, 2021. StateFlow and MutableStateFlow. [online] Available at: <https://kotlinlang.org/docs/flow-stateflow-and-sharedflow.html#stateflow> [Accessed 5 October 2025].