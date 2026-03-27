package com.getaltair.kairos.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.getaltair.kairos.domain.entity.Habit
import java.util.UUID

/**
 * Builds Android Notification objects for habit reminders.
 *
 * All notification text complies with invariant D-2 (shame-free messaging).
 * No streak language, no guilt, no blame -- only neutral or encouraging phrasing.
 */
class HabitReminderBuilder(private val context: Context) {

    /**
     * Builds the initial habit reminder notification.
     *
     * @param habit the habit to remind about
     * @param notificationId used as the base for PendingIntent request codes;
     *   action buttons use offsets from this value (snooze +100, skip +200)
     */
    fun buildReminder(habit: Habit, notificationId: Int): Notification = baseBuilder(habit, notificationId)
        .setContentText(habit.anchorBehavior)
        .build()

    /**
     * Builds a persistent follow-up notification.
     *
     * Follow-up text varies by number:
     * - 1: same body as initial (anchor behavior)
     * - 2: "Still waiting when you're ready."
     * - 3: "Last reminder for today."
     *
     * @param habit the habit to remind about
     * @param followUpNumber 1-based follow-up index
     * @param notificationId used for PendingIntent request codes
     */
    fun buildFollowUp(habit: Habit, followUpNumber: Int, notificationId: Int): Notification {
        require(followUpNumber in 1..NotificationConstants.MAX_FOLLOW_UPS) {
            "followUpNumber must be in 1..${NotificationConstants.MAX_FOLLOW_UPS}, was $followUpNumber"
        }
        return baseBuilder(habit, notificationId)
            .setContentText(followUpBody(habit, followUpNumber))
            .build()
    }

    /**
     * Returns the appropriate follow-up body text.
     * Exposed for testing without needing a full Android Context for Notification building.
     */
    internal fun followUpBody(habit: Habit, followUpNumber: Int): String = when (followUpNumber) {
        1 -> habit.anchorBehavior
        2 -> FOLLOW_UP_2_BODY
        3 -> FOLLOW_UP_3_BODY
        else -> throw IllegalArgumentException("Invalid followUpNumber: $followUpNumber")
    }

    /**
     * Returns the notification title text for a given habit.
     * Exposed for testing.
     */
    internal fun titleText(habit: Habit): String = "Time for: ${habit.name}"

    private fun baseBuilder(habit: Habit, notificationId: Int): NotificationCompat.Builder {
        val habitId = habit.id

        return NotificationCompat.Builder(context, NotificationChannels.CHANNEL_HABIT_REMINDERS)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(titleText(habit))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(buildContentIntent())
            .addAction(buildCompleteAction(habitId, notificationId))
            .addAction(buildSnoozeAction(habitId, notificationId))
            .addAction(buildSkipAction(habitId, notificationId))
    }

    private fun buildContentIntent(): PendingIntent {
        // Open the main activity when the notification body is tapped
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().apply { setClassName(context, "com.getaltair.kairos.MainActivity") }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildCompleteAction(habitId: UUID, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(ACTION_COMPLETE_HABIT).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_HABIT_ID, habitId.toString())
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, "Done", pi).build()
    }

    private fun buildSnoozeAction(habitId: UUID, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(ACTION_SNOOZE_HABIT).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_HABIT_ID, habitId.toString())
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode + SNOOZE_REQUEST_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, "Snooze", pi).build()
    }

    private fun buildSkipAction(habitId: UUID, requestCode: Int): NotificationCompat.Action {
        val intent = Intent(ACTION_SKIP_HABIT).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_HABIT_ID, habitId.toString())
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode + SKIP_REQUEST_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Action.Builder(0, "Skip", pi).build()
    }

    companion object {
        const val ACTION_COMPLETE_HABIT = "com.getaltair.kairos.ACTION_COMPLETE_HABIT"
        const val ACTION_SNOOZE_HABIT = "com.getaltair.kairos.ACTION_SNOOZE_HABIT"
        const val ACTION_SKIP_HABIT = "com.getaltair.kairos.ACTION_SKIP_HABIT"
        const val EXTRA_HABIT_ID = NotificationConstants.EXTRA_HABIT_ID

        /** Body text for follow-up 2 (D-2 compliant). */
        const val FOLLOW_UP_2_BODY = "Still waiting when you're ready."

        /** Body text for follow-up 3 (D-2 compliant). */
        const val FOLLOW_UP_3_BODY = "Last reminder for today."

        private const val SNOOZE_REQUEST_OFFSET = 100
        private const val SKIP_REQUEST_OFFSET = 200
    }
}
