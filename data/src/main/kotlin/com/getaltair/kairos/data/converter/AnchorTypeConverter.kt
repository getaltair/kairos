package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.AnchorType

/**
 * Type converter for [AnchorType] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "AfterBehavior") for storage.
 */
class AnchorTypeConverter {

    /**
     * Converts [AnchorType] to its simple class name [String].
     */
    @TypeConverter
    fun anchorTypeToString(anchorType: AnchorType?): String? = anchorType?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [AnchorType].
     */
    @TypeConverter
    fun stringToAnchorType(name: String?): AnchorType? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "AfterBehavior" -> AnchorType.AfterBehavior
            "BeforeBehavior" -> AnchorType.BeforeBehavior
            "AtLocation" -> AnchorType.AtLocation
            "AtTime" -> AnchorType.AtTime
            else -> null
        }
    }
}
