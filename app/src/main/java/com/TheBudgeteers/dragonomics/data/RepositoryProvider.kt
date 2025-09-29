package com.TheBudgeteers.dragonomics.data

import android.content.Context

// RepositoryProvider.kt
// Provides a single Repository instance for the app.
// Uses the AppDatabase singleton under the hood.

object RepositoryProvider {
    @Volatile
    private var INSTANCE: Repository? = null

    fun getRepository(context: Context): Repository {
        return INSTANCE ?: synchronized(this) {
            val db = AppDatabase.getDatabase(context)
            val instance = Repository(db)
            INSTANCE = instance
            instance
        }
    }
}
