package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.TheBudgeteers.dragonomics.data.Repository

// RepositoryViewModelFactory.kt
// Generic factory that creates any ViewModel requiring a Repository in its constructor.

class RepositoryViewModelFactory(
    private val repository: Repository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return try {
            modelClass.getConstructor(Repository::class.java).newInstance(repository)
        } catch (e: Exception) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}", e)
        }
    }
}
