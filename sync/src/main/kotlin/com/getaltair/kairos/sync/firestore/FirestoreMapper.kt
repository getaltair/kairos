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

// ---------------------------------------------------------------------------
// Sealed-class serialization helpers
//
// Every sealed class in the domain uses PascalCase data-object names
// (e.g. AfterBehavior, NotFeelingWell). Firestore stores them as
// UPPER_SNAKE_CASE strings (e.g. AFTER_BEHAVIOR, NOT_FEELING_WELL) to
// match the document schemas in docs/08-erd.md.
// ---------------------------------------------------------------------------

// --- AnchorType ---

internal fun AnchorType.toTag(): String = when (this) {
    is AnchorType.AfterBehavior -> "AFTER_BEHAVIOR"
    is AnchorType.BeforeBehavior -> "BEFORE_BEHAVIOR"
    is AnchorType.AtLocation -> "AT_LOCATION"
    is AnchorType.AtTime -> "AT_TIME"
}

internal fun anchorTypeFromTag(tag: String): AnchorType = when (tag) {
    "AFTER_BEHAVIOR" -> AnchorType.AfterBehavior
    "BEFORE_BEHAVIOR" -> AnchorType.BeforeBehavior
    "AT_LOCATION" -> AnchorType.AtLocation
    "AT_TIME" -> AnchorType.AtTime
    else -> throw IllegalArgumentException("Unknown AnchorType tag: $tag")
}

// --- HabitCategory ---

internal fun HabitCategory.toTag(): String = when (this) {
    is HabitCategory.Morning -> "MORNING"
    is HabitCategory.Afternoon -> "AFTERNOON"
    is HabitCategory.Evening -> "EVENING"
    is HabitCategory.Anytime -> "ANYTIME"
    is HabitCategory.Departure -> "DEPARTURE"
}

internal fun habitCategoryFromTag(tag: String): HabitCategory = when (tag) {
    "MORNING" -> HabitCategory.Morning
    "AFTERNOON" -> HabitCategory.Afternoon
    "EVENING" -> HabitCategory.Evening
    "ANYTIME" -> HabitCategory.Anytime
    "DEPARTURE" -> HabitCategory.Departure
    else -> throw IllegalArgumentException("Unknown HabitCategory tag: $tag")
}

// --- HabitFrequency ---

internal fun HabitFrequency.toTag(): String = when (this) {
    is HabitFrequency.Daily -> "DAILY"
    is HabitFrequency.Weekdays -> "WEEKDAYS"
    is HabitFrequency.Weekends -> "WEEKENDS"
    is HabitFrequency.Custom -> "CUSTOM"
}

internal fun habitFrequencyFromTag(tag: String): HabitFrequency = when (tag) {
    "DAILY" -> HabitFrequency.Daily
    "WEEKDAYS" -> HabitFrequency.Weekdays
    "WEEKENDS" -> HabitFrequency.Weekends
    "CUSTOM" -> HabitFrequency.Custom
    else -> throw IllegalArgumentException("Unknown HabitFrequency tag: $tag")
}

// --- HabitPhase ---

internal fun HabitPhase.toTag(): String = when (this) {
    is HabitPhase.ONBOARD -> "ONBOARD"
    is HabitPhase.FORMING -> "FORMING"
    is HabitPhase.MAINTAINING -> "MAINTAINING"
    is HabitPhase.LAPSED -> "LAPSED"
    is HabitPhase.RELAPSED -> "RELAPSED"
}

internal fun habitPhaseFromTag(tag: String): HabitPhase = when (tag) {
    "ONBOARD" -> HabitPhase.ONBOARD
    "FORMING" -> HabitPhase.FORMING
    "MAINTAINING" -> HabitPhase.MAINTAINING
    "LAPSED" -> HabitPhase.LAPSED
    "RELAPSED" -> HabitPhase.RELAPSED
    else -> throw IllegalArgumentException("Unknown HabitPhase tag: $tag")
}

// --- HabitStatus ---

internal fun HabitStatus.toTag(): String = when (this) {
    is HabitStatus.Active -> "ACTIVE"
    is HabitStatus.Paused -> "PAUSED"
    is HabitStatus.Archived -> "ARCHIVED"
}

