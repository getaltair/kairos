package com.getaltair.kairos.domain.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearActionTest {

    // -- CompleteHabit --

    @Test
    fun `CompleteHabit round trip with full completion`() {
        val original = WearAction.CompleteHabit(habitId = "h1", type = "FULL")
        val json = original.toJson()
        val restored = WearAction.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored is WearAction.CompleteHabit)
        val r = restored as WearAction.CompleteHabit
        assertEquals("h1", r.habitId)
        assertEquals("FULL", r.type)
        assertNull(r.partialPercent)
    }

    @Test
    fun `CompleteHabit round trip with partial percent`() {
        val original = WearAction.CompleteHabit(habitId = "h2", type = "PARTIAL", partialPercent = 60)
        val json = original.toJson()
        val restored = WearAction.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored is WearAction.CompleteHabit)
        val r = restored as WearAction.CompleteHabit
        assertEquals("h2", r.habitId)
        assertEquals("PARTIAL", r.type)
        assertEquals(60, r.partialPercent)
    }

    // -- SkipHabit --

    @Test
    fun `SkipHabit round trip with reason`() {
        val original = WearAction.SkipHabit(habitId = "h3", reason = "Sick today")
        val json = original.toJson()
        val restored = WearAction.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored is WearAction.SkipHabit)
        val r = restored as WearAction.SkipHabit
        assertEquals("h3", r.habitId)
        assertEquals("Sick today", r.reason)
    }

    @Test
    fun `SkipHabit round trip with null reason`() {
        val original = WearAction.SkipHabit(habitId = "h4", reason = null)
        val json = original.toJson()
        val restored = WearAction.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored is WearAction.SkipHabit)
        val r = restored as WearAction.SkipHabit
        assertEquals("h4", r.habitId)
        assertNull(r.reason)
    }

    // -- StartRoutine --

    @Test
    fun `StartRoutine round trip`() {
        val original = WearAction.StartRoutine(routineId = "r1")
        val json = original.toJson()
        val restored = WearAction.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored is WearAction.StartRoutine)
        assertEquals("r1", (restored as WearAction.StartRoutine).routineId)
    }

    // -- AdvanceRoutineStep --

    @Test
    fun `AdvanceRoutineStep round trip`() {
        val original = WearAction.AdvanceRoutineStep(executionId = "e1")
        val json = original.toJson()
        val restored = WearAction.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored is WearAction.AdvanceRoutineStep)
        assertEquals("e1", (restored as WearAction.AdvanceRoutineStep).executionId)
    }

    // -- PauseRoutine --

    @Test
    fun `PauseRoutine round trip`() {
        val original = WearAction.PauseRoutine(executionId = "e2")
        val json = original.toJson()
        val restored = WearAction.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored is WearAction.PauseRoutine)
        assertEquals("e2", (restored as WearAction.PauseRoutine).executionId)
    }

    // -- SkipRoutineStep --

    @Test
    fun `SkipRoutineStep round trip`() {
        val original = WearAction.SkipRoutineStep(executionId = "e3")
        val json = original.toJson()
        val restored = WearAction.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored is WearAction.SkipRoutineStep)
        assertEquals("e3", (restored as WearAction.SkipRoutineStep).executionId)
    }

    // -- fromJson error cases --

    @Test
    fun `fromJson returns null for missing type field`() {
        val json = """{"habitId":"h1","completionType":"FULL","partialPercent":null}"""
        assertNull(WearAction.fromJson(json))
    }

    @Test
    fun `fromJson returns null for unknown type`() {
        val json = """{"type":"unknown_action","habitId":"h1"}"""
        assertNull(WearAction.fromJson(json))
    }

    @Test
    fun `fromJson returns null for empty string`() {
        assertNull(WearAction.fromJson(""))
    }

    @Test
    fun `fromJson returns null for garbage input`() {
        assertNull(WearAction.fromJson("not json"))
    }

    @Test
    fun `fromJson returns null when required habitId missing for CompleteHabit`() {
        val json = """{"type":"habit_completed","completionType":"FULL","partialPercent":null}"""
        assertNull(WearAction.fromJson(json))
    }

    @Test
    fun `fromJson returns null when required habitId missing for SkipHabit`() {
        val json = """{"type":"habit_skipped","reason":"tired"}"""
        assertNull(WearAction.fromJson(json))
    }

    @Test
    fun `fromJson returns null when required routineId missing for StartRoutine`() {
        val json = """{"type":"routine_started"}"""
        assertNull(WearAction.fromJson(json))
    }

    @Test
    fun `fromJson returns null when required executionId missing for AdvanceRoutineStep`() {
        val json = """{"type":"routine_step_done"}"""
        assertNull(WearAction.fromJson(json))
    }

    @Test
    fun `fromJson returns null when required executionId missing for PauseRoutine`() {
        val json = """{"type":"routine_paused"}"""
        assertNull(WearAction.fromJson(json))
    }

    @Test
    fun `fromJson returns null when required executionId missing for SkipRoutineStep`() {
        val json = """{"type":"routine_step_skipped"}"""
        assertNull(WearAction.fromJson(json))
    }

    @Test
    fun `CompleteHabit defaults completionType to FULL when missing`() {
        val json = """{"type":"habit_completed","habitId":"h5","partialPercent":null}"""
        val restored = WearAction.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored is WearAction.CompleteHabit)
        assertEquals("FULL", (restored as WearAction.CompleteHabit).type)
    }
}
