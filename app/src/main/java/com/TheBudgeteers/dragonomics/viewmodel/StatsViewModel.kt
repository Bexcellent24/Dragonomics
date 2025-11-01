package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.MonthlyStats
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * StatsViewModel manages financial statistics and user data for the stats screen.
 * UPDATED FOR FIREBASE: Uses String userId and UserProfile instead of Long/UserEntity.
 */
class StatsViewModel(private val repository: Repository) : ViewModel() {

    // Monthly financial statistics (income, expenses, balance)
    private val _monthlyStats = MutableStateFlow<MonthlyStats?>(null)
    val monthlyStats: StateFlow<MonthlyStats?> = _monthlyStats

    // User profile for goals and personal information (Firebase version)
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    /**
     * Load monthly statistics for a specific time period.
     * UPDATED: Now accepts String userId (Firebase UID).
     */
    fun loadMonthlyStats(userId: String, start: Long, end: Long) {
        viewModelScope.launch {
            repository.getMonthlyStatsFlow(userId, start, end).collect { stats ->
                _monthlyStats.value = stats
            }
        }
    }

    fun loadCurrentPeriodStats(userId: String) {
        viewModelScope.launch {
            repository.getCurrentPeriodStatsFlow(userId).collect { stats ->
                _monthlyStats.value = stats
            }
        }
    }

    /**
     * Load user profile including savings goals.
     * UPDATED: Now accepts String userId and returns UserProfile.
     */
    fun loadUser(userId: String) {
        viewModelScope.launch {
            repository.getUserFlow(userId).collect { user ->
                _userProfile.value = user
            }
        }
    }
}