package com.getaltair.kairos.data.converter

import androidx.room.TypeConverter
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
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import timber.log.Timber

/**
 * Room type converters collection.
 * Each converter method is annotated with @TypeConverter for Room to discover them.
 */
@Suppress("unused")
object RoomTypeConverters {

    private val moshi: Moshi = Moshi.Builder().build()

    private val uuidListAdapter: JsonAdapter<List<UUID>> by lazy {
        val type = Types.newParameterizedType(List::class.java, UUID::class.java)
        moshi.adapter(type)
    }

    private val stringListAdapter: JsonAdapter<List<String>> by lazy {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        moshi.adapter(type)
    }

    private val mapAdapter: JsonAdapter<Map<String, Any>> by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter(type)
    }

    private val blockerListAdapter: JsonAdapter<List<Blocker>> by lazy {
        val type = Types.newParameterizedType(List::class.java, Blocker::class.java)
        moshi.adapter(type)
    }

    // ==================== Instant Converter ====================

    @TypeConverter
    fun instantToLong(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun longToInstant(timestamp: Long?): Instant? = timestamp?.let { Instant.ofEpochMilli(it) }

    // ==================== LocalDate Converter ====================

    private val dateFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.format(dateFormatter)

    @TypeConverter
    fun stringToLocalDate(dateString: String?): LocalDate? = dateString?.let {
        LocalDate.parse(it, dateFormatter)
    }

    // ==================== LocalTime Converter ====================

    private val timeFormatter = java.time.format.DateTimeFormatter.ISO_LOCAL_TIME

    @TypeConverter
    fun localTimeToString(time: LocalTime?): String? = time?.format(timeFormatter)

    @TypeConverter
    fun stringToLocalTime(timeString: String?): LocalTime? = timeString?.let {
        LocalTime.parse(it, timeFormatter)
    }

    // ==================== DayOfWeek Set Converter ====================

    @TypeConverter
    fun dayOfWeekSetToString(days: Set<java.time.DayOfWeek>?): String? = days?.map { it.name }?.joinToString(",")

    @TypeConverter
    fun stringToDayOfWeekSet(daysString: String?): Set<java.time.DayOfWeek>? {
        if (daysString.isNullOrBlank()) return null
        return daysString.split(",")
            .mapNotNull { it.trim().takeIf { it.isNotBlank() } }
            .mapNotNull { dayName ->
                try {
                    java.time.DayOfWeek.valueOf(dayName)
                } catch (e: IllegalArgumentException) {
                    Timber.w("Invalid day name: $dayName, skipping")
                    null
                }
            }
            .toSet()
    }

    // ==================== UUID List Converter ====================

    @TypeConverter
    fun uuidListToString(list: List<UUID>?): String? = list?.let { uuidListAdapter.toJson(it) }

    @TypeConverter
    fun stringToUuidList(json: String?): List<UUID>? {
        if (json.isNullOrBlank()) return null
        return try {
            uuidListAdapter.fromJson(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse UUID list from JSON: $json")
            null
        }
    }

    // ==================== String List Converter ====================

    @TypeConverter
    fun stringListToString(list: List<String>?): String? = list?.let { stringListAdapter.toJson(it) }

    @TypeConverter
    fun stringToStringList(json: String?): List<String>? {
        if (json.isNullOrBlank()) return null
        return try {
            stringListAdapter.fromJson(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse string list from JSON: $json")
            null
        }
    }

    // ==================== Map Converter ====================

    @TypeConverter
    fun mapToString(map: Map<String, Any>?): String? = map?.let { mapAdapter.toJson(it) }

    @TypeConverter
    fun stringToMap(json: String?): Map<String, Any>? {
        if (json.isNullOrBlank()) return null
        return try {
            mapAdapter.fromJson(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse map from JSON: $json")
            null
        }
    }

    // ==================== Blocker List Converter ====================

    @TypeConverter
    fun blockerListToString(blockers: List<Blocker>?): String? = blockers?.let { blockerListAdapter.toJson(it) }

    @TypeConverter
    fun stringToBlockerList(json: String?): List<Blocker>? {
        if (json.isNullOrBlank()) return null
        return try {
            blockerListAdapter.fromJson(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse blocker list from JSON: $json")
            null
        }
    }

    // ==================== Enum Converters (Sealed Class Objects) ====================

    // AnchorType
    @TypeConverter
    fun anchorTypeToString(anchorType: AnchorType?): String? = anchorType?.javaClass?.simpleName

    @TypeConverter
    fun stringToAnchorType(name: String?): AnchorType? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "AfterBehavior" -> AnchorType.AfterBehavior
            "BeforeBehavior" -> AnchorType.BeforeBehavior
            "AtLocation" -> AnchorType.AtLocation
            "AtTime" -> AnchorType.AtTime
            else -> null
        }
    }

    // HabitCategory
    @TypeConverter
    fun habitCategoryToString(category: HabitCategory?): String? = category?.javaClass?.simpleName

    @TypeConverter
    fun stringToHabitCategory(name: String?): HabitCategory? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Morning" -> HabitCategory.Morning
            "Afternoon" -> HabitCategory.Afternoon
            "Evening" -> HabitCategory.Evening
            "Anytime" -> HabitCategory.Anytime
            "Departure" -> HabitCategory.Departure
            else -> null
        }
    }

    // HabitFrequency
    @TypeConverter
    fun habitFrequencyToString(frequency: HabitFrequency?): String? = frequency?.javaClass?.simpleName

    @TypeConverter
    fun stringToHabitFrequency(name: String?): HabitFrequency? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Daily" -> HabitFrequency.Daily
            "Weekdays" -> HabitFrequency.Weekdays
            "Weekends" -> HabitFrequency.Weekends
            "Custom" -> HabitFrequency.Custom
            else -> null
        }
    }

    // HabitPhase (using fromSimpleName)
    @TypeConverter
    fun habitPhaseToString(phase: HabitPhase?): String? = phase?.javaClass?.simpleName

    @TypeConverter
    fun stringToHabitPhase(name: String?): HabitPhase? {
        if (name.isNullOrBlank()) return null
        return HabitPhase.fromSimpleName(name)
    }

    // HabitStatus
    @TypeConverter
    fun habitStatusToString(status: HabitStatus?): String? = status?.javaClass?.simpleName

    @TypeConverter
    fun stringToHabitStatus(name: String?): HabitStatus? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Active" -> HabitStatus.Active
            "Paused" -> HabitStatus.Paused
            "Archived" -> HabitStatus.Archived
            else -> null
        }
    }

    // CompletionType
    @TypeConverter
    fun completionTypeToString(type: CompletionType?): String? = type?.javaClass?.simpleName

    @TypeConverter
    fun stringToCompletionType(name: String?): CompletionType? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Full" -> CompletionType.Full
            "Partial" -> CompletionType.Partial
            "Skipped" -> CompletionType.Skipped
            "Missed" -> CompletionType.Missed
            else -> null
        }
    }

    // SkipReason
    @TypeConverter
    fun skipReasonToString(reason: SkipReason?): String? = reason?.javaClass?.simpleName

    @TypeConverter
    fun stringToSkipReason(name: String?): SkipReason? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "TooTired" -> SkipReason.TooTired
            "NoTime" -> SkipReason.NoTime
            "NotFeelingWell" -> SkipReason.NotFeelingWell
            "Traveling" -> SkipReason.Traveling
            "TookDayOff" -> SkipReason.TookDayOff
            "Other" -> SkipReason.Other
            else -> null
        }
    }

    // RoutineStatus
    @TypeConverter
    fun routineStatusToString(status: RoutineStatus?): String? = status?.javaClass?.simpleName

    @TypeConverter
    fun stringToRoutineStatus(name: String?): RoutineStatus? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Active" -> RoutineStatus.Active
            "Paused" -> RoutineStatus.Paused
            "Archived" -> RoutineStatus.Archived
            else -> null
        }
    }

    // ExecutionStatus
    @TypeConverter
    fun executionStatusToString(status: ExecutionStatus?): String? = status?.javaClass?.simpleName

    @TypeConverter
    fun stringToExecutionStatus(name: String?): ExecutionStatus? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "NotStarted" -> ExecutionStatus.NotStarted
            "InProgress" -> ExecutionStatus.InProgress
            "Paused" -> ExecutionStatus.Paused
            "Completed" -> ExecutionStatus.Completed
            "Abandoned" -> ExecutionStatus.Abandoned
            else -> null
        }
    }

    // RecoveryType
    @TypeConverter
    fun recoveryTypeToString(type: RecoveryType?): String? = type?.javaClass?.simpleName

    @TypeConverter
    fun stringToRecoveryType(name: String?): RecoveryType? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Lapse" -> RecoveryType.Lapse
            "Relapse" -> RecoveryType.Relapse
            else -> null
        }
    }

    // SessionStatus
    @TypeConverter
    fun sessionStatusToString(status: SessionStatus?): String? = status?.javaClass?.simpleName

    @TypeConverter
    fun stringToSessionStatus(name: String?): SessionStatus? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Pending" -> SessionStatus.Pending
            "Completed" -> SessionStatus.Completed
            "Abandoned" -> SessionStatus.Abandoned
            else -> null
        }
    }

    // RecoveryAction
    @TypeConverter
    fun recoveryActionToString(action: RecoveryAction?): String? = action?.javaClass?.simpleName

    @TypeConverter
    fun stringToRecoveryAction(name: String?): RecoveryAction? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "Resume" -> RecoveryAction.Resume
            "Simplify" -> RecoveryAction.Simplify
            "Pause" -> RecoveryAction.Pause
            "Archive" -> RecoveryAction.Archive
            "FreshStart" -> RecoveryAction.FreshStart
            else -> null
        }
    }

    // Theme
    @TypeConverter
    fun themeToString(theme: Theme?): String? = theme?.javaClass?.simpleName

    @TypeConverter
    fun stringToTheme(name: String?): Theme? {
        if (name.isNullOrBlank()) return null
        return when (name) {
            "System" -> Theme.System
            "Light" -> Theme.Light
            "Dark" -> Theme.Dark
            else -> null
        }
    }
}
