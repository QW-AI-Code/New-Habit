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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duck.twominute.AppState
import com.duck.twominute.AppViewModel
import com.duck.twominute.Identity

@Composable
fun IdentitiesScreen(state: AppState, viewModel: AppViewModel) {
    val fa = state.isPersian
    var editing by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Identity?>(null) }
    if (editing) {
        IdentityEditor(fa, editTarget, state.defaultDurationSeconds, { editing = false; editTarget = null }) { identity -> viewModel.saveIdentity(identity); editing = false; editTarget = null }
        return
    }
    val visible = if (state.showArchived) state.identities else state.active
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tr(fa, "هویت‌های من", "My identities"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Pill(tr(fa, "افزودن", "Add"), icon = Icons.Rounded.Add, selected = true) { editTarget = null; editing = true }
        }
        Spacer(Modifier.height(8.dp))
        if (state.archived.isNotEmpty()) {
            Pill(tr(fa, "نمایش آرشیو", "Show archived"), icon = Icons.Rounded.Archive, selected = state.showArchived) { viewModel.setShowArchived(!state.showArchived) }
            Spacer(Modifier.height(10.dp))
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
            Pill(formatReminderTime(identity.reminderHour, identity.reminderMinute, fa), icon = if (identity.hasReminder()) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff, accent = accent)
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
                formatReminderTime(draft.reminderHour, draft.reminderMinute, fa),
                icon = if (draft.hasReminder()) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                accent = accent,
                selected = draft.hasReminder()
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
            label = { Text(label) },
            placeholder = { Text(example, color = Muted) }
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
    var reminderOpen by remember { mutableStateOf(false) }
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
        ExampleField(draft.ritualEn, { draft = draft.copy(ritualEn = it) }, "Ritual (English, optional)", "Read one page", fa, accent)
        SectionCard {
            Text(tr(fa, "مدت آیین", "Ritual length"), style = MaterialTheme.typography.labelLarge, color = Muted); Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Identity.DURATION_PRESETS.forEach { preset -> Pill(durationLabel(preset, fa), accent = accent, selected = draft.safeDuration == preset) { draft = draft.copy(durationSeconds = preset) } }; Pill(tr(fa, "دلخواه", "Custom"), icon = Icons.Rounded.Timer, accent = accent, selected = draft.safeDuration !in Identity.DURATION_PRESETS) { durationOpen = true } }
            Spacer(Modifier.height(8.dp))
            Text(tr(fa, "انتخاب فعلی: ", "Currently: ") + formatClock(draft.safeDuration) + " · " + durationLabel(draft.safeDuration, fa), style = MaterialTheme.typography.labelMedium, color = accent)
            Spacer(Modifier.height(14.dp)); Stepper(tr(fa, "چند بار در روز", "Times per day"), draft.safeTimesPerDay.toString(), accent, { draft = draft.copy(timesPerDay = (draft.safeTimesPerDay - 1).coerceAtLeast(1)) }, { draft = draft.copy(timesPerDay = (draft.safeTimesPerDay + 1).coerceAtMost(20)) })
        }
        SectionCard {
            Text(tr(fa, "آیکون", "Icon"), style = MaterialTheme.typography.labelLarge, color = Muted); Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { identityGlyphs.forEach { glyph -> val chosen = draft.iconKey == glyph.key; Box(Modifier.size(42.dp).background(if (chosen) accent else RaisedNavy.copy(alpha = 0.7f), CircleShape).border(1.dp, if (chosen) accent else Hairline, CircleShape).clickable { draft = draft.copy(iconKey = glyph.key) }, contentAlignment = Alignment.Center) { Icon(glyph.vector, glyph.key, tint = if (chosen) Navy else Ink, modifier = Modifier.size(20.dp)) } } }
            Spacer(Modifier.height(16.dp)); Text(tr(fa, "رنگ", "Accent"), style = MaterialTheme.typography.labelLarge, color = Muted); Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { accentPalette.forEach { hex -> val color = parseColor(hex); val chosen = draft.colorHex == hex; Box(Modifier.size(36.dp).background(color, CircleShape).border(if (chosen) 3.dp else 1.dp, if (chosen) Ink else Hairline, CircleShape).clickable { draft = draft.copy(colorHex = hex) }) } }
        }
        SectionCard {
            Text(tr(fa, "یادآور", "Reminder"), style = MaterialTheme.typography.labelLarge, color = Muted); Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(formatReminderTime(draft.reminderHour, draft.reminderMinute, fa), icon = Icons.Rounded.Notifications, accent = accent, selected = draft.hasReminder()) { reminderOpen = true }
                if (draft.hasReminder()) Pill(tr(fa, "حذف یادآور", "Clear"), icon = Icons.Rounded.NotificationsOff, accent = Coral) { draft = draft.copy(reminderHour = null, reminderMinute = null) }
                Pill(tr(fa, "زنگ سیستم", "Ringtone"), icon = Icons.Rounded.MusicNote, accent = accent) { val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply { putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL); putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, tr(fa, "انتخاب زنگ", "Pick a tone")); putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false) }; runCatching { pickRingtone.launch(intent) } }
                Pill(tr(fa, "آهنگ دلخواه", "My audio"), icon = Icons.Rounded.LibraryMusic, accent = accent, selected = draft.soundUri != null) { runCatching { pickAudio.launch(arrayOf("audio/*")) } }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (draft.hasReminder()) tr(fa, "همین حالا روی ", "Set to ") + formatReminderTime(draft.reminderHour, draft.reminderMinute, fa) + tr(fa, " تنظیم شده", "") else tr(fa, "مثال: ۰۷:۳۰ صبح", "Example: 07:30 AM"),
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
        Button(onClick = { onSave(draft.copy(titleFa = draft.titleFa.trim(), titleEn = draft.titleEn.trim(), ritualFa = draft.ritualFa.trim(), ritualEn = draft.ritualEn.trim(), whyFa = draft.whyFa.trim())) }, enabled = draft.titleFa.isNotBlank() || draft.titleEn.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp)) { Text(tr(fa, "ذخیره", "Save")) }
        TextButton(onClick = onCancel, Modifier.fillMaxWidth()) { Text(tr(fa, "انصراف", "Cancel"), color = Muted) }
        Spacer(Modifier.height(20.dp))
    }
    if (durationOpen) DurationDialog(fa, draft.safeDuration, accent, { durationOpen = false }) { seconds -> draft = draft.copy(durationSeconds = seconds); durationOpen = false }
    if (reminderOpen) {
        ReminderTimeDialog(
            fa = fa,
            initialHour = draft.reminderHour ?: 9,
            initialMinute = draft.reminderMinute ?: 0,
            accent = accent,
            onLiveChange = { hour, minute -> draft = draft.copy(reminderHour = hour, reminderMinute = minute) },
            onDismiss = { reminderOpen = false },
            onConfirm = { hour, minute -> draft = draft.copy(reminderHour = hour, reminderMinute = minute); reminderOpen = false }
        )
    }
}
