package com.getaltair.kairos.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Defines and creates all notification channels for the Kairos app.
 *
 * Channels are created at app startup so the system knows about them
 * before any notification is posted. Users can customize channel behavior
 * (sound, vibration, importance) through system settings.
 */
object NotificationChannels {
    const val CHANNEL_HABIT_REMINDERS = "habit_reminders"
    const val CHANNEL_RECOVERY = "recovery"
    const val CHANNEL_ROUTINE_TIMER = "routine_timer"
    const val CHANNEL_SYSTEM = "system"

    /**
     * Creates all notification channels.
     * Safe to call multiple times; the system ignores re-creation of existing channels.
     */
    fun createAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val habitReminders = NotificationChannel(
            CHANNEL_HABIT_REMINDERS,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for your scheduled habits"
        }

        val recovery = NotificationChannel(
            CHANNEL_RECOVERY,
            "Recovery",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Recovery check-ins and encouragement"
        }

        val routineTimer = NotificationChannel(
            CHANNEL_ROUTINE_TIMER,
            "Routine Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Active routine timer progress"
        }

        val system = NotificationChannel(
            CHANNEL_SYSTEM,
            "System",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "System notifications and sync status"
        }

        manager.createNotificationChannels(listOf(habitReminders, recovery, routineTimer, system))
    }
}
