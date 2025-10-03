package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: Repository,
    private val userId: Long
) : ViewModel() {

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            repository.getUserFlow(userId).collect { userEntity ->
                _user.value = userEntity
            }
        }
    }

    fun updateGoals(minGoal: Double?, maxGoal: Double?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = _user.value ?: return@launch
                repository.updateUserGoals(userId, minGoal, maxGoal)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getDisplayName(firstName: String, lastName: String): String {
        return when {
            firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
            firstName.isNotEmpty() -> firstName
            lastName.isNotEmpty() -> lastName
            else -> _user.value?.username ?: "User Name"
        }
    }
}