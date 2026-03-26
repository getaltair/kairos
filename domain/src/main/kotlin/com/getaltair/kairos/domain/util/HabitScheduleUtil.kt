package com.getaltair.kairos.domain.util

import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitFrequency
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Shared utility for determining habit schedule applicability.
 *
 * Centralizes the logic for checking whether a habit is due on a given day
 * and counting due days within a date range, so that repositories and use
 * cases share a single source of truth.
 */
object HabitScheduleUtil {

    val WEEKDAYS: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    val WEEKENDS: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    /**
     * Checks whether [habit] is due on the given [dayOfWeek] based on its frequency.
     *
     * - [HabitFrequency.Daily]: always due.
     * - [HabitFrequency.Weekdays]: due Monday through Friday.
     * - [HabitFrequency.Weekends]: due Saturday and Sunday.
     * - [HabitFrequency.Custom]: due if [dayOfWeek] is in [Habit.activeDays].
     */
    fun isDueOnDate(habit: Habit, dayOfWeek: DayOfWeek): Boolean = when (habit.frequency) {
        is HabitFrequency.Daily -> true
        is HabitFrequency.Weekdays -> dayOfWeek in WEEKDAYS
        is HabitFrequency.Weekends -> dayOfWeek in WEEKENDS
        is HabitFrequency.Custom -> habit.activeDays?.contains(dayOfWeek) ?: false
    }

    /**
     * Counts how many days in the inclusive range [start]..[end] the [habit] is due,
     * according to its frequency and active days.
     */
    fun countDueDays(habit: Habit, start: LocalDate, end: LocalDate): Int {
        var count = 0
        var date = start
        while (!date.isAfter(end)) {
            if (isDueOnDate(habit, date.dayOfWeek)) {
                count++
            }
            date = date.plusDays(1)
        }
        return count
    }
}
