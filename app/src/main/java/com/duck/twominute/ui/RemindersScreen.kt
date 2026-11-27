package com.duck.twominute.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.duck.twominute.AppState
import com.duck.twominute.AppViewModel
import com.duck.twominute.alarms.ReminderHealth
import com.duck.twominute.reminders.Reminder
import com.duck.twominute.reminders.ReminderSchedule
import com.duck.twominute.reminders.ReminderText
import com.duck.twominute.reminders.RepeatRule
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Wall-clock "now" as a [LocalDateTime]. */
private fun localNow(millis: Long): LocalDateTime =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()

private fun toMillis(at: LocalDateTime): Long = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** The ring the scheduler would arm right now (same floor rule as ReminderAlarmScheduler). */
internal fun nextRing(reminder: Reminder, nowMillis: Long): LocalDateTime? {
    if (!reminder.enabled) return null
    return ReminderSchedule.nextAfter(reminder, localNow(maxOf(nowMillis, reminder.lastHandledAt)))
}

/** A fresh reminder on [date] at the next full hour (tomorrow 09:00 when today is almost over). */
internal fun newReminderDraft(date: LocalDate?, nowMillis: Long = System.currentTimeMillis()): Reminder {
    val now = localNow(nowMillis)
    val today = now.toLocalDate()
    val day = date ?: today
    return if (day == today) {
        if (now.hour >= 23) Reminder(startEpochDay = today.plusDays(1).toEpochDay(), hour = 9, minute = 0)
        else Reminder(startEpochDay = today.toEpochDay(), hour = now.hour + 1, minute = 0)
    } else {
        Reminder(startEpochDay = day.toEpochDay(), hour = 9, minute = 0)
    }
}

