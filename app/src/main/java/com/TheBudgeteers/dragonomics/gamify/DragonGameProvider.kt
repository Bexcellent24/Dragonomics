package com.TheBudgeteers.dragonomics.gamify

import android.content.Context

object DragonGameProvider {

    // Map of userId to DragonGame instance
    private val instances = mutableMapOf<String, DragonGame>()

    // begin code attribution
    // Provide a single instance using double-checked locking with @Volatile and synchronized.
    // Adapted from:
    // Kotlin, 2024. kotlin.jvm.Volatile & synchronized. [online]
    // Available at: <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-volatile/>
    // [Accessed 6 October 2025].

     // Get or create a DragonGame instance for a specific user.
    @Synchronized
    fun get(context: Context, userId: String): DragonGame {
        return instances.getOrPut(userId) {
            DragonGame(DragonStore(context.applicationContext, userId))
        }
    }

    // Clear the dragon game instance for a user (call on logout).

    @Synchronized
    fun clear(userId: String) {
        instances.remove(userId)
    }


     // Clear all instances (call on app logout/reset).
    @Synchronized
    fun clearAll() {
        instances.clear()
    }

    // end code attribution (Kotlin, 2024)
}

// reference list
// Kotlin, 2024. kotlin.jvm.Volatile. [online]
// Available at: <https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-volatile/> [Accessed 6 October 2025].