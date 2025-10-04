package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.Achievement
import com.TheBudgeteers.dragonomics.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AchievementsViewModel : ViewModel() {

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            // TODO: Load from database/repository instead of hard-coding
            // For now, keeping the hard-coded ones but making them configurable
            val list = listOf(
                Achievement(
                    id = "master",
                    title = "Dragon Master",
                    description = "Unlock all customizations for your dragon.",
                    medalRes = R.drawable.gold_badge,
                    achieved = false
                ),
                Achievement(
                    id = "hoard",
                    title = "Dragon's Hoard",
                    description = "Have 30,000 or more in a savings nest.",
                    medalRes = R.drawable.silver_badge,
                    achieved = false
                ),
                Achievement(
                    id = "streak",
                    title = "Flames of authority",
                    description = "Log for 30 days in a row.",
                    medalRes = R.drawable.bronze_badge,
                    achieved = true
                )
            )
            _achievements.value = list
        }
    }

    fun checkAchievements(
        totalSavings: Double = 0.0,
        loginStreakDays: Int = 0,
        allCustomizationsUnlocked: Boolean = false
    ) {
        viewModelScope.launch {
            val updated = _achievements.value.map { achievement ->
                when (achievement.id) {
                    "hoard" -> achievement.copy(achieved = totalSavings >= 30000.0)
                    "streak" -> achievement.copy(achieved = loginStreakDays >= 30)
                    "master" -> achievement.copy(achieved = allCustomizationsUnlocked)
                    else -> achievement
                }
            }
            _achievements.value = updated
        }
    }

    fun unlockAchievement(achievementId: String) {
        viewModelScope.launch {
            val updated = _achievements.value.map {
                if (it.id == achievementId) it.copy(achieved = true) else it
            }
            _achievements.value = updated
            // TODO: Persist to database
        }
    }
}