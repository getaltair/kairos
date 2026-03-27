package com.getaltair.kairos.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import timber.log.Timber

/**
 * Builds and posts Android notifications for the recovery system.
 *
 * All notification text complies with FR-6 (shame-free messaging):
 * no streak language, no blame, no guilt, no "failed/failure", no "try harder",
 * no "give up", no "should have". Only neutral or encouraging phrasing.
 */
class RecoveryNotificationBuilder(private val context: Context) {

    private val notificationManager by lazy { NotificationManagerCompat.from(context) }

    // -- Build methods (return Notification without posting) --

    /**
     * Builds a Day 3+ lapse prompt notification.
     * Shame-free: acknowledges the gap without blame.
     */
    fun buildLapseNotification(habitName: String, missedDays: Int): Notification = baseBuilder()
        .setContentTitle(lapseTitle(habitName))
        .setContentText(lapseBody(missedDays))
        .build()

    /**
     * Builds a Day 7+ relapse prompt notification.
     * More supportive tone for extended absence.
     */
    fun buildRelapseNotification(habitName: String): Notification = baseBuilder()
        .setContentTitle(relapseTitle(habitName))
        .setContentText(RELAPSE_BODY)
        .build()

    /**
     * Builds a Monday/1st-of-month batch fresh start notification.
     */
    fun buildFreshStartNotification(habitCount: Int): Notification = baseBuilder()
        .setContentTitle(FRESH_START_TITLE)
        .setContentText(freshStartBody(habitCount))
        .build()

    // -- Post methods (build + send to system) --

    /**
     * Posts a lapse notification with a unique ID derived from the habit ID.
     */
    fun postLapseNotification(habitId: String, habitName: String, missedDays: Int) {
        val notification = buildLapseNotification(habitName, missedDays)
        val notificationId = (habitId.hashCode() and 0x0FFF_FFFF) or LAPSE_TAG
        postSafely(notificationId, notification)
    }

    /**
     * Posts a relapse notification with a unique ID derived from the habit ID.
     */
    fun postRelapseNotification(habitId: String, habitName: String) {
        val notification = buildRelapseNotification(habitName)
        val notificationId = (habitId.hashCode() and 0x0FFF_FFFF) or RELAPSE_TAG
        postSafely(notificationId, notification)
    }

    /**
     * Posts a single fresh start notification (not per-habit).
     */
    fun postFreshStartNotification(habitCount: Int) {
        val notification = buildFreshStartNotification(habitCount)
        postSafely(FRESH_START_NOTIFICATION_ID, notification)
    }

    // -- Text generation (internal for testing) --

    internal fun lapseTitle(habitName: String): String = "Still here for $habitName"

    internal fun lapseBody(missedDays: Int): String = when {
        missedDays <= 3 -> LAPSE_BODY_SHORT
        else -> LAPSE_BODY_LONG
    }

    internal fun relapseTitle(habitName: String): String = "Ready when you are, $habitName"

    internal fun freshStartBody(habitCount: Int): String = when (habitCount) {
        1 -> FRESH_START_BODY_SINGLE
        else -> "$habitCount habits are ready for a comeback whenever you are."
    }

    // -- Private helpers --

    private fun baseBuilder(): NotificationCompat.Builder =
        NotificationCompat.Builder(context, NotificationChannels.CHANNEL_RECOVERY)
            .setSmallIcon(context.applicationInfo.icon)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(buildContentIntent())

    private fun buildContentIntent(): PendingIntent {
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

    @Suppress("MissingPermission")
    private fun postSafely(notificationId: Int, notification: Notification) {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(notificationId, notification)
            } else {
                Timber.w("POST_NOTIFICATIONS permission not granted; skipping recovery notification")
            }
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException posting recovery notification")
        }
    }

    companion object {
        /** Lapse body for 3 or fewer consecutive days. */
        const val LAPSE_BODY_SHORT = "Ready when you are. Tap to check in."

        /** Lapse body for 4+ consecutive days. */
        const val LAPSE_BODY_LONG = "No rush at all. Tap whenever you want to check in."

        /** Relapse body text. */
        const val RELAPSE_BODY = "It has been a little while. Ready for a fresh start?"

        /** Fresh start title. */
        const val FRESH_START_TITLE = "A fresh start awaits"

        /** Fresh start body when exactly one habit is eligible. */
        const val FRESH_START_BODY_SINGLE = "One habit is ready for a comeback whenever you are."

        // Notification ID tags -- clear upper nibble before applying tag to prevent collision
        private const val LAPSE_TAG = 0x1000_0000
        private const val RELAPSE_TAG = 0x2000_0000
        private const val FRESH_START_NOTIFICATION_ID = 0x3000_0001
    }
}
