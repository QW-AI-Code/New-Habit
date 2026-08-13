package com.duck.twominute.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.duck.twominute.R
import java.util.Locale

val Navy = Color(0xFF04090F)
val DeepNavy = Color(0xFF091422)
val SurfaceNavy = Color(0xFF0E1B2A)
val RaisedNavy = Color(0xFF16283A)
val Hairline = Color(0xFF1E3346)
val Turquoise = Color(0xFF39D6C3)
val TurquoiseDim = Color(0xFF1E8A80)
val Violet = Color(0xFF8B7BF7)
val Amber = Color(0xFFF5B85C)
val Coral = Color(0xFFE2725B)
val Ink = Color(0xFFEAF4F3)
val Muted = Color(0xFF95A9B6)
val Aurora = Color(0xFF4FC8FF)
val Plum = Color(0xFF6B4BE0)
val Synapse = Color(0xFFB08BFF)

val accentPalette = listOf("#39D6C3", "#8B7BF7", "#F5B85C", "#E2725B", "#5AC8FA", "#7ED957", "#FF7AB8", "#C3CBD6")

fun parseColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Turquoise)
fun appBackground(): Brush = Brush.verticalGradient(listOf(DeepNavy, Navy, Color(0xFF02060B)))

fun tr(fa: Boolean, faText: String, enText: String): String {
    val f = faText.replace("آیین هویت", "ساخت عادت جدید").replace("آیین انجام‌شده", "ساخت عادت جدید").replace("آیین‌های", "عادت‌های").replace("آیین‌ها", "عادت‌ها").replace("آیینش", "عادتش").replace("آیین", "عادت")
    val e = enText.replace("Identity Ritual", "New Habit").replace("Rituals", "New Habit").replace("rituals", "New Habit").replace("Ritual", "Habit").replace("ritual", "habit")
    return if (fa) f else e
}

@Composable
fun AppBackdrop(modifier: Modifier = Modifier) {
    val drift = rememberInfiniteTransition(label = "backdrop")
    val t by drift.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    Canvas(modifier) {
        drawRect(brush = Brush.verticalGradient(listOf(DeepNavy, Navy, Color(0xFF02060B))))
        val w = size.width
        val h = size.height
        val first = Offset(w * (0.16f + 0.14f * t), h * (0.14f + 0.04f * t))
        drawCircle(brush = Brush.radialGradient(listOf(Turquoise.copy(alpha = 0.14f), Color.Transparent), first, w * 0.8f), radius = w * 0.8f, center = first)
        val second = Offset(w * (0.92f - 0.16f * t), h * (0.44f + 0.07f * t))
        drawCircle(brush = Brush.radialGradient(listOf(Plum.copy(alpha = 0.16f), Color.Transparent), second, w * 0.85f), radius = w * 0.85f, center = second)
        val third = Offset(w * 0.48f, h * (0.96f - 0.03f * t))
        drawCircle(brush = Brush.radialGradient(listOf(Aurora.copy(alpha = 0.10f), Color.Transparent), third, w * 0.95f), radius = w * 0.95f, center = third)
    }
}

fun formatClock(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val rest = safe % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, rest) else "%02d:%02d".format(minutes, rest)
}

fun durationLabel(seconds: Int, fa: Boolean): String = when {
    seconds < 60 -> tr(fa, "$seconds ثانیه", "${seconds}s")
    seconds % 60 == 0 -> tr(fa, "${seconds / 60} دقیقه", "${seconds / 60}m")
    else -> formatClock(seconds)
}

fun formatReminderTime(hour: Int?, minute: Int?, fa: Boolean): String {
    if (hour == null) return tr(fa, "تنظیم یادآور", "Set reminder")
    val h = hour.coerceIn(0, 23)
    val m = (minute ?: 0).coerceIn(0, 59)
    val displayHour = if (h == 0) 12 else if (h > 12) h - 12 else h
    val suffix = if (h < 12) tr(fa, "صبح", "AM") else tr(fa, "عصر", "PM")
    return "%02d:%02d %s".format(Locale.US, displayHour, m, suffix)
}

val IranSans = FontFamily(
    Font(R.font.iransans_regular, FontWeight.Normal),
    Font(R.font.iransans_bold, FontWeight.Bold)
)

private fun typographyWith(family: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family)
    )
}

private val TwoMinuteColors = darkColorScheme(primary = TurquoiseDim, onPrimary = Ink, primaryContainer = TurquoiseDim, onPrimaryContainer = Ink, secondary = Turquoise, onSecondary = Navy, secondaryContainer = RaisedNavy, onSecondaryContainer = Ink, background = Navy, onBackground = Ink, surface = SurfaceNavy, onSurface = Ink, surfaceVariant = RaisedNavy, onSurfaceVariant = Muted, outline = Hairline, error = Coral, onError = Ink)

@Composable
fun TwoMinuteTheme(
    rightToLeft: Boolean,
    content: @Composable () -> Unit
) {
    val direction = if (rightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = typographyWith(IranSans)
    CompositionLocalProvider(
        LocalLayoutDirection provides direction,
        LocalContentColor provides Ink,
        LocalTextStyle provides typography.bodyLarge.copy(fontFamily = IranSans, color = Ink)
    ) {
        MaterialTheme(colorScheme = TwoMinuteColors, typography = typography, content = content)
    }
}
