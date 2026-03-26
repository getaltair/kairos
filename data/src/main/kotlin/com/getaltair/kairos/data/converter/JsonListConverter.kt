package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.UUID

/**
 * Type converter for [List<UUID>] to/from [String] using Moshi.
 *
 * Serializes UUID lists as JSON arrays for storage.
 */
class JsonListConverter {

    private val moshi: Moshi = Moshi.Builder().build()

    private val uuidListAdapter: JsonAdapter<List<UUID>> by lazy {
        val type = Types.newParameterizedType(List::class.java, UUID::class.java)
        moshi.adapter(type)
    }

    private val stringListAdapter: JsonAdapter<List<String>> by lazy {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        moshi.adapter(type)
    }

    /**
     * Converts [List<UUID>] to JSON [String].
     */
    @TypeConverter
    fun uuidListToString(list: List<UUID>?): String? = list?.let { uuidListAdapter.toJson(it) }

    /**
     * Converts JSON [String] to [List<UUID>].
     */
    @TypeConverter
    fun stringToUuidList(json: String?): List<UUID>? {
        if (json.isNullOrBlank()) return null
        return try {
            uuidListAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts [List<String>] to JSON [String].
     * Used for habit subtasks.
     */
    @TypeConverter
    fun stringListToString(list: List<String>?): String? =
        list?.let { stringListAdapter.toJson(it) }

    /**
     * Converts JSON [String] to [List<String>].
     */
    @TypeConverter
    fun stringToStringList(json: String?): List<String>? {
        if (json.isNullOrBlank()) return null
        return try {
            stringListAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
