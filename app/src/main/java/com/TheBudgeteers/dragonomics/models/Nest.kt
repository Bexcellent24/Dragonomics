package com.TheBudgeteers.dragonomics.models

import com.TheBudgeteers.dragonomics.data.NestType

/**
 * Model for a "Nest" (budget category) in the app.
 *
 * Each nest has a name, budget (for expenses), an icon, a colour and a type (income/expense).
 * It's linked to a specific user for multi-user support.
 * Works with Transaction so transactions can be grouped under a nest.
 * Mood is calculated based on spending progress and affects the UI dragon's mood.
 *
 * UPDATED FOR FIREBASE:
 * - id: String (Firestore document ID, not auto-generated Long)
 * - userId: String (Firebase UID instead of Room Long)
 * - Removed Room annotations (@Entity, @PrimaryKey, @ForeignKey, etc.)
 */
data class Nest(
    val id: String = "", // Firestore document ID (empty string for new nests)
    val userId: String, // Firebase UID linking nest to a specific user
    val name: String, // Name of the nest/category
    val budget: Double?, // Monthly budget for this nest (null if income nest)
    val icon: String, // Icon reference or path for the nest
    val colour: String, // Hex colour for UI styling of this nest
    val type: NestType, // Tells if nest is income or expense
)