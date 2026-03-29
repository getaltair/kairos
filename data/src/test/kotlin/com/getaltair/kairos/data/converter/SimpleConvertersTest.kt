package com.getaltair.kairos.data.converter

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Round-trip tests for [SimpleConverters] object.
 * SimpleConverters is an object singleton providing time-based converters
 * without Moshi dependencies.
 */
class SimpleConvertersTest {

    // ==================== Instant ====================

    @Test
    fun `SimpleConverters round trips a typical instant`() {
        val original = Instant.parse("2025-01-15T10:30:00Z")
        val stored = SimpleConverters.instantToLong(original)
        val restored = SimpleConverters.longToInstant(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SimpleConverters round trips epoch zero`() {
        val original = Instant.EPOCH
        val stored = SimpleConverters.instantToLong(original)
        assertNotNull(stored)
        assertEquals(0L, stored)
        val restored = SimpleConverters.longToInstant(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SimpleConverters null instant produces null long`() {
        assertNull(SimpleConverters.instantToLong(null))
    }

    @Test
    fun `SimpleConverters null long produces null instant`() {
        assertNull(SimpleConverters.longToInstant(null))
    }

    // ==================== LocalDate ====================

    @Test
    fun `SimpleConverters round trips a typical date`() {
        val original = LocalDate.of(2025, 6, 15)
        val stored = SimpleConverters.localDateToString(original)
        val restored = SimpleConverters.stringToLocalDate(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SimpleConverters round trips leap day`() {
        val original = LocalDate.of(2024, 2, 29)
        val stored = SimpleConverters.localDateToString(original)
        val restored = SimpleConverters.stringToLocalDate(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SimpleConverters stores date as ISO format string`() {
        val original = LocalDate.of(2025, 3, 5)
        val stored = SimpleConverters.localDateToString(original)
        assertEquals("2025-03-05", stored)
    }

    @Test
    fun `SimpleConverters null date produces null string`() {
        assertNull(SimpleConverters.localDateToString(null))
    }

    @Test
    fun `SimpleConverters null string produces null date`() {
        assertNull(SimpleConverters.stringToLocalDate(null))
    }

    // ==================== LocalTime ====================

    @Test
    fun `SimpleConverters round trips a typical time`() {
        val original = LocalTime.of(14, 30, 0)
        val stored = SimpleConverters.localTimeToString(original)
        val restored = SimpleConverters.stringToLocalTime(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SimpleConverters round trips midnight`() {
        val original = LocalTime.MIDNIGHT
        val stored = SimpleConverters.localTimeToString(original)
        val restored = SimpleConverters.stringToLocalTime(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SimpleConverters round trips noon`() {
        val original = LocalTime.NOON
        val stored = SimpleConverters.localTimeToString(original)
        val restored = SimpleConverters.stringToLocalTime(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SimpleConverters null time produces null string`() {
        assertNull(SimpleConverters.localTimeToString(null))
    }

    @Test
    fun `SimpleConverters null string produces null time`() {
        assertNull(SimpleConverters.stringToLocalTime(null))
    }
}
