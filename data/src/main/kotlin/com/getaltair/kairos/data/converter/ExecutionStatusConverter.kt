package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.ExecutionStatus

/**
 * Type converter for [ExecutionStatus] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "NotStarted") for storage.
 */
class ExecutionStatusConverter {

    /**
     * Converts [ExecutionStatus] to its simple class name [String].
     */
    @TypeConverter
    fun executionStatusToString(status: ExecutionStatus?): String? = status?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [ExecutionStatus].
     */
    @TypeConverter
    fun stringToExecutionStatus(name: String?): ExecutionStatus? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "NotStarted" -> ExecutionStatus.NotStarted
            "InProgress" -> ExecutionStatus.InProgress
            "Paused" -> ExecutionStatus.Paused
            "Completed" -> ExecutionStatus.Completed
            "Abandoned" -> ExecutionStatus.Abandoned
            else -> throw IllegalArgumentException("Unknown ExecutionStatus: $name")
        }
    }
}
