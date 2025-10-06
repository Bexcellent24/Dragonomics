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

    // begin code attribution
    // Publish the latest DragonState; StateFlow holds and replays the most recent value to new collectors.
    // Adapted from:
    // Android Developers, 2021. StateFlow behavior (hot flow, latest value). [online]
    // Available at: <https://developer.android.com/kotlin/flow/stateflow-and-sharedflow> [Accessed 6 October 2025].
    //Push a new DragonState
    fun notifyChanged(state: DragonState) {
        _stateChanged.value = state
    }
    // end code attribution (Android Developers, 2021)
}
// reference list
// Android Developers, 2021. StateFlow and SharedFlow. [online]
// Available at: <https://developer.android.com/kotlin/flow/stateflow-and-sharedflow> [Accessed 6 October 2025].
