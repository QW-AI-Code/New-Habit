package com.duck.twominute.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duck.twominute.AppState
import com.duck.twominute.R

private const val GITHUB_HANDLE = "QW-AI-Code"
private const val GITHUB_URL = "https://github.com/QW-AI-Code"

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
        Spacer(Modifier.height(22.dp))
        DeveloperCard(fa)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun DeveloperCard(fa: Boolean) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(20.dp)
    val cardBrush = Brush.linearGradient(listOf(Color(0xFF17293C), Color(0xFF101D2E), Color(0xFF152740)))
    val badgeBrush = Brush.linearGradient(listOf(Color(0xFF243343), Color(0xFF0C131B)))
    val openGithub: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        Unit
    }

    Text(
        tr(fa, "سازنده", "Developer"),
        color = Muted,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBrush, shape)
            .border(1.dp, Hairline, shape)
            .clickable(onClick = openGithub)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(badgeBrush, CircleShape)
                .border(1.dp, Turquoise.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = "GitHub",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                GITHUB_HANDLE,
                color = Ink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr)
            )
            Text(
                "github.com/" + GITHUB_HANDLE,
                color = Turquoise,
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr)
            )
            Text(
                tr(fa, "برای دیدن سورس و نسخه‌های جدید ضربه بزن.", "Tap to view the source code and latest releases."),
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.size(10.dp))
        Icon(
            Icons.Rounded.OpenInNew,
            contentDescription = tr(fa, "باز کردن گیت‌هاب", "Open GitHub"),
            tint = Muted,
            modifier = Modifier.size(20.dp)
        )
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
