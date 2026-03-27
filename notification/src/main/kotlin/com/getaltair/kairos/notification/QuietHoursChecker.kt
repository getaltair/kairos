package com.getaltair.kairos.notification

import java.time.LocalTime

/**
 * Determines whether a given time falls within the user's quiet hours window.
 *
 * Quiet hours suppress non-critical notifications (habit reminders, recovery prompts)
 * while allowing user-initiated notifications (routine timer) to pass through.
 * Deferred notifications are delivered shortly after quiet hours end.
 */
class QuietHoursChecker {

    /**
     * Returns true if [now] falls within the quiet hours range [start, end).
     * Handles midnight-crossing ranges (e.g. 22:00 to 07:00).
     *
     * The start time is inclusive and the end time is exclusive,
     * so a notification scheduled exactly at the end of quiet hours will be delivered.
     */
    fun isInQuietHours(now: LocalTime, start: LocalTime, end: LocalTime): Boolean = if (start.isBefore(end)) {
        // Normal range: e.g. 23:00 to 23:30 (same day)
        !now.isBefore(start) && now.isBefore(end)
    } else {
        // Midnight-crossing range: e.g. 22:00 to 07:00
        !now.isBefore(start) || now.isBefore(end)
    }

    /**
     * Returns the time to deliver a deferred notification (the end of quiet hours).
     * Adds 1 minute buffer after quiet hours end so the notification fires
     * clearly outside the quiet window.
     */
    fun getNextDeliveryTime(quietHoursEnd: LocalTime): LocalTime = quietHoursEnd.plusMinutes(1)
}
