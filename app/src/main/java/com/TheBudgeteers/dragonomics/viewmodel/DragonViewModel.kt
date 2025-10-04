package com.TheBudgeteers.dragonomics.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.gamify.DragonGame
import com.TheBudgeteers.dragonomics.gamify.DragonGameEvents
import com.TheBudgeteers.dragonomics.gamify.DragonGameProvider
import com.TheBudgeteers.dragonomics.gamify.DragonRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DragonUiState(
    val level: Int = 1,
    val xpIntoLevel: Int = 0,
    val xpProgress: Int = 0,
    val mood: DragonRules.Mood = DragonRules.Mood.NEUTRAL,
    val dragonImageRes: Int = 0,
    val moodIconRes: Int = 0
)

class DragonViewModel(private val dragonGame: DragonGame) : ViewModel() {

    private val _uiState = MutableStateFlow(DragonUiState())
    val uiState: StateFlow<DragonUiState> = _uiState.asStateFlow()

    init {
        // Trigger daily login on initialization
        dragonGame.onDailyLogin()

        // Observe dragon game state changes
        viewModelScope.launch {
            DragonGameEvents.stateChanged.collect { state ->
                if (state != null) {
                    updateUiState()
                }
            }
        }

        // Initial UI update
        updateUiState()
    }

    private fun updateUiState() {
        val state = dragonGame.state
        val xpPercent = (state.xpIntoLevel * 100) / DragonRules.XP_PER_LEVEL

        _uiState.value = DragonUiState(
            level = state.level,
            xpIntoLevel = state.xpIntoLevel,
            xpProgress = xpPercent,
            mood = state.mood,
            dragonImageRes = DragonRules.dragonImageFor(state.level),
            moodIconRes = DragonRules.moodIconFor(state.mood)
        )
    }

    fun onExpenseLogged(addedPhoto: Boolean) {
        dragonGame.onExpenseLogged(addedPhoto)
        updateUiState()
    }

    fun onBudgetEvaluated(
        under80Percent: Boolean,
        between80And100: Boolean,
        overBudget: Boolean,
        betweenMinAndMaxGoal: Boolean,
        aboveMaxGoal: Boolean
    ) {
        dragonGame.onBudgetEvaluated(
            under80Percent = under80Percent,
            between80And100 = between80And100,
            overBudget = overBudget,
            betweenMinAndMaxGoal = betweenMinAndMaxGoal,
            aboveMaxGoal = aboveMaxGoal
        )
        updateUiState()
    }

    fun setOverallMood(mood: DragonRules.Mood) {
        dragonGame.setOverallMood(mood)
        updateUiState()
    }

    class Factory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val dragonGame = DragonGameProvider.get(context)
            return DragonViewModel(dragonGame) as T
        }
    }
}