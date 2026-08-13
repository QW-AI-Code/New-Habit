package com.duck.twominute.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duck.twominute.AppState
import com.duck.twominute.AppViewModel
import com.duck.twominute.Identity
import com.duck.twominute.PersianDate
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: AppState, viewModel: AppViewModel, onOpenSettings: () -> Unit) {
    val fa = state.isPersian
    val running by viewModel.running.collectAsStateWithLifecycle()
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val secondsLeft by viewModel.secondsLeft.collectAsStateWithLifecycle()
    val totalSeconds by viewModel.totalSeconds.collectAsStateWithLifecycle()
    val justCompleted by viewModel.justCompleted.collectAsStateWithLifecycle()

    val identity = state.current()
    val accent = if (identity == null) Turquoise else parseColor(identity.colorHex)
    var durationOpen by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(identity?.id, identity?.durationSeconds, running) {
        if (!running) {
            viewModel.prepare(identity?.safeDuration ?: state.defaultDurationSeconds)
        }
    }

    DisposableEffect(running, paused, state.keepScreenOn) {
        view.keepScreenOn = running && !paused && state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(justCompleted) {
        if (justCompleted) {
            delay(2600L)
            viewModel.acknowledgeCompletion()
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (totalSeconds <= 0) {
            0f
        } else {
            1f - secondsLeft.toFloat() / totalSeconds.toFloat()
        },
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "ritual-progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        HomeHeader(
            state = state,
            fa = fa,
            onToggleLanguage = { viewModel.setLanguage(if (fa) "en" else "fa") },
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = Turquoise,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr(fa, "هدف امروز", "Today's goal"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Muted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${state.doneToday} / ${state.goalToday}",
                    fontWeight = FontWeight.Bold,
                    color = if (state.doneToday >= state.goalToday) Turquoise else Ink
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ThinBar(
                fraction = state.doneToday.toFloat() / state.goalToday.toFloat(),
                accent = Turquoise,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            WeekStrip(state = state, fa = fa)
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (identity == null) {
            EmptyToday(fa)
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = identity.id,
                    transitionSpec = {
                        (fadeIn(tween(280)) + slideInVertically(animationSpec = tween(320)) { full -> full / 8 }) togetherWith
                            fadeOut(tween(180))
                    },
                    label = "focus-identity"
                ) { shownId ->
                    val shown = state.active.firstOrNull { it.id == shownId } ?: identity
                    val shownAccent = parseColor(shown.colorHex)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IdentityAvatar(identity = shown, size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tr(fa, "هویت بعدی", "NEXT IDENTITY"),
                                color = shownAccent,
                                fontSize = 12.sp,
                                letterSpacing = 1.1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = shown.title(fa),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = shown.ritual(fa),
                            style = MaterialTheme.typography.titleMedium,
                            color = Muted,
                            textAlign = TextAlign.Center
                        )
                        if (shown.why(fa).isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = shown.why(fa),
                                style = MaterialTheme.typography.bodySmall,
                                color = shownAccent.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 60, 120, 300, 600).forEach { preset ->
                        Pill(
                            text = durationLabel(preset, fa),
                            accent = accent,
                            selected = !running && identity.safeDuration == preset,
                            onClick = {
                                if (!running) viewModel.setDuration(identity.id, preset)
                            }
                        )
                    }
                    Pill(
                        text = tr(fa, "دلخواه", "Custom"),
                        icon = Icons.Rounded.Timer,
                        accent = accent,
                        selected = !running && identity.safeDuration !in listOf(30, 60, 120, 300, 600),
                        onClick = { if (!running) durationOpen = true }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = tr(fa, "مدت انتخاب‌شده: ", "Selected length: ") + formatClock(identity.safeDuration) + " · " + durationLabel(identity.safeDuration, fa),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                TimerDial(
                    progress = progress,
                    accent = accent,
                    running = running,
                    paused = paused,
                    timeText = formatClock(secondsLeft),
                    hintText = when {
                        paused -> tr(fa, "ادامه بده", "Resume")
                        running -> identity.title(fa)
                        else -> tr(fa, "شروع کن", "Start")
                    },
                    onClick = {
                        when {
                            !running -> viewModel.start(identity)
                            paused -> viewModel.resume()
                            else -> viewModel.pause()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (running) {
                        Pill(
                            text = tr(fa, "توقف", "Stop"),
                            icon = Icons.Rounded.Stop,
                            accent = Coral,
                            onClick = { viewModel.stop() }
                        )
                        Pill(
                            text = tr(fa, "+۱ دقیقه", "+1 min"),
                            icon = Icons.Rounded.Timer,
                            accent = accent,
                            onClick = { viewModel.addSeconds(60) }
                        )
                    } else {
                        Pill(
                            text = tr(fa, "هویت بعدی", "Next"),
                            icon = Icons.Rounded.SkipNext,
                            accent = accent,
                            onClick = { viewModel.skipToNext() }
                        )
                        Pill(
                            text = tr(fa, "ثبت بدون تایمر", "Log it"),
                            icon = Icons.Rounded.Check,
                            accent = accent,
                            onClick = { viewModel.logNow(identity.id) }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = justCompleted,
                    enter = fadeIn(tween(240)) + scaleIn(animationSpec = tween(320, easing = FastOutSlowInEasing), initialScale = 0.85f),
                    exit = fadeOut(tween(200))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr(fa, "ثبت شد. یک قدم دیگر نزدیک‌تر.", "Logged. One step closer."),
                            color = accent,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = tr(fa, "چک‌لیست امروز", "Today's checklist"),
                style = MaterialTheme.typography.labelLarge,
                color = Muted
            )
            Spacer(modifier = Modifier.height(10.dp))
            state.active.forEach { row ->
                ChecklistRow(
                    identity = row,
                    fa = fa,
                    highlighted = row.id == identity.id,
                    onLog = { viewModel.logNow(row.id) },
                    onSelect = { viewModel.selectIdentity(row.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (durationOpen && identity != null) {
        DurationDialog(
            fa = fa,
            initialSeconds = identity.safeDuration,
            accent = accent,
            onDismiss = { durationOpen = false },
            onConfirm = { seconds ->
                viewModel.setDuration(identity.id, seconds)
                durationOpen = false
            }
        )
    }
}

@Composable
private fun HomeHeader(
    state: AppState,
    fa: Boolean,
    onToggleLanguage: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val today = LocalDate.now()
    val jalali = PersianDate.fromGregorian(today)
    val dateLabel = if (fa) {
        "${jalali.day} ${PersianDate.monthName(jalali.month)} ${jalali.year}"
    } else {
        today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))
    }
    val streak = state.goalStreak()

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tr(fa, "آیین امروز", "Today's ritual"),
                style = MaterialTheme.typography.labelMedium,
                color = Muted
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        AnimatedVisibility(visible = streak > 0, enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.8f), exit = fadeOut(tween(200))) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Amber.copy(alpha = 0.14f), RoundedCornerShape(50))
                    .border(1.dp, Amber.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$streak",
                    color = Amber,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        IconButton(onClick = onToggleLanguage) {
            Icon(Icons.Rounded.Language, contentDescription = "language", tint = Muted)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "settings", tint = Muted)
        }
    }
}

@Composable
private fun ChecklistRow(
    identity: Identity,
    fa: Boolean,
    highlighted: Boolean,
    onLog: () -> Unit,
    onSelect: () -> Unit
) {
    val accent = parseColor(identity.colorHex)
    val done = identity.doneToday
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (highlighted) RaisedNavy.copy(alpha = 0.55f) else SurfaceNavy,
                RoundedCornerShape(18.dp)
            )
            .border(
                1.dp,
                if (highlighted) accent.copy(alpha = 0.5f) else Hairline,
                RoundedCornerShape(18.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        IdentityAvatar(identity = identity, size = 36.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = identity.title(fa),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = durationLabel(identity.safeDuration, fa) + " · " + identity.ritual(fa),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    if (done) accent else RaisedNavy,
                    CircleShape
                )
                .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
                .clickable { onLog() },
            contentAlignment = Alignment.Center
        ) {
            if (identity.safeTimesPerDay > 1 && !done && identity.todayCount > 0) {
                Text(
                    text = "${identity.todayCount}/${identity.safeTimesPerDay}",
                    fontSize = 10.sp,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (done) Navy else Muted,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyToday(fa: Boolean) {
    SectionCard {
        Text(
            text = tr(fa, "هنوز هویتی نساخته‌ای", "No identity yet"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = tr(
                fa,
                "به زبانه‌ی هویت‌ها برو، یک هویت بساز و مدت آیینش را هرچه خواستی تعیین کن.",
                "Open the Identities tab, create one identity and pick any ritual length you like."
            ),
            color = Muted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
