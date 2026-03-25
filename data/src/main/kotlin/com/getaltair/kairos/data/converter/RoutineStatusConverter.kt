package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.RoutineStatus

/**
 * Type converter for [RoutineStatus] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "Active") for storage.
 */
class RoutineStatusConverter {

    /**
     * Converts [RoutineStatus] to its simple class name [String].
     */
    @TypeConverter
    fun routineStatusToString(status: RoutineStatus?): String? = status?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [RoutineStatus].
     */
    @TypeConverter
    fun stringToRoutineStatus(name: String?): RoutineStatus? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Active" -> RoutineStatus.Active
            "Paused" -> RoutineStatus.Paused
            "Archived" -> RoutineStatus.Archived
            else -> null
        }
    }
}
