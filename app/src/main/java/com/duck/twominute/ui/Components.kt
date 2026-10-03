package com.duck.twominute.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.duck.twominute.AppState
import com.duck.twominute.Identity
import java.time.LocalDate
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(SurfaceNavy, SurfaceNavy.copy(alpha = 0.86f))), RoundedCornerShape(24.dp))
            .border(1.dp, Hairline, RoundedCornerShape(24.dp))
            .padding(18.dp),
        content = content
    )
}

@Composable
fun IdentityAvatar(identity: Identity, size: Dp = 44.dp) {
    val accent = parseColor(identity.colorHex)
    Box(
        Modifier
            .size(size)
            .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.26f), accent.copy(alpha = 0.08f))), CircleShape)
            .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(glyphFor(identity.iconKey), null, tint = accent, modifier = Modifier.size(size * 0.5f))
    }
}

/**
 * A rounded chip-button.
 *
 * Its label is laid out on one line by default ([maxLines]) and ends in "…" if
 * the space really runs out, so a squeezed pill can never break its word into a
 * column of single letters (the v1.0.1 «افزودن» bug). The icon comes first in the
 * row, so it keeps its size; the label takes what is left.
 */
@Composable
fun Pill(
    text: String,
    icon: ImageVector? = null,
    accent: Color = Turquoise,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pill-scale"
    )
    val container by animateColorAsState(targetValue = if (selected) accent else RaisedNavy.copy(alpha = 0.62f), animationSpec = tween(240), label = "pill-bg")
    val outline by animateColorAsState(targetValue = if (selected) accent else Hairline, animationSpec = tween(240), label = "pill-outline")
    val label by animateColorAsState(targetValue = if (selected) Navy else Ink, animationSpec = tween(240), label = "pill-label")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(container)
            .border(1.dp, outline, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null) { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) Navy else accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = maxLines.coerceAtLeast(1),
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Screen title with its action buttons.
 *
 * Laid out as a flow: when the title and the actions fit side by side they share
 * one line (title at the start, actions at the end); when they do not — a narrow
 * phone, a large system font, a longer English label — the actions move to their
 * own line instead of being squeezed. Nothing is ever compressed into a column.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp)
        )
        Row(
            modifier = Modifier.align(Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

@Composable
fun StatTile(icon: ImageVector, value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(SurfaceNavy, RaisedNavy.copy(alpha = 0.55f))), RoundedCornerShape(20.dp))
            .border(1.dp, Hairline, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(10.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

@Composable
fun ThinBar(fraction: Float, accent: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 640, easing = FastOutSlowInEasing),
        label = "thin-bar"
    )
    Box(modifier.height(9.dp).clip(RoundedCornerShape(50)).background(RaisedNavy.copy(alpha = 0.9f))) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(9.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.72f), Aurora.copy(alpha = 0.85f))))
        )
    }
}

@Composable
fun WeekStrip(state: AppState, fa: Boolean, accent: Color = Turquoise) {
    val today = LocalDate.now()
    val days = (0..6).map { today.minusDays((6 - it).toLong()) }
    val pulse = rememberInfiniteTransition(label = "week")
    val ring by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "week-ring"
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            val count = state.completionsOn(day)
            val met = count >= state.goalToday
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(weekdayInitial(day.dayOfWeek.value, fa), style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(31.dp)
                        .background(if (met) accent else RaisedNavy.copy(alpha = 0.7f), CircleShape)
                        .border(if (day == today) 1.5.dp else 1.dp, if (day == today) accent.copy(alpha = ring) else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        met -> Icon(Icons.Rounded.Check, null, tint = Navy, modifier = Modifier.size(16.dp))
                        count > 0 -> Box(Modifier.size(8.dp).background(accent, CircleShape))
                    }
                }
            }
        }
    }
}

