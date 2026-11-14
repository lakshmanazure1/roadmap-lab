package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Mode
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.ui.components.AutosaveField
import com.lakshman.roadmaplab.ui.components.Choice
import com.lakshman.roadmaplab.ui.components.KeyValue
import com.lakshman.roadmaplab.ui.components.LabField
import com.lakshman.roadmaplab.ui.components.Label
import com.lakshman.roadmaplab.ui.components.LabOutlinedButton
import com.lakshman.roadmaplab.ui.components.MonoText
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.components.ScaleRow
import com.lakshman.roadmaplab.ui.components.Timeline
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab
import kotlinx.coroutines.delay

@Composable
fun TodayScreen(
    ui: UiState,
    vm: LabViewModel,
    padding: PaddingValues,
    listState: LazyListState,
    onOpenTask: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onReview: () -> Unit,
    onBackup: () -> Unit,
) {
    val lab = LocalLab.current
    val today = ui.today
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    MonoText(Fmt.day(today), style = LabType.monoSmall, color = lab.muted)
                    Text("Today", style = MaterialTheme.typography.headlineSmall, color = lab.text)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Label("Streak")
                    MonoText("${ui.streak} d", style = LabType.number, color = lab.accent)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = lab.muted)
                }
            }
        }

        if (ui.restart) {
            item {
                Panel {
                    Label("Restart", color = lab.accent)
                    Spacer(Modifier.height(6.dp))
                    Text("Welcome back. Here are just your next priorities.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Muted("The rest of the plan has quietly moved forward. No catching up needed — start with one task.")
                    Spacer(Modifier.height(10.dp))
                    LabOutlinedButton("Got it", onClick = { vm.updateSettings { it.copy(restartDismissedDay = today) } })
                }
            }
        }

        if (ui.lowMood) {
            item {
                Panel {
                    Label("A note for you")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your mood has been low on most of the last 10 days. Talking helps — call family or a friend today. " +
                            "Tele-MANAS (14416) is free and confidential, any time.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Muted("This is not a diagnosis, and nothing is shared with anyone.")
                    Spacer(Modifier.height(10.dp))
                    LabOutlinedButton("Hide for a week", onClick = { vm.updateSettings { it.copy(lowMoodHiddenUntil = today + 7) } })
                }
            }
        }

        item {
            Column {
                Text(ui.motivation.text, style = MaterialTheme.typography.bodyLarge, color = lab.text, fontStyle = FontStyle.Italic)
                ui.motivation.source?.let { Muted("— $it") }
            }
        }

        item { YouAreHerePanel(ui, vm) }
        item { MorningPanel(ui, vm) }
        item { TasksPanel(ui, vm, onOpenTask) }
        item { IfThenPanel(ui, vm) }
        item { EveningPanel(ui, vm, onReview) }

        if (ui.backupDue) {
            item {
                Panel {
                    Label("Weekly backup")
                    Spacer(Modifier.height(6.dp))
                    Text("Save a dated backup to your Google Drive.", style = MaterialTheme.typography.bodyMedium)
                    Muted("Needs internet only for this moment. In the next screen choose Drive and your backup folder.")
                    Spacer(Modifier.height(10.dp))
                    LabOutlinedButton("Back up to Drive", onClick = onBackup, color = lab.accent)
                }
            }
        }
    }
}

