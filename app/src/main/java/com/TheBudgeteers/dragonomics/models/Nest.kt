package com.TheBudgeteers.dragonomics.models

import com.TheBudgeteers.dragonomics.data.NestType

// Model for a "Nest" (budget category) in the app.
data class Nest(
    val id: String = "", // Firestore document ID (empty string for new nests)
    val userId: String, // Firebase UID linking nest to a specific user
    val name: String, // Name of the nest/category
    val budget: Double?, // Monthly budget for this nest (null if income nest)
    val icon: String, // Icon reference or path for the nest
    val colour: String, // Hex colour for UI styling of this nest
    val type: NestType, // Tells if nest is income or expense
)