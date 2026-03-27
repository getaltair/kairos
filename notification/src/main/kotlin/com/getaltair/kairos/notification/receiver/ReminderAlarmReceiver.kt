package com.getaltair.kairos.notification.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.getaltair.kairos.data.dao.HabitNotificationDao
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.PreferencesRepository
import com.getaltair.kairos.notification.HabitReminderBuilder
import com.getaltair.kairos.notification.NotificationIdStrategy
import com.getaltair.kairos.notification.NotificationScheduler
import com.getaltair.kairos.notification.QuietHoursChecker
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * BroadcastReceiver triggered by AlarmManager for habit reminders.
 *
 * Handles both initial reminders and persistent follow-ups.
 * Respects quiet hours by deferring delivery to the end of the quiet window.
 */
class ReminderAlarmReceiver :
    BroadcastReceiver(),
    KoinComponent {

    private val quietHoursChecker: QuietHoursChecker by inject()
    private val preferencesRepository: PreferencesRepository by inject()
    private val habitRepository: HabitRepository by inject()
    private val habitNotificationDao: HabitNotificationDao by inject()
    private val habitReminderBuilder: HabitReminderBuilder by inject()
    private val notificationScheduler: NotificationScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val habitIdString = intent.getStringExtra(HabitReminderBuilder.EXTRA_HABIT_ID) ?: run {
            Timber.w("ReminderAlarmReceiver: missing habit_id extra")
            return
        }
        val habitId = try {
            UUID.fromString(habitIdString)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "ReminderAlarmReceiver: invalid habit_id: %s", habitIdString)
            return
        }
        val followUpNumber = intent.getIntExtra(NotificationScheduler.EXTRA_FOLLOW_UP_NUMBER, 0)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleAlarm(context, habitId, followUpNumber)
            } catch (e: Exception) {
                Timber.e(e, "ReminderAlarmReceiver: error handling alarm for %s", habitId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAlarm(context: Context, habitId: UUID, followUpNumber: Int) {
        // Check global notification preference
        val prefsResult = preferencesRepository.get()
        if (prefsResult is Result.Error) {
            Timber.e("Failed to load preferences: %s", prefsResult.message)
            return
        }
        val prefs = (prefsResult as Result.Success).value

        if (!prefs.notificationEnabled) {
            Timber.d("Notifications globally disabled; skipping reminder for %s", habitId)
            return
        }

        // Check quiet hours
        if (prefs.quietHoursEnabled) {
            val now = LocalTime.now()
            if (quietHoursChecker.isInQuietHours(now, prefs.quietHoursStart, prefs.quietHoursEnd)) {
                val deliveryTime = quietHoursChecker.getNextDeliveryTime(prefs.quietHoursEnd)
                Timber.d("In quiet hours; deferring reminder for %s to %s", habitId, deliveryTime)
                notificationScheduler.scheduleAtTime(habitId, deliveryTime)
                return
            }
        }

        // Fetch the habit
        val habitResult = habitRepository.getById(habitId)
        if (habitResult is Result.Error) {
            Timber.e("Habit not found for reminder: %s", habitResult.message)
            return
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
        }
    }
}
