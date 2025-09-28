package com.TheBudgeteers.dragonomics.data

import androidx.room.TypeConverter
import com.TheBudgeteers.dragonomics.models.NestType
import java.util.Date


// Converters.kt
// Room can only store simple data types, so we need converters for custom types.
// This class tells Room how to store and retrieve Dates and NestType enums.
// Works with AppDatabase via @TypeConverters so Room knows how to handle these types.

class Converters {

    // Converts a Long timestamp to a Date object when reading from the database
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    // Converts a Date object to a Long timestamp when writing to the database
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // Converts a String stored in the database back into a NestType enum
    @TypeConverter
    fun fromNestType(value: String?): NestType? = value?.let { NestType.valueOf(it) }

    // Converts a NestType enum into a String for storage in the database
    @TypeConverter
    fun nestTypeToString(nestType: NestType?): String? = nestType?.name
}