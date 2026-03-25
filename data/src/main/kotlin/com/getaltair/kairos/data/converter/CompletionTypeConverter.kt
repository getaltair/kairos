package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.CompletionType

/**
 * Type converter for [CompletionType] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "Full") for storage.
 */
class CompletionTypeConverter {

    /**
     * Converts [CompletionType] to its simple class name [String].
     */
    @TypeConverter
    fun completionTypeToString(type: CompletionType?): String? = type?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [CompletionType].
     */
    @TypeConverter
    fun stringToCompletionType(name: String?): CompletionType? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Full" -> CompletionType.Full
            "Partial" -> CompletionType.Partial
            "Skipped" -> CompletionType.Skipped
            "Missed" -> CompletionType.Missed
            else -> null
        }
    }
}
