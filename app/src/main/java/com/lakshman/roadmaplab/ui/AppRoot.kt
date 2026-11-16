package com.lakshman.roadmaplab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type
import kotlinx.coroutines.flow.SharedFlow

/** Where you are in the app. Tabs at the bottom; everything else opens on top and Back closes it. */
sealed interface Screen {
    data class Task(val id: String) : Screen
    data class Check(val taskId: String, val index: Int) : Screen
    data class Weekly(val week: Int) : Screen
    data class EvidenceEdit(val id: Long?, val day: Long, val itemId: String?, val story: Boolean) : Screen
    data class Area(val id: String) : Screen
    data object Practice : Screen
    data object Settings : Screen
    data object HowItWorks : Screen
}

/** What a screen can open. */
class Nav(val open: (Screen) -> Unit, val back: () -> Unit, val tab: (Int) -> Unit)

private class Tab(val label: String, val icon: ImageVector, val selected: ImageVector)

private val TABS = listOf(
    Tab("Today", Icons.Outlined.Today, Icons.Filled.Today),
    Tab("Plan", Icons.Outlined.ViewWeek, Icons.Filled.ViewWeek),
    Tab("Progress", Icons.Outlined.Insights, Icons.Filled.Insights),
    Tab("Log", Icons.Outlined.EditNote, Icons.Filled.EditNote),
)

@Composable
fun AppRoot(ui: UiState?, act: Actions, notes: SharedFlow<Note>?, backup: BackupHooks) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().background(p.bg)) {
        when {
            ui == null -> Column(Modifier.fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) { Muted("Loading your plan…") }
            !ui.s.settings.v2Intro -> IntroScreen(ui, act)
            else -> MainFlow(ui, act, notes, backup)
        }
    }
}

@Composable
private fun MainFlow(ui: UiState, act: Actions, notes: SharedFlow<Note>?, backup: BackupHooks) {
    val p = LocalPalette.current
    val stack = remember { mutableStateListOf<Screen>() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val nav = remember { Nav(open = { stack.add(it) }, back = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }, tab = { tab = it }) }

    LaunchedEffect(notes) {
        notes?.collect { n ->
            val r = snack.showSnackbar(n.text, actionLabel = n.action, withDismissAction = n.action == null)
            if (r == SnackbarResult.ActionPerformed) n.onAction?.invoke()
        }
    }
    BackHandler(enabled = stack.isEmpty() && tab != 0) { tab = 0 }
    BackHandler(enabled = stack.isNotEmpty()) { nav.back() }

    Scaffold(
        containerColor = p.bg,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (stack.isEmpty()) NavigationBar(containerColor = p.surface, tonalElevation = 0.dp) {
                TABS.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(if (tab == i) t.selected else t.icon, contentDescription = null) },
                        label = { Text(t.label, style = Type.caption) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = p.accent, selectedTextColor = p.accent, indicatorColor = p.accentSoft,
                            unselectedIconColor = p.text2, unselectedTextColor = p.text2,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when (val top = stack.lastOrNull()) {
                null -> when (tab) {
                    0 -> TodayScreen(ui, act, nav)
                    1 -> PlanScreen(ui, act, nav)
                    2 -> ProgressScreen(ui, nav)
                    else -> LogScreen(ui, act, nav, backup)
                }
                is Screen.Task -> TaskScreen(ui, act, nav, top.id)
                is Screen.Check -> CheckScreen(ui, act, nav, top.taskId, top.index)
                is Screen.Weekly -> WeeklyReviewScreen(ui, act, nav, top.week)
                is Screen.EvidenceEdit -> EvidenceScreen(ui, act, nav, top)
                is Screen.Area -> AreaScreen(ui, nav, top.id)
                Screen.Practice -> PracticeScreen(ui, act, nav)
                Screen.Settings -> SettingsScreen(ui, act, nav, backup)
                Screen.HowItWorks -> HowItWorksScreen(ui, nav)
            }
        }
    }
}

/** Backup, restore and export need the system file picker, so the activity provides them. */
class BackupHooks(
    val backupNow: () -> Unit = {},
    val restore: () -> Unit = {},
    val exportEvidence: () -> Unit = {},
    val importPlan: () -> Unit = {},
    val useBundledPlan: () -> Unit = {},
    val usingImportedPlan: Boolean = false,
    val safetyCopies: Int = 0,
    /** "Mon 16 Nov, before your fresh start" for the newest safety copy, or null. */
    val latestCopy: String? = null,
    val bringBack: () -> Unit = {},
)

@Suppress("unused")
private val Transparent = Color.Transparent
