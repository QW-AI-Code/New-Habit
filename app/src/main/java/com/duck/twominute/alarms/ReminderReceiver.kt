package com.duck.twominute.alarms

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.duck.twominute.MainActivity
import com.duck.twominute.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val identityId = intent.getLongExtra(AlarmScheduler.EXTRA_ID, 0L)

        if (intent.action == ACTION_SNOOZE) {
            snooze(context, intent, identityId)
            return
        }

        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE_FA)
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.app_name)
        val ritual = intent.getStringExtra(AlarmScheduler.EXTRA_RITUAL_FA).orEmpty()
        val soundUri = intent.getStringExtra(AlarmScheduler.EXTRA_SOUND)
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }

        notify(context, intent, identityId, title, ritual, soundUri)

        // Daily repeat: schedule the next occurrence right after this one fires.
        BootReceiver.rescheduleAsync(context)
    }

    private fun snooze(context: Context, intent: Intent, identityId: Long) {
        context.getSystemService(NotificationManager::class.java)?.cancel(identityId.toInt())
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val again = Intent(context, ReminderReceiver::class.java)
            .putExtras(intent.extras ?: Bundle())
        again.action = null
        val pending = PendingIntent.getBroadcast(
            context,
            snoozeCode(identityId),
            again,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L,
            pending
        )
    }

    private fun notify(
        context: Context,
        source: Intent,
        identityId: Long,
        title: String,
        ritual: String,
        soundUri: Uri?
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channelId = if (soundUri != null) "identity_custom_$identityId" else "identity_default"
        val channel = NotificationChannel(
            channelId,
            title,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            if (soundUri != null) {
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
        }
        manager.createNotificationChannel(channel)

        val openApp = PendingIntent.getActivity(
            context,
            identityId.toInt(),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java)
            .putExtras(source.extras ?: Bundle())
        snoozeIntent.action = ACTION_SNOOZE
        val snoozePending = PendingIntent.getBroadcast(
            context,
            snoozeCode(identityId),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = ritual.ifBlank { context.getString(R.string.reminder_body) }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(0, context.getString(R.string.action_start_now), openApp)
            .addAction(0, context.getString(R.string.action_snooze), snoozePending)
            .build()

        val allowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (allowed) {
            manager.notify(identityId.toInt(), notification)
        }
    }

    private fun snoozeCode(identityId: Long): Int = (identityId % Int.MAX_VALUE).toInt() xor 0x5A5A

    companion object {
        const val ACTION_SNOOZE = "com.duck.twominute.SNOOZE"
        private const val SNOOZE_MINUTES = 10L
    }
}
