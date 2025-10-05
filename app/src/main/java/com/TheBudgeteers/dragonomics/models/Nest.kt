package com.TheBudgeteers.dragonomics.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


// This is the model for a "Nest", basically a category in the app.
// Each nest has a name, budget (for expenses), an icon, a colour and a type (income/expense).
// It also linked to a specific user for multi-user support.
// Works with Transaction.kt so transactions can be grouped under a nest.
// Mood is calculated based on spending progress and affects the UI dragon's mood.



// begin code attribution
// Entity structure and Room annotations adapted from:
// Android Developers official guide to defining entities in Room

@Entity(
    tableName = "nests",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // Delete nests if user is deleted
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["type"])
    ]
)
data class Nest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // unique id for the nest

    val userId: Long, // links nest to a specific user

    val name: String, // the name of the nest
    val budget: Double?, // monthly budget for this nest (null if income nest)
    val icon: String, // icon reference or path for the nest
    val colour: String, // hex colour for UI styling of this nest
    val type: NestType, // tells if nest is income or expense
)

// end code attribution (Android Developers, 2020)

enum class NestType {
    INCOME,
    EXPENSE
}

// possible moods for a nest based on budget progress
enum class Mood {
    POSITIVE, // on track
    NEUTRAL,  // borderline
    NEGATIVE  // overspent
}

// reference list
// Android Developers, 2020. Define data using Room entities. [online] Available at: <https://developer.android.com/training/data-storage/room/defining-data> [Accessed 17 September 2025].