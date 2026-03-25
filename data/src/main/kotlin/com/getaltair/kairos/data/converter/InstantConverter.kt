package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Type converter for [Instant] to [Long] (Unix epoch milliseconds).
 *
 * Storing Instant as Long is more efficient than String storage
 * and allows for easy date/time calculations.
 */
class InstantConverter {

    /**
     * Converts [Instant] to [Long] (milliseconds since epoch).
     */
    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilli()

    /**
     * Converts [Long] (milliseconds since epoch) to [Instant].
     */
    @TypeConverter
    fun longToInstant(timestamp: Long?): Instant? = timestamp?.let { Instant.ofEpochMilli(it) }
}