@Composable
fun RemindersScreen(state: AppState, viewModel: AppViewModel, prefillEpochDay: Long?, onPrefillConsumed: () -> Unit) {
    val fa = state.isPersian
    var editing by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Reminder?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }
    // "Add a reminder for this day" from the calendar.
    LaunchedEffect(prefillEpochDay) {
        if (prefillEpochDay != null) {
            editTarget = newReminderDraft(LocalDate.ofEpochDay(prefillEpochDay))
            editing = true
            onPrefillConsumed()
        }
    }

    val target = editTarget
    if (editing && target != null) {
        ReminderEditor(
            fa = fa,
            initial = target,
            onCancel = { editing = false; editTarget = null },
            onSave = { draft ->
                viewModel.saveReminder(draft)
                editing = false
                editTarget = null
            }
        )
        return
    }

    val rows = state.reminders.map { it to nextRing(it, now) }
    val active = rows.filter { it.first.enabled && it.second != null }.sortedBy { it.second }
    val paused = rows.filter { !it.first.enabled }
    val finished = rows.filter { it.first.enabled && it.second == null }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        ScreenHeader(tr(fa, "یادآورها", "Reminders")) {
            Pill(tr(fa, "یادآور تازه", "New reminder"), icon = Icons.Rounded.Add, selected = true) {
                editTarget = newReminderDraft(null)
                editing = true
            }
        }
        Spacer(Modifier.height(12.dp))
        ReminderHealthCard(fa, viewModel) { now = System.currentTimeMillis() }
        Spacer(Modifier.height(14.dp))

        if (rows.isEmpty()) {
            SectionCard {
                Text(tr(fa, "هنوز یادآوری نساخته‌ای", "No reminders yet"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    tr(
                        fa,
                        "یک تاریخ شمسی و ساعت انتخاب کن تا همان لحظه اعلان بیاید. می‌توانی یک روز را انتخاب کنی و بگویی مثلاً از ۹ صبح تا ۹ شب هر ۱ ساعت یادآوری کند، یا آن را هر روز، هر هفته، هر ماه یا هر سال تکرار کنی.",
                        "Pick a date and a time and get a notification right then. You can also pick a day and ring, for example, every hour from 9 AM to 9 PM, or repeat it daily, weekly, monthly or yearly."
                    ),
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                Pill(tr(fa, "ساخت اولین یادآور", "Create the first reminder"), icon = Icons.Rounded.Add, selected = true) {
                    editTarget = newReminderDraft(null)
                    editing = true
                }
            }
        }

        if (active.isNotEmpty()) SectionTitle(tr(fa, "فعال", "Active"))
        active.forEach { (reminder, next) ->
            ReminderCard(reminder, next, now, fa, viewModel) { editTarget = reminder; editing = true }
            Spacer(Modifier.height(10.dp))
        }
        if (paused.isNotEmpty()) SectionTitle(tr(fa, "خاموش", "Off"))
        paused.forEach { (reminder, next) ->
            ReminderCard(reminder, next, now, fa, viewModel) { editTarget = reminder; editing = true }
            Spacer(Modifier.height(10.dp))
        }
        if (finished.isNotEmpty()) SectionTitle(tr(fa, "تمام‌شده", "Finished"))
        finished.forEach { (reminder, next) ->
            ReminderCard(reminder, next, now, fa, viewModel) { editTarget = reminder; editing = true }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = Muted, modifier = Modifier.padding(vertical = 6.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderCard(reminder: Reminder, next: LocalDateTime?, nowMillis: Long, fa: Boolean, viewModel: AppViewModel, onEdit: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = when {
        !reminder.enabled -> Muted
        next == null -> Violet
        else -> Amber
    }
    val todayDone = reminder.repeatsWithinDay && reminder.doneEpochDays.contains(LocalDate.now().toEpochDay())
    Column(
        Modifier
            .fillMaxWidth()
            .background(SurfaceNavy, RoundedCornerShape(22.dp))
            .border(1.dp, if (reminder.enabled && next != null) accent.copy(alpha = 0.35f) else Hairline, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(accent.copy(alpha = 0.14f), CircleShape).border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (!reminder.enabled) Icons.Rounded.NotificationsOff else if (reminder.repeatsWithinDay || reminder.repeat != RepeatRule.ONCE) Icons.Rounded.Repeat else Icons.Rounded.Alarm,
                    null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(reminder.title.ifBlank { tr(fa, "یادآور", "Reminder") }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (reminder.note.isNotBlank()) Text(reminder.note, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = reminder.enabled, onCheckedChange = { viewModel.setReminderEnabled(reminder.id, it) })
            Box {
                IconButton({ menuOpen = true }) { Icon(Icons.Rounded.MoreVert, tr(fa, "گزینه‌ها", "Options"), tint = Muted) }
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem({ Text(tr(fa, "ویرایش", "Edit")) }, { menuOpen = false; onEdit() }, leadingIcon = { Icon(Icons.Rounded.Edit, null) })
                    DropdownMenuItem({ Text(tr(fa, "حذف یادآور", "Delete reminder")) }, { menuOpen = false; viewModel.deleteReminder(reminder.id) }, leadingIcon = { Icon(Icons.Rounded.Delete, null) })
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(ReminderText.repeatSummary(reminder, fa), icon = Icons.Rounded.CalendarMonth, accent = accent, maxLines = 2)
            Pill(ReminderText.timeSummary(reminder, fa), icon = if (reminder.repeatsWithinDay) Icons.Rounded.Repeat else Icons.Rounded.Schedule, accent = accent, maxLines = 2)
        }
        Spacer(Modifier.height(10.dp))
        val status = when {
            !reminder.enabled -> tr(fa, "خاموش است؛ هیچ اعلانی نمی‌آید.", "Off: no notifications.")
            next == null -> tr(fa, "تمام شد؛ زمان دیگری برای اعلان نمانده.", "Finished: no rings left.")
            else -> tr(fa, "اعلان بعدی: ", "Next: ") + ReminderText.dateTime(next, fa) + " · " + ReminderText.relative(nowMillis, toMillis(next), fa)
        }
        Text(status, style = MaterialTheme.typography.labelMedium, color = if (reminder.enabled && next != null) accent else Muted)
        if (todayDone) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr(fa, "برای امروز تمام شد؛ بقیه یادآورهای امروز نمی‌آیند.", "Done for today: the rest of today is skipped."), style = MaterialTheme.typography.labelMedium, color = Muted, modifier = Modifier.weight(1f))
                Pill(tr(fa, "برگرداندن", "Undo"), icon = Icons.Rounded.Undo, accent = accent) { viewModel.clearReminderDoneToday(reminder.id) }
            }
        }
    }
}

/**
 * Shows, in plain words, anything on this phone that would stop a reminder and
 * opens the right settings page. Also sends a real test notification.
 */
@Composable
private fun ReminderHealthCard(fa: Boolean, viewModel: AppViewModel, onRefresh: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    var testNote by remember { mutableStateOf("") }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        tick += 1
        onRefresh()
    }
    val health = remember(tick) { ReminderHealth.check(context) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        tick += 1
        if (!granted) ReminderHealth.launch(context, ReminderHealth.notificationSettings(context), ReminderHealth.appDetails(context))
    }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (health.essentialOk) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                null,
                tint = if (health.essentialOk) Turquoise else Coral,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (health.essentialOk) tr(fa, "یادآورها آماده‌اند", "Reminders are ready") else tr(fa, "یادآورها ممکن است نرسند", "Reminders may not arrive"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        if (!health.notificationsEnabled || !health.channelsEnabled) {
            HealthRow(
                icon = Icons.Rounded.NotificationsOff,
                title = tr(fa, "اعلان‌ها خاموش است", "Notifications are off"),
                body = tr(fa, "بدون اجازه اعلان هیچ یادآوری نمایش داده نمی‌شود.", "Without notification access no reminder can be shown."),
                action = tr(fa, "روشن کردن", "Turn on"),
                fa = fa
            ) {
                val needsRuntime = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                if (needsRuntime) {
                    askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    ReminderHealth.launch(context, ReminderHealth.notificationSettings(context), ReminderHealth.appDetails(context))
                }
            }
        }
        if (!health.exactAlarms) {
            HealthRow(
                icon = Icons.Rounded.Alarm,
                title = tr(fa, "زنگ دقیق مجاز نیست", "Exact alarms are not allowed"),
                body = tr(fa, "بدون این اجازه، اندروید ممکن است یادآور را با تأخیر بفرستد.", "Without it Android may deliver reminders late."),
                action = tr(fa, "اجازه دادن", "Allow"),
                fa = fa
            ) {
                ReminderHealth.launch(context, ReminderHealth.exactAlarmSettings(context), ReminderHealth.appDetails(context))
            }
        }
        if (!health.batteryUnrestricted) {
            HealthRow(
                icon = Icons.Rounded.BatteryAlert,
                title = tr(fa, "بهینه‌سازی باتری روشن است", "Battery optimization is on"),
                body = tr(
                    fa,
                    "روی بعضی گوشی‌ها (مثل شیائومی، هواوی، سامسونگ و اوپو) این گزینه یادآورها را متوقف می‌کند. «بدون محدودیت» را انتخاب کن و در صورت وجود، «اجرای خودکار» را هم روشن کن.",
                    "On some phones (Xiaomi, Huawei, Samsung, Oppo…) this stops reminders. Choose “Unrestricted” and, if your phone has it, turn on “Autostart”."
                ),
                action = tr(fa, "بدون محدودیت", "Unrestrict"),
                fa = fa
            ) {
                ReminderHealth.launch(context, ReminderHealth.batteryPrompt(context), ReminderHealth.batteryList(), ReminderHealth.appDetails(context))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr(fa, "برای اطمینان یک اعلان آزمایشی بفرست.", "Send a test notification to be sure."),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Pill(tr(fa, "اعلان آزمایشی", "Test"), icon = Icons.Rounded.NotificationsActive, accent = Turquoise) {
                viewModel.sendTestReminder()
                testNote = tr(
                    fa,
                    "تا ۵ ثانیه دیگر یک اعلان آزمایشی می‌رسد. اگر نرسید، موارد بالا را بررسی کن.",
                    "A test notification arrives in about 5 seconds. If it does not, check the items above."
                )
            }
        }
        if (testNote.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(testNote, style = MaterialTheme.typography.labelMedium, color = Turquoise)
        }
    }
}

@Composable
private fun HealthRow(icon: ImageVector, title: String, body: String, action: String, fa: Boolean, onFix: () -> Unit) {
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Coral, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.width(8.dp))
        Pill(action, accent = Coral, selected = true, onClick = onFix)
    }
}

// ------------------------------------------------------------------ editor

private enum class TimeTarget { START, END }
private enum class DateTarget { START, UNTIL }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderEditor(fa: Boolean, initial: Reminder, onCancel: () -> Unit, onSave: (Reminder) -> Unit) {
    val isNew = initial.id == 0L
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    val accent = Amber
    val nowMillis = remember(draft) { System.currentTimeMillis() }
    val today = LocalDate.now()
    // The preview ignores lastHandledAt: after saving, the schedule starts from "now".
    val preview = remember(draft, nowMillis) { ReminderSchedule.upcoming(draft.copy(enabled = true, lastHandledAt = 0L), localNow(nowMillis), 5) }
    val ringsPerDay = remember(draft) { ReminderSchedule.timesOfDay(draft) }
    val startMinute = draft.safeHour * 60 + draft.safeMinute
    val endMinute = draft.safeEndHour * 60 + draft.safeEndMinute
    val endBeforeStart = draft.repeatsWithinDay && endMinute <= startMinute
    val titleOk = draft.title.isNotBlank()
    val canSave = titleOk && preview.isNotEmpty()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (isNew) tr(fa, "یادآور تازه", "New reminder") else tr(fa, "ویرایش یادآور", "Edit reminder"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = draft.title,
            onValueChange = { draft = draft.copy(title = it.take(120)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(tr(fa, "عنوان یادآور", "Title"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text(tr(fa, "مثال: خوردن دارو", "Example: take my medicine"), color = Muted, maxLines = 1) },
            isError = !titleOk && draft.title.isNotEmpty()
        )
        OutlinedTextField(
            value = draft.note,
            onValueChange = { draft = draft.copy(note = it.take(500)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr(fa, "توضیح (اختیاری)", "Note (optional)"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text(tr(fa, "مثال: بعد از غذا با یک لیوان آب", "Example: after a meal, with water"), color = Muted, maxLines = 2) }
        )

        // ----- date and time
        SectionCard {
            Text(tr(fa, "تاریخ و ساعت", "Date and time"), style = MaterialTheme.typography.labelLarge, color = Muted)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (draft.repeat == RepeatRule.ONCE) tr(fa, "تاریخ", "Date") else tr(fa, "شروع از", "Starts on"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Pill(ReminderText.date(draft.startDate, fa), icon = Icons.Rounded.CalendarMonth, accent = accent, selected = true) { dateTarget = DateTarget.START }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(tr(fa, "امروز", "Today"), accent = accent, selected = draft.startDate == today) { draft = draft.copy(startEpochDay = today.toEpochDay()) }
                Pill(tr(fa, "فردا", "Tomorrow"), accent = accent, selected = draft.startDate == today.plusDays(1)) { draft = draft.copy(startEpochDay = today.plusDays(1).toEpochDay()) }
                Pill(tr(fa, "پس‌فردا", "In 2 days"), accent = accent, selected = draft.startDate == today.plusDays(2)) { draft = draft.copy(startEpochDay = today.plusDays(2).toEpochDay()) }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (draft.repeatsWithinDay) tr(fa, "ساعت شروع", "Start time") else tr(fa, "ساعت", "Time"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Pill(ReminderText.clock(draft.safeHour, draft.safeMinute, fa), icon = Icons.Rounded.Schedule, accent = accent, selected = true) { timeTarget = TimeTarget.START }
            }
        }

        // ----- repeat within the day
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr(fa, "تکرار در طول روز", "Repeat during the day"), style = MaterialTheme.typography.bodyLarge)
                    Text(tr(fa, "مثلاً هر ۱ ساعت از ۹ صبح تا ۹ شب", "For example every hour from 9 AM to 9 PM"), style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Switch(
                    checked = draft.repeatsWithinDay,
                    onCheckedChange = { on ->
                        draft = if (on) {
                            val start = draft.safeHour * 60 + draft.safeMinute
                            val end = maxOf(start + 60, 21 * 60).coerceAtMost(23 * 60 + 59)
                            draft.copy(intervalMinutes = 60, endHour = end / 60, endMinute = end % 60)
                        } else {
                            draft.copy(intervalMinutes = 0)
                        }
                    }
                )
            }
            if (draft.repeatsWithinDay) {
                Spacer(Modifier.height(12.dp))
                Text(tr(fa, "هر چند وقت یک‌بار", "How often"), style = MaterialTheme.typography.labelMedium, color = Muted)
                Spacer(Modifier.height(8.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderSchedule.INTERVAL_PRESETS.forEach { minutes ->
                        Pill(ReminderText.duration(minutes, fa), accent = accent, selected = draft.intervalMinutes == minutes) { draft = draft.copy(intervalMinutes = minutes) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Stepper(
                    label = tr(fa, "دلخواه", "Custom"),
                    value = ReminderText.digits(draft.intervalMinutes.toString(), fa),
                    accent = accent,
                    onMinus = { draft = draft.copy(intervalMinutes = (draft.intervalMinutes - 5).coerceIn(ReminderSchedule.MIN_INTERVAL, ReminderSchedule.MAX_INTERVAL)) },
                    onPlus = { draft = draft.copy(intervalMinutes = (draft.intervalMinutes + 5).coerceIn(ReminderSchedule.MIN_INTERVAL, ReminderSchedule.MAX_INTERVAL)) }
                )
                Text(tr(fa, "عدد بالا به دقیقه است", "The number above is in minutes"), style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(fa, "تا ساعت", "Until"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Pill(ReminderText.clock(draft.safeEndHour, draft.safeEndMinute, fa), icon = Icons.Rounded.Schedule, accent = accent, selected = true) { timeTarget = TimeTarget.END }
                }
                Spacer(Modifier.height(8.dp))
                if (endBeforeStart) {
                    Text(tr(fa, "ساعت پایان باید بعد از ساعت شروع باشد؛ فعلاً فقط یک اعلان در روز می‌آید.", "The end must be after the start; for now it rings once a day."), style = MaterialTheme.typography.labelMedium, color = Coral)
                } else {
                    val shown = ringsPerDay.take(8).joinToString(if (fa) "، " else ", ") { ReminderText.clock24(it / 60, it % 60, fa) }
                    val more = if (ringsPerDay.size > 8) " …" else ""
                    Text(
                        tr(fa, "${ReminderText.digits(ringsPerDay.size.toString(), true)} اعلان در هر روز: ", "${ringsPerDay.size} notifications a day: ") + shown + more,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }
            }
        }

        // ----- repeat across days
        SectionCard {
            Text(tr(fa, "تکرار روزها", "Repeat on days"), style = MaterialTheme.typography.labelLarge, color = Muted)
            Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    RepeatRule.ONCE to tr(fa, "فقط همان روز", "Only that day"),
                    RepeatRule.DAILY to tr(fa, "هر روز", "Every day"),
                    RepeatRule.WEEKLY to tr(fa, "روزهای هفته", "Weekdays"),
                    RepeatRule.MONTHLY to tr(fa, "هر ماه (شمسی)", "Monthly (Jalali)"),
                    RepeatRule.YEARLY to tr(fa, "هر سال (شمسی)", "Yearly (Jalali)")
                ).forEach { (rule, label) ->
                    Pill(label, accent = accent, selected = draft.repeat == rule) {
                        draft = draft.copy(repeat = rule, weekdays = if (rule == RepeatRule.WEEKLY) draft.effectiveWeekdays() else draft.weekdays)
                    }
                }
            }
            if (draft.repeat == RepeatRule.WEEKLY) {
                Spacer(Modifier.height(10.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderText.WEEK_ORDER.forEach { iso ->
                        val current = draft.effectiveWeekdays()
                        val chosen = current.contains(iso)
                        Pill(ReminderText.weekday(iso, fa), accent = accent, selected = chosen) {
                            val updated = if (chosen) current - iso else current + iso
                            // At least one weekday stays selected.
                            if (updated.isNotEmpty()) draft = draft.copy(weekdays = updated.sorted())
                        }
                    }
                }
            }
            if (draft.repeat != RepeatRule.ONCE) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(fa, "پایان تکرار", "Ends"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    val until = draft.untilDate
                    Pill(
                        if (until == null) tr(fa, "بدون پایان", "Never") else ReminderText.date(until, fa),
                        icon = Icons.Rounded.CalendarMonth,
                        accent = accent,
                        selected = until != null
                    ) { dateTarget = DateTarget.UNTIL }
                }
                if (draft.untilEpochDay != null) {
                    Spacer(Modifier.height(6.dp))
                    Pill(tr(fa, "حذف تاریخ پایان", "Remove end date"), accent = Coral) { draft = draft.copy(untilEpochDay = null) }
                }
            }
        }

        // ----- live preview of the real schedule
        SectionCard {
            Text(tr(fa, "اعلان‌های بعدی", "Next notifications"), style = MaterialTheme.typography.labelLarge, color = Muted)
            Spacer(Modifier.height(8.dp))
            if (preview.isEmpty()) {
                Text(
                    if (draft.repeat == RepeatRule.ONCE) tr(fa, "این زمان گذشته است؛ تاریخ یا ساعت دیگری انتخاب کن.", "This time has passed; pick another date or time.")
                    else tr(fa, "با این تنظیمات هیچ اعلانی در آینده نمی‌آید.", "With these settings nothing will ring in the future."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Coral
                )
            } else {
                preview.forEach { at ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Notifications, null, tint = accent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(ReminderText.dateTime(at, fa), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(ReminderText.relative(nowMillis, toMillis(at), fa), style = MaterialTheme.typography.labelSmall, color = Muted)
                    }
                }
            }
        }

        if (!titleOk) Text(tr(fa, "برای ذخیره، یک عنوان بنویس.", "Write a title to save."), style = MaterialTheme.typography.labelMedium, color = Muted)
        Button(onClick = { onSave(draft) }, enabled = canSave, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(tr(fa, "ذخیره یادآور", "Save reminder")) }
        TextButton(onClick = onCancel, Modifier.fillMaxWidth()) { Text(tr(fa, "انصراف", "Cancel"), color = Muted) }
        Spacer(Modifier.height(20.dp))
    }

    when (timeTarget) {
        TimeTarget.START -> ReminderTimeDialog(
            fa = fa,
            initialHour = draft.safeHour,
            initialMinute = draft.safeMinute,
            accent = accent,
            title = if (draft.repeatsWithinDay) tr(fa, "ساعت شروع", "Start time") else tr(fa, "ساعت یادآور", "Reminder time"),
            onDismiss = { timeTarget = null },
            onConfirm = { h, m -> draft = draft.copy(hour = h, minute = m); timeTarget = null }
        )
        TimeTarget.END -> ReminderTimeDialog(
            fa = fa,
            initialHour = draft.safeEndHour,
            initialMinute = draft.safeEndMinute,
            accent = accent,
            title = tr(fa, "پایان تکرار در روز", "Repeat until"),
            onDismiss = { timeTarget = null },
            onConfirm = { h, m -> draft = draft.copy(endHour = h, endMinute = m); timeTarget = null }
        )
        null -> Unit
    }
    when (dateTarget) {
        DateTarget.START -> JalaliDatePickerDialog(
            fa = fa,
            initial = draft.startDate,
            accent = accent,
            title = tr(fa, "تاریخ یادآور", "Reminder date"),
            // A new reminder cannot start in the past; an old repeating one may keep its first day.
            minDate = if (draft.startDate.isBefore(today)) draft.startDate else today,
            onDismiss = { dateTarget = null },
            onConfirm = { picked ->
                val until = draft.untilEpochDay?.takeIf { it >= picked.toEpochDay() }
                draft = draft.copy(startEpochDay = picked.toEpochDay(), untilEpochDay = until)
                dateTarget = null
            }
        )
        DateTarget.UNTIL -> JalaliDatePickerDialog(
            fa = fa,
            initial = draft.untilDate ?: draft.startDate.plusMonths(1),
            accent = accent,
            title = tr(fa, "آخرین روز تکرار", "Last day"),
            minDate = draft.startDate,
            onDismiss = { dateTarget = null },
            onConfirm = { picked -> draft = draft.copy(untilEpochDay = picked.toEpochDay()); dateTarget = null }
        )
        null -> Unit
    }
}
