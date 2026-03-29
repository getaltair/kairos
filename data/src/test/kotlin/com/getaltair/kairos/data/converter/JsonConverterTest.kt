package com.getaltair.kairos.data.converter

import com.getaltair.kairos.domain.enums.Blocker
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Round-trip tests for JSON-based converters:
 * [JsonListConverter], [JsonMapConverter], [BlockerConverter], [DayOfWeekListConverter].
 */
class JsonConverterTest {

    private lateinit var jsonListConverter: JsonListConverter
    private lateinit var jsonMapConverter: JsonMapConverter
    private lateinit var blockerConverter: BlockerConverter
    private lateinit var dayOfWeekListConverter: DayOfWeekListConverter

    @Before
    fun setUp() {
        jsonListConverter = JsonListConverter()
        jsonMapConverter = JsonMapConverter()
        blockerConverter = BlockerConverter()
        dayOfWeekListConverter = DayOfWeekListConverter()
    }

    // ==================== JsonListConverter - UUID Lists ====================

    @Test
    fun `JsonListConverter UUID list round trips single UUID`() {
        val original = listOf(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        val stored = jsonListConverter.uuidListToString(original)
        val restored = jsonListConverter.stringToUuidList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `JsonListConverter UUID list round trips multiple UUIDs`() {
        val original = listOf(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"),
            UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"),
        )
        val stored = jsonListConverter.uuidListToString(original)
        val restored = jsonListConverter.stringToUuidList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `JsonListConverter UUID list round trips empty list`() {
        val original = emptyList<UUID>()
        val stored = jsonListConverter.uuidListToString(original)
        assertNotNull(stored)
        val restored = jsonListConverter.stringToUuidList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `JsonListConverter null UUID list produces null string`() {
        val stored = jsonListConverter.uuidListToString(null)
        assertNull(stored)
    }

    @Test
    fun `JsonListConverter null string produces null UUID list`() {
        val restored = jsonListConverter.stringToUuidList(null)
        assertNull(restored)
    }

    @Test
    fun `JsonListConverter blank string produces null UUID list`() {
        val restored = jsonListConverter.stringToUuidList("")
        assertNull(restored)
        val restoredBlank = jsonListConverter.stringToUuidList("  ")
        assertNull(restoredBlank)
    }

    @Test
    fun `JsonListConverter malformed JSON returns null for UUID list`() {
        assertNull(jsonListConverter.stringToUuidList("not json at all"))
    }

    @Test
    fun `JsonListConverter invalid UUID in valid JSON skips malformed entries`() {
        val json = """["550e8400-e29b-41d4-a716-446655440000","not-a-uuid"]"""
        val result = jsonListConverter.stringToUuidList(json)
        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), result[0])
    }

    @Test
    fun `JsonListConverter empty JSON array returns empty list for UUIDs`() {
        val result = jsonListConverter.stringToUuidList("[]")
        assertNotNull(result)
        assertEquals(0, result!!.size)
    }

    // ==================== JsonListConverter - String Lists ====================

    @Test
    fun `JsonListConverter round trips a single string list`() {
        val original = listOf("task1")
        val stored = jsonListConverter.stringListToString(original)
        val restored = jsonListConverter.stringToStringList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `JsonListConverter round trips multiple strings`() {
        val original = listOf("Brush teeth", "Floss", "Mouthwash")
        val stored = jsonListConverter.stringListToString(original)
        val restored = jsonListConverter.stringToStringList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `JsonListConverter round trips empty string list`() {
        val original = emptyList<String>()
        val stored = jsonListConverter.stringListToString(original)
        assertNotNull(stored)
        val restored = jsonListConverter.stringToStringList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `JsonListConverter round trips strings with special characters`() {
        val original = listOf("task with spaces", "task/with/slashes", "task\"with\"quotes")
        val stored = jsonListConverter.stringListToString(original)
        val restored = jsonListConverter.stringToStringList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `JsonListConverter null string list produces null string`() {
        val stored = jsonListConverter.stringListToString(null)
        assertNull(stored)
    }

    @Test
    fun `JsonListConverter null string produces null string list`() {
        val restored = jsonListConverter.stringToStringList(null)
        assertNull(restored)
    }

    @Test
    fun `JsonListConverter blank string produces null string list`() {
        val restored = jsonListConverter.stringToStringList("")
        assertNull(restored)
        val restoredBlank = jsonListConverter.stringToStringList("  ")
        assertNull(restoredBlank)
    }

    // ==================== JsonMapConverter ====================

    @Test
    fun `JsonMapConverter round trips a single entry map`() {
        val original = mapOf<String, Any>("key" to "value")
        val stored = jsonMapConverter.mapToString(original)
        val restored = jsonMapConverter.stringToMap(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `JsonMapConverter round trips a multi-entry map`() {
        val original = mapOf<String, Any>(
            "email" to true,
            "push" to false,
            "frequency" to "daily",
        )
        val stored = jsonMapConverter.mapToString(original)
        val restored = jsonMapConverter.stringToMap(stored)
        assertNotNull(restored)
        val restoredMap = restored!!
        assertEquals("daily", restoredMap["frequency"])
        assertEquals(true, restoredMap["email"])
        assertEquals(false, restoredMap["push"])
    }

    @Test
    fun `JsonMapConverter round trips an empty map`() {
        val original = emptyMap<String, Any>()
        val stored = jsonMapConverter.mapToString(original)
        assertNotNull(stored)
        val restored = jsonMapConverter.stringToMap(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `JsonMapConverter null map produces null string`() {
        val stored = jsonMapConverter.mapToString(null)
        assertNull(stored)
    }

    @Test
    fun `JsonMapConverter null string produces null map`() {
        val restored = jsonMapConverter.stringToMap(null)
        assertNull(restored)
    }

    @Test
    fun `JsonMapConverter blank string produces null map`() {
        val restored = jsonMapConverter.stringToMap("")
        assertNull(restored)
        val restoredBlank = jsonMapConverter.stringToMap("  ")
        assertNull(restoredBlank)
    }

    @Test
    fun `JsonMapConverter malformed JSON returns null`() {
        assertNull(jsonMapConverter.stringToMap("not json"))
    }

    // ==================== BlockerConverter ====================

    @Test
    fun `BlockerConverter round trips all Blocker values`() {
        val allBlockers = listOf(
            Blocker.NoEnergy,
            Blocker.PainPhysical,
            Blocker.PainMental,
            Blocker.TooBusy,
            Blocker.FamilyEmergency,
            Blocker.WorkEmergency,
            Blocker.Sick,
            Blocker.Weather,
            Blocker.EquipmentFailure,
            Blocker.Other,
        )
        val stored = blockerConverter.blockerListToString(allBlockers)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(allBlockers, restored)
    }

    @Test
    fun `BlockerConverter round trips NoEnergy individually`() {
        val original = listOf(Blocker.NoEnergy)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips PainPhysical individually`() {
        val original = listOf(Blocker.PainPhysical)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips PainMental individually`() {
        val original = listOf(Blocker.PainMental)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips TooBusy individually`() {
        val original = listOf(Blocker.TooBusy)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips FamilyEmergency individually`() {
        val original = listOf(Blocker.FamilyEmergency)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips WorkEmergency individually`() {
        val original = listOf(Blocker.WorkEmergency)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips Sick individually`() {
        val original = listOf(Blocker.Sick)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips Weather individually`() {
        val original = listOf(Blocker.Weather)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips EquipmentFailure individually`() {
        val original = listOf(Blocker.EquipmentFailure)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips Other individually`() {
        val original = listOf(Blocker.Other)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips empty list`() {
        val original = emptyList<Blocker>()
        val stored = blockerConverter.blockerListToString(original)
        assertNotNull(stored)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter round trips multiple blockers`() {
        val original = listOf(Blocker.NoEnergy, Blocker.Sick, Blocker.Weather)
        val stored = blockerConverter.blockerListToString(original)
        val restored = blockerConverter.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `BlockerConverter null list produces null string`() {
        val stored = blockerConverter.blockerListToString(null)
        assertNull(stored)
    }

    @Test
    fun `BlockerConverter null string produces null list`() {
        val restored = blockerConverter.stringToBlockerList(null)
        assertNull(restored)
    }

    @Test
    fun `BlockerConverter blank string produces null list`() {
        val restored = blockerConverter.stringToBlockerList("")
        assertNull(restored)
        val restoredBlank = blockerConverter.stringToBlockerList("  ")
        assertNull(restoredBlank)
    }

    @Test
    fun `BlockerConverter malformed JSON returns null`() {
        assertNull(blockerConverter.stringToBlockerList("garbage"))
    }

    @Test
    fun `BlockerConverter unknown blocker names are silently dropped`() {
        val json = """["NoEnergy","FutureBlocker","Sick"]"""
        val result = blockerConverter.stringToBlockerList(json)
        assertNotNull(result)
        assertEquals(listOf(Blocker.NoEnergy, Blocker.Sick), result)
    }

    // ==================== DayOfWeekListConverter ====================

    @Test
    fun `DayOfWeekListConverter round trips all days`() {
        val original = java.time.DayOfWeek.entries.toSet()
        val stored = dayOfWeekListConverter.dayOfWeekSetToString(original)
        val restored = dayOfWeekListConverter.stringToDayOfWeekSet(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `DayOfWeekListConverter round trips weekdays`() {
        val original = setOf(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY,
        )
        val stored = dayOfWeekListConverter.dayOfWeekSetToString(original)
        val restored = dayOfWeekListConverter.stringToDayOfWeekSet(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `DayOfWeekListConverter round trips weekends`() {
        val original = setOf(
            java.time.DayOfWeek.SATURDAY,
            java.time.DayOfWeek.SUNDAY,
        )
        val stored = dayOfWeekListConverter.dayOfWeekSetToString(original)
        val restored = dayOfWeekListConverter.stringToDayOfWeekSet(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `DayOfWeekListConverter round trips single day`() {
        val original = setOf(java.time.DayOfWeek.MONDAY)
        val stored = dayOfWeekListConverter.dayOfWeekSetToString(original)
        val restored = dayOfWeekListConverter.stringToDayOfWeekSet(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `DayOfWeekListConverter round trips each individual day`() {
        java.time.DayOfWeek.entries.forEach { day ->
            val original = setOf(day)
            val stored = dayOfWeekListConverter.dayOfWeekSetToString(original)
            val restored = dayOfWeekListConverter.stringToDayOfWeekSet(stored)
            assertEquals("Failed round-trip for $day", original, restored)
        }
    }

    @Test
    fun `DayOfWeekListConverter round trips empty set`() {
        val original = emptySet<java.time.DayOfWeek>()
        val stored = dayOfWeekListConverter.dayOfWeekSetToString(original)
        assertNotNull(stored)
        // Empty set stores as empty string, which parses back to null
        assertTrue(stored!!.isEmpty())
    }

    @Test
    fun `DayOfWeekListConverter null set produces null string`() {
        val stored = dayOfWeekListConverter.dayOfWeekSetToString(null)
        assertNull(stored)
    }

    @Test
    fun `DayOfWeekListConverter null string produces null set`() {
        val restored = dayOfWeekListConverter.stringToDayOfWeekSet(null)
        assertNull(restored)
    }

    @Test
    fun `DayOfWeekListConverter blank string produces null set`() {
        val restored = dayOfWeekListConverter.stringToDayOfWeekSet("")
        assertNull(restored)
        val restoredBlank = dayOfWeekListConverter.stringToDayOfWeekSet("  ")
        assertNull(restoredBlank)
    }

    @Test
    fun `DayOfWeekListConverter ignores invalid day names gracefully`() {
        val restored = dayOfWeekListConverter.stringToDayOfWeekSet("MONDAY,INVALID,FRIDAY")
        assertNotNull(restored)
        assertEquals(setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.FRIDAY), restored)
    }
}
