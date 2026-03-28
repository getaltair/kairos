package com.getaltair.kairos.dashboard.data

import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.enums.SkipReason
import com.google.cloud.Timestamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(AdminFirestoreMapper::class.java)

// ---------------------------------------------------------------------------
// Sealed-class tag conversions  (mirrors the tag values in sync/FirestoreMapper.kt but uses Admin SDK types and permissive defaults)
//
// Firestore stores every sealed-class value as an UPPER_SNAKE_CASE string.
// ---------------------------------------------------------------------------

// --- AnchorType ---

private fun anchorTypeFromTag(tag: String): AnchorType = when (tag) {
    "AFTER_BEHAVIOR" -> AnchorType.AfterBehavior

    "BEFORE_BEHAVIOR" -> AnchorType.BeforeBehavior

    "AT_LOCATION" -> AnchorType.AtLocation

    "AT_TIME" -> AnchorType.AtTime

    else -> {
        log.warn("Unrecognized AnchorType tag '{}', defaulting to AfterBehavior", tag)
        AnchorType.AfterBehavior
    }
}

// --- HabitCategory ---

private fun habitCategoryFromTag(tag: String): HabitCategory = when (tag) {
    "MORNING" -> HabitCategory.Morning

    "AFTERNOON" -> HabitCategory.Afternoon

    "EVENING" -> HabitCategory.Evening

    "ANYTIME" -> HabitCategory.Anytime

    "DEPARTURE" -> HabitCategory.Departure

    else -> {
        log.warn("Unrecognized HabitCategory tag '{}', defaulting to Anytime", tag)
        HabitCategory.Anytime
    }
}

// --- HabitFrequency ---

private fun habitFrequencyFromTag(tag: String): HabitFrequency = when (tag) {
    "DAILY" -> HabitFrequency.Daily

    "WEEKDAYS" -> HabitFrequency.Weekdays

    "WEEKENDS" -> HabitFrequency.Weekends

    "CUSTOM" -> HabitFrequency.Custom

    else -> {
        log.warn("Unrecognized HabitFrequency tag '{}', defaulting to Daily", tag)
        HabitFrequency.Daily
    }
}

// --- HabitPhase ---

private fun habitPhaseFromTag(tag: String): HabitPhase = when (tag) {
    "ONBOARD" -> HabitPhase.ONBOARD

    "FORMING" -> HabitPhase.FORMING

    "MAINTAINING" -> HabitPhase.MAINTAINING

    "LAPSED" -> HabitPhase.LAPSED

    "RELAPSED" -> HabitPhase.RELAPSED

    else -> {
        log.warn("Unrecognized HabitPhase tag '{}', defaulting to ONBOARD", tag)
        HabitPhase.ONBOARD
    }
}

// --- HabitStatus ---

private fun habitStatusFromTag(tag: String): HabitStatus = when (tag) {
    "ACTIVE" -> HabitStatus.Active

    "PAUSED" -> HabitStatus.Paused

    "ARCHIVED" -> HabitStatus.Archived

    else -> {
        log.warn("Unrecognized HabitStatus tag '{}', defaulting to Active", tag)
        HabitStatus.Active
    }
}

// --- CompletionType ---

private fun completionTypeFromTag(tag: String): CompletionType = when (tag) {
    "FULL" -> CompletionType.Full

    "PARTIAL" -> CompletionType.Partial

    "SKIPPED" -> CompletionType.Skipped

    "MISSED" -> CompletionType.Missed

    else -> {
        log.warn("Unrecognized CompletionType tag '{}', defaulting to Full", tag)
        CompletionType.Full
    }
}

// --- SkipReason ---

private fun skipReasonFromTag(tag: String): SkipReason = when (tag) {
    "TOO_TIRED" -> SkipReason.TooTired

    "NO_TIME" -> SkipReason.NoTime

    "NOT_FEELING_WELL" -> SkipReason.NotFeelingWell

    "TRAVELING" -> SkipReason.Traveling

    "TOOK_DAY_OFF" -> SkipReason.TookDayOff

    "OTHER" -> SkipReason.Other

    else -> {
        log.warn("Unrecognized SkipReason tag '{}', defaulting to Other", tag)
        SkipReason.Other
    }
}

// ---------------------------------------------------------------------------
// Timestamp helpers  (Admin SDK uses com.google.cloud.Timestamp)
// ---------------------------------------------------------------------------

/** Converts a Cloud [Timestamp] to a [java.time.Instant]. */
private fun Timestamp.toInstant(): java.time.Instant = java.time.Instant.ofEpochSecond(seconds, nanos.toLong())

/** Converts a [java.time.Instant] to a Cloud [Timestamp]. */
private fun java.time.Instant.toCloudTimestamp(): Timestamp = Timestamp.ofTimeSecondsAndNanos(epochSecond, nano)

// ---------------------------------------------------------------------------
// Domain -> Tag conversions  (for writing back to Firestore)
// ---------------------------------------------------------------------------

/** Converts a [CompletionType] to its Firestore UPPER_SNAKE_CASE tag. */
private fun completionTypeToTag(type: CompletionType): String = when (type) {
    is CompletionType.Full -> "FULL"
    is CompletionType.Partial -> "PARTIAL"
    is CompletionType.Skipped -> "SKIPPED"
    is CompletionType.Missed -> "MISSED"
}

// ---------------------------------------------------------------------------
// Firestore Map -> Domain Entity
// ---------------------------------------------------------------------------

