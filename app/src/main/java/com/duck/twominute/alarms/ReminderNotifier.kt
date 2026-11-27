package com.duck.twominute.alarms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.duck.twominute.MainActivity
import com.duck.twominute.R
import com.duck.twominute.reminders.Reminder
import com.duck.twominute.reminders.ReminderSchedule
import com.duck.twominute.reminders.ReminderText
import java.time.Instant
import java.time.ZoneId

/** Builds and posts the notifications of standalone reminders (v1.1.0). */
object ReminderNotifier {

    const val CHANNEL_ID = "custom_reminders"
    private const val TAG = "custom_reminder"
    private const val TEST_NOTIFICATION_ID = 0x7E57

    /**
     * The one correct "may we post?" check on every Android version.
     *
     * v1.0.x asked `checkSelfPermission(POST_NOTIFICATIONS)`. That permission only
     * exists on Android 13+, so on Android 8–12 the answer was always "denied" and
     * no reminder notification was ever shown. [NotificationManagerCompat.areNotificationsEnabled]
     * covers both the Android 13 runtime permission and the per-app switch of older versions.
     */
    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notificationId(reminderId: Long): Int = (reminderId xor (reminderId ushr 32)).toInt()

    fun ensureChannel(context: Context, fa: Boolean): String {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return CHANNEL_ID
        val channel = NotificationChannel(
            CHANNEL_ID,
            if (fa) "یادآورها" else "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = if (fa) "یادآورهایی که با تاریخ و ساعت تنظیم کرده‌ای" else "Reminders you set for a date and time"
            enableVibration(true)
            enableLights(true)
        }
        manager.createNotificationChannel(channel)
        return CHANNEL_ID
    }

    fun show(context: Context, fa: Boolean, reminder: Reminder, scheduledAt: Long, missed: Boolean, snoozed: Boolean) {
        if (!canPost(context)) {
            Log.w(TAG, "Notifications are disabled for the app; reminder ${reminder.id} not shown")
            return
        }
        val channelId = ensureChannel(context, fa)
        val zone = ZoneId.systemDefault()
        val at = Instant.ofEpochMilli(scheduledAt).atZone(zone).toLocalDateTime()
        val whenText = ReminderText.dateTime(at, fa)

        val status = when {
            missed -> if (fa) "وقتی گوشی خاموش بود زمانش رسید · $whenText" else "Was due while the phone was off · $whenText"
            snoozed -> if (fa) "یادآوری دوباره · $whenText" else "Reminder again · $whenText"
            reminder.repeatsWithinDay -> {
                val rings = ReminderSchedule.timesOfDay(reminder)
                val index = rings.indexOf(at.hour * 60 + at.minute)
                if (index >= 0 && rings.size > 1) {
                    if (fa) {
                        ReminderText.digits("یادآور ${index + 1} از ${rings.size} امروز · ", true) + ReminderText.clock(at.hour, at.minute, true)
                    } else {
                        "Reminder ${index + 1} of ${rings.size} today · " + ReminderText.clock(at.hour, at.minute, false)
                    }
                } else {
                    whenText
                }
            }
            else -> whenText
        }
        val note = reminder.note.trim()
        val body = if (note.isEmpty()) status else note + "\n" + status
        val title = reminder.title.trim().ifBlank { if (fa) "یادآور" else "Reminder" }

        val doneLabel = if (reminder.repeatsWithinDay) {
            if (fa) "تمام برای امروز" else "Done for today"
        } else {
            if (fa) "انجام شد" else "Done"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setWhen(scheduledAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, reminder.id))
            .addAction(0, doneLabel, action(context, reminder.id, ReminderAlarmScheduler.KIND_DONE))
            .addAction(0, if (fa) "۱۰ دقیقه بعد" else "Snooze 10 min", action(context, reminder.id, ReminderAlarmScheduler.KIND_SNOOZE_ACTION))
            .build()

        post(context, notificationId(reminder.id), notification)
    }

    fun showTest(context: Context, fa: Boolean) {
        if (!canPost(context)) return
        val channelId = ensureChannel(context, fa)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (fa) "اعلان آزمایشی New Habit" else "New Habit test notification")
            .setContentText(if (fa) "یادآورها روی این گوشی درست می‌رسند." else "Reminders reach you on this phone.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, ReminderAlarmScheduler.TEST_ID))
            .build()
        post(context, TEST_NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context, reminderId: Long) {
        context.getSystemService(NotificationManager::class.java)?.cancel(TAG, notificationId(reminderId))
    }

    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        try {
            manager.notify(TAG, id, notification)
        } catch (error: SecurityException) {
            Log.w(TAG, "Notification permission was revoked", error)
        }
    }

    private fun openApp(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setData(ReminderAlarmScheduler.dataUri(reminderId, ReminderAlarmScheduler.KIND_OPEN))
            .putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_REMINDERS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun action(context: Context, reminderId: Long, kind: String): PendingIntent {
        val intent = Intent(context, CustomReminderReceiver::class.java)
            .setData(ReminderAlarmScheduler.dataUri(reminderId, kind))
            .putExtra(ReminderAlarmScheduler.EXTRA_ID, reminderId)
            .putExtra(ReminderAlarmScheduler.EXTRA_KIND, kind)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
