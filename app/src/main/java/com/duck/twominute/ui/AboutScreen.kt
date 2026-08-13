package com.duck.twominute.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duck.twominute.AppState

@Composable
fun AboutScreen(state: AppState) {
    val fa = state.isPersian
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 40.dp)) {
        Text(tr(fa, "درباره برنامه", "About"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Column(modifier = Modifier.fillMaxWidth().background(SurfaceNavy, RoundedCornerShape(24.dp)).border(1.dp, Hairline, RoundedCornerShape(24.dp)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Psychology, null, tint = Turquoise, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(10.dp))
            Text(tr(fa, "ساخت عادت جدید", "New Habit"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(tr(fa, "نسخه ۱.۰.۰", "Version 1.0.0"), color = Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Text(tr(fa, "یک ابزار ساده و حرفه‌ای برای ساختن عادت‌ها بر پایه هویتی که می‌خواهی بسازی.", "A focused habit tool for building the identity you want through small, repeatable habits."), color = Ink, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(16.dp))
        AboutFeature(Icons.Rounded.Psychology, tr(fa, "مدت دلخواه", "Flexible duration"), tr(fa, "برای هر عادت از ۱۰ ثانیه تا ۲ ساعت زمان تعیین کن.", "Choose any habit length from 10 seconds to 2 hours."))
        AboutFeature(Icons.Rounded.CheckCircle, tr(fa, "ثبت و یادآوری", "Logging and reminders"), tr(fa, "تایمر، ثبت دستی، هدف روزانه و یادآورهای قابل تنظیم.", "Timer, manual logging, daily goals and configurable reminders."))
        AboutFeature(Icons.Rounded.TrendingUp, tr(fa, "پیشرفت واقعی", "Real progress"), tr(fa, "زنجیره‌ها، تقویم، نمودار فعالیت و پشتیبان‌گیری از اطلاعات.", "Streaks, calendar, activity view and data backup."))
        Spacer(Modifier.height(18.dp))
        Column(modifier = Modifier.fillMaxWidth().background(RaisedNavy, RoundedCornerShape(18.dp)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = Ink, modifier = Modifier.size(28.dp))
                Spacer(Modifier.size(12.dp))
                Text(tr(fa, "ساخت عادت جدید", "New Habit"), fontWeight = FontWeight.Bold)
            }
            Text(tr(fa, "یک مسیر ساده برای ساخت عادت‌های پایدار.", "A simple path to building lasting habits."), modifier = Modifier.padding(start = 40.dp, top = 6.dp), color = Turquoise, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AboutFeature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Turquoise, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(12.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
