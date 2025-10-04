package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Mood
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * UI state for a single nest with all computed values
 */
data class NestUiState(
    val nest: Nest,
    val spent: Double,
    val budget: Double,
    val remaining: Double,
    val progress: Double,
    val mood: Mood
)



// ViewModel for the Nest entity.
// Handles calculating progress, moods, and providing reactive UI state for nests.
// Used by the UI layer (Adapters, Fragments, Activities) to display nest data and state.
// Also handles CRUD operations for nests and aggregate mood calculations.


class NestViewModel(private val repository: Repository) : ViewModel() {

 // ========== UI LOGIC STUFF ==========

     //Returns a Flow emitting UI state for a single nest.
     //Automatically updates when spent amounts change.
    fun getNestUiStateFlow(nestId: Long): Flow<NestUiState> = flow {
        val nest = repository.getNestById(nestId)

        if (nest.type == NestType.INCOME) {
            // Income nests: budget = total income, spent = amount spent FROM this income.
            val totalIncome = repository.getTransactionsByNestId(nestId)
                .sumOf { it.amount }
                .coerceAtLeast(0.0)

            // Observe spent FROM this income source (fromCategoryId)
            repository.getSpentAmountFromNestFlow(nestId).collect { spent ->
                val displayedSpent = spent ?: 0.0
                val remaining = totalIncome - displayedSpent
                val progress = calculateNestProgress(nest, displayedSpent)
                val mood = calculateMood(progress)

                emit(NestUiState(
                    nest = nest,
                    spent = displayedSpent,
                    budget = totalIncome,
                    remaining = remaining,
                    progress = progress,
                    mood = mood
                ))
            }
        } else {
            // For expense type nests use set budget
            val budget = nest.budget ?: 0.0

            // Observe spent IN this expense category (categoryId)
            repository.getSpentInCategoryFlow(nestId).collect { spent ->
                val displayedSpent = spent
                val remaining = budget - displayedSpent
                val progress = calculateNestProgress(nest, displayedSpent)
                val mood = calculateMood(progress)

                emit(NestUiState(
                    nest = nest,
                    spent = displayedSpent,
                    budget = budget,
                    remaining = remaining,
                    progress = progress,
                    mood = mood
                ))
            }
        }
    }


     // Returns UI state for a nest as a single snapshot.

    suspend fun getNestUiState(nestId: Long): NestUiState {
        val nest = repository.getNestById(nestId)
        val spent = repository.getTransactionsByNestId(nestId)
            .sumOf { it.amount }
            .coerceAtLeast(0.0)

        val budget = if (nest.type == NestType.INCOME) {
            spent // For income, budget = total income
        } else {
            nest.budget ?: 0.0
        }

        return buildUiState(nest, spent, budget)
    }

    private fun buildUiState(nest: Nest, spent: Double, budget: Double): NestUiState {
        val remaining = budget - spent
        val progress = calculateNestProgress(nest, spent)
        val mood = calculateMood(progress)

        return NestUiState(
            nest = nest,
            spent = spent,
            budget = budget,
            remaining = remaining,
            progress = progress,
            mood = mood
        )
    }

    // ========== Progress and Mood Calculations ==========


     // Calculate progress (0.0 to 1.0) for a nest
     // - EXPENSE: (budget - spent) / budget, clamped to 0..1
     // - INCOME: always 1.0 (doesn't affect overall mood)

    fun calculateNestProgress(nest: Nest, totalSpent: Double): Double {
        return if (nest.type == NestType.EXPENSE) {
            val b = nest.budget
            if (b != null && b > 0.0) {
                val remaining = (b - totalSpent.coerceAtLeast(0.0))
                (remaining / b).coerceIn(0.0, 1.0)
            } else {
                0.0 // No budget = angry
            }
        } else {
            1.0 // Income always happy
        }
    }


     // Converts progress to a mood state.
    fun calculateMood(progress: Double): Mood = when {
        progress >= 0.75 -> Mood.POSITIVE
        progress >= 0.4  -> Mood.NEUTRAL
        else             -> Mood.NEGATIVE
    }

    // ========== NEST CRUD ==========

    suspend fun getNestById(nestId: Long): Nest = repository.getNestById(nestId)

    fun addNest(nest: Nest, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.addNest(nest)
            onDone?.invoke()
        }
    }

    suspend fun getNestsByType(type: NestType): List<Nest> =
        repository.getNests().filter { it.type == type }


    // ========== FLOWS ==========

    fun getSpentAmountFlow(nestId: Long): Flow<Double?> =
        repository.getSpentAmountFromNestFlow(nestId)

    fun getNestsByTypeLive(type: NestType): Flow<List<Nest>> =
        repository.getNestsFlowByType(type)

    fun getSpentAmountsInRange(start: Long, end: Long): Flow<Map<Long, Double>> {
        return repository.getSpentAmountsInRange(start, end)
            .map { list -> list.associate { it.nestId to it.spent } }
    }

    // ========== OVERALL MOOD ==========

    enum class Weighting { EQUAL, BUDGET, SPENT }

    // Computes weighted average progress across all nests of a type.
    suspend fun computeOverallProgress(
        type: NestType = NestType.EXPENSE,
        weighting: Weighting = Weighting.BUDGET
    ): Double {
        val nests = repository.getNests().filter { it.type == type }
        if (nests.isEmpty()) return 0.5

        data class Row(val nest: Nest, val spent: Double, val progress: Double)
        val rows = nests.map { n ->
            val spent = repository.getTransactionsByNestId(n.id)
                .sumOf { it.amount }
                .coerceAtLeast(0.0)
            val prog = calculateNestProgress(n, spent)
            Row(n, spent, prog)
        }

        val weights: List<Double> = when (weighting) {
            Weighting.EQUAL  -> List(rows.size) { 1.0 }
            Weighting.BUDGET -> rows.map { it.nest.budget?.takeIf { b -> b > 0.0 } ?: 0.0 }
            Weighting.SPENT  -> rows.map { it.spent }
        }

        val totalWeight = weights.sum()
        return if (totalWeight <= 0.0) {
            rows.map { it.progress }.average()
        } else {
            rows.indices.sumOf { i -> rows[i].progress * (weights[i] / totalWeight) }
        }
    }

    // Returns overall mood and average progress for a type of nest.
    suspend fun getOverallMood(
        type: NestType = NestType.EXPENSE,
        weighting: Weighting = Weighting.BUDGET
    ): Pair<Mood, Double> {
        val avg = computeOverallProgress(type, weighting)
        return calculateMood(avg) to avg
    }

}