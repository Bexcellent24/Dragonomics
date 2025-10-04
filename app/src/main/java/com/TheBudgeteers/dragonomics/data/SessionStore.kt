package com.TheBudgeteers.dragonomics.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Simple class to manage session data using DataStore.
// Stores and retrieves the currently logged-in user ID.

private val Context.dataStore by preferencesDataStore("session")

class SessionStore(private val context: Context) {

    private val KEY_USER_ID = longPreferencesKey("user_id")

    // Flow for observing the stored user ID
    val userId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    // Saves or removes the user ID in DataStore
    suspend fun setUser(id: Long?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(KEY_USER_ID)
            else prefs[KEY_USER_ID] = id
        }
    }
}
