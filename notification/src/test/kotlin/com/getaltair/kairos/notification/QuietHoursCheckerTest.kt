package com.getaltair.kairos.notification

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuietHoursCheckerTest {

    private lateinit var checker: QuietHoursChecker

    @Before
    fun setUp() {
        checker = QuietHoursChecker()
    }

    // --- Normal range (no midnight crossing) ---

    @Test
    fun `normal range - time inside returns true`() {
        val start = LocalTime.of(14, 0) // 2:00 PM
        val end = LocalTime.of(16, 0) // 4:00 PM
        assertTrue(checker.isInQuietHours(LocalTime.of(15, 0), start, end))
    }

    @Test
    fun `normal range - time outside returns false`() {
        val start = LocalTime.of(14, 0)
        val end = LocalTime.of(16, 0)
        assertFalse(checker.isInQuietHours(LocalTime.of(12, 0), start, end))
    }

    @Test
    fun `normal range - time after end returns false`() {
        val start = LocalTime.of(14, 0)
        val end = LocalTime.of(16, 0)
        assertFalse(checker.isInQuietHours(LocalTime.of(17, 0), start, end))
    }

    // --- Midnight-crossing range (e.g. 22:00 to 07:00) ---

    @Test
    fun `midnight crossing - 23_00 is in quiet hours`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)
        assertTrue(checker.isInQuietHours(LocalTime.of(23, 0), start, end))
    }

    @Test
    fun `midnight crossing - 06_00 is in quiet hours`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)
        assertTrue(checker.isInQuietHours(LocalTime.of(6, 0), start, end))
    }

    @Test
    fun `midnight crossing - 08_00 is not in quiet hours`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)
        assertFalse(checker.isInQuietHours(LocalTime.of(8, 0), start, end))
    }

    @Test
    fun `midnight crossing - 12_00 is not in quiet hours`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)
        assertFalse(checker.isInQuietHours(LocalTime.of(12, 0), start, end))
    }

    // --- Boundary conditions ---

    @Test
    fun `boundary - exactly at start is inclusive`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)
        assertTrue(checker.isInQuietHours(LocalTime.of(22, 0), start, end))
    }

    @Test
    fun `boundary - just before end is in quiet hours`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)
        assertTrue(checker.isInQuietHours(LocalTime.of(6, 59), start, end))
    }

    @Test
    fun `boundary - exactly at end is exclusive`() {
        val start = LocalTime.of(22, 0)
        val end = LocalTime.of(7, 0)
        assertFalse(checker.isInQuietHours(LocalTime.of(7, 0), start, end))
    }

    @Test
    fun `boundary - normal range at start is inclusive`() {
        val start = LocalTime.of(14, 0)
        val end = LocalTime.of(16, 0)
        assertTrue(checker.isInQuietHours(LocalTime.of(14, 0), start, end))
    }

    @Test
    fun `boundary - normal range at end is exclusive`() {
        val start = LocalTime.of(14, 0)
        val end = LocalTime.of(16, 0)
        assertFalse(checker.isInQuietHours(LocalTime.of(16, 0), start, end))
    }

    // --- getNextDeliveryTime ---

    @Test
    fun `getNextDeliveryTime adds 1 minute to end time`() {
        val end = LocalTime.of(7, 0)
        assertEquals(LocalTime.of(7, 1), checker.getNextDeliveryTime(end))
    }

    @Test
    fun `getNextDeliveryTime handles non-zero minutes`() {
        val end = LocalTime.of(7, 30)
        assertEquals(LocalTime.of(7, 31), checker.getNextDeliveryTime(end))
    }
}
