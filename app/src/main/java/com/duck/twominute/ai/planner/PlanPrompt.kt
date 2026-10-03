package com.duck.twominute.ai.planner

import com.duck.twominute.ui.accentPalette
import com.duck.twominute.ui.identityGlyphs
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

/** What the user told the planner. */
data class PlanRequest(
    val goal: String,
    val level: Level = Level.BEGINNER,
    /** Total minutes a day the user can really spend. */
    val minutesPerDay: Int = 20,
    val timeOfDay: TimeOfDay = TimeOfDay.FLEXIBLE,
    val daysPerWeek: Int = 7,
    val intensity: Intensity = Intensity.BALANCED,
    val weeks: Int = 8,
    /** Free text: constraints, injuries, schedule, preferences. */
    val notes: String = "",
    val persianUi: Boolean = true,
    /** Set when the user asks to change an existing plan. */
    val previousPlanJson: String? = null,
    val feedback: String = "",
) {
    enum class Level(val promptName: String) {
        BEGINNER("complete beginner"),
        ELEMENTARY("elementary / some experience"),
        INTERMEDIATE("intermediate"),
        ADVANCED("advanced"),
    }

    enum class TimeOfDay(val promptName: String) {
        MORNING("mornings"),
        MIDDAY("around midday"),
        EVENING("evenings"),
        FLEXIBLE("flexible, spread over the day"),
    }

    enum class Intensity(val promptName: String, val habitRange: String) {
        LIGHT("light: protect motivation, minimum viable doses", "2-3"),
        BALANCED("balanced", "3-4"),
        INTENSIVE("intensive: ambitious but sustainable", "4-6"),
    }
}

/**
 * The prompt and the JSON schema of the AI planner.
 *
 * The system instruction encodes current, evidence-based behaviour-change practice
 * so every plan — "learn English", "build my ideal body", or any of thousands of
 * other identities — is built the same principled way, and it maps the answer
 * onto exactly what the app can store (duration, repetitions, reminders, icons).
 */
object PlanPrompt {

    val iconKeys: List<String> get() = identityGlyphs.map { it.key }
    val colors: List<String> get() = accentPalette

    fun systemInstruction(): String = """
You are "New Habit Coach", a world-class behaviour-change designer, learning scientist and certified coach.
You turn ONE goal into an identity-based habit plan for the Android app "New Habit". Users write in Persian or English.

METHOD — apply the current evidence base, never generic advice:
1. Identity first (identity-based habits): define who the user becomes ("I am someone who …"); every habit is a vote for that identity.
2. Tiny start, then progressive overload: begin below the motivation threshold (2-minute rule / Tiny Habits), grow load step by step only once the habit is automatic. Each habit gets a concrete level-up rule.
3. Implementation intentions and habit stacking: every habit has an anchor cue — "After I [existing routine], I will [habit] at [place]".
4. Environment design and friction: make the good behaviour obvious and easy, the bad one invisible and hard.
5. WOOP / mental contrasting: name the realistic obstacles and give an if-then plan for each.
6. Consistency over intensity: "never miss twice", minimum viable version on bad days. Automaticity takes ~66 days on average (range 18-254), so set expectations honestly.
7. Domain science — use the best current methods of the field. Examples:
   - Languages: CEFR levels, comprehensible input, spaced repetition (SRS) and active recall, shadowing, output practice with feedback, high-frequency vocabulary first, a weekly speaking session.
   - Fitness / body composition: WHO activity guidelines (150-300 min moderate or 75-150 min vigorous cardio a week, strength training ≥2 days a week), progressive overload, ~1.6 g/kg/day protein for muscle gain, 7-9 h sleep, deload weeks, mobility, step count, moderate calorie deficit (not crash diets) for fat loss.
   - Skills (coding, music, writing, speaking): deliberate practice at the edge of ability, project-based learning, fast feedback loops, interleaving, retrieval practice.
   - Mind and wellbeing: evidence-based mindfulness and breathing, sleep hygiene, journaling, gratitude, digital boundaries.
   - Money, career, study: automation, measurable weekly reviews, time blocking, deep work, Pomodoro, the Feynman technique.
8. Measurable milestones on a week timeline and a weekly review habit when it helps.

APP CONSTRAINTS — the answer is imported directly, so follow them exactly:
- Each habit: durationMinutes is an integer 1-120 (the timer length of ONE repetition). timesPerDay is an integer 1-6.
- reminderTimes: EXACTLY timesPerDay strings "HH:mm" (24-hour), all different, sensible for the user's preferred time of day and spaced apart.
- weekdays: ISO numbers 1=Monday … 7=Sunday. Use an empty array for every day. Rest days for strength work are welcome.
- The sum of (durationMinutes × timesPerDay) over all habits that run on the same day must respect the user's daily minutes.
- iconKey must be one of: ${iconKeys.joinToString(", ")}.
- colorHex must be one of: ${colors.joinToString(", ")}. Give different habits different colours.
- phase is the 1-based index of the phase in which the habit starts; early phases have fewer and smaller habits.
- titleFa/titleEn is a short identity sentence ("من کسی هستم که هر روز انگلیسی گوش می‌دهد" / "I am someone who listens to English daily"), max ~60 characters.
- ritualFa/ritualEn is the concrete action with its amount, max ~90 characters.

LANGUAGE: Every *Fa field is natural, fluent, modern Persian (not a literal translation, Persian digits are fine). Every *En field is natural English. Never leave one of the pair empty.

SAFETY: For health, fitness, diet, mental-health or medical goals give conservative, safe guidance and put a short professional-advice note in safetyFa/safetyEn; otherwise leave them empty. Never recommend extreme diets, dangerous loads or anything illegal. If the goal is harmful, reframe it into its healthy version.

Return ONLY the JSON object of the response schema. No markdown, no commentary.
""".trimIndent()

