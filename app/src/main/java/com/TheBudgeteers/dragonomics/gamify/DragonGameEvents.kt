package com.TheBudgeteers.dragonomics.gamify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/*
Purpose:
  - Simple in-process event bus to publish the latest DragonState to interested collectors.

 */

object DragonGameEvents {

    //Holds the latest DragonState (null until first publish)
    private val _stateChanged = MutableStateFlow<DragonState?>(null)
    val stateChanged: StateFlow<DragonState?> = _stateChanged

    //Push a new DragonState
    fun notifyChanged(state: DragonState) {
        _stateChanged.value = state
    }
}
