package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.HabitCategory

/**
 * Type converter for [HabitCategory] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "Morning") for storage.
 */
class HabitCategoryConverter {

    /**
     * Converts [HabitCategory] to its simple class name [String].
     */
    @TypeConverter
    fun habitCategoryToString(category: HabitCategory?): String? = category?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [HabitCategory].
     */
    @TypeConverter
    fun stringToHabitCategory(name: String?): HabitCategory? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Morning" -> HabitCategory.Morning
            "Afternoon" -> HabitCategory.Afternoon
            "Evening" -> HabitCategory.Evening
            "Anytime" -> HabitCategory.Anytime
            "Departure" -> HabitCategory.Departure
            else -> null
        }
    }
}
