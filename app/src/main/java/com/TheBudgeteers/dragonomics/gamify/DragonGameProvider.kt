package com.TheBudgeteers.dragonomics.gamify

import android.content.Context

/*
Purpose:
- Provides a single, app-wide instance of DragonGame.
 */

object DragonGameProvider {
    @Volatile private var instance: DragonGame? = null

    fun get(context: Context): DragonGame =
        instance ?: synchronized(this) {
            instance ?: DragonGame(DragonStore(context.applicationContext)).also { instance = it }
        }
}
