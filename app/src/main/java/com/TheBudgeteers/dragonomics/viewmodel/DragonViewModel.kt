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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
DragonViewModel

Purpose:
  - Holds and exposes UI-facing dragon state for the Home/Shop screens
  - Bridges the domain/game layer and UI via StateFlow<DragonUiState>
  - Listens to DragonGameEvents and maps domain state

References:
 - Kotlin Flow for UI state:
     * StateFlow & asStateFlow: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
     * Flow collection basics: https://developer.android.com/kotlin/flow
 - ViewModelProvider.Factory:
     * https://developer.android.com/reference/androidx/lifecycle/ViewModelProvider.Factory

Author: Android | Date: 2025-10-05
*/

//Core progression
data class DragonUiState(
    val level: Int = 1,
    val xpIntoLevel: Int = 0,
    val xpProgress: Int = 0,

    //Current mood & visual assets resolved from DragonRules
    val mood: DragonRules.Mood = DragonRules.Mood.NEUTRAL,
    val dragonImageRes: Int = 0,
    val moodIconRes: Int = 0,

    //UI-only state and equipped cosmetics by default
    val isExpanded: Boolean = false,
    val equippedHornsId: String? = "horns_chipped",
    val equippedWingsId: String? = "wings_ragged"
)

//ViewModel that observes the domain game
class DragonViewModel(private val dragonGame: DragonGame) : ViewModel() {

    //Mutable inside ViewModel, exposed as read-only to the UI.
    private val _uiState = MutableStateFlow(DragonUiState())
    val uiState: StateFlow<DragonUiState> = _uiState.asStateFlow()

    init {
        //Trigger daily login on initialisation
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

    /*
      Pull the latest domain state and map it into bindable UI values
      Notes:
       - xpProgress uses integer math; ensure XP_PER_LEVEL > 0 in DragonRules.
       - Visuals (dragon/mood icons) are resolved centrally via DragonRules.
     */
    private fun updateUiState() {
        val state = dragonGame.state
        val xpPercent = (state.xpIntoLevel * 100) / DragonRules.XP_PER_LEVEL

        _uiState.update { currentState ->
            currentState.copy(
                level = state.level,
                xpIntoLevel = state.xpIntoLevel,
                xpProgress = xpPercent,
                mood = state.mood,
                dragonImageRes = DragonRules.dragonImageFor(state.level, state.mood),
                moodIconRes = DragonRules.moodIconFor(state.mood)
            )
        }
    }

    //Function called by HomeActivity to toggle view size
    fun toggleExpansion() {
        _uiState.update { it.copy(isExpanded = !it.isExpanded) }
    }

    //Function called by ShopViewModel to update equipped item
    fun setEquippedAccessory(accessoryType: String, itemId: String) {
        _uiState.update { state ->
            when(accessoryType) {
                "horns" -> state.copy(equippedHornsId = itemId)
                "wings" -> state.copy(equippedWingsId = itemId)
                else -> state
            }
        }
    }

    //Domain event: user logged an expense
    fun onExpenseLogged(addedPhoto: Boolean) {
        dragonGame.onExpenseLogged(addedPhoto)
        updateUiState()
    }

    //Domain event: user evaluated their budget
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

    // Force the overall mood and refresh visuals.
    fun setOverallMood(mood: DragonRules.Mood) {
        dragonGame.setOverallMood(mood)
        updateUiState()
    }


     // Factory for constructing DragonViewModel with a DragonGame dependency
    class Factory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val dragonGame = DragonGameProvider.get(context)
            return DragonViewModel(dragonGame) as T
        }
    }
}
