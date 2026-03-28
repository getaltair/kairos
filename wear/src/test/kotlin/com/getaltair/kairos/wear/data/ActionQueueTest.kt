package com.getaltair.kairos.wear.data

import com.getaltair.kairos.domain.wear.WearAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ActionQueue] JSON serialization round-trip logic.
 *
 * Since ActionQueue depends on Android DataStore (Context-backed),
 * these tests validate the JSON parsing and splitting helpers
 * indirectly through the domain WearAction serialization contract.
 */
class ActionQueueTest {

    @Test
    fun `WearAction CompleteHabit round-trips through JSON`() {
        val action = WearAction.CompleteHabit("habit-1", "FULL")
        val json = action.toJson()
        val restored = WearAction.fromJson(json)
        assertTrue(restored is WearAction.CompleteHabit)
        val complete = restored as WearAction.CompleteHabit
        assertEquals("habit-1", complete.habitId)
        assertEquals("FULL", complete.type)
    }

    @Test
    fun `WearAction CompleteHabit with partial percent round-trips`() {
        val action = WearAction.CompleteHabit("habit-2", "PARTIAL", 75)
        val json = action.toJson()
        val restored = WearAction.fromJson(json) as WearAction.CompleteHabit
        assertEquals("habit-2", restored.habitId)
        assertEquals("PARTIAL", restored.type)
        assertEquals(75, restored.partialPercent)
    }

    @Test
    fun `WearAction SkipHabit round-trips through JSON`() {
        val action = WearAction.SkipHabit("habit-2", "not today")
        val json = action.toJson()
        val restored = WearAction.fromJson(json)
        assertTrue(restored is WearAction.SkipHabit)
        val skip = restored as WearAction.SkipHabit
        assertEquals("habit-2", skip.habitId)
        assertEquals("not today", skip.reason)
    }

    @Test
    fun `WearAction SkipHabit without reason round-trips`() {
        val action = WearAction.SkipHabit("habit-3")
        val json = action.toJson()
        val restored = WearAction.fromJson(json) as WearAction.SkipHabit
        assertEquals("habit-3", restored.habitId)
        assertEquals(null, restored.reason)
    }

    @Test
    fun `WearAction StartRoutine round-trips through JSON`() {
        val action = WearAction.StartRoutine("routine-1")
        val json = action.toJson()
        val restored = WearAction.fromJson(json)
        assertTrue(restored is WearAction.StartRoutine)
        assertEquals("routine-1", (restored as WearAction.StartRoutine).routineId)
    }

    @Test
    fun `WearAction AdvanceRoutineStep round-trips through JSON`() {
        val action = WearAction.AdvanceRoutineStep("exec-1")
        val json = action.toJson()
        val restored = WearAction.fromJson(json)
        assertTrue(restored is WearAction.AdvanceRoutineStep)
        assertEquals("exec-1", (restored as WearAction.AdvanceRoutineStep).executionId)
    }

    @Test
    fun `WearAction PauseRoutine round-trips through JSON`() {
        val action = WearAction.PauseRoutine("exec-2")
        val json = action.toJson()
        val restored = WearAction.fromJson(json)
        assertTrue(restored is WearAction.PauseRoutine)
        assertEquals("exec-2", (restored as WearAction.PauseRoutine).executionId)
    }

    @Test
    fun `fromJson returns null for invalid JSON`() {
        val result = WearAction.fromJson("{\"invalid\":\"data\"}")
        assertEquals(null, result)
    }

    @Test
    fun `fromJson returns null for empty string`() {
        val result = WearAction.fromJson("")
        assertEquals(null, result)
    }
}