fun weekdayInitial(isoDay: Int, fa: Boolean): String = if (fa) when (isoDay) { 1 -> "د"; 2 -> "س"; 3 -> "چ"; 4 -> "پ"; 5 -> "ج"; 6 -> "ش"; else -> "ی" } else when (isoDay) { 1 -> "M"; 2 -> "T"; 3 -> "W"; 4 -> "T"; 5 -> "F"; 6 -> "S"; else -> "S" }
fun weekdayName(isoDay: Int, fa: Boolean): String = if (fa) when (isoDay) { 1 -> "دوشنبه"; 2 -> "سه‌شنبه"; 3 -> "چهارشنبه"; 4 -> "پنجشنبه"; 5 -> "جمعه"; 6 -> "شنبه"; else -> "یکشنبه" } else when (isoDay) { 1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"; else -> "Sun" }

/** The centrepiece dial: ticks, sweeping gradient arc, glowing head and a springy core button. */
@Composable
fun TimerDial(
    progress: Float,
    accent: Color,
    running: Boolean,
    paused: Boolean,
    timeText: String,
    hintText: String,
    onClick: () -> Unit,
    diameter: Dp = 272.dp
) {
    val active = running && !paused
    val pulse = rememberInfiniteTransition(label = "dial")
    val breathe by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dial-breathe"
    )
    val spin by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7200, easing = LinearEasing), RepeatMode.Restart),
        label = "dial-spin"
    )
    val halo by animateFloatAsState(targetValue = if (active) 0.26f else 0.12f, animationSpec = tween(700), label = "dial-halo")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "dial-press"
    )
    val idle = if (active) 1f else 0.985f + 0.015f * breathe
    val clamped = progress.coerceIn(0f, 1f)

    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val box = size
            val d = box.minDimension
            val center = Offset(box.width / 2f, box.height / 2f)
            val stroke = d * 0.042f
            val radius = d / 2f - stroke * 2.3f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.copy(alpha = halo * (0.55f + 0.45f * breathe)), Color.Transparent),
                    center,
                    d / 2f
                ),
                radius = d / 2f,
                center = center
            )
            val ticks = 60
            for (i in 0 until ticks) {
                val rad = ((-90f + i * (360f / ticks)) * (PI / 180f)).toFloat()
                val passed = i.toFloat() / ticks <= clamped
                val len = if (i % 5 == 0) d * 0.042f else d * 0.024f
                val outer = radius + stroke * 1.95f
                val dx = cos(rad)
                val dy = sin(rad)
                drawLine(
                    color = if (passed) accent.copy(alpha = 0.8f) else Hairline,
                    start = Offset(center.x + dx * (outer - len), center.y + dy * (outer - len)),
                    end = Offset(center.x + dx * outer, center.y + dy * outer),
                    strokeWidth = d * 0.0065f,
                    cap = StrokeCap.Round
                )
            }
            drawCircle(color = RaisedNavy, radius = radius, center = center, style = Stroke(width = stroke))
            drawCircle(color = Hairline.copy(alpha = 0.55f), radius = radius - stroke, center = center, style = Stroke(width = d * 0.004f))
            if (clamped > 0.002f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.22f), accent, Aurora, accent), center),
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val rad = ((-90f + 360f * clamped) * (PI / 180f)).toFloat()
                val head = Offset(center.x + cos(rad) * radius, center.y + sin(rad) * radius)
                drawCircle(
                    brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.9f), Color.Transparent), head, stroke * 2.6f),
                    radius = stroke * 2.6f,
                    center = head
                )
                drawCircle(color = Ink, radius = stroke * 0.38f, center = head)
            }
            if (active) {
                rotate(degrees = spin, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, accent.copy(alpha = 0.18f), Color.Transparent), center),
                        startAngle = -90f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = stroke * 1.7f)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(diameter * 0.64f)
                .graphicsLayer { scaleX = press * idle; scaleY = press * idle }
                .clip(CircleShape)
                .background(
                    if (running) Brush.verticalGradient(listOf(RaisedNavy, SurfaceNavy))
                    else Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.74f)))
                )
                .border(1.dp, if (running) accent.copy(alpha = 0.45f) else Color.Transparent, CircleShape)
                .clickable(interactionSource = interaction, indication = null) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 18.dp)) {
                Crossfade(targetState = active, label = "dial-icon") { isActive ->
                    Icon(
                        if (isActive) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        tint = if (running) accent else Navy,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(timeText, fontSize = 38.sp, fontWeight = FontWeight.Bold, color = if (running) Ink else Navy, maxLines = 1)
                if (hintText.isNotBlank()) {
                    // While running this is the identity title: keep it inside the circle.
                    Text(
                        hintText,
                        fontSize = 12.sp,
                        color = if (running) Muted else Navy.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun Stepper(label: String, value: String, accent: Color = Turquoise, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (label.isNotBlank()) {
            // The buttons keep their size; a long label wraps instead of pushing them away.
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(10.dp))
        }
        StepperButton(Icons.Rounded.Remove, accent, onMinus)
        Text(value, Modifier.width(60.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
        StepperButton(Icons.Rounded.Add, accent, onPlus)
    }
}

@Composable
private fun StepperButton(icon: ImageVector, accent: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "step-scale"
    )
    Box(
        Modifier
            .size(36.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(RaisedNavy)
            .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
    }
}

/** Live clock face used by the reminder picker so the chosen time is visible instantly. */
@Composable
fun MiniClockFace(hour: Int, minute: Int, accent: Color = Turquoise, diameter: Dp = 122.dp) {
    val h = hour.coerceIn(0, 23)
    val m = minute.coerceIn(0, 59)
    val minuteAngle by animateFloatAsState(targetValue = m * 6f, animationSpec = tween(280), label = "minute-hand")
    val hourAngle by animateFloatAsState(targetValue = (h % 12) * 30f + m * 0.5f, animationSpec = tween(280), label = "hour-hand")
    Canvas(Modifier.size(diameter)) {
        val d = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = d / 2f - d * 0.07f
        drawCircle(
            brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent), center, d / 2f),
            radius = d / 2f,
            center = center
        )
        drawCircle(color = SurfaceNavy, radius = radius, center = center)
        drawCircle(color = accent.copy(alpha = 0.55f), radius = radius, center = center, style = Stroke(width = d * 0.02f))
        for (i in 0 until 12) {
            val rad = ((-90f + i * 30f) * (PI / 180f)).toFloat()
            val inner = radius * 0.82f
            drawLine(
                color = if (i % 3 == 0) Ink.copy(alpha = 0.8f) else Muted.copy(alpha = 0.4f),
                start = Offset(center.x + cos(rad) * inner, center.y + sin(rad) * inner),
                end = Offset(center.x + cos(rad) * radius * 0.93f, center.y + sin(rad) * radius * 0.93f),
                strokeWidth = d * 0.012f,
                cap = StrokeCap.Round
            )
        }
        val hourRad = ((hourAngle - 90f) * (PI / 180f)).toFloat()
        val minuteRad = ((minuteAngle - 90f) * (PI / 180f)).toFloat()
        drawLine(
            color = Ink,
            start = center,
            end = Offset(center.x + cos(hourRad) * radius * 0.5f, center.y + sin(hourRad) * radius * 0.5f),
            strokeWidth = d * 0.045f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = accent,
            start = center,
            end = Offset(center.x + cos(minuteRad) * radius * 0.74f, center.y + sin(minuteRad) * radius * 0.74f),
            strokeWidth = d * 0.03f,
            cap = StrokeCap.Round
        )
        drawCircle(color = accent, radius = d * 0.035f, center = center)
    }
}

/** In app reminder picker. Every tap updates the big clock and the caller instantly. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderTimeDialog(
    fa: Boolean,
    initialHour: Int,
    initialMinute: Int,
    accent: Color = Turquoise,
    title: String? = null,
    onLiveChange: (Int, Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceNavy, RoundedCornerShape(28.dp))
                .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                .padding(22.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title ?: tr(fa, "زمان یادآور", "Reminder time"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                tr(fa, "هر تغییری بدهی، همان لحظه اینجا نمایش داده می‌شود", "Whatever you pick shows up here instantly"),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            MiniClockFace(hour, minute, accent)
            Spacer(Modifier.height(12.dp))
            Text("%02d:%02d".format(Locale.US, hour, minute), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(formatReminderTime(hour, minute, fa), style = MaterialTheme.typography.bodyMedium, color = Ink)
            Spacer(Modifier.height(18.dp))
            Stepper(
                label = tr(fa, "ساعت", "Hour"),
                value = "%02d".format(Locale.US, hour),
                accent = accent,
                onMinus = { hour = (hour + 23) % 24; onLiveChange(hour, minute) },
                onPlus = { hour = (hour + 1) % 24; onLiveChange(hour, minute) }
            )
            Spacer(Modifier.height(10.dp))
            Stepper(
                label = tr(fa, "دقیقه", "Minute"),
                value = "%02d".format(Locale.US, minute),
                accent = accent,
                onMinus = { minute = (minute + 55) % 60; onLiveChange(hour, minute) },
                onPlus = { minute = (minute + 5) % 60; onLiveChange(hour, minute) }
            )
            Spacer(Modifier.height(16.dp))
            Text(tr(fa, "زمان‌های پرکاربرد", "Quick picks"), style = MaterialTheme.typography.labelMedium, color = Muted)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(6 to 30, 7 to 0, 8 to 0, 12 to 30, 18 to 0, 21 to 0, 22 to 30).forEach { preset ->
                    val h = preset.first
                    val m = preset.second
                    Pill(
                        text = "%02d:%02d".format(Locale.US, h, m),
                        accent = accent,
                        selected = hour == h && minute == m,
                        onClick = { hour = h; minute = m; onLiveChange(h, m) }
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onConfirm(hour, minute) },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDim, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text(tr(fa, "تایید", "Set")) }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(tr(fa, "انصراف", "Cancel"), color = Muted)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationDialog(fa: Boolean, initialSeconds: Int, accent: Color = Turquoise, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var seconds by remember { mutableIntStateOf(initialSeconds.coerceIn(Identity.MIN_DURATION, Identity.MAX_DURATION)) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceNavy, RoundedCornerShape(28.dp))
                .border(1.dp, Hairline, RoundedCornerShape(28.dp))
                .padding(22.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(tr(fa, "مدت آیین", "Ritual length"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                tr(fa, "هر مدتی که خودت بخواهی، از ۱۰ ثانیه تا ۲ ساعت", "Anything you want, from 10 seconds to 2 hours"),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Text(formatClock(seconds), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(durationLabel(seconds, fa), style = MaterialTheme.typography.bodyMedium, color = Ink)
            Spacer(Modifier.height(14.dp))
            Slider(
                value = seconds.coerceAtMost(3600).toFloat(),
                onValueChange = { raw ->
                    seconds = ((raw / 5f).roundToInt() * 5).coerceIn(Identity.MIN_DURATION, Identity.MAX_DURATION)
                },
                valueRange = Identity.MIN_DURATION.toFloat()..3600f,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = RaisedNavy),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                tr(fa, "بکش و ببین: عدد بالا همان لحظه عوض می‌شود", "Drag it: the value above updates instantly"),
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Stepper(
                label = tr(fa, "دقیقه", "Minutes"),
                value = (seconds / 60).toString(),
                accent = accent,
                onMinus = { seconds = (seconds - 60).coerceIn(Identity.MIN_DURATION, Identity.MAX_DURATION) },
                onPlus = { seconds = (seconds + 60).coerceIn(Identity.MIN_DURATION, Identity.MAX_DURATION) }
            )
            Spacer(Modifier.height(10.dp))
            Stepper(
                label = tr(fa, "ثانیه", "Seconds"),
                value = (seconds % 60).toString(),
                accent = accent,
                onMinus = { seconds = (seconds - 5).coerceIn(Identity.MIN_DURATION, Identity.MAX_DURATION) },
                onPlus = { seconds = (seconds + 5).coerceIn(Identity.MIN_DURATION, Identity.MAX_DURATION) }
            )
            Spacer(Modifier.height(18.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Identity.DURATION_PRESETS.forEach { preset ->
                    Pill(durationLabel(preset, fa), accent = accent, selected = seconds == preset) { seconds = preset }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onConfirm(seconds) },
                colors = ButtonDefaults.buttonColors(containerColor = TurquoiseDim, contentColor = Ink),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text(tr(fa, "تایید", "Set")) }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(tr(fa, "انصراف", "Cancel"), color = Muted)
            }
        }
    }
}

@Composable
fun Heatmap(state: AppState, accent: Color, weeks: Int = 12) {
    val today = LocalDate.now()
    val total = weeks * 7
    val days = (0 until total).map { today.minusDays((total - 1 - it).toLong()) }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        days.chunked(7).forEach { column ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                column.forEach { day ->
                    val count = state.completionsOn(day)
                    val alpha = when {
                        count <= 0 -> 0.07f
                        count == 1 -> 0.34f
                        count == 2 -> 0.62f
                        else -> 1f
                    }
                    Box(Modifier.size(12.dp).background(accent.copy(alpha = alpha), RoundedCornerShape(3.dp)))
                }
            }
        }
    }
}
