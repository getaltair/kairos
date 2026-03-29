package com.getaltair.kairos.data.converter

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Round-trip tests for all time-based converters:
 * [InstantConverter], [LocalDateConverter], [LocalTimeConverter].
 */
class TimeConverterTest {

    private lateinit var instantConverter: InstantConverter
    private lateinit var localDateConverter: LocalDateConverter
    private lateinit var localTimeConverter: LocalTimeConverter

    @Before
    fun setUp() {
        instantConverter = InstantConverter()
        localDateConverter = LocalDateConverter()
        localTimeConverter = LocalTimeConverter()
    }

    // ==================== InstantConverter ====================

    @Test
    fun `InstantConverter round trips a typical instant`() {
        val original = Instant.parse("2025-01-15T10:30:00Z")
        val stored = instantConverter.instantToLong(original)
        val restored = instantConverter.longToInstant(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `InstantConverter round trips epoch zero`() {
        val original = Instant.EPOCH
        val stored = instantConverter.instantToLong(original)
        assertNotNull(stored)
        assertEquals(0L, stored)
        val restored = instantConverter.longToInstant(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `InstantConverter round trips a date far in the future`() {
        val original = Instant.parse("2099-12-31T23:59:59.999Z")
        val stored = instantConverter.instantToLong(original)
        val restored = instantConverter.longToInstant(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `InstantConverter round trips a date in the past`() {
        val original = Instant.parse("1970-01-01T00:00:01Z")
        val stored = instantConverter.instantToLong(original)
        val restored = instantConverter.longToInstant(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `InstantConverter stores as epoch milliseconds`() {
        val original = Instant.parse("2025-06-15T12:00:00Z")
        val stored = instantConverter.instantToLong(original)
        assertNotNull(stored)
        assertEquals(original.toEpochMilli(), stored)
    }

    @Test
    fun `InstantConverter null instant produces null long`() {
        val stored = instantConverter.instantToLong(null)
        assertNull(stored)
    }

    @Test
    fun `InstantConverter null long produces null instant`() {
        val restored = instantConverter.longToInstant(null)
        assertNull(restored)
    }

    // ==================== LocalDateConverter ====================

    @Test
    fun `LocalDateConverter round trips a typical date`() {
        val original = LocalDate.of(2025, 6, 15)
        val stored = localDateConverter.localDateToString(original)
        val restored = localDateConverter.stringToLocalDate(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalDateConverter round trips January 1st`() {
        val original = LocalDate.of(2025, 1, 1)
        val stored = localDateConverter.localDateToString(original)
        val restored = localDateConverter.stringToLocalDate(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalDateConverter round trips December 31st`() {
        val original = LocalDate.of(2025, 12, 31)
        val stored = localDateConverter.localDateToString(original)
        val restored = localDateConverter.stringToLocalDate(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalDateConverter round trips leap day`() {
        val original = LocalDate.of(2024, 2, 29)
        val stored = localDateConverter.localDateToString(original)
        val restored = localDateConverter.stringToLocalDate(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalDateConverter stores as ISO format string`() {
        val original = LocalDate.of(2025, 6, 15)
        val stored = localDateConverter.localDateToString(original)
        assertEquals("2025-06-15", stored)
    }

    @Test
    fun `LocalDateConverter null date produces null string`() {
        val stored = localDateConverter.localDateToString(null)
        assertNull(stored)
    }

    @Test
    fun `LocalDateConverter null string produces null date`() {
        val restored = localDateConverter.stringToLocalDate(null)
        assertNull(restored)
    }

    // ==================== LocalTimeConverter ====================

    @Test
    fun `LocalTimeConverter round trips a typical time`() {
        val original = LocalTime.of(14, 30, 0)
        val stored = localTimeConverter.localTimeToString(original)
        val restored = localTimeConverter.stringToLocalTime(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalTimeConverter round trips midnight`() {
        val original = LocalTime.MIDNIGHT
        val stored = localTimeConverter.localTimeToString(original)
        val restored = localTimeConverter.stringToLocalTime(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalTimeConverter round trips end of day`() {
        val original = LocalTime.of(23, 59, 59)
        val stored = localTimeConverter.localTimeToString(original)
        val restored = localTimeConverter.stringToLocalTime(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalTimeConverter round trips noon`() {
        val original = LocalTime.NOON
        val stored = localTimeConverter.localTimeToString(original)
        val restored = localTimeConverter.stringToLocalTime(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalTimeConverter round trips time with seconds`() {
        val original = LocalTime.of(8, 15, 45)
        val stored = localTimeConverter.localTimeToString(original)
        val restored = localTimeConverter.stringToLocalTime(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalTimeConverter null time produces null string`() {
        val stored = localTimeConverter.localTimeToString(null)
        assertNull(stored)
    }

    @Test
    fun `LocalTimeConverter null string produces null time`() {
        val restored = localTimeConverter.stringToLocalTime(null)
        assertNull(restored)
    }
}
