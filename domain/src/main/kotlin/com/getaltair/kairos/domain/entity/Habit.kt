package com.getaltair.kairos.domain.entity

import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Core entity representing a habit.
 * The fundamental unit of behavior change in Kairos.
 *
 * @property id Unique identifier for this habit
 * @property name Display name (1-100 characters, required)
 * @property description Optional description of the habit
 * @property icon Emoji or icon reference for UI
 * @property color Hex color code for UI
 * @property anchorBehavior The context trigger (e.g., "After brushing teeth")
 * @property anchorType The type of anchor (after/before/location/time)
 * @property timeWindowStart Optional start time for AT_TIME anchor
 * @property timeWindowEnd Optional end time for AT_TIME anchor
 * @property category Time-of-day category for the habit
 * @property frequency How often the habit should be done
 * @property activeDays Set of days (for CUSTOM frequency)
 * @property estimatedSeconds Estimated duration in seconds (default 300)
 * @property microVersion Optional smaller habit version for flexibility
 * @property allowPartialCompletion Whether partial completion is allowed (always true)
 * @property subtasks Ordered list of subtasks
 * @property phase Current habit phase in lifecycle
 * @property status Current habit status (active/paused/archived)
 * @property createdAt When the habit was created
 * @property updatedAt When the habit was last updated
 * @property pausedAt When the habit was paused (nullable)
 * @property archivedAt When the habit was archived (nullable)
 * @property lapseThresholdDays Days missed before triggering lapse (default 3)
 * @property relapseThresholdDays Days missed before triggering relapse (default 7)
 */
data class Habit(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val anchorBehavior: String,
    val anchorType: AnchorType,
    val timeWindowStart: String? = null, // ISO time format "HH:mm"
    val timeWindowEnd: String? = null, // ISO time format "HH:mm"
    val category: HabitCategory,
    val frequency: HabitFrequency,
    val activeDays: Set<java.time.DayOfWeek>? = null, // For CUSTOM frequency
    val estimatedSeconds: Int = 300,
    val microVersion: String? = null,
    val allowPartialCompletion: Boolean = true,
    val subtasks: List<String>? = null,
    val phase: HabitPhase = HabitPhase.ONBOARD,
    val status: HabitStatus = HabitStatus.Active,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val pausedAt: Instant? = null,
    val archivedAt: Instant? = null,
    val lapseThresholdDays: Int = 3,
    val relapseThresholdDays: Int = 7
) {
    /**
     * Creates an updated copy preserving the original [id] and [createdAt].
     * The [updatedAt] timestamp defaults to now.
     * Excludes [id] and [createdAt] from parameters to prevent accidental identity changes.
     */
    fun copy(
        name: String = this.name,
        description: String? = this.description,
        icon: String? = this.icon,
        color: String? = this.color,
        anchorBehavior: String = this.anchorBehavior,
        anchorType: AnchorType = this.anchorType,
        timeWindowStart: String? = this.timeWindowStart,
        timeWindowEnd: String? = this.timeWindowEnd,
        category: HabitCategory = this.category,
        frequency: HabitFrequency = this.frequency,
        activeDays: Set<java.time.DayOfWeek>? = this.activeDays,
        estimatedSeconds: Int = this.estimatedSeconds,
        microVersion: String? = this.microVersion,
        allowPartialCompletion: Boolean = this.allowPartialCompletion,
        subtasks: List<String>? = this.subtasks,
        phase: HabitPhase = this.phase,
        status: HabitStatus = this.status,
        pausedAt: Instant? = this.pausedAt,
        archivedAt: Instant? = this.archivedAt,
        lapseThresholdDays: Int = this.lapseThresholdDays,
        relapseThresholdDays: Int = this.relapseThresholdDays,
        updatedAt: Instant = Instant.now()
    ): Habit = Habit(
        id = this.id,
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
        createdAt = this.createdAt,
        updatedAt = updatedAt,
        pausedAt = pausedAt,
        archivedAt = archivedAt,
        lapseThresholdDays = lapseThresholdDays,
        relapseThresholdDays = relapseThresholdDays
    )

    /**
     * Checks if this habit is currently active.
     * Active habits are visible on the Today screen and generate notifications.
     */
    val isActive: Boolean
        get() = status == HabitStatus.Active && pausedAt == null && archivedAt == null

    /**
     * Checks if this habit is in the DEPARTURE category.
     * Departure items are shown on the Pi dashboard, not the phone Today screen.
     */
    val isDeparture: Boolean
        get() = category == HabitCategory.Departure

    /**
     * Checks if this habit is due on the given date based on its frequency.
     * This is a simplified check -- actual frequency filtering should consider
     * the habit's phase and missed streaks as well.
     *
     * Weekdays and Weekends use fixed day sets; only Custom frequency consults [activeDays].
     *
     * @param today the date to check against; defaults to the current date
     */
    fun isDueToday(today: LocalDate = LocalDate.now()): Boolean = when (frequency) {
        is HabitFrequency.Daily -> true

        is HabitFrequency.Weekdays ->
            today.dayOfWeek in setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

        is HabitFrequency.Weekends ->
            today.dayOfWeek in setOf(SATURDAY, SUNDAY)

        is HabitFrequency.Custom ->
            activeDays?.contains(today.dayOfWeek) ?: false
    }
}
