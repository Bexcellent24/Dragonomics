package com.TheBudgeteers.dragonomics.models

// Simple model to hold spending totals for a nest.

data class NestSpent(
    val nestId: String, // Changed from Long to String for Firebase
    val spent: Double
)