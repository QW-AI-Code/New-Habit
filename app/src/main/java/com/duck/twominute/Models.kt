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
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
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

    fun hasReminder(): Boolean = reminderHour != null

    val safeDuration: Int
        get() = durationSeconds.coerceIn(MIN_DURATION, MAX_DURATION)

    val safeTimesPerDay: Int
        get() = timesPerDay.coerceIn(1, 20)

    /** Every day when reminderDays is empty, otherwise only the picked weekdays. */
    fun runsOn(date: LocalDate): Boolean =
        reminderDays.isEmpty() || reminderDays.contains(date.dayOfWeek.value)

    fun successDates(): Set<LocalDate> {
        val days = LinkedHashSet<LocalDate>(successes.size)
        successes.forEach { days.add(it.toLocalDate()) }
        return days
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
        val days = successDates()
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

    fun bestStreak(): Int {
        val days = successDates().sorted()
        if (days.isEmpty()) return 0
        var best = 1
        var run = 1
        for (index in 1 until days.size) {
            run = if (days[index - 1].plusDays(1) == days[index]) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /** Share of the last [window] days that were completed, as 0f..1f. */
    fun consistency(window: Int = 30, today: LocalDate = LocalDate.now()): Float {
        val days = successDates()
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
