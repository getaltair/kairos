package com.getaltair.kairos.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.getaltair.kairos.data.dao.HabitNotificationDao
import com.getaltair.kairos.notification.receiver.ReminderAlarmReceiver
import java.time.LocalTime
import java.util.Calendar
import java.util.UUID
import timber.log.Timber

/**
 * AlarmManager-based scheduler for habit reminder notifications.
 *
 * Uses exact alarms ([AlarmManager.setExactAndAllowWhileIdle]) for precise timing.
 * Min SDK 31 guarantees API availability without version checks.
 */
class NotificationScheduler(private val context: Context, private val dao: HabitNotificationDao) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules an exact alarm for a habit at [time] today (or tomorrow if [time] has passed).
     */
    fun scheduleReminder(habitId: UUID, time: LocalTime) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pendingIntent = buildAlarmIntent(habitId, NotificationIdStrategy.reminderId(habitId))
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
        Timber.d("Scheduled reminder for habit %s at %s", habitId, time)
    }

    /**
     * Cancels any pending alarm for the given habit.
     */
    fun cancelReminder(habitId: UUID) {
        val pendingIntent = buildAlarmIntent(habitId, NotificationIdStrategy.reminderId(habitId))
        alarmManager.cancel(pendingIntent)
        Timber.d("Cancelled reminder for habit %s", habitId)
    }

    /**
     * Schedules a one-time snooze alarm [delayMinutes] from now.
     */
    fun scheduleSnooze(habitId: UUID, delayMinutes: Int = 10) {
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        val pendingIntent = buildAlarmIntent(habitId, NotificationIdStrategy.snoozedId(habitId))
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
        Timber.d("Scheduled snooze for habit %s in %d min", habitId, delayMinutes)
    }

    /**
     * Schedules a persistent follow-up alarm.
     *
     * Delays by follow-up number:
     * - 1: 15 minutes from now
     * - 2: 30 minutes from now
     * - 3: 60 minutes from now
     *
     * @param followUpNumber 1-based follow-up index
     */
    fun scheduleFollowUp(habitId: UUID, followUpNumber: Int) {
        val delayMinutes = when (followUpNumber) {
            1 -> 15
            2 -> 30
            3 -> 60
            else -> return
        }
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        val requestCode = NotificationIdStrategy.followUpId(habitId, followUpNumber)
        val pendingIntent = buildFollowUpAlarmIntent(habitId, followUpNumber, requestCode)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
        Timber.d(
            "Scheduled follow-up %d for habit %s in %d min",
            followUpNumber,
            habitId,
            delayMinutes
        )
    }

    /**
     * Cancels all follow-up alarms (numbers 1, 2, 3) for a habit.
     */
    fun cancelFollowUps(habitId: UUID) {
        for (number in 1..3) {
            val requestCode = NotificationIdStrategy.followUpId(habitId, number)
            val pendingIntent = buildFollowUpAlarmIntent(habitId, number, requestCode)
            alarmManager.cancel(pendingIntent)
        }
        Timber.d("Cancelled all follow-ups for habit %s", habitId)
    }

    /**
     * Re-registers alarms for all enabled notifications from the database.
     * Typically called after device boot or app update.
     */
    suspend fun rescheduleAll() {
        val notifications = dao.getEnabled()
        notifications.forEach { notif ->
            scheduleReminder(notif.habitId, notif.time)
        }
        Timber.d("Rescheduled %d habit reminders", notifications.size)
    }

    /**
     * Schedules an alarm at a specific [LocalTime] for today (used for quiet-hours deferral).
     * If the time has already passed today the alarm fires immediately.
     */
    fun scheduleAtTime(habitId: UUID, time: LocalTime) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val pendingIntent = buildAlarmIntent(habitId, NotificationIdStrategy.reminderId(habitId))
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
        Timber.d("Scheduled deferred reminder for habit %s at %s", habitId, time)
    }

    // -- Private helpers --

    private fun buildAlarmIntent(habitId: UUID, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(HabitReminderBuilder.EXTRA_HABIT_ID, habitId.toString())
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildFollowUpAlarmIntent(habitId: UUID, followUpNumber: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(HabitReminderBuilder.EXTRA_HABIT_ID, habitId.toString())
            putExtra(EXTRA_FOLLOW_UP_NUMBER, followUpNumber)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_FOLLOW_UP_NUMBER = "follow_up_number"
    }
}
