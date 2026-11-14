package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.ui.components.Label
import com.lakshman.roadmaplab.ui.components.MonoText
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.components.TrackBar
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab

@Composable
fun PlanScreen(
    ui: UiState,
    vm: LabViewModel,
    padding: PaddingValues,
    listState: LazyListState,
    expanded: SnapshotStateMap<String, Boolean>,
    onOpenTask: (String) -> Unit,
) {
    val lab = LocalLab.current
    val r = ui.s.roadmap ?: return
    val p = ui.progress
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column {
                Text("Progress", style = MaterialTheme.typography.headlineSmall)
                Muted(r.title)
                if (ui.s.settings.reducedPlan) {
                    Text("Reduced plan is on: optional tasks are set aside.", style = MaterialTheme.typography.bodySmall, color = lab.accent)
                }
            }
        }
        item { YouAreHerePanel(ui, vm) }
        item {
            Panel {
                Label("Tracks")
                r.tracks.forEach { track ->
                    Spacer(Modifier.height(16.dp))
                    TrackBar(track, p.trackPercent[track] ?: 0.0)
                }
            }
        }
        item {
            Panel {
                Label("Milestones")
                r.milestones.forEach { m ->
                    Spacer(Modifier.height(12.dp))
                    val days = (m.day - ui.today).toInt()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(Modifier.size(8.dp).background(lab.track(m.track)))
                        Spacer(Modifier.width(8.dp))
                        MonoText(Fmt.short(m.day), style = LabType.monoSmall, color = lab.muted, modifier = Modifier.width(64.dp))
                        Text(m.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                            color = if (days < 0) lab.muted else lab.text)
                        MonoText(if (days < 0) "passed" else Fmt.relDays(days), style = LabType.monoSmall,
                            color = if (days in 0..14) lab.accent else lab.muted)
                    }
                }
            }
        }
        item { Label("Phases", Modifier.padding(top = 4.dp)) }
        items(ui.phases, key = { it.phase.id }) { pp ->
            val open = expanded[pp.phase.id] ?: false
            Panel(padding = 16.dp, stripe = lab.track(pp.phase.track)) {
                Row(
                    Modifier.fillMaxWidth().clickable { expanded[pp.phase.id] = !open },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        com.lakshman.roadmaplab.ui.components.TrackTag(pp.phase.track)
                        Text(pp.phase.title, style = MaterialTheme.typography.titleSmall)
                        MonoText("${Fmt.range(pp.phase.startDay, pp.phase.endDay)} · ${pp.done}/${pp.tasks.size} tasks",
                            style = LabType.monoSmall, color = lab.muted)
                    }
                    MonoText(Fmt.pct0(pp.percent), style = LabType.number, color = if (pp.percent >= 99.95) lab.accent else lab.text)
                    Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = lab.muted)
                }
                if (open) {
                    Spacer(Modifier.height(8.dp))
                    pp.tasks.forEach { t -> PlanTaskRow(t, ui.latest[t.id]?.status, t.id in ui.s.dayPlans[ui.today].orEmpty()) { onOpenTask(t.id) } }
                }
            }
        }
    }
}

@Composable
private fun PlanTaskRow(task: PlanTask, status: TaskStatus?, isToday: Boolean, onClick: () -> Unit) {
    val lab = LocalLab.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoText(Fmt.short(task.plannedDay), style = LabType.monoSmall, color = lab.muted, modifier = Modifier.width(56.dp))
        MonoText(
            when (status) {
                TaskStatus.DONE -> "✓"
                TaskStatus.PARTLY -> "◐"
                TaskStatus.SKIPPED -> "–"
                null -> "·"
            },
            style = LabType.mono, color = if (status == TaskStatus.DONE) lab.accent else lab.muted, modifier = Modifier.width(20.dp),
        )
        Text(
            task.title + if (isToday) "  (today)" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = if (status == TaskStatus.DONE) lab.muted else lab.text,
            modifier = Modifier.weight(1f),
        )
    }
}
