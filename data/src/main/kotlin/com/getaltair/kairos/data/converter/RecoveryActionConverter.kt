package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.RecoveryAction

/**
 * Type converter for [RecoveryAction] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "Resume") for storage.
 */
class RecoveryActionConverter {

    /**
     * Converts [RecoveryAction] to its simple class name [String].
     */
    @TypeConverter
    fun recoveryActionToString(action: RecoveryAction?): String? = action?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [RecoveryAction].
     */
    @TypeConverter
    fun stringToRecoveryAction(name: String?): RecoveryAction? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Resume" -> RecoveryAction.Resume
            "Simplify" -> RecoveryAction.Simplify
            "Pause" -> RecoveryAction.Pause
            "Archive" -> RecoveryAction.Archive
            "FreshStart" -> RecoveryAction.FreshStart
            else -> null
        }
    }
}
