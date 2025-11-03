package com.TheBudgeteers.dragonomics.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


// Simple class to manage session data using DataStore.
// Stores and retrieves the currently logged-in Firebase user UID.

private val Context.dataStore by preferencesDataStore("session")

class SessionStore(private val context: Context) {

    // Changed from longPreferencesKey to stringPreferencesKey for Firebase UID
    private val KEY_USER_ID = stringPreferencesKey("user_id")

    // Flow for observing the stored Firebase user UID
    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    // Saves or removes the Firebase user UID in DataStore
    suspend fun setUser(id: String?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(KEY_USER_ID)
            else prefs[KEY_USER_ID] = id
        }
    }

    // Get the current user ID synchronously
     fun getCurrentUserId(): String? {
        var userId: String? = null
        context.dataStore.data.map { prefs ->
            userId = prefs[KEY_USER_ID]
        }
        return userId
    }
}