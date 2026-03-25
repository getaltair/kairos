package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Simple type converters for Room (without Moshi).
 * Used to isolate the KSP processing issue.
 */
object SimpleConverters {

    // ==================== Instant Converter ====================

    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun longToInstant(timestamp: Long?): Instant? = timestamp?.let { Instant.ofEpochMilli(it) }

    // ==================== LocalDate Converter ====================

    private val dateFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.format(dateFormatter)

    @TypeConverter
    fun stringToLocalDate(dateString: String?): LocalDate? = dateString?.let {
        LocalDate.parse(it, dateFormatter)
    }

    // ==================== LocalTime Converter ====================

    private val timeFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_TIME

    @TypeConverter
    fun localTimeToString(time: LocalTime?): String? = time?.format(timeFormatter)

    @TypeConverter
    fun stringToLocalTime(timeString: String?): LocalTime? = timeString?.let {
        LocalTime.parse(it, timeFormatter)
    }
}
