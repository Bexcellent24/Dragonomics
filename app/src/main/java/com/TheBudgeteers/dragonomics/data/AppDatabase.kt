package com.TheBudgeteers.dragonomics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.models.QuestEntity
import com.TheBudgeteers.dragonomics.models.UserEntity

// Main Room database for the app.
// Holds all entities and links them to their DAOs.
// Implemented as a singleton to prevent multiple instances.
// All data operations filtered by userId

@Database(
    entities = [Transaction::class, Nest::class, UserEntity::class, QuestEntity::class],
    version = 10
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // DAO accessors for each data type
    abstract fun transactionDao(): TransactionDao
    abstract fun nestDao(): NestDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // begin code attribution
        // The singleton Room database builder pattern is adapted from:
        // Android Developers official documentation on Room databases

        // Returns the single DB instance
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dragonomics_db"
                )
                    // Wipes and rebuilds DB on version mismatch (temporary during dev)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // end code attribution (Android Developers, 2020)
    }
}

// reference list
// Android Developers, 2020. Room Database with a View - Kotlin. [online] Available at: <https://developer.android.com/codelabs/android-room-with-a-view-kotlin> [Accessed 15 September 2025].
// Android Studio, 2020. Room persistence library. [online] Available at: <https://developer.android.com/training/data-storage/room> [Accessed 15 September 2025].