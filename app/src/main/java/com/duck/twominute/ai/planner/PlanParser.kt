package com.duck.twominute.ai.planner

import com.duck.twominute.Identity
import com.duck.twominute.ReminderPlanner
import com.duck.twominute.ReminderTime
import kotlinx.serialization.json.Json

/**
 * Turns the model's answer into a plan the app can trust.
 *
 * A schema makes Gemini's output well-formed most of the time, not always, and a
 * model can still invent an icon, repeat a reminder time or forget one. Nothing
 * here throws on bad values: they are clamped, replaced or dropped, and the
 * reminder list of every habit always ends up exactly `timesPerDay` long.
 */
object PlanParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val encoder = Json { encodeDefaults = true }

    const val MAX_HABITS = 8
    const val MAX_TIMES_PER_DAY = 6
    const val MAX_LIST = 12

    /** Null when the text contains no readable plan at all. */
    fun parse(raw: String): HabitPlan? {
        val body = extractObject(raw) ?: return null
        val decoded = runCatching { json.decodeFromString(HabitPlan.serializer(), body) }.getOrNull() ?: return null
        val clean = sanitize(decoded)
        return if (clean.habits.isEmpty()) null else clean
    }

    fun encode(plan: HabitPlan): String = encoder.encodeToString(HabitPlan.serializer(), plan)

    fun decodeStored(raw: String): HabitPlan? =
        if (raw.isBlank()) null else runCatching { json.decodeFromString(HabitPlan.serializer(), raw) }.getOrNull()

    /** Removes ```json fences or any chatter around the JSON object. */
    fun extractObject(raw: String): String? {
        val text = raw.trim()
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return text.substring(start, end + 1)
    }

    /** "7:30", "07:30", "19:05", "۰۷:۳۰" → time; anything else → null. */
    fun parseTime(raw: String): ReminderTime? {
        val ascii = buildString {
            raw.trim().forEach { ch ->
                append(
                    when (ch) {
                        in '۰'..'۹' -> '0' + (ch - '۰')
                        in '٠'..'٩' -> '0' + (ch - '٠')
                        '：', '.' -> ':'
                        else -> ch
                    }
                )
            }
        }
        val match = Regex("""^(\d{1,2}):(\d{2})""").find(ascii) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return ReminderTime(hour, minute)
    }

    fun format(time: ReminderTime): String = "%02d:%02d".format(java.util.Locale.US, time.safeHour, time.safeMinute)

    fun sanitize(plan: HabitPlan): HabitPlan {
        val icons = PlanPrompt.iconKeys.toSet()
        val palette = PlanPrompt.colors
        val weeks = plan.totalWeeks.coerceIn(1, 52)
        val phases = plan.phases
            .filter { it.titleFa.isNotBlank() || it.titleEn.isNotBlank() }
            .take(MAX_LIST)
            .map { phase ->
                val start = phase.startWeek.coerceIn(1, weeks)
                phase.copy(startWeek = start, endWeek = phase.endWeek.coerceIn(start, weeks))
            }
        val usedColors = mutableSetOf<String>()
        val habits = plan.habits
            .filter { it.titleFa.isNotBlank() || it.titleEn.isNotBlank() }
            .take(MAX_HABITS)
            .mapIndexed { index, habit ->
                val times = habit.timesPerDay.coerceIn(1, MAX_TIMES_PER_DAY)
                val parsed = habit.reminderTimes.mapNotNull { parseTime(it) }
                val fitted = ReminderPlanner.fit(parsed.ifEmpty { listOf(ReminderTime(9, 0)) }, times)
                val color = habit.colorHex.uppercase().takeIf { candidate -> palette.any { it.equals(candidate, ignoreCase = true) } }
                    ?.let { candidate -> palette.first { it.equals(candidate, ignoreCase = true) } }
                    ?.takeIf { it !in usedColors || usedColors.size >= palette.size }
                    ?: palette.firstOrNull { it !in usedColors }
                    ?: palette[index % palette.size]
                usedColors += color
                habit.copy(
                    durationMinutes = habit.durationMinutes.coerceIn(1, Identity.MAX_DURATION / 60),
                    timesPerDay = times,
                    reminderTimes = fitted.map { format(it) },
                    weekdays = habit.weekdays.filter { it in 1..7 }.distinct().sorted().let { if (it.size == 7) emptyList() else it },
                    iconKey = habit.iconKey.takeIf { it in icons } ?: "spark",
                    colorHex = color,
                    phase = habit.phase.coerceIn(1, phases.size.coerceAtLeast(1)),
                )
            }
        return plan.copy(
            totalWeeks = weeks,
            methods = plan.methods.filter { it.nameFa.isNotBlank() || it.nameEn.isNotBlank() }.take(MAX_LIST),
            phases = phases,
            habits = habits,
            milestones = plan.milestones
                .filter { it.fa.isNotBlank() || it.en.isNotBlank() }
                .map { it.copy(week = it.week.coerceIn(1, weeks)) }
                .sortedBy { it.week }
                .take(MAX_LIST),
            obstacles = plan.obstacles.filter { it.ifFa.isNotBlank() || it.ifEn.isNotBlank() }.take(MAX_LIST),
            tipsFa = plan.tipsFa.filter { it.isNotBlank() }.take(MAX_LIST),
            tipsEn = plan.tipsEn.filter { it.isNotBlank() }.take(MAX_LIST),
        )
    }
}
