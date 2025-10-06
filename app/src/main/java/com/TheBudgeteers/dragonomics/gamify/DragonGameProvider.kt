package com.TheBudgeteers.dragonomics.gamify

import android.content.Context

/*
Purpose:
- Provides a single, app-wide instance of DragonGame.
 */

object DragonGameProvider {

    // begin code attribution
    // Provide a single instance using double-checked locking with @Volatile and synchronized.
    // Adapted from:
    // Kotlin, 2024. kotlin.jvm.Volatile & synchronized. [online]
    // Available at: <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-volatile/>
    // [Accessed 6 October 2025].
    @Volatile private var instance: DragonGame? = null

    fun get(context: Context): DragonGame =
        instance ?: synchronized(this) {
            instance ?: DragonGame(DragonStore(context.applicationContext)).also { instance = it }
        }
    // end code attribution (Kotlin, 2024)
}
// reference list
// Kotlin, 2024. kotlin.jvm.Volatile. [online]
// Available at: <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-volatile/> [Accessed 6 October 2025].
