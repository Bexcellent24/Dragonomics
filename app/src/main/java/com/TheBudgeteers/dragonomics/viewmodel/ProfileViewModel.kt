package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ProfileViewModel manages user profile data and settings
// Handles loading user information and updating savings goals
// Provides formatted display names for the UI
// Tracks loading state for a better user experience

class ProfileViewModel(
    private val repository: Repository,
    private val userId: Long
) : ViewModel() {

    // Current user entity with all profile information
    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    // Loading indicator for goal updates
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUser()
    }

    // begin code attribution
    // Collecting Flow in ViewModel adapted from:
    // Android Developers: StateFlow and SharedFlow

    // Start observing user data from the database
    // Updates automatically when user info changes
    private fun loadUser() {
        viewModelScope.launch {
            repository.getUserFlow(userId).collect { userEntity ->
                _user.value = userEntity
            }
        }
    }

    // end code attribution (Android Developers, 2021)

    // Update the user's minimum and maximum savings goals
    // Shows loading indicator during the update
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


    // Get formatted display name based on available information
    // Priority: Full name > First name > Last name > Username > "User Name"
    fun getDisplayName(firstName: String, lastName: String): String {
        return when {
            firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
            firstName.isNotEmpty() -> firstName
            lastName.isNotEmpty() -> lastName
            else -> _user.value?.username ?: "User Name"
        }
    }
}

// reference list
// Android Developers, 2021. StateFlow and SharedFlow. [online] Available at: <https://developer.android.com/kotlin/flow/stateflow-and-sharedflow> [Accessed 5 October 2025].