package com.lakshman.roadmaplab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.lakshman.roadmaplab.ui.components.MonoText
import com.lakshman.roadmaplab.ui.theme.LocalLab

sealed interface Screen {
    data object Tabs : Screen
    data class TaskDetail(val taskId: String) : Screen
    data class DayDetail(val day: Long) : Screen
    data object Settings : Screen
}

private class Tab(val label: String, val icon: ImageVector, val selected: ImageVector)

private val TABS = listOf(
    Tab("Today", Icons.Outlined.Today, Icons.Filled.Today),
    Tab("Review", Icons.Outlined.School, Icons.Filled.School),
    Tab("Plan", Icons.Outlined.Timeline, Icons.Filled.Timeline),
    Tab("Journal", Icons.Outlined.Book, Icons.Filled.Book),
)

@Composable
fun AppRoot(ui: UiState?, vm: LabViewModel) {
    val lab = LocalLab.current
    val wash = if (lab.soft) {
        Brush.verticalGradient(0f to lab.bgTop, 0.35f to lab.bgMid, 1f to lab.bg)
    } else {
        Brush.verticalGradient(listOf(lab.bg, lab.bg))
    }
    Box(Modifier.fillMaxSize().background(wash)) {
        when {
            ui == null || ui.s.roadmap == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MonoText("Loading the plan…", color = lab.muted)
            }
            !ui.s.settings.onboarded -> OnboardingScreen(ui, vm)
            else -> MainFlow(ui, vm)
        }
    }
}

@Composable
private fun MainFlow(ui: UiState, vm: LabViewModel) {
    val lab = LocalLab.current
    val stack = remember { mutableStateListOf<Screen>() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val todayList = rememberLazyListState()
    val planList = rememberLazyListState()
    val journalList = rememberLazyListState()
    val expandedPhases = remember { mutableStateMapOf<String, Boolean>() }
    val backup = rememberBackupToDrive(vm)
    fun open(s: Screen) { stack.add(s) }
    fun back() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }

    BackHandler(enabled = stack.isEmpty() && tab != 0) { tab = 0 }
    BackHandler(enabled = stack.isNotEmpty()) { back() }

    when (val top = stack.lastOrNull() ?: Screen.Tabs) {
        is Screen.TaskDetail -> TaskScreen(ui, vm, top.taskId, onBack = { back() })
        is Screen.DayDetail -> DayScreen(ui, vm, top.day, onBack = { back() })
        Screen.Settings -> SettingsScreen(ui, vm, onBack = { back() }, onBackup = backup)
        Screen.Tabs -> Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = lab.panel, tonalElevation = 0.dp) {
                    TABS.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(if (tab == i) t.selected else t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (lab.soft) lab.text else lab.accent,
                                selectedTextColor = if (lab.soft) lab.text else lab.accent,
                                indicatorColor = if (lab.soft) Color.Transparent else lab.grid,
                                unselectedIconColor = lab.muted, unselectedTextColor = lab.muted,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            when (tab) {
                0 -> TodayScreen(
                    ui, vm, padding, todayList,
                    onOpenTask = { open(Screen.TaskDetail(it)) },
                    onOpenSettings = { open(Screen.Settings) },
                    onReview = { tab = 1 },
                    onBackup = backup,
                )
                1 -> ReviewScreen(ui, vm, padding)
                2 -> PlanScreen(ui, vm, padding, planList, expandedPhases, onOpenTask = { open(Screen.TaskDetail(it)) })
                else -> JournalScreen(ui, padding, journalList, onOpenDay = { open(Screen.DayDetail(it)) })
            }
        }
    }
}
