package com.getaltair.kairos.domain.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearHabitDataTest {

    private fun typical() = WearHabitData(
        id = "habit-123",
        name = "Morning Run",
        anchorBehavior = "After coffee",
        category = "MORNING",
        estimatedSeconds = 1800,
        icon = "run",
        color = "#FF5722",
    )

    // -- single-object round trips --

    @Test
    fun `toJson fromJson round trip with typical data`() {
        val original = typical()
        val json = original.toJson()
        val restored = WearHabitData.fromJson(json)

        assertNotNull(restored)
        restored!!
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertEquals(original.anchorBehavior, restored.anchorBehavior)
        assertEquals(original.category, restored.category)
        assertEquals(original.estimatedSeconds, restored.estimatedSeconds)
        assertEquals(original.icon, restored.icon)
        assertEquals(original.color, restored.color)
    }

    @Test
    fun `toJson fromJson round trip with null optional fields`() {
        val original = typical().copy(icon = null, color = null)
        val json = original.toJson()
        val restored = WearHabitData.fromJson(json)

        assertNotNull(restored)
        restored!!
        assertEquals(original.id, restored.id)
        assertEquals(original.name, restored.name)
        assertNull(restored.icon)
        assertNull(restored.color)
    }

    @Test
    fun `toJson fromJson with special characters in name`() {
        val original = typical().copy(name = """He said "hello" \ ok""")
        val json = original.toJson()

        // The escaped JSON must not break parsing
        val restored = WearHabitData.fromJson(json)
        assertNotNull(restored)
    }

    @Test
    fun `toJson fromJson with zero estimatedSeconds`() {
        val original = typical().copy(estimatedSeconds = 0)
        val json = original.toJson()
        val restored = WearHabitData.fromJson(json)

        assertNotNull(restored)
        assertEquals(0, restored!!.estimatedSeconds)
    }

    // -- null / invalid input --

    @Test
    fun `fromJson returns null for missing id field`() {
        val json =
            """{"name":"Run","anchorBehavior":"x","category":"MORNING",""" +
                """"estimatedSeconds":60,"icon":null,"color":null}"""
        assertNull(WearHabitData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for blank id`() {
        val json =
            """{"id":"","name":"Run","anchorBehavior":"x","category":"MORNING",""" +
                """"estimatedSeconds":60,"icon":null,"color":null}"""
        assertNull(WearHabitData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for missing name field`() {
        val json =
            """{"id":"abc","anchorBehavior":"x","category":"MORNING",""" +
                """"estimatedSeconds":60,"icon":null,"color":null}"""
        assertNull(WearHabitData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for blank name`() {
        val json =
            """{"id":"abc","name":"","anchorBehavior":"x","category":"MORNING",""" +
                """"estimatedSeconds":60,"icon":null,"color":null}"""
        assertNull(WearHabitData.fromJson(json))
    }

    @Test
    fun `fromJson returns null for empty string`() {
        assertNull(WearHabitData.fromJson(""))
    }

    @Test
    fun `fromJson returns null for garbage input`() {
        assertNull(WearHabitData.fromJson("not json at all"))
    }

    // -- list round trips --

    @Test
    fun `listToJson listFromJson round trip with empty list`() {
        val json = WearHabitData.listToJson(emptyList())
        assertEquals("[]", json)
        val restored = WearHabitData.listFromJson(json)
        assertTrue(restored.isEmpty())
    }

    @Test
    fun `listToJson listFromJson round trip with multiple items`() {
        val items = listOf(
            typical(),
            typical().copy(id = "habit-456", name = "Read", estimatedSeconds = 900),
            typical().copy(id = "habit-789", name = "Stretch", icon = null),
        )
        val json = WearHabitData.listToJson(items)
        val restored = WearHabitData.listFromJson(json)

        assertEquals(items.size, restored.size)
        for (i in items.indices) {
            assertEquals(items[i].id, restored[i].id)
            assertEquals(items[i].name, restored[i].name)
        }
    }

    @Test
    fun `listFromJson with blank string returns empty list`() {
        assertTrue(WearHabitData.listFromJson("").isEmpty())
        assertTrue(WearHabitData.listFromJson("   ").isEmpty())
    }

    @Test
    fun `listFromJson with empty array returns empty list`() {
        assertTrue(WearHabitData.listFromJson("[]").isEmpty())
    }

    @Test
    fun `listFromJson with malformed JSON returns empty list`() {
        // Each element individually fails fromJson (returns null), so mapNotNull yields empty
        val malformed = "[{bad},{also bad}]"
        assertTrue(WearHabitData.listFromJson(malformed).isEmpty())
    }
}
