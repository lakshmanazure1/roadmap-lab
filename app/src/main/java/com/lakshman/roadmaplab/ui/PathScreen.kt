package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakshman.roadmaplab.domain.Milestone
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.ui.components.LabOutlinedButton
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.theme.LocalLab

/** One row on the path: a phase banner, a task node or a milestone. */
private sealed interface PathItem {
    val key: String

    data class Banner(val title: String, val track: String) : PathItem {
        override val key get() = "banner-$title"
    }

    data class Node(val task: PlanTask, val state: NodeState, val index: Int) : PathItem {
        override val key get() = "node-${task.id}"
    }

    data class Trophy(val milestone: Milestone, val index: Int) : PathItem {
        override val key get() = "ms-${milestone.id}"
    }
}

private enum class NodeState { DONE, TODAY, TODAY_DONE, NEXT }

/** Duolingo-style zigzag: horizontal offsets in dp. */
private val ZIGZAG = listOf(0, 56, 84, 56, 0, -56, -84, -56)

@Composable
fun PathScreen(
    ui: UiState,
    vm: LabViewModel,
    padding: PaddingValues,
    listState: LazyListState,
    onStart: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onPractice: () -> Unit,
    onBackup: () -> Unit,
) {
    val lab = LocalLab.current
    val items = remember(ui) { buildPath(ui) }
    val dayDone = ui.todayLog.eveningDone
    val started = ui.todayTasks.any { it.todayStatus != null } || ui.reviewedToday > 0

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "header") { PathHeader(ui, onOpenSettings) }
            if (ui.restart) {
                item(key = "restart") {
                    Panel {
                        Text("Welcome back 👋", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Muted("No catching up needed. Today shows just your next steps; the rest of the plan moved forward.",
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        LabOutlinedButton("Got it", onClick = { vm.updateSettings { it.copy(restartDismissedDay = ui.today) } })
                    }
                }
            }
            if (ui.backupDue) {
                item(key = "backup") {
                    Panel(onClick = onBackup) {
                        Text("Sunday backup ☁", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Muted("Tap to save a dated copy to Google Drive.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items.forEach { pi ->
                item(key = pi.key) {
                    when (pi) {
                        is PathItem.Banner -> PhaseBanner(pi.title, pi.track)
                        is PathItem.Node -> TaskNode(pi, onOpenTask)
                        is PathItem.Trophy -> TrophyNode(pi, ui.today)
                    }
                }
            }
        }

        // The one big button.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            val label = when {
                dayDone && ui.reviewQueue.isNotEmpty() -> "DAY COMPLETE ✓  ·  PRACTICE MORE"
                dayDone -> "DAY COMPLETE ✓"
                started -> "CONTINUE TODAY"
                else -> "START TODAY"
            }
            Button(
                onClick = { if (dayDone) { if (ui.reviewQueue.isNotEmpty()) onPractice() } else onStart() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(12.dp, RoundedCornerShape(50)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (dayDone) lab.done else lab.text,
                    contentColor = Color.White,
                ),
            ) {
                if (!dayDone) Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(label, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            }
        }
    }
}

private fun buildPath(ui: UiState): List<PathItem> {
    val r = ui.s.roadmap ?: return emptyList()
    val reduced = ui.s.settings.reducedPlan
    val todayIds = ui.s.dayPlans[ui.today].orEmpty().toSet()
    val active = r.tasks.filter { Planner.isActive(it, reduced) }.sortedWith(compareBy({ it.plannedDay }, { it.order }))
    val done = active.filter { ui.latest[it.id]?.status == TaskStatus.DONE && it.id !in todayIds }
        .sortedBy { ui.latest[it.id]?.createdAt ?: 0 }
    val today = active.filter { it.id in todayIds }
    val upcoming = active.filter { ui.latest[it.id]?.status != TaskStatus.DONE && it.id !in todayIds }.take(10)
    val shown = done.takeLast(3) + today + upcoming

    val phases = r.phases.associateBy { it.id }
    val out = ArrayList<PathItem>()
    var lastPhase: String? = null
    var i = 0
    val lastDay = shown.maxOfOrNull { it.plannedDay } ?: ui.today
    val milestones = r.milestones.filter { it.day >= ui.today && it.day <= lastDay + 1 }.toMutableList()
    for (t in shown) {
        while (milestones.isNotEmpty() && milestones.first().day < t.plannedDay && t.id !in todayIds && ui.latest[t.id]?.status != TaskStatus.DONE) {
            out.add(PathItem.Trophy(milestones.removeAt(0), i++))
        }
        if (t.phaseId != lastPhase) {
            val p = phases[t.phaseId]
            if (p != null) out.add(PathItem.Banner(p.title, p.track))
            lastPhase = t.phaseId
        }
        val todayStatus = ui.todayTasks.firstOrNull { it.task.id == t.id }?.todayStatus
        val state = when {
            t.id in todayIds && todayStatus != null && todayStatus != TaskStatus.SKIPPED -> NodeState.TODAY_DONE
            t.id in todayIds -> NodeState.TODAY
            ui.latest[t.id]?.status == TaskStatus.DONE -> NodeState.DONE
            else -> NodeState.NEXT
        }
        out.add(PathItem.Node(t, state, i++))
    }
    milestones.forEach { out.add(PathItem.Trophy(it, i++)) }
    return out
}

@Composable
private fun PathHeader(ui: UiState, onOpenSettings: () -> Unit) {
    val lab = LocalLab.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(Fmt.greeting(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light), color = lab.text)
            Muted("${Fmt.day(ui.today)} · ${Fmt.pct(ui.progress.percent)} done · ${ui.progress.label}")
        }
        Pill {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Streak", tint = Color(0xFFFF9600), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text("${ui.streak}", style = MaterialTheme.typography.titleMedium, color = lab.text)
        }
        Spacer(Modifier.width(8.dp))
        Surface(onClick = onOpenSettings, shape = CircleShape, color = lab.panel, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = lab.text, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun Pill(content: @Composable RowScope.() -> Unit) {
    val lab = LocalLab.current
    Surface(shape = RoundedCornerShape(50), color = lab.panel) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
private fun PhaseBanner(title: String, track: String) {
    val lab = LocalLab.current
    val c = lab.track(track)
    Surface(shape = RoundedCornerShape(20.dp), color = c, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(track.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f), letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}

@Composable
private fun TaskNode(item: PathItem.Node, onOpenTask: (String) -> Unit) {
    val lab = LocalLab.current
    val t = item.task
    val c = lab.track(t.track)
    val offset = ZIGZAG[item.index % ZIGZAG.size].dp
    val isToday = item.state == NodeState.TODAY || item.state == NodeState.TODAY_DONE
    val size = if (isToday) 84.dp else 68.dp
    Column(Modifier.fillMaxWidth().offset(x = offset), horizontalAlignment = Alignment.CenterHorizontally) {
        if (item.state == NodeState.TODAY) {
            Surface(shape = RoundedCornerShape(50), color = lab.panel, modifier = Modifier.shadow(6.dp, RoundedCornerShape(50))) {
                Text("TODAY", style = MaterialTheme.typography.labelMedium, color = c, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        val fill = when (item.state) {
            NodeState.DONE, NodeState.TODAY_DONE -> c
            NodeState.TODAY -> c
            NodeState.NEXT -> Color(0xFFE3E8EC)
        }
        Box(
            Modifier
                .size(size)
                .shadow(if (isToday) 10.dp else 3.dp, CircleShape)
                .clip(CircleShape)
                .background(fill)
                .then(if (item.state == NodeState.TODAY) Modifier.border(BorderStroke(5.dp, Color.White.copy(alpha = 0.7f)), CircleShape) else Modifier)
                .clickable { onOpenTask(t.id) },
            contentAlignment = Alignment.Center,
        ) {
            when (item.state) {
                NodeState.DONE, NodeState.TODAY_DONE ->
                    Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(34.dp))
                NodeState.TODAY ->
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Today", tint = Color.White, modifier = Modifier.size(40.dp))
                NodeState.NEXT ->
                    Box(Modifier.size(14.dp).clip(CircleShape).background(c.copy(alpha = 0.55f)))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            t.title,
            style = if (isToday) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            color = if (item.state == NodeState.NEXT) lab.muted else lab.text,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
        if (item.state == NodeState.NEXT) Muted(Fmt.short(t.plannedDay))
    }
}

@Composable
private fun TrophyNode(item: PathItem.Trophy, today: Long) {
    val lab = LocalLab.current
    val offset = ZIGZAG[item.index % ZIGZAG.size].dp
    val days = (item.milestone.day - today).toInt()
    Column(Modifier.fillMaxWidth().offset(x = offset), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(72.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Color(0xFFFFC800)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = "Milestone", tint = Color.White, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(item.milestone.title, style = MaterialTheme.typography.titleSmall, color = lab.text, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp))
        Muted("Milestone · ${Fmt.relDays(days)}")
    }
}
