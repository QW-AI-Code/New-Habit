package com.duck.twominute.reminders

import com.duck.twominute.PersianDate
import com.duck.twominute.toPersianDigits
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Human-readable texts for dates, times and reminder schedules. Persian texts use
 * the Jalali calendar and Persian digits; English texts use the Gregorian calendar.
 * Plain Kotlin (no Compose), so notifications and the UI share exactly one wording.
 */
object ReminderText {

    private val weekdaysFa = mapOf(1 to "دوشنبه", 2 to "سه‌شنبه", 3 to "چهارشنبه", 4 to "پنجشنبه", 5 to "جمعه", 6 to "شنبه", 7 to "یکشنبه")
    private val weekdaysEn = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")

    /** Saturday first, like the Iranian week. */
    val WEEK_ORDER = listOf(6, 7, 1, 2, 3, 4, 5)

    fun digits(text: String, fa: Boolean): String = if (fa) text.toPersianDigits() else text

    fun weekday(isoDay: Int, fa: Boolean): String =
        (if (fa) weekdaysFa[isoDay] else weekdaysEn[isoDay]) ?: ""

    /** "09:30 صبح" / "09:30 AM". */
    fun clock(hour: Int, minute: Int, fa: Boolean): String {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val displayHour = if (h == 0) 12 else if (h > 12) h - 12 else h
        val suffix = if (fa) {
            when (h) {
                in 0..3 -> "بامداد"
                in 4..11 -> "صبح"
                in 12..13 -> "ظهر"
                in 14..16 -> "بعدازظهر"
                in 17..19 -> "عصر"
                else -> "شب"
            }
        } else {
            if (h < 12) "AM" else "PM"
        }
        return digits("%02d:%02d %s".format(Locale.US, displayHour, m, suffix), fa)
    }

    /** "09:30" in 24-hour form, used where space is tight. */
    fun clock24(hour: Int, minute: Int, fa: Boolean): String =
        digits("%02d:%02d".format(Locale.US, hour.coerceIn(0, 23), minute.coerceIn(0, 59)), fa)

    /** "جمعه ۶ آذر ۱۴۰۵" / "Fri, 27 Nov 2026". */
    fun date(date: LocalDate, fa: Boolean, withWeekday: Boolean = true): String {
        if (fa) {
            val j = PersianDate.fromGregorian(date)
            val core = "${j.day} ${PersianDate.monthName(j.month)} ${j.year}".toPersianDigits()
            return if (withWeekday) weekday(date.dayOfWeek.value, true) + " " + core else core
        }
        val pattern = if (withWeekday) "EEE, d MMM yyyy" else "d MMM yyyy"
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
    }

    /** "۶ آذر" without the year, for compact labels. */
    fun shortDate(date: LocalDate, fa: Boolean): String {
        if (fa) {
            val j = PersianDate.fromGregorian(date)
            return "${j.day} ${PersianDate.monthName(j.month)}".toPersianDigits()
        }
        return date.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    }

    fun dateTime(at: LocalDateTime, fa: Boolean): String =
        date(at.toLocalDate(), fa) + " · " + clock(at.hour, at.minute, fa)

    /** "۱ ساعت و ۳۰ دقیقه" / "1 h 30 min". */
    fun duration(minutes: Int, fa: Boolean): String {
        val safe = minutes.coerceAtLeast(0)
        val h = safe / 60
        val m = safe % 60
        val text = if (fa) {
            when {
                h == 0 -> "$m دقیقه"
                m == 0 -> "$h ساعت"
                else -> "$h ساعت و $m دقیقه"
            }
        } else {
            when {
                h == 0 -> "$m min"
                m == 0 -> "$h h"
                else -> "$h h $m min"
            }
        }
        return digits(text, fa)
    }

    /** "۲ ساعت و ۵ دقیقه دیگر" / "in 2 h 5 min". */
    fun relative(fromMillis: Long, toMillis: Long, fa: Boolean): String {
        val minutes = ((toMillis - fromMillis) / 60_000L).coerceAtLeast(0L)
        if (minutes < 1L) return if (fa) "کمتر از یک دقیقه دیگر" else "in less than a minute"
        val days = minutes / (24L * 60L)
        val text = if (days >= 2L) {
            if (fa) "$days روز دیگر" else "in $days days"
        } else {
            val core = duration(minutes.toInt(), fa)
            if (fa) "$core دیگر" else "in $core"
        }
        return digits(text, fa)
    }

    /** Day-repetition summary, for example "هر هفته: شنبه، دوشنبه". */
    fun repeatSummary(reminder: Reminder, fa: Boolean): String {
        val start = reminder.startDate
        val base = when (reminder.repeat) {
            RepeatRule.ONCE -> date(start, fa)
            RepeatRule.DAILY -> if (fa) "هر روز" else "Every day"
            RepeatRule.WEEKLY -> {
                val names = reminder.effectiveWeekdays().sortedBy { WEEK_ORDER.indexOf(it) }.joinToString(if (fa) "، " else ", ") { weekday(it, fa) }
                if (fa) "هر هفته: $names" else "Weekly: $names"
            }
            RepeatRule.MONTHLY -> {
                val j = PersianDate.fromGregorian(start)
                if (fa) digits("هر ماه، روز ${j.day} (شمسی)", true) else "Monthly, day ${j.day} of the Jalali month"
            }
            RepeatRule.YEARLY -> {
                val j = PersianDate.fromGregorian(start)
                if (fa) digits("هر سال، ${j.day} ${PersianDate.monthName(j.month)}", true) else "Yearly, Jalali ${j.day}/${j.month}"
            }
        }
        val from = if (reminder.repeat != RepeatRule.ONCE && start.isAfter(LocalDate.now())) {
            if (fa) " · از ${shortDate(start, true)}" else " · from ${shortDate(start, false)}"
        } else {
            ""
        }
        val until = reminder.untilDate?.takeIf { reminder.repeat != RepeatRule.ONCE }?.let {
            if (fa) " · تا ${date(it, true, withWeekday = false)}" else " · until ${date(it, false, withWeekday = false)}"
        } ?: ""
        return base + from + until
    }

    /** Time summary, for example "هر ۱ ساعت از ۰۹:۰۰ تا ۲۱:۰۰ (۱۳ بار)". */
    fun timeSummary(reminder: Reminder, fa: Boolean): String {
        val start = clock(reminder.safeHour, reminder.safeMinute, fa)
        if (!reminder.repeatsWithinDay) return start
        val rings = ReminderSchedule.timesOfDay(reminder)
        if (rings.size <= 1) return start
        val last = rings.last()
        val end = clock(last / 60, last % 60, fa)
        val every = duration(reminder.intervalMinutes.coerceIn(ReminderSchedule.MIN_INTERVAL, ReminderSchedule.MAX_INTERVAL), fa)
        return if (fa) {
            "هر $every از $start تا $end (${digits(rings.size.toString(), true)} بار)"
        } else {
            "Every $every from $start to $end (${rings.size} times)"
        }
    }

    fun fullSummary(reminder: Reminder, fa: Boolean): String =
        repeatSummary(reminder, fa) + " · " + timeSummary(reminder, fa)
}
