package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.SessionStatus

/**
 * Type converter for [SessionStatus] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "Pending") for storage.
 */
class SessionStatusConverter {

    /**
     * Converts [SessionStatus] to its simple class name [String].
     */
    @TypeConverter
    fun sessionStatusToString(status: SessionStatus?): String? = status?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [SessionStatus].
     */
    @TypeConverter
    fun stringToSessionStatus(name: String?): SessionStatus? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Pending" -> SessionStatus.Pending
            "Completed" -> SessionStatus.Completed
            "Abandoned" -> SessionStatus.Abandoned
            else -> null
        }
    }
}
