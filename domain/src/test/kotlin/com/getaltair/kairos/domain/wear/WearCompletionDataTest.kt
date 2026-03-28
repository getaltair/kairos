package com.getaltair.kairos.domain.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearCompletionDataTest {

    private fun typical() = WearCompletionData(
        id = "comp-001",
        habitId = "habit-123",
        date = "2026-03-28",
        type = "FULL",
        partialPercent = null,
    )

    // -- single-object round trips --

    @Test
    fun `toJson fromJson round trip with typical data`() {
        val original = typical()
        val json = original.toJson()
        val restored = WearCompletionData.fromJson(json)

        assertNotNull(restored)
        restored!!
        assertEquals(original.id, restored.id)
        assertEquals(original.habitId, restored.habitId)
        assertEquals(original.date, restored.date)
        assertEquals(original.type, restored.type)
        assertEquals(original.partialPercent, restored.partialPercent)
    }

    @Test
    fun `toJson fromJson round trip with partial percent`() {
        val original = typical().copy(type = "PARTIAL", partialPercent = 75)
        val json = original.toJson()
        val restored = WearCompletionData.fromJson(json)

        assertNotNull(restored)
        restored!!
        assertEquals("PARTIAL", restored.type)
        assertEquals(75, restored.partialPercent)
    }

    @Test
    fun `toJson fromJson round trip with null partialPercent`() {
        val original = typical()
        val json = original.toJson()

        // JSON should contain "partialPercent":null
        assertTrue(json.contains("\"partialPercent\":null"))

        val restored = WearCompletionData.fromJson(json)
        assertNotNull(restored)
        assertNull(restored!!.partialPercent)
    }

    @Test
    fun `toJson fromJson with special characters in date`() {
        // Dates are simple strings; verify no breakage with an unusual value
        val original = typical().copy(date = "2026/03/28")
        val json = original.toJson()
        val restored = WearCompletionData.fromJson(json)

        assertNotNull(restored)
        assertEquals("2026/03/28", restored!!.date)
    }

    // -- null / invalid input --

    @Test
    fun `fromJson returns null for missing id field`() {
        val json = """{"habitId":"h1","date":"2026-03-28","type":"FULL","partialPercent":null}"""
        assertNull(WearCompletionData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for blank id`() {
        val json = """{"id":"","habitId":"h1","date":"2026-03-28","type":"FULL","partialPercent":null}"""
        assertNull(WearCompletionData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for missing habitId field`() {
        val json = """{"id":"c1","date":"2026-03-28","type":"FULL","partialPercent":null}"""
        assertNull(WearCompletionData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for blank habitId`() {
        val json = """{"id":"c1","habitId":"","date":"2026-03-28","type":"FULL","partialPercent":null}"""
        assertNull(WearCompletionData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for empty string`() {
        assertNull(WearCompletionData.fromJson(""))
    }

    @Test
    fun `fromJson returns null for garbage input`() {
        assertNull(WearCompletionData.fromJson("not json"))
    }

    // -- list round trips --

    @Test
    fun `listToJson listFromJson round trip with empty list`() {
        val json = WearCompletionData.listToJson(emptyList())
        assertEquals("[]", json)
        val restored = WearCompletionData.listFromJson(json)
        assertTrue(restored.isEmpty())
    }

    @Test
    fun `listToJson listFromJson round trip with multiple items`() {
        val items = listOf(
            typical(),
            typical().copy(id = "comp-002", habitId = "habit-456", type = "PARTIAL", partialPercent = 50),
            typical().copy(id = "comp-003", habitId = "habit-789"),
        )
        val json = WearCompletionData.listToJson(items)
        val restored = WearCompletionData.listFromJson(json)

        assertEquals(items.size, restored.size)
        for (i in items.indices) {
            assertEquals(items[i].id, restored[i].id)
            assertEquals(items[i].habitId, restored[i].habitId)
            assertEquals(items[i].partialPercent, restored[i].partialPercent)
        }
    }

    @Test
    fun `listFromJson with blank string returns empty list`() {
        assertTrue(WearCompletionData.listFromJson("").isEmpty())
        assertTrue(WearCompletionData.listFromJson("   ").isEmpty())
    }

    @Test
    fun `listFromJson with empty array returns empty list`() {
        assertTrue(WearCompletionData.listFromJson("[]").isEmpty())
    }

    @Test
    fun `listFromJson with malformed JSON returns empty list`() {
        val malformed = "[{garbage},{more garbage}]"
        assertTrue(WearCompletionData.listFromJson(malformed).isEmpty())
    }
}
