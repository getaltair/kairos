package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.Blocker
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Type converter for [List<Blocker>] sealed class to/from JSON [String].
 *
 * Serializes Blocker lists as JSON arrays for storage.
 * Used in RecoverySession for blockers list.
 */
class BlockerConverter {

    private val moshi: Moshi = Moshi.Builder().build()

    private val blockerListAdapter: JsonAdapter<List<Blocker>> by lazy {
        val type = Types.newParameterizedType(List::class.java, Blocker::class.java)
        moshi.adapter(type)
    }

    /**
     * Converts [List<Blocker>] to JSON [String].
     */
    @TypeConverter
    fun blockerListToString(blockers: List<Blocker>?): String? =
        blockers?.let { list -> blockerListAdapter.toJson(list) }

    /**
     * Converts JSON [String] to [List<Blocker>].
     */
    @TypeConverter
    fun stringToBlockerList(json: String?): List<Blocker>? {
        if (json.isNullOrBlank()) return null
        return try {
            blockerListAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
