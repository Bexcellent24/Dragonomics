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

    // begin code attribution
    // Reading values from Preferences DataStore via Flow adapted from:
    // “Working with Preferences DataStore”

    // Flow for observing the stored Firebase user UID
    val userId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    // end code attribution (Android Developers, 2022)

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

// Android Developers. 2022. Working with Preferences DataStore. [online] Available at: <https://developer.android.com/codelabs/android-preferences-datastore> [Accessed 4 November 2025]

