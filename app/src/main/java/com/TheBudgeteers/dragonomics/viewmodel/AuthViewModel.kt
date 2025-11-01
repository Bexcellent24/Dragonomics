package com.TheBudgeteers.dragonomics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.FirebaseAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
AuthViewModel

Purpose:
  - Owns authentication state for the UI.
  - Coordinates sign-up and login calls to Firebase Authentication.
  - Updated to use Firebase UID (String) instead of Room userId (Long)

References:
 - Kotlin stdlib: Result and fold.
     * Result API: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/
 - Coroutines Dispatchers and threading:
     * Dispatchers.IO: https://kotlinlang.org/docs/coroutines-basics.html#dispatchers-and-threads

Author: Kotlin | Date: 2025-10-05
*/

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val userId: String) : AuthState() // Changed from Long to String for Firebase UID
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val authRepo = FirebaseAuthRepository()

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    /*
     Register a new user with Firebase Authentication.
     Threading:
      - Set Loading on the main thread
      - Do the Firebase call on Dispatchers.IO
      - Update StateFlow with success/error
     */
    fun signUp(username: String, email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val res = authRepo.signUpUser(username, email, password)
            _state.value = res.fold(
                onSuccess = { uid -> AuthState.Success(uid) },
                onFailure = { AuthState.Error(it.message ?: "Sign up failed") }
            )
        }
    }

    /*
     Log in an existing user with Firebase Authentication.
     Firebase authenticates via email+password, but we look up email from username.
     */
    fun logIn(username: String, password: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.value = AuthState.Loading
        _state.value = authRepo.loginUser(username, password)
            .fold(
                { AuthState.Success(it) },
                { AuthState.Error("Invalid username or password") }
            )
    }

    /*
     Get the currently logged-in Firebase user ID
     */
    fun getCurrentUserId(): String? = authRepo.getCurrentUserId()

    /*
     Sign out the current user
     */
    fun signOut() = authRepo.signOut()
}