package com.duck.twominute.ai.usage

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Gemini's free daily quota is counted per calendar day in Pacific time and resets
 * at midnight there — not at midnight on the user's phone. Everything that talks
 * about "today" in terms of quota uses this clock.
 */
object QuotaClock {

    private val PACIFIC: TimeZone = TimeZone.getTimeZone("America/Los_Angeles")

    /** `2026-09-28` — the quota day [nowMs] falls into. */
    fun dayKey(nowMs: Long = System.currentTimeMillis()): String {
        val c = Calendar.getInstance(PACIFIC).apply { timeInMillis = nowMs }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** Milliseconds until the next reset. */
    fun millisUntilReset(nowMs: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance(PACIFIC).apply { timeInMillis = nowMs }
        val reset = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (reset.timeInMillis - nowMs).coerceAtLeast(0L)
    }
}
