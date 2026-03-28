package com.getaltair.kairos.domain.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearRoutineDataTest {

    private fun typical() = WearRoutineData(
        routineId = "routine-001",
        executionId = "exec-abc",
        name = "Morning Routine",
        steps = listOf("Stretch", "Meditate", "Journal"),
        currentStepIndex = 1,
        status = "RUNNING",
        remainingSeconds = 300,
    )

    // -- single-object round trips --

    @Test
    fun `toJson fromJson round trip with typical data`() {
        val original = typical()
        val json = original.toJson()
        val restored = WearRoutineData.fromJson(json)

        assertNotNull(restored)
        restored!!
        assertEquals(original.routineId, restored.routineId)
        assertEquals(original.executionId, restored.executionId)
        assertEquals(original.name, restored.name)
        assertEquals(original.steps, restored.steps)
        assertEquals(original.currentStepIndex, restored.currentStepIndex)
        assertEquals(original.status, restored.status)
        assertEquals(original.remainingSeconds, restored.remainingSeconds)
    }

    @Test
    fun `toJson fromJson round trip with empty steps`() {
        val original = typical().copy(steps = emptyList())
        val json = original.toJson()
        val restored = WearRoutineData.fromJson(json)

        assertNotNull(restored)
        assertTrue(restored!!.steps.isEmpty())
    }

    @Test
    fun `toJson fromJson round trip with single step`() {
        val original = typical().copy(steps = listOf("Only Step"))
        val json = original.toJson()
        val restored = WearRoutineData.fromJson(json)

        assertNotNull(restored)
        assertEquals(listOf("Only Step"), restored!!.steps)
    }

    @Test
    fun `toJson fromJson round trip with step names containing commas`() {
        val original = typical().copy(
            steps = listOf("Wake up, stretch", "Eat breakfast, drink water", "Go outside"),
        )
        val json = original.toJson()
        val restored = WearRoutineData.fromJson(json)

        assertNotNull(restored)
        restored!!
        assertEquals(3, restored.steps.size)
        assertEquals("Wake up, stretch", restored.steps[0])
        assertEquals("Eat breakfast, drink water", restored.steps[1])
        assertEquals("Go outside", restored.steps[2])
    }

    @Test
    fun `toJson fromJson with zero integer fields`() {
        val original = typical().copy(currentStepIndex = 0, remainingSeconds = 0)
        val json = original.toJson()
        val restored = WearRoutineData.fromJson(json)

        assertNotNull(restored)
        assertEquals(0, restored!!.currentStepIndex)
        assertEquals(0, restored.remainingSeconds)
    }

    @Test
    fun `fromJson correctly parses steps list`() {
        val json =
            """{"routineId":"r1","executionId":"e1","name":"Test","steps":["A","B","C"],""" +
                """"currentStepIndex":0,"status":"RUNNING","remainingSeconds":60}"""
        val restored = WearRoutineData.fromJson(json)

        assertNotNull(restored)
        assertEquals(listOf("A", "B", "C"), restored!!.steps)
    }

    // -- null / invalid input --

    @Test
    fun `fromJson returns null for missing routineId`() {
        val json =
            """{"executionId":"e1","name":"Test","steps":[],""" +
                """"currentStepIndex":0,"status":"RUNNING","remainingSeconds":60}"""
        assertNull(WearRoutineData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for blank routineId`() {
        val json =
            """{"routineId":"","executionId":"e1","name":"Test","steps":[],""" +
                """"currentStepIndex":0,"status":"RUNNING","remainingSeconds":60}"""
        assertNull(WearRoutineData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for missing executionId`() {
        val json =
            """{"routineId":"r1","name":"Test","steps":[],""" +
                """"currentStepIndex":0,"status":"RUNNING","remainingSeconds":60}"""
        assertNull(WearRoutineData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for blank executionId`() {
        val json =
            """{"routineId":"r1","executionId":"","name":"Test","steps":[],""" +
                """"currentStepIndex":0,"status":"RUNNING","remainingSeconds":60}"""
        assertNull(WearRoutineData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for empty string`() {
        assertNull(WearRoutineData.fromJson(""))
    }

    @Test
    fun `fromJson returns null for garbage input`() {
        assertNull(WearRoutineData.fromJson("not json"))
    }
}
