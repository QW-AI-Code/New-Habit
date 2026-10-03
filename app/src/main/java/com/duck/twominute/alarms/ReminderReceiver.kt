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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.duck.twominute.AppState
import com.duck.twominute.AppStore
import com.duck.twominute.Identity
import com.duck.twominute.MainActivity
import com.duck.twominute.R
import com.duck.twominute.toPersianDigits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires one reminder slot.
 *
 * v1.0.1: the identity is read fresh from storage (titles, language and progress
 * are always current), a reminder is skipped when the day is already complete, the
 * notification has a "Done" action that logs the repetition, and only the slot
 * that fired is re-armed for its next occurrence.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                handle(appContext, intent)
            } catch (error: Exception) {
                Log.w(TAG, "Reminder could not be handled", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        val identityId = intent.getLongExtra(AlarmScheduler.EXTRA_ID, 0L)
        val slot = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT, 0)
        val store = AppStore(context)

        when (intent.action) {
            ACTION_SNOOZE -> {
                cancelNotification(context, identityId, slot)
                snooze(context, intent, identityId, slot)
                return
            }
            ACTION_DONE -> {
                cancelNotification(context, identityId, slot)
                store.update { current -> logRepetition(current, identityId) }
                return
            }
        }

        val state = store.state.first()
        val identity = state.identities.firstOrNull { it.id == identityId }
        // Deleted or archived since the alarm was set: stay silent, nothing to re-arm.
        if (identity == null || identity.archived) return

        val firedAt = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, 0L)
        val now = System.currentTimeMillis()
        AlarmScheduler(context).rescheduleSlot(identity, slot, maxOf(now, firedAt) + RESCHEDULE_MARGIN_MS)

        // Every repetition of today is already logged: a reminder would only nag.
        if (identity.doneToday) return

        notify(context, intent, state, identity, slot)
    }

    private fun logRepetition(current: AppState, identityId: Long): AppState {
        if (current.identities.none { it.id == identityId }) return current
        return current.copy(
            identities = current.identities.map {
                if (it.id == identityId) it.copy(successes = it.successes + System.currentTimeMillis()) else it
            }
        )
    }

    private fun snooze(context: Context, intent: Intent, identityId: Long, slot: Int) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val again = Intent(context, ReminderReceiver::class.java)
            .putExtras(intent.extras ?: Bundle())
        again.action = null
        val pending = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.code(identityId, slot, AlarmScheduler.KIND_SNOOZE),
            again,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L,
            pending
        )
    }

    private fun cancelNotification(context: Context, identityId: Long, slot: Int) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(AlarmScheduler.code(identityId, slot, AlarmScheduler.KIND_NOTIFICATION))
    }

    private fun notify(context: Context, source: Intent, state: AppState, identity: Identity, slot: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val fa = state.isPersian
        val soundUri = identity.soundUri
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val channelId = ensureChannel(manager, identity, soundUri, fa)

        val openApp = PendingIntent.getActivity(
            context,
            AlarmScheduler.code(identity.id, slot, AlarmScheduler.KIND_OPEN),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java)
            .putExtras(source.extras ?: Bundle())
        snoozeIntent.action = ACTION_SNOOZE
        val snoozePending = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.code(identity.id, slot, AlarmScheduler.KIND_SNOOZE),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java)
            .putExtra(AlarmScheduler.EXTRA_ID, identity.id)
            .putExtra(AlarmScheduler.EXTRA_SLOT, slot)
        doneIntent.action = ACTION_DONE
        val donePending = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.code(identity.id, slot, AlarmScheduler.KIND_DONE),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = identity.title(fa).ifBlank { context.getString(R.string.app_name) }
        val ritual = identity.ritual(fa).ifBlank { context.getString(R.string.reminder_body) }
        val total = identity.safeTimesPerDay
        val body = if (total > 1) {
            val progress = if (fa) {
                "یادآور ${(slot + 1).toString().toPersianDigits()} از ${total.toString().toPersianDigits()} · امروز ${identity.todayCount.toString().toPersianDigits()}/${total.toString().toPersianDigits()}"
            } else {
                "Reminder ${slot + 1} of $total · today ${identity.todayCount}/$total"
            }
            "$ritual\n$progress"
        } else {
            ritual
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(0, if (fa) "انجام شد" else "Done", donePending)
            .addAction(0, if (fa) "شروع" else "Start now", openApp)
            .addAction(0, if (fa) "۱۰ دقیقه بعد" else "Snooze 10 min", snoozePending)
            .build()

        val allowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (allowed) {
            manager.notify(AlarmScheduler.code(identity.id, slot, AlarmScheduler.KIND_NOTIFICATION), notification)
        }
    }

    /**
     * A channel's sound is fixed once Android created it, so a changed custom sound
     * needs a new channel id (v1.0.0 kept playing the first sound forever). Stale
     * channels of the same identity are removed.
     */
    private fun ensureChannel(manager: NotificationManager, identity: Identity, soundUri: Uri?, fa: Boolean): String {
        if (soundUri == null) {
            val channel = NotificationChannel(
                DEFAULT_CHANNEL,
                if (fa) "یادآورهای عادت" else "Habit reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { enableVibration(true) }
            manager.createNotificationChannel(channel)
            return DEFAULT_CHANNEL
        }
        val prefix = "identity_sound_${identity.id}_"
        val channelId = prefix + Integer.toHexString(soundUri.toString().hashCode())
        runCatching {
            manager.notificationChannels
                .filter { (it.id.startsWith(prefix) && it.id != channelId) || it.id == "identity_custom_${identity.id}" }
                .forEach { manager.deleteNotificationChannel(it.id) }
        }
        val channel = NotificationChannel(
            channelId,
            identity.title(fa).ifBlank { if (fa) "یادآور" else "Reminder" },
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
        }
        manager.createNotificationChannel(channel)
        return channelId
    }

    companion object {
        const val ACTION_SNOOZE = "com.duck.twominute.SNOOZE"
        const val ACTION_DONE = "com.duck.twominute.DONE"
        private const val SNOOZE_MINUTES = 10L
        private const val RESCHEDULE_MARGIN_MS = 30_000L
        private const val DEFAULT_CHANNEL = "identity_default"
        private const val TAG = "ReminderReceiver"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
