package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.Theme

/**
 * Type converter for [Theme] sealed class to/from [String].
 *
 * Uses simple class name (e.g., "System") for storage.
 */
class ThemeConverter {

    /**
     * Converts [Theme] to its simple class name [String].
     */
    @TypeConverter
    fun themeToString(theme: Theme?): String? = theme?.javaClass?.simpleName

    /**
     * Converts simple class name [String] to [Theme].
     */
    @TypeConverter
    fun stringToTheme(name: String?): Theme? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "System" -> Theme.System
            "Light" -> Theme.Light
            "Dark" -> Theme.Dark
            else -> null
        }
    }
}
