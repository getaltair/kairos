package com.getaltair.kairos.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.getaltair.kairos.notification.HabitReminderBuilder
import com.getaltair.kairos.notification.NotificationConstants
import com.getaltair.kairos.notification.ReminderHandler
import java.util.UUID
import kotlinx.coroutines.CancellationException
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

    private val reminderHandler: ReminderHandler by inject()

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
        val followUpNumber = intent.getIntExtra(NotificationConstants.EXTRA_FOLLOW_UP_NUMBER, 0)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reminderHandler.handle(context, habitId, followUpNumber)
            } catch (e: SecurityException) {
                Timber.e(e, "SecurityException for habit %s; exact alarm permission may be revoked", habitId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "ReminderAlarmReceiver: error handling alarm for %s", habitId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
