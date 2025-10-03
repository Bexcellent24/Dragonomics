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

class StatsViewModel(private val repository: Repository) : ViewModel() {

    private val _monthlyStats = MutableStateFlow<MonthlyStats?>(null)
    val monthlyStats: StateFlow<MonthlyStats?> = _monthlyStats

    private val _userEntity = MutableStateFlow<UserEntity?>(null)
    val userEntity: StateFlow<UserEntity?> = _userEntity.asStateFlow()

    fun loadMonthlyStats(start: Long, end: Long) {
        viewModelScope.launch {
            repository.getMonthlyStatsFlow(start, end).collect { stats ->
                _monthlyStats.value = stats
            }
        }
    }

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            repository.getUserFlow(userId).collect { user ->
                _userEntity.value = user
            }
        }
    }
}