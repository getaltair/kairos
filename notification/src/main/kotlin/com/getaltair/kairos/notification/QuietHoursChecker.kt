package com.getaltair.kairos.notification

import java.time.LocalTime

/**
 * Checks whether the current time falls within the configured quiet hours window.
 *
 * Kept as a class (rather than an object) to allow injection via Koin
 * and mocking in tests.
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
     *
     * @throws IllegalArgumentException if [start] equals [end]
     */
    fun isInQuietHours(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        require(start != end) { "Quiet hours start and end must differ; both are $start" }
        return if (start.isBefore(end)) {
            // Same-day range: e.g. 09:00 to 17:00
            !now.isBefore(start) && now.isBefore(end)
        } else {
            // Midnight-crossing range: e.g. 22:00 to 07:00
            !now.isBefore(start) || now.isBefore(end)
        }
    }

    /**
     * Returns the delivery time just after quiet hours end.
     *
     * The caller ([NotificationScheduler.scheduleAtTime]) adds a day if this
     * time has already passed today, so midnight-crossing cases are handled
     * there rather than here.
     */
    fun getNextDeliveryTime(quietHoursEnd: LocalTime): LocalTime = quietHoursEnd.plusMinutes(1)
}
