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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import com.duck.twominute.R
import com.duck.twominute.toPersianDigits
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

fun durationLabel(seconds: Int, fa: Boolean): String {
    val label = when {
        seconds < 60 -> tr(fa, "$seconds ثانیه", "${seconds}s")
        seconds % 60 == 0 -> tr(fa, "${seconds / 60} دقیقه", "${seconds / 60}m")
        else -> formatClock(seconds)
    }
    // Persian UI: Persian digits, so "5 دقیقه" never sits next to "۵ دقیقه".
    return if (fa) label.toPersianDigits() else label
}

fun formatReminderTime(hour: Int?, minute: Int?, fa: Boolean): String {
    if (hour == null) return tr(fa, "تنظیم یادآور", "Set reminder")
    val h = hour.coerceIn(0, 23)
    val m = (minute ?: 0).coerceIn(0, 59)
    val displayHour = if (h == 0) 12 else if (h > 12) h - 12 else h
    val suffix = if (h < 12) tr(fa, "صبح", "AM") else tr(fa, "عصر", "PM")
    val label = "%02d:%02d %s".format(Locale.US, displayHour, m, suffix)
    return if (fa) label.toPersianDigits() else label
}

/**
 * Vazirmatn (Vazir) for the whole app, Persian and English alike.
 *
 * The previous IRANSans files drew some glyph forms wrongly (the three dots of
 * «پ» came out upside down in bold). Vazirmatn ships correct forms for every
 * Persian letter plus a matching Latin set, so one family covers both languages.
 * Five static weights are bundled so Material's Medium/SemiBold styles get a real
 * cut instead of a synthesized one.
 */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold)
)

/**
 * Persian glyphs are taller than Latin ones (dots above and below, kaf/gaf
 * strokes). Material's fixed sp line heights are tuned for Latin and clip or
 * overlap Persian text, so every style gets a line height relative to its own
 * font size, centred and never trimmed: whatever size a Text uses, its lines
 * always have room.
 */
private fun TextStyle.roomy(family: FontFamily, rtl: Boolean): TextStyle = copy(
    fontFamily = family,
    lineHeight = if (rtl) 1.62.em else 1.45.em,
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None),
    letterSpacing = if (rtl) 0.em else letterSpacing
)

private fun typographyWith(family: FontFamily, rtl: Boolean): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.roomy(family, rtl),
        displayMedium = base.displayMedium.roomy(family, rtl),
        displaySmall = base.displaySmall.roomy(family, rtl),
        headlineLarge = base.headlineLarge.roomy(family, rtl),
        headlineMedium = base.headlineMedium.roomy(family, rtl),
        headlineSmall = base.headlineSmall.roomy(family, rtl),
        titleLarge = base.titleLarge.roomy(family, rtl),
        titleMedium = base.titleMedium.roomy(family, rtl),
        titleSmall = base.titleSmall.roomy(family, rtl),
        bodyLarge = base.bodyLarge.roomy(family, rtl),
        bodyMedium = base.bodyMedium.roomy(family, rtl),
        bodySmall = base.bodySmall.roomy(family, rtl),
        labelLarge = base.labelLarge.roomy(family, rtl),
        labelMedium = base.labelMedium.roomy(family, rtl),
        labelSmall = base.labelSmall.roomy(family, rtl)
    )
}

private val TwoMinuteColors = darkColorScheme(primary = TurquoiseDim, onPrimary = Ink, primaryContainer = TurquoiseDim, onPrimaryContainer = Ink, secondary = Turquoise, onSecondary = Navy, secondaryContainer = RaisedNavy, onSecondaryContainer = Ink, background = Navy, onBackground = Ink, surface = SurfaceNavy, onSurface = Ink, surfaceVariant = RaisedNavy, onSurfaceVariant = Muted, outline = Hairline, error = Coral, onError = Ink)

@Composable
fun TwoMinuteTheme(
    rightToLeft: Boolean,
    content: @Composable () -> Unit
) {
    val direction = if (rightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = remember(rightToLeft) { typographyWith(Vazirmatn, rightToLeft) }
    CompositionLocalProvider(
        LocalLayoutDirection provides direction,
        LocalContentColor provides Ink,
        LocalTextStyle provides typography.bodyLarge.copy(color = Ink)
    ) {
        MaterialTheme(colorScheme = TwoMinuteColors, typography = typography, content = content)
    }
}
