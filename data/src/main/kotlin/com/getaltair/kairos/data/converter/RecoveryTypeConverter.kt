package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.RecoveryType

/**
 * Type converter for [RecoveryType] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "Lapse") for storage.
 */
class RecoveryTypeConverter {

    /**
     * Converts [RecoveryType] to its simple class name [String].
     */
    @TypeConverter
    fun recoveryTypeToString(type: RecoveryType?): String? = type?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [RecoveryType].
     */
    @TypeConverter
    fun stringToRecoveryType(name: String?): RecoveryType? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Lapse" -> RecoveryType.Lapse
            "Relapse" -> RecoveryType.Relapse
            else -> null
        }
    }
}
