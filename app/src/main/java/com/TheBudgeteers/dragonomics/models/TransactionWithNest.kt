package com.TheBudgeteers.dragonomics.models


// Combines a transaction with its related nest(s).
// Used for displaying transactions with their category and optional source category.
data class TransactionWithNest(
   val transaction: Transaction, // the transaction itself
   val categoryNest: Nest,       // the nest for categoryId
   val fromNest: Nest?           // optional nest for fromCategoryId (null if income)
)

