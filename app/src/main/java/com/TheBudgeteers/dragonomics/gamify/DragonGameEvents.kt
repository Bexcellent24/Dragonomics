package com.TheBudgeteers.dragonomics.gamify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DragonGameEvents {
    private val _stateChanged = MutableStateFlow<DragonState?>(null)
    val stateChanged: StateFlow<DragonState?> = _stateChanged

    fun notifyChanged(state: DragonState) {
        _stateChanged.value = state
    }
}
