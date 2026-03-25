package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.HabitStatus

/**
 * Type converter for [HabitStatus] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "Active") for storage.
 */
class HabitStatusConverter {

    /**
     * Converts [HabitStatus] to its simple class name [String].
     */
    @TypeConverter
    fun habitStatusToString(status: HabitStatus?): String? = status?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [HabitStatus].
     */
    @TypeConverter
    fun stringToHabitStatus(name: String?): HabitStatus? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Active" -> HabitStatus.Active
            "Paused" -> HabitStatus.Paused
            "Archived" -> HabitStatus.Archived
            else -> null
        }
    }
}
