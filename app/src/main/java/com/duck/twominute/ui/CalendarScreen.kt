package com.duck.twominute.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.AddAlert
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.duck.twominute.AppState
import com.duck.twominute.JalaliDate
import com.duck.twominute.PersianDate
import com.duck.twominute.toPersianDigits
import com.duck.twominute.reminders.ReminderSchedule
import com.duck.twominute.reminders.ReminderText
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val persianWeekLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
internal val gregorianWeekLabels = listOf("S", "M", "T", "W", "T", "F", "S")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(state: AppState, onAddReminder: (LocalDate) -> Unit = {}) {
    val fa = state.isPersian
    val today = remember { LocalDate.now() }
    var jalaliMode by rememberSaveable { mutableStateOf(fa) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(today) }
    val days = remember(jalaliMode, monthOffset) { if (jalaliMode) jalaliMonthCells(today, monthOffset) else gregorianMonthCells(today, monthOffset) }
    val headerTitle = remember(jalaliMode, monthOffset) {
        if (jalaliMode) { val anchor = shiftedJalaliMonth(today, monthOffset); (PersianDate.monthName(anchor.second) + " " + anchor.first).toPersianDigits() }
        else YearMonth.from(today).plusMonths(monthOffset.toLong()).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
    }
    val headerSubtitle = remember(jalaliMode, monthOffset) {
        val firstReal = days.filterNotNull().firstOrNull() ?: today
        if (jalaliMode) YearMonth.from(firstReal).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
        else { val jalali = PersianDate.fromGregorian(firstReal); (PersianDate.monthName(jalali.month) + " " + jalali.year).toPersianDigits() }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(tr(fa, "تقویم", "Calendar"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(tr(fa, "شمسی", "Jalali"), selected = jalaliMode) { jalaliMode = true }
            Pill(tr(fa, "میلادی", "Gregorian"), selected = !jalaliMode) { jalaliMode = false }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            // RTL mirrors the row: "previous" sits on the right in Persian, so its arrow points right.
            IconButton({ monthOffset -= 1 }) { Icon(if (fa) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft, tr(fa, "ماه قبل", "Previous month"), tint = Turquoise) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(headerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(headerSubtitle, style = MaterialTheme.typography.bodySmall, color = Muted) }
            IconButton({ monthOffset += 1 }) { Icon(if (fa) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight, tr(fa, "ماه بعد", "Next month"), tint = Turquoise) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) { (if (jalaliMode) persianWeekLabels else gregorianWeekLabels).forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted, style = MaterialTheme.typography.labelSmall) } }
        Spacer(Modifier.height(6.dp))
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val hasReminder = date != null && state.reminders.any { it.enabled && ReminderSchedule.occursOn(it, date) && !it.doneEpochDays.contains(date.toEpochDay()) }
                    DayCell(Modifier.weight(1f), date, jalaliMode, today, selected, if (date == null) 0 else state.completionsOn(date), state.goalToday, hasReminder) { selected = it }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(20.dp))
        val selectedJalali = PersianDate.fromGregorian(selected)
        SectionCard {
            Text((selectedJalali.day.toString() + " " + PersianDate.monthName(selectedJalali.month) + " " + selectedJalali.year).toPersianDigits(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(selected.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)), style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(12.dp))
            // v1.1.0: the reminders of this day, and a shortcut to add one on it.
            val dayReminders = state.reminders.filter { it.enabled && ReminderSchedule.occursOn(it, selected) && !it.doneEpochDays.contains(selected.toEpochDay()) }
            dayReminders.forEach { reminder ->
                val rings = ReminderSchedule.ringsOn(reminder, selected)
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Alarm, null, tint = Amber, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp))
                    Text(reminder.title.ifBlank { tr(fa, "یادآور", "Reminder") }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    val first = rings.firstOrNull()
                    Text(
                        if (first == null) "" else if (rings.size > 1) ReminderText.clock24(first.hour, first.minute, fa) + " +" + ReminderText.digits((rings.size - 1).toString(), fa) else ReminderText.clock24(first.hour, first.minute, fa),
                        color = Amber,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            if (!selected.isBefore(today)) {
                Spacer(Modifier.height(6.dp))
                Pill(tr(fa, "افزودن یادآور برای این روز", "Add a reminder on this day"), icon = Icons.Rounded.AddAlert, accent = Amber) { onAddReminder(selected) }
                Spacer(Modifier.height(10.dp))
            }
            val done = state.identitiesDoneOn(selected)
            if (done.isEmpty()) Text(tr(fa, "این روز آیینی ثبت نشده", "Nothing logged on this day"), color = Muted, style = MaterialTheme.typography.bodyMedium)
            else done.forEach { identity ->
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    IdentityAvatar(identity, 30.dp); Spacer(Modifier.width(10.dp)); Text(identity.title(fa), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text("×" + identity.countOn(selected).toString().let { if (fa) it.toPersianDigits() else it }, color = parseColor(identity.colorHex), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DayCell(modifier: Modifier, date: LocalDate?, jalaliMode: Boolean, today: LocalDate, selected: LocalDate, completions: Int, goal: Int, hasReminder: Boolean, onSelect: (LocalDate) -> Unit) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (date != null) {
            val isToday = date == today; val isSelected = date == selected; val met = completions >= goal && completions > 0
            val raw = if (jalaliMode) PersianDate.fromGregorian(date).day.toString() else date.dayOfMonth.toString()
            val label = if (jalaliMode) raw.toPersianDigits() else raw
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.aspectRatio(1f).padding(3.dp).background(when { met -> Turquoise.copy(alpha=.22f); completions > 0 -> Turquoise.copy(alpha=.1f); isSelected -> RaisedNavy; else -> androidx.compose.ui.graphics.Color.Transparent }, CircleShape).border(if (isToday || isSelected) 1.dp else 0.dp, when { isToday -> Turquoise; isSelected -> Muted; else -> androidx.compose.ui.graphics.Color.Transparent }, CircleShape).clickable { onSelect(date) }) {
                // Digits need no tall Persian line box; a compact one keeps the number and its dots inside the circle.
                Text(label, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.25.em), maxLines = 1, color = if (isToday) Turquoise else Ink, fontWeight = if (isToday || met) FontWeight.Bold else FontWeight.Normal)
                Spacer(Modifier.height(2.dp))
                if (completions > 0 || hasReminder) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(completions.coerceAtMost(3)) { Box(Modifier.size(4.dp).background(Turquoise, CircleShape)) }
                    // Amber dot: a reminder rings on this day.
                    if (hasReminder) Box(Modifier.size(4.dp).background(Amber, CircleShape))
                }
            }
        }
    }
}

internal fun shiftedJalaliMonth(today: LocalDate, offset: Int): Pair<Int, Int> { val current = PersianDate.fromGregorian(today); var year = current.year; var month = current.month + offset; while (month > 12) { month -= 12; year++ }; while (month < 1) { month += 12; year-- }; return year to month }
internal fun jalaliMonthCells(today: LocalDate, offset: Int): List<LocalDate?> { val (year, month) = shiftedJalaliMonth(today, offset); val length = PersianDate.monthLength(year, month); val firstDay = PersianDate.toGregorian(JalaliDate(year, month, 1)); val leading = (firstDay.dayOfWeek.value + 1) % 7; return ArrayList<LocalDate?>(leading + length).apply { repeat(leading) { add(null) }; for (day in 1..length) add(PersianDate.toGregorian(JalaliDate(year, month, day))) } }
internal fun gregorianMonthCells(today: LocalDate, offset: Int): List<LocalDate?> { val month = YearMonth.from(today).plusMonths(offset.toLong()); val firstDay = month.atDay(1); val leading = firstDay.dayOfWeek.value % 7; return ArrayList<LocalDate?>(leading + month.lengthOfMonth()).apply { repeat(leading) { add(null) }; for (day in 1..month.lengthOfMonth()) add(month.atDay(day)) } }
