package com.duck.twominute.alarms

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Everything on the phone that can silently stop a reminder, checked in one place
 * so the Reminders screen can show what is wrong and open the right settings page.
 */
object ReminderHealth {

    data class Status(
        /** App notifications switched on (and, on Android 13+, the permission granted). */
        val notificationsEnabled: Boolean,
        /** None of the reminder channels was muted by the user. */
        val channelsEnabled: Boolean,
        /** Exact alarms allowed, so a 09:00 reminder rings at 09:00 and not "around" it. */
        val exactAlarms: Boolean,
        /** The app is exempt from battery optimization (helps on aggressive phones). */
        val batteryUnrestricted: Boolean
    ) {
        val essentialOk: Boolean get() = notificationsEnabled && channelsEnabled && exactAlarms
    }

    private val CHANNELS = listOf(ReminderNotifier.CHANNEL_ID, "identity_default")

    fun check(context: Context): Status {
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(NotificationManager::class.java)
        val channelsOk = CHANNELS.all { id ->
            val channel = runCatching { manager?.getNotificationChannel(id) }.getOrNull()
            channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
        }
        val power = appContext.getSystemService(PowerManager::class.java)
        return Status(
            notificationsEnabled = ReminderNotifier.canPost(appContext),
            channelsEnabled = channelsOk,
            exactAlarms = ReminderAlarmScheduler(appContext).canScheduleExact(),
            batteryUnrestricted = power?.isIgnoringBatteryOptimizations(appContext.packageName) == true
        )
    }

    fun notificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun exactAlarmSettings(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            null
        }

    /** The direct "allow running in background" prompt; the app is sideloaded, not a Play Store build. */
    @SuppressLint("BatteryLife")
    fun batteryPrompt(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun batteryList(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun appDetails(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens the first intent that resolves; returns false when none could be started. */
    fun launch(context: Context, vararg intents: Intent?): Boolean {
        for (intent in intents) {
            if (intent == null) continue
            val ok = runCatching { context.startActivity(intent) }.isSuccess
            if (ok) return true
        }
        return false
    }
}
