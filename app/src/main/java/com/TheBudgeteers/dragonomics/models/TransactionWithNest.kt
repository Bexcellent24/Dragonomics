package com.TheBudgeteers.dragonomics.models

data class TransactionWithNest(
   val transaction: Transaction, // the transaction itself
   val categoryNest: Nest,       // the nest for categoryId
   val fromNest: Nest?           // optional nest for fromCategoryId (null if income)
)

