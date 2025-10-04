package com.TheBudgeteers.dragonomics.data

import androidx.room.TypeConverter
import com.TheBudgeteers.dragonomics.models.NestType
import java.util.Date


// Handles data type conversions for Room.
// Room only supports basic types, so this converts Dates and NestType enums.
// Registered in AppDatabase using @TypeConverters.

class Converters {

    // Converts Long (timestamp) to Date when reading from DB
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    // Converts Date to Long (timestamp) when writing to DB
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // Converts stored String to NestType enum
    @TypeConverter
    fun fromNestType(value: String?): NestType? = value?.let { NestType.valueOf(it) }

    // Converts NestType enum to String for storage
    @TypeConverter
    fun nestTypeToString(nestType: NestType?): String? = nestType?.name
}