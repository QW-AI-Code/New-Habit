package com.duck.twominute.reminders

import kotlinx.serialization.Serializable
import java.time.LocalDate

/** How the days of a reminder repeat. Monthly and yearly follow the Jalali (Shamsi) calendar. */
@Serializable
enum class RepeatRule { ONCE, DAILY, WEEKLY, MONTHLY, YEARLY }

/**
 * A standalone reminder (added in v1.1.0): a title, a first day picked on the
 * Jalali calendar, a time, and optional repetition.
 *
 * Two independent kinds of repetition can be combined:
 *  - across days: [repeat] (once, every day, some weekdays, every Jalali month or year),
 *  - within a day: [intervalMinutes] > 0 rings every N minutes from [hour]:[minute]
 *    until [endHour]:[endMinute] (for example "on 7 Azar, every hour from 09:00 to 21:00").
 *
 * Dates are stored as [LocalDate.toEpochDay] so the data is calendar-neutral; the
 * UI shows and picks them in the Jalali calendar.
 */
@Serializable
data class Reminder(
    val id: Long = 0L,
    val title: String = "",
    val note: String = "",
    /** First day the reminder may ring, as an epoch day. */
    val startEpochDay: Long = LocalDate.now().toEpochDay(),
    val hour: Int = 9,
    val minute: Int = 0,
    val repeat: RepeatRule = RepeatRule.ONCE,
    /** ISO weekdays (1 = Monday .. 7 = Sunday) for [RepeatRule.WEEKLY]. Empty = the weekday of the first day. */
    val weekdays: List<Int> = emptyList(),
    /** Last day (inclusive) a repeating reminder may ring; null = no end. */
    val untilEpochDay: Long? = null,
    /** 0 = rings once on each active day; otherwise every N minutes until the end time. */
    val intervalMinutes: Int = 0,
    val endHour: Int = 21,
    val endMinute: Int = 0,
    val enabled: Boolean = true,
    /** Days the user marked "done for today": the remaining rings of those days are skipped. */
    val doneEpochDays: List<Long> = emptyList(),
    /**
     * Epoch millis of the latest ring that was already handled (or of the moment the
     * reminder was saved). Nothing at or before it is ever shown again, which makes a
     * duplicated alarm delivery or a reboot harmless.
     */
    val lastHandledAt: Long = 0L,
    val createdAt: Long = 0L
) {
    val safeHour: Int get() = hour.coerceIn(0, 23)
    val safeMinute: Int get() = minute.coerceIn(0, 59)
    val safeEndHour: Int get() = endHour.coerceIn(0, 23)
    val safeEndMinute: Int get() = endMinute.coerceIn(0, 59)

    val startDate: LocalDate get() = LocalDate.ofEpochDay(startEpochDay)
    val untilDate: LocalDate? get() = untilEpochDay?.let { LocalDate.ofEpochDay(it) }

    val repeatsWithinDay: Boolean get() = intervalMinutes > 0

    /** Valid weekdays, falling back to the weekday of the first day. */
    fun effectiveWeekdays(): List<Int> =
        weekdays.filter { it in 1..7 }.distinct().sorted().ifEmpty { listOf(startDate.dayOfWeek.value) }
}
