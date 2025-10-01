package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.TheBudgeteers.dragonomics.data.Repository

class NestViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}