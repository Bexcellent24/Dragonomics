package com.TheBudgeteers.dragonomics.models

import androidx.room.Entity
import androidx.room.PrimaryKey


// Nest.kt
// This is the model for a "Nest" — basically a category in the app.
// Each nest has a name, budget (for expenses), an icon, a colour and a type (income/expense).
// Works with Transaction.kt so transactions can be grouped under a nest.
// Mood is calculated based on spending progress and affects the UI dragon’s mood.

@Entity(tableName = "nests") // tells Room this is a table in the database
data class Nest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // unique id for the nest
    val name: String, // the name of the nest
    val budget: Double?, // monthly budget for this nest (null if income nest)
    val icon: String, // icon reference or path for the nest
    val colour: String, // hex colour for UI styling of this nest
    val type: NestType, // tells if nest is income or expense
)

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
