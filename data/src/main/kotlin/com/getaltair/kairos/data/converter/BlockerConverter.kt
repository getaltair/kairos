package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import com.getaltair.kairos.domain.enums.Blocker
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber

/**
 * Type converter for [List<Blocker>] sealed class to/from JSON [String].
 *
 * Serializes Blocker lists as JSON arrays of simple name strings for storage.
 * Uses a when-based mapping consistent with other sealed class converters.
 */
class BlockerConverter {

    private val moshi: Moshi = Moshi.Builder().build()

    private val stringListAdapter: JsonAdapter<List<String>> by lazy {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        moshi.adapter(type)
    }

    /**
     * Converts [List<Blocker>] to JSON [String].
     */
    @TypeConverter
    fun blockerListToString(blockers: List<Blocker>?): String? = blockers?.let { list ->
        stringListAdapter.toJson(list.map { it.javaClass.simpleName })
    }

    /**
     * Converts JSON [String] to [List<Blocker>].
     */
    @TypeConverter
    fun stringToBlockerList(json: String?): List<Blocker>? {
        if (json.isNullOrBlank()) return null
        return try {
            val names = stringListAdapter.fromJson(json) ?: return null
            names.mapNotNull { blockerFromName(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse blocker list from JSON: $json")
            null
        }
    }

    private fun blockerFromName(name: String): Blocker? = when (name) {
        "NoEnergy" -> Blocker.NoEnergy
        "PainPhysical" -> Blocker.PainPhysical
        "PainMental" -> Blocker.PainMental
        "TooBusy" -> Blocker.TooBusy
        "FamilyEmergency" -> Blocker.FamilyEmergency
        "WorkEmergency" -> Blocker.WorkEmergency
        "Sick" -> Blocker.Sick
        "Weather" -> Blocker.Weather
        "EquipmentFailure" -> Blocker.EquipmentFailure
        "Other" -> Blocker.Other
        else -> null
    }
}