internal fun habitStatusFromTag(tag: String): HabitStatus = when (tag) {
    "ACTIVE" -> HabitStatus.Active
    "PAUSED" -> HabitStatus.Paused
    "ARCHIVED" -> HabitStatus.Archived
    else -> throw IllegalArgumentException("Unknown HabitStatus tag: $tag")
}

// --- CompletionType ---

internal fun CompletionType.toTag(): String = when (this) {
    is CompletionType.Full -> "FULL"
    is CompletionType.Partial -> "PARTIAL"
    is CompletionType.Skipped -> "SKIPPED"
    is CompletionType.Missed -> "MISSED"
}

internal fun completionTypeFromTag(tag: String): CompletionType = when (tag) {
    "FULL" -> CompletionType.Full
    "PARTIAL" -> CompletionType.Partial
    "SKIPPED" -> CompletionType.Skipped
    "MISSED" -> CompletionType.Missed
    else -> throw IllegalArgumentException("Unknown CompletionType tag: $tag")
}

// --- SkipReason ---

internal fun SkipReason.toTag(): String = when (this) {
    is SkipReason.TooTired -> "TOO_TIRED"
    is SkipReason.NoTime -> "NO_TIME"
    is SkipReason.NotFeelingWell -> "NOT_FEELING_WELL"
    is SkipReason.Traveling -> "TRAVELING"
    is SkipReason.TookDayOff -> "TOOK_DAY_OFF"
    is SkipReason.Other -> "OTHER"
}

internal fun skipReasonFromTag(tag: String): SkipReason = when (tag) {
    "TOO_TIRED" -> SkipReason.TooTired
    "NO_TIME" -> SkipReason.NoTime
    "NOT_FEELING_WELL" -> SkipReason.NotFeelingWell
    "TRAVELING" -> SkipReason.Traveling
    "TOOK_DAY_OFF" -> SkipReason.TookDayOff
    "OTHER" -> SkipReason.Other
    else -> throw IllegalArgumentException("Unknown SkipReason tag: $tag")
}

// --- RecoveryType ---

internal fun RecoveryType.toTag(): String = when (this) {
    is RecoveryType.Lapse -> "LAPSE"
    is RecoveryType.Relapse -> "RELAPSE"
}

internal fun recoveryTypeFromTag(tag: String): RecoveryType = when (tag) {
    "LAPSE" -> RecoveryType.Lapse
    "RELAPSE" -> RecoveryType.Relapse
    else -> throw IllegalArgumentException("Unknown RecoveryType tag: $tag")
}

// --- SessionStatus ---

internal fun SessionStatus.toTag(): String = when (this) {
    is SessionStatus.Pending -> "PENDING"
    is SessionStatus.Completed -> "COMPLETED"
    is SessionStatus.Abandoned -> "ABANDONED"
}

internal fun sessionStatusFromTag(tag: String): SessionStatus = when (tag) {
    "PENDING" -> SessionStatus.Pending
    "COMPLETED" -> SessionStatus.Completed
    "ABANDONED" -> SessionStatus.Abandoned
    else -> throw IllegalArgumentException("Unknown SessionStatus tag: $tag")
}

// --- RecoveryAction ---

internal fun RecoveryAction.toTag(): String = when (this) {
    is RecoveryAction.Resume -> "RESUME"
    is RecoveryAction.Simplify -> "SIMPLIFY"
    is RecoveryAction.Pause -> "PAUSE"
    is RecoveryAction.Archive -> "ARCHIVE"
    is RecoveryAction.FreshStart -> "FRESH_START"
}

internal fun recoveryActionFromTag(tag: String): RecoveryAction = when (tag) {
    "RESUME" -> RecoveryAction.Resume
    "SIMPLIFY" -> RecoveryAction.Simplify
    "PAUSE" -> RecoveryAction.Pause
    "ARCHIVE" -> RecoveryAction.Archive
    "FRESH_START" -> RecoveryAction.FreshStart
    else -> throw IllegalArgumentException("Unknown RecoveryAction tag: $tag")
}

// --- Blocker ---

