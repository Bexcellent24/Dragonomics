package com.TheBudgeteers.dragonomics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.utils.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
AuthViewModel

Purpose:
  - Owns authentication state for the UI.
  - Coordinates sign-up and login calls to the Repository layer.

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
    data class Success(val userId: Long) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RepositoryProvider.getRepository(app)

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    /*
     Register a new user.
     Threading:
      - Set Loading on the main thread
      - Do the DB call on Dispatchers.IO
      - Update StateFlow with success/error
     */

    fun signUp(username: String, email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val res = repo.signUpUser(username, email, password)
            _state.value = res.fold(
                onSuccess = { id -> AuthState.Success(id) },
                onFailure = { AuthState.Error(it.message ?: "Sign up failed") }
            )
        }
    }

    // Repository returns user result and fold(return) into success/error for the UI.
    fun logIn(u: String, p: String) = viewModelScope.launch {
        _state.value = AuthState.Loading
        _state.value = repo.loginUser(u, p)
            .fold({ AuthState.Success(it.id) }, { AuthState.Error("Invalid username or password") })
    }
}
