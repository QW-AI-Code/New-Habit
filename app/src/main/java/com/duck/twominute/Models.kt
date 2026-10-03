package com.duck.twominute

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Epoch millis to a local calendar day. */
fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@Serializable
data class Identity(
    val id: Long = System.currentTimeMillis(),
    val titleFa: String = "",
    val titleEn: String = "",
    val ritualFa: String = "",
    val ritualEn: String = "",
    /** The reason behind the identity. Shown on the focus card as a nudge. */
    val whyFa: String = "",
    val whyEn: String = "",
    /** Ritual length in seconds. Free choice, no longer locked to two minutes. */
    val durationSeconds: Int = DEFAULT_DURATION,
    /** How many repetitions count as a finished day. */
    val timesPerDay: Int = 1,
    /** v1.0.0 single reminder. Still written (mirrors the first slot) so a downgrade keeps working. */
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    /** One entry per repetition of the day; see [reminderSlots]. Added in v1.0.1. */
    val reminderTimes: List<ReminderTime> = emptyList(),
    /** ISO weekdays (1 = Monday .. 7 = Sunday). Empty means every day. */
    val reminderDays: List<Int> = emptyList(),
    val soundUri: String? = null,
    val iconKey: String = "spark",
    val colorHex: String = "#39D6C3",
    val archived: Boolean = false,
    val successes: List<Long> = emptyList()
) {

    fun title(fa: Boolean): String =
        if (fa) titleFa.ifBlank { titleEn } else titleEn.ifBlank { titleFa }

    fun ritual(fa: Boolean): String =
        if (fa) ritualFa.ifBlank { ritualEn } else ritualEn.ifBlank { ritualFa }

    fun why(fa: Boolean): String =
        if (fa) whyFa.ifBlank { whyEn } else whyEn.ifBlank { whyFa }

    fun hasReminder(): Boolean = reminderTimes.isNotEmpty() || reminderHour != null

    /**
     * The reminders that are actually scheduled: exactly [safeTimesPerDay] distinct
     * times when reminders are on, nothing when they are off. A v1.0.0 identity
     * that stored one time for several repetitions is grown to the right count here.
     */
    fun reminderSlots(): List<ReminderTime> {
        val stored = when {
            reminderTimes.isNotEmpty() -> reminderTimes
            reminderHour != null -> listOf(ReminderTime(reminderHour, reminderMinute ?: 0))
            else -> return emptyList()
        }
        return ReminderPlanner.fit(stored, safeTimesPerDay)
    }

    /** Stores [slots] fitted to the repetitions of the day; an empty list switches reminders off. */
    fun withReminders(slots: List<ReminderTime>): Identity {
        if (slots.isEmpty()) return copy(reminderTimes = emptyList(), reminderHour = null, reminderMinute = null)
        val fitted = ReminderPlanner.fit(slots, safeTimesPerDay)
        val first = fitted.first()
        return copy(reminderTimes = fitted, reminderHour = first.hour, reminderMinute = first.minute)
    }

    /** Changes the repetitions of the day and keeps the reminder count equal to it. */
    fun withTimesPerDay(times: Int): Identity {
        val updated = copy(timesPerDay = times.coerceIn(1, MAX_TIMES_PER_DAY))
        return if (hasReminder()) updated.withReminders(reminderSlots()) else updated
    }

    val safeDuration: Int
        get() = durationSeconds.coerceIn(MIN_DURATION, MAX_DURATION)

    val safeTimesPerDay: Int
        get() = timesPerDay.coerceIn(1, MAX_TIMES_PER_DAY)

    /** Every day when reminderDays is empty, otherwise only the picked weekdays. */
    fun runsOn(date: LocalDate): Boolean =
        reminderDays.isEmpty() || reminderDays.contains(date.dayOfWeek.value)

    fun successDates(): Set<LocalDate> {
        val days = LinkedHashSet<LocalDate>(successes.size)
        successes.forEach { days.add(it.toLocalDate()) }
        return days
    }

    /**
     * Days on which every repetition was done. With "3 times a day" a day with a
     * single log is progress, not a finished day, so it must not extend a streak.
     */
    fun completedDates(): Set<LocalDate> {
        val counts = HashMap<LocalDate, Int>()
        successes.forEach { stamp ->
            val day = stamp.toLocalDate()
            counts[day] = (counts[day] ?: 0) + 1
        }
        val goal = safeTimesPerDay
        return counts.filterValues { it >= goal }.keys
    }

    fun countOn(date: LocalDate): Int = successes.count { it.toLocalDate() == date }

    fun doneOn(date: LocalDate): Boolean = countOn(date) >= safeTimesPerDay

    val doneToday: Boolean
        get() = doneOn(LocalDate.now())

    val todayCount: Int
        get() = countOn(LocalDate.now())

    /**
     * Consecutive completed days counting back from today. A day the identity is
     * not scheduled for never breaks the chain, and today stays open until it is
     * actually finished.
     */
    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        val days = completedDates()
        if (days.isEmpty()) return 0
        var cursor = if (days.contains(today)) today else today.minusDays(1)
        var streak = 0
        var guard = 0
        while (guard < 4000) {
            guard += 1
            if (days.contains(cursor)) {
                streak += 1
            } else if (runsOn(cursor)) {
                break
            }
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Same rules as [currentStreak]: a day the identity does not run on never breaks a chain. */
    fun bestStreak(): Int {
        val days = completedDates().sorted()
        if (days.isEmpty()) return 0
        var best = 1
        var run = 1
        for (index in 1 until days.size) {
            var gapBreaks = false
            var cursor = days[index - 1].plusDays(1)
            var guard = 0
            while (cursor.isBefore(days[index]) && guard < 4000) {
                guard += 1
                if (runsOn(cursor)) {
                    gapBreaks = true
                    break
                }
                cursor = cursor.plusDays(1)
            }
            run = if (gapBreaks) 1 else run + 1
            if (run > best) best = run
        }
        return best
    }

    /** Share of the last [window] days that were completed, as 0f..1f. */
    fun consistency(window: Int = 30, today: LocalDate = LocalDate.now()): Float {
        val days = completedDates()
        var scheduled = 0
        var hit = 0
        for (back in 0 until window) {
            val day = today.minusDays(back.toLong())
            if (!runsOn(day)) continue
            scheduled += 1
            if (days.contains(day)) hit += 1
        }
        if (scheduled == 0) return 0f
        return hit.toFloat() / scheduled.toFloat()
    }

    val totalSeconds: Int
        get() = successes.size * safeDuration

    companion object {
        const val DEFAULT_DURATION = 120
        const val MIN_DURATION = 10
        const val MAX_DURATION = 7200
        const val MAX_TIMES_PER_DAY = ReminderPlanner.MAX_SLOTS

        val DURATION_PRESETS = listOf(30, 60, 120, 300, 600, 900, 1200, 1800)
    }
}

@Serializable
data class AppState(
    val identities: List<Identity> = emptyList(),
    val currentIndex: Int = 0,
    val language: String = "fa",
    /** Length used for brand new identities. */
    val defaultDurationSeconds: Int = Identity.DEFAULT_DURATION,
    /** How many rituals a day counts as a win. */
    val dailyGoal: Int = 1,
    val vibrate: Boolean = true,
    val chime: Boolean = true,
    val keepScreenOn: Boolean = true,
    val showArchived: Boolean = false
) {

    val isPersian: Boolean get() = language == "fa"

    val active: List<Identity> get() = identities.filterNot { it.archived }

    val archived: List<Identity> get() = identities.filter { it.archived }

    fun current(): Identity? {
        val list = active
        if (list.isEmpty()) return null
        return list[currentIndex.coerceIn(0, list.size - 1)]
    }

    fun completionsOn(date: LocalDate): Int = identities.sumOf { it.countOn(date) }

    val goalToday: Int get() = dailyGoal.coerceIn(1, 20)

    val doneToday: Int get() = completionsOn(LocalDate.now())

    /** Days in a row where the daily goal was met. */
    fun goalStreak(today: LocalDate = LocalDate.now()): Int {
        var cursor = if (completionsOn(today) >= goalToday) today else today.minusDays(1)
        var streak = 0
        var guard = 0
        while (guard < 4000) {
            guard += 1
            if (completionsOn(cursor) < goalToday) break
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun bestGoalStreak(): Int {
        val days = identities.flatMap { it.successDates() }.distinct().sorted()
        if (days.isEmpty()) return 0
        val qualified = days.filter { completionsOn(it) >= goalToday }
        if (qualified.isEmpty()) return 0
        var best = 1
        var run = 1
        for (index in 1 until qualified.size) {
            run = if (qualified[index - 1].plusDays(1) == qualified[index]) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    val totalCompletions: Int get() = identities.sumOf { it.successes.size }

    val totalMinutes: Int get() = identities.sumOf { it.totalSeconds } / 60

    fun identitiesDoneOn(date: LocalDate): List<Identity> =
        identities.filter { it.countOn(date) > 0 }
}