@Composable
fun YouAreHerePanel(ui: UiState, vm: LabViewModel) {
    val lab = LocalLab.current
    val p = ui.progress
    val r = ui.s.roadmap ?: return
    Panel(grid = true) {
        Row(verticalAlignment = Alignment.Bottom) {
            MonoText(Fmt.pct(p.percent), style = LabType.bigNumber, color = lab.accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.padding(bottom = 6.dp)) {
                Label("Overall progress")
                MonoText(p.label, style = LabType.mono, color = if (p.gapDays < -7) lab.amber else lab.muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Timeline(r.timelineStart, r.timelineEnd, ui.today, p.youDay, r.milestones)
        val next = p.nextMilestone
        val days = p.daysToMilestone
        if (next != null && days != null) {
            Spacer(Modifier.height(6.dp))
            MonoText("Next: ${next.title} · ${Fmt.relDays(days)}", style = LabType.monoSmall, color = lab.text, maxLines = 2)
        }
        if (p.offerReducedPlan && !ui.s.settings.reducedPlan) {
            Spacer(Modifier.height(10.dp))
            Text("More than two weeks behind. Your roadmap's rule: switch to the reduced plan, don't quit.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            LabOutlinedButton("Use the reduced plan", onClick = {
                vm.updateSettings { it.copy(reducedPlan = true) }
                vm.launch { replanToday() }
            }, color = lab.accent)
        }
    }
}

@Composable
private fun MorningPanel(ui: UiState, vm: LabViewModel) {
    val lab = LocalLab.current
    val log = ui.todayLog
    val day = ui.today
    Panel {
        Label("Morning check")
        Spacer(Modifier.height(10.dp))
        ScaleRow("Mood", log.mood, { n -> vm.launch { saveDayLog(day) { it.copy(mood = n) } } }, "low", "great")
        Spacer(Modifier.height(12.dp))
        ScaleRow("Energy", log.energy, { n -> vm.launch { saveDayLog(day) { it.copy(energy = n) } } }, "drained", "full")
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label("Sleep (optional)", Modifier.weight(1f))
            IconButton(onClick = {
                val v = ((log.sleepHours ?: 7f) - 0.5f).coerceAtLeast(0f)
                vm.launch { saveDayLog(day) { it.copy(sleepHours = v) } }
            }) { Text("−", style = LabType.number) }
            MonoText(log.sleepHours?.let { "${it} h" } ?: "– h", style = LabType.number)
            IconButton(onClick = {
                val v = ((log.sleepHours ?: 7f) + 0.5f).coerceAtMost(14f)
                vm.launch { saveDayLog(day) { it.copy(sleepHours = v) } }
            }) { Text("+", style = LabType.number) }
        }
        if (ui.lowEnergy) {
            Spacer(Modifier.height(6.dp))
            Text("Low energy: each task shows its 15-minute version. It still counts for the streak.",
                style = MaterialTheme.typography.bodySmall, color = lab.accent)
        }
        Spacer(Modifier.height(4.dp))
        Muted("This is to adjust the day, not to judge it.")
    }
}

@Composable
private fun TasksPanel(ui: UiState, vm: LabViewModel, onOpenTask: (String) -> Unit) {
    val lab = LocalLab.current
    val mode = ui.s.settings.mode
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label("Today's tasks", Modifier.weight(1f))
            MonoText("${ui.todayTasks.size} / ${Planner.MAX_TASKS_PER_DAY} · ${mode.label}", style = LabType.monoSmall, color = lab.muted)
        }
        if (ui.todayTasks.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Nothing planned for today.", style = MaterialTheme.typography.bodyMedium)
        }
        ui.todayTasks.forEachIndexed { i, tt ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = lab.grid)
            else Spacer(Modifier.height(10.dp))
            TaskCard(tt, mode, ui.lowEnergy, vm, onOpenTask)
        }
        val pull = ui.canPull
        if (pull != null) {
            Spacer(Modifier.height(12.dp))
            Muted("All done for today. Want to get ahead?")
            Spacer(Modifier.height(6.dp))
            LabOutlinedButton("Do one more: ${pull.title}", onClick = { vm.launch { pullIntoToday(pull.id) } }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TaskCard(tt: TodayTask, mode: Mode, lowEnergy: Boolean, vm: LabViewModel, onOpenTask: (String) -> Unit) {
    val lab = LocalLab.current
    val t = tt.task
    val planned = if (lowEnergy) t.minMinimum else t.minutes(mode)
    var expanded by remember(t.id) { mutableStateOf(false) }
    var minutes by remember(t.id, planned) { mutableIntStateOf(planned) }
    var editing by remember(t.id) { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(t.track, Modifier.weight(1f))
            MonoText(if (lowEnergy) "$planned min · short version" else Fmt.min(planned), style = LabType.monoSmall, color = lab.muted)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable { expanded = !expanded }) {
            Text(t.title, style = MaterialTheme.typography.titleMedium, color = lab.text, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Hide details" else "Show details", tint = lab.muted)
        }
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            KeyValue("Learn", t.learn)
            KeyValue("Apply", t.apply)
            KeyValue("Done when", t.doneWhen)
            TextButton(onClick = { onOpenTask(t.id) }) { Text("Task details and history") }
        }
        Spacer(Modifier.height(8.dp))
        val status = tt.todayStatus
        if (status != null && !editing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = if (status == TaskStatus.SKIPPED) lab.muted else lab.accent
                MonoText(
                    when (status) {
                        TaskStatus.DONE -> "✓ Done · ${Fmt.min(tt.todayMinutes)}"
                        TaskStatus.PARTLY -> "◐ Partly done · ${Fmt.min(tt.todayMinutes)}"
                        TaskStatus.SKIPPED -> "– Skipped · moves to the next free day"
                    },
                    style = LabType.mono, color = color, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { editing = true }) { Text("Change") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("Minutes spent", Modifier.weight(1f))
                IconButton(onClick = { minutes = (minutes - 5).coerceAtLeast(0) }) { Text("−", style = LabType.number) }
                MonoText(Fmt.min(minutes), style = LabType.number)
                IconButton(onClick = { minutes = (minutes + 5).coerceAtMost(600) }) { Text("+", style = LabType.number) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Done", false, onClick = {
                    vm.launch { logTask(t.id, TaskStatus.DONE, minutes, lowEnergy) }; editing = false
                }, modifier = Modifier.weight(1f))
                Choice("Partly", false, onClick = {
                    vm.launch { logTask(t.id, TaskStatus.PARTLY, minutes, lowEnergy) }; editing = false
                }, modifier = Modifier.weight(1f))
                Choice("Skip", false, onClick = {
                    vm.launch { logTask(t.id, TaskStatus.SKIPPED, 0, false) }; editing = false
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}

private val CUES = listOf("After dinner", "After my morning tea", "When I'm back in my room", "After my last call")
private val PLACES = listOf("at my desk", "at the table", "in my room")

@Composable
private fun IfThenPanel(ui: UiState, vm: LabViewModel) {
    val day = ui.today
    val firstOpen = ui.todayTasks.firstOrNull { it.todayStatus == null }?.task?.title
    val what = firstOpen?.substringBefore(":")?.trim() ?: "5 review cards"
    var cue by remember(day) { mutableStateOf(CUES[0]) }
    var place by remember(day) { mutableStateOf(PLACES[0]) }
    var text by remember(day) { mutableStateOf(ui.todayLog.ifThen) }
    var dirty by remember(day) { mutableStateOf(false) }
    LaunchedEffect(day, text) {
        if (dirty) {
            delay(500)
            val value = text
            vm.launch { saveDayLog(day) { it.copy(ifThen = value) } }
            dirty = false
        }
    }
    Panel {
        Label("If-then plan")
        Spacer(Modifier.height(4.dp))
        Muted("Decide when and where. Pick a moment and a place, or write your own.")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CUES.forEach { c -> Choice(c, c == cue, onClick = { cue = c }) }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PLACES.forEach { p -> Choice(p, p == place, onClick = { place = p }) }
        }
        Spacer(Modifier.height(8.dp))
        LabOutlinedButton("Use: $cue, $place, I will do $what.", onClick = {
            text = "$cue, $place, I will do $what."
            dirty = true
        }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        LabField(value = text, onChange = { text = it; dirty = true }, label = "Your plan for today")
    }
}

@Composable
private fun EveningPanel(ui: UiState, vm: LabViewModel, onReview: () -> Unit) {
    val lab = LocalLab.current
    val day = ui.today
    val log = ui.todayLog
    val toReview = ui.reviewQueue.size
    Panel {
        Label("Evening log")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Review", style = MaterialTheme.typography.titleSmall)
                MonoText(
                    if (toReview == 0) "No cards due · ${ui.reviewedToday} reviewed today"
                    else "$toReview due · about ${minOf(toReview, 5)} to start",
                    style = LabType.monoSmall, color = lab.muted,
                )
            }
            LabOutlinedButton("Start", onClick = onReview, enabled = toReview > 0, color = lab.accent)
        }
        Spacer(Modifier.height(12.dp))
        AutosaveField(key = "note-$day", initial = log.note, label = "One line of notes",
            onSave = { text -> vm.launch { saveDayLog(day) { it.copy(note = text) } } })
        if (ui.s.settings.showEvidenceLog) {
            Spacer(Modifier.height(12.dp))
            Label("Evidence log")
            Muted("One line a day. No client names or confidential numbers.")
            Spacer(Modifier.height(6.dp))
            AutosaveField("evm-$day", log.evModule, "Module", { v -> vm.launch { saveDayLog(day) { it.copy(evModule = v) } } }, singleLine = true)
            Spacer(Modifier.height(6.dp))
            AutosaveField("evp-$day", log.evProblem, "Problem", { v -> vm.launch { saveDayLog(day) { it.copy(evProblem = v) } } })
            Spacer(Modifier.height(6.dp))
            AutosaveField("eva-$day", log.evAction, "What I did", { v -> vm.launch { saveDayLog(day) { it.copy(evAction = v) } } })
            Spacer(Modifier.height(6.dp))
            AutosaveField("evr-$day", log.evResult, "Result", { v -> vm.launch { saveDayLog(day) { it.copy(evResult = v) } } })
            Spacer(Modifier.height(6.dp))
            AutosaveField("evl-$day", log.evLearning, "What I learned", { v -> vm.launch { saveDayLog(day) { it.copy(evLearning = v) } } })
        }
        Spacer(Modifier.height(12.dp))
        if (!log.eveningDone) {
            Button(
                onClick = { vm.launch { finishEvening(day) } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(4.dp),
            ) { Text("Finish evening log") }
        } else {
            MonoText(Fmt.logTitle(log.logNumber, day, log.finishedAt), style = LabType.mono, color = lab.accent)
            Spacer(Modifier.height(6.dp))
            MonoText("Progress ${Fmt.pct(ui.progress.percent)} · ${ui.progress.label} · streak ${ui.streak} d",
                style = LabType.monoSmall, color = lab.text)
            if (ui.tomorrow.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Label("Tomorrow")
                ui.tomorrow.forEach { t -> Text("· ${t.title}", style = MaterialTheme.typography.bodyMedium) }
            }
            TextButton(onClick = { vm.launch { reopenEvening(day) } }) { Text("Edit again") }
        }
    }
}
