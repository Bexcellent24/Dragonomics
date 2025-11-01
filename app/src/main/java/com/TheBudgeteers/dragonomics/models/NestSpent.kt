package com.TheBudgeteers.dragonomics.models

/**
 * Simple model to hold spending totals for a nest.
 * Used in reports and aggregated queries.
 *
 * UPDATED FOR FIREBASE:
 * - nestId: String (Firestore document ID instead of Long)
 */
data class NestSpent(
    val nestId: String, // Changed from Long to String for Firebase
    val spent: Double
)