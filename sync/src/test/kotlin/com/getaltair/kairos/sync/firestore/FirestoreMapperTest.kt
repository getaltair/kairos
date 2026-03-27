package com.getaltair.kairos.sync.firestore

import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.entity.RoutineHabit
import com.getaltair.kairos.domain.entity.RoutineVariant
import com.getaltair.kairos.domain.entity.UserPreferences
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
import com.google.firebase.Timestamp
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

class FirestoreMapperTest {

    // Fixed instants for deterministic tests (truncated to seconds for
    // Timestamp round-trip fidelity).
    private val now: Instant = Instant.ofEpochSecond(1_700_000_000L)
    private val later: Instant = Instant.ofEpochSecond(1_700_001_000L)

    // -----------------------------------------------------------------------
    // Timestamp helpers
    // -----------------------------------------------------------------------

    @Test
    fun `Instant toTimestamp round-trips correctly`() {
        val instant = Instant.ofEpochSecond(1_700_000_000L, 123_456_789L)
        val ts = instant.toTimestamp()
        val result = ts.toInstant()
        assertEquals(instant, result)
    }

    // -----------------------------------------------------------------------
    // Enum tag serialization
    // -----------------------------------------------------------------------

    @Test
    fun `AnchorType tags round-trip`() {
        val types = listOf(
            AnchorType.AfterBehavior,
            AnchorType.BeforeBehavior,
            AnchorType.AtLocation,
            AnchorType.AtTime,
        )
        types.forEach { type ->
            assertEquals(type, anchorTypeFromTag(type.toTag()))
        }
    }

    @Test
    fun `HabitCategory tags round-trip`() {
        val categories = listOf(
            HabitCategory.Morning,
            HabitCategory.Afternoon,
            HabitCategory.Evening,
            HabitCategory.Anytime,
            HabitCategory.Departure,
        )
        categories.forEach { cat ->
            assertEquals(cat, habitCategoryFromTag(cat.toTag()))
        }
    }

    @Test
    fun `HabitFrequency tags round-trip`() {
        val freqs = listOf(
            HabitFrequency.Daily,
            HabitFrequency.Weekdays,
            HabitFrequency.Weekends,
            HabitFrequency.Custom,
        )
        freqs.forEach { freq ->
            assertEquals(freq, habitFrequencyFromTag(freq.toTag()))
        }
    }

    @Test
    fun `HabitPhase tags round-trip`() {
        val phases = listOf(
            HabitPhase.ONBOARD,
            HabitPhase.FORMING,
            HabitPhase.MAINTAINING,
            HabitPhase.LAPSED,
            HabitPhase.RELAPSED,
        )
        phases.forEach { phase ->
            assertEquals(phase, habitPhaseFromTag(phase.toTag()))
        }
    }

    @Test
    fun `HabitStatus tags round-trip`() {
        val statuses = listOf(
            HabitStatus.Active,
            HabitStatus.Paused,
            HabitStatus.Archived,
        )
        statuses.forEach { status ->
            assertEquals(status, habitStatusFromTag(status.toTag()))
        }
    }

    @Test
    fun `CompletionType tags round-trip`() {
        val types = listOf(
            CompletionType.Full,
            CompletionType.Partial,
            CompletionType.Skipped,
            CompletionType.Missed,
        )
        types.forEach { type ->
            assertEquals(type, completionTypeFromTag(type.toTag()))
        }
    }

    @Test
    fun `SkipReason tags round-trip`() {
        val reasons = listOf(
            SkipReason.TooTired,
            SkipReason.NoTime,
            SkipReason.NotFeelingWell,
            SkipReason.Traveling,
            SkipReason.TookDayOff,
            SkipReason.Other,
        )
        reasons.forEach { reason ->
            assertEquals(reason, skipReasonFromTag(reason.toTag()))
        }
    }

    @Test
    fun `RecoveryType tags round-trip`() {
        val types = listOf(RecoveryType.Lapse, RecoveryType.Relapse)
        types.forEach { type ->
            assertEquals(type, recoveryTypeFromTag(type.toTag()))
        }
    }

    @Test
    fun `SessionStatus tags round-trip`() {
        val statuses = listOf(
            SessionStatus.Pending,
            SessionStatus.Completed,
            SessionStatus.Abandoned,
        )
        statuses.forEach { status ->
            assertEquals(status, sessionStatusFromTag(status.toTag()))
        }
    }

    @Test
    fun `RecoveryAction tags round-trip`() {
        val actions = listOf(
            RecoveryAction.Resume,
            RecoveryAction.Simplify,
            RecoveryAction.Pause,
            RecoveryAction.Archive,
            RecoveryAction.FreshStart,
        )
        actions.forEach { action ->
            assertEquals(action, recoveryActionFromTag(action.toTag()))
        }
    }

    @Test
    fun `Blocker tags round-trip`() {
        val blockers = listOf(
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
        blockers.forEach { blocker ->
            assertEquals(blocker, blockerFromTag(blocker.toTag()))
        }
    }

    @Test
    fun `RoutineStatus tags round-trip`() {
        val statuses = listOf(
            RoutineStatus.Active,
            RoutineStatus.Paused,
            RoutineStatus.Archived,
        )
        statuses.forEach { status ->
            assertEquals(status, routineStatusFromTag(status.toTag()))
        }
    }

    @Test
    fun `ExecutionStatus tags round-trip`() {
        val statuses = listOf(
            ExecutionStatus.NotStarted,
            ExecutionStatus.InProgress,
            ExecutionStatus.Paused,
            ExecutionStatus.Completed,
            ExecutionStatus.Abandoned,
        )
        statuses.forEach { status ->
            assertEquals(status, executionStatusFromTag(status.toTag()))
        }
    }

    @Test
    fun `Theme tags round-trip`() {
        val themes = listOf(Theme.System, Theme.Light, Theme.Dark)
        themes.forEach { theme ->
            assertEquals(theme, themeFromTag(theme.toTag()))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown AnchorType tag throws`() {
        anchorTypeFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown CompletionType tag throws`() {
        completionTypeFromTag("INVALID")
    }

    // -----------------------------------------------------------------------
    // Habit round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `Habit round-trips through Firestore map`() {
        val id = UUID.randomUUID()
        val habit = Habit(
            id = id,
            name = "Meditate",
            description = "10 min morning meditation",
            icon = "brain",
            color = "#FF5733",
            anchorBehavior = "After brushing teeth",
            anchorType = AnchorType.AfterBehavior,
            timeWindowStart = "07:00",
            timeWindowEnd = "08:00",
            category = HabitCategory.Morning,
            frequency = HabitFrequency.Custom,
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            estimatedSeconds = 600,
            microVersion = "5 deep breaths",
            allowPartialCompletion = true,
            subtasks = listOf("Sit down", "Set timer", "Focus"),
            phase = HabitPhase.FORMING,
            status = HabitStatus.Active,
            createdAt = now,
            updatedAt = later,
            pausedAt = null,
            archivedAt = null,
            lapseThresholdDays = 3,
            relapseThresholdDays = 7,
        )

        val map = habit.toFirestoreMap()
        val restored = FirestoreMapper.habitFromMap(map)

        assertEquals(habit.id, restored.id)
        assertEquals(habit.name, restored.name)
        assertEquals(habit.description, restored.description)
        assertEquals(habit.icon, restored.icon)
        assertEquals(habit.color, restored.color)
        assertEquals(habit.anchorBehavior, restored.anchorBehavior)
        assertEquals(habit.anchorType, restored.anchorType)
        assertEquals(habit.timeWindowStart, restored.timeWindowStart)
        assertEquals(habit.timeWindowEnd, restored.timeWindowEnd)
        assertEquals(habit.category, restored.category)
        assertEquals(habit.frequency, restored.frequency)
        assertEquals(habit.activeDays, restored.activeDays)
        assertEquals(habit.estimatedSeconds, restored.estimatedSeconds)
        assertEquals(habit.microVersion, restored.microVersion)
        assertEquals(habit.allowPartialCompletion, restored.allowPartialCompletion)
        assertEquals(habit.subtasks, restored.subtasks)
        assertEquals(habit.phase, restored.phase)
        assertEquals(habit.status, restored.status)
        assertEquals(habit.createdAt, restored.createdAt)
        assertEquals(habit.updatedAt, restored.updatedAt)
        assertNull(restored.pausedAt)
        assertNull(restored.archivedAt)
        assertEquals(habit.lapseThresholdDays, restored.lapseThresholdDays)
        assertEquals(habit.relapseThresholdDays, restored.relapseThresholdDays)
    }

    @Test
    fun `Habit with nullable fields set round-trips`() {
        val habit = Habit(
            id = UUID.randomUUID(),
            name = "Exercise",
            anchorBehavior = "After waking up",
            anchorType = AnchorType.AtTime,
            timeWindowStart = "06:00",
            timeWindowEnd = "07:00",
            category = HabitCategory.Morning,
            frequency = HabitFrequency.Daily,
            phase = HabitPhase.MAINTAINING,
            status = HabitStatus.Paused,
            createdAt = now,
            updatedAt = later,
            pausedAt = later,
            archivedAt = null,
        )

        val map = habit.toFirestoreMap()
        val restored = FirestoreMapper.habitFromMap(map)

        assertNotNull(restored.pausedAt)
        assertEquals(later, restored.pausedAt)
    }

    @Test
    fun `Habit map contains version field`() {
        val habit = Habit(
            id = UUID.randomUUID(),
            name = "Read",
            anchorBehavior = "Before bed",
            anchorType = AnchorType.BeforeBehavior,
            category = HabitCategory.Evening,
            frequency = HabitFrequency.Daily,
            createdAt = now,
            updatedAt = now,
        )

        val map = habit.toFirestoreMap()
        assertNotNull(map["version"])
        assert(map["version"] is Long)
    }

    @Test
    fun `Habit with null activeDays serializes correctly`() {
        val habit = Habit(
            id = UUID.randomUUID(),
            name = "Walk",
            anchorBehavior = "After lunch",
            anchorType = AnchorType.AfterBehavior,
            category = HabitCategory.Afternoon,
            frequency = HabitFrequency.Daily,
            activeDays = null,
            createdAt = now,
            updatedAt = now,
        )

        val map = habit.toFirestoreMap()
        assertNull(map["activeDays"])

        val restored = FirestoreMapper.habitFromMap(map)
        assertNull(restored.activeDays)
    }

    @Test
    fun `Habit with null timeWindow serializes correctly`() {
        val habit = Habit(
            id = UUID.randomUUID(),
            name = "Stretch",
            anchorBehavior = "After sitting",
            anchorType = AnchorType.AfterBehavior,
            category = HabitCategory.Anytime,
            frequency = HabitFrequency.Daily,
            timeWindowStart = null,
            timeWindowEnd = null,
            createdAt = now,
            updatedAt = now,
        )

        val map = habit.toFirestoreMap()
        assertNull(map["timeWindow"])

        val restored = FirestoreMapper.habitFromMap(map)
        assertNull(restored.timeWindowStart)
        assertNull(restored.timeWindowEnd)
    }

    // -----------------------------------------------------------------------
    // Completion round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `Completion FULL round-trips through Firestore map`() {
        val completion = Completion(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = LocalDate.of(2025, 3, 15),
            completedAt = now,
            type = CompletionType.Full,
            partialPercent = null,
            skipReason = null,
            energyLevel = 4,
            note = "Felt great",
            createdAt = now,
            updatedAt = later,
        )

        val map = completion.toFirestoreMap()
        val restored = FirestoreMapper.completionFromMap(map)

        assertEquals(completion.id, restored.id)
        assertEquals(completion.habitId, restored.habitId)
        assertEquals(completion.date, restored.date)
        assertEquals(completion.completedAt, restored.completedAt)
        assertEquals(completion.type, restored.type)
        assertNull(restored.partialPercent)
        assertNull(restored.skipReason)
        assertEquals(completion.energyLevel, restored.energyLevel)
        assertEquals(completion.note, restored.note)
        assertEquals(completion.createdAt, restored.createdAt)
        assertEquals(completion.updatedAt, restored.updatedAt)
    }

    @Test
    fun `Completion PARTIAL round-trips with partialPercent`() {
        val completion = Completion(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = LocalDate.of(2025, 3, 15),
            completedAt = now,
            type = CompletionType.Partial,
            partialPercent = 50,
            createdAt = now,
        )

        val map = completion.toFirestoreMap()
        val restored = FirestoreMapper.completionFromMap(map)

        assertEquals(CompletionType.Partial, restored.type)
        assertEquals(50, restored.partialPercent)
    }

    @Test
    fun `Completion SKIPPED round-trips with skipReason`() {
        val completion = Completion(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = LocalDate.of(2025, 3, 15),
            completedAt = now,
            type = CompletionType.Skipped,
            skipReason = SkipReason.TooTired,
            createdAt = now,
        )

        val map = completion.toFirestoreMap()
        val restored = FirestoreMapper.completionFromMap(map)

        assertEquals(CompletionType.Skipped, restored.type)
        assertEquals(SkipReason.TooTired, restored.skipReason)
    }

    @Test
    fun `Completion with null optional fields round-trips`() {
        val completion = Completion(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = LocalDate.of(2025, 6, 1),
            completedAt = now,
            type = CompletionType.Full,
            energyLevel = null,
            note = null,
            createdAt = now,
        )

        val map = completion.toFirestoreMap()
        val restored = FirestoreMapper.completionFromMap(map)

        assertNull(restored.energyLevel)
        assertNull(restored.note)
    }

    // -----------------------------------------------------------------------
    // Routine round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `Routine round-trips through Firestore map`() {
        val routine = Routine(
            id = UUID.randomUUID(),
            name = "Morning Routine",
            description = "Wake-up sequence",
            icon = "sun",
            color = "#FFD700",
            category = HabitCategory.Morning,
            status = RoutineStatus.Active,
            createdAt = now,
            updatedAt = later,
        )

        val map = routine.toFirestoreMap()
        val restored = FirestoreMapper.routineFromMap(map)

        assertEquals(routine.id, restored.id)
        assertEquals(routine.name, restored.name)
        assertEquals(routine.description, restored.description)
        assertEquals(routine.icon, restored.icon)
        assertEquals(routine.color, restored.color)
        assertEquals(routine.category, restored.category)
        assertEquals(routine.status, restored.status)
        assertEquals(routine.createdAt, restored.createdAt)
        assertEquals(routine.updatedAt, restored.updatedAt)
    }

    @Test
    fun `Routine with null optional fields round-trips`() {
        val routine = Routine(
            id = UUID.randomUUID(),
            name = "Quick",
            description = null,
            icon = null,
            color = null,
            category = HabitCategory.Anytime,
            status = RoutineStatus.Archived,
            createdAt = now,
            updatedAt = now,
        )

        val map = routine.toFirestoreMap()
        val restored = FirestoreMapper.routineFromMap(map)

        assertNull(restored.description)
        assertNull(restored.icon)
        assertNull(restored.color)
    }

    // -----------------------------------------------------------------------
    // RoutineHabit round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `RoutineHabit round-trips through Firestore map`() {
        val variantId1 = UUID.randomUUID()
        val variantId2 = UUID.randomUUID()
        val rh = RoutineHabit(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            orderIndex = 2,
            overrideDurationSeconds = 120,
            variantIds = listOf(variantId1, variantId2),
            createdAt = now,
            updatedAt = later,
        )

        val map = rh.toFirestoreMap()
        val restored = FirestoreMapper.routineHabitFromMap(map)

        assertEquals(rh.id, restored.id)
        assertEquals(rh.routineId, restored.routineId)
        assertEquals(rh.habitId, restored.habitId)
        assertEquals(rh.orderIndex, restored.orderIndex)
        assertEquals(rh.overrideDurationSeconds, restored.overrideDurationSeconds)
        assertEquals(rh.variantIds, restored.variantIds)
        assertEquals(rh.createdAt, restored.createdAt)
        assertEquals(rh.updatedAt, restored.updatedAt)
    }

    @Test
    fun `RoutineHabit with null optional fields round-trips`() {
        val rh = RoutineHabit(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            orderIndex = 0,
            overrideDurationSeconds = null,
            variantIds = null,
        )

        val map = rh.toFirestoreMap()
        val restored = FirestoreMapper.routineHabitFromMap(map)

        assertNull(restored.overrideDurationSeconds)
        assertNull(restored.variantIds)
    }

    // -----------------------------------------------------------------------
    // RoutineVariant round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `RoutineVariant round-trips through Firestore map`() {
        val variant = RoutineVariant(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            name = "Quick Version",
            estimatedMinutes = 15,
            isDefault = true,
            createdAt = now,
            updatedAt = later,
        )

        val map = variant.toFirestoreMap()
        val restored = FirestoreMapper.routineVariantFromMap(map)

        assertEquals(variant.id, restored.id)
        assertEquals(variant.routineId, restored.routineId)
        assertEquals(variant.name, restored.name)
        assertEquals(variant.estimatedMinutes, restored.estimatedMinutes)
        assertEquals(variant.isDefault, restored.isDefault)
        assertEquals(variant.createdAt, restored.createdAt)
        assertEquals(variant.updatedAt, restored.updatedAt)
    }

    @Test
    fun `RoutineVariant isDefault defaults to false when missing`() {
        val variant = RoutineVariant(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            name = "Standard",
            estimatedMinutes = 30,
            isDefault = false,
        )

        // Simulate a map where isDefault is missing
        val map = variant.toFirestoreMap().toMutableMap()
        map.remove("isDefault")

        val restored = FirestoreMapper.routineVariantFromMap(map)
        assertEquals(false, restored.isDefault)
    }

    // -----------------------------------------------------------------------
    // RoutineExecution round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `RoutineExecution round-trips through Firestore map`() {
        val execution = RoutineExecution(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            variantId = UUID.randomUUID(),
            startedAt = now,
            completedAt = later,
            status = ExecutionStatus.Completed,
            currentStepIndex = 3,
            currentStepRemainingSeconds = 45,
            totalPausedSeconds = 10,
            createdAt = now,
            updatedAt = later,
        )

        val map = execution.toFirestoreMap()
        val restored = FirestoreMapper.routineExecutionFromMap(map)

        assertEquals(execution.id, restored.id)
        assertEquals(execution.routineId, restored.routineId)
        assertEquals(execution.variantId, restored.variantId)
        assertEquals(execution.startedAt, restored.startedAt)
        assertEquals(execution.completedAt, restored.completedAt)
        assertEquals(execution.status, restored.status)
        assertEquals(execution.currentStepIndex, restored.currentStepIndex)
        assertEquals(
            execution.currentStepRemainingSeconds,
            restored.currentStepRemainingSeconds,
        )
        assertEquals(execution.totalPausedSeconds, restored.totalPausedSeconds)
        assertEquals(execution.createdAt, restored.createdAt)
        assertEquals(execution.updatedAt, restored.updatedAt)
    }

    @Test
    fun `RoutineExecution with null optional fields round-trips`() {
        val execution = RoutineExecution(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            variantId = null,
            startedAt = now,
            completedAt = null,
            status = ExecutionStatus.InProgress,
            currentStepIndex = 0,
            currentStepRemainingSeconds = null,
            totalPausedSeconds = 0,
        )

        val map = execution.toFirestoreMap()
        val restored = FirestoreMapper.routineExecutionFromMap(map)

        assertNull(restored.variantId)
        assertNull(restored.completedAt)
        assertNull(restored.currentStepRemainingSeconds)
    }

    // -----------------------------------------------------------------------
    // RecoverySession round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `RecoverySession round-trips through Firestore map`() {
        val session = RecoverySession(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            type = RecoveryType.Lapse,
            status = SessionStatus.Completed,
            triggeredAt = now,
            completedAt = later,
            blockers = setOf(Blocker.NoEnergy, Blocker.TooBusy),
            action = RecoveryAction.Resume,
            notes = "Getting back on track",
            createdAt = now,
            updatedAt = later,
        )

        val map = session.toFirestoreMap()
        val restored = FirestoreMapper.recoverySessionFromMap(map)

        assertEquals(session.id, restored.id)
        assertEquals(session.habitId, restored.habitId)
        assertEquals(session.type, restored.type)
        assertEquals(session.status, restored.status)
        assertEquals(session.triggeredAt, restored.triggeredAt)
        assertEquals(session.completedAt, restored.completedAt)
        assertEquals(session.blockers, restored.blockers)
        assertEquals(session.action, restored.action)
        assertEquals(session.notes, restored.notes)
        assertEquals(session.createdAt, restored.createdAt)
        assertEquals(session.updatedAt, restored.updatedAt)
    }

    @Test
    fun `RecoverySession Pending with null action round-trips`() {
        val session = RecoverySession(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            type = RecoveryType.Relapse,
            status = SessionStatus.Pending,
            triggeredAt = now,
            completedAt = null,
            blockers = setOf(Blocker.Sick),
            action = null,
            notes = null,
        )

        val map = session.toFirestoreMap()
        val restored = FirestoreMapper.recoverySessionFromMap(map)

        assertNull(restored.completedAt)
        assertNull(restored.action)
        assertNull(restored.notes)
    }

    // -----------------------------------------------------------------------
    // UserPreferences round-trip
    // -----------------------------------------------------------------------

    @Test
    fun `UserPreferences round-trips through Firestore map`() {
        val prefs = UserPreferences(
            id = UUID.randomUUID(),
            userId = "firebase-uid-123",
            notificationEnabled = true,
            defaultReminderTime = LocalTime.of(9, 30),
            theme = Theme.Dark,
            energyTrackingEnabled = true,
            notificationChannels = mapOf("push" to true, "email" to false),
            createdAt = now,
            updatedAt = later,
        )

        val map = prefs.toFirestoreMap()
        val restored = FirestoreMapper.userPreferencesFromMap(map)

        assertEquals(prefs.id, restored.id)
        assertEquals(prefs.userId, restored.userId)
        assertEquals(prefs.notificationEnabled, restored.notificationEnabled)
        assertEquals(prefs.defaultReminderTime, restored.defaultReminderTime)
        assertEquals(prefs.theme, restored.theme)
        assertEquals(prefs.energyTrackingEnabled, restored.energyTrackingEnabled)
        assertEquals(prefs.notificationChannels, restored.notificationChannels)
        assertEquals(prefs.createdAt, restored.createdAt)
        assertEquals(prefs.updatedAt, restored.updatedAt)
    }

    @Test
    fun `UserPreferences with null optional fields round-trips`() {
        val prefs = UserPreferences(
            id = UUID.randomUUID(),
            userId = null,
            notificationChannels = null,
            createdAt = now,
            updatedAt = now,
        )

        val map = prefs.toFirestoreMap()
        val restored = FirestoreMapper.userPreferencesFromMap(map)

        assertNull(restored.userId)
        assertNull(restored.notificationChannels)
    }

    @Test
    fun `UserPreferences defaults applied when boolean fields missing`() {
        val prefs = UserPreferences(
            id = UUID.randomUUID(),
            createdAt = now,
            updatedAt = now,
        )

        // Simulate a map where boolean fields are missing
        val map = prefs.toFirestoreMap().toMutableMap()
        map.remove("notificationEnabled")
        map.remove("energyTrackingEnabled")

        val restored = FirestoreMapper.userPreferencesFromMap(map)
        assertEquals(true, restored.notificationEnabled)
        assertEquals(false, restored.energyTrackingEnabled)
    }

    // -----------------------------------------------------------------------
    // Version field presence
    // -----------------------------------------------------------------------

    @Test
    fun `all toFirestoreMap functions include version field`() {
        val habitMap = Habit(
            id = UUID.randomUUID(),
            name = "Test",
            anchorBehavior = "After X",
            anchorType = AnchorType.AfterBehavior,
            category = HabitCategory.Morning,
            frequency = HabitFrequency.Daily,
            createdAt = now,
            updatedAt = now,
        ).toFirestoreMap()

        val completionMap = Completion(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            date = LocalDate.of(2025, 1, 1),
            completedAt = now,
            type = CompletionType.Full,
            createdAt = now,
        ).toFirestoreMap()

        val routineMap = Routine(
            id = UUID.randomUUID(),
            name = "R",
            category = HabitCategory.Morning,
            createdAt = now,
            updatedAt = now,
        ).toFirestoreMap()

        val routineHabitMap = RoutineHabit(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            orderIndex = 0,
        ).toFirestoreMap()

        val variantMap = RoutineVariant(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            name = "V",
            estimatedMinutes = 5,
        ).toFirestoreMap()

        val executionMap = RoutineExecution(
            id = UUID.randomUUID(),
            routineId = UUID.randomUUID(),
            startedAt = now,
        ).toFirestoreMap()

        val sessionMap = RecoverySession(
            id = UUID.randomUUID(),
            habitId = UUID.randomUUID(),
            type = RecoveryType.Lapse,
            status = SessionStatus.Pending,
            triggeredAt = now,
            blockers = setOf(Blocker.NoEnergy),
        ).toFirestoreMap()

        val prefsMap = UserPreferences(
            id = UUID.randomUUID(),
            createdAt = now,
            updatedAt = now,
        ).toFirestoreMap()

        listOf(
            habitMap,
            completionMap,
            routineMap,
            routineHabitMap,
            variantMap,
            executionMap,
            sessionMap,
            prefsMap,
        ).forEach { map ->
            assertNotNull("version should be present", map["version"])
            assert(map["version"] is Long) {
                "version should be Long"
            }
        }
    }

    // -----------------------------------------------------------------------
    // Unknown tag tests (Task 2)
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `unknown HabitCategory tag throws`() {
        habitCategoryFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown HabitFrequency tag throws`() {
        habitFrequencyFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown HabitPhase tag throws`() {
        habitPhaseFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown HabitStatus tag throws`() {
        habitStatusFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown SkipReason tag throws`() {
        skipReasonFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown RecoveryType tag throws`() {
        recoveryTypeFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown SessionStatus tag throws`() {
        sessionStatusFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown RecoveryAction tag throws`() {
        recoveryActionFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown Blocker tag throws`() {
        blockerFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown RoutineStatus tag throws`() {
        routineStatusFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown ExecutionStatus tag throws`() {
        executionStatusFromTag("INVALID")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown Theme tag throws`() {
        themeFromTag("INVALID")
    }

    // -----------------------------------------------------------------------
    // Habit with empty subtasks (Task 3)
    // -----------------------------------------------------------------------

    @Test
    fun `Habit with empty subtasks serializes correctly and deserializes to null`() {
        val habit = Habit(
            id = UUID.randomUUID(),
            name = "Meditate",
            anchorBehavior = "After brushing teeth",
            anchorType = AnchorType.AfterBehavior,
            category = HabitCategory.Morning,
            frequency = HabitFrequency.Daily,
            subtasks = emptyList(),
            createdAt = now,
            updatedAt = now,
        )

        val map = habit.toFirestoreMap()
        assertEquals(emptyList<String>(), map["subtasks"])

        val restored = FirestoreMapper.habitFromMap(map)
        assertNull(restored.subtasks) // empty list normalizes to null
    }

    // -----------------------------------------------------------------------
    // Habit map structure assertion (Task 4)
    // -----------------------------------------------------------------------

    @Test
    fun `Habit toFirestoreMap contains expected keys and field formats`() {
        val id = UUID.randomUUID()
        val habit = Habit(
            id = id,
            name = "Meditate",
            description = "10 min morning meditation",
            icon = "brain",
            color = "#FF5733",
            anchorBehavior = "After brushing teeth",
            anchorType = AnchorType.AfterBehavior,
            timeWindowStart = "07:00",
            timeWindowEnd = "08:00",
            category = HabitCategory.Morning,
            frequency = HabitFrequency.Custom,
            activeDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            estimatedSeconds = 600,
            microVersion = "5 deep breaths",
            allowPartialCompletion = true,
            subtasks = listOf("Sit down", "Set timer"),
            phase = HabitPhase.FORMING,
            status = HabitStatus.Active,
            createdAt = now,
            updatedAt = later,
            pausedAt = null,
            archivedAt = null,
            lapseThresholdDays = 3,
            relapseThresholdDays = 7,
        )

        val map = habit.toFirestoreMap(version = 42L)

        val expectedKeys = setOf(
            "id", "name", "description", "icon", "color",
            "anchorBehavior", "anchorType", "timeWindow",
            "category", "frequency", "activeDays",
            "estimatedSeconds", "microVersion", "allowPartial",
            "subtasks", "lapseThresholdDays", "relapseThresholdDays",
            "phase", "status", "createdAt", "updatedAt",
            "pausedAt", "archivedAt", "version",
        )
        assertEquals(expectedKeys, map.keys)

        // Assert specific field formats
        assertEquals("AFTER_BEHAVIOR", map["anchorType"])
        assertEquals("MORNING", map["category"])
        assertEquals("CUSTOM", map["frequency"])
        assertEquals("FORMING", map["phase"])
        assertEquals("ACTIVE", map["status"])
        assertEquals(42L, map["version"])
        assertEquals(id.toString(), map["id"])
    }

    // -----------------------------------------------------------------------
    // Reflection-based sealed subclass coverage (Task 5)
    // -----------------------------------------------------------------------

    @Test
    fun `all AnchorType subclasses round-trip through tags`() {
        AnchorType::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, anchorTypeFromTag(tag))
        }
    }

    @Test
    fun `all HabitCategory subclasses round-trip through tags`() {
        HabitCategory::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, habitCategoryFromTag(tag))
        }
    }

    @Test
    fun `all HabitFrequency subclasses round-trip through tags`() {
        HabitFrequency::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, habitFrequencyFromTag(tag))
        }
    }

    @Test
    fun `all HabitPhase subclasses round-trip through tags`() {
        HabitPhase::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, habitPhaseFromTag(tag))
        }
    }

    @Test
    fun `all HabitStatus subclasses round-trip through tags`() {
        HabitStatus::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, habitStatusFromTag(tag))
        }
    }

    @Test
    fun `all CompletionType subclasses round-trip through tags`() {
        CompletionType::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, completionTypeFromTag(tag))
        }
    }

    @Test
    fun `all SkipReason subclasses round-trip through tags`() {
        SkipReason::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, skipReasonFromTag(tag))
        }
    }

    @Test
    fun `all RecoveryType subclasses round-trip through tags`() {
        RecoveryType::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, recoveryTypeFromTag(tag))
        }
    }

    @Test
    fun `all SessionStatus subclasses round-trip through tags`() {
        SessionStatus::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, sessionStatusFromTag(tag))
        }
    }

    @Test
    fun `all RecoveryAction subclasses round-trip through tags`() {
        RecoveryAction::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, recoveryActionFromTag(tag))
        }
    }

    @Test
    fun `all Blocker subclasses round-trip through tags`() {
        Blocker::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, blockerFromTag(tag))
        }
    }

    @Test
    fun `all RoutineStatus subclasses round-trip through tags`() {
        RoutineStatus::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, routineStatusFromTag(tag))
        }
    }

    @Test
    fun `all ExecutionStatus subclasses round-trip through tags`() {
        ExecutionStatus::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, executionStatusFromTag(tag))
        }
    }

    @Test
    fun `all Theme subclasses round-trip through tags`() {
        Theme::class.sealedSubclasses.forEach { subclass ->
            val instance = subclass.objectInstance ?: return@forEach
            val tag = instance.toTag()
            assertEquals(instance, themeFromTag(tag))
        }
    }

    // -----------------------------------------------------------------------
    // FirestoreMappingException test (Task 6)
    // -----------------------------------------------------------------------

    @Test
    fun `habitFromMap with missing name field throws FirestoreMappingException`() {
        val incompleteMap = mapOf<String, Any?>("id" to UUID.randomUUID().toString())
        val ex = org.junit.Assert.assertThrows(FirestoreMappingException::class.java) {
            FirestoreMapper.habitFromMap(incompleteMap)
        }
        assertTrue(ex.message!!.contains("Habit"))
        assertTrue(ex.message!!.contains("name"))
    }

    @Test
    fun `completionFromMap with missing date field throws FirestoreMappingException`() {
        val incompleteMap = mapOf<String, Any?>(
            "id" to UUID.randomUUID().toString(),
            "habitId" to UUID.randomUUID().toString(),
        )
        val ex = org.junit.Assert.assertThrows(FirestoreMappingException::class.java) {
            FirestoreMapper.completionFromMap(incompleteMap)
        }
        assertTrue(ex.message!!.contains("Completion"))
        assertTrue(ex.message!!.contains("date"))
    }

    @Test
    fun `routineHabitFromMap with missing orderIndex throws FirestoreMappingException`() {
        val incompleteMap = mapOf<String, Any?>(
            "id" to UUID.randomUUID().toString(),
            "routineId" to UUID.randomUUID().toString(),
            "habitId" to UUID.randomUUID().toString(),
        )
        val ex = org.junit.Assert.assertThrows(FirestoreMappingException::class.java) {
            FirestoreMapper.routineHabitFromMap(incompleteMap)
        }
        assertTrue(ex.message!!.contains("RoutineHabit"))
        assertTrue(ex.message!!.contains("orderIndex"))
    }

    // -----------------------------------------------------------------------
    // RecoverySession boundary case (Task 7)
    // -----------------------------------------------------------------------

    @Test
    fun `recoverySessionFromMap with empty blockers propagates domain constraint exception`() {
        val triggeredTs = now.toTimestamp()
        val createdTs = now.toTimestamp()
        val updatedTs = now.toTimestamp()
        val validMapWithEmptyBlockers = mapOf<String, Any?>(
            "id" to UUID.randomUUID().toString(),
            "habitId" to UUID.randomUUID().toString(),
            "type" to "LAPSE",
            "status" to "PENDING",
            "triggeredAt" to triggeredTs,
            "completedAt" to null,
            "blockers" to emptyList<String>(),
            "action" to null,
            "notes" to null,
            "createdAt" to createdTs,
            "updatedAt" to updatedTs,
        )
        org.junit.Assert.assertThrows(Exception::class.java) {
            FirestoreMapper.recoverySessionFromMap(validMapWithEmptyBlockers)
        }
    }
}
