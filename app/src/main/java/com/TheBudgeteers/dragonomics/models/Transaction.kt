package com.TheBudgeteers.dragonomics.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date


// This is the main model for a single transaction in the app.
// Every transaction is stored in the database and linked to a Nest (category).
// Works with Nest.kt to group transactions, and with TransactionDao to be saved/retrieved.

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // unique id, generated automatically

    val title: String, // name for the transaction
    val amount: Double, // how much money was spent/earned
    val date: Date, // when this transaction happened
    val photoPath: String?, // optional path to a receipt/photo
    val description: String?, // optional extra info about the transaction
    val categoryId: Long, // links this transaction to a Nest (category)
    val fromCategoryId: Long? // for expenses, where the money came from; null for incoming transactions
)
