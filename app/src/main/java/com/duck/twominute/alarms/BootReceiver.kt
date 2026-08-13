package com.duck.twominute.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.duck.twominute.AppStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Restores alarms without opening the UI after a reboot. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
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

        suspend fun scheduleAll(context: Context) {
            val scheduler = AlarmScheduler(context)
            AppStore(context).state.first()
                .identities
                .filter { it.hasReminder() && !it.archived }
                .forEach { scheduler.schedule(it) }
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
