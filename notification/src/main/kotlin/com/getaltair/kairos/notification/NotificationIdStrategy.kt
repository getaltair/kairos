package com.getaltair.kairos.notification

import java.util.UUID

/**
 * Deterministic notification ID generation strategy.
 *
 * Each habit gets a deterministic base ID derived from its UUID's hashCode.
 * Collisions are theoretically possible but statistically unlikely for
 * typical habit counts. Follow-up and snoozed variants are offset from
 * this base so they can be independently cancelled.
 */
object NotificationIdStrategy {
    private const val FOLLOW_UP_OFFSET = 1000
    private const val SNOOZE_OFFSET = 500
    const val ROUTINE_TIMER_ID = 9001
    const val SYNC_STATUS_ID = 9002

    /** Base reminder notification ID for a habit. Always non-negative. */
    fun reminderId(habitId: UUID): Int = habitId.hashCode() and 0x7FFFFFFF

    /**
     * Notification ID for a persistent follow-up reminder. Always non-negative.
     *
     * @param followUpNumber 1-based follow-up index (1st, 2nd, 3rd, etc.)
     */
    fun followUpId(habitId: UUID, followUpNumber: Int): Int =
        (habitId.hashCode() and 0x7FFFFFFF) + FOLLOW_UP_OFFSET + followUpNumber

    /** Notification ID for a snoozed reminder. Always non-negative. */
    fun snoozedId(habitId: UUID): Int = (habitId.hashCode() and 0x7FFFFFFF) + SNOOZE_OFFSET
}
