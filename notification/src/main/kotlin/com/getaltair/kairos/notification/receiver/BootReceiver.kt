package com.getaltair.kairos.notification.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
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
 * AlarmManager alarms (including exact alarms requiring SCHEDULE_EXACT_ALARM)
 * are cleared on reboot, so this receiver restores them from the database
 * to ensure habit reminders continue working.
 */
class BootReceiver :
    BroadcastReceiver(),
    KoinComponent {

    private val notificationScheduler: NotificationScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != ACTION_RETRY_RESCHEDULE) return

        Timber.d("Boot completed; rescheduling all habit reminders")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationScheduler.rescheduleAll()
            } catch (e: Exception) {
                Timber.e(e, "BootReceiver: failed to reschedule alarms; scheduling retry")
                try {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val retryIntent = Intent(context, BootReceiver::class.java).apply {
                        action = ACTION_RETRY_RESCHEDULE
                    }
                    val retryPi = PendingIntent.getBroadcast(
                        context,
                        RETRY_REQUEST_CODE,
                        retryIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + RETRY_DELAY_MS,
                        retryPi
                    )
                } catch (retryError: Exception) {
                    Timber.e(retryError, "BootReceiver: retry scheduling also failed")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_RETRY_RESCHEDULE = "com.getaltair.kairos.RETRY_RESCHEDULE"
        private const val RETRY_REQUEST_CODE = 9999
        private const val RETRY_DELAY_MS = 5 * 60 * 1000L // 5 minutes
    }
}
