package com.duck.twominute.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duck.twominute.R

/** Exact launcher asset, with a light pulse traveling through its existing brain folds. */
@Composable
fun SynapseIntro(fa: Boolean, accent: Color = Color(0xFF2F70C2), onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(3600, easing = LinearEasing)); onFinished() }
    Box(Modifier.fillMaxSize().clickable(interactionSource = interaction, indication = null) { onFinished() }, contentAlignment = Alignment.Center) {
        AppBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(292.dp), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(292.dp).alpha((progress.value * 4f).coerceIn(0f, 1f)))
                Canvas(Modifier.size(292.dp)) { drawFoldPulse(progress.value, accent) }
            }
            Spacer(Modifier.height(14.dp))
            Text(tr(fa, "ساخت عادت جدید", "New Habit"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink.copy(alpha = reveal(progress.value, .38f, .7f)), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(tr(fa, "یک مسیر تازه در مغز ساخته می‌شود", "Building a new neural pathway"), style = MaterialTheme.typography.bodyMedium, color = Muted.copy(alpha = reveal(progress.value, .48f, .9f)), textAlign = TextAlign.Center)
        }
    }
}

private fun reveal(v: Float, from: Float, to: Float) = ((v - from) / (to - from)).coerceIn(0f, 1f)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFoldPulse(progress: Float, accent: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val points = listOf(.34f to .30f, .43f to .42f, .33f to .53f, .43f to .65f, .66f to .30f, .57f to .42f, .67f to .53f, .57f to .65f)
    val travel = reveal(progress, .22f, .92f) * (points.size - 1)
    points.forEachIndexed { index, (x, y) ->
        val amount = (travel - index + 1f).coerceIn(0f, 1f)
        if (amount > 0f) {
            val p = Offset(size.width * x, size.height * y)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .95f * amount), accent.copy(alpha = .8f * amount), Color.Transparent), p, size.minDimension * .075f), size.minDimension * .075f, p)
        }
    }
    val pulse = reveal(progress, .84f, 1f)
    if (pulse > 0f) drawCircle(color = accent.copy(alpha = .35f * (1f - pulse)), radius = size.minDimension * (.18f + .42f * pulse), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
}
