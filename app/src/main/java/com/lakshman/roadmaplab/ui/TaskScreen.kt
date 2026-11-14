package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.ui.components.BackBar
import com.lakshman.roadmaplab.ui.components.Choice
import com.lakshman.roadmaplab.ui.components.KeyValue
import com.lakshman.roadmaplab.ui.components.Label
import com.lakshman.roadmaplab.ui.components.LabOutlinedButton
import com.lakshman.roadmaplab.ui.components.MonoText
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab

@Composable
fun TaskScreen(ui: UiState, vm: LabViewModel, taskId: String, onBack: () -> Unit) {
    val lab = LocalLab.current
    val t = ui.tasksById[taskId]
    Scaffold(
        containerColor = lab.bg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { BackBar(t?.title ?: "Task", onBack) },
    ) { padding ->
        if (t == null) {
            Box(Modifier.padding(padding).padding(16.dp)) { Muted("This task is no longer in the roadmap.") }
        } else {
        val phase = ui.s.roadmap?.phases?.firstOrNull { it.id == t.phaseId }
        val logs = ui.s.taskLogs.filter { it.taskId == t.id }.sortedByDescending { it.createdAt }
        val status = ui.latest[t.id]?.status
        val todayIds = ui.s.dayPlans[ui.today].orEmpty()
        val minutes = t.minutes(ui.s.settings.mode)
        val cards = (t.unlocks.toSet() + ui.s.cards.values.filter { it.unlockedBy == t.id }.map { it.id }).size
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Panel {
                Label(t.track + (phase?.let { " · ${it.title}" } ?: ""))
                Spacer(Modifier.height(4.dp))
                Text(t.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                KeyValue("Planned", Fmt.day(t.plannedDay))
                KeyValue("Minutes", "${t.minFull} India · ${t.minDrc} DRC · ${t.minMinimum} minimum")
                KeyValue("Learn", t.learn)
                KeyValue("Apply", t.apply)
                KeyValue("Done when", t.doneWhen)
                if (cards > 0) KeyValue("Unlocks", "$cards question cards")
                if (!t.core) KeyValue("Optional", "Set aside in the reduced plan")
            }
            Panel {
                Label("Status")
                Spacer(Modifier.height(4.dp))
                MonoText(status?.label ?: "Not started", style = LabType.number,
                    color = if (status == TaskStatus.DONE) lab.accent else lab.text)
                Spacer(Modifier.height(10.dp))
                Muted("Logging here counts for today (${Fmt.min(minutes)} for the ${ui.s.settings.mode.label} mode).")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice("Done", false, onClick = { vm.launch { logTask(t.id, TaskStatus.DONE, minutes, false) } }, modifier = Modifier.weight(1f))
                    Choice("Partly", false, onClick = { vm.launch { logTask(t.id, TaskStatus.PARTLY, minutes / 2, false) } }, modifier = Modifier.weight(1f))
                    Choice("Skip", false, onClick = { vm.launch { logTask(t.id, TaskStatus.SKIPPED, 0, false) } }, modifier = Modifier.weight(1f))
                }
                if (status != TaskStatus.DONE && t.id !in todayIds && todayIds.size < Planner.MAX_TASKS_PER_DAY) {
                    Spacer(Modifier.height(8.dp))
                    LabOutlinedButton("Add to today", onClick = { vm.launch { pullIntoToday(t.id) } }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (logs.isNotEmpty()) {
                Panel {
                    Label("History")
                    logs.forEach { l ->
                        Spacer(Modifier.height(6.dp))
                        MonoText("${Fmt.full(l.day)} · ${l.status.label} · ${Fmt.min(l.minutes)}" + if (l.usedMinimum) " · short" else "",
                            style = LabType.monoSmall)
                        if (l.note.isNotBlank()) Muted(l.note)
                    }
                }
            }
        }
        }
    }
}
