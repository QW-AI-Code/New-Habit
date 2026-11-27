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
import java.time.LocalDate

/**
 * Handles every broadcast of a standalone reminder: its ring, a snoozed ring,
 * the test notification and the "Done" / "Snooze" notification buttons.
 *
 * A ring is "claimed" inside one atomic DataStore transaction (lastHandledAt is
 * moved to the ring time), so a duplicated delivery, the app start that the alarm
 * itself causes, or the boot-time catch-up can never show the same ring twice.
 */
class CustomReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                handle(appContext, intent)
            } catch (error: Exception) {
                Log.w(TAG, "Reminder broadcast could not be handled", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderAlarmScheduler.EXTRA_ID, 0L)
        val kind = intent.getStringExtra(ReminderAlarmScheduler.EXTRA_KIND) ?: ReminderAlarmScheduler.KIND_ALARM
        val store = AppStore(context)
        val scheduler = ReminderAlarmScheduler(context)

        when (kind) {
            ReminderAlarmScheduler.KIND_TEST -> {
                ReminderNotifier.showTest(context, store.state.first().isPersian)
                return
            }
            ReminderAlarmScheduler.KIND_SNOOZE_ACTION -> {
                ReminderNotifier.cancel(context, id)
                scheduler.snooze(id)
                return
            }
            ReminderAlarmScheduler.KIND_DONE -> {
                ReminderNotifier.cancel(context, id)
                scheduler.cancelSnooze(id)
                val today = LocalDate.now()
                val written = store.update { current ->
                    current.copy(
                        reminders = current.reminders.map { reminder ->
                            if (reminder.id == id && reminder.repeatsWithinDay) {
                                // "Done for today": the remaining rings of today are skipped.
                                reminder.copy(doneEpochDays = ReminderSchedule.pruneDoneDays(reminder.doneEpochDays + today.toEpochDay(), today))
                            } else {
                                reminder
                            }
                        }
                    )
                }
                written.reminders.firstOrNull { it.id == id }?.let { scheduler.schedule(it) }
                return
            }
        }

        // A regular ring (KIND_ALARM) or a snoozed one (KIND_SNOOZE).
        val triggerAt = intent.getLongExtra(ReminderAlarmScheduler.EXTRA_TRIGGER_AT, System.currentTimeMillis())
        val snoozed = kind == ReminderAlarmScheduler.KIND_SNOOZE
        var toShow: Reminder? = null
        val written = store.update { current ->
            val found = current.reminders.firstOrNull { it.id == id }
            when {
                // Deleted or switched off since the alarm was armed: stay silent.
                found == null || !found.enabled -> current
                snoozed -> {
                    toShow = found
                    current
                }
                // This ring was already shown (duplicate delivery or boot catch-up).
                found.lastHandledAt >= triggerAt -> current
                else -> {
                    val claimed = found.copy(lastHandledAt = triggerAt)
                    toShow = claimed
                    current.copy(reminders = current.reminders.map { if (it.id == id) claimed else it })
                }
            }
        }
        if (!snoozed) {
            // Arm the following ring before anything else can fail.
            written.reminders.firstOrNull { it.id == id }?.let { scheduler.schedule(it) }
        }
        val shown = toShow ?: return
        ReminderNotifier.show(context, written.isPersian, shown, triggerAt, missed = false, snoozed = snoozed)
    }

    companion object {
        private const val TAG = "CustomReminderReceiver"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