internal fun Blocker.toTag(): String = when (this) {
    is Blocker.NoEnergy -> "NO_ENERGY"
    is Blocker.PainPhysical -> "PAIN_PHYSICAL"
    is Blocker.PainMental -> "PAIN_MENTAL"
    is Blocker.TooBusy -> "TOO_BUSY"
    is Blocker.FamilyEmergency -> "FAMILY_EMERGENCY"
    is Blocker.WorkEmergency -> "WORK_EMERGENCY"
    is Blocker.Sick -> "SICK"
    is Blocker.Weather -> "WEATHER"
    is Blocker.EquipmentFailure -> "EQUIPMENT_FAILURE"
    is Blocker.Other -> "OTHER"
}

internal fun blockerFromTag(tag: String): Blocker = when (tag) {
    "NO_ENERGY" -> Blocker.NoEnergy
    "PAIN_PHYSICAL" -> Blocker.PainPhysical
    "PAIN_MENTAL" -> Blocker.PainMental
    "TOO_BUSY" -> Blocker.TooBusy
    "FAMILY_EMERGENCY" -> Blocker.FamilyEmergency
    "WORK_EMERGENCY" -> Blocker.WorkEmergency
    "SICK" -> Blocker.Sick
    "WEATHER" -> Blocker.Weather
    "EQUIPMENT_FAILURE" -> Blocker.EquipmentFailure
    "OTHER" -> Blocker.Other
    else -> throw IllegalArgumentException("Unknown Blocker tag: $tag")
}

// --- RoutineStatus ---

internal fun RoutineStatus.toTag(): String = when (this) {
    is RoutineStatus.Active -> "ACTIVE"
    is RoutineStatus.Paused -> "PAUSED"
    is RoutineStatus.Archived -> "ARCHIVED"
}

internal fun routineStatusFromTag(tag: String): RoutineStatus = when (tag) {
    "ACTIVE" -> RoutineStatus.Active
    "PAUSED" -> RoutineStatus.Paused
    "ARCHIVED" -> RoutineStatus.Archived
    else -> throw IllegalArgumentException("Unknown RoutineStatus tag: $tag")
}

// --- ExecutionStatus ---

internal fun ExecutionStatus.toTag(): String = when (this) {
    is ExecutionStatus.NotStarted -> "NOT_STARTED"
    is ExecutionStatus.InProgress -> "IN_PROGRESS"
    is ExecutionStatus.Paused -> "PAUSED"
    is ExecutionStatus.Completed -> "COMPLETED"
    is ExecutionStatus.Abandoned -> "ABANDONED"
}

internal fun executionStatusFromTag(tag: String): ExecutionStatus = when (tag) {
    "NOT_STARTED" -> ExecutionStatus.NotStarted
    "IN_PROGRESS" -> ExecutionStatus.InProgress
    "PAUSED" -> ExecutionStatus.Paused
    "COMPLETED" -> ExecutionStatus.Completed
    "ABANDONED" -> ExecutionStatus.Abandoned
    else -> throw IllegalArgumentException("Unknown ExecutionStatus tag: $tag")
}

// --- Theme ---

internal fun Theme.toTag(): String = when (this) {
    is Theme.System -> "SYSTEM"
    is Theme.Light -> "LIGHT"
    is Theme.Dark -> "DARK"
}

internal fun themeFromTag(tag: String): Theme = when (tag) {
    "SYSTEM" -> Theme.System
    "LIGHT" -> Theme.Light
    "DARK" -> Theme.Dark
    else -> throw IllegalArgumentException("Unknown Theme tag: $tag")
}

// ---------------------------------------------------------------------------
// Timestamp helpers
// ---------------------------------------------------------------------------

/** Converts a [java.time.Instant] to a Firebase [Timestamp]. */
internal fun Instant.toTimestamp(): Timestamp = Timestamp(epochSecond, nano)

// Timestamp.toInstant() is provided by the Firebase SDK as a member function,
// so no extension is needed for the reverse direction.

// ---------------------------------------------------------------------------
// Entity -> Firestore Map  (extension functions on each entity)
// ---------------------------------------------------------------------------

