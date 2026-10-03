package com.duck.twominute.ai.planner

import com.duck.twominute.Identity
import com.duck.twominute.ReminderTime
import kotlinx.serialization.Serializable

/**
 * A complete identity plan as the AI planner returns it. Every text exists in
 * Persian and English because the app switches language at runtime.
 *
 * The field names are the JSON contract with Gemini (see [PlanPrompt.schema]);
 * every field has a default so a partial answer still decodes.
 */
@Serializable
data class HabitPlan(
    val planTitleFa: String = "",
    val planTitleEn: String = "",
    /** "I am someone who …" — the identity the whole plan votes for. */
    val identityFa: String = "",
    val identityEn: String = "",
    val summaryFa: String = "",
    val summaryEn: String = "",
    val totalWeeks: Int = 8,
    val methods: List<PlanMethod> = emptyList(),
    val phases: List<PlanPhase> = emptyList(),
    val habits: List<PlanHabit> = emptyList(),
    val milestones: List<PlanMilestone> = emptyList(),
    /** WOOP style "if obstacle, then action" plans. */
    val obstacles: List<PlanObstacle> = emptyList(),
    val tipsFa: List<String> = emptyList(),
    val tipsEn: List<String> = emptyList(),
    /** Set for health, fitness and diet goals; empty otherwise. */
    val safetyFa: String = "",
    val safetyEn: String = "",
) {
    fun title(fa: Boolean): String = pick(fa, planTitleFa, planTitleEn)
    fun identity(fa: Boolean): String = pick(fa, identityFa, identityEn)
    fun summary(fa: Boolean): String = pick(fa, summaryFa, summaryEn)
    fun tips(fa: Boolean): List<String> = if (fa) tipsFa.ifEmpty { tipsEn } else tipsEn.ifEmpty { tipsFa }
    fun safety(fa: Boolean): String = pick(fa, safetyFa, safetyEn)
}

@Serializable
data class PlanMethod(
    val nameFa: String = "",
    val nameEn: String = "",
    val howFa: String = "",
    val howEn: String = "",
) {
    fun name(fa: Boolean): String = pick(fa, nameFa, nameEn)
    fun how(fa: Boolean): String = pick(fa, howFa, howEn)
}

@Serializable
data class PlanPhase(
    val startWeek: Int = 1,
    val endWeek: Int = 2,
    val titleFa: String = "",
    val titleEn: String = "",
    val focusFa: String = "",
    val focusEn: String = "",
) {
    fun title(fa: Boolean): String = pick(fa, titleFa, titleEn)
    fun focus(fa: Boolean): String = pick(fa, focusFa, focusEn)
}

@Serializable
data class PlanHabit(
    val titleFa: String = "",
    val titleEn: String = "",
    val ritualFa: String = "",
    val ritualEn: String = "",
    val whyFa: String = "",
    val whyEn: String = "",
    /** Implementation intention: "After I …, I will …". */
    val cueFa: String = "",
    val cueEn: String = "",
    /** How the habit grows once it is automatic. */
    val levelUpFa: String = "",
    val levelUpEn: String = "",
    val durationMinutes: Int = 2,
    val timesPerDay: Int = 1,
    /** "HH:mm", 24 h. Exactly [timesPerDay] entries after sanitising. */
    val reminderTimes: List<String> = emptyList(),
    /** ISO weekdays, 1 = Monday … 7 = Sunday. Empty = every day. */
    val weekdays: List<Int> = emptyList(),
    val iconKey: String = "spark",
    val colorHex: String = "#39D6C3",
    /** The phase (1-based) in which this habit starts. */
    val phase: Int = 1,
) {
    fun title(fa: Boolean): String = pick(fa, titleFa, titleEn)
    fun ritual(fa: Boolean): String = pick(fa, ritualFa, ritualEn)
    fun why(fa: Boolean): String = pick(fa, whyFa, whyEn)
    fun cue(fa: Boolean): String = pick(fa, cueFa, cueEn)
    fun levelUp(fa: Boolean): String = pick(fa, levelUpFa, levelUpEn)

    fun parsedTimes(): List<ReminderTime> = reminderTimes.mapNotNull { PlanParser.parseTime(it) }

    /** The identity the app stores when the user adds this habit. */
    fun toIdentity(): Identity {
        val base = Identity(
            titleFa = titleFa.trim(),
            titleEn = titleEn.trim(),
            ritualFa = ritualFa.trim(),
            ritualEn = ritualEn.trim(),
            whyFa = whyFa.trim(),
            whyEn = whyEn.trim(),
            durationSeconds = (durationMinutes * 60).coerceIn(Identity.MIN_DURATION, Identity.MAX_DURATION),
            timesPerDay = timesPerDay.coerceIn(1, Identity.MAX_TIMES_PER_DAY),
            reminderDays = weekdays.filter { it in 1..7 }.distinct().sorted().let { if (it.size == 7) emptyList() else it },
            iconKey = iconKey,
            colorHex = colorHex,
        )
        val times = parsedTimes()
        return if (times.isEmpty()) base else base.withReminders(times)
    }
}

@Serializable
data class PlanMilestone(
    val week: Int = 1,
    val fa: String = "",
    val en: String = "",
) {
    fun text(fa: Boolean): String = pick(fa, this.fa, en)
}

@Serializable
data class PlanObstacle(
    val ifFa: String = "",
    val ifEn: String = "",
    val thenFa: String = "",
    val thenEn: String = "",
) {
    fun whenText(fa: Boolean): String = pick(fa, ifFa, ifEn)
    fun thenText(fa: Boolean): String = pick(fa, thenFa, thenEn)
}

private fun pick(fa: Boolean, faText: String, enText: String): String =
    if (fa) faText.ifBlank { enText } else enText.ifBlank { faText }
