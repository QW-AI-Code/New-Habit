package com.duck.twominute.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duck.twominute.AppState
import com.duck.twominute.toPersianDigits

@Composable
fun ProgressScreen(state: AppState) {
    val fa = state.isPersian
    val streak = state.goalStreak()
    val best = state.bestGoalStreak()
    fun number(value: Int): String = if (fa) value.toString().toPersianDigits() else value.toString()
    fun percent(value: Int): String = if (fa) "$value٪".toPersianDigits() else "$value%"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(tr(fa, "پیشرفت", "Progress"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Rounded.LocalFireDepartment, number(streak), tr(fa, "روز پیوسته", "Day streak"), Amber, Modifier.weight(1f))
            StatTile(Icons.Rounded.EmojiEvents, number(best), tr(fa, "رکورد تو", "Best streak"), Violet, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Rounded.CheckCircle, number(state.totalCompletions), tr(fa, "ساخت عادت جدید", "New Habit"), Turquoise, Modifier.weight(1f))
            StatTile(Icons.Rounded.Schedule, number(state.totalMinutes), tr(fa, "دقیقه سرمایه‌گذاری", "Minutes invested"), Coral, Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        SectionCard {
            Text(tr(fa, "دوازده هفته گذشته", "Last twelve weeks"), style = MaterialTheme.typography.labelLarge, color = Muted)
            Spacer(Modifier.height(14.dp)); Heatmap(state, Turquoise, 12); Spacer(Modifier.height(12.dp))
            Text(tr(fa, "هر مربع یک روز است. روشن‌تر یعنی دفعات بیشتر ساخت عادت.", "Each square is a day. Brighter means more New Habit completions."), style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Spacer(Modifier.height(18.dp))
        Text(tr(fa, "هر هویت", "By identity"), style = MaterialTheme.typography.labelLarge, color = Muted)
        Spacer(Modifier.height(10.dp))
        if (state.identities.isEmpty()) SectionCard {
            Text(tr(fa, "وقتی هویت بسازی و ساخت عادت جدید را ثبت کنی، آمارت همین‌جا جمع می‌شود.", "Create an identity and log New Habit completions to see your numbers here."), color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        state.identities.forEach { identity ->
            val accent = parseColor(identity.colorHex)
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IdentityAvatar(identity, 38.dp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                        Text(identity.title(fa), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(tr(fa, "${identity.currentStreak()} روز پیوسته · رکورد ${identity.bestStreak()}", "${identity.currentStreak()} day streak · best ${identity.bestStreak()}"), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Text(number(identity.successes.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
                }
                Spacer(Modifier.height(12.dp)); ThinBar(identity.consistency(30), accent, Modifier.fillMaxWidth()); Spacer(Modifier.height(6.dp))
                val consistency = (identity.consistency(30) * 100).toInt()
                Text(tr(fa, "پایداری ۳۰ روزه: ${consistency.toString().toPersianDigits()}٪ · ${number(identity.totalSeconds / 60)} دقیقه", "30 day consistency: $consistency% · ${identity.totalSeconds / 60} min"), style = MaterialTheme.typography.labelMedium, color = Muted)
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}