    fun userPayload(request: PlanRequest, today: LocalDate = LocalDate.now()): String = buildString {
        appendLine("TODAY: $today")
        appendLine("USER INTERFACE LANGUAGE: ${if (request.persianUi) "Persian" else "English"}")
        appendLine("GOAL: ${request.goal.trim()}")
        appendLine("CURRENT LEVEL: ${request.level.promptName}")
        appendLine("TIME AVAILABLE: about ${request.minutesPerDay} minutes per day in total")
        appendLine("PREFERRED TIME OF DAY: ${request.timeOfDay.promptName}")
        appendLine("ACTIVE DAYS PER WEEK: ${request.daysPerWeek}")
        appendLine("INTENSITY: ${request.intensity.promptName} — plan ${request.intensity.habitRange} habits")
        appendLine("PLAN LENGTH: ${request.weeks} weeks (totalWeeks = ${request.weeks})")
        if (request.notes.isNotBlank()) appendLine("PERSONAL NOTES AND CONSTRAINTS: ${request.notes.trim()}")
        val previous = request.previousPlanJson
        if (!previous.isNullOrBlank()) {
            appendLine()
            appendLine("THE USER ALREADY HAS THIS PLAN:")
            appendLine(previous)
            appendLine()
            appendLine("REVISE IT according to this feedback, keep what still fits, and return the full revised plan:")
            appendLine(request.feedback.ifBlank { "Make it more effective and easier to stick to." })
        }
    }

    /** Gemini response schema (OpenAPI subset). Mirrors [HabitPlan]. */
    fun schema(): JsonObject = obj(
        required = listOf(
            "planTitleFa", "planTitleEn", "identityFa", "identityEn", "summaryFa", "summaryEn",
            "totalWeeks", "methods", "phases", "habits", "milestones", "obstacles", "tipsFa", "tipsEn",
        ),
        properties = linkedMapOf(
            "planTitleFa" to str(), "planTitleEn" to str(),
            "identityFa" to str(), "identityEn" to str(),
            "summaryFa" to str(), "summaryEn" to str(),
            "totalWeeks" to int(),
            "methods" to arr(obj(
                required = listOf("nameFa", "nameEn", "howFa", "howEn"),
                properties = linkedMapOf("nameFa" to str(), "nameEn" to str(), "howFa" to str(), "howEn" to str()),
            )),
            "phases" to arr(obj(
                required = listOf("startWeek", "endWeek", "titleFa", "titleEn", "focusFa", "focusEn"),
                properties = linkedMapOf(
                    "startWeek" to int(), "endWeek" to int(),
                    "titleFa" to str(), "titleEn" to str(), "focusFa" to str(), "focusEn" to str(),
                ),
            )),
            "habits" to arr(obj(
                required = listOf(
                    "titleFa", "titleEn", "ritualFa", "ritualEn", "whyFa", "whyEn", "cueFa", "cueEn",
                    "levelUpFa", "levelUpEn", "durationMinutes", "timesPerDay", "reminderTimes",
                    "weekdays", "iconKey", "colorHex", "phase",
                ),
                properties = linkedMapOf(
                    "titleFa" to str(), "titleEn" to str(),
                    "ritualFa" to str(), "ritualEn" to str(),
                    "whyFa" to str(), "whyEn" to str(),
                    "cueFa" to str(), "cueEn" to str(),
                    "levelUpFa" to str(), "levelUpEn" to str(),
                    "durationMinutes" to int(),
                    "timesPerDay" to int(),
                    "reminderTimes" to arr(str()),
                    "weekdays" to arr(int()),
                    "iconKey" to enumOf(iconKeys),
                    "colorHex" to enumOf(colors),
                    "phase" to int(),
                ),
            )),
            "milestones" to arr(obj(
                required = listOf("week", "fa", "en"),
                properties = linkedMapOf("week" to int(), "fa" to str(), "en" to str()),
            )),
            "obstacles" to arr(obj(
                required = listOf("ifFa", "ifEn", "thenFa", "thenEn"),
                properties = linkedMapOf("ifFa" to str(), "ifEn" to str(), "thenFa" to str(), "thenEn" to str()),
            )),
            "tipsFa" to arr(str()),
            "tipsEn" to arr(str()),
            "safetyFa" to str(),
            "safetyEn" to str(),
        ),
    )

    private fun str(): JsonObject = buildJsonObject { put("type", "STRING") }
    private fun int(): JsonObject = buildJsonObject { put("type", "INTEGER") }
    private fun arr(items: JsonObject): JsonObject = buildJsonObject {
        put("type", "ARRAY")
        put("items", items)
    }

    private fun enumOf(values: List<String>): JsonObject = buildJsonObject {
        put("type", "STRING")
        put("format", "enum")
        put("enum", buildJsonArray { values.forEach { add(it) } })
    }

    private fun obj(required: List<String>, properties: LinkedHashMap<String, JsonObject>): JsonObject = buildJsonObject {
        put("type", "OBJECT")
        put("properties", buildJsonObject { properties.forEach { (name, schema) -> put(name, schema) } })
        put("required", buildJsonArray { required.forEach { add(it) } })
        put("propertyOrdering", buildJsonArray { properties.keys.forEach { add(it) } })
    }
}
