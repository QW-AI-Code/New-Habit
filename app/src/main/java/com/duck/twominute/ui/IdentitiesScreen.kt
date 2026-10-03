package com.duck.twominute.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duck.twominute.AppState
import com.duck.twominute.AppViewModel
import com.duck.twominute.Identity
import com.duck.twominute.ReminderPlanner
import com.duck.twominute.ReminderTime
import com.duck.twominute.ai.planner.PlannerViewModel
import androidx.compose.material.icons.rounded.AutoAwesome

@Composable
fun IdentitiesScreen(state: AppState, viewModel: AppViewModel, planner: PlannerViewModel) {
    val fa = state.isPersian
    var editing by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Identity?>(null) }
    var planning by rememberSaveable { mutableStateOf(false) }
    if (planning) {
        PlannerScreen(state, viewModel, planner) { planning = false }
        return
    }
    if (editing) {
        IdentityEditor(fa, editTarget, state.defaultDurationSeconds, { editing = false; editTarget = null }) { identity -> viewModel.saveIdentity(identity); editing = false; editTarget = null }
        return
    }
    val visible = if (state.showArchived) state.identities else state.active
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        // Title and actions share a line only when they fit; otherwise the actions wrap below.
        ScreenHeader(tr(fa, "هویت‌های من", "My identities")) {
            Pill(tr(fa, "برنامه هوشمند", "AI plan"), icon = Icons.Rounded.AutoAwesome, accent = Violet) { planning = true }
            Pill(tr(fa, "افزودن", "Add"), icon = Icons.Rounded.Add, selected = true) { editTarget = null; editing = true }
        }
        Spacer(Modifier.height(8.dp))
        if (state.archived.isNotEmpty()) {
            Pill(tr(fa, "نمایش آرشیو", "Show archived"), icon = Icons.Rounded.Archive, selected = state.showArchived) { viewModel.setShowArchived(!state.showArchived) }
            Spacer(Modifier.height(10.dp))
        }
        if (visible.isEmpty()) {
            SectionCard {
                Text(tr(fa, "برنامه‌ای کامل با هوش مصنوعی بساز", "Let AI build a full plan"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(tr(fa, "هدفت را بنویس؛ مربی هوشمند مراحل، عادت‌ها، یادآورها و نقاط عطف را بر پایه روش‌های علمی می‌چیند.", "Write your goal: the AI coach lays out phases, habits, reminders and milestones using proven methods."), color = Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Pill(tr(fa, "شروع با مربی هوشمند", "Start with the AI coach"), icon = Icons.Rounded.AutoAwesome, accent = Violet, selected = true) { planning = true }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (visible.isEmpty()) SectionCard { Text(tr(fa, "مثال: هویت «من یک کتاب‌خوان هستم» با آیین «خواندن یک صفحه». مدت آیین را هم خودت انتخاب می‌کنی.", "Example: the identity «I am a reader» with the ritual «read one page». You pick the length."), color = Muted, style = MaterialTheme.typography.bodyMedium) }
        visible.forEach { identity ->
            IdentityCard(identity, fa, viewModel) { editTarget = identity; editing = true }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityCard(identity: Identity, fa: Boolean, viewModel: AppViewModel, onEdit: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = parseColor(identity.colorHex)
    Column(Modifier.fillMaxWidth().background(SurfaceNavy, RoundedCornerShape(22.dp)).border(1.dp, Hairline, RoundedCornerShape(22.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IdentityAvatar(identity, 44.dp); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(identity.title(fa), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(2.dp)); Text(identity.ritual(fa), style = MaterialTheme.typography.bodyMedium, color = Muted) }
            Box {
                IconButton({ menuOpen = true }) { Icon(Icons.Rounded.MoreVert, null, tint = Muted) }
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem({ Text(tr(fa, "ویرایش", "Edit")) }, { menuOpen = false; onEdit() }, leadingIcon = { Icon(Icons.Rounded.Edit, null) })
                    DropdownMenuItem({ Text(tr(fa, "بردن به بالا", "Move up")) }, { menuOpen = false; viewModel.moveIdentity(identity.id, -1) }, leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) })
                    DropdownMenuItem({ Text(tr(fa, "بردن به پایین", "Move down")) }, { menuOpen = false; viewModel.moveIdentity(identity.id, 1) }, leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) })
                    if (identity.successes.isNotEmpty()) DropdownMenuItem({ Text(tr(fa, "برگرداندن آخرین ثبت", "Undo last log")) }, { menuOpen = false; viewModel.undoLast(identity.id) }, leadingIcon = { Icon(Icons.Rounded.Undo, null) })
                    DropdownMenuItem({ Text(if (identity.archived) tr(fa, "بازگرداندن از آرشیو", "Unarchive") else tr(fa, "آرشیو کردن", "Archive")) }, { menuOpen = false; viewModel.toggleArchive(identity.id) }, leadingIcon = { Icon(if (identity.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, null) })
                    DropdownMenuItem({ Text(tr(fa, "حذف هویت", "Delete identity")) }, { menuOpen = false; viewModel.deleteIdentity(identity.id) }, leadingIcon = { Icon(Icons.Rounded.Delete, null) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(durationLabel(identity.safeDuration, fa), icon = Icons.Rounded.Timer, accent = accent)
            Pill(reminderSummary(identity, fa), icon = if (identity.hasReminder()) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff, accent = accent, maxLines = 2)
            if (identity.safeTimesPerDay > 1) Pill(tr(fa, "${identity.safeTimesPerDay} بار در روز", "${identity.safeTimesPerDay}x a day"), icon = Icons.Rounded.Check, accent = accent)
            if (identity.currentStreak() > 0) Pill(tr(fa, "${identity.currentStreak()} روز پیوسته", "${identity.currentStreak()} day streak"), icon = Icons.Rounded.LocalFireDepartment, accent = Amber)
        }
        Spacer(Modifier.height(12.dp)); ThinBar(identity.consistency(30), accent, Modifier.fillMaxWidth()); Spacer(Modifier.height(6.dp))
        Text(tr(fa, "۳۰ روز گذشته: ${(identity.consistency(30) * 100).toInt()}٪ · مجموع ${identity.successes.size} بار", "Last 30 days: ${(identity.consistency(30) * 100).toInt()}% · ${identity.successes.size} total"), style = MaterialTheme.typography.labelMedium, color = Muted)
    }
}

/** Mirrors the draft while it is being typed, so nothing is a guess. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityLivePreview(draft: Identity, fa: Boolean) {
    val accent = parseColor(draft.colorHex)
    val titleTyped = draft.title(fa)
    val ritualTyped = draft.ritual(fa)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(RaisedNavy, SurfaceNavy)), RoundedCornerShape(22.dp))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Text(tr(fa, "پیش‌نمایش زنده", "Live preview"), style = MaterialTheme.typography.labelMedium, color = accent)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IdentityAvatar(draft, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = titleTyped.ifBlank { tr(fa, "من یک کتاب‌خوان هستم", "I am a reader") },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (titleTyped.isBlank()) Muted else Ink
                )
                Text(
                    text = ritualTyped.ifBlank { tr(fa, "خواندن یک صفحه", "Read one page") },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
        }
        if (draft.why(fa).isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(draft.why(fa), style = MaterialTheme.typography.bodySmall, color = accent.copy(alpha = 0.9f))
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(durationLabel(draft.safeDuration, fa), icon = Icons.Rounded.Timer, accent = accent)
            Pill(
                reminderSummary(draft, fa),
                icon = if (draft.hasReminder()) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                accent = accent,
                selected = draft.hasReminder(),
                maxLines = 2
            )
            if (draft.safeTimesPerDay > 1) Pill(tr(fa, "${draft.safeTimesPerDay} بار در روز", "${draft.safeTimesPerDay}x a day"), icon = Icons.Rounded.Check, accent = accent)
        }
    }
}

/** Text field with a worked example in the placeholder and a live echo underneath. */
@Composable
private fun ExampleField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    example: String,
    fa: Boolean,
    accent: Color,
    singleLine: Boolean = true
) {
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            // Long labels/examples end in "…" instead of sliding under the field border.
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            placeholder = { Text(example, color = Muted, maxLines = if (singleLine) 1 else 3, overflow = TextOverflow.Ellipsis) }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (value.isBlank()) tr(fa, "مثال: ", "Example: ") + example else tr(fa, "همین حالا: ", "Right now: ") + value,
            style = MaterialTheme.typography.labelSmall,
            color = if (value.isBlank()) Muted else accent.copy(alpha = 0.95f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityEditor(fa: Boolean, initial: Identity?, defaultDuration: Int, onCancel: () -> Unit, onSave: (Identity) -> Unit) {
    val context = LocalContext.current
    var draft by remember(initial?.id) { mutableStateOf(initial ?: Identity(durationSeconds = defaultDuration)) }
    var durationOpen by remember { mutableStateOf(false) }
    /** Index of the reminder slot being edited; 0 also switches reminders on. */
    var editingSlot by remember { mutableStateOf<Int?>(null) }
    var reminderNote by remember { mutableStateOf("") }
    val accent = parseColor(draft.colorHex)
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) { runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; draft = draft.copy(soundUri = uri.toString()) } }
    val pickRingtone = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val picked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java) else @Suppress("DEPRECATION") data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            draft = draft.copy(soundUri = picked?.toString())
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (initial == null) tr(fa, "هویت تازه", "New identity") else tr(fa, "ویرایش هویت", "Edit identity"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        IdentityLivePreview(draft, fa)
        if (initial == null) {
            Text(tr(fa, "از نمونه‌ها شروع کن", "Start from a template"), style = MaterialTheme.typography.labelLarge, color = Muted)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { identityTemplates.forEach { template -> Pill(tr(fa, template.titleFa, template.titleEn), icon = glyphFor(template.iconKey), accent = parseColor(template.colorHex)) { draft = draft.copy(titleFa = template.titleFa, titleEn = template.titleEn, ritualFa = template.ritualFa, ritualEn = template.ritualEn, iconKey = template.iconKey, colorHex = template.colorHex, durationSeconds = template.seconds) } } }
        }
        ExampleField(draft.titleFa, { draft = draft.copy(titleFa = it) }, "هویت (فارسی)", "من یک کتاب‌خوان هستم", fa, accent)
        ExampleField(draft.ritualFa, { draft = draft.copy(ritualFa = it) }, "آیین (فارسی)", "خواندن یک صفحه کتاب", fa, accent)
        ExampleField(draft.whyFa, { draft = draft.copy(whyFa = it) }, "چرا این هویت برایم مهم است؟ (اختیاری)", "چون می‌خواهم آرام‌تر فکر کنم", fa, accent, singleLine = false)
        ExampleField(draft.titleEn, { draft = draft.copy(titleEn = it) }, "Identity (English, optional)", "I am a reader", fa, accent)
        ExampleField(draft.ritualEn, { draft = draft.copy(ritualEn = it) }, "Habit (English, optional)", "Read one page", fa, accent)
        ExampleField(draft.whyEn, { draft = draft.copy(whyEn = it) }, "Why it matters (English, optional)", "Because I want a calmer mind", fa, accent, singleLine = false)
        SectionCard {
            Text(tr(fa, "مدت آیین", "Ritual length"), style = MaterialTheme.typography.labelLarge, color = Muted); Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Identity.DURATION_PRESETS.forEach { preset -> Pill(durationLabel(preset, fa), accent = accent, selected = draft.safeDuration == preset) { draft = draft.copy(durationSeconds = preset) } }; Pill(tr(fa, "دلخواه", "Custom"), icon = Icons.Rounded.Timer, accent = accent, selected = draft.safeDuration !in Identity.DURATION_PRESETS) { durationOpen = true } }
            Spacer(Modifier.height(8.dp))
            Text(tr(fa, "انتخاب فعلی: ", "Currently: ") + formatClock(draft.safeDuration) + " · " + durationLabel(draft.safeDuration, fa), style = MaterialTheme.typography.labelMedium, color = accent)
            Spacer(Modifier.height(14.dp)); Stepper(tr(fa, "چند بار در روز", "Times per day"), draft.safeTimesPerDay.toString(), accent, { draft = draft.withTimesPerDay(draft.safeTimesPerDay - 1); reminderNote = "" }, { draft = draft.withTimesPerDay(draft.safeTimesPerDay + 1); reminderNote = "" })
            if (draft.safeTimesPerDay > 1) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (draft.hasReminder()) tr(fa, "${draft.safeTimesPerDay} یادآور جدا برای هر تکرار تنظیم شده است", "${draft.safeTimesPerDay} separate reminders, one for each repetition")
                    else tr(fa, "با روشن کردن یادآور، برای هر تکرار یک یادآور جدا ساخته می‌شود", "Turn on reminders to get one reminder per repetition"),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
        }
        SectionCard {
            Text(tr(fa, "آیکون", "Icon"), style = MaterialTheme.typography.labelLarge, color = Muted); Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { identityGlyphs.forEach { glyph -> val chosen = draft.iconKey == glyph.key; Box(Modifier.size(42.dp).background(if (chosen) accent else RaisedNavy.copy(alpha = 0.7f), CircleShape).border(1.dp, if (chosen) accent else Hairline, CircleShape).clickable { draft = draft.copy(iconKey = glyph.key) }, contentAlignment = Alignment.Center) { Icon(glyph.vector, glyph.key, tint = if (chosen) Navy else Ink, modifier = Modifier.size(20.dp)) } } }
            Spacer(Modifier.height(16.dp)); Text(tr(fa, "رنگ", "Accent"), style = MaterialTheme.typography.labelLarge, color = Muted); Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { accentPalette.forEach { hex -> val color = parseColor(hex); val chosen = draft.colorHex == hex; Box(Modifier.size(36.dp).background(color, CircleShape).border(if (chosen) 3.dp else 1.dp, if (chosen) Ink else Hairline, CircleShape).clickable { draft = draft.copy(colorHex = hex) }) } }
        }
        SectionCard {
            Text(
                tr(fa, "یادآورها", "Reminders") + if (draft.hasReminder()) " · " + draft.reminderSlots().size.toString() else "",
                style = MaterialTheme.typography.labelLarge,
                color = Muted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                tr(fa, "تعداد یادآورها همیشه برابر «چند بار در روز» است.", "The number of reminders always equals “Times per day”."),
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Spacer(Modifier.height(10.dp))
            if (!draft.hasReminder()) {
                Pill(tr(fa, "تنظیم یادآور", "Set reminder"), icon = Icons.Rounded.Notifications, accent = accent) { editingSlot = 0 }
            } else {
                val slots = draft.reminderSlots()
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEachIndexed { index, time ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tr(fa, "یادآور ${index + 1}", "Reminder ${index + 1}"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink,
                                modifier = Modifier.weight(1f)
                            )
                            Pill(formatReminderTime(time.hour, time.minute, fa), icon = Icons.Rounded.Edit, accent = accent, selected = true) { editingSlot = index }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (reminderNote.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(reminderNote, style = MaterialTheme.typography.labelMedium, color = Coral)
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (draft.hasReminder() && draft.safeTimesPerDay > 1) Pill(tr(fa, "پخش یکنواخت در روز", "Spread evenly"), icon = Icons.Rounded.Timer, accent = accent) {
                    val current = draft.reminderSlots()
                    val first = current.first()
                    val lastMinute = maxOf(current.last().minuteOfDay, 21 * 60)
                    draft = draft.withReminders(ReminderPlanner.spread(first, ReminderTime.ofMinuteOfDay(lastMinute), draft.safeTimesPerDay))
                    reminderNote = ""
                }
                if (draft.hasReminder()) Pill(tr(fa, "حذف یادآورها", "Clear"), icon = Icons.Rounded.NotificationsOff, accent = Coral) { draft = draft.withReminders(emptyList()); reminderNote = "" }
                Pill(tr(fa, "زنگ سیستم", "Ringtone"), icon = Icons.Rounded.MusicNote, accent = accent) { val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply { putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL); putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, tr(fa, "انتخاب زنگ", "Pick a tone")); putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false) }; runCatching { pickRingtone.launch(intent) } }
                Pill(tr(fa, "آهنگ دلخواه", "My audio"), icon = Icons.Rounded.LibraryMusic, accent = accent, selected = draft.soundUri != null) { runCatching { pickAudio.launch(arrayOf("audio/*")) } }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (draft.hasReminder()) tr(fa, "همین حالا: ", "Right now: ") + reminderSummary(draft, fa, full = true) else tr(fa, "مثال: ۰۷:۳۰ صبح", "Example: 07:30 AM"),
                style = MaterialTheme.typography.labelMedium,
                color = if (draft.hasReminder()) accent else Muted
            )
            Spacer(Modifier.height(14.dp)); Text(tr(fa, "روزهای هفته (خالی = هر روز)", "Weekdays (empty = every day)"), style = MaterialTheme.typography.bodySmall, color = Muted); Spacer(Modifier.height(8.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { listOf(6, 7, 1, 2, 3, 4, 5).forEach { iso -> val chosen = draft.reminderDays.contains(iso); Pill(weekdayName(iso, fa), accent = accent, selected = chosen) { draft = draft.copy(reminderDays = if (chosen) draft.reminderDays - iso else draft.reminderDays + iso) } } }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (draft.reminderDays.isEmpty()) tr(fa, "انتخاب فعلی: هر روز", "Currently: every day") else tr(fa, "انتخاب فعلی: ", "Currently: ") + draft.reminderDays.sorted().joinToString(" · ") { weekdayName(it, fa) },
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
        }
        Button(onClick = { onSave(draft.copy(titleFa = draft.titleFa.trim(), titleEn = draft.titleEn.trim(), ritualFa = draft.ritualFa.trim(), ritualEn = draft.ritualEn.trim(), whyFa = draft.whyFa.trim(), whyEn = draft.whyEn.trim())) }, enabled = draft.titleFa.isNotBlank() || draft.titleEn.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(tr(fa, "ذخیره", "Save")) }
        TextButton(onClick = onCancel, Modifier.fillMaxWidth()) { Text(tr(fa, "انصراف", "Cancel"), color = Muted) }
        Spacer(Modifier.height(20.dp))
    }
    if (durationOpen) DurationDialog(fa, draft.safeDuration, accent, { durationOpen = false }) { seconds -> draft = draft.copy(durationSeconds = seconds); durationOpen = false }
    val slotIndex = editingSlot
    if (slotIndex != null) {
        val slots = draft.reminderSlots()
        val initialTime = slots.getOrNull(slotIndex) ?: ReminderTime(9, 0)
        ReminderTimeDialog(
            fa = fa,
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            accent = accent,
            title = if (draft.safeTimesPerDay > 1) tr(fa, "زمان یادآور ${slotIndex + 1}", "Reminder ${slotIndex + 1} time") else null,
            // The draft only changes on confirm: Cancel really cancels (v1.0.0 kept the live value).
            onDismiss = { editingSlot = null },
            onConfirm = { hour, minute ->
                val picked = ReminderTime(hour, minute)
                val clash = slots.withIndex().any { it.index != slotIndex && it.value.minuteOfDay == picked.minuteOfDay }
                if (clash) {
                    reminderNote = tr(fa, "این ساعت قبلاً برای یادآور دیگری انتخاب شده؛ ساعت دیگری انتخاب کن.", "That time is already used by another reminder, pick a different one.")
                } else {
                    val updated = if (slots.isEmpty()) listOf(picked) else slots.toMutableList().also { it[slotIndex] = picked }
                    draft = draft.withReminders(updated)
                    reminderNote = ""
                }
                editingSlot = null
            }
        )
    }
}

/** "07:00 AM · 12:00 PM · 06:00 PM" or "07:00 AM +4" when there are many. */
fun reminderSummary(identity: Identity, fa: Boolean, full: Boolean = false): String {
    if (!identity.hasReminder()) return tr(fa, "تنظیم یادآور", "Set reminder")
    val slots = identity.reminderSlots()
    val labels = slots.map { formatReminderTime(it.hour, it.minute, fa) }
    return if (full || labels.size <= 3) labels.joinToString(" · ") else labels.first() + " +" + (labels.size - 1).toString()
}
