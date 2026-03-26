package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Type converter for [Map<String, Any>] to/from [String] using Moshi.
 *
 * Serializes maps as JSON objects for storage.
 * Used for UserPreferences.notificationChannels.
 */
class JsonMapConverter {

    private val moshi: Moshi = Moshi.Builder().build()

    private val mapAdapter: JsonAdapter<Map<String, Any>> by lazy {
        val type = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Any::class.java
        )
        moshi.adapter(type)
    }

    /**
     * Converts [Map<String, Any>] to JSON [String].
     */
    @TypeConverter
    fun mapToString(map: Map<String, Any>?): String? = map?.let { mapAdapter.toJson(it) }

    /**
     * Converts JSON [String] to [Map<String, Any>].
     */
    @TypeConverter
    fun stringToMap(json: String?): Map<String, Any>? {
        if (json.isNullOrBlank()) return null
        return try {
            mapAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
