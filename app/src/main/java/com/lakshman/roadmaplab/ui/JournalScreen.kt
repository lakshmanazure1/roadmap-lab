package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.ui.components.AutosaveField
import com.lakshman.roadmaplab.ui.components.BackBar
import com.lakshman.roadmaplab.ui.components.KeyValue
import com.lakshman.roadmaplab.ui.components.Label
import com.lakshman.roadmaplab.ui.components.MonoText
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab

private fun DayLog.searchText(): String =
    listOf(note, ifThen, evModule, evProblem, evAction, evResult, evLearning).joinToString(" ").lowercase()

@Composable
fun JournalScreen(ui: UiState, padding: PaddingValues, listState: LazyListState, onOpenDay: (Long) -> Unit) {
    val lab = LocalLab.current
    var query by rememberSaveable { mutableStateOf("") }
    val entries = remember(ui.s.dayLogs, query) {
        val q = query.trim().lowercase()
        ui.s.dayLogs.values
            .filter { it.counts || it.mood != null || it.ifThen.isNotBlank() }
            .filter { q.isEmpty() || it.searchText().contains(q) }
            .sortedByDescending { it.day }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
          Column {
            Text("Journal", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search notes and evidence") },
                singleLine = true,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth(),
            )
          }
        }
        if (entries.isEmpty()) {
            item {
                Muted(if (query.isBlank()) "Your daily notes and evidence log appear here. Write one line each evening." else "Nothing matches “$query”.")
            }
        }
        items(entries, key = { it.day }) { d ->
            val done = ui.s.taskLogs.filter { it.day == d.day && it.status == TaskStatus.DONE }
                .mapNotNull { ui.tasksById[it.taskId]?.title }.distinct()
            Panel(onClick = { onOpenDay(d.day) }) {
                MonoText(Fmt.logTitle(d.logNumber, d.day, d.finishedAt), style = LabType.mono, color = lab.accent)
                val measures = listOfNotNull(
                    d.mood?.let { "mood $it/5" }, d.energy?.let { "energy $it/5" }, d.sleepHours?.let { "sleep $it h" },
                ).joinToString(" · ")
                if (measures.isNotEmpty()) MonoText(measures, style = LabType.monoSmall, color = lab.muted)
                if (d.note.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(d.note, style = MaterialTheme.typography.bodyMedium)
                }
                if (d.hasEvidence) {
                    Spacer(Modifier.height(6.dp))
                    Label("Evidence")
                    if (d.evModule.isNotBlank()) KeyValue("Module", d.evModule)
                    if (d.evProblem.isNotBlank()) KeyValue("Problem", d.evProblem)
                    if (d.evAction.isNotBlank()) KeyValue("Action", d.evAction)
                    if (d.evResult.isNotBlank()) KeyValue("Result", d.evResult)
                    if (d.evLearning.isNotBlank()) KeyValue("Learning", d.evLearning)
                }
                if (done.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    done.forEach { MonoText("✓ $it", style = LabType.monoSmall, color = lab.muted, maxLines = 1) }
                }
            }
        }
    }
}

/** Edit one day's note and evidence line. */
@Composable
fun DayScreen(ui: UiState, vm: LabViewModel, day: Long, onBack: () -> Unit) {
    val lab = LocalLab.current
    val log = ui.s.dayLogs[day] ?: DayLog(day)
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { BackBar(Fmt.logTitle(log.logNumber, day, log.finishedAt), onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MonoText(Fmt.day(day), style = LabType.monoSmall, color = lab.muted)
            if (log.ifThen.isNotBlank()) KeyValue("If-then", log.ifThen)
            AutosaveField("dn-$day", log.note, "Note", { v -> vm.launch { saveDayLog(day) { it.copy(note = v) } } }, minLines = 2)
            Label("Evidence log")
            Muted("No client names or confidential numbers.")
            AutosaveField("dm-$day", log.evModule, "Module", { v -> vm.launch { saveDayLog(day) { it.copy(evModule = v) } } }, singleLine = true)
            AutosaveField("dp-$day", log.evProblem, "Problem", { v -> vm.launch { saveDayLog(day) { it.copy(evProblem = v) } } })
            AutosaveField("da-$day", log.evAction, "What I did", { v -> vm.launch { saveDayLog(day) { it.copy(evAction = v) } } })
            AutosaveField("dr-$day", log.evResult, "Result", { v -> vm.launch { saveDayLog(day) { it.copy(evResult = v) } } })
            AutosaveField("dl-$day", log.evLearning, "What I learned", { v -> vm.launch { saveDayLog(day) { it.copy(evLearning = v) } } })
        }
    }
}
