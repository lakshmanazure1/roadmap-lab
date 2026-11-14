package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Mode
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.ui.components.AutosaveField
import com.lakshman.roadmaplab.ui.components.Choice
import com.lakshman.roadmaplab.ui.components.KeyValue
import com.lakshman.roadmaplab.ui.components.LabField
import com.lakshman.roadmaplab.ui.components.LabOutlinedButton
import com.lakshman.roadmaplab.ui.components.Label
import com.lakshman.roadmaplab.ui.components.MonoText
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.components.ScaleRow
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Timeline
import com.lakshman.roadmaplab.ui.components.TrackTag
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    MonoText(Fmt.day(today), style = LabType.monoSmall, color = lab.muted)
                    Spacer(Modifier.height(2.dp))
                    Text("Today", style = MaterialTheme.typography.headlineMedium, color = lab.text)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = lab.accent.copy(alpha = 0.15f)) {
                    MonoText("Streak ${ui.streak} d", style = LabType.number, color = lab.accent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = lab.muted)
                }
            }
        }

        if (ui.restart) {
            item {
                Panel(stripe = lab.accent) {
                    Text("Welcome back", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Muted("Here are just your next priorities. The rest of the plan has quietly moved forward — no catching up needed.",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))
                    LabOutlinedButton("Got it", onClick = { vm.updateSettings { it.copy(restartDismissedDay = today) } })
                }
            }
        }

        if (ui.lowMood) {
            item {
                Panel(stripe = lab.communication) {
                    Text("A note for you", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your mood has been low on most of the last 10 days. Talking helps — call family or a friend today. " +
                            "Tele-MANAS (14416) is free and confidential, any time.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Muted("This is not a diagnosis, and nothing is shared with anyone.")
                    Spacer(Modifier.height(14.dp))
                    LabOutlinedButton("Hide for a week", onClick = { vm.updateSettings { it.copy(lowMoodHiddenUntil = today + 7) } })
                }
            }
        }

        item {
            Panel(stripe = lab.accent) {
                Text(ui.motivation.text, style = MaterialTheme.typography.bodyLarge, color = lab.text, fontStyle = FontStyle.Italic)
                ui.motivation.source?.let {
                    Spacer(Modifier.height(6.dp))
                    Muted("— $it")
                }
            }
        }

        item { YouAreHerePanel(ui, vm) }

        item { SectionTitle("Morning", lab.morning, "about 1 min") }
        item { MorningPanel(ui, vm) }

        item {
            SectionTitle("Today's tasks", lab.ai, "${ui.todayTasks.size} / ${Planner.MAX_TASKS_PER_DAY} · ${ui.s.settings.mode.label}")
        }
        if (ui.todayTasks.isEmpty()) {
            item { Panel { Text("Nothing planned for today. Rest is part of the plan.", style = MaterialTheme.typography.bodyLarge) } }
        }
        ui.todayTasks.forEach { tt ->
            item(key = "task-${tt.task.id}") { TaskCard(tt, ui.s.settings.mode, ui.lowEnergy, vm, onOpenTask) }
        }
        ui.canPull?.let { pull ->
            item {
                Panel {
                    Muted("All done for today. Want to get ahead?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    LabOutlinedButton("Do one more: ${pull.title}", onClick = { vm.launch { pullIntoToday(pull.id) } },
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item { IfThenPanel(ui, vm) }

        item { SectionTitle("Evening", lab.evening, "2–3 min") }
        item { EveningPanel(ui, vm, onReview) }

        if (ui.backupDue) {
            item {
                Panel(stripe = lab.networking) {
                    Text("Weekly backup", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Muted("Save a dated copy to your Google Drive. Needs internet only for this moment.",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))
                    LabOutlinedButton("Back up to Drive", onClick = onBackup, color = lab.networking)
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
        Label("Overall progress")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            MonoText(Fmt.pct(p.percent), style = LabType.bigNumber, color = lab.accent)
            Spacer(Modifier.width(14.dp))
            MonoText(p.label, style = LabType.number, color = if (p.gapDays < -7) lab.amber else lab.text,
                modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(16.dp))
        Timeline(r.timelineStart, r.timelineEnd, ui.today, p.youDay, r.milestones)
        val next = p.nextMilestone
        val days = p.daysToMilestone
        if (next != null && days != null) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(lab.track(next.track)))
                Spacer(Modifier.width(8.dp))
                Text("Next: ${next.title}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                MonoText(Fmt.relDays(days), style = LabType.monoSmall, color = lab.accent)
            }
        }
        if (p.offerReducedPlan && !ui.s.settings.reducedPlan) {
            Spacer(Modifier.height(14.dp))
            Text("More than two weeks behind. Your roadmap's rule: switch to the reduced plan, don't quit.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
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
    val filled = log.mood != null && log.energy != null
    var editing by remember(day) { mutableStateOf(false) }
    Panel(stripe = lab.morning) {
        if (filled && !editing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label("Checked in")
                    Spacer(Modifier.height(4.dp))
                    MonoText(
                        "Mood ${log.mood}/5 · Energy ${log.energy}/5" + (log.sleepHours?.let { " · Sleep $it h" } ?: ""),
                        style = LabType.number,
                    )
                }
                TextButton(onClick = { editing = true }) { Text("Edit") }
            }
        } else {
            ScaleRow("Mood", log.mood, { n -> vm.launch { saveDayLog(day) { it.copy(mood = n) } } }, "low", "great")
            Spacer(Modifier.height(20.dp))
            ScaleRow("Energy", log.energy, { n -> vm.launch { saveDayLog(day) { it.copy(energy = n) } } }, "drained", "full")
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("Sleep (optional)", Modifier.weight(1f))
                IconButton(onClick = {
                    val v = ((log.sleepHours ?: 7f) - 0.5f).coerceAtLeast(0f)
                    vm.launch { saveDayLog(day) { it.copy(sleepHours = v) } }
                }) { Text("−", style = LabType.number) }
                MonoText(log.sleepHours?.let { "$it h" } ?: "– h", style = LabType.number)
                IconButton(onClick = {
                    val v = ((log.sleepHours ?: 7f) + 0.5f).coerceAtMost(14f)
                    vm.launch { saveDayLog(day) { it.copy(sleepHours = v) } }
                }) { Text("+", style = LabType.number) }
            }
            if (filled) {
                Spacer(Modifier.height(8.dp))
                LabOutlinedButton("Done", onClick = { editing = false }, modifier = Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(4.dp))
                Muted("This is to adjust the day, not to judge it.")
            }
        }
        if (ui.lowEnergy) {
            Spacer(Modifier.height(10.dp))
            Text("Low energy: each task shows its 15-minute version. It still counts for the streak.",
                style = MaterialTheme.typography.bodyMedium, color = lab.accent)
        }
    }
}

@Composable
private fun TaskCard(tt: TodayTask, mode: Mode, lowEnergy: Boolean, vm: LabViewModel, onOpenTask: (String) -> Unit) {
    val lab = LocalLab.current
    val t = tt.task
    val trackColor = lab.track(t.track)
    val planned = if (lowEnergy) t.minMinimum else t.minutes(mode)
    var expanded by remember(t.id) { mutableStateOf(false) }
    var minutes by remember(t.id, planned) { mutableIntStateOf(planned) }
    var editing by remember(t.id) { mutableStateOf(false) }
    val status = tt.todayStatus

    Panel(stripe = trackColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackTag(t.track, Modifier.weight(1f))
            MonoText(if (lowEnergy) "$planned min · short" else Fmt.min(planned), style = LabType.monoSmall, color = lab.muted)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable { expanded = !expanded }) {
            Text(t.title, style = MaterialTheme.typography.titleMedium, color = lab.text, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Hide details" else "Show details", tint = lab.muted)
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            KeyValue("Learn", t.learn)
            Spacer(Modifier.height(4.dp))
            KeyValue("Apply", t.apply)
            Spacer(Modifier.height(4.dp))
            KeyValue("Done when", t.doneWhen)
            TextButton(onClick = { onOpenTask(t.id) }) { Text("Task details and history") }
        }
        Spacer(Modifier.height(14.dp))
        if (status != null && !editing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = when (status) {
                    TaskStatus.DONE -> lab.done
                    TaskStatus.PARTLY -> lab.partly
                    TaskStatus.SKIPPED -> lab.muted
                }
                Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.18f), modifier = Modifier.weight(1f, fill = false)) {
                    MonoText(
                        when (status) {
                            TaskStatus.DONE -> "✓ Done · ${Fmt.min(tt.todayMinutes)}"
                            TaskStatus.PARTLY -> "◐ Partly · ${Fmt.min(tt.todayMinutes)}"
                            TaskStatus.SKIPPED -> "– Skipped · moves forward"
                        },
                        style = LabType.mono, color = lab.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { editing = true }) { Text("Change") }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("Minutes spent", Modifier.weight(1f))
                IconButton(onClick = { minutes = (minutes - 5).coerceAtLeast(0) }) { Text("−", style = LabType.number) }
                MonoText(Fmt.min(minutes), style = LabType.number)
                IconButton(onClick = { minutes = (minutes + 5).coerceAtMost(600) }) { Text("+", style = LabType.number) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Choice("Done", false, onClick = {
                    vm.launch { logTask(t.id, TaskStatus.DONE, minutes, lowEnergy) }; editing = false
                }, modifier = Modifier.weight(1f), color = lab.done)
                Choice("Partly", false, onClick = {
                    vm.launch { logTask(t.id, TaskStatus.PARTLY, minutes, lowEnergy) }; editing = false
                }, modifier = Modifier.weight(1f), color = lab.partly)
                Choice("Skip", false, onClick = {
                    vm.launch { logTask(t.id, TaskStatus.SKIPPED, 0, false) }; editing = false
                }, modifier = Modifier.weight(1f), color = lab.muted)
            }
        }
    }
}

private val CUES = listOf("After dinner", "After my morning tea", "When I'm back in my room", "After my last call")
private val PLACES = listOf("at my desk", "at the table", "in my room")

@Composable
private fun IfThenPanel(ui: UiState, vm: LabViewModel) {
    val lab = LocalLab.current
    val day = ui.today
    val firstOpen = ui.todayTasks.firstOrNull { it.todayStatus == null }?.task?.title
    val what = firstOpen?.substringBefore(":")?.trim() ?: "5 review cards"
    var cue by remember(day) { mutableStateOf(CUES[0]) }
    var place by remember(day) { mutableStateOf(PLACES[0]) }
    var text by remember(day) { mutableStateOf(ui.todayLog.ifThen) }
    var dirty by remember(day) { mutableStateOf(false) }
    var editing by remember(day) { mutableStateOf(ui.todayLog.ifThen.isBlank()) }
    LaunchedEffect(day, text) {
        if (dirty) {
            delay(500)
            val value = text
            vm.launch { saveDayLog(day) { it.copy(ifThen = value) } }
            dirty = false
        }
    }
    Panel(stripe = lab.accent) {
        Label("If-then plan")
        Spacer(Modifier.height(8.dp))
        if (!editing && text.isNotBlank()) {
            Text(text, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            TextButton(onClick = { editing = true }) { Text("Change") }
        } else {
            Muted("Decide when and where. Pick a moment and a place.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CUES.forEach { c -> Choice(c, c == cue, onClick = { cue = c }) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PLACES.forEach { p -> Choice(p, p == place, onClick = { place = p }) }
            }
            Spacer(Modifier.height(12.dp))
            Text("“$cue, $place, I will do $what.”", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    text = "$cue, $place, I will do $what."
                    dirty = true
                    editing = false
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(8.dp),
            ) { Text("Set this plan") }
            Spacer(Modifier.height(10.dp))
            LabField(value = text, onChange = { text = it; dirty = true }, label = "Or write your own")
        }
    }
}

@Composable
private fun EveningPanel(ui: UiState, vm: LabViewModel, onReview: () -> Unit) {
    val lab = LocalLab.current
    val day = ui.today
    val log = ui.todayLog
    val toReview = ui.reviewQueue.size
    var showEvidence by remember(day) { mutableStateOf(log.hasEvidence) }

    // 1. Review
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Panel(stripe = lab.ai) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("1 · Review questions", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    MonoText(
                        if (toReview == 0) "No cards due · ${ui.reviewedToday} reviewed today"
                        else "$toReview due · start with ${minOf(toReview, 5)}",
                        style = LabType.monoSmall, color = lab.muted,
                    )
                }
                Button(onClick = onReview, enabled = toReview > 0, shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.heightIn(min = 48.dp)) { Text("Start") }
            }
        }

        // 2. Note + evidence
        Panel(stripe = lab.evening) {
            Text("2 · One line of notes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            AutosaveField(key = "note-$day", initial = log.note, label = "What happened today?",
                onSave = { text -> vm.launch { saveDayLog(day) { it.copy(note = text) } } })
            if (ui.s.settings.showEvidenceLog) {
                Spacer(Modifier.height(10.dp))
                if (!showEvidence) {
                    TextButton(onClick = { showEvidence = true }) { Text("+ Add evidence-log line") }
                } else {
                    Spacer(Modifier.height(6.dp))
                    Label("Evidence log")
                    Spacer(Modifier.height(2.dp))
                    Muted("No client names or confidential numbers.")
                    Spacer(Modifier.height(10.dp))
                    AutosaveField("evm-$day", log.evModule, "Module", { v -> vm.launch { saveDayLog(day) { it.copy(evModule = v) } } }, singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    AutosaveField("evp-$day", log.evProblem, "Problem", { v -> vm.launch { saveDayLog(day) { it.copy(evProblem = v) } } })
                    Spacer(Modifier.height(10.dp))
                    AutosaveField("eva-$day", log.evAction, "What I did", { v -> vm.launch { saveDayLog(day) { it.copy(evAction = v) } } })
                    Spacer(Modifier.height(10.dp))
                    AutosaveField("evr-$day", log.evResult, "Result", { v -> vm.launch { saveDayLog(day) { it.copy(evResult = v) } } })
                    Spacer(Modifier.height(10.dp))
                    AutosaveField("evl-$day", log.evLearning, "What I learned", { v -> vm.launch { saveDayLog(day) { it.copy(evLearning = v) } } })
                }
            }
        }

        // 3. Finish
        Panel(stripe = lab.done) {
            if (!log.eveningDone) {
                Text("3 · Finish the day", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.launch { finishEvening(day) } },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = lab.done, contentColor = lab.onAccent),
                ) { Text("Finish evening log") }
            } else {
                MonoText(Fmt.logTitle(log.logNumber, day, log.finishedAt), style = LabType.number, color = lab.done)
                Spacer(Modifier.height(10.dp))
                MonoText("Progress ${Fmt.pct(ui.progress.percent)} · ${ui.progress.label}", style = LabType.mono)
                MonoText("Streak ${ui.streak} d", style = LabType.mono)
                if (ui.tomorrow.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Label("Tomorrow")
                    ui.tomorrow.forEach { t ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(lab.track(t.track)))
                            Spacer(Modifier.width(10.dp))
                            Text(t.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { vm.launch { reopenEvening(day) } }) { Text("Edit again") }
            }
        }
    }
}
