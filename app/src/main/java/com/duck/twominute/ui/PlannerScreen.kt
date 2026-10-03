package com.duck.twominute.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duck.twominute.AppState
import com.duck.twominute.AppViewModel
import com.duck.twominute.ai.planner.HabitPlan
import com.duck.twominute.ai.planner.PlanHabit
import com.duck.twominute.ai.planner.PlanRequest
import com.duck.twominute.ai.planner.PlannerViewModel
import com.duck.twominute.toPersianDigits

/**
 * The AI habit planner: one goal in, a complete identity plan out (phases,
 * habits with cues and reminders, milestones, if-then plans), previewed before
 * anything is added to the app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(state: AppState, app: AppViewModel, planner: PlannerViewModel, onClose: () -> Unit) {
    val fa = state.isPersian
    val settings by planner.settings.collectAsStateWithLifecycle()
    val status by planner.status.collectAsStateWithLifecycle()
    val plan by planner.plan.collectAsStateWithLifecycle()
    val added by planner.added.collectAsStateWithLifecycle()
    val storedGoal by planner.goal.collectAsStateWithLifecycle()
    val models by planner.models.collectAsStateWithLifecycle()

    var aiSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var goal by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable { mutableStateOf(PlanRequest.Level.BEGINNER) }
    var minutes by rememberSaveable { mutableIntStateOf(20) }
    var timeOfDay by rememberSaveable { mutableStateOf(PlanRequest.TimeOfDay.FLEXIBLE) }
    var days by rememberSaveable { mutableIntStateOf(7) }
    var intensity by rememberSaveable { mutableStateOf(PlanRequest.Intensity.BALANCED) }
    var weeks by rememberSaveable { mutableIntStateOf(8) }
    var notes by rememberSaveable { mutableStateOf("") }
    var feedback by rememberSaveable { mutableStateOf("") }
    var showForm by rememberSaveable { mutableStateOf(true) }
    var note by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(storedGoal) { if (goal.isBlank() && storedGoal.isNotBlank()) goal = storedGoal }
    LaunchedEffect(plan == null) { showForm = plan == null }
    BackHandler(onBack = onClose)

    val working = status is PlannerViewModel.Status.Working
    fun num(value: Int): String = if (fa) value.toString().toPersianDigits() else value.toString()
    val keyCheck by planner.keyCheck.collectAsStateWithLifecycle()
    val connected = models.isNotEmpty()
    // No model name before the key has connected: the subtitle tells the truth.
    val modelLabel = when {
        settings.apiKey.isBlank() -> tr(fa, "کلید API وارد نشده", "No API key yet")
        !connected && keyCheck is PlannerViewModel.KeyCheck.Checking -> tr(fa, "در حال اتصال…", "Connecting…")
        !connected -> tr(fa, "اتصال تأیید نشده", "Not connected")
        else -> (models.firstOrNull { it.id == settings.model } ?: models.first()).label(fa)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr(fa, "بازگشت", "Back"), tint = Ink) }
            Column(Modifier.weight(1f)) {
                Text(tr(fa, "مربی هوشمند عادت", "AI habit coach"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(modelLabel, style = MaterialTheme.typography.labelMedium, color = if (connected) Muted else Amber, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton({ aiSettingsOpen = true }) { Icon(Icons.Rounded.Settings, tr(fa, "تنظیمات هوش مصنوعی", "AI settings"), tint = Turquoise) }
        }
        Spacer(Modifier.height(10.dp))

        if (settings.apiKey.isBlank()) {
            SectionCard {
                Text(tr(fa, "برای شروع یک کلید رایگان لازم است", "A free key is needed to start"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    tr(fa, "کلید رایگان Gemini را از Google AI Studio بگیر و در تنظیمات هوش مصنوعی وارد کن. هیچ هزینه‌ای ندارد.", "Get a free Gemini key from Google AI Studio and paste it into AI settings. It costs nothing."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                Spacer(Modifier.height(10.dp))
                Pill(tr(fa, "وارد کردن کلید", "Add key"), icon = Icons.Rounded.Settings, selected = true) { aiSettingsOpen = true }
            }
            Spacer(Modifier.height(12.dp))
        } else if (!connected && keyCheck !is PlannerViewModel.KeyCheck.Checking) {
            SectionCard {
                Text(tr(fa, "اتصال کلید تأیید نشده", "Key not connected yet"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    tr(fa, "کلید ذخیره شده ولی اتصالش هنوز موفق نبوده؛ تا اتصال برقرار نشود مدلی برای ساخت برنامه در دسترس نیست.", "The key is saved but has not connected yet; no model is available for planning until it does."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(tr(fa, "بررسی اتصال", "Test connection"), icon = Icons.Rounded.Refresh, selected = true) { planner.refreshModels() }
                    Pill(tr(fa, "تنظیمات هوش مصنوعی", "AI settings"), icon = Icons.Rounded.Settings) { aiSettingsOpen = true }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (plan != null && !showForm) {
            Pill(tr(fa, "برنامه تازه برای هدف دیگر", "New plan for another goal"), icon = Icons.Rounded.AutoAwesome) { showForm = true }
            Spacer(Modifier.height(12.dp))
        }

        AnimatedVisibility(visible = showForm) {
            SectionCard {
                Text(tr(fa, "هدفت چیست؟", "What is your goal?"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    tr(fa, "هر هدفی: یادگیری زبان، اندام ایده‌آل، کتاب‌خوانی، برنامه‌نویسی، آرامش ذهن…", "Any goal: learn a language, your ideal body, reading, coding, a calmer mind…"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Spacer(Modifier.height(10.dp))
                PlannerField(goal, { goal = it }, tr(fa, "مثلاً: می‌خواهم انگلیسی را روان صحبت کنم", "e.g. I want to speak English fluently"), singleLine = false)
                Spacer(Modifier.height(8.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    goalExamples(fa).forEach { example -> Pill(example, accent = Violet) { goal = example } }
                }

                Spacer(Modifier.height(16.dp))
                Label(tr(fa, "سطح فعلی", "Current level"))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanRequest.Level.entries.forEach { option -> Pill(levelLabel(option, fa), selected = level == option) { level = option } }
                }

                Spacer(Modifier.height(14.dp))
                Stepper(tr(fa, "زمان روزانه (دقیقه)", "Minutes per day"), num(minutes), Turquoise, { minutes = (minutes - 5).coerceAtLeast(5) }, { minutes = (minutes + 5).coerceAtMost(240) })
                Spacer(Modifier.height(10.dp))
                Stepper(tr(fa, "روزهای فعال در هفته", "Active days per week"), num(days), Turquoise, { days = (days - 1).coerceAtLeast(1) }, { days = (days + 1).coerceAtMost(7) })

                Spacer(Modifier.height(14.dp))
                Label(tr(fa, "زمان مناسب روز", "Preferred time of day"))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanRequest.TimeOfDay.entries.forEach { option -> Pill(timeLabel(option, fa), selected = timeOfDay == option) { timeOfDay = option } }
                }

                Spacer(Modifier.height(14.dp))
                Label(tr(fa, "شدت برنامه", "Intensity"))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanRequest.Intensity.entries.forEach { option -> Pill(intensityLabel(option, fa), selected = intensity == option) { intensity = option } }
                }

                Spacer(Modifier.height(14.dp))
                Label(tr(fa, "طول برنامه", "Plan length"))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 8, 12, 16, 24).forEach { option -> Pill(tr(fa, "${num(option)} هفته", "$option weeks"), selected = weeks == option) { weeks = option } }
                }

                Spacer(Modifier.height(14.dp))
                Label(tr(fa, "توضیحات شخصی (اختیاری)", "Personal notes (optional)"))
                PlannerField(notes, { notes = it }, tr(fa, "محدودیت‌ها، آسیب‌دیدگی، برنامه کاری، علاقه‌ها…", "Constraints, injuries, work schedule, preferences…"), singleLine = false)

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        note = ""
                        planner.generate(
                            PlanRequest(
                                goal = goal.trim(), level = level, minutesPerDay = minutes, timeOfDay = timeOfDay,
                                daysPerWeek = days, intensity = intensity, weeks = weeks, notes = notes.trim(), persianUi = fa,
                            )
                        )
                    },
                    enabled = goal.isNotBlank() && !working,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr(fa, "ساخت برنامه", "Build my plan"))
                }
            }
        }

        val current = status
        if (current is PlannerViewModel.Status.Working) {
            Spacer(Modifier.height(12.dp))
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Turquoise, strokeWidth = 2.5.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (current.revising) tr(fa, "در حال بازنویسی برنامه…", "Revising your plan…") else tr(fa, "در حال طراحی برنامه…", "Designing your plan…"),
                            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold
                        )
                        Text(tr(fa, "معمولاً کمتر از یک دقیقه طول می‌کشد", "Usually takes less than a minute"), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    TextButton(onClick = { planner.cancel() }) { Text(tr(fa, "توقف", "Stop"), color = Coral) }
                }
            }
        }
        if (current is PlannerViewModel.Status.Failed) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().background(Coral.copy(alpha = 0.10f), RoundedCornerShape(20.dp)).border(1.dp, Coral.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(16.dp)
            ) {
                Text(errorText(fa, current.kind), style = MaterialTheme.typography.bodyMedium, color = Ink)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val needsSettings = current.kind == PlannerViewModel.ErrorKind.NO_KEY || current.kind == PlannerViewModel.ErrorKind.NOT_CONNECTED || current.kind == PlannerViewModel.ErrorKind.AUTH || current.kind == PlannerViewModel.ErrorKind.QUOTA_DAILY
                    if (needsSettings) Pill(tr(fa, "تنظیمات هوش مصنوعی", "AI settings"), icon = Icons.Rounded.Settings, accent = Coral, selected = true) { planner.dismissError(); aiSettingsOpen = true }
                    Pill(tr(fa, "باشه", "OK"), accent = Coral) { planner.dismissError() }
                }
            }
        }

        val shown = plan
        if (shown != null) {
            Spacer(Modifier.height(16.dp))
            PlanPreview(shown, fa, added) { indexes ->
                planner.addHabits(indexes, app) { count ->
                    if (count > 0) note = tr(fa, "${num(count)} عادت به هویت‌هایت اضافه شد", "$count habits added to your identities")
                }
            }
            if (note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(note, style = MaterialTheme.typography.labelLarge, color = Turquoise)
            }
            Spacer(Modifier.height(16.dp))
            SectionCard {
                Text(tr(fa, "می‌خواهی چیزی عوض شود؟", "Want something changed?"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                PlannerField(feedback, { feedback = it }, tr(fa, "مثلاً: صبح‌ها وقت ندارم، تمرین‌ها کوتاه‌تر باشد", "e.g. no time in the mornings, make sessions shorter"), singleLine = false)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { planner.revise(feedback, fa); feedback = "" }, enabled = !working) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        Text(tr(fa, "بازنویسی برنامه", "Revise plan"))
                    }
                    OutlinedButton(onClick = { planner.discardPlan(); note = "" }, enabled = !working) {
                        Icon(Icons.Rounded.Delete, null, tint = Coral, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                        Text(tr(fa, "حذف برنامه", "Discard"), color = Coral)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    if (aiSettingsOpen) AiSettingsDialog(fa, planner) { aiSettingsOpen = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanPreview(plan: HabitPlan, fa: Boolean, added: Set<Int>, onAdd: (List<Int>) -> Unit) {
    fun num(value: Int): String = if (fa) value.toString().toPersianDigits() else value.toString()
    SectionCard {
        Text(plan.title(fa), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (plan.identity(fa).isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("«" + plan.identity(fa) + "»", style = MaterialTheme.typography.bodyLarge, color = Turquoise, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(plan.summary(fa), style = MaterialTheme.typography.bodyMedium, color = Ink)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(tr(fa, "${num(plan.totalWeeks)} هفته", "${plan.totalWeeks} weeks"), icon = Icons.Rounded.CalendarMonth)
            Pill(tr(fa, "${num(plan.habits.size)} عادت", "${plan.habits.size} habits"), icon = Icons.Rounded.Repeat)
        }
        if (plan.safety(fa).isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().background(Amber.copy(alpha = 0.10f), RoundedCornerShape(16.dp)).padding(12.dp)) {
                Icon(Icons.Rounded.HealthAndSafety, null, tint = Amber, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(plan.safety(fa), style = MaterialTheme.typography.bodySmall, color = Ink, modifier = Modifier.weight(1f))
            }
        }
    }

    if (plan.methods.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        SectionCard {
            Label(tr(fa, "روش‌های علمی این برنامه", "Science behind the plan"))
            plan.methods.forEach { method ->
                Spacer(Modifier.height(8.dp))
                Text(method.name(fa), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(method.how(fa), style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }

    if (plan.phases.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        SectionCard {
            Label(tr(fa, "مراحل", "Phases"))
            plan.phases.forEachIndexed { index, phase ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(26.dp).background(Turquoise.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(num(index + 1), style = MaterialTheme.typography.labelLarge, color = Turquoise)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(phase.title(fa), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(tr(fa, "هفته ${num(phase.startWeek)} تا ${num(phase.endWeek)}", "Weeks ${phase.startWeek}-${phase.endWeek}"), style = MaterialTheme.typography.labelMedium, color = Turquoise)
                        Text(phase.focus(fa), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(tr(fa, "عادت‌های پیشنهادی", "Suggested habits"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        val remaining = plan.habits.indices.filter { it !in added }
        if (remaining.isNotEmpty()) Pill(tr(fa, "افزودن همه", "Add all"), icon = Icons.Rounded.Add, selected = true) { onAdd(remaining) }
    }
    plan.habits.forEachIndexed { index, habit ->
        Spacer(Modifier.height(10.dp))
        HabitCard(habit, fa, index in added) { onAdd(listOf(index)) }
    }

    if (plan.milestones.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        SectionCard {
            Label(tr(fa, "نقاط عطف", "Milestones"))
            plan.milestones.forEach { milestone ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Flag, null, tint = Amber, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                    Text(tr(fa, "هفته ${num(milestone.week)}: ", "Week ${milestone.week}: ") + milestone.text(fa), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (plan.obstacles.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        SectionCard {
            Label(tr(fa, "اگر… آنگاه… (برنامه موانع)", "If… then… (obstacle plans)"))
            plan.obstacles.forEach { obstacle ->
                Spacer(Modifier.height(8.dp))
                Text(tr(fa, "اگر ", "If ") + obstacle.whenText(fa), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(tr(fa, "آنگاه ", "Then ") + obstacle.thenText(fa), style = MaterialTheme.typography.bodySmall, color = Turquoise)
            }
        }
    }

    val tips = plan.tips(fa)
    if (tips.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        SectionCard {
            Label(tr(fa, "نکته‌ها", "Tips"))
            tips.forEach { tip ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = Amber, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                    Text(tip, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitCard(habit: PlanHabit, fa: Boolean, isAdded: Boolean, onAdd: () -> Unit) {
    val accent = parseColor(habit.colorHex)
    fun num(value: Int): String = if (fa) value.toString().toPersianDigits() else value.toString()
    Column(Modifier.fillMaxWidth().background(SurfaceNavy, RoundedCornerShape(22.dp)).border(1.dp, if (isAdded) accent else Hairline, RoundedCornerShape(22.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(accent.copy(alpha = 0.18f), CircleShape).border(1.dp, accent.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(glyphFor(habit.iconKey), null, tint = accent, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.title(fa), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(habit.ritual(fa), style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }
        if (habit.cue(fa).isNotBlank()) { Spacer(Modifier.height(10.dp)); Detail(tr(fa, "نشانه", "Cue"), habit.cue(fa), accent) }
        if (habit.why(fa).isNotBlank()) { Spacer(Modifier.height(6.dp)); Detail(tr(fa, "چرا", "Why"), habit.why(fa), accent) }
        if (habit.levelUp(fa).isNotBlank()) { Spacer(Modifier.height(6.dp)); Detail(tr(fa, "قدم بعد", "Level up"), habit.levelUp(fa), accent) }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(durationLabel(habit.durationMinutes * 60, fa), icon = Icons.Rounded.Timer, accent = accent)
            if (habit.timesPerDay > 1) Pill(tr(fa, "${num(habit.timesPerDay)} بار در روز", "${habit.timesPerDay}x a day"), icon = Icons.Rounded.Repeat, accent = accent)
            val times = habit.parsedTimes()
            if (times.isNotEmpty()) Pill(times.joinToString(" · ") { formatReminderTime(it.hour, it.minute, fa) }, icon = Icons.Rounded.Notifications, accent = accent, maxLines = 3)
            Pill(
                if (habit.weekdays.isEmpty()) tr(fa, "هر روز", "Every day") else habit.weekdays.joinToString(" ") { weekdayName(it, fa) },
                icon = Icons.Rounded.CalendarMonth,
                accent = accent,
                maxLines = 2
            )
            Pill(tr(fa, "مرحله ${num(habit.phase)}", "Phase ${habit.phase}"), accent = accent)
        }
        Spacer(Modifier.height(12.dp))
        if (isAdded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, null, tint = accent, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(tr(fa, "اضافه شد", "Added"), color = accent, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Navy), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text(tr(fa, "افزودن به هویت‌ها", "Add to identities"))
            }
        }
    }
}

@Composable
private fun Detail(label: String, text: String, accent: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Text("$label: ", style = MaterialTheme.typography.labelLarge, color = accent, maxLines = 1)
        Text(text, style = MaterialTheme.typography.bodySmall, color = Ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = Muted)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PlannerField(value: String, onChange: (String) -> Unit, placeholder: String, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        placeholder = { Text(placeholder, color = Muted) },
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Turquoise, unfocusedBorderColor = Hairline, cursorColor = Turquoise)
    )
}

private fun goalExamples(fa: Boolean): List<String> = if (fa) listOf(
    "یادگیری زبان انگلیسی",
    "رسیدن به اندام ایده‌آل",
    "کتاب‌خوان شدن",
    "یادگیری برنامه‌نویسی",
    "ذهن آرام‌تر و خواب بهتر",
) else listOf(
    "Learn English",
    "Build my ideal body",
    "Become a reader",
    "Learn to code",
    "A calmer mind and better sleep",
)

private fun levelLabel(level: PlanRequest.Level, fa: Boolean): String = when (level) {
    PlanRequest.Level.BEGINNER -> tr(fa, "مبتدی", "Beginner")
    PlanRequest.Level.ELEMENTARY -> tr(fa, "کمی تجربه", "Some experience")
    PlanRequest.Level.INTERMEDIATE -> tr(fa, "متوسط", "Intermediate")
    PlanRequest.Level.ADVANCED -> tr(fa, "پیشرفته", "Advanced")
}

private fun timeLabel(time: PlanRequest.TimeOfDay, fa: Boolean): String = when (time) {
    PlanRequest.TimeOfDay.MORNING -> tr(fa, "صبح", "Morning")
    PlanRequest.TimeOfDay.MIDDAY -> tr(fa, "ظهر", "Midday")
    PlanRequest.TimeOfDay.EVENING -> tr(fa, "عصر و شب", "Evening")
    PlanRequest.TimeOfDay.FLEXIBLE -> tr(fa, "منعطف", "Flexible")
}

private fun intensityLabel(intensity: PlanRequest.Intensity, fa: Boolean): String = when (intensity) {
    PlanRequest.Intensity.LIGHT -> tr(fa, "سبک", "Light")
    PlanRequest.Intensity.BALANCED -> tr(fa, "متعادل", "Balanced")
    PlanRequest.Intensity.INTENSIVE -> tr(fa, "فشرده", "Intensive")
}
