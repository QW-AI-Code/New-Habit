package com.duck.twominute.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.duck.twominute.AppStore
import com.duck.twominute.reminders.Reminder
import com.duck.twominute.reminders.ReminderSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Restores alarms without opening the UI after a reboot, an update, a clock change or a permission change. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val afterBoot = intent.action?.let { it in BOOT_ACTIONS } == true
        scope.launch {
            try {
                // A ring that fell while the phone was off is shown once, marked as missed.
                if (afterBoot) catchUpMissed(appContext)
                // AlarmManager alarms are cleared by Android on reboot. Rebuild every
                // active reminder from DataStore, including after app updates and clock changes.
                scheduleAll(appContext)
            } catch (error: Exception) {
                Log.w(TAG, "Could not restore reminders after ${intent.action}", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Only rings from the last few hours are reported; older ones would just be noise. */
        private const val MISSED_WINDOW_MS = 6L * 60L * 60L * 1000L

        private val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )

        suspend fun scheduleAll(context: Context) {
            val state = AppStore(context).state.first()
            val identityScheduler = AlarmScheduler(context)
            state.identities
                .filter { it.hasReminder() && !it.archived }
                .forEach { identityScheduler.schedule(it) }
            val reminderScheduler = ReminderAlarmScheduler(context)
            state.reminders.forEach { reminderScheduler.schedule(it) }
        }

        /**
         * Shows at most one "missed" notification per reminder for rings that were due
         * while the phone was off. Each ring is claimed atomically (lastHandledAt), so
         * it can never be shown twice.
         */
        private suspend fun catchUpMissed(context: Context) {
            val store = AppStore(context)
            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val claimed = ArrayList<Pair<Reminder, Long>>()
            val written = store.update { current ->
                claimed.clear()
                val updated = current.reminders.map { reminder ->
                    if (!reminder.enabled) return@map reminder
                    val fromMillis = maxOf(reminder.lastHandledAt, now - MISSED_WINDOW_MS)
                    val from = Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDateTime()
                    val to = Instant.ofEpochMilli(now).atZone(zone).toLocalDateTime()
                    val missed = ReminderSchedule.lastInWindow(reminder, from, to) ?: return@map reminder
                    val missedAt = missed.atZone(zone).toInstant().toEpochMilli()
                    val next = reminder.copy(lastHandledAt = missedAt)
                    claimed += next to missedAt
                    next
                }
                if (claimed.isEmpty()) current else current.copy(reminders = updated)
            }
            claimed.forEach { (reminder, at) ->
                ReminderNotifier.show(context, written.isPersian, reminder, at, missed = true, snoozed = false)
            }
        }

        fun rescheduleAsync(context: Context) {
            val appContext = context.applicationContext
            scope.launch {
                try {
                    scheduleAll(appContext)
                } catch (error: Exception) {
                    Log.w(TAG, "Could not reschedule reminders", error)
                }
            }
        }
    }
}
