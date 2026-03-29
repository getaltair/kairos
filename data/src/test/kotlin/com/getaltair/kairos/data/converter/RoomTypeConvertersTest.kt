package com.getaltair.kairos.data.converter

import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.enums.RecoveryAction
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.RoutineStatus
import com.getaltair.kairos.domain.enums.SessionStatus
import com.getaltair.kairos.domain.enums.SkipReason
import com.getaltair.kairos.domain.enums.Theme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the aggregate [RoomTypeConverters] object that wires all converters together.
 * Verifies that RoomTypeConverters delegates correctly across all converter types.
 */
class RoomTypeConvertersTest {

    // ==================== Time Converters ====================

    @Test
    fun `Instant round trip through RoomTypeConverters`() {
        val original = Instant.parse("2025-06-15T12:00:00Z")
        val stored = RoomTypeConverters.instantToLong(original)
        val restored = RoomTypeConverters.longToInstant(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `Instant null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.instantToLong(null))
        assertNull(RoomTypeConverters.longToInstant(null))
    }

    @Test
    fun `LocalDate round trip through RoomTypeConverters`() {
        val original = LocalDate.of(2025, 6, 15)
        val stored = RoomTypeConverters.localDateToString(original)
        val restored = RoomTypeConverters.stringToLocalDate(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalDate null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.localDateToString(null))
        assertNull(RoomTypeConverters.stringToLocalDate(null))
    }

    @Test
    fun `LocalTime round trip through RoomTypeConverters`() {
        val original = LocalTime.of(8, 30, 0)
        val stored = RoomTypeConverters.localTimeToString(original)
        val restored = RoomTypeConverters.stringToLocalTime(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `LocalTime null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.localTimeToString(null))
        assertNull(RoomTypeConverters.stringToLocalTime(null))
    }

    // ==================== DayOfWeek Set Converter ====================

    @Test
    fun `DayOfWeek set round trip through RoomTypeConverters`() {
        val original = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val stored = RoomTypeConverters.dayOfWeekSetToString(original)
        val restored = RoomTypeConverters.stringToDayOfWeekSet(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `DayOfWeek set null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.dayOfWeekSetToString(null))
        assertNull(RoomTypeConverters.stringToDayOfWeekSet(null))
    }

    @Test
    fun `DayOfWeek set blank string returns null in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.stringToDayOfWeekSet(""))
        assertNull(RoomTypeConverters.stringToDayOfWeekSet("  "))
    }

    // ==================== UUID List Converter ====================

    @Test
    fun `UUID list round trip through RoomTypeConverters`() {
        val original = listOf(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
        val stored = RoomTypeConverters.uuidListToString(original)
        val restored = RoomTypeConverters.stringToUuidList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `UUID list null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.uuidListToString(null))
        assertNull(RoomTypeConverters.stringToUuidList(null))
    }

    // ==================== String List Converter ====================

    @Test
    fun `String list round trip through RoomTypeConverters`() {
        val original = listOf("task1", "task2", "task3")
        val stored = RoomTypeConverters.stringListToString(original)
        val restored = RoomTypeConverters.stringToStringList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `String list null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.stringListToString(null))
        assertNull(RoomTypeConverters.stringToStringList(null))
    }

    // ==================== Map Converter ====================

    @Test
    fun `Map round trip through RoomTypeConverters`() {
        val original = mapOf<String, Any>("key1" to "value1", "key2" to true)
        val stored = RoomTypeConverters.mapToString(original)
        val restored = RoomTypeConverters.stringToMap(stored)
        assertNotNull(restored)
        val restoredMap = restored!!
        assertEquals("value1", restoredMap["key1"])
        assertEquals(true, restoredMap["key2"])
    }

