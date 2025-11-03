package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.gamify.Achievement
import com.TheBudgeteers.dragonomics.gamify.AchievementWithProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


// ViewModel for managing achievements display.
// Now uses real Firebase data instead of hardcoded values.

class AchievementsViewModel(
    private val repository: Repository = Repository()
) : ViewModel() {

    private val _achievements = MutableStateFlow<List<AchievementDisplay>>(emptyList())
    val achievements: StateFlow<List<AchievementDisplay>> = _achievements.asStateFlow()

    private val _completedAchievements = MutableStateFlow<List<AchievementDisplay>>(emptyList())
    val completedAchievements: StateFlow<List<AchievementDisplay>> = _completedAchievements.asStateFlow()

    private val _userGold = MutableStateFlow(0)
    val userGold: StateFlow<Int> = _userGold.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Load achievements for a user.
    fun loadAchievements(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Get achievements with user progress
                val achievementsWithProgress = repository.getAchievementsWithProgress(userId)

                // Convert to display format
                val displayList = achievementsWithProgress.map { awp ->
                    AchievementDisplay(
                        id = awp.achievement.id,
                        title = awp.achievement.title,
                        description = awp.achievement.description,
                        medalRes = awp.achievement.medalRes,
                        goldReward = awp.achievement.goldReward,
                        achieved = awp.achieved,
                        progress = awp.progress,
                        targetValue = awp.achievement.targetValue,
                        progressText = getProgressText(awp)
                    )
                }

                _achievements.value = displayList

                _completedAchievements.value = displayList.filter { it.achieved }

                // Load user's gold
                _userGold.value = repository.getUserGold(userId)

            } catch (e: Exception) {
                // Handle error - could emit error state
                _achievements.value = emptyList()
                _completedAchievements.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Initialize achievements in Firebase
    fun initializeAchievements() {
        viewModelScope.launch {
            repository.initializeAchievements()
        }
    }

    // Format progress text for display.
    private fun getProgressText(awp: AchievementWithProgress): String {
        return when {
            awp.achieved -> "Completed!"
            awp.achievement.targetValue > 1 -> "${awp.progress}/${awp.achievement.targetValue}"
            else -> "Not yet completed"
        }
    }
}

// Display model for achievements in the UI.
data class AchievementDisplay(
    val id: String,
    val title: String,
    val description: String,
    val medalRes: Int,
    val goldReward: Int,
    val achieved: Boolean,
    val progress: Int,
    val targetValue: Int,
    val progressText: String
)