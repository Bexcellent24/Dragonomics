package com.TheBudgeteers.dragonomics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Mood
import com.TheBudgeteers.dragonomics.data.NestType
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Nest
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

/**
 * ViewModel for the Nest entity.
 * UPDATED FOR FIREBASE: Uses String IDs instead of Long.
 */
class NestViewModel(private val repository: Repository) : ViewModel() {

    /**
     * Returns a Flow emitting UI state for a single nest.
     * Automatically updates when spent amounts change.
     * UPDATED: Now accepts String userId and nestId (Firebase).
     */
    fun getNestUiStateFlow(userId: String, nestId: String): Flow<NestUiState> = flow {
        val nest = repository.getNestById(userId, nestId)
            ?: return@flow // Exit if nest doesn't exist

        if (nest.type == NestType.INCOME) {
            // Income nests: budget = total income, spent = amount spent FROM this income
            val totalIncome = repository.getTransactionsByNestId(userId, nestId)
                .sumOf { it.amount }
                .coerceAtLeast(0.0)

            // Observe spent FROM this income source (fromCategoryId)
            repository.getSpentAmountFromNestFlow(userId, nestId).collect { spent ->
                val displayedSpent = spent
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
            repository.getSpentInCategoryFlow(userId, nestId).collect { spent ->
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

    /**
     * Returns UI state for a nest as a single snapshot.
     * UPDATED: Now accepts String userId and nestId.
     */
    suspend fun getNestUiState(userId: String, nestId: String): NestUiState {
        val nest = repository.getNestById(userId, nestId)
            ?: return NestUiState(
                nest = Nest(
                    id = nestId,
                    userId = userId,
                    name = "Unknown",
                    budget = null,
                    icon = "",
                    colour = "#000000",
                    type = NestType.EXPENSE
                ),
                spent = 0.0,
                budget = 0.0,
                remaining = 0.0,
                progress = 0.0,
                mood = Mood.NEGATIVE
            )

        val spent = repository.getTransactionsByNestId(userId, nestId)
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

    /**
     * Calculate progress (0.0 to 1.0) for a nest
     */
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

    /**
     * Converts progress to a mood state.
     */
    fun calculateMood(progress: Double): Mood = when {
        progress >= 0.75 -> Mood.POSITIVE
        progress >= 0.4  -> Mood.NEUTRAL
        else             -> Mood.NEGATIVE
    }

    /**
     * Get a single nest by ID.
     * UPDATED: Now accepts String userId and nestId.
     */
    suspend fun getNestById(userId: String, nestId: String): Nest? =
        repository.getNestById(userId, nestId)

    /**
     * Add a new nest.
     * UPDATED: Now accepts String userId.
     */
    fun addNest(userId: String, nest: Nest, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.addNest(userId, nest)
            onDone?.invoke()
        }
    }

    /**
     * Get nests filtered by type.
     * UPDATED: Now accepts String userId.
     */
    suspend fun getNestsByType(userId: String, type: NestType): List<Nest> =
        repository.getNests(userId).filter { it.type == type }

    /**
     * Get spent amount flow for a nest.
     * UPDATED: Now accepts String userId and nestId.
     */
    fun getSpentAmountFlow(userId: String, nestId: String): Flow<Double> =
        repository.getSpentAmountFromNestFlow(userId, nestId)

    /**
     * Get nests by type as reactive Flow.
     * UPDATED: Now accepts String userId.
     */
    fun getNestsByTypeLive(userId: String, type: NestType): Flow<List<Nest>> =
        repository.getNestsFlowByType(userId, type)

    /**
     * Get spent amounts grouped by nest within date range.
     * UPDATED: Now accepts String userId and returns Map<String, Double>.
     */
    fun getSpentAmountsInRange(userId: String, start: Long, end: Long): Flow<Map<String, Double>> {
        return repository.getSpentAmountsInRange(userId, start, end)
            .map { list -> list.associate { it.nestId to it.spent } }
    }

    enum class Weighting { EQUAL, BUDGET, SPENT }

    /**
     * Computes weighted average progress across all nests of a type.
     * UPDATED: Now accepts String userId.
     */
    suspend fun computeOverallProgress(
        userId: String,
        type: NestType = NestType.EXPENSE,
        weighting: Weighting = Weighting.BUDGET
    ): Double {
        val nests = repository.getNests(userId).filter { it.type == type }
        if (nests.isEmpty()) return 0.5

        data class Row(val nest: Nest, val spent: Double, val progress: Double)
        val rows = nests.map { n ->
            val spent = repository.getTransactionsByNestId(userId, n.id)
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

    /**
     * Returns overall mood and average progress for a type of nest.
     * UPDATED: Now accepts String userId.
     */
    suspend fun getOverallMood(
        userId: String,
        type: NestType = NestType.EXPENSE,
        weighting: Weighting = Weighting.BUDGET
    ): Pair<Mood, Double> {
        val avg = computeOverallProgress(userId, type, weighting)
        return calculateMood(avg) to avg
    }
}