package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ProfileViewModel manages user profile data and settings
// Handles loading user information and updating profile details
// Provides formatted display names for the UI
// Tracks loading state for a better user experience

class ProfileViewModel(
    private val repository: Repository,
    private val userId: String
) : ViewModel() {

    // Current user profile with all profile information
    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user.asStateFlow()

    // Loading indicator for updates
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUser()
    }

    // Start observing user data from the database
    // Updates automatically when user info changes
    private fun loadUser() {
        viewModelScope.launch {
            repository.getUserFlow(userId).collect { userProfile ->
                _user.value = userProfile
            }
        }
    }

    // Update the user's profile name
    fun updateProfile(firstName: String, lastName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateUserProfile(userId, firstName, lastName)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update the user's minimum and maximum savings goals
    fun updateGoals(minGoal: Double?, maxGoal: Double?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateUserGoals(userId, minGoal, maxGoal)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Update profile picture URL
    fun updateProfilePicture(profilePictureUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateProfilePicture(userId, profilePictureUrl)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Get formatted display name based on available information
    // Priority: Full name > First name > Last name > Username > "User Name"
    fun getDisplayName(): String {
        val user = _user.value ?: return "User Name"
        return when {
            user.firstName.isNotEmpty() && user.lastName.isNotEmpty() -> "${user.firstName} ${user.lastName}"
            user.firstName.isNotEmpty() -> user.firstName
            user.lastName.isNotEmpty() -> user.lastName
            else -> user.username
        }
    }
}

// reference list
// Android Developers, 2021. StateFlow and SharedFlow. [online] Available at: <https://developer.android.com/kotlin/flow/stateflow-and-sharedflow> [Accessed 5 October 2025].