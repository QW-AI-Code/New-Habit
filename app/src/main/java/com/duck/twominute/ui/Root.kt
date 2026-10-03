package com.duck.twominute.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duck.twominute.AppViewModel
import com.duck.twominute.ai.planner.PlannerViewModel

@Composable
fun TwoMinuteRoot(viewModel: AppViewModel = viewModel(), planner: PlannerViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var aiSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var introDone by rememberSaveable { mutableStateOf(false) }
    val fa = state.isPersian
    val labels = listOf(tr(fa, "امروز", "Today"), tr(fa, "هویت‌ها", "Identities"), tr(fa, "تقویم", "Calendar"), tr(fa, "پیشرفت", "Progress"), tr(fa, "درباره", "About"))
    val icons = listOf(Icons.Rounded.Today, Icons.Rounded.Person, Icons.Rounded.CalendarMonth, Icons.Rounded.Insights, Icons.Rounded.Info)
    val contentAlpha by animateFloatAsState(targetValue = if (introDone) 1f else 0f, animationSpec = tween(520), label = "content-alpha")
    val contentScale by animateFloatAsState(targetValue = if (introDone) 1f else 1.05f, animationSpec = tween(620), label = "content-scale")

    TwoMinuteTheme(rightToLeft = fa) {
        Surface(modifier = Modifier.fillMaxSize(), color = Navy, contentColor = Ink) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppBackdrop(Modifier.fillMaxSize())
                Scaffold(
                    modifier = Modifier.graphicsLayer { alpha = contentAlpha; scaleX = contentScale; scaleY = contentScale },
                    containerColor = Color.Transparent,
                    contentColor = Ink,
                    bottomBar = {
                        Column(Modifier.fillMaxWidth()) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                            NavigationBar(containerColor = SurfaceNavy.copy(alpha = 0.95f), contentColor = Ink, tonalElevation = 0.dp, windowInsets = NavigationBarDefaults.windowInsets) {
                                labels.forEachIndexed { index, label ->
                                    val selected = selectedTab == index
                                    val iconScale by animateFloatAsState(targetValue = if (selected) 1.14f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "nav-icon")
                                    NavigationBarItem(selected = selected, onClick = { selectedTab = index }, icon = { Icon(icons[index], label, Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }) }, label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Turquoise, selectedTextColor = Turquoise, indicatorColor = Turquoise.copy(alpha = 0.16f), unselectedIconColor = Muted, unselectedTextColor = Muted, disabledIconColor = Muted, disabledTextColor = Muted), alwaysShowLabel = true)
                                }
                            }
                        }
                    },
                    content = { innerPadding ->
                        Box(Modifier.fillMaxSize().padding(innerPadding)) {
                            AnimatedContent(targetState = selectedTab, transitionSpec = { (fadeIn(tween(260)) + slideInVertically(animationSpec = tween(300)) { full -> full / 24 }) togetherWith fadeOut(tween(160)) }, label = "tab-switch") { tab ->
                                when (tab) {
                                    0 -> HomeScreen(state, viewModel) { settingsOpen = true }
                                    1 -> IdentitiesScreen(state, viewModel, planner)
                                    2 -> CalendarScreen(state)
                                    3 -> ProgressScreen(state)
                                    else -> AboutScreen(state)
                                }
                            }
                        }
                    }
                )
                if (settingsOpen) SettingsDialog(state, viewModel, onOpenAi = { settingsOpen = false; aiSettingsOpen = true }) { settingsOpen = false }
                if (aiSettingsOpen) AiSettingsDialog(fa, planner) { aiSettingsOpen = false }
                AnimatedVisibility(visible = !introDone, enter = fadeIn(tween(1)), exit = fadeOut(tween(560))) { SynapseIntro(fa = fa) { introDone = true } }
            }
        }
    }
}
