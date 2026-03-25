package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Type converter for [LocalTime] to [String] (ISO format).
 *
 * Uses ISO-8601 format (HH:mm) for storage and retrieval.
 */
class LocalTimeConverter {

    private val formatter = DateTimeFormatter.ISO_LOCAL_TIME

    /**
     * Converts [LocalTime] to [String] in ISO format (HH:mm).
     */
    @TypeConverter
    fun localTimeToString(time: LocalTime?): String? = time?.format(formatter)

    /**
     * Converts [String] in ISO format (HH:mm) to [LocalTime].
     */
    @TypeConverter
    fun stringToLocalTime(timeString: String?): LocalTime? = timeString?.let {
        LocalTime.parse(it, formatter)
    }
}
