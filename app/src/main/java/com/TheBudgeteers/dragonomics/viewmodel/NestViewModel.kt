package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Mood
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import kotlinx.coroutines.launch


// NestViewModel.kt
// This ViewModel manages nests (categories) and their progress/mood calculations for the UI.
// Talks to the Repository to get nest data and related transactions.
// Calculates how much budget is left for each nest and determines the nest’s mood.
// Keeps logic off the main thread using coroutines.


class NestViewModel(private val repository: Repository) : ViewModel() {

    // Gets a nest by ID from the repository and passes it to the callback
    fun getNestById(nestId: Long, callback: (Nest) -> Unit) {
        viewModelScope.launch {
            val nest = repository.getNestById(nestId)
            callback(nest)
        }
    }

    // Calculates how much of the budget is left for the nest
    fun calculateNestProgress(nest: Nest, totalSpent: Double): Double {
        return if (nest.type == NestType.EXPENSE && nest.budget != null) {
            ((nest.budget - totalSpent) / nest.budget).coerceIn(0.0, 1.0) // keep value between 0 and 1
        } else {
            1.0 // Incoming nests always show as full budget
        }
    }

    // Determines the mood of the nest based on progress
    fun calculateMood(progress: Double): Mood {
        return when {
            progress >= 0.75 -> Mood.POSITIVE
            progress >= 0.4 -> Mood.NEUTRAL
            else -> Mood.NEGATIVE
        }
    }

    // Gets both progress and mood for a nest by ID
    fun getNestProgressAndMood(nestId: Long, callback: (Double, Mood) -> Unit) {
        viewModelScope.launch {
            val nest = repository.getNestById(nestId) // get nest details
            val spent = repository.getTransactionsByNestId(nestId).sumOf { it.amount } // sum transactions
            val progress = calculateNestProgress(nest, spent) // calculate budget progress
            val mood = calculateMood(progress) // calculate mood
            callback(progress, mood) // return values to UI
        }
    }
}
