package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import java.time.DayOfWeek

/**
 * Type converter for [Set<DayOfWeek>] to/from [String].
 *
 * Stores days as comma-separated values (e.g., "MONDAY,TUESDAY,WEDNESDAY").
 */
class DayOfWeekListConverter {

    /**
     * Converts [Set<DayOfWeek>] to comma-separated [String].
     */
    @TypeConverter
    fun dayOfWeekSetToString(days: Set<DayOfWeek>?): String? =
        days?.map { it.name }?.joinToString(",")

    /**
     * Converts comma-separated [String] to [Set<DayOfWeek>].
     */
    @TypeConverter
    fun stringToDayOfWeekSet(daysString: String?): Set<DayOfWeek>? {
        if (daysString.isNullOrBlank()) return null

        return daysString.split(",")
            .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
            .mapNotNull { dayName ->
                try {
                    DayOfWeek.valueOf(dayName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            .toSet()
    }
}
