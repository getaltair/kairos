package com.getaltair.kairos.wear

import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.SkipReason
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests the internal enum parsing helpers on [WearMessageListenerService].
 *
 * The test class lives in the same package so it can access `internal`
 * visibility members. A relaxed MockK instance sidesteps the Android
 * Service lifecycle while allowing direct calls to the pure-logic parsers.
 */
class WearMessageListenerEnumTest {

    private lateinit var service: WearMessageListenerService

    @Before
    fun setUp() {
        service = mockk(relaxed = true)
        every { service.parseCompletionType(any()) } answers { callOriginal() }
        every { service.parseSkipReason(any()) } answers { callOriginal() }
    }

    // ------------------------------------------------------------------
    // parseCompletionType
    // ------------------------------------------------------------------

    @Test
    fun `parseCompletionType FULL returns Full`() {
        assertEquals(CompletionType.Full, service.parseCompletionType("FULL"))
    }

    @Test
    fun `parseCompletionType DONE returns Full`() {
        assertEquals(CompletionType.Full, service.parseCompletionType("DONE"))
    }

    @Test
    fun `parseCompletionType full lowercase returns Full`() {
        assertEquals(CompletionType.Full, service.parseCompletionType("full"))
    }

    @Test
    fun `parseCompletionType Done mixed case returns Full`() {
        assertEquals(CompletionType.Full, service.parseCompletionType("Done"))
    }

    @Test
    fun `parseCompletionType PARTIAL returns Partial`() {
        assertEquals(CompletionType.Partial, service.parseCompletionType("PARTIAL"))
    }

    @Test
    fun `parseCompletionType partial lowercase returns Partial`() {
        assertEquals(CompletionType.Partial, service.parseCompletionType("partial"))
    }

    @Test
    fun `parseCompletionType SKIPPED returns Skipped`() {
        assertEquals(CompletionType.Skipped, service.parseCompletionType("SKIPPED"))
    }

    @Test
    fun `parseCompletionType skipped lowercase returns Skipped`() {
        assertEquals(CompletionType.Skipped, service.parseCompletionType("skipped"))
    }

    @Test
    fun `parseCompletionType unknown string defaults to Full`() {
        assertEquals(CompletionType.Full, service.parseCompletionType("UNKNOWN"))
    }

    @Test
    fun `parseCompletionType empty string defaults to Full`() {
        assertEquals(CompletionType.Full, service.parseCompletionType(""))
    }

    @Test
    fun `parseCompletionType covers all known CompletionType variants`() {
        // Ensure the parser can produce every CompletionType it should handle.
        // Missed is not produced by the parser (only Full, Partial, Skipped).
        val produced = setOf(
            service.parseCompletionType("FULL"),
            service.parseCompletionType("PARTIAL"),
            service.parseCompletionType("SKIPPED"),
        )
        assertTrue(produced.contains(CompletionType.Full))
        assertTrue(produced.contains(CompletionType.Partial))
        assertTrue(produced.contains(CompletionType.Skipped))
    }

    // ------------------------------------------------------------------
    // parseSkipReason
    // ------------------------------------------------------------------

    @Test
    fun `parseSkipReason too tired returns TooTired`() {
        assertEquals(SkipReason.TooTired, service.parseSkipReason("too tired"))
    }

    @Test
    fun `parseSkipReason too_tired returns TooTired`() {
        assertEquals(SkipReason.TooTired, service.parseSkipReason("too_tired"))
    }

    @Test
    fun `parseSkipReason no time returns NoTime`() {
        assertEquals(SkipReason.NoTime, service.parseSkipReason("no time"))
    }

    @Test
    fun `parseSkipReason no_time returns NoTime`() {
        assertEquals(SkipReason.NoTime, service.parseSkipReason("no_time"))
    }

    @Test
    fun `parseSkipReason not feeling well returns NotFeelingWell`() {
        assertEquals(SkipReason.NotFeelingWell, service.parseSkipReason("not feeling well"))
    }

    @Test
    fun `parseSkipReason not_feeling_well returns NotFeelingWell`() {
        assertEquals(SkipReason.NotFeelingWell, service.parseSkipReason("not_feeling_well"))
    }

    @Test
    fun `parseSkipReason traveling returns Traveling`() {
        assertEquals(SkipReason.Traveling, service.parseSkipReason("traveling"))
    }

    @Test
    fun `parseSkipReason took day off returns TookDayOff`() {
        assertEquals(SkipReason.TookDayOff, service.parseSkipReason("took day off"))
    }

    @Test
    fun `parseSkipReason took_day_off returns TookDayOff`() {
        assertEquals(SkipReason.TookDayOff, service.parseSkipReason("took_day_off"))
    }

    @Test
    fun `parseSkipReason other returns Other`() {
        assertEquals(SkipReason.Other, service.parseSkipReason("other"))
    }

    @Test
    fun `parseSkipReason Traveling uppercase returns Traveling`() {
        // Input is lowercased internally, so "Traveling" -> "traveling"
        assertEquals(SkipReason.Traveling, service.parseSkipReason("Traveling"))
    }

    @Test
    fun `parseSkipReason unknown string returns null`() {
        assertNull(service.parseSkipReason("something_else"))
    }

    @Test
    fun `parseSkipReason empty string returns null`() {
        assertNull(service.parseSkipReason(""))
    }

    @Test
    fun `parseSkipReason covers all SkipReason variants`() {
        val produced = setOf(
            service.parseSkipReason("too tired"),
            service.parseSkipReason("no time"),
            service.parseSkipReason("not feeling well"),
            service.parseSkipReason("traveling"),
            service.parseSkipReason("took day off"),
            service.parseSkipReason("other"),
        )
        assertTrue(produced.contains(SkipReason.TooTired))
        assertTrue(produced.contains(SkipReason.NoTime))
        assertTrue(produced.contains(SkipReason.NotFeelingWell))
        assertTrue(produced.contains(SkipReason.Traveling))
        assertTrue(produced.contains(SkipReason.TookDayOff))
        assertTrue(produced.contains(SkipReason.Other))
    }
}