/** Converts this [Habit] to a Firestore-compatible [Map]. */
fun Habit.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "name" to name,
    "description" to description,
    "icon" to icon,
    "color" to color,
    "anchorBehavior" to anchorBehavior,
    "anchorType" to anchorType.toTag(),
    "timeWindow" to if (timeWindowStart != null && timeWindowEnd != null) {
        mapOf("start" to timeWindowStart, "end" to timeWindowEnd)
    } else {
        null
    },
    "category" to category.toTag(),
    "frequency" to frequency.toTag(),
    "activeDays" to activeDays?.map { it.name },
    "estimatedSeconds" to estimatedSeconds,
    "microVersion" to microVersion,
    "allowPartial" to allowPartialCompletion,
    "subtasks" to (subtasks ?: emptyList()),
    "lapseThresholdDays" to lapseThresholdDays,
    "relapseThresholdDays" to relapseThresholdDays,
    "phase" to phase.toTag(),
    "status" to status.toTag(),
    "createdAt" to createdAt.toTimestamp(),
    "updatedAt" to updatedAt.toTimestamp(),
    "pausedAt" to pausedAt?.toTimestamp(),
    "archivedAt" to archivedAt?.toTimestamp(),
    "version" to System.currentTimeMillis(),
)

/** Converts this [Completion] to a Firestore-compatible [Map]. */
fun Completion.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "habitId" to habitId.toString(),
    "date" to date.toString(), // YYYY-MM-DD
    "completedAt" to completedAt.toTimestamp(),
    "type" to type.toTag(),
    "partialPercent" to partialPercent,
    "skipReason" to skipReason?.toTag(),
    "energyLevel" to energyLevel,
    "note" to note,
    "createdAt" to createdAt.toTimestamp(),
    "version" to System.currentTimeMillis(),
)

/** Converts this [Routine] to a Firestore-compatible [Map]. */
fun Routine.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "name" to name,
    "description" to description,
    "icon" to icon,
    "color" to color,
    "category" to category.toTag(),
    "status" to status.toTag(),
    "createdAt" to createdAt.toTimestamp(),
    "updatedAt" to updatedAt.toTimestamp(),
    "version" to System.currentTimeMillis(),
)

/** Converts this [RoutineHabit] to a Firestore-compatible [Map]. */
fun RoutineHabit.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "routineId" to routineId.toString(),
    "habitId" to habitId.toString(),
    "orderIndex" to orderIndex,
    "overrideDurationSeconds" to overrideDurationSeconds,
    "variantIds" to variantIds?.map { it.toString() },
    "version" to System.currentTimeMillis(),
)

/** Converts this [RoutineVariant] to a Firestore-compatible [Map]. */
fun RoutineVariant.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "routineId" to routineId.toString(),
    "name" to name,
    "estimatedMinutes" to estimatedMinutes,
    "isDefault" to isDefault,
    "version" to System.currentTimeMillis(),
)

/** Converts this [RoutineExecution] to a Firestore-compatible [Map]. */
fun RoutineExecution.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "routineId" to routineId.toString(),
    "variantId" to variantId?.toString(),
    "startedAt" to startedAt.toTimestamp(),
    "completedAt" to completedAt?.toTimestamp(),
    "status" to status.toTag(),
    "currentStepIndex" to currentStepIndex,
    "currentStepRemainingSeconds" to currentStepRemainingSeconds,
    "totalPausedSeconds" to totalPausedSeconds,
    "version" to System.currentTimeMillis(),
)

/** Converts this [RecoverySession] to a Firestore-compatible [Map]. */
fun RecoverySession.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "habitId" to habitId.toString(),
    "type" to type.toTag(),
    "status" to status.toTag(),
    "triggeredAt" to triggeredAt.toTimestamp(),
    "completedAt" to completedAt?.toTimestamp(),
    "blockers" to blockers.map { it.toTag() },
    "action" to action?.toTag(),
    "notes" to notes,
    "version" to System.currentTimeMillis(),
)

