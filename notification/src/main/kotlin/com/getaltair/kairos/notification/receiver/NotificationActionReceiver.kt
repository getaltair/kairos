package com.getaltair.kairos.notification.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.getaltair.kairos.core.usecase.CompleteHabitUseCase
import com.getaltair.kairos.core.usecase.SkipHabitUseCase
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.notification.HabitReminderBuilder
import com.getaltair.kairos.notification.NotificationIdStrategy
import com.getaltair.kairos.notification.NotificationScheduler
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * BroadcastReceiver handling notification action buttons (Done, Snooze, Skip).
 *
 * Each action dismisses the notification and performs the appropriate operation
 * via the corresponding use case.
 */
class NotificationActionReceiver :
    BroadcastReceiver(),
    KoinComponent {

    private val completeHabitUseCase: CompleteHabitUseCase by inject()
    private val skipHabitUseCase: SkipHabitUseCase by inject()
    private val notificationScheduler: NotificationScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val habitIdString = intent.getStringExtra(HabitReminderBuilder.EXTRA_HABIT_ID) ?: run {
            Timber.w("NotificationActionReceiver: missing habit_id extra")
            return
        }
        val habitId = try {
            UUID.fromString(habitIdString)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "NotificationActionReceiver: invalid habit_id: %s", habitIdString)
            return
        }

        val notifManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    HabitReminderBuilder.ACTION_COMPLETE_HABIT -> {
                        Timber.d("Action: complete habit %s", habitId)
                        completeHabitUseCase(habitId, CompletionType.Full)
                        cancelNotificationAndFollowUps(notifManager, habitId)
                    }

                    HabitReminderBuilder.ACTION_SNOOZE_HABIT -> {
                        Timber.d("Action: snooze habit %s", habitId)
                        cancelNotification(notifManager, habitId)
                        notificationScheduler.scheduleSnooze(habitId, SNOOZE_DELAY_MINUTES)
                    }

                    HabitReminderBuilder.ACTION_SKIP_HABIT -> {
                        Timber.d("Action: skip habit %s", habitId)
                        skipHabitUseCase(habitId)
                        cancelNotificationAndFollowUps(notifManager, habitId)
                    }

                    else -> {
                        Timber.w("NotificationActionReceiver: unknown action %s", intent.action)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "NotificationActionReceiver: error processing action for %s", habitId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun cancelNotificationAndFollowUps(notifManager: NotificationManager, habitId: UUID) {
        notifManager.cancel(NotificationIdStrategy.reminderId(habitId))
        // Cancel all possible follow-up notification IDs
        for (i in 1..MAX_FOLLOW_UPS) {
            notifManager.cancel(NotificationIdStrategy.followUpId(habitId, i))
        }
        notificationScheduler.cancelFollowUps(habitId)
    }

    private fun cancelNotification(notifManager: NotificationManager, habitId: UUID) {
        notifManager.cancel(NotificationIdStrategy.reminderId(habitId))
    }

    companion object {
        private const val SNOOZE_DELAY_MINUTES = 10
        private const val MAX_FOLLOW_UPS = 3
    }
}
