package com.getaltair.kairos.domain.model

import java.time.LocalTime

/**
 * Represents a time window with start and end times.
 * Used for optional time constraints on habits (AT_TIME anchor type).
 */
data class TimeWindow(val start: LocalTime, val end: LocalTime) {
    /**
     * Checks if given time falls within this time window.
     * Handles wrap-around cases where end < start (overnight window).
     *
     * @param time The time to check
     * @return true if time is within window (inclusive), false otherwise
     */
    fun contains(time: LocalTime): Boolean = if (end < start) {
        // Overnight window: time is after start OR before end
        !time.isBefore(start) || !time.isAfter(end)
    } else {
        // Normal window: time is between start and end
        !time.isBefore(start) && !time.isAfter(end)
    }
}