/** Converts this [UserPreferences] to a Firestore-compatible [Map]. */
fun UserPreferences.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "userId" to userId,
    "notificationEnabled" to notificationEnabled,
    "defaultReminderTime" to defaultReminderTime.toString(), // HH:mm
    "theme" to theme.toTag(),
    "energyTrackingEnabled" to energyTrackingEnabled,
    "notificationChannels" to notificationChannels,
    "createdAt" to createdAt.toTimestamp(),
    "updatedAt" to updatedAt.toTimestamp(),
    "version" to System.currentTimeMillis(),
)

// ---------------------------------------------------------------------------
// Firestore Map -> Entity  (factory functions on a central mapper object)
//
// Domain entity data classes do not declare companion objects, so we group
// the deserialization logic inside FirestoreMapper rather than adding
// companion extensions.
// ---------------------------------------------------------------------------

/**
 * Factory that reconstructs domain entities from Firestore document maps.
 *
 * Each function mirrors the field layout produced by the corresponding
 * `toFirestoreMap()` extension above.
 */
object FirestoreMapper {

    /** Reconstructs a [Habit] from a Firestore document [Map]. */
    fun habitFromMap(map: Map<String, Any?>): Habit {
        @Suppress("UNCHECKED_CAST")
        val timeWindow = map["timeWindow"] as? Map<String, String>

        @Suppress("UNCHECKED_CAST")
        val activeDaysRaw = map["activeDays"] as? List<String>

        return Habit(
            id = UUID.fromString(map["id"] as String),
            name = map["name"] as String,
            description = map["description"] as? String,
            icon = map["icon"] as? String,
            color = map["color"] as? String,
            anchorBehavior = map["anchorBehavior"] as String,
            anchorType = anchorTypeFromTag(map["anchorType"] as String),
            timeWindowStart = timeWindow?.get("start"),
            timeWindowEnd = timeWindow?.get("end"),
            category = habitCategoryFromTag(map["category"] as String),
            frequency = habitFrequencyFromTag(map["frequency"] as String),
            activeDays = activeDaysRaw?.map { DayOfWeek.valueOf(it) }?.toSet(),
            estimatedSeconds = (map["estimatedSeconds"] as Number).toInt(),
            microVersion = map["microVersion"] as? String,
            allowPartialCompletion = map["allowPartial"] as? Boolean ?: true,
            subtasks = (map["subtasks"] as? List<*>)
                ?.filterIsInstance<String>()
                ?.ifEmpty { null },
            lapseThresholdDays = (map["lapseThresholdDays"] as Number).toInt(),
            relapseThresholdDays = (map["relapseThresholdDays"] as Number).toInt(),
            phase = habitPhaseFromTag(map["phase"] as String),
            status = habitStatusFromTag(map["status"] as String),
            createdAt = (map["createdAt"] as Timestamp).toInstant(),
            updatedAt = (map["updatedAt"] as Timestamp).toInstant(),
            pausedAt = (map["pausedAt"] as? Timestamp)?.toInstant(),
            archivedAt = (map["archivedAt"] as? Timestamp)?.toInstant(),
        )
    }

    /** Reconstructs a [Completion] from a Firestore document [Map]. */
    fun completionFromMap(map: Map<String, Any?>): Completion = Completion(
        id = UUID.fromString(map["id"] as String),
        habitId = UUID.fromString(map["habitId"] as String),
        date = LocalDate.parse(map["date"] as String),
        completedAt = (map["completedAt"] as Timestamp).toInstant(),
        type = completionTypeFromTag(map["type"] as String),
        partialPercent = (map["partialPercent"] as? Number)?.toInt(),
        skipReason = (map["skipReason"] as? String)
            ?.let { skipReasonFromTag(it) },
        energyLevel = (map["energyLevel"] as? Number)?.toInt(),
        note = map["note"] as? String,
        createdAt = (map["createdAt"] as Timestamp).toInstant(),
    )

    /** Reconstructs a [Routine] from a Firestore document [Map]. */
    fun routineFromMap(map: Map<String, Any?>): Routine = Routine(
        id = UUID.fromString(map["id"] as String),
        name = map["name"] as String,
        description = map["description"] as? String,
        icon = map["icon"] as? String,
        color = map["color"] as? String,
        category = habitCategoryFromTag(map["category"] as String),
        status = routineStatusFromTag(map["status"] as String),
        createdAt = (map["createdAt"] as Timestamp).toInstant(),
        updatedAt = (map["updatedAt"] as Timestamp).toInstant(),
    )

