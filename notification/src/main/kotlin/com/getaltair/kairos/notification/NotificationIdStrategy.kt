package com.getaltair.kairos.notification

import java.util.UUID

/**
 * Deterministic notification ID generation strategy.
 *
 * Each habit gets a unique base ID derived from its UUID. Follow-up and
 * snoozed variants are offset from this base so they can be independently
 * cancelled without affecting the original reminder notification.
 */
object NotificationIdStrategy {
    private const val FOLLOW_UP_OFFSET = 1000
    private const val SNOOZE_OFFSET = 500
    const val ROUTINE_TIMER_ID = 9001
    const val SYNC_STATUS_ID = 9002

    /** Base reminder notification ID for a habit. */
    fun reminderId(habitId: UUID): Int = habitId.hashCode()

    /**
     * Notification ID for a persistent follow-up reminder.
     *
     * @param followUpNumber 1-based follow-up index (1st, 2nd, 3rd, etc.)
     */
    fun followUpId(habitId: UUID, followUpNumber: Int): Int = habitId.hashCode() + FOLLOW_UP_OFFSET + followUpNumber

    /** Notification ID for a snoozed reminder. */
    fun snoozedId(habitId: UUID): Int = habitId.hashCode() + SNOOZE_OFFSET
}
