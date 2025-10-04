package com.TheBudgeteers.dragonomics.models


// Simple model to hold spending totals for a nest.
// Used in reports and aggregated queries.
data class NestSpent(
    val nestId: Long,
    val spent: Double
)