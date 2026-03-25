package com.getaltair.kairos.data.converter

import androidx.test.ext.junit.runners.AndroidJUnit4
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for all type converters.
 * Verifies round-trip conversion and edge cases.
 */
@RunWith(AndroidJUnit4::class.java)
class ConvertersTest {

    private lateinit var instantConverter: InstantConverter
    private lateinit var localDateConverter: LocalDateConverter
    private lateinit var localTimeConverter: LocalTimeConverter
    private lateinit var dayOfWeekListConverter: DayOfWeekListConverter
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
    private lateinit var sessionStatusConverter: SessionStatusConverter
    private lateinit var recoveryActionConverter: RecoveryActionConverter
    private lateinit var themeConverter: ThemeConverter
    private lateinit var blockerConverter: BlockerConverter

    @Before
    fun setup() {
        instantConverter = InstantConverter()
        localDateConverter = LocalDateConverter()
        localTimeConverter = LocalTimeConverter()
        dayOfWeekListConverter = DayOfWeekListConverter()
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
        sessionStatusConverter = SessionStatusConverter()
        recoveryActionConverter = RecoveryActionConverter()
        themeConverter = ThemeConverter()
        blockerConverter = BlockerConverter()
    }

    @After
    fun tearDown() {
        // Clean up resources if needed
    }

    // ==================== Instant Converter Tests ====================

    @Test
    fun instantConverter_roundTrip() {
        val original = Instant.now()
        val timestamp = instantConverter.instantToLong(original)
        val result = instantConverter.longToInstant(timestamp)

        assertEquals(original.toEpochMilli(), result?.toEpochMilli())
    }

    @Test
    fun instantConverter_nullInput() {
        val result = instantConverter.longToInstant(null)
        assertNull(result)
    }

    @Test
    fun instantConverter_nullOutput() {
        val result = instantConverter.instantToLong(null)
        assertNull(result)
    }

    // ==================== LocalDate Converter Tests ====================

    @Test
    fun localDateConverter_roundTrip() {
        val original = LocalDate.of(2024, 3, 25)
        val dateString = localDateConverter.localDateToString(original)
        val result = localDateConverter.stringToLocalDate(dateString)

        assertEquals(original, result)
    }

    @Test
    fun localDateConverter_nullInput() {
        val result = localDateConverter.stringToLocalDate(null)
        assertNull(result)
    }

    @Test
    fun localDateConverter_nullOutput() {
        val result = localDateConverter.localDateToString(null)
        assertNull(result)
    }

    // ==================== LocalTime Converter Tests ====================

    @Test
    fun localTimeConverter_roundTrip() {
        val original = LocalTime.of(14, 30)
        val timeString = localTimeConverter.localTimeToString(original)
        val result = localTimeConverter.stringToLocalTime(timeString)

        assertEquals(original, result)
    }

    @Test
    fun localTimeConverter_nullInput() {
        val result = localTimeConverter.stringToLocalTime(null)
        assertNull(result)
    }

    @Test
    fun localTimeConverter_nullOutput() {
        val result = localTimeConverter.localTimeToString(null)
        assertNull(result)
    }

    // ==================== DayOfWeekList Converter Tests ====================

    @Test
    fun dayOfWeekListConverter_roundTrip() {
        val original = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY)
        val string = dayOfWeekListConverter.dayOfWeekSetToString(original)
        val result = dayOfWeekListConverter.stringToDayOfWeekSet(string)

