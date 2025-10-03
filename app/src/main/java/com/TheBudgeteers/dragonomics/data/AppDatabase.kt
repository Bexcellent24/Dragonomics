package com.TheBudgeteers.dragonomics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.Transaction

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.TheBudgeteers.dragonomics.models.UserEntity

// AppDatabase.kt
// Main Room database. Holds all tables (entities) and their DAOs.
// Added a singleton so only one DB instance exists across the whole app.

@Database(entities = [Transaction::class, Nest::class, UserEntity::class], version = 5)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun nestDao(): NestDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dragonomics_db"
                )
                    .fallbackToDestructiveMigration() // wipes DB if schema mismatches
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

