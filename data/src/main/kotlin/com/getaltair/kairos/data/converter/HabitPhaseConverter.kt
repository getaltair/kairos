package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.HabitPhase

/**
 * Type converter for [HabitPhase] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "ONBOARD") for storage.
 */
class HabitPhaseConverter {

    /**
     * Converts [HabitPhase] to its simple class name [String].
     */
    @TypeConverter
    fun habitPhaseToString(phase: HabitPhase?): String? = phase?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [HabitPhase].
     */
    @TypeConverter
    fun stringToHabitPhase(name: String?): HabitPhase? {
        if (name.isNullOrBlank()) return null
        return HabitPhase.fromSimpleName(name)
    }
}
