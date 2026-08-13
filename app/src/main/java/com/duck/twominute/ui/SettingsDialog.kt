package com.duck.twominute.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.duck.twominute.AppState
import com.duck.twominute.AppViewModel

@Composable
fun SettingsDialog(state: AppState, viewModel: AppViewModel, onDismiss: () -> Unit) {
    val fa = state.isPersian
    val context = LocalContext.current
    var durationOpen by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(pendingBackup.toByteArray())
                }
            }.isSuccess
            note = if (ok) {
                tr(fa, "پشتیبان ذخیره شد", "Backup saved")
            } else {
                tr(fa, "ذخیره نشد", "Could not save")
            }
        }
    }

    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text == null) {
                note = tr(fa, "فایل خوانده نشد", "Could not read the file")
            } else {
                viewModel.importJson(text) { success ->
                    note = if (success) {
                        tr(fa, "بازیابی انجام شد", "Restored")
                    } else {
                        tr(fa, "فایل معتبر نبود", "That file was not valid")
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceNavy, RoundedCornerShape(28.dp))
                .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                .padding(22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tr(fa, "تنظیمات", "Settings"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr(fa, "زبان", "Language"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Pill(
                    text = tr(fa, "فارسی", "English"),
                    icon = Icons.Rounded.Language,
                    selected = true,
                    onClick = { viewModel.setLanguage(if (fa) "en" else "fa") }
                )
            }

            HorizontalDivider(color = Hairline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(fa, "مدت پیش‌فرض", "Default length"),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = tr(
                            fa,
                            "برای هویت‌های تازه استفاده می‌شود",
                            "Used for brand new identities"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
                Pill(
                    text = durationLabel(state.defaultDurationSeconds, fa),
                    icon = Icons.Rounded.Timer,
                    onClick = { durationOpen = true }
                )
            }

            HorizontalDivider(color = Hairline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr(fa, "هدف روزانه", "Daily goal"),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = tr(fa, "چند آیین در روز یعنی برد", "Rituals a day that count as a win"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
                Stepper(
                    label = "",
                    value = state.goalToday.toString(),
                    onMinus = { viewModel.setDailyGoal(state.goalToday - 1) },
                    onPlus = { viewModel.setDailyGoal(state.goalToday + 1) }
                )
            }

            HorizontalDivider(color = Hairline)

            ToggleRow(
                title = tr(fa, "لرزش", "Vibration"),
                checked = state.vibrate,
                onChange = { viewModel.setVibrate(it) }
            )
            ToggleRow(
                title = tr(fa, "صدای پایان آیین", "Chime when finished"),
                checked = state.chime,
                onChange = { viewModel.setChime(it) }
            )
            ToggleRow(
                title = tr(fa, "روشن ماندن صفحه در آیین", "Keep screen on during a ritual"),
                checked = state.keepScreenOn,
                onChange = { viewModel.setKeepScreenOn(it) }
            )
            ToggleRow(
                title = tr(fa, "نمایش هویت‌های آرشیو‌شده", "Show archived identities"),
                checked = state.showArchived,
                onChange = { viewModel.setShowArchived(it) }
            )

            HorizontalDivider(color = Hairline)

            Text(
                text = tr(fa, "پشتیبان‌گیری", "Backup"),
                style = MaterialTheme.typography.bodyLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    text = tr(fa, "خروجی", "Export"),
                    icon = Icons.Rounded.Download,
                    onClick = {
                        viewModel.exportJson { json ->
                            pendingBackup = json
                            runCatching { createBackup.launch("two-minute-backup.json") }
                        }
                    }
                )
                Pill(
                    text = tr(fa, "بازیابی", "Import"),
                    icon = Icons.Rounded.Upload,
                    onClick = {
                        runCatching {
                            openBackup.launch(arrayOf("application/json", "text/plain"))
                        }
                    }
                )
            }

            if (note.isNotBlank()) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = Turquoise
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(tr(fa, "بستن", "Close"), color = Muted)
            }
        }
    }

    if (durationOpen) {
        DurationDialog(
            fa = fa,
            initialSeconds = state.defaultDurationSeconds,
            onDismiss = { durationOpen = false },
            onConfirm = { seconds ->
                viewModel.setDefaultDuration(seconds)
                durationOpen = false
            }
        )
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
