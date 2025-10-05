package com.TheBudgeteers.dragonomics.utils

import android.content.Context
import com.TheBudgeteers.dragonomics.data.AppDatabase
import com.TheBudgeteers.dragonomics.data.Repository

object RepositoryProvider {
    @Volatile
    private var INSTANCE: Repository? = null

    //Returns the singleton Repository.
    fun getRepository(context: Context): Repository {
        return INSTANCE ?: synchronized(this) {
            val db = AppDatabase.Companion.getDatabase(context)
            val instance = Repository(db)
            INSTANCE = instance
            instance
        }
    }
}