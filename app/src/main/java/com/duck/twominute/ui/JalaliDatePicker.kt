package com.duck.twominute.ui

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import com.duck.twominute.PersianDate
import com.duck.twominute.reminders.ReminderText
import com.duck.twominute.toPersianDigits
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Month-grid date picker that works in the Jalali (Shamsi) calendar by default,
 * with a Gregorian view one tap away. The week starts on Saturday in Jalali mode.
 * Days before [minDate] cannot be picked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JalaliDatePickerDialog(
    fa: Boolean,
    initial: LocalDate,
    accent: Color = Turquoise,
    title: String? = null,
    minDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    var jalaliMode by remember { mutableStateOf(fa) }
    var picked by remember { mutableStateOf(initial) }
    // The grid is anchored on the initial day; offset 0 shows its month.
    var monthOffset by remember { mutableIntStateOf(0) }
    val cells = remember(jalaliMode, monthOffset, initial) {
        if (jalaliMode) jalaliMonthCells(initial, monthOffset) else gregorianMonthCells(initial, monthOffset)
    }
    val header = remember(jalaliMode, monthOffset, initial) {
        if (jalaliMode) {
            val (year, month) = shiftedJalaliMonth(initial, monthOffset)
            (PersianDate.monthName(month) + " " + year).toPersianDigits()
        } else {
            YearMonth.from(initial).plusMonths(monthOffset.toLong()).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
        }
    }

    fun jumpTo(date: LocalDate) {
        picked = date
        monthOffset = if (jalaliMode) {
            val a = PersianDate.fromGregorian(initial)
            val b = PersianDate.fromGregorian(date)
            (b.year - a.year) * 12 + (b.month - a.month)
        } else {
            val a = YearMonth.from(initial)
            val b = YearMonth.from(date)
            (b.year - a.year) * 12 + (b.monthValue - a.monthValue)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceNavy, RoundedCornerShape(28.dp))
                .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title ?: tr(fa, "انتخاب تاریخ", "Pick a date"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(ReminderText.date(picked, fa), style = MaterialTheme.typography.bodyLarge, color = accent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            // The other calendar underneath, so both are always visible.
            Text(ReminderText.date(picked, !fa), style = MaterialTheme.typography.bodySmall, color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(tr(fa, "شمسی", "Jalali"), accent = accent, selected = jalaliMode) { if (!jalaliMode) { jalaliMode = true; jumpTo(picked) } }
                Pill(tr(fa, "میلادی", "Gregorian"), accent = accent, selected = !jalaliMode) { if (jalaliMode) { jalaliMode = false; jumpTo(picked) } }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // In RTL the start arrow points right; "previous" is always the start side.
                IconButton({ monthOffset -= 1 }) { Icon(if (fa) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft, tr(fa, "ماه قبل", "Previous month"), tint = accent) }
                Text(header, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                IconButton({ monthOffset += 1 }) { Icon(if (fa) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight, tr(fa, "ماه بعد", "Next month"), tint = accent) }
            }
            Row(Modifier.fillMaxWidth()) {
                (if (jalaliMode) persianWeekLabels else gregorianWeekLabels).forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(4.dp))
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date != null) {
                                val disabled = minDate != null && date.isBefore(minDate)
                                val isPicked = date == picked
                                val isToday = date == today
                                val label = if (jalaliMode) PersianDate.fromGregorian(date).day.toString().toPersianDigits() else date.dayOfMonth.toString()
                                Box(
                                    Modifier
                                        .padding(2.dp)
                                        .aspectRatio(1f)
                                        .background(if (isPicked) accent else Color.Transparent, CircleShape)
                                        .border(if (isToday && !isPicked) 1.dp else 0.dp, if (isToday && !isPicked) accent else Color.Transparent, CircleShape)
                                        .then(if (disabled) Modifier else Modifier.clickable { picked = date }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.25.em),
                                        maxLines = 1,
                                        color = when {
                                            isPicked -> Navy
                                            disabled -> Muted.copy(alpha = 0.35f)
                                            isToday -> accent
                                            else -> Ink
                                        },
                                        fontWeight = if (isPicked || isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(tr(fa, "امروز", "Today"), accent = accent, selected = picked == today) { jumpTo(today) }
                Pill(tr(fa, "فردا", "Tomorrow"), accent = accent, selected = picked == today.plusDays(1)) { jumpTo(today.plusDays(1)) }
                Pill(tr(fa, "پس‌فردا", "In 2 days"), accent = accent, selected = picked == today.plusDays(2)) { jumpTo(today.plusDays(2)) }
                Pill(tr(fa, "هفته بعد", "Next week"), accent = accent, selected = picked == today.plusDays(7)) { jumpTo(today.plusDays(7)) }
            }
            Spacer(Modifier.height(16.dp))
            val valid = minDate == null || !picked.isBefore(minDate)
            Button(
                onClick = { onConfirm(picked) },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDim, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text(tr(fa, "تایید", "Set")) }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(tr(fa, "انصراف", "Cancel"), color = Muted) }
        }
    }
}
