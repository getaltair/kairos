package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.HabitFrequency

/**
 * Type converter for [HabitFrequency] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "Daily") for storage.
 */
class HabitFrequencyConverter {

    /**
     * Converts [HabitFrequency] to its simple class name [String].
     */
    @TypeConverter
    fun habitFrequencyToString(frequency: HabitFrequency?): String? = frequency?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [HabitFrequency].
     */
    @TypeConverter
    fun stringToHabitFrequency(name: String?): HabitFrequency? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Daily" -> HabitFrequency.Daily
            "Weekdays" -> HabitFrequency.Weekdays
            "Weekends" -> HabitFrequency.Weekends
            "Custom" -> HabitFrequency.Custom
            else -> null
        }
    }
}
