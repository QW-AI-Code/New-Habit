package com.duck.twominute.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.duck.twominute.reminders.Reminder
import com.duck.twominute.reminders.ReminderSchedule
import java.time.Instant
import java.time.ZoneId

/**
 * Arms the alarms of standalone reminders (v1.1.0).
 *
 * Exactly one alarm per reminder is pending at any time: its next ring. When it
 * fires, [CustomReminderReceiver] shows the notification and arms the following
 * ring. Every PendingIntent carries a unique data URI
 * (`newhabit://reminder/<id>/<kind>`), so reminders can never collide with each
 * other or with the identity alarms, whatever their ids are.
 */
class ReminderAlarmScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    /**
     * Cancels the pending ring of [reminder] and arms the next one after
     * max(now, lastHandledAt). Returns the trigger time, or null when nothing was armed.
     */
    fun schedule(reminder: Reminder, nowMillis: Long = System.currentTimeMillis()): Long? {
        cancelAlarm(reminder.id)
        val manager = alarmManager ?: return null
        if (!reminder.enabled) return null
        val zone = ZoneId.systemDefault()
        val floorMillis = maxOf(nowMillis, reminder.lastHandledAt)
        val after = Instant.ofEpochMilli(floorMillis).atZone(zone).toLocalDateTime()
        val next = ReminderSchedule.nextAfter(reminder, after) ?: return null
        val triggerAt = next.atZone(zone).toInstant().toEpochMilli()
        val pending = pendingIntent(reminder.id, KIND_ALARM, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT) ?: return null
        setExact(manager, triggerAt, pending)
        return triggerAt
    }

    /** Shows the same reminder again in [minutes] minutes, without touching its regular schedule. */
    fun snooze(reminderId: Long, minutes: Long = SNOOZE_MINUTES) {
        val manager = alarmManager ?: return
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val pending = pendingIntent(reminderId, KIND_SNOOZE, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        setExact(manager, triggerAt, pending)
    }

    /**
     * Sends a test notification through the real pipeline (AlarmManager, receiver,
     * channel and permission), so the user can see whether reminders reach them.
     */
    fun scheduleTest(delaySeconds: Long = TEST_DELAY_SECONDS): Long {
        val triggerAt = System.currentTimeMillis() + delaySeconds * 1000L
        val manager = alarmManager ?: return triggerAt
        val pending = pendingIntent(TEST_ID, KIND_TEST, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT) ?: return triggerAt
        setExact(manager, triggerAt, pending)
        return triggerAt
    }

    fun cancel(reminderId: Long) {
        cancelAlarm(reminderId)
        cancelKind(reminderId, KIND_SNOOZE)
    }

    fun cancelSnooze(reminderId: Long) = cancelKind(reminderId, KIND_SNOOZE)

    private fun cancelAlarm(reminderId: Long) = cancelKind(reminderId, KIND_ALARM)

    private fun cancelKind(reminderId: Long, kind: String) {
        val existing = pendingIntent(reminderId, kind, 0L, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager?.cancel(existing)
        existing.cancel()
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    private fun setExact(manager: AlarmManager, triggerAt: Long, pending: PendingIntent) {
        try {
            if (canScheduleExact()) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                // Exact alarms are switched off for this app: still deliver, a little later at worst.
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (security: SecurityException) {
            // The exact-alarm access was revoked between the check and the call.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(reminderId: Long, kind: String, triggerAt: Long, flag: Int): PendingIntent? {
        val intent = Intent(appContext, CustomReminderReceiver::class.java)
            .setData(dataUri(reminderId, kind))
            .putExtra(EXTRA_ID, reminderId)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_TRIGGER_AT, triggerAt)
        return PendingIntent.getBroadcast(appContext, 0, intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val EXTRA_ID = "custom_reminder_id"
        const val EXTRA_KIND = "custom_reminder_kind"
        const val EXTRA_TRIGGER_AT = "custom_reminder_trigger_at"

        const val KIND_ALARM = "alarm"
        const val KIND_SNOOZE = "snooze"
        const val KIND_TEST = "test"
        const val KIND_DONE = "done"
        const val KIND_SNOOZE_ACTION = "snooze_action"
        const val KIND_OPEN = "open"

        const val SNOOZE_MINUTES = 10L
        const val TEST_DELAY_SECONDS = 5L
        const val TEST_ID = -1L

        fun dataUri(reminderId: Long, kind: String): Uri = Uri.parse("newhabit://reminder/$reminderId/$kind")
    }
}
