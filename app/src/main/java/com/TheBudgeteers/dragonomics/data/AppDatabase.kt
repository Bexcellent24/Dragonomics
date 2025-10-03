package com.TheBudgeteers.dragonomics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.TheBudgeteers.dragonomics.models.Nest
import com.TheBudgeteers.dragonomics.models.Transaction
import com.TheBudgeteers.dragonomics.models.UserEntity

// If you have a Converters class (e.g., for dates/uris), keep this.
// Otherwise remove @TypeConverters and the import.
@Database(
    entities = [Transaction::class, Nest::class, UserEntity::class],
    version = 5,                 // <— keep this in sync with your latest schema
    exportSchema = false
)
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
                    // Dev-friendly; wipes DB on schema mismatch.
                    // Replace with proper Migrations when ready.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
