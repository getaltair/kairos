package com.getaltair.kairos.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.getaltair.kairos.notification.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * BroadcastReceiver that re-registers all AlarmManager alarms after device boot.
 *
 * AlarmManager alarms are cleared on reboot, so this receiver restores them
 * from the database to ensure habit reminders continue working.
 */
class BootReceiver :
    BroadcastReceiver(),
    KoinComponent {

    private val notificationScheduler: NotificationScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Timber.d("Boot completed; rescheduling all habit reminders")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationScheduler.rescheduleAll()
            } catch (e: Exception) {
                Timber.e(e, "BootReceiver: failed to reschedule alarms")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
