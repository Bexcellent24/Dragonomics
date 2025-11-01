package com.TheBudgeteers.dragonomics.models

import java.util.Date

/**
 * Main model for a single transaction in the app.
 *
 * Every transaction is linked to a Nest (category) and a specific user.
 * Works with Nest to group transactions, and with TransactionDao/Firebase to be saved/retrieved.
 *
 * UPDATED FOR FIREBASE:
 * - id: String (Firestore document ID, not auto-generated Long)
 * - userId: String (Firebase UID instead of Room Long)
 * - categoryId: String (Firestore nest document ID instead of Long)
 * - fromCategoryId: String? (Firestore nest document ID instead of Long?)
 * - Removed Room annotations (@Entity, @PrimaryKey, @ForeignKey, etc.)
 */
data class Transaction(
    val id: String = "", // Firestore document ID (empty string for new transactions)
    val userId: String, // Firebase UID linking transaction to a specific user
    val title: String, // Name/description for the transaction
    val amount: Double, // How much money was spent/earned
    val date: Date, // When this transaction happened
    val photoPath: String?, // Optional path to a receipt/photo
    val description: String?, // Optional extra info about the transaction
    val categoryId: String, // Links this transaction to a Nest (category) - Firestore document ID
    val fromCategoryId: String? // For expenses, where the money came from; null for incoming transactions
)