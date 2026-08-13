package com.duck.twominute.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.duck.twominute.Identity
import java.time.ZonedDateTime

class AlarmScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(identity: Identity) {
        cancel(identity.id)
        val hour = identity.reminderHour
        if (hour == null || identity.archived || alarmManager == null) return
        val minute = identity.reminderMinute ?: 0
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        var guard = 0
        while (!identity.runsOn(next.toLocalDate()) && guard < 14) {
            next = next.plusDays(1)
            guard += 1
        }

        val triggerAt = next.toInstant().toEpochMilli()
        val pending = pendingIntent(identity, PendingIntent.FLAG_UPDATE_CURRENT)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exactAllowed) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // Still deliver reminders on devices where exact-alarm special access is off.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(identityId: Long) {
        val intent = Intent(appContext, ReminderReceiver::class.java)
        val existing = PendingIntent.getBroadcast(
            appContext,
            requestCode(identityId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager?.cancel(existing)
            existing.cancel()
        }
    }

    private fun pendingIntent(identity: Identity, flag: Int): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, identity.id)
            putExtra(EXTRA_TITLE_FA, identity.titleFa)
            putExtra(EXTRA_TITLE_EN, identity.titleEn)
            putExtra(EXTRA_RITUAL_FA, identity.ritualFa)
            putExtra(EXTRA_RITUAL_EN, identity.ritualEn)
            putExtra(EXTRA_SOUND, identity.soundUri)
        }
        return PendingIntent.getBroadcast(appContext, requestCode(identity.id), intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun requestCode(identityId: Long): Int = (identityId % Int.MAX_VALUE).toInt()

    companion object {
        const val EXTRA_ID = "identity_id"
        const val EXTRA_TITLE_FA = "identity_title_fa"
        const val EXTRA_TITLE_EN = "identity_title_en"
        const val EXTRA_RITUAL_FA = "identity_ritual_fa"
        const val EXTRA_RITUAL_EN = "identity_ritual_en"
        const val EXTRA_SOUND = "identity_sound"
    }
}
