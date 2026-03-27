package com.getaltair.kairos.dashboard.data

import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.enums.SkipReason
import com.google.cloud.Timestamp
import java.time.DayOfWeek
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminFirestoreMapperTest {

    // Fixed instants for deterministic tests (truncated to seconds for
    // Timestamp round-trip fidelity).
    private val now: Instant = Instant.ofEpochSecond(1_700_000_000L)
    private val later: Instant = Instant.ofEpochSecond(1_700_001_000L)

    /** Helper: creates a Cloud [Timestamp] from a [java.time.Instant]. */
    private fun Instant.toCloudTimestamp(): Timestamp = Timestamp.ofTimeSecondsAndNanos(epochSecond, nano)

    // -----------------------------------------------------------------------
    // habitFromMap tests
    // -----------------------------------------------------------------------

    @Test
    fun habitFromMap_fullMap_roundTripsAllFields() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "name" to "Meditate",
            "description" to "10 min morning meditation",
            "icon" to "brain",
            "color" to "#FF5733",
            "anchorBehavior" to "After brushing teeth",
            "anchorType" to "AFTER_BEHAVIOR",
            "timeWindow" to mapOf("start" to "07:00", "end" to "08:00"),
            "category" to "MORNING",
            "frequency" to "CUSTOM",
            "activeDays" to listOf("MONDAY", "WEDNESDAY"),
            "estimatedSeconds" to 600L,
            "microVersion" to "5 deep breaths",
            "allowPartial" to true,
            "subtasks" to listOf("Sit down", "Set timer", "Focus"),
            "lapseThresholdDays" to 5L,
            "relapseThresholdDays" to 10L,
            "phase" to "FORMING",
            "status" to "ACTIVE",
            "createdAt" to now.toCloudTimestamp(),
            "updatedAt" to later.toCloudTimestamp(),
            "pausedAt" to null,
            "archivedAt" to null,
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertEquals(id, habit.id)
        assertEquals("Meditate", habit.name)
        assertEquals("10 min morning meditation", habit.description)
        assertEquals("brain", habit.icon)
        assertEquals("#FF5733", habit.color)
        assertEquals("After brushing teeth", habit.anchorBehavior)
        assertEquals(AnchorType.AfterBehavior, habit.anchorType)
        assertEquals("07:00", habit.timeWindowStart)
        assertEquals("08:00", habit.timeWindowEnd)
        assertEquals(HabitCategory.Morning, habit.category)
        assertEquals(HabitFrequency.Custom, habit.frequency)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), habit.activeDays)
        assertEquals(600, habit.estimatedSeconds)
        assertEquals("5 deep breaths", habit.microVersion)
        assertTrue(habit.allowPartialCompletion)
        assertEquals(listOf("Sit down", "Set timer", "Focus"), habit.subtasks)
        assertEquals(5, habit.lapseThresholdDays)
        assertEquals(10, habit.relapseThresholdDays)
        assertEquals(HabitPhase.FORMING, habit.phase)
        assertEquals(HabitStatus.Active, habit.status)
        assertEquals(now, habit.createdAt)
        assertEquals(later, habit.updatedAt)
        assertNull(habit.pausedAt)
        assertNull(habit.archivedAt)
    }

    @Test
    fun habitFromMap_minimalMap_usesDefaults() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = emptyMap()

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertEquals(id, habit.id)
        assertEquals("", habit.name)
        assertEquals("", habit.anchorBehavior)
        assertEquals(AnchorType.AfterBehavior, habit.anchorType)
        assertNull(habit.timeWindowStart)
        assertNull(habit.timeWindowEnd)
        assertEquals(HabitCategory.Anytime, habit.category)
        assertEquals(HabitFrequency.Daily, habit.frequency)
        assertNull(habit.activeDays)
        assertEquals(300, habit.estimatedSeconds)
        assertNull(habit.microVersion)
        assertTrue(habit.allowPartialCompletion)
        assertNull(habit.subtasks)
        assertEquals(3, habit.lapseThresholdDays)
        assertEquals(7, habit.relapseThresholdDays)
        assertEquals(HabitPhase.ONBOARD, habit.phase)
        assertEquals(HabitStatus.Active, habit.status)
        assertNull(habit.pausedAt)
        assertNull(habit.archivedAt)
    }

    @Test
    fun habitFromMap_unknownCategoryTag_defaultsToAnytime() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "category" to "NONEXISTENT_CATEGORY",
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertEquals(HabitCategory.Anytime, habit.category)
    }

    @Test
    fun habitFromMap_unknownFrequencyTag_defaultsToDaily() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "frequency" to "BIWEEKLY",
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertEquals(HabitFrequency.Daily, habit.frequency)
    }

    @Test
    fun habitFromMap_missingTimeWindow_nullStartEnd() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "name" to "Walk",
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertNull(habit.timeWindowStart)
        assertNull(habit.timeWindowEnd)
    }

    @Test
    fun habitFromMap_malformedActiveDays_skipsInvalid() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "activeDays" to listOf("MONDAY", "GARBAGE", "FRIDAY", "NOT_A_DAY"),
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), habit.activeDays)
    }

    @Test
    fun habitFromMap_unknownAnchorTypeTag_defaultsToAfterBehavior() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "anchorType" to "UNKNOWN_ANCHOR",
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertEquals(AnchorType.AfterBehavior, habit.anchorType)
    }

    @Test
    fun habitFromMap_unknownPhaseTag_defaultsToOnboard() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "phase" to "GRADUATED",
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertEquals(HabitPhase.ONBOARD, habit.phase)
    }

    @Test
    fun habitFromMap_unknownStatusTag_defaultsToActive() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "status" to "DELETED",
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertEquals(HabitStatus.Active, habit.status)
    }

    @Test
    fun habitFromMap_emptySubtasks_becomesNull() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "subtasks" to emptyList<String>(),
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertNull(habit.subtasks)
    }

    @Test
    fun habitFromMap_pausedAtPresent_parsesCorrectly() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "pausedAt" to later.toCloudTimestamp(),
        )

        val habit = AdminFirestoreMapper.habitFromMap(id.toString(), map)

        assertEquals(later, habit.pausedAt)
    }

    @Test
    fun habitFromMap_allCategories_parsedCorrectly() {
        val tags = mapOf(
            "MORNING" to HabitCategory.Morning,
            "AFTERNOON" to HabitCategory.Afternoon,
            "EVENING" to HabitCategory.Evening,
            "ANYTIME" to HabitCategory.Anytime,
            "DEPARTURE" to HabitCategory.Departure,
        )
        val id = UUID.randomUUID().toString()

        tags.forEach { (tag, expected) ->
            val map: Map<String, Any?> = mapOf("category" to tag)
            val habit = AdminFirestoreMapper.habitFromMap(id, map)
            assertEquals("Category tag '$tag' should map to $expected", expected, habit.category)
        }
    }

    @Test
    fun habitFromMap_allFrequencies_parsedCorrectly() {
        val tags = mapOf(
            "DAILY" to HabitFrequency.Daily,
            "WEEKDAYS" to HabitFrequency.Weekdays,
            "WEEKENDS" to HabitFrequency.Weekends,
            "CUSTOM" to HabitFrequency.Custom,
        )
        val id = UUID.randomUUID().toString()

        tags.forEach { (tag, expected) ->
            val map: Map<String, Any?> = mapOf("frequency" to tag)
            val habit = AdminFirestoreMapper.habitFromMap(id, map)
            assertEquals("Frequency tag '$tag' should map to $expected", expected, habit.frequency)
        }
    }

    @Test
    fun habitFromMap_allPhases_parsedCorrectly() {
        val tags = mapOf(
            "ONBOARD" to HabitPhase.ONBOARD,
            "FORMING" to HabitPhase.FORMING,
            "MAINTAINING" to HabitPhase.MAINTAINING,
            "LAPSED" to HabitPhase.LAPSED,
            "RELAPSED" to HabitPhase.RELAPSED,
        )
        val id = UUID.randomUUID().toString()

        tags.forEach { (tag, expected) ->
            val map: Map<String, Any?> = mapOf("phase" to tag)
            val habit = AdminFirestoreMapper.habitFromMap(id, map)
            assertEquals("Phase tag '$tag' should map to $expected", expected, habit.phase)
        }
    }

    @Test
    fun habitFromMap_allStatuses_parsedCorrectly() {
        val tags = mapOf(
            "ACTIVE" to HabitStatus.Active,
            "PAUSED" to HabitStatus.Paused,
            "ARCHIVED" to HabitStatus.Archived,
        )
        val id = UUID.randomUUID().toString()

        tags.forEach { (tag, expected) ->
            val map: Map<String, Any?> = mapOf("status" to tag)
            val habit = AdminFirestoreMapper.habitFromMap(id, map)
            assertEquals("Status tag '$tag' should map to $expected", expected, habit.status)
        }
    }

    @Test
    fun habitFromMap_allAnchorTypes_parsedCorrectly() {
        val tags = mapOf(
            "AFTER_BEHAVIOR" to AnchorType.AfterBehavior,
            "BEFORE_BEHAVIOR" to AnchorType.BeforeBehavior,
            "AT_LOCATION" to AnchorType.AtLocation,
            "AT_TIME" to AnchorType.AtTime,
        )
        val id = UUID.randomUUID().toString()

        tags.forEach { (tag, expected) ->
            val map: Map<String, Any?> = mapOf("anchorType" to tag)
            val habit = AdminFirestoreMapper.habitFromMap(id, map)
            assertEquals("AnchorType tag '$tag' should map to $expected", expected, habit.anchorType)
        }
    }

    // -----------------------------------------------------------------------
    // completionFromMap tests
    // -----------------------------------------------------------------------

    @Test
    fun completionFromMap_booleanType_setsNoTypeFields() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-03-15",
            "completedAt" to now.toCloudTimestamp(),
            "type" to "FULL",
            "energyLevel" to 3L,
            "note" to "Felt great",
            "createdAt" to now.toCloudTimestamp(),
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(id, completion.id)
        assertEquals(habitId, completion.habitId)
        assertEquals(CompletionType.Full, completion.type)
        assertNull(completion.partialPercent)
        assertNull(completion.skipReason)
        assertEquals(3, completion.energyLevel)
        assertEquals("Felt great", completion.note)
    }

    @Test
    fun completionFromMap_partialType_setsPartialPercent() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "completedAt" to now.toCloudTimestamp(),
            "type" to "PARTIAL",
            "partialPercent" to 50L,
            "createdAt" to now.toCloudTimestamp(),
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(CompletionType.Partial, completion.type)
        assertEquals(50, completion.partialPercent)
        assertNull(completion.skipReason)
    }

    @Test
    fun completionFromMap_skippedType_setsSkipReason() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "completedAt" to now.toCloudTimestamp(),
            "type" to "SKIPPED",
            "skipReason" to "TOO_TIRED",
            "createdAt" to now.toCloudTimestamp(),
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(CompletionType.Skipped, completion.type)
        assertEquals(SkipReason.TooTired, completion.skipReason)
        assertNull(completion.partialPercent)
    }

    @Test
    fun completionFromMap_energyLevelCoercedLow() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "type" to "FULL",
            "energyLevel" to 0L,
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(1, completion.energyLevel)
    }

    @Test
    fun completionFromMap_energyLevelCoercedHigh() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "type" to "FULL",
            "energyLevel" to 10L,
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(5, completion.energyLevel)
    }

    @Test
    fun completionFromMap_partialPercentCoercedLow() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "type" to "PARTIAL",
            "partialPercent" to 0L,
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(1, completion.partialPercent)
    }

    @Test
    fun completionFromMap_partialPercentCoercedHigh() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "type" to "PARTIAL",
            "partialPercent" to 200L,
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(99, completion.partialPercent)
    }

    @Test(expected = IllegalStateException::class)
    fun completionFromMap_missingHabitId_throwsError() {
        val id = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "date" to "2025-06-01",
            "type" to "FULL",
        )

        AdminFirestoreMapper.completionFromMap(id.toString(), map)
    }

    @Test(expected = IllegalStateException::class)
    fun completionFromMap_missingDate_throwsError() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "type" to "FULL",
        )

        AdminFirestoreMapper.completionFromMap(id.toString(), map)
    }

    @Test
    fun completionFromMap_allCompletionTypes_parsedCorrectly() {
        val habitId = UUID.randomUUID().toString()
        val baseMap: Map<String, Any?> = mapOf(
            "habitId" to habitId,
            "date" to "2025-06-01",
        )

        val typeMapping = mapOf(
            "FULL" to CompletionType.Full,
            "PARTIAL" to CompletionType.Partial,
            "SKIPPED" to CompletionType.Skipped,
            "MISSED" to CompletionType.Missed,
        )

        typeMapping.forEach { (tag, expected) ->
            val map = baseMap + ("type" to tag)
            val completion = AdminFirestoreMapper.completionFromMap(UUID.randomUUID().toString(), map)
            assertEquals("CompletionType tag '$tag' should map to $expected", expected, completion.type)
        }
    }

    @Test
    fun completionFromMap_unknownCompletionType_defaultsToFull() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "type" to "NONEXISTENT_TYPE",
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(CompletionType.Full, completion.type)
    }

    @Test
    fun completionFromMap_allSkipReasons_parsedCorrectly() {
        val habitId = UUID.randomUUID().toString()
        val baseMap: Map<String, Any?> = mapOf(
            "habitId" to habitId,
            "date" to "2025-06-01",
            "type" to "SKIPPED",
        )

        val reasonMapping = mapOf(
            "TOO_TIRED" to SkipReason.TooTired,
            "NO_TIME" to SkipReason.NoTime,
            "NOT_FEELING_WELL" to SkipReason.NotFeelingWell,
            "TRAVELING" to SkipReason.Traveling,
            "TOOK_DAY_OFF" to SkipReason.TookDayOff,
            "OTHER" to SkipReason.Other,
        )

        reasonMapping.forEach { (tag, expected) ->
            val map = baseMap + ("skipReason" to tag)
            val completion = AdminFirestoreMapper.completionFromMap(UUID.randomUUID().toString(), map)
            assertEquals("SkipReason tag '$tag' should map to $expected", expected, completion.skipReason)
        }
    }

    @Test
    fun completionFromMap_unknownSkipReason_defaultsToOther() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "type" to "SKIPPED",
            "skipReason" to "DOG_ATE_HOMEWORK",
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(SkipReason.Other, completion.skipReason)
    }

    @Test
    fun completionFromMap_nullEnergyLevel_remainsNull() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "type" to "FULL",
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertNull(completion.energyLevel)
    }

    @Test
    fun completionFromMap_missedType_noPartialOrSkip() {
        val id = UUID.randomUUID()
        val habitId = UUID.randomUUID()
        val map: Map<String, Any?> = mapOf(
            "habitId" to habitId.toString(),
            "date" to "2025-06-01",
            "type" to "MISSED",
            "partialPercent" to 50L,
            "skipReason" to "TOO_TIRED",
        )

        val completion = AdminFirestoreMapper.completionFromMap(id.toString(), map)

        assertEquals(CompletionType.Missed, completion.type)
        assertNull(completion.partialPercent)
        assertNull(completion.skipReason)
    }
}
