package com.TheBudgeteers.dragonomics.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date


// This is the main model for a single transaction in the app.
// Every transaction is stored in the database and linked to a Nest (category).
// it is also linked to a specific user for multi-user support.
// Works with Nest.kt to group transactions, and with TransactionDao to be saved/retrieved.


// begin code attribution
// Entity structure and Room annotations adapted from:
// Android Developers official guide to defining entities in Room

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // Delete transactions if user is deleted
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["categoryId"]),
        Index(value = ["date"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // unique id, generated automatically

    val userId: Long, // links transaction to a specific user

    val title: String, // name for the transaction
    val amount: Double, // how much money was spent/earned
    val date: Date, // when this transaction happened
    val photoPath: String?, // optional path to a receipt/photo
    val description: String?, // optional extra info about the transaction
    val categoryId: Long, // links this transaction to a Nest (category)
    val fromCategoryId: Long? // for expenses, where the money came from; null for incoming transactions
)
// end code attribution (Android Developers, 2020)

// reference list
// Android Developers, 2020. Define data using Room entities. [online] Available at: <https://developer.android.com/training/data-storage/room/defining-data> [Accessed 17 September 2025].