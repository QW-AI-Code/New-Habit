package com.duck.twominute

import kotlinx.serialization.Serializable

/** One reminder slot of an identity: a time of day, minute precision. */
@Serializable
data class ReminderTime(
    val hour: Int = 9,
    val minute: Int = 0
) {
    val safeHour: Int get() = hour.coerceIn(0, 23)
    val safeMinute: Int get() = minute.coerceIn(0, 59)

    /** 0 .. 1439. */
    val minuteOfDay: Int get() = safeHour * 60 + safeMinute

    fun sanitized(): ReminderTime = ReminderTime(safeHour, safeMinute)

    companion object {
        fun ofMinuteOfDay(value: Int): ReminderTime {
            val wrapped = ((value % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
            return ReminderTime(wrapped / 60, wrapped % 60)
        }

        const val MINUTES_PER_DAY = 1440
    }
}

/**
 * The one place that decides how many reminders an identity has and when.
 *
 * The invariant every caller relies on: when reminders are switched on, an
 * identity has exactly `timesPerDay` slots, all at different minutes, sorted from
 * the earliest. v1.0.0 stored a single time no matter how many repetitions a day
 * the identity asked for; [fit] grows (or trims) any stored list to the right size.
 */
object ReminderPlanner {

    /** Latest time a suggested slot is placed at. */
    const val DAY_END = 22 * 60

    /** Spacing used when a new slot is appended after the last one. */
    const val DEFAULT_STEP = 180

    /** Smallest distance between two suggested slots. */
    const val MIN_GAP = 15

    /**
     * Exactly [count] distinct, sorted slots. Existing times are kept (duplicates
     * collapse into one), the latest ones are dropped when there are too many, and
     * missing ones are suggested in the free time of the day.
     */
    fun fit(stored: List<ReminderTime>, count: Int): List<ReminderTime> {
        val wanted = count.coerceIn(1, MAX_SLOTS)
        val minutes = stored.map { it.minuteOfDay }.distinct().sorted().toMutableList()
        if (minutes.isEmpty()) minutes += 9 * 60
        while (minutes.size > wanted) minutes.removeAt(minutes.size - 1)
        var guard = 0
        while (minutes.size < wanted && guard < MAX_SLOTS * 4) {
            guard += 1
            val next = suggestNext(minutes) ?: break
            minutes += next
            minutes.sort()
        }
        return minutes.map { ReminderTime.ofMinuteOfDay(it) }
    }

    /**
     * Evenly spreads [count] slots from [start] to [end] (inclusive). Used by the
     * "spread evenly" action in the editor and by the AI planner.
     */
    fun spread(start: ReminderTime, end: ReminderTime, count: Int): List<ReminderTime> {
        val wanted = count.coerceIn(1, MAX_SLOTS)
        if (wanted == 1) return listOf(start.sanitized())
        val from = start.minuteOfDay
        var to = end.minuteOfDay
        if (to <= from) to = (from + DEFAULT_STEP * (wanted - 1)).coerceAtMost(ReminderTime.MINUTES_PER_DAY - 1)
        val span = (to - from).coerceAtLeast(wanted - 1)
        val result = (0 until wanted).map { index -> from + (span * index) / (wanted - 1) }
        return fit(result.map { ReminderTime.ofMinuteOfDay(it) }, wanted)
    }

    /** True when two slots share the same minute. */
    fun hasDuplicates(slots: List<ReminderTime>): Boolean =
        slots.map { it.minuteOfDay }.distinct().size != slots.size

    private fun suggestNext(taken: List<Int>): Int? {
        val sorted = taken.sorted()
        val last = sorted.last()
        // 1) After the last slot, if the evening still has room.
        val tail = DAY_END - last
        if (tail >= 60) return last + minOf(DEFAULT_STEP, tail)
        // 2) In the middle of the largest gap between two slots.
        var bestStart = -1
        var bestGap = 0
        for (index in 1 until sorted.size) {
            val gap = sorted[index] - sorted[index - 1]
            if (gap > bestGap) {
                bestGap = gap
                bestStart = sorted[index - 1]
            }
        }
        if (bestStart >= 0 && bestGap >= MIN_GAP * 2) return bestStart + bestGap / 2
        // 3) Before the first slot, from 06:00 on.
        val first = sorted.first()
        if (first - 6 * 60 >= MIN_GAP * 2) return first - (first - 6 * 60) / 2
        // 4) Anywhere that is still free, minute by minute after the last slot.
        val set = sorted.toHashSet()
        for (offset in 1 until ReminderTime.MINUTES_PER_DAY) {
            val candidate = (last + offset * MIN_GAP) % ReminderTime.MINUTES_PER_DAY
            if (candidate !in set) return candidate
        }
        return null
    }

    const val MAX_SLOTS = 20
}
