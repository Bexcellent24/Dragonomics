package com.TheBudgeteers.dragonomics.utils

import android.content.Context
import com.TheBudgeteers.dragonomics.data.Repository

/**
 * Simple singleton provider for the Firebase Repository.
 * No database dependency needed - Repository creates its own Firebase instances.
 *
 * UPDATED FOR FIREBASE: Removed AppDatabase dependency.
 */
object RepositoryProvider {

    @Volatile
    private var INSTANCE: Repository? = null

    /**
     * Get the singleton Repository instance.
     * Context parameter kept for compatibility but not actually needed for Firebase.
     */
    fun getRepository(context: Context): Repository {
        return INSTANCE ?: synchronized(this) {
            val instance = Repository()
            INSTANCE = instance
            instance
        }
    }

    /**
     * Clear the repository instance (useful for testing or logout).
     */
    fun clearInstance() {
        INSTANCE = null
    }
}