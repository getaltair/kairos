package com.getaltair.kairos.domain.testutil

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
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.RoutineStatus
import com.getaltair.kairos.domain.enums.SessionStatus
import com.getaltair.kairos.domain.enums.Theme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Centralized test factory for creating domain entities with sensible defaults.
 * All test modules should use these factories instead of constructing entities directly,
 * ensuring consistency and reducing duplication across test suites.
 */
object HabitFactory {

    /** A fixed instant used as the default for all timestamp fields. */
    val DEFAULT_INSTANT: Instant = Instant.parse("2025-01-01T00:00:00Z")

    /** A fixed UUID used as the default habit ID for cross-entity consistency. */
    val DEFAULT_HABIT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    /** A fixed UUID used as the default routine ID for cross-entity consistency. */
    val DEFAULT_ROUTINE_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")

    // ---- Habit ----

    fun habit(
        id: UUID = DEFAULT_HABIT_ID,
        name: String = "Test Habit",
        description: String? = null,
        icon: String? = null,
        color: String? = null,
        anchorBehavior: String = "After brushing teeth",
        anchorType: AnchorType = AnchorType.AfterBehavior,
        timeWindowStart: String? = null,
        timeWindowEnd: String? = null,
        category: HabitCategory = HabitCategory.Morning,
        frequency: HabitFrequency = HabitFrequency.Daily,
        activeDays: Set<DayOfWeek>? = null,
        estimatedSeconds: Int = 300,
        microVersion: String? = null,
        allowPartialCompletion: Boolean = true,
        subtasks: List<String>? = null,
        phase: HabitPhase = HabitPhase.ONBOARD,
        status: HabitStatus = HabitStatus.Active,
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT,
        pausedAt: Instant? = null,
        archivedAt: Instant? = null,
        lapseThresholdDays: Int = 3,
        relapseThresholdDays: Int = 7,
    ): Habit = Habit(
        id = id,
        name = name,
        description = description,
        icon = icon,
        color = color,
        anchorBehavior = anchorBehavior,
        anchorType = anchorType,
        timeWindowStart = timeWindowStart,
        timeWindowEnd = timeWindowEnd,
        category = category,
        frequency = frequency,
        activeDays = activeDays,
        estimatedSeconds = estimatedSeconds,
        microVersion = microVersion,
        allowPartialCompletion = allowPartialCompletion,
        subtasks = subtasks,
        phase = phase,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pausedAt = pausedAt,
        archivedAt = archivedAt,
        lapseThresholdDays = lapseThresholdDays,
        relapseThresholdDays = relapseThresholdDays,
    )

    // ---- Completion ----

    fun completion(
        id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000010"),
        habitId: UUID = DEFAULT_HABIT_ID,
        date: LocalDate = LocalDate.of(2025, 1, 1),
        completedAt: Instant = DEFAULT_INSTANT,
        type: CompletionType = CompletionType.Full,
        partialPercent: Int? = null,
        skipReason: com.getaltair.kairos.domain.enums.SkipReason? = null,
        energyLevel: Int? = null,
        note: String? = null,
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT,
    ): Completion = Completion(
        id = id,
        habitId = habitId,
        date = date,
        completedAt = completedAt,
        type = type,
        partialPercent = partialPercent,
        skipReason = skipReason,
        energyLevel = energyLevel,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ---- Routine ----

    fun routine(
        id: UUID = DEFAULT_ROUTINE_ID,
        name: String = "Morning Routine",
        description: String? = null,
        icon: String? = null,
        color: String? = null,
        category: HabitCategory = HabitCategory.Morning,
        status: RoutineStatus = RoutineStatus.Active,
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT,
    ): Routine = Routine(
        id = id,
        name = name,
        description = description,
        icon = icon,
        color = color,
        category = category,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ---- RoutineExecution ----

    fun routineExecution(
        id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000020"),
        routineId: UUID = DEFAULT_ROUTINE_ID,
        variantId: UUID? = null,
        startedAt: Instant = DEFAULT_INSTANT,
        completedAt: Instant? = null,
        status: ExecutionStatus = ExecutionStatus.NotStarted,
        currentStepIndex: Int = 0,
        currentStepRemainingSeconds: Int? = null,
        totalPausedSeconds: Int = 0,
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT,
    ): RoutineExecution = RoutineExecution(
        id = id,
        routineId = routineId,
        variantId = variantId,
        startedAt = startedAt,
        completedAt = completedAt,
        status = status,
        currentStepIndex = currentStepIndex,
        currentStepRemainingSeconds = currentStepRemainingSeconds,
        totalPausedSeconds = totalPausedSeconds,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ---- RoutineHabit ----

    fun routineHabit(
        id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000030"),
        routineId: UUID = DEFAULT_ROUTINE_ID,
        habitId: UUID = DEFAULT_HABIT_ID,
        orderIndex: Int = 0,
        overrideDurationSeconds: Int? = null,
        variantIds: List<UUID> = emptyList(),
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT,
    ): RoutineHabit = RoutineHabit(
        id = id,
        routineId = routineId,
        habitId = habitId,
        orderIndex = orderIndex,
        overrideDurationSeconds = overrideDurationSeconds,
        variantIds = variantIds,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ---- RoutineVariant ----

    fun routineVariant(
        id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000040"),
        routineId: UUID = DEFAULT_ROUTINE_ID,
        name: String = "Standard",
        estimatedMinutes: Int = 30,
        isDefault: Boolean = false,
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT,
    ): RoutineVariant = RoutineVariant(
        id = id,
        routineId = routineId,
        name = name,
        estimatedMinutes = estimatedMinutes,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ---- RecoverySession ----

    fun recoverySession(
        id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000050"),
        habitId: UUID = DEFAULT_HABIT_ID,
        type: RecoveryType = RecoveryType.Lapse,
        status: SessionStatus = SessionStatus.Pending,
        triggeredAt: Instant = DEFAULT_INSTANT,
        completedAt: Instant? = null,
        blockers: Set<Blocker> = setOf(Blocker.NoEnergy),
        action: com.getaltair.kairos.domain.enums.RecoveryAction? = null,
        notes: String? = null,
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT,
    ): RecoverySession = RecoverySession(
        id = id,
        habitId = habitId,
        type = type,
        status = status,
        triggeredAt = triggeredAt,
        completedAt = completedAt,
        blockers = blockers,
        action = action,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    // ---- UserPreferences ----

    fun userPreferences(
        id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000060"),
        userId: String? = null,
        notificationEnabled: Boolean = true,
        defaultReminderTime: LocalTime = LocalTime.of(9, 0),
        theme: Theme = Theme.System,
        energyTrackingEnabled: Boolean = false,
        notificationChannels: Map<String, Any>? = null,
        quietHoursEnabled: Boolean = true,
        quietHoursStart: LocalTime = LocalTime.of(22, 0),
        quietHoursEnd: LocalTime = LocalTime.of(7, 0),
        createdAt: Instant = DEFAULT_INSTANT,
        updatedAt: Instant = DEFAULT_INSTANT,
    ): UserPreferences = UserPreferences(
        id = id,
        userId = userId,
        notificationEnabled = notificationEnabled,
        defaultReminderTime = defaultReminderTime,
        theme = theme,
        energyTrackingEnabled = energyTrackingEnabled,
        notificationChannels = notificationChannels,
        quietHoursEnabled = quietHoursEnabled,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