/**
 * Reconstructs domain entities from Firestore Admin SDK document maps.
 *
 * Field names and tag values match the sync module's `FirestoreMapper`
 * exactly, but this mapper uses `com.google.cloud.Timestamp` (Admin SDK)
 * instead of `com.google.firebase.Timestamp` (Android SDK).
 *
 * Unlike the sync mapper, unknown enum tags fall back to sensible defaults
 * rather than throwing, because the dashboard is a read-only display that
 * should never crash on unexpected data.
 */
object AdminFirestoreMapper {

    /**
     * Reconstructs a [Habit] from a Firestore document [Map].
     *
     * @param id the document ID (used as the habit UUID)
     * @param map the document data
     */
    fun habitFromMap(id: String, map: Map<String, Any?>): Habit {
        @Suppress("UNCHECKED_CAST")
        val timeWindow = map["timeWindow"] as? Map<String, String>

        @Suppress("UNCHECKED_CAST")
        val activeDaysRaw = map["activeDays"] as? List<String>

        return Habit(
            id = UUID.fromString(id),
            name = map["name"] as? String ?: "",
            description = map["description"] as? String,
            icon = map["icon"] as? String,
            color = map["color"] as? String,
            anchorBehavior = map["anchorBehavior"] as? String ?: "",
            anchorType = anchorTypeFromTag(map["anchorType"] as? String ?: "AFTER_BEHAVIOR"),
            timeWindowStart = timeWindow?.get("start"),
            timeWindowEnd = timeWindow?.get("end"),
            category = habitCategoryFromTag(map["category"] as? String ?: "ANYTIME"),
            frequency = habitFrequencyFromTag(map["frequency"] as? String ?: "DAILY"),
            activeDays = activeDaysRaw?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }?.toSet(),
            estimatedSeconds = (map["estimatedSeconds"] as? Number)?.toInt() ?: 300,
            microVersion = map["microVersion"] as? String,
            allowPartialCompletion = map["allowPartial"] as? Boolean ?: true,
            subtasks = (map["subtasks"] as? List<*>)
                ?.filterIsInstance<String>()
                ?.ifEmpty { null },
            lapseThresholdDays = (map["lapseThresholdDays"] as? Number)?.toInt() ?: 3,
            relapseThresholdDays = (map["relapseThresholdDays"] as? Number)?.toInt() ?: 7,
            phase = habitPhaseFromTag(map["phase"] as? String ?: "ONBOARD"),
            status = habitStatusFromTag(map["status"] as? String ?: "ACTIVE"),
            createdAt = (map["createdAt"] as? Timestamp)?.toInstant() ?: java.time.Instant.now(),
            updatedAt = (map["updatedAt"] as? Timestamp)?.toInstant() ?: java.time.Instant.now(),
            pausedAt = (map["pausedAt"] as? Timestamp)?.toInstant(),
            archivedAt = (map["archivedAt"] as? Timestamp)?.toInstant(),
        )
    }

    /**
     * Converts a [Completion] to a Firestore-compatible [Map].
     *
     * Produces the exact same field set as `Completion.toFirestoreMap()` in
     * the sync module, but uses `com.google.cloud.Timestamp` (Admin SDK)
     * instead of `com.google.firebase.Timestamp` (Android SDK).
     *
     * @param id the document ID (completion UUID as string)
     * @param completion the domain completion entity
     */
    fun completionToMap(id: String, completion: Completion): Map<String, Any?> = mapOf(
        "id" to id,
        "habitId" to completion.habitId.toString(),
        "date" to completion.date.toString(), // YYYY-MM-DD
        "completedAt" to completion.completedAt.toCloudTimestamp(),
        "type" to completionTypeToTag(completion.type),
        "partialPercent" to completion.partialPercent,
        "skipReason" to null,
        "energyLevel" to completion.energyLevel,
        "note" to completion.note,
        "createdAt" to completion.createdAt.toCloudTimestamp(),
        "updatedAt" to completion.updatedAt.toCloudTimestamp(),
        "version" to System.currentTimeMillis(),
    )

    /**
     * Reconstructs a [Completion] from a Firestore document [Map].
     *
     * @param id the document ID (used as the completion UUID)
     * @param map the document data
     */
    fun completionFromMap(id: String, map: Map<String, Any?>): Completion {
        val typeTag = map["type"] as? String ?: "FULL"
        val type = completionTypeFromTag(typeTag)

        // updatedAt intentionally omitted -- not displayed on dashboard
        return Completion(
            id = UUID.fromString(id),
            habitId = UUID.fromString(
                map["habitId"] as? String
                    ?: error("Missing or non-string 'habitId' in completion doc $id"),
            ),
            date = LocalDate.parse(
                map["date"] as? String
                    ?: error("Missing or non-string 'date' in completion doc $id"),
            ),
            completedAt = (map["completedAt"] as? Timestamp)?.toInstant() ?: java.time.Instant.now(),
            type = type,
            partialPercent = if (type is CompletionType.Partial) {
                (map["partialPercent"] as? Number)?.toInt()?.coerceIn(1, 99)
            } else {
                null
            },
            skipReason = if (type is CompletionType.Skipped) {
                (map["skipReason"] as? String)?.let { skipReasonFromTag(it) }
            } else {
                null
            },
            energyLevel = (map["energyLevel"] as? Number)?.toInt()?.coerceIn(1, 5),
            note = map["note"] as? String,
            createdAt = (map["createdAt"] as? Timestamp)?.toInstant() ?: java.time.Instant.now(),
        )
    }
}
