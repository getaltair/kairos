package com.getaltair.kairos.notification

import android.annotation.SuppressLint
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
 * Requires [android.Manifest.permission.SCHEDULE_EXACT_ALARM] permission.
 * All scheduling methods check [AlarmManager.canScheduleExactAlarms] before
 * setting alarms and return [ScheduleResult] to indicate success or denial.
 *
 * The app-level minSdk is 31, so all API-31 calls are safe at runtime even
 * though the library convention plugin sets minSdk = 28.
 */
@SuppressLint("NewApi")
class NotificationScheduler(private val context: Context, private val dao: HabitNotificationDao) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules an exact alarm for a habit at [time] today (or tomorrow if [time] has passed).
     */
    fun scheduleReminder(habitId: UUID, time: LocalTime): ScheduleResult {
        if (!alarmManager.canScheduleExactAlarms()) {
            Timber.w("Cannot schedule exact alarms; permission revoked for habit %s", habitId)
            return ScheduleResult.ExactAlarmDenied
        }
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
        return ScheduleResult.Scheduled
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
    fun scheduleSnooze(habitId: UUID, delayMinutes: Int = 10): ScheduleResult {
        require(delayMinutes > 0) { "delayMinutes must be positive, was $delayMinutes" }
        if (!alarmManager.canScheduleExactAlarms()) {
            Timber.w("Cannot schedule exact alarms; permission revoked for habit %s", habitId)
            return ScheduleResult.ExactAlarmDenied
        }
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60_000L
        val pendingIntent = buildAlarmIntent(habitId, NotificationIdStrategy.snoozedId(habitId))
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
        Timber.d("Scheduled snooze for habit %s in %d min", habitId, delayMinutes)
        return ScheduleResult.Scheduled
    }

    /**
     * Schedules a persistent follow-up alarm.
     *
     * The delay increases with each follow-up number (see implementation
     * for current values).
     *
     * @param followUpNumber 1-based follow-up index (1..MAX_FOLLOW_UPS)
     */
    fun scheduleFollowUp(habitId: UUID, followUpNumber: Int): ScheduleResult {
        require(followUpNumber in 1..NotificationConstants.MAX_FOLLOW_UPS) {
            "followUpNumber must be in 1..${NotificationConstants.MAX_FOLLOW_UPS}, was $followUpNumber"
        }
        if (!alarmManager.canScheduleExactAlarms()) {
            Timber.w("Cannot schedule exact alarms; permission revoked for habit %s", habitId)
            return ScheduleResult.ExactAlarmDenied
        }
        val delayMinutes = when (followUpNumber) {
            1 -> 15
            2 -> 30
            3 -> 60
            else -> return ScheduleResult.Scheduled
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
        return ScheduleResult.Scheduled
    }

    /**
     * Cancels all follow-up alarms (numbers 1, 2, 3) for a habit.
     */
    fun cancelFollowUps(habitId: UUID) {
        for (number in 1..NotificationConstants.MAX_FOLLOW_UPS) {
            val requestCode = NotificationIdStrategy.followUpId(habitId, number)
            val pendingIntent = buildFollowUpAlarmIntent(habitId, number, requestCode)
            alarmManager.cancel(pendingIntent)
        }
        Timber.d("Cancelled all follow-ups for habit %s", habitId)
    }

    /**
     * Re-registers alarms for all enabled notifications from the database.
     * Called after device boot via [BootReceiver].
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
     *
     * Unlike [scheduleReminder], this method is for one-shot deferred delivery
     * and does not represent a recurring daily alarm. If the time has already
     * passed today, the alarm is scheduled for tomorrow.
     */
    fun scheduleAtTime(habitId: UUID, time: LocalTime): ScheduleResult {
        if (!alarmManager.canScheduleExactAlarms()) {
            Timber.w("Cannot schedule exact alarms; permission revoked for habit %s", habitId)
            return ScheduleResult.ExactAlarmDenied
        }
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
        Timber.d("Scheduled deferred reminder for habit %s at %s", habitId, time)
        return ScheduleResult.Scheduled
    }

    // -- Private helpers --

    private fun buildAlarmIntent(habitId: UUID, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(NotificationConstants.EXTRA_HABIT_ID, habitId.toString())
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
            putExtra(NotificationConstants.EXTRA_HABIT_ID, habitId.toString())
            putExtra(NotificationConstants.EXTRA_FOLLOW_UP_NUMBER, followUpNumber)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_FOLLOW_UP_NUMBER = NotificationConstants.EXTRA_FOLLOW_UP_NUMBER
    }
}