        assertEquals(original, result)
    }

    @Test
    fun dayOfWeekListConverter_nullInput() {
        val result = dayOfWeekListConverter.stringToDayOfWeekSet(null)
        assertNull(result)
    }

    @Test
    fun dayOfWeekListConverter_emptySet() {
        val result = dayOfWeekListConverter.dayOfWeekSetToString(emptySet())
        assertNull(result)
    }

    // ==================== AnchorType Converter Tests ====================

    @Test
    fun anchorTypeConverter_roundTrip() {
        val original = AnchorType.AfterBehavior
        val string = anchorTypeConverter.anchorTypeToString(original)
        val result = anchorTypeConverter.stringToAnchorType(string)

        assertEquals(original, result)
    }

    @Test
    fun anchorTypeConverter_allTypes() {
        assertEquals(AnchorType.AfterBehavior, anchorTypeConverter.stringToAnchorType("AfterBehavior"))
        assertEquals(AnchorType.BeforeBehavior, anchorTypeConverter.stringToAnchorType("BeforeBehavior"))
        assertEquals(AnchorType.AtLocation, anchorTypeConverter.stringToAnchorType("AtLocation"))
        assertEquals(AnchorType.AtTime, anchorTypeConverter.stringToAnchorType("AtTime"))
    }

    @Test
    fun anchorTypeConverter_nullInput() {
        val result = anchorTypeConverter.stringToAnchorType(null)
        assertNull(result)
    }

    @Test
    fun anchorTypeConverter_nullOutput() {
        val result = anchorTypeConverter.anchorTypeToString(null)
        assertNull(result)
    }

    // ==================== HabitCategory Converter Tests ====================

    @Test
    fun habitCategoryConverter_roundTrip() {
        val original = HabitCategory.Morning
        val string = habitCategoryConverter.habitCategoryToString(original)
        val result = habitCategoryConverter.stringToHabitCategory(string)

        assertEquals(original, result)
    }

    @Test
    fun habitCategoryConverter_allTypes() {
        assertEquals(HabitCategory.Morning, habitCategoryConverter.stringToHabitCategory("Morning"))
        assertEquals(HabitCategory.Afternoon, habitCategoryConverter.stringToHabitCategory("Afternoon"))
        assertEquals(HabitCategory.Evening, habitCategoryConverter.stringToHabitCategory("Evening"))
        assertEquals(HabitCategory.Anytime, habitCategoryConverter.stringToHabitCategory("Anytime"))
        assertEquals(HabitCategory.Departure, habitCategoryConverter.stringToHabitCategory("Departure"))
    }

    @Test
    fun habitCategoryConverter_nullInput() {
        val result = habitCategoryConverter.stringToHabitCategory(null)
        assertNull(result)
    }

    @Test
    fun habitCategoryConverter_nullOutput() {
        val result = habitCategoryConverter.habitCategoryToString(null)
        assertNull(result)
    }

    // ==================== HabitFrequency Converter Tests ====================

    @Test
    fun habitFrequencyConverter_roundTrip() {
        val original = HabitFrequency.Daily
        val string = habitFrequencyConverter.habitFrequencyToString(original)
        val result = habitFrequencyConverter.stringToHabitFrequency(string)

        assertEquals(original, result)
    }

    @Test
    fun habitFrequencyConverter_allTypes() {
        assertEquals(HabitFrequency.Daily, habitFrequencyConverter.stringToHabitFrequency("Daily"))
        assertEquals(HabitFrequency.Weekdays, habitFrequencyConverter.stringToHabitFrequency("Weekdays"))
        assertEquals(HabitFrequency.Weekends, habitFrequencyConverter.stringToHabitFrequency("Weekends"))
        assertEquals(HabitFrequency.Custom, habitFrequencyConverter.stringToHabitFrequency("Custom"))
    }

    @Test
    fun habitFrequencyConverter_nullInput() {
        val result = habitFrequencyConverter.stringToHabitFrequency(null)
        assertNull(result)
    }

    @Test
    fun habitFrequencyConverter_nullOutput() {
        val result = habitFrequencyConverter.habitFrequencyToString(null)
        assertNull(result)
    }

    // ==================== HabitPhase Converter Tests ====================

    @Test
    fun habitPhaseConverter_roundTrip() {
        val original = HabitPhase.ONBOARDING
        val string = habitPhaseConverter.habitPhaseToString(original)
        val result = habitPhaseConverter.stringToHabitPhase(string)

        assertEquals(original, result)
    }

    @Test
    fun habitPhaseConverter_allTypes() {
        assertEquals(HabitPhase.ONBOARD, habitPhaseConverter.stringToHabitPhase("ONBOARD"))
        assertEquals(HabitPhase.FORMING, habitPhaseConverter.stringToHabitPhase("FORMING"))
        assertEquals(HabitPhase.MAINTAINING, habitPhaseConverter.stringToHabitPhase("MAINTAINING"))
        assertEquals(HabitPhase.LAPSED, habitPhaseConverter.stringToHabitPhase("LAPSED"))
        assertEquals(HabitPhase.RELAPSED, habitPhaseConverter.stringToHabitPhase("RELAPSED"))
    }

    @Test
    fun habitPhaseConverter_nullInput() {
        val result = habitPhaseConverter.stringToHabitPhase(null)
        assertNull(result)
    }

    @Test
    fun habitPhaseConverter_nullOutput() {
        val result = habitPhaseConverter.habitPhaseToString(null)
        assertNull(result)
    }

    // ==================== HabitStatus Converter Tests ====================

    @Test
    fun habitStatusConverter_roundTrip() {
        val original = HabitStatus.Active
        val string = habitStatusConverter.habitStatusToString(original)
        val result = habitStatusConverter.stringToHabitStatus(string)

        assertEquals(original, result)
    }

    @Test
    fun habitStatusConverter_allTypes() {
        assertEquals(HabitStatus.Active, habitStatusConverter.stringToHabitStatus("Active"))
        assertEquals(HabitStatus.Paused, habitStatusConverter.stringToHabitStatus("Paused"))
        assertEquals(HabitStatus.Archived, habitStatusConverter.stringToHabitStatus("Archived"))
    }

    @Test
    fun habitStatusConverter_nullInput() {
        val result = habitStatusConverter.stringToHabitStatus(null)
        assertNull(result)
    }

    @Test
    fun habitStatus_converter_nullOutput() {
        val result = habitStatusConverter.habitStatusToString(null)
        assertNull(result)
    }

    // ==================== CompletionType Converter Tests ====================

    @Test
    fun completionTypeConverter_roundTrip() {
        val original = CompletionType.Full
        val string = completionTypeConverter.completionTypeToString(original)
        val result = completionTypeConverter.stringToCompletionType(string)

        assertEquals(original, result)
    }

    @Test
    fun completionTypeConverter_allTypes() {
        assertEquals(CompletionType.Full, completionTypeConverter.stringToCompletionType("Full"))
        assertEquals(CompletionType.Partial, completionTypeConverter.stringToCompletionType("Partial"))
        assertEquals(CompletionType.Skipped, completionTypeConverter.stringToCompletionType("Skipped"))
        assertEquals(CompletionType.Missed, completionTypeConverter.stringToCompletionType("Missed"))
    }

    @Test
    fun completionTypeConverter_nullInput() {
        val result = completionTypeConverter.stringToCompletionType(null)
        assertNull(result)
    }

    @Test
    fun completionTypeConverter_nullOutput() {
        val result = completionTypeConverter.completionTypeToString(null)
        assertNull(result)
    }

    // ==================== SkipReason Converter Tests ====================

    @Test
    fun skipReasonConverter_roundTrip() {
        val original = SkipReason.TooTired
        val string = skipReasonConverter.skipReasonToString(original)
        val result = skipReasonConverter.stringToSkipReason(string)

        assertEquals(original, result)
    }

    @Test
    fun skipReasonConverter_allTypes() {
        assertEquals(SkipReason.TooTired, skipReasonConverter.stringToSkipReason("TooTired"))
        assertEquals(SkipReason.NoTime, skipReasonConverter.stringToSkipReason("NoTime"))
        assertEquals(SkipReason.NotFeelingWell, skipReasonConverter.stringToSkipReason("NotFeelingWell"))
        assertEquals(SkipReason.Traveling, skipReasonConverter.stringToSkipReason("Traveling"))
        assertEquals(SkipReason.TookDayOff, skipReasonConverter.stringToSkipReason("TookDayOff"))
        assertEquals(SkipReason.Other, skipReasonConverter.stringToSkipReason("Other"))
    }

    @Test
    fun skipReasonConverter_nullInput() {
        val result = skipReasonConverter.stringToSkipReason(null)
        assertNull(result)
    }

    @Test
    fun skipReasonConverter_nullOutput() {
        val result = skipReasonConverter.skipReasonToString(null)
        assertNull(result)
    }

    // ==================== RoutineStatus Converter Tests ====================

    @Test
    fun routineStatusConverter_roundTrip() {
        val original = RoutineStatus.Active
        val string = routineStatusConverter.routineStatusToString(original)
        val result = routineStatusConverter.stringToRoutineStatus(string)

        assertEquals(original, result)
    }

    @Test
    fun routineStatusConverter_allTypes() {
        assertEquals(RoutineStatus.Active, routineStatusConverter.stringToRoutineStatus("Active"))
        assertEquals(RoutineStatus.Paused, routineStatusConverter.stringToRoutineStatus("Paused"))
        assertEquals(RoutineStatus.Archived, routineStatusConverter.stringToRoutineStatus("Archived"))
    }

    @Test
    fun routineStatusConverter_nullInput() {
        val result = routineStatusConverter.stringToRoutineStatus(null)
        assertNull(result)
    }

    @Test
    fun routineStatusConverter_nullOutput() {
        val result = routineStatusConverter.routineStatusToString(null)
        assertNull(result)
    }

    // ==================== ExecutionStatus Converter Tests ====================

    @Test
    fun executionStatusConverter_roundTrip() {
        val original = ExecutionStatus.InProgress
        val string = executionStatusConverter.executionStatusToString(original)
        val result = executionStatusConverter.stringToExecutionStatus(string)

        assertEquals(original, result)
    }

    @Test
    fun executionStatusConverter_allTypes() {
        assertEquals(ExecutionStatus.NotStarted, executionStatusConverter.stringToExecutionStatus("NotStarted"))
        assertEquals(ExecutionStatus.InProgress, executionStatusConverter.stringToExecutionStatus("InProgress"))
        assertEquals(ExecutionStatus.Paused, executionStatusConverter.stringToExecutionStatus("Paused"))
        assertEquals(ExecutionStatus.Completed, executionStatusConverter.stringToExecutionStatus("Completed"))
        assertEquals(ExecutionStatus.Abandoned, executionStatusConverter.stringToExecutionStatus("Abandoned"))
    }

    @Test
    fun executionStatusConverter_nullInput() {
        val result = executionStatusConverter.stringToExecutionStatus(null)
        assertNull(result)
    }

    @Test
    fun executionStatusConverter_nullOutput() {
        val result = executionStatusConverter.executionStatusToString(null)
        assertNull(result)
    }

    // ==================== RecoveryType Converter Tests ====================

    @Test
    fun recoveryTypeConverter_roundTrip() {
        val original = RecoveryType.Lapse
        val string = recoveryTypeConverter.recoveryTypeToString(original)
        val result = recoveryTypeConverter.stringToRecoveryType(string)

        assertEquals(original, result)
    }

    @Test
    fun recoveryTypeConverter_allTypes() {
        assertEquals(RecoveryType.Lapse, recoveryTypeConverter.stringToRecoveryType("Lapse"))
        assertEquals(RecoveryType.Relapse, recoveryTypeConverter.stringToRecoveryType("Relapse"))
    }

    @Test
    fun recoveryTypeConverter_nullInput() {
        val result = recoveryTypeConverter.stringToRecoveryType(null)
        assertNull(result)
    }

    @Test
    fun recoveryTypeConverter_nullOutput() {
        val result = recoveryTypeConverter.recoveryTypeToString(null)
        assertNull(result)
    }

    // ==================== SessionStatus Converter Tests ====================

    @Test
    fun sessionStatusConverter_roundTrip() {
        val original = SessionStatus.Pending
        val string = sessionStatusConverter.sessionStatusToString(original)
        val result = sessionStatusConverter.stringToSessionStatus(string)

        assertEquals(original, result)
    }

    @Test
    fun sessionStatusConverter_allTypes() {
        assertEquals(SessionStatus.Pending, sessionStatusConverter.stringToSessionStatus("Pending"))
        assertEquals(SessionStatus.Completed, sessionStatusConverter.stringToSessionStatus("Completed"))
        assertEquals(SessionStatus.Abandoned, sessionStatusConverter.stringToSessionStatus("Abandoned"))
    }

    @Test
    fun sessionStatus_converter_nullInput() {
        val result = sessionStatusConverter.stringToSessionStatus(null)
        assertNull(result)
    }

    @Test
    fun sessionStatus_converter_nullOutput() {
        val result = sessionStatusConverter.sessionStatusToString(null)
        assertNull(result)
    }

    // ==================== RecoveryAction Converter Tests ====================

    @Test
    fun recoveryActionConverter_roundTrip() {
        val original = RecoveryAction.Resume
        val string = recoveryActionConverter.recoveryActionToString(original)
        val result = recoveryActionConverter.stringToRecoveryAction(string)

        assertEquals(original, result)
    }

    @Test
    fun recoveryActionConverter_allTypes() {
        assertEquals(RecoveryAction.Resume, recoveryActionConverter.stringToRecoveryAction("Resume"))
        assertEquals(RecoveryAction.Simplify, recoveryActionConverter.stringToRecoveryAction("Simplify"))
        assertEquals(RecoveryAction.Pause, recoveryActionConverter.stringToRecoveryAction("Pause"))
        assertEquals(RecoveryAction.Archive, recoveryActionConverter.stringToRecoveryAction("Archive"))
        assertEquals(RecoveryAction.FreshStart, recoveryActionConverter.stringToRecoveryAction("FreshStart"))
    }

    @Test
    fun recoveryActionConverter_nullInput() {
        val result = recoveryActionConverter.stringToRecoveryAction(null)
        assertNull(result)
    }

    @Test
    fun recoveryActionConverter_nullOutput() {
        val result = recoveryActionConverter.recoveryActionToString(null)
        assertNull(result)
    }

    // ==================== Theme Converter Tests ====================

    @Test
    fun themeConverter_roundTrip() {
        val original = Theme.Dark
        val string = themeConverter.themeToString(original)
        val result = themeConverter.stringToTheme(string)

        assertEquals(original, result)
    }

    @Test
    fun themeConverter_allTypes() {
        assertEquals(Theme.System, themeConverter.stringToTheme("System"))
        assertEquals(Theme.Light, themeConverter.stringToTheme("Light"))
        assertEquals(Theme.Dark, themeConverter.stringToTheme("Dark"))
    }

    @Test
    fun themeConverter_nullInput() {
        val result = themeConverter.stringToTheme(null)
        assertNull(result)
    }

    @Test
    fun themeConverter_nullOutput() {
        val result = themeConverter.themeToString(null)
        assertNull(result)
    }

    // ==================== BlockerConverter Tests ====================

    @Test
    fun blockerConverter_roundTrip() {
        val original = listOf(Blocker.NoEnergy, Blocker.TooBusy)
        val string = blockerConverter.blockerListToString(original)
        val result = blockerConverter.stringToBlockerList(string)

        assertEquals(original, result)
    }

    @Test
    fun blockerConverter_allTypes() {
        assertEquals(Blocker.NoEnergy, blockerConverter.stringToBlockerList("[\"NoEnergy\"]"))
        assertEquals(Blocker.PainPhysical, blockerConverter.stringToBlockerList("[\"PainPhysical\"]"))
        assertEquals(Blocker.PainMental, blockerConverter.stringToBlockerList("[\"PainMental\"]"))
        assertEquals(Blocker.TooBusy, blockerConverter.stringToBlockerList("[\"TooBusy\"]"))
        assertEquals(Blocker.FamilyEmergency, blockerConverter.stringToBlockerList("[\"FamilyEmergency\"]"))
        assertEquals(Blocker.WorkEmergency, blockerConverter.stringToBlockerList("[\"WorkEmergency\"]"))
        assertEquals(Blocker.Sick, blockerConverter.stringToBlockerList("[\"Sick\"]"))
        assertEquals(Blocker.Weather, blockerConverter.stringToBlockerList("[\"Weather\"]"))
        assertEquals(Blocker.EquipmentFailure, blockerConverter.stringToBlockerList("[\"EquipmentFailure\"]"))
        assertEquals(Blocker.Other, blockerConverter.stringToBlockerList("[\"Other\"]"))
    }

    @Test
    fun blockerConverter_nullInput() {
        val result = blockerConverter.stringToBlockerList(null)
        assertNull(result)
    }

    @Test
    fun blockerConverter_emptyInput() {
        val result = blockerConverter.stringToBlockerList(emptyList())
        assertNull(result)
    }

    @Test
    fun blockerConverter_emptyJson() {
        val result = blockerConverter.stringToBlockerList("[]")
        assertNull(result)
    }

    @Test
    fun blockerConverter_invalidJson() {
        val result = blockerConverter.stringToBlockerList("invalid json")
        assertNull(result)
    }

    @Test
    fun blockerConverter_nullOutput() {
        val result = blockerConverter.blockerListToString(null)
        assertNull(result)
    }
}
