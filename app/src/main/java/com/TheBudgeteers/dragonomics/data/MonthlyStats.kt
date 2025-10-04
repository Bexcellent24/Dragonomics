package com.TheBudgeteers.dragonomics.data


// Holds basic monthly financial data for reports or dashboards.
// Simple container used for displaying totals.
data class MonthlyStats(
    val income: Double,
    val expenses: Double,
    val remaining: Double
)