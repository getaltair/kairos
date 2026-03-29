package com.getaltair.kairos.data.converter

import com.getaltair.kairos.domain.enums.AnchorType
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Round-trip tests for all sealed-class (enum-like) converters.
 * Each converter is tested for every value and null handling.
 */
class EnumConverterTest {

    private lateinit var anchorTypeConverter: AnchorTypeConverter
    private lateinit var habitCategoryConverter: HabitCategoryConverter
    private lateinit var habitFrequencyConverter: HabitFrequencyConverter
    private lateinit var habitPhaseConverter: HabitPhaseConverter
    private lateinit var habitStatusConverter: HabitStatusConverter
    private lateinit var completionTypeConverter: CompletionTypeConverter
    private lateinit var skipReasonConverter: SkipReasonConverter
    private lateinit var routineStatusConverter: RoutineStatusConverter
    private lateinit var executionStatusConverter: ExecutionStatusConverter
    private lateinit var recoveryTypeConverter: RecoveryTypeConverter
    private lateinit var recoveryActionConverter: RecoveryActionConverter
    private lateinit var sessionStatusConverter: SessionStatusConverter
    private lateinit var themeConverter: ThemeConverter

    @Before
    fun setUp() {
        anchorTypeConverter = AnchorTypeConverter()
        habitCategoryConverter = HabitCategoryConverter()
        habitFrequencyConverter = HabitFrequencyConverter()
        habitPhaseConverter = HabitPhaseConverter()
        habitStatusConverter = HabitStatusConverter()
        completionTypeConverter = CompletionTypeConverter()
        skipReasonConverter = SkipReasonConverter()
        routineStatusConverter = RoutineStatusConverter()
        executionStatusConverter = ExecutionStatusConverter()
        recoveryTypeConverter = RecoveryTypeConverter()
        recoveryActionConverter = RecoveryActionConverter()
        sessionStatusConverter = SessionStatusConverter()
        themeConverter = ThemeConverter()
    }

    // ==================== AnchorTypeConverter ====================

