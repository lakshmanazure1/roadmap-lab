package com.lakshman.roadmaplab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type
import kotlinx.coroutines.flow.SharedFlow

/** Where you are in the app. Tabs at the bottom (a rail on large screens); everything else opens on top and Back closes it. */
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

    val body: @Composable () -> Unit = {
        // Smooth motion: tabs fade through; screens opened on top slide in from the side.
        AnimatedContent(
            targetState = Triple(stack.lastOrNull(), tab, stack.size),
            transitionSpec = {
                when {
                    targetState.third > initialState.third ->
                        (slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(200))) togetherWith fadeOut(tween(120))
                    targetState.third < initialState.third ->
                        fadeIn(tween(200)) togetherWith (slideOutHorizontally(tween(220)) { it / 6 } + fadeOut(tween(160)))
                    else -> (fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(220, delayMillis = 60), initialScale = 0.98f)) togetherWith
                        fadeOut(tween(80))
                }
            },
            label = "screens",
        ) { (top, t, _) ->
            Readable {
                when (top) {
                    null -> when (t) {
                        0 -> TodayScreen(ui, act, nav)
                        1 -> PlanScreen(ui, act, nav)
                        2 -> ProgressScreen(ui, nav)
                        else -> LogScreen(ui, act, nav, backup)
                    }
                    is Screen.Task -> TaskScreen(ui, act, nav, top.id)
                    is Screen.Check -> CheckScreen(ui, act, nav, top.taskId, top.index)
                    is Screen.Weekly -> WeeklyReviewScreen(ui, act, nav, top.week)
                    is Screen.EvidenceEdit -> EvidenceScreen(ui, act, nav, top)
                    is Screen.Area -> AreaScreen(ui, act, nav, top.id)
                    Screen.Practice -> PracticeScreen(ui, act, nav)
                    Screen.Settings -> SettingsScreen(ui, act, nav, backup)
                    Screen.HowItWorks -> HowItWorksScreen(ui, nav)
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = WidthClass.of(maxWidth)
        CompositionLocalProvider(LocalWidthClass provides width) {
            if (width == WidthClass.COMPACT) {
                // Phones: the 4 destinations sit at the bottom, in the thumb zone.
                Scaffold(
                    containerColor = p.bg,
                    snackbarHost = { SnackbarHost(snack) },
                    bottomBar = {
                        AnimatedVisibility(stack.isEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            NavigationBar(containerColor = p.surface2.copy(alpha = 0.6f).compositeOver(p.bg)) {
                                TABS.forEachIndexed { i, t ->
                                    NavigationBarItem(
                                        selected = tab == i, onClick = { tab = i },
                                        icon = { Icon(if (tab == i) t.selected else t.icon, contentDescription = null) },
                                        label = { Text(t.label, style = Type.caption) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = p.onAccentSoft, selectedTextColor = p.text, indicatorColor = p.accentSoft,
                                            unselectedIconColor = p.text2, unselectedTextColor = p.text2,
                                        ),
                                    )
                                }
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) { body() }
                }
            } else {
                // Foldables and tablets: a navigation rail on the side, content beside it.
                Row(Modifier.fillMaxSize().background(p.bg)) {
                    NavigationRail(containerColor = p.bg, modifier = Modifier.fillMaxHeight().statusBarsPadding()) {
                        Spacer(Modifier.height(12.dp))
                        TABS.forEachIndexed { i, t ->
                            NavigationRailItem(
                                selected = stack.isEmpty() && tab == i, onClick = { stack.clear(); tab = i },
                                icon = { Icon(if (tab == i) t.selected else t.icon, contentDescription = null) },
                                label = { Text(t.label, style = Type.caption) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = p.onAccentSoft, selectedTextColor = p.text, indicatorColor = p.accentSoft,
                                    unselectedIconColor = p.text2, unselectedTextColor = p.text2,
                                ),
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight().navigationBarsPadding()) {
                        body()
                        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
    }
}

/** Material window size classes by width: compact (phones) < 600 dp ≤ medium (foldables) < 840 dp ≤ expanded (tablets). */
enum class WidthClass {
    COMPACT, MEDIUM, EXPANDED;

    companion object {
        fun of(width: Dp): WidthClass = when {
            width < 600.dp -> COMPACT
            width < 840.dp -> MEDIUM
            else -> EXPANDED
        }
    }
}

val LocalWidthClass = staticCompositionLocalOf { WidthClass.COMPACT }

/**
 * Keeps single-column screens at a readable width on large screens (centred, at most 720 dp);
 * screens with their own large-screen layout (Today on tablets) use the full width.
 */
@Composable
fun Readable(content: @Composable () -> Unit) {
    val wide = LocalWidthClass.current == WidthClass.EXPANDED
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(if (wide) Modifier.fillMaxHeight().widthIn(max = 1200.dp) else Modifier.fillMaxHeight().widthIn(max = 720.dp)) { content() }
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