    @Test
    fun `Map null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.mapToString(null))
        assertNull(RoomTypeConverters.stringToMap(null))
    }

    // ==================== Blocker List Converter ====================

    @Test
    fun `Blocker list round trip through RoomTypeConverters`() {
        val original = listOf(Blocker.NoEnergy, Blocker.Sick)
        val stored = RoomTypeConverters.blockerListToString(original)
        val restored = RoomTypeConverters.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `Blocker list round trips all blockers through RoomTypeConverters`() {
        val original = listOf(
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
        val stored = RoomTypeConverters.blockerListToString(original)
        val restored = RoomTypeConverters.stringToBlockerList(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `Blocker list null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.blockerListToString(null))
        assertNull(RoomTypeConverters.stringToBlockerList(null))
    }

    // ==================== Enum Converters ====================

    @Test
    fun `AnchorType round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            AnchorType.AfterBehavior,
            AnchorType.BeforeBehavior,
            AnchorType.AtLocation,
            AnchorType.AtTime,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.anchorTypeToString(value)
            val restored = RoomTypeConverters.stringToAnchorType(stored)
            assertEquals("Failed round-trip for AnchorType.$value", value, restored)
        }
    }

    @Test
    fun `AnchorType null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.anchorTypeToString(null))
        assertNull(RoomTypeConverters.stringToAnchorType(null))
    }

    @Test
    fun `HabitCategory round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            HabitCategory.Morning,
            HabitCategory.Afternoon,
            HabitCategory.Evening,
            HabitCategory.Anytime,
            HabitCategory.Departure,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.habitCategoryToString(value)
            val restored = RoomTypeConverters.stringToHabitCategory(stored)
            assertEquals("Failed round-trip for HabitCategory.$value", value, restored)
        }
    }

    @Test
    fun `HabitCategory null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.habitCategoryToString(null))
        assertNull(RoomTypeConverters.stringToHabitCategory(null))
    }

    @Test
    fun `HabitFrequency round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            HabitFrequency.Daily,
            HabitFrequency.Weekdays,
            HabitFrequency.Weekends,
            HabitFrequency.Custom,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.habitFrequencyToString(value)
            val restored = RoomTypeConverters.stringToHabitFrequency(stored)
            assertEquals("Failed round-trip for HabitFrequency.$value", value, restored)
        }
    }

    @Test
    fun `HabitFrequency null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.habitFrequencyToString(null))
        assertNull(RoomTypeConverters.stringToHabitFrequency(null))
    }

    @Test
    fun `HabitPhase round trips all values through RoomTypeConverters`() {
        HabitPhase.ALL.forEach { value ->
            val stored = RoomTypeConverters.habitPhaseToString(value)
            val restored = RoomTypeConverters.stringToHabitPhase(stored)
            assertEquals("Failed round-trip for HabitPhase.$value", value, restored)
        }
    }

    @Test
    fun `HabitPhase null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.habitPhaseToString(null))
        assertNull(RoomTypeConverters.stringToHabitPhase(null))
    }

    @Test
    fun `HabitStatus round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            HabitStatus.Active,
            HabitStatus.Paused,
            HabitStatus.Archived,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.habitStatusToString(value)
            val restored = RoomTypeConverters.stringToHabitStatus(stored)
            assertEquals("Failed round-trip for HabitStatus.$value", value, restored)
        }
    }

    @Test
    fun `HabitStatus null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.habitStatusToString(null))
        assertNull(RoomTypeConverters.stringToHabitStatus(null))
    }

    @Test
    fun `CompletionType round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            CompletionType.Full,
            CompletionType.Partial,
            CompletionType.Skipped,
            CompletionType.Missed,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.completionTypeToString(value)
            val restored = RoomTypeConverters.stringToCompletionType(stored)
            assertEquals("Failed round-trip for CompletionType.$value", value, restored)
        }
    }

    @Test
    fun `CompletionType null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.completionTypeToString(null))
        assertNull(RoomTypeConverters.stringToCompletionType(null))
    }

    @Test
    fun `SkipReason round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            SkipReason.TooTired,
            SkipReason.NoTime,
            SkipReason.NotFeelingWell,
            SkipReason.Traveling,
            SkipReason.TookDayOff,
            SkipReason.Other,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.skipReasonToString(value)
            val restored = RoomTypeConverters.stringToSkipReason(stored)
            assertEquals("Failed round-trip for SkipReason.$value", value, restored)
        }
    }

    @Test
    fun `SkipReason null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.skipReasonToString(null))
        assertNull(RoomTypeConverters.stringToSkipReason(null))
    }

    @Test
    fun `RoutineStatus round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            RoutineStatus.Active,
            RoutineStatus.Paused,
            RoutineStatus.Archived,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.routineStatusToString(value)
            val restored = RoomTypeConverters.stringToRoutineStatus(stored)
            assertEquals("Failed round-trip for RoutineStatus.$value", value, restored)
        }
    }

    @Test
    fun `RoutineStatus null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.routineStatusToString(null))
        assertNull(RoomTypeConverters.stringToRoutineStatus(null))
    }

    @Test
    fun `ExecutionStatus round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            ExecutionStatus.NotStarted,
            ExecutionStatus.InProgress,
            ExecutionStatus.Paused,
            ExecutionStatus.Completed,
            ExecutionStatus.Abandoned,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.executionStatusToString(value)
            val restored = RoomTypeConverters.stringToExecutionStatus(stored)
            assertEquals("Failed round-trip for ExecutionStatus.$value", value, restored)
        }
    }

    @Test
    fun `ExecutionStatus null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.executionStatusToString(null))
        assertNull(RoomTypeConverters.stringToExecutionStatus(null))
    }

    @Test
    fun `RecoveryType round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            RecoveryType.Lapse,
            RecoveryType.Relapse,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.recoveryTypeToString(value)
            val restored = RoomTypeConverters.stringToRecoveryType(stored)
            assertEquals("Failed round-trip for RecoveryType.$value", value, restored)
        }
    }

    @Test
    fun `RecoveryType null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.recoveryTypeToString(null))
        assertNull(RoomTypeConverters.stringToRecoveryType(null))
    }

    @Test
    fun `SessionStatus round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            SessionStatus.Pending,
            SessionStatus.Completed,
            SessionStatus.Abandoned,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.sessionStatusToString(value)
            val restored = RoomTypeConverters.stringToSessionStatus(stored)
            assertEquals("Failed round-trip for SessionStatus.$value", value, restored)
        }
    }

    @Test
    fun `SessionStatus null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.sessionStatusToString(null))
        assertNull(RoomTypeConverters.stringToSessionStatus(null))
    }

    @Test
    fun `RecoveryAction round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            RecoveryAction.Resume,
            RecoveryAction.Simplify,
            RecoveryAction.Pause,
            RecoveryAction.Archive,
            RecoveryAction.FreshStart,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.recoveryActionToString(value)
            val restored = RoomTypeConverters.stringToRecoveryAction(stored)
            assertEquals("Failed round-trip for RecoveryAction.$value", value, restored)
        }
    }

    @Test
    fun `RecoveryAction null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.recoveryActionToString(null))
        assertNull(RoomTypeConverters.stringToRecoveryAction(null))
    }

    @Test
    fun `Theme round trips all values through RoomTypeConverters`() {
        val allValues = listOf(
            Theme.System,
            Theme.Light,
            Theme.Dark,
        )
        allValues.forEach { value ->
            val stored = RoomTypeConverters.themeToString(value)
            val restored = RoomTypeConverters.stringToTheme(stored)
            assertEquals("Failed round-trip for Theme.$value", value, restored)
        }
    }

    @Test
    fun `Theme null handling in RoomTypeConverters`() {
        assertNull(RoomTypeConverters.themeToString(null))
        assertNull(RoomTypeConverters.stringToTheme(null))
    }
}
