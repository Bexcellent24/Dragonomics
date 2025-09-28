package com.TheBudgeteers.dragonomics.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.Transaction


// AppDatabase.kt
// This is the main Room database class for the app.
// It ties together all the entities (Transaction, Nest) and their DAOs (data access object).
// Room uses this to generate the database and give access to the DAOs.
// The TypeConverters allow storing custom data types like Date or NestType in the database.


@Database(entities = [Transaction::class, Nest::class], version = 1) // tells Room which entities are in the DB
@TypeConverters(Converters::class) // converts custom types for Room
abstract class AppDatabase : RoomDatabase() {

    // Accessor for transactions table
    abstract fun transactionDao(): TransactionDao

    // Accessor for nests table
    abstract fun nestDao(): NestDao
}