package com.getaltair.kairos.domain.model

import java.time.LocalDate

/**
 * Represents a date range with start and end dates.
 * Used for reporting and analytics queries.
 */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    companion object {
        /**
         * Creates a DateRange for last N days.
         */
        fun lastDays(days: Int): DateRange {
            val end = LocalDate.now()
            val start = end.minusDays(days.toLong())
            return DateRange(start = start, end = end)
        }

        /**
         * Creates a DateRange for this week (Monday to Sunday).
         */
        fun thisWeek(): DateRange {
            val now = LocalDate.now()
            val start = now.minusDays(now.dayOfWeek.value - 1L)
            val end = start.plusDays(6)
            return DateRange(start = start, end = end)
        }

        /**
         * Creates a DateRange for this month.
         */
        fun thisMonth(): DateRange {
            val now = LocalDate.now()
            val start = now.withDayOfMonth(1)
            val end = start.withDayOfMonth(start.lengthOfMonth())
            return DateRange(start = start, end = end)
        }
    }

    /**
     * Checks if given date falls within this date range.
     *
     * @param date The date to check
     * @return true if date is within range (inclusive), false otherwise
     */
    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    /**
     * Calculates number of days in this range.
     *
     * @return day count (inclusive of both start and end)
     */
    fun dayCount(): Int = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1

    /**
     * Checks if this date range is empty.
     *
     * @return true if start is after end, false otherwise
     */
    fun isEmpty(): Boolean = start.isAfter(end)
}
