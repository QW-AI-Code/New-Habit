package com.duck.twominute.reminders

import com.duck.twominute.PersianDate
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Pure, Android-free schedule math for [Reminder]s. Everything that decides *when*
 * a reminder rings lives here so it can be unit tested on the JVM.
 *
 * Times are local wall-clock values ([LocalDateTime]); the alarm scheduler turns
 * them into an instant with the device time zone at the last moment.
 */
object ReminderSchedule {

    const val MIN_INTERVAL = 5
    const val MAX_INTERVAL = 12 * 60
    const val MAX_RINGS_PER_DAY = 288

    /** Interval presets offered by the editor, in minutes. */
    val INTERVAL_PRESETS = listOf(15, 30, 60, 120, 180, 240)

    /** Enough for a yearly Jalali reminder plus skipped days. */
    private const val SEARCH_DAYS = 800

    /** Minute-of-day values (sorted) at which the reminder rings on an active day. */
    fun timesOfDay(reminder: Reminder): List<Int> {
        val start = reminder.safeHour * 60 + reminder.safeMinute
        if (!reminder.repeatsWithinDay) return listOf(start)
        val step = reminder.intervalMinutes.coerceIn(MIN_INTERVAL, MAX_INTERVAL)
        val end = reminder.safeEndHour * 60 + reminder.safeEndMinute
        if (end <= start) return listOf(start)
        val result = ArrayList<Int>()
        var cursor = start
        while (cursor <= end && result.size < MAX_RINGS_PER_DAY) {
            result += cursor
            cursor += step
        }
        return result
    }

    /** True when [date] is one of the reminder's active days (ignores "done" days). */
    fun occursOn(reminder: Reminder, date: LocalDate): Boolean {
        val start = reminder.startDate
        if (date.isBefore(start)) return false
        val until = reminder.untilEpochDay
        if (until != null && date.toEpochDay() > until) return false
        return when (reminder.repeat) {
            RepeatRule.ONCE -> date == start
            RepeatRule.DAILY -> true
            RepeatRule.WEEKLY -> reminder.effectiveWeekdays().contains(date.dayOfWeek.value)
            RepeatRule.MONTHLY -> {
                val first = PersianDate.fromGregorian(start)
                val day = PersianDate.fromGregorian(date)
                // The 31st rings on the 30th (or the 29th of Esfand) in shorter months.
                day.day == minOf(first.day, PersianDate.monthLength(day.year, day.month))
            }
            RepeatRule.YEARLY -> {
                val first = PersianDate.fromGregorian(start)
                val day = PersianDate.fromGregorian(date)
                // 30 Esfand rings on 29 Esfand in non-leap years.
                day.month == first.month && day.day == minOf(first.day, PersianDate.monthLength(day.year, day.month))
            }
        }
    }

    /** The first ring strictly after [after], or null when the reminder never rings again. */
    fun nextAfter(reminder: Reminder, after: LocalDateTime): LocalDateTime? {
        val times = timesOfDay(reminder)
        val start = reminder.startDate
        val until = reminder.untilEpochDay
        val done = reminder.doneEpochDays.toHashSet()
        var day = if (after.toLocalDate().isAfter(start)) after.toLocalDate() else start
        var guard = 0
        while (guard < SEARCH_DAYS) {
            guard += 1
            if (until != null && day.toEpochDay() > until) return null
            if (reminder.repeat == RepeatRule.ONCE && day.isAfter(start)) return null
            if (day.toEpochDay() !in done && occursOn(reminder, day)) {
                for (minuteOfDay in times) {
                    val at = day.atTime(minuteOfDay / 60, minuteOfDay % 60)
                    if (at.isAfter(after)) return at
                }
            }
            day = day.plusDays(1)
        }
        return null
    }

    /** Up to [count] next rings after [after], in order. */
    fun upcoming(reminder: Reminder, after: LocalDateTime, count: Int): List<LocalDateTime> {
        val result = ArrayList<LocalDateTime>(count.coerceAtLeast(0))
        var cursor = after
        while (result.size < count) {
            val next = nextAfter(reminder, cursor) ?: break
            result += next
            cursor = next
        }
        return result
    }

    /** The latest ring in the window (from, to], used to report a ring missed while the phone was off. */
    fun lastInWindow(reminder: Reminder, from: LocalDateTime, to: LocalDateTime): LocalDateTime? {
        var cursor = from
        var last: LocalDateTime? = null
        var guard = 0
        while (guard < 5000) {
            guard += 1
            val next = nextAfter(reminder, cursor) ?: break
            if (next.isAfter(to)) break
            last = next
            cursor = next
        }
        return last
    }

    /** Rings of [date], in order (empty when the reminder is not active that day). */
    fun ringsOn(reminder: Reminder, date: LocalDate): List<LocalDateTime> {
        if (!occursOn(reminder, date)) return emptyList()
        return timesOfDay(reminder).map { date.atTime(it / 60, it % 60) }
    }

    /** Keeps only recent "done" days so the list cannot grow forever. */
    fun pruneDoneDays(days: List<Long>, today: LocalDate): List<Long> {
        val floor = today.toEpochDay() - 2
        return days.filter { it >= floor }.distinct().sorted()
    }
}