    @Test
    fun `AnchorTypeConverter round trips AfterBehavior`() {
        val original = AnchorType.AfterBehavior
        val stored = anchorTypeConverter.anchorTypeToString(original)
        val restored = anchorTypeConverter.stringToAnchorType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `AnchorTypeConverter round trips BeforeBehavior`() {
        val original = AnchorType.BeforeBehavior
        val stored = anchorTypeConverter.anchorTypeToString(original)
        val restored = anchorTypeConverter.stringToAnchorType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `AnchorTypeConverter round trips AtLocation`() {
        val original = AnchorType.AtLocation
        val stored = anchorTypeConverter.anchorTypeToString(original)
        val restored = anchorTypeConverter.stringToAnchorType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `AnchorTypeConverter round trips AtTime`() {
        val original = AnchorType.AtTime
        val stored = anchorTypeConverter.anchorTypeToString(original)
        val restored = anchorTypeConverter.stringToAnchorType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `AnchorTypeConverter null input produces null output`() {
        assertNull(anchorTypeConverter.anchorTypeToString(null))
        assertNull(anchorTypeConverter.stringToAnchorType(null))
    }

    @Test
    fun `AnchorTypeConverter blank string produces null`() {
        assertNull(anchorTypeConverter.stringToAnchorType(""))
        assertNull(anchorTypeConverter.stringToAnchorType("  "))
    }

    @Test
    fun `AnchorTypeConverter unknown string produces null`() {
        assertNull(anchorTypeConverter.stringToAnchorType("UnknownValue"))
    }

    // ==================== HabitCategoryConverter ====================

    @Test
    fun `HabitCategoryConverter round trips Morning`() {
        val original = HabitCategory.Morning
        val stored = habitCategoryConverter.habitCategoryToString(original)
        val restored = habitCategoryConverter.stringToHabitCategory(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitCategoryConverter round trips Afternoon`() {
        val original = HabitCategory.Afternoon
        val stored = habitCategoryConverter.habitCategoryToString(original)
        val restored = habitCategoryConverter.stringToHabitCategory(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitCategoryConverter round trips Evening`() {
        val original = HabitCategory.Evening
        val stored = habitCategoryConverter.habitCategoryToString(original)
        val restored = habitCategoryConverter.stringToHabitCategory(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitCategoryConverter round trips Anytime`() {
        val original = HabitCategory.Anytime
        val stored = habitCategoryConverter.habitCategoryToString(original)
        val restored = habitCategoryConverter.stringToHabitCategory(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitCategoryConverter round trips Departure`() {
        val original = HabitCategory.Departure
        val stored = habitCategoryConverter.habitCategoryToString(original)
        val restored = habitCategoryConverter.stringToHabitCategory(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitCategoryConverter null input produces null output`() {
        assertNull(habitCategoryConverter.habitCategoryToString(null))
        assertNull(habitCategoryConverter.stringToHabitCategory(null))
    }

    @Test
    fun `HabitCategoryConverter blank string produces null`() {
        assertNull(habitCategoryConverter.stringToHabitCategory(""))
        assertNull(habitCategoryConverter.stringToHabitCategory("  "))
    }

    @Test
    fun `HabitCategoryConverter unknown string produces null`() {
        assertNull(habitCategoryConverter.stringToHabitCategory("UnknownValue"))
    }

    // ==================== HabitFrequencyConverter ====================

    @Test
    fun `HabitFrequencyConverter round trips Daily`() {
        val original = HabitFrequency.Daily
        val stored = habitFrequencyConverter.habitFrequencyToString(original)
        val restored = habitFrequencyConverter.stringToHabitFrequency(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitFrequencyConverter round trips Weekdays`() {
        val original = HabitFrequency.Weekdays
        val stored = habitFrequencyConverter.habitFrequencyToString(original)
        val restored = habitFrequencyConverter.stringToHabitFrequency(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitFrequencyConverter round trips Weekends`() {
        val original = HabitFrequency.Weekends
        val stored = habitFrequencyConverter.habitFrequencyToString(original)
        val restored = habitFrequencyConverter.stringToHabitFrequency(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitFrequencyConverter round trips Custom`() {
        val original = HabitFrequency.Custom
        val stored = habitFrequencyConverter.habitFrequencyToString(original)
        val restored = habitFrequencyConverter.stringToHabitFrequency(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitFrequencyConverter null input produces null output`() {
        assertNull(habitFrequencyConverter.habitFrequencyToString(null))
        assertNull(habitFrequencyConverter.stringToHabitFrequency(null))
    }

    @Test
    fun `HabitFrequencyConverter blank string produces null`() {
        assertNull(habitFrequencyConverter.stringToHabitFrequency(""))
        assertNull(habitFrequencyConverter.stringToHabitFrequency("  "))
    }

    @Test
    fun `HabitFrequencyConverter unknown string produces null`() {
        assertNull(habitFrequencyConverter.stringToHabitFrequency("UnknownValue"))
    }

    // ==================== HabitPhaseConverter ====================

    @Test
    fun `HabitPhaseConverter round trips ONBOARD`() {
        val original = HabitPhase.ONBOARD
        val stored = habitPhaseConverter.habitPhaseToString(original)
        val restored = habitPhaseConverter.stringToHabitPhase(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitPhaseConverter round trips FORMING`() {
        val original = HabitPhase.FORMING
        val stored = habitPhaseConverter.habitPhaseToString(original)
        val restored = habitPhaseConverter.stringToHabitPhase(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitPhaseConverter round trips MAINTAINING`() {
        val original = HabitPhase.MAINTAINING
        val stored = habitPhaseConverter.habitPhaseToString(original)
        val restored = habitPhaseConverter.stringToHabitPhase(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitPhaseConverter round trips LAPSED`() {
        val original = HabitPhase.LAPSED
        val stored = habitPhaseConverter.habitPhaseToString(original)
        val restored = habitPhaseConverter.stringToHabitPhase(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitPhaseConverter round trips RELAPSED`() {
        val original = HabitPhase.RELAPSED
        val stored = habitPhaseConverter.habitPhaseToString(original)
        val restored = habitPhaseConverter.stringToHabitPhase(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitPhaseConverter null input produces null output`() {
        assertNull(habitPhaseConverter.habitPhaseToString(null))
        assertNull(habitPhaseConverter.stringToHabitPhase(null))
    }

    @Test
    fun `HabitPhaseConverter blank string produces null`() {
        assertNull(habitPhaseConverter.stringToHabitPhase(""))
        assertNull(habitPhaseConverter.stringToHabitPhase("  "))
    }

    @Test
    fun `HabitPhaseConverter unknown string produces null`() {
        assertNull(habitPhaseConverter.stringToHabitPhase("UnknownValue"))
    }

    // ==================== HabitStatusConverter ====================

    @Test
    fun `HabitStatusConverter round trips Active`() {
        val original = HabitStatus.Active
        val stored = habitStatusConverter.habitStatusToString(original)
        val restored = habitStatusConverter.stringToHabitStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitStatusConverter round trips Paused`() {
        val original = HabitStatus.Paused
        val stored = habitStatusConverter.habitStatusToString(original)
        val restored = habitStatusConverter.stringToHabitStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitStatusConverter round trips Archived`() {
        val original = HabitStatus.Archived
        val stored = habitStatusConverter.habitStatusToString(original)
        val restored = habitStatusConverter.stringToHabitStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `HabitStatusConverter null input produces null output`() {
        assertNull(habitStatusConverter.habitStatusToString(null))
        assertNull(habitStatusConverter.stringToHabitStatus(null))
    }

    @Test
    fun `HabitStatusConverter blank string produces null`() {
        assertNull(habitStatusConverter.stringToHabitStatus(""))
        assertNull(habitStatusConverter.stringToHabitStatus("  "))
    }

    @Test
    fun `HabitStatusConverter unknown string produces null`() {
        assertNull(habitStatusConverter.stringToHabitStatus("UnknownValue"))
    }

    // ==================== CompletionTypeConverter ====================

    @Test
    fun `CompletionTypeConverter round trips Full`() {
        val original = CompletionType.Full
        val stored = completionTypeConverter.completionTypeToString(original)
        val restored = completionTypeConverter.stringToCompletionType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `CompletionTypeConverter round trips Partial`() {
        val original = CompletionType.Partial
        val stored = completionTypeConverter.completionTypeToString(original)
        val restored = completionTypeConverter.stringToCompletionType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `CompletionTypeConverter round trips Skipped`() {
        val original = CompletionType.Skipped
        val stored = completionTypeConverter.completionTypeToString(original)
        val restored = completionTypeConverter.stringToCompletionType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `CompletionTypeConverter round trips Missed`() {
        val original = CompletionType.Missed
        val stored = completionTypeConverter.completionTypeToString(original)
        val restored = completionTypeConverter.stringToCompletionType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `CompletionTypeConverter null input produces null output`() {
        assertNull(completionTypeConverter.completionTypeToString(null))
        assertNull(completionTypeConverter.stringToCompletionType(null))
    }

    @Test
    fun `CompletionTypeConverter blank string produces null`() {
        assertNull(completionTypeConverter.stringToCompletionType(""))
        assertNull(completionTypeConverter.stringToCompletionType("  "))
    }

    @Test
    fun `CompletionTypeConverter unknown string produces null`() {
        assertNull(completionTypeConverter.stringToCompletionType("UnknownValue"))
    }

    // ==================== SkipReasonConverter ====================

    @Test
    fun `SkipReasonConverter round trips TooTired`() {
        val original = SkipReason.TooTired
        val stored = skipReasonConverter.skipReasonToString(original)
        val restored = skipReasonConverter.stringToSkipReason(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SkipReasonConverter round trips NoTime`() {
        val original = SkipReason.NoTime
        val stored = skipReasonConverter.skipReasonToString(original)
        val restored = skipReasonConverter.stringToSkipReason(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SkipReasonConverter round trips NotFeelingWell`() {
        val original = SkipReason.NotFeelingWell
        val stored = skipReasonConverter.skipReasonToString(original)
        val restored = skipReasonConverter.stringToSkipReason(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SkipReasonConverter round trips Traveling`() {
        val original = SkipReason.Traveling
        val stored = skipReasonConverter.skipReasonToString(original)
        val restored = skipReasonConverter.stringToSkipReason(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SkipReasonConverter round trips TookDayOff`() {
        val original = SkipReason.TookDayOff
        val stored = skipReasonConverter.skipReasonToString(original)
        val restored = skipReasonConverter.stringToSkipReason(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SkipReasonConverter round trips Other`() {
        val original = SkipReason.Other
        val stored = skipReasonConverter.skipReasonToString(original)
        val restored = skipReasonConverter.stringToSkipReason(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SkipReasonConverter null input produces null output`() {
        assertNull(skipReasonConverter.skipReasonToString(null))
        assertNull(skipReasonConverter.stringToSkipReason(null))
    }

    @Test
    fun `SkipReasonConverter blank string produces null`() {
        assertNull(skipReasonConverter.stringToSkipReason(""))
        assertNull(skipReasonConverter.stringToSkipReason("  "))
    }

    @Test
    fun `SkipReasonConverter unknown string produces null`() {
        assertNull(skipReasonConverter.stringToSkipReason("UnknownValue"))
    }

    // ==================== RoutineStatusConverter ====================

    @Test
    fun `RoutineStatusConverter round trips Active`() {
        val original = RoutineStatus.Active
        val stored = routineStatusConverter.routineStatusToString(original)
        val restored = routineStatusConverter.stringToRoutineStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RoutineStatusConverter round trips Paused`() {
        val original = RoutineStatus.Paused
        val stored = routineStatusConverter.routineStatusToString(original)
        val restored = routineStatusConverter.stringToRoutineStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RoutineStatusConverter round trips Archived`() {
        val original = RoutineStatus.Archived
        val stored = routineStatusConverter.routineStatusToString(original)
        val restored = routineStatusConverter.stringToRoutineStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RoutineStatusConverter null input produces null output`() {
        assertNull(routineStatusConverter.routineStatusToString(null))
        assertNull(routineStatusConverter.stringToRoutineStatus(null))
    }

    @Test
    fun `RoutineStatusConverter blank string produces null`() {
        assertNull(routineStatusConverter.stringToRoutineStatus(""))
        assertNull(routineStatusConverter.stringToRoutineStatus("  "))
    }

    @Test
    fun `RoutineStatusConverter unknown string produces null`() {
        assertNull(routineStatusConverter.stringToRoutineStatus("UnknownValue"))
    }

    // ==================== ExecutionStatusConverter ====================

    @Test
    fun `ExecutionStatusConverter round trips NotStarted`() {
        val original = ExecutionStatus.NotStarted
        val stored = executionStatusConverter.executionStatusToString(original)
        val restored = executionStatusConverter.stringToExecutionStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `ExecutionStatusConverter round trips InProgress`() {
        val original = ExecutionStatus.InProgress
        val stored = executionStatusConverter.executionStatusToString(original)
        val restored = executionStatusConverter.stringToExecutionStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `ExecutionStatusConverter round trips Paused`() {
        val original = ExecutionStatus.Paused
        val stored = executionStatusConverter.executionStatusToString(original)
        val restored = executionStatusConverter.stringToExecutionStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `ExecutionStatusConverter round trips Completed`() {
        val original = ExecutionStatus.Completed
        val stored = executionStatusConverter.executionStatusToString(original)
        val restored = executionStatusConverter.stringToExecutionStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `ExecutionStatusConverter round trips Abandoned`() {
        val original = ExecutionStatus.Abandoned
        val stored = executionStatusConverter.executionStatusToString(original)
        val restored = executionStatusConverter.stringToExecutionStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `ExecutionStatusConverter null input produces null output`() {
        assertNull(executionStatusConverter.executionStatusToString(null))
        assertNull(executionStatusConverter.stringToExecutionStatus(null))
    }

    @Test
    fun `ExecutionStatusConverter blank string produces null`() {
        assertNull(executionStatusConverter.stringToExecutionStatus(""))
        assertNull(executionStatusConverter.stringToExecutionStatus("  "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ExecutionStatusConverter unknown string throws IllegalArgumentException`() {
        executionStatusConverter.stringToExecutionStatus("UnknownValue")
    }

    // ==================== RecoveryTypeConverter ====================

    @Test
    fun `RecoveryTypeConverter round trips Lapse`() {
        val original = RecoveryType.Lapse
        val stored = recoveryTypeConverter.recoveryTypeToString(original)
        val restored = recoveryTypeConverter.stringToRecoveryType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RecoveryTypeConverter round trips Relapse`() {
        val original = RecoveryType.Relapse
        val stored = recoveryTypeConverter.recoveryTypeToString(original)
        val restored = recoveryTypeConverter.stringToRecoveryType(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RecoveryTypeConverter null input produces null output`() {
        assertNull(recoveryTypeConverter.recoveryTypeToString(null))
        assertNull(recoveryTypeConverter.stringToRecoveryType(null))
    }

    @Test
    fun `RecoveryTypeConverter blank string produces null`() {
        assertNull(recoveryTypeConverter.stringToRecoveryType(""))
        assertNull(recoveryTypeConverter.stringToRecoveryType("  "))
    }

    @Test
    fun `RecoveryTypeConverter unknown string produces null`() {
        assertNull(recoveryTypeConverter.stringToRecoveryType("UnknownValue"))
    }

    // ==================== RecoveryActionConverter ====================

    @Test
    fun `RecoveryActionConverter round trips Resume`() {
        val original = RecoveryAction.Resume
        val stored = recoveryActionConverter.recoveryActionToString(original)
        val restored = recoveryActionConverter.stringToRecoveryAction(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RecoveryActionConverter round trips Simplify`() {
        val original = RecoveryAction.Simplify
        val stored = recoveryActionConverter.recoveryActionToString(original)
        val restored = recoveryActionConverter.stringToRecoveryAction(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RecoveryActionConverter round trips Pause`() {
        val original = RecoveryAction.Pause
        val stored = recoveryActionConverter.recoveryActionToString(original)
        val restored = recoveryActionConverter.stringToRecoveryAction(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RecoveryActionConverter round trips Archive`() {
        val original = RecoveryAction.Archive
        val stored = recoveryActionConverter.recoveryActionToString(original)
        val restored = recoveryActionConverter.stringToRecoveryAction(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RecoveryActionConverter round trips FreshStart`() {
        val original = RecoveryAction.FreshStart
        val stored = recoveryActionConverter.recoveryActionToString(original)
        val restored = recoveryActionConverter.stringToRecoveryAction(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `RecoveryActionConverter null input produces null output`() {
        assertNull(recoveryActionConverter.recoveryActionToString(null))
        assertNull(recoveryActionConverter.stringToRecoveryAction(null))
    }

    @Test
    fun `RecoveryActionConverter blank string produces null`() {
        assertNull(recoveryActionConverter.stringToRecoveryAction(""))
        assertNull(recoveryActionConverter.stringToRecoveryAction("  "))
    }

    @Test
    fun `RecoveryActionConverter unknown string produces null`() {
        assertNull(recoveryActionConverter.stringToRecoveryAction("UnknownValue"))
    }

    // ==================== SessionStatusConverter ====================

    @Test
    fun `SessionStatusConverter round trips Pending`() {
        val original = SessionStatus.Pending
        val stored = sessionStatusConverter.sessionStatusToString(original)
        val restored = sessionStatusConverter.stringToSessionStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SessionStatusConverter round trips Completed`() {
        val original = SessionStatus.Completed
        val stored = sessionStatusConverter.sessionStatusToString(original)
        val restored = sessionStatusConverter.stringToSessionStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SessionStatusConverter round trips Abandoned`() {
        val original = SessionStatus.Abandoned
        val stored = sessionStatusConverter.sessionStatusToString(original)
        val restored = sessionStatusConverter.stringToSessionStatus(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `SessionStatusConverter null input produces null output`() {
        assertNull(sessionStatusConverter.sessionStatusToString(null))
        assertNull(sessionStatusConverter.stringToSessionStatus(null))
    }

    @Test
    fun `SessionStatusConverter blank string produces null`() {
        assertNull(sessionStatusConverter.stringToSessionStatus(""))
        assertNull(sessionStatusConverter.stringToSessionStatus("  "))
    }

    @Test
    fun `SessionStatusConverter unknown string produces null`() {
        assertNull(sessionStatusConverter.stringToSessionStatus("UnknownValue"))
    }

    // ==================== ThemeConverter ====================

    @Test
    fun `ThemeConverter round trips System`() {
        val original = Theme.System
        val stored = themeConverter.themeToString(original)
        val restored = themeConverter.stringToTheme(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `ThemeConverter round trips Light`() {
        val original = Theme.Light
        val stored = themeConverter.themeToString(original)
        val restored = themeConverter.stringToTheme(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `ThemeConverter round trips Dark`() {
        val original = Theme.Dark
        val stored = themeConverter.themeToString(original)
        val restored = themeConverter.stringToTheme(stored)
        assertEquals(original, restored)
    }

    @Test
    fun `ThemeConverter null input produces null output`() {
        assertNull(themeConverter.themeToString(null))
        assertNull(themeConverter.stringToTheme(null))
    }

    @Test
    fun `ThemeConverter blank string produces null`() {
        assertNull(themeConverter.stringToTheme(""))
        assertNull(themeConverter.stringToTheme("  "))
    }

    @Test
    fun `ThemeConverter unknown string produces null`() {
        assertNull(themeConverter.stringToTheme("UnknownValue"))
    }
}
