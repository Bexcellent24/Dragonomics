package com.TheBudgeteers.dragonomics.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.FirebaseCosmeticsRepository
import com.TheBudgeteers.dragonomics.gamify.DragonGame
import com.TheBudgeteers.dragonomics.gamify.DragonGameEvents
import com.TheBudgeteers.dragonomics.gamify.DragonGameProvider
import com.TheBudgeteers.dragonomics.gamify.DragonRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * DragonViewModel - Holds UI-facing dragon state for Home/Shop screens.
 * UPDATED FOR FIREBASE: Now loads equipped cosmetics from Firestore.
 */

// Core progression
data class DragonUiState(
    val level: Int = 1,
    val xpIntoLevel: Int = 0,
    val xpProgress: Int = 0,

    // Current mood & visual assets resolved from DragonRules
    val mood: DragonRules.Mood = DragonRules.Mood.NEUTRAL,
    val dragonImageRes: Int = 0,
    val moodIconRes: Int = 0,

    // UI-only state and equipped cosmetics loaded from Firebase
    val isExpanded: Boolean = false,
    val equippedHornsId: String? = "horns_chipped",
    val equippedWingsId: String? = "wings_ragged",
    val equippedPaletteId: String = "pal_ember"
)

// ViewModel that observes the domain game
class DragonViewModel(private val dragonGame: DragonGame) : ViewModel() {

    private val cosmeticsRepo = FirebaseCosmeticsRepository()
    private var currentUserId: String? = null

    // Mutable inside ViewModel, exposed as read-only to the UI.
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

    /**
     * Initialize with user ID to load their equipped cosmetics.
     * Call this from HomeActivity after getting userId from SessionStore.
     */
    fun initialize(userId: String) {
        currentUserId = userId
        loadEquippedCosmetics(userId)
    }

    /**
     * Load equipped cosmetics from Firebase
     */
    private fun loadEquippedCosmetics(userId: String) {
        viewModelScope.launch {
            cosmeticsRepo.getCosmeticsDataFlow(userId).collect { data ->
                if (data == null) return@collect

                _uiState.update { currentState ->
                    currentState.copy(
                        equippedHornsId = data.equippedHorns,
                        equippedWingsId = data.equippedWings,
                        equippedPaletteId = data.equippedPalette
                    )
                }
            }
        }
    }

    /**
     * Pull the latest domain state and map it into bindable UI values
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

    /**
     * Toggle expansion (called by HomeActivity)
     */
    fun toggleExpansion() {
        _uiState.update { it.copy(isExpanded = !it.isExpanded) }
    }

    /**
     * Update equipped accessory (called when user equips something from shop)
     * This updates the UI state immediately without waiting for Firestore
     */
    fun setEquippedAccessory(accessoryType: String, itemId: String) {
        _uiState.update { state ->
            when (accessoryType) {
                "horns" -> state.copy(equippedHornsId = itemId)
                "wings" -> state.copy(equippedWingsId = itemId)
                "palette" -> state.copy(equippedPaletteId = itemId)
                else -> state
            }
        }
    }

    // Domain event: user logged an expense
    fun onExpenseLogged(addedPhoto: Boolean) {
        dragonGame.onExpenseLogged(addedPhoto)
        updateUiState()
    }

    // Domain event: user evaluated their budget
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

    // Force the overall mood and refresh visuals
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