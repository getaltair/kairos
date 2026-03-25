package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.SkipReason

/**
 * Type converter for [SkipReason] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "TooTired") for storage.
 */
class SkipReasonConverter {

    /**
     * Converts [SkipReason] to its simple class name [String].
     */
    @TypeConverter
    fun skipReasonToString(reason: SkipReason?): String? = reason?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [SkipReason].
     */
    @TypeConverter
    fun stringToSkipReason(name: String?): SkipReason? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "TooTired" -> SkipReason.TooTired
            "NoTime" -> SkipReason.NoTime
            "NotFeelingWell" -> SkipReason.NotFeelingWell
            "Traveling" -> SkipReason.Traveling
            "TookDayOff" -> SkipReason.TookDayOff
            "Other" -> SkipReason.Other
            else -> null
        }
    }
}
