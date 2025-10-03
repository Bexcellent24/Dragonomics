package com.TheBudgeteers.dragonomics.viewmodel

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.TheBudgeteers.dragonomics.data.Repository
import com.TheBudgeteers.dragonomics.models.Mood
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.NestType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NestViewModel(private val repository: Repository) : ViewModel() {

    // ---- Existing API ----
    fun getNestById(nestId: Long, callback: (Nest) -> Unit) {
        viewModelScope.launch {
            val nest = repository.getNestById(nestId)
            callback(nest)
        }
    }

    /**
     * Progress is 0..1
     * - EXPENSE with valid budget: (budget - spent) / budget, clamped
     * - EXPENSE with null/zero budget: 0.0  (treat as Angry / no room)
     * - INCOME: 1.0  (doesn't affect expense-based overall)
     */
    fun calculateNestProgress(nest: Nest, totalSpent: Double): Double {
        return if (nest.type == NestType.EXPENSE) {
            val b = nest.budget
            if (b != null && b > 0.0) {
                val left = (b - totalSpent.coerceAtLeast(0.0))
                (left / b).coerceIn(0.0, 1.0)
            } else {
                0.0
            }
        } else {
            1.0
        }
    }

    fun calculateMood(progress: Double): Mood = when {
        progress >= 0.75 -> Mood.POSITIVE
        progress >= 0.4  -> Mood.NEUTRAL
        else             -> Mood.NEGATIVE
    }

    fun getNestProgressAndMoodWithSpent(
        nestId: Long,
        callback: (Double, Mood, Double) -> Unit
    ) {
        viewModelScope.launch {
            val nest = repository.getNestById(nestId)
            val transactions = repository.getTransactionsByNestId(nestId)
            val spent = transactions.sumOf { it.amount }.coerceAtLeast(0.0)
            val progress = calculateNestProgress(nest, spent)
            val mood = calculateMood(progress)
            callback(progress, mood, spent)
        }
    }

    fun addNest(nest: Nest, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.addNest(nest)
            onDone?.invoke()
        }
    }

    // ---- Queries / flows ----

    suspend fun getNestsByType(type: NestType): List<Nest> =
        repository.getNests().filter { it.type == type }
/*
    fun getIncomeNestBudget(nestId: Long): Double {
        var budget = 0.0
        runBlocking {
            budget = repository.getTotalIncomeForNest(nestId)
        }
        return budget
    }*/

    fun getSpentAmountFlow(nestId: Long) =
        repository.getSpentAmountFromNestFlow(nestId)

    fun getNestsByTypeLive(type: NestType) =
        repository.getNestsFlowByType(type)

    /** Optional helper (keep only if your Repository exposes getSpentAmountsInRange). */
    fun getSpentAmountsInRange(start: Long, end: Long): Flow<Map<Long, Double>> {
        return repository.getSpentAmountsInRange(start, end)
            .map { list -> list.associate { it.nestId to it.spent } }
    }

    // ---- Overall mood for Home ----

    enum class Weighting { EQUAL, BUDGET, SPENT }

    /**
     * Average progress across nests, with optional weighting.
     * If there are NO expense nests, return 0.5 (neutral).
     */
    suspend fun computeOverallProgress(
        type: NestType = NestType.EXPENSE,
        weighting: Weighting = Weighting.BUDGET
    ): Double {
        val nests = repository.getNests().filter { it.type == type }
        if (nests.isEmpty()) return 0.5 // neutral default, not forced-happy

        data class Row(val nest: Nest, val spent: Double, val progress: Double)
        val rows = nests.map { n ->
            val spent = repository.getTransactionsByNestId(n.id).sumOf { it.amount }.coerceAtLeast(0.0)
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

    fun getOverallMood(
        type: NestType = NestType.EXPENSE,
        weighting: Weighting = Weighting.BUDGET,
        callback: (mood: Mood, averageProgress: Double) -> Unit
    ) {
        viewModelScope.launch {
            val avgProgress = computeOverallProgress(type, weighting)
            val overallMood = calculateMood(avgProgress)
            callback(overallMood, avgProgress)
        }
    }

    suspend fun getOverallMoodSuspend(
        type: NestType = NestType.EXPENSE,
        weighting: Weighting = Weighting.BUDGET
    ): Pair<Mood, Double> {
        val avg = computeOverallProgress(type, weighting)
        return calculateMood(avg) to avg
    }

    // Map mood -> your actual drawable names used in XML
    @DrawableRes
    fun Mood.asDrawableRes(): Int = when (this) {
        Mood.POSITIVE -> com.TheBudgeteers.dragonomics.R.drawable.happy_mood
        Mood.NEUTRAL  -> com.TheBudgeteers.dragonomics.R.drawable.neutral_mood
        Mood.NEGATIVE -> com.TheBudgeteers.dragonomics.R.drawable.angry_mood
    }.let { resolved ->
        if (resolved != 0) resolved else com.TheBudgeteers.dragonomics.R.drawable.happy_mood
    }
}
