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

// AppDatabase.kt
// Main Room database. Holds all tables (entities) and their DAOs.
// Added a singleton so only one DB instance exists across the whole app.

@Database(entities = [Transaction::class, Nest::class, UserEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    @Entity(
        tableName = "users",
        indices = [Index(value = ["username"], unique = true)]
    )
    data class UserEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val username: String,
        val email: String,
        val passwordHash: String,
        val salt: String
    )

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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