    /** Reconstructs a [RoutineHabit] from a Firestore document [Map]. */
    fun routineHabitFromMap(map: Map<String, Any?>): RoutineHabit {
        @Suppress("UNCHECKED_CAST")
        val variantIdsRaw = map["variantIds"] as? List<String>
        return RoutineHabit(
            id = UUID.fromString(map["id"] as String),
            routineId = UUID.fromString(map["routineId"] as String),
            habitId = UUID.fromString(map["habitId"] as String),
            orderIndex = (map["orderIndex"] as Number).toInt(),
            overrideDurationSeconds =
                (map["overrideDurationSeconds"] as? Number)?.toInt(),
            variantIds = variantIdsRaw?.map { UUID.fromString(it) },
        )
    }

    /** Reconstructs a [RoutineVariant] from a Firestore document [Map]. */
    fun routineVariantFromMap(map: Map<String, Any?>): RoutineVariant = RoutineVariant(
        id = UUID.fromString(map["id"] as String),
        routineId = UUID.fromString(map["routineId"] as String),
        name = map["name"] as String,
        estimatedMinutes = (map["estimatedMinutes"] as Number).toInt(),
        isDefault = map["isDefault"] as? Boolean ?: false,
    )

    /** Reconstructs a [RoutineExecution] from a Firestore document [Map]. */
    fun routineExecutionFromMap(map: Map<String, Any?>): RoutineExecution = RoutineExecution(
        id = UUID.fromString(map["id"] as String),
        routineId = UUID.fromString(map["routineId"] as String),
        variantId = (map["variantId"] as? String)
            ?.let { UUID.fromString(it) },
        startedAt = (map["startedAt"] as Timestamp).toInstant(),
        completedAt = (map["completedAt"] as? Timestamp)?.toInstant(),
        status = executionStatusFromTag(map["status"] as String),
        currentStepIndex = (map["currentStepIndex"] as Number).toInt(),
        currentStepRemainingSeconds =
            (map["currentStepRemainingSeconds"] as? Number)?.toInt(),
        totalPausedSeconds = (map["totalPausedSeconds"] as Number).toInt(),
    )

    /** Reconstructs a [RecoverySession] from a Firestore document [Map]. */
    fun recoverySessionFromMap(map: Map<String, Any?>): RecoverySession {
        @Suppress("UNCHECKED_CAST")
        val blockerTags = map["blockers"] as List<String>
        return RecoverySession(
            id = UUID.fromString(map["id"] as String),
            habitId = UUID.fromString(map["habitId"] as String),
            type = recoveryTypeFromTag(map["type"] as String),
            status = sessionStatusFromTag(map["status"] as String),
            triggeredAt = (map["triggeredAt"] as Timestamp).toInstant(),
            completedAt = (map["completedAt"] as? Timestamp)?.toInstant(),
            blockers = blockerTags.map { blockerFromTag(it) },
            action = (map["action"] as? String)
                ?.let { recoveryActionFromTag(it) },
            notes = map["notes"] as? String,
        )
    }

    /** Reconstructs [UserPreferences] from a Firestore document [Map]. */
    fun userPreferencesFromMap(map: Map<String, Any?>): UserPreferences {
        @Suppress("UNCHECKED_CAST")
        val channels = map["notificationChannels"] as? Map<String, Any>
        return UserPreferences(
            id = UUID.fromString(map["id"] as String),
            userId = map["userId"] as? String,
            notificationEnabled =
                map["notificationEnabled"] as? Boolean ?: true,
            defaultReminderTime =
                LocalTime.parse(map["defaultReminderTime"] as String),
            theme = themeFromTag(map["theme"] as String),
            energyTrackingEnabled =
                map["energyTrackingEnabled"] as? Boolean ?: false,
            notificationChannels = channels,
            createdAt = (map["createdAt"] as Timestamp).toInstant(),
            updatedAt = (map["updatedAt"] as Timestamp).toInstant(),
        )
    }
}
