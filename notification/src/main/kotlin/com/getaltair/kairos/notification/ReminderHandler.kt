package com.getaltair.kairos.notification

import android.app.NotificationManager
import android.content.Context
import com.getaltair.kairos.data.dao.HabitNotificationDao
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.PreferencesRepository
import java.time.LocalTime
import java.util.UUID
import timber.log.Timber

/**
 * Result of handling a reminder alarm.
 *
 * Used to communicate the outcome back to the receiver for logging,
 * and to enable deterministic testing without Android framework coupling.
 */
sealed class ReminderResult {
    data object NotificationPosted : ReminderResult()
    data object NotificationsDisabled : ReminderResult()
    data object DeferredToQuietHoursEnd : ReminderResult()
    data class HabitNotFound(val message: String) : ReminderResult()
    data class PreferencesError(val message: String) : ReminderResult()
}

/**
 * Handles the core logic of a reminder alarm: preference checks,
 * quiet hours deferral, notification posting, follow-up scheduling,
 * and next-day re-scheduling.
 *
 * Extracted from [com.getaltair.kairos.notification.receiver.ReminderAlarmReceiver]
 * so the logic can be unit-tested without Android BroadcastReceiver coupling.
 */
class ReminderHandler(
    private val preferencesRepository: PreferencesRepository,
    private val habitRepository: HabitRepository,
    private val habitNotificationDao: HabitNotificationDao,
    private val habitReminderBuilder: HabitReminderBuilder,
    private val notificationScheduler: NotificationScheduler,
    private val quietHoursChecker: QuietHoursChecker,
) {

    /**
     * Processes a reminder alarm for the given habit.
     *
     * @param context Android context for obtaining the [NotificationManager]
     * @param habitId the habit to remind about
     * @param followUpNumber 0 for initial reminder, 1..MAX_FOLLOW_UPS for follow-ups
     * @return the outcome of the handling for logging and testing
     */
    suspend fun handle(context: Context, habitId: UUID, followUpNumber: Int,): ReminderResult {
        // Check global notification preference
        val prefsResult = preferencesRepository.get()
        if (prefsResult is Result.Error) {
            Timber.e("Failed to load preferences: %s", prefsResult.message)
            return ReminderResult.PreferencesError(prefsResult.message)
        }
        val prefs = (prefsResult as Result.Success).value

        if (!prefs.notificationEnabled) {
            Timber.d("Notifications globally disabled; skipping reminder for %s", habitId)
            return ReminderResult.NotificationsDisabled
        }

        // Check quiet hours
        if (prefs.quietHoursEnabled) {
            val now = LocalTime.now()
            if (quietHoursChecker.isInQuietHours(now, prefs.quietHoursStart, prefs.quietHoursEnd)) {
                val deliveryTime = quietHoursChecker.getNextDeliveryTime(prefs.quietHoursEnd)
                Timber.d("In quiet hours; deferring reminder for %s to %s", habitId, deliveryTime)
                notificationScheduler.scheduleAtTime(habitId, deliveryTime)
                return ReminderResult.DeferredToQuietHoursEnd
            }
        }

        // Fetch the habit
        val habitResult = habitRepository.getById(habitId)
        if (habitResult is Result.Error) {
            Timber.e("Habit not found for reminder: %s", habitResult.message)
            return ReminderResult.HabitNotFound(habitResult.message)
        }
        val habit = (habitResult as Result.Success).value

        // Build and post notification
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (followUpNumber > 0) {
            val notifId = NotificationIdStrategy.followUpId(habitId, followUpNumber)
            val notification = habitReminderBuilder.buildFollowUp(
                habit,
                followUpNumber,
                notifId
            )
            notifManager.notify(notifId, notification)
            Timber.d("Posted follow-up %d for habit %s", followUpNumber, habitId)

            // Schedule next follow-up if applicable
            val notifEntity = habitNotificationDao.getForHabit(habitId)
            if (notifEntity != null && notifEntity.isPersistent) {
                val nextFollowUp = followUpNumber + 1
                if (nextFollowUp <= notifEntity.maxFollowUps) {
                    notificationScheduler.scheduleFollowUp(habitId, nextFollowUp)
                }
            }
        } else {
            val notifId = NotificationIdStrategy.reminderId(habitId)
            val notification = habitReminderBuilder.buildReminder(habit, notifId)
            notifManager.notify(notifId, notification)
            Timber.d("Posted initial reminder for habit %s", habitId)

            // If persistent reminders are enabled, schedule first follow-up
            val notifEntity = habitNotificationDao.getForHabit(habitId)
            if (notifEntity != null && notifEntity.isPersistent) {
                notificationScheduler.scheduleFollowUp(habitId, 1)
            }

            // Re-schedule for tomorrow
            val rescheduleEntity = notifEntity ?: habitNotificationDao.getForHabit(habitId)
            if (rescheduleEntity != null && rescheduleEntity.isEnabled) {
                notificationScheduler.scheduleReminder(habitId, rescheduleEntity.time)
            }
        }

        return ReminderResult.NotificationPosted
    }
}
