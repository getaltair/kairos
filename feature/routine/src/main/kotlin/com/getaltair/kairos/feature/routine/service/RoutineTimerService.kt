package com.getaltair.kairos.feature.routine.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service providing persistent notification during routine execution.
 *
 * Displays the current habit name, time remaining, and step info in the
 * notification. Provides Done, Skip, and Pause/Resume action buttons that
 * communicate back to the ViewModel via [RoutineTimerState].
 *
 * The ViewModel drives all timer logic and pushes updates to this service
 * via [ACTION_UPDATE] intents. The service is a passive notification host.
 *
 * Lifecycle:
 * - Started via [ACTION_START] with extras for routine/habit info
 * - Updated via [ACTION_UPDATE] as the timer ticks
 * - Stopped via [ACTION_STOP] or [stopSelf]
 * - Notification action buttons emit [RoutineTimerState.TimerAction]
 */
class RoutineTimerService : Service() {

    companion object {
        const val ACTION_START = "com.getaltair.kairos.ROUTINE_TIMER_START"
        const val ACTION_STOP = "com.getaltair.kairos.ROUTINE_TIMER_STOP"
        const val ACTION_DONE = "com.getaltair.kairos.ROUTINE_TIMER_DONE"
        const val ACTION_SKIP = "com.getaltair.kairos.ROUTINE_TIMER_SKIP"
        const val ACTION_PAUSE = "com.getaltair.kairos.ROUTINE_TIMER_PAUSE"

        const val ACTION_UPDATE = "com.getaltair.kairos.ROUTINE_TIMER_UPDATE"
        const val ACTION_RESUME = "com.getaltair.kairos.ROUTINE_TIMER_RESUME"

        const val EXTRA_ROUTINE_NAME = "routine_name"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_TIME_REMAINING = "time_remaining"
        const val EXTRA_STEP_INFO = "step_info"
        const val EXTRA_IS_PAUSED = "is_paused"

        const val NOTIFICATION_CHANNEL_ID = "routine_timer"
        const val NOTIFICATION_ID = 1002
    }

    private var timeRemaining: Int = 0
    private var routineName: String = ""
    private var habitName: String = ""
    private var stepInfo: String = ""
    private var isPaused: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForeground(intent)

            ACTION_UPDATE -> handleUpdate(intent)

            ACTION_RESUME -> RoutineTimerState.emitAction(RoutineTimerState.TimerAction.RESUME)

            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            ACTION_DONE -> {
                RoutineTimerState.emitAction(RoutineTimerState.TimerAction.DONE)
            }

            ACTION_SKIP -> {
                RoutineTimerState.emitAction(RoutineTimerState.TimerAction.SKIP)
            }

            ACTION_PAUSE -> {
                RoutineTimerState.emitAction(RoutineTimerState.TimerAction.PAUSE)
            }
        }
        return START_STICKY
    }

    private fun startForeground(intent: Intent) {
        routineName = intent.getStringExtra(EXTRA_ROUTINE_NAME) ?: "Routine"
        habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Habit"
        timeRemaining = intent.getIntExtra(EXTRA_TIME_REMAINING, 0)
        stepInfo = intent.getStringExtra(EXTRA_STEP_INFO) ?: ""
        isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun handleUpdate(intent: Intent) {
        habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: habitName
        timeRemaining = intent.getIntExtra(EXTRA_TIME_REMAINING, timeRemaining)
        stepInfo = intent.getStringExtra(EXTRA_STEP_INFO) ?: stepInfo
        isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, isPaused)
        updateNotification()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Routine Timer",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows the current routine timer progress"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val minutes = timeRemaining / 60
        val seconds = timeRemaining % 60
        val timeText = String.format("%d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("$routineName - $habitName")
            .setContentText("$timeText remaining $stepInfo")
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .addAction(buildAction("Done", ACTION_DONE, 1))
            .addAction(buildAction("Skip", ACTION_SKIP, 2))
            .addAction(
                if (isPaused) {
                    buildAction("Resume", ACTION_RESUME, 3)
                } else {
                    buildAction("Pause", ACTION_PAUSE, 3)
                },
            )
            .build()
    }

    private fun buildAction(title: String, action: String, requestCode: Int,): NotificationCompat.Action {
        val intent = Intent(this, RoutineTimerService::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(0, title, pendingIntent).build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}
