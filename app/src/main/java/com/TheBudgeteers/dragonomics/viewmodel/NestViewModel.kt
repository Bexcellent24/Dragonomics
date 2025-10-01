package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Mood
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


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
    fun getNestProgressAndMoodWithSpent(nestId: Long, callback: (Double, Mood, Double) -> Unit) {
        viewModelScope.launch {
            val nest = repository.getNestById(nestId)
            val transactions = repository.getTransactionsByNestId(nestId)
            val spent = transactions.sumOf { it.amount }
            val progress = calculateNestProgress(nest, spent)
            val mood = calculateMood(progress)
            callback(progress, mood, spent)
        }
    }

    fun addNest(nest: Nest, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.addNest(nest) // you’ll need this method in Repository
            onDone?.invoke()
        }
    }

    suspend fun getNestsByType(type: NestType): List<Nest> {
        return repository.getNests().filter { it.type == type }
    }

    fun getIncomeNestBudget(nestId: Long): Double {
        var budget = 0.0
        runBlocking {
            budget = repository.getTotalIncomeForNest(nestId)
        }
        return budget
    }

    fun getSpentAmountFlow(nestId: Long) =
        repository.getSpentAmountFromNestFlow(nestId)

    fun getNestsByTypeLive(type: NestType) = repository.getNestsFlowByType(type)
}
