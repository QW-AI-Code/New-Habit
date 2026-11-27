package com.duck.twominute.reminders

import com.duck.twominute.JalaliDate
import com.duck.twominute.PersianDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The schedule math decides when a reminder rings, so every rule is pinned down
 * here: one-shot, "every hour on that day", weekly, Jalali monthly/yearly with
 * short months, end dates and "done for today".
 */
class ReminderScheduleTest {

    private val now = LocalDateTime.of(2026, 11, 27, 5, 49)

    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

    @Test
    fun `a one time reminder rings once at its date and time`() {
        val r = Reminder(startEpochDay = day(2026, 11, 28).toEpochDay(), hour = 9, minute = 30)
        assertEquals(listOf(LocalDateTime.of(2026, 11, 28, 9, 30)), ReminderSchedule.upcoming(r, now, 5))
    }

    @Test
    fun `a one time reminder in the past never rings`() {
        val r = Reminder(startEpochDay = day(2026, 11, 27).toEpochDay(), hour = 5, minute = 0)
        assertNull(ReminderSchedule.nextAfter(r, now))
    }

    @Test
    fun `every hour on one day rings from start to end inclusive and then stops`() {
        val r = Reminder(startEpochDay = day(2026, 11, 28).toEpochDay(), hour = 9, minute = 0, intervalMinutes = 60, endHour = 21, endMinute = 0)
        val all = ReminderSchedule.upcoming(r, now, 100)
        assertEquals(13, all.size)
        assertEquals(LocalDateTime.of(2026, 11, 28, 9, 0), all.first())
        assertEquals(LocalDateTime.of(2026, 11, 28, 21, 0), all.last())
        assertEquals(13, ReminderSchedule.timesOfDay(r).size)
    }

    @Test
    fun `the next ring after a ring is the following slot`() {
        val r = Reminder(startEpochDay = day(2026, 11, 28).toEpochDay(), hour = 9, minute = 0, intervalMinutes = 60, endHour = 21, endMinute = 0)
        assertEquals(LocalDateTime.of(2026, 11, 28, 11, 0), ReminderSchedule.nextAfter(r, LocalDateTime.of(2026, 11, 28, 10, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 28, 11, 0), ReminderSchedule.nextAfter(r, LocalDateTime.of(2026, 11, 28, 10, 30)))
    }

    @Test
    fun `an end before the start rings only once a day`() {
        val r = Reminder(startEpochDay = day(2026, 11, 28).toEpochDay(), hour = 18, minute = 0, intervalMinutes = 60, endHour = 9, endMinute = 0)
        assertEquals(listOf(18 * 60), ReminderSchedule.timesOfDay(r))
    }

    @Test
    fun `weekly rings only on the picked weekdays`() {
        // 2026-11-27 is a Friday. Picked: Saturday (6) and Monday (1).
        val r = Reminder(startEpochDay = day(2026, 11, 27).toEpochDay(), hour = 8, minute = 0, repeat = RepeatRule.WEEKLY, weekdays = listOf(6, 1))
        assertEquals(
            listOf(
                LocalDateTime.of(2026, 11, 28, 8, 0),
                LocalDateTime.of(2026, 11, 30, 8, 0),
                LocalDateTime.of(2026, 12, 5, 8, 0),
                LocalDateTime.of(2026, 12, 7, 8, 0)
            ),
            ReminderSchedule.upcoming(r, now, 4)
        )
    }

    @Test
    fun `monthly on the 31st rings on the last day of shorter Jalali months`() {
        val start = PersianDate.toGregorian(JalaliDate(1405, 6, 31))
        assertEquals(day(2026, 9, 22), start)
        val r = Reminder(startEpochDay = start.toEpochDay(), hour = 10, minute = 0, repeat = RepeatRule.MONTHLY)
        val next = ReminderSchedule.upcoming(r, LocalDateTime.of(2026, 9, 22, 11, 0), 2)
        assertEquals(LocalDateTime.of(2026, 10, 22, 10, 0), next[0])
        assertEquals(JalaliDate(1405, 7, 30), PersianDate.fromGregorian(next[0].toLocalDate()))
        assertEquals(JalaliDate(1405, 8, 30), PersianDate.fromGregorian(next[1].toLocalDate()))
    }

    @Test
    fun `yearly on 30 Esfand rings on 29 Esfand in common years`() {
        val start = PersianDate.toGregorian(JalaliDate(1403, 12, 30))
        assertEquals(day(2025, 3, 20), start)
        val r = Reminder(startEpochDay = start.toEpochDay(), hour = 10, minute = 0, repeat = RepeatRule.YEARLY)
        val next = ReminderSchedule.nextAfter(r, LocalDateTime.of(2025, 3, 20, 11, 0))
        assertEquals(LocalDateTime.of(2026, 3, 20, 10, 0), next)
        assertEquals(JalaliDate(1404, 12, 29), PersianDate.fromGregorian(next!!.toLocalDate()))
    }

    @Test
    fun `an end date stops a daily reminder`() {
        val r = Reminder(startEpochDay = day(2026, 11, 27).toEpochDay(), hour = 7, minute = 0, repeat = RepeatRule.DAILY, untilEpochDay = day(2026, 11, 29).toEpochDay())
        assertEquals(3, ReminderSchedule.upcoming(r, now, 10).size)
    }

    @Test
    fun `done for today skips the rest of today`() {
        val r = Reminder(
            startEpochDay = day(2026, 11, 27).toEpochDay(), hour = 6, minute = 0, repeat = RepeatRule.DAILY,
            intervalMinutes = 120, endHour = 12, endMinute = 0, doneEpochDays = listOf(day(2026, 11, 27).toEpochDay())
        )
        assertEquals(LocalDateTime.of(2026, 11, 28, 6, 0), ReminderSchedule.nextAfter(r, now))
    }

    @Test
    fun `a missed ring is found in the window`() {
        val r = Reminder(startEpochDay = day(2026, 11, 27).toEpochDay(), hour = 9, minute = 0, intervalMinutes = 60, endHour = 12, endMinute = 0)
        val missed = ReminderSchedule.lastInWindow(r, LocalDateTime.of(2026, 11, 27, 8, 0), LocalDateTime.of(2026, 11, 27, 10, 30))
        assertEquals(LocalDateTime.of(2026, 11, 27, 10, 0), missed)
    }

    @Test
    fun `old done days are pruned`() {
        val today = day(2026, 11, 27)
        val pruned = ReminderSchedule.pruneDoneDays(listOf(today.toEpochDay() - 10, today.toEpochDay(), today.toEpochDay()), today)
        assertEquals(listOf(today.toEpochDay()), pruned)
    }

    @Test
    fun `Jalali conversion matches known dates and round trips`() {
        assertEquals(JalaliDate(1405, 9, 6), PersianDate.fromGregorian(day(2026, 11, 27)))
        assertEquals(JalaliDate(1404, 1, 1), PersianDate.fromGregorian(day(2025, 3, 21)))
        assertEquals(JalaliDate(1403, 12, 30), PersianDate.fromGregorian(day(2025, 3, 20)))
        assertTrue(PersianDate.isLeapYear(1403))
        var cursor = day(2020, 1, 1)
        while (cursor.isBefore(day(2032, 1, 1))) {
            assertEquals(cursor, PersianDate.toGregorian(PersianDate.fromGregorian(cursor)))
            cursor = cursor.plusDays(1)
        }
    }
}
