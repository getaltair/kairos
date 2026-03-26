package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Type converter for [LocalDate] to [String] (ISO format).
 *
 * Uses ISO-8601 format (YYYY-MM-DD) for storage and retrieval.
 */
class LocalDateConverter {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Converts [LocalDate] to [String] in ISO format (YYYY-MM-DD).
     */
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.format(formatter)

    /**
     * Converts [String] in ISO format (YYYY-MM-DD) to [LocalDate].
     */
    @TypeConverter
    fun stringToLocalDate(dateString: String?): LocalDate? = dateString?.let {
        LocalDate.parse(it, formatter)
    }
}
