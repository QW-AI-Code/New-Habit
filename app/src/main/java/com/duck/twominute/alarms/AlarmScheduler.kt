package com.duck.twominute.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.duck.twominute.Identity
import com.duck.twominute.ReminderPlanner
import com.duck.twominute.ReminderTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules one alarm per reminder slot.
 *
 * v1.0.0 kept a single alarm per identity (one request code), so "3 times a day"
 * could never ring more than once. Every slot now has its own request code, and
 * cancelling an identity clears every possible slot plus the old v1.0.0 alarm.
 */
class AlarmScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun schedule(identity: Identity) {
        cancel(identity.id)
        if (identity.archived || alarmManager == null) return
        identity.reminderSlots().forEachIndexed { slot, time ->
            scheduleSlot(identity, slot, time, System.currentTimeMillis())
        }
    }

    /**
     * Re-arms a single slot after it fired. [notBeforeMillis] is just after the
     * moment that fired, so the same minute is never scheduled twice.
     */
    fun rescheduleSlot(identity: Identity, slot: Int, notBeforeMillis: Long) {
        if (identity.archived || alarmManager == null) return
        val time = identity.reminderSlots().getOrNull(slot) ?: return
        scheduleSlot(identity, slot, time, notBeforeMillis)
    }

    private fun scheduleSlot(identity: Identity, slot: Int, time: ReminderTime, notBeforeMillis: Long) {
        val manager = alarmManager ?: return
        val zone = ZoneId.systemDefault()
        val floor = ZonedDateTime.ofInstant(Instant.ofEpochMilli(notBeforeMillis), zone)
        var next = floor.withHour(time.safeHour).withMinute(time.safeMinute).withSecond(0).withNano(0)
        if (!next.isAfter(floor)) next = next.plusDays(1)
        var guard = 0
        while (!identity.runsOn(next.toLocalDate()) && guard < 14) {
            next = next.plusDays(1)
            guard += 1
        }

        val triggerAt = next.toInstant().toEpochMilli()
        val pending = pendingIntent(identity, slot, triggerAt)
        val exactAllowed = canScheduleExact()
        if (exactAllowed) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // Still deliver reminders on devices where exact-alarm special access is off.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true

    fun cancel(identityId: Long) {
        for (slot in 0 until ReminderPlanner.MAX_SLOTS) cancelCode(slotCode(identityId, slot))
        cancelCode(legacyCode(identityId))
    }

    private fun cancelCode(code: Int) {
        val intent = Intent(appContext, ReminderReceiver::class.java)
        val existing = PendingIntent.getBroadcast(
            appContext,
            code,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager?.cancel(existing)
            existing.cancel()
        }
    }

    private fun pendingIntent(identity: Identity, slot: Int, triggerAt: Long): PendingIntent {
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_ID, identity.id)
            putExtra(EXTRA_SLOT, slot)
            putExtra(EXTRA_TRIGGER_AT, triggerAt)
            putExtra(EXTRA_TITLE_FA, identity.titleFa)
            putExtra(EXTRA_TITLE_EN, identity.titleEn)
            putExtra(EXTRA_RITUAL_FA, identity.ritualFa)
            putExtra(EXTRA_RITUAL_EN, identity.ritualEn)
            putExtra(EXTRA_SOUND, identity.soundUri)
        }
        return PendingIntent.getBroadcast(
            appContext,
            slotCode(identity.id, slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val EXTRA_ID = "identity_id"
        const val EXTRA_SLOT = "identity_slot"
        const val EXTRA_TRIGGER_AT = "identity_trigger_at"
        const val EXTRA_TITLE_FA = "identity_title_fa"
        const val EXTRA_TITLE_EN = "identity_title_en"
        const val EXTRA_RITUAL_FA = "identity_ritual_fa"
        const val EXTRA_RITUAL_EN = "identity_ritual_en"
        const val EXTRA_SOUND = "identity_sound"

        /** Kinds of request code / notification id derived from one slot. */
        const val KIND_ALARM = 1
        const val KIND_NOTIFICATION = 2
        const val KIND_OPEN = 3
        const val KIND_SNOOZE = 4
        const val KIND_DONE = 5

        /** Stable, collision-resistant code for one (identity, slot, kind) triple. */
        fun code(identityId: Long, slot: Int, kind: Int): Int {
            val folded = (identityId xor (identityId ushr 32)).toInt()
            return (folded * 31 + slot) * 31 + kind
        }

        fun slotCode(identityId: Long, slot: Int): Int = code(identityId, slot, KIND_ALARM)

        /** The request code v1.0.0 used, so its alarm can be cancelled after an update. */
        fun legacyCode(identityId: Long): Int = (identityId % Int.MAX_VALUE).toInt()
    }
}
