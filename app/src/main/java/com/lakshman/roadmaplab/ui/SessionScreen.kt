package com.lakshman.roadmaplab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.ui.components.AutosaveField
import com.lakshman.roadmaplab.ui.components.KeyValue
import com.lakshman.roadmaplab.ui.components.Label
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.Panel
import com.lakshman.roadmaplab.ui.theme.LabType
import com.lakshman.roadmaplab.ui.theme.LocalLab

private sealed interface Step {
    data object Intro : Step
    data class DoTask(val taskId: String) : Step
    data class Question(val cardId: String, val n: Int, val of: Int) : Step
    data object Note : Step
    data object Complete : Step
}

/** The day as one card at a time: tasks → a few questions → one line of notes → done. */
@Composable
fun SessionScreen(ui: UiState, vm: LabViewModel, onClose: () -> Unit) {
    val lab = LocalLab.current
    // Fix the steps when the session starts so the list doesn't shift while you answer.
    val steps = remember {
        val list = ArrayList<Step>()
        list.add(Step.Intro)
        ui.todayTasks.filter { it.todayStatus == null || it.todayStatus == TaskStatus.SKIPPED }
            .forEach { list.add(Step.DoTask(it.task.id)) }
        val qs = ui.reviewQueue.take(5)
        qs.forEachIndexed { i, s -> list.add(Step.Question(s.cardId, i + 1, qs.size)) }
        list.add(Step.Note)
        list.add(Step.Complete)
        list.toList()
    }
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    fun next() { if (index < steps.lastIndex) index++ }

    BackHandler { onClose() }

    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Top bar: close + progress
            if (step != Step.Complete) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = lab.muted) }
                    val f = index.toFloat() / steps.lastIndex.coerceAtLeast(1)
                    Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(50)).background(Color(0xFFE3E8EC))) {
                        Box(Modifier.fillMaxWidth(f.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(lab.done))
                    }
                    Spacer(Modifier.width(16.dp))
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                key(index) {
                    when (step) {
                        Step.Intro -> IntroStep(ui, steps, onGo = { next() })
                        is Step.DoTask -> TaskStep(ui, vm, step.taskId, onNext = { next() })
                        is Step.Question -> {
                            val card = ui.s.cards[step.cardId]
                            val state = ui.s.cardStates.firstOrNull { it.cardId == step.cardId }
                            if (card == null || state == null) {
                                LaunchedEffect(Unit) { next() }
                            } else {
                                Text("Quick recall", style = MaterialTheme.typography.headlineSmall)
                                Muted("Try to answer before you look. It's what makes it stick.")
                                CardView(card, state, step.n - 1, step.of) { rating, asMcq ->
                                    if (rating != Rating.AGAIN) correct++
                                    vm.launch { review(card.id, rating, asMcq) }
                                    next()
                                }
                            }
                        }
                        Step.Note -> NoteStep(ui, vm, onFinish = {
                            vm.launch { finishEvening(ui.today) }
                            next()
                        })
                        Step.Complete -> CompleteStep(ui, correct, steps.count { it is Step.Question }, onClose)
                    }
                }
            }
        }
    }
}

@Composable
private fun BigButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
    ) { Text(text, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp) }
}

@Composable
private fun IntroStep(ui: UiState, steps: List<Step>, onGo: () -> Unit) {
    val lab = LocalLab.current
    val tasks = steps.filterIsInstance<Step.DoTask>().mapNotNull { ui.tasksById[it.taskId] }
    val qs = steps.count { it is Step.Question }
    Spacer(Modifier.height(12.dp))
    Text("Today", style = LabType.greeting.copy(fontSize = 34.sp), color = lab.text)
    Text(ui.motivation.text, style = MaterialTheme.typography.bodyLarge, color = lab.muted)
    Spacer(Modifier.height(4.dp))
    Panel {
        tasks.forEachIndexed { i, t ->
            if (i > 0) Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(lab.track(t.track)))
                Spacer(Modifier.width(12.dp))
                Text(t.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Muted(Fmt.min(t.minutes(ui.s.settings.mode)))
            }
        }
        if (tasks.isNotEmpty()) Spacer(Modifier.height(14.dp))
        if (qs > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(lab.ai))
                Spacer(Modifier.width(12.dp))
                Text("$qs quick questions", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Muted("~${qs} min")
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(lab.evening))
            Spacer(Modifier.width(12.dp))
            Text("One line of notes", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Muted("1 min")
        }
    }
    Spacer(Modifier.height(8.dp))
    BigButton("LET'S GO", lab.text, onGo)
}

@Composable
private fun TaskStep(ui: UiState, vm: LabViewModel, taskId: String, onNext: () -> Unit) {
    val lab = LocalLab.current
    val t = ui.tasksById[taskId]
    if (t == null) {
        LaunchedEffect(Unit) { onNext() }
        return
    }
    val c = lab.track(t.track)
    var short by remember { mutableStateOf(false) }
    val minutes = if (short) t.minMinimum else t.minutes(ui.s.settings.mode)
    Spacer(Modifier.height(8.dp))
    Surface(shape = RoundedCornerShape(50), color = c) {
        Text(t.track.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White, letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
    }
    Text(t.title, style = MaterialTheme.typography.headlineSmall, color = lab.text)
    Muted("${Fmt.min(minutes)}${if (short) " · short version" else ""}", style = MaterialTheme.typography.bodyMedium)
    Panel {
        KeyValue("Learn", t.learn)
        Spacer(Modifier.height(10.dp))
        KeyValue("Apply", t.apply)
        Spacer(Modifier.height(10.dp))
        KeyValue("Done when", t.doneWhen)
    }
    TextButton(onClick = { short = !short }) {
        Text(if (short) "Back to the full version" else "Short on time? Do the 15-minute version")
    }
    BigButton("I DID IT  ✓", lab.done, onClick = {
        vm.launch { logTask(t.id, TaskStatus.DONE, minutes, short) }
        onNext()
    })
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { vm.launch { logTask(t.id, TaskStatus.PARTLY, minutes / 2, short) }; onNext() },
            modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50),
        ) { Text("Did part of it") }
        OutlinedButton(
            onClick = { vm.launch { logTask(t.id, TaskStatus.SKIPPED, 0, false) }; onNext() },
            modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(50),
        ) { Text("Not today") }
    }
    TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("Haven't started yet — come back later", color = lab.muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NoteStep(ui: UiState, vm: LabViewModel, onFinish: () -> Unit) {
    val lab = LocalLab.current
    val day = ui.today
    val log = ui.todayLog
    var evidence by remember { mutableStateOf(log.hasEvidence) }
    Spacer(Modifier.height(8.dp))
    Text("One line for today", style = MaterialTheme.typography.headlineSmall, color = lab.text)
    Muted("What did you learn or build? One sentence is enough.", style = MaterialTheme.typography.bodyMedium)
    AutosaveField(key = "s-note-$day", initial = log.note, label = "Today I…", minLines = 3,
        onSave = { v -> vm.launch { saveDayLog(day) { it.copy(note = v) } } })
    if (ui.s.settings.showEvidenceLog) {
        if (!evidence) {
            TextButton(onClick = { evidence = true }) { Text("+ Add a work story for interviews (evidence log)") }
        } else {
            Label("Evidence log · no client names or confidential numbers")
            AutosaveField("s-evm-$day", log.evModule, "Module", { v -> vm.launch { saveDayLog(day) { it.copy(evModule = v) } } }, singleLine = true)
            AutosaveField("s-evp-$day", log.evProblem, "Problem", { v -> vm.launch { saveDayLog(day) { it.copy(evProblem = v) } } })
            AutosaveField("s-eva-$day", log.evAction, "What I did", { v -> vm.launch { saveDayLog(day) { it.copy(evAction = v) } } })
            AutosaveField("s-evr-$day", log.evResult, "Result", { v -> vm.launch { saveDayLog(day) { it.copy(evResult = v) } } })
            AutosaveField("s-evl-$day", log.evLearning, "What I learned", { v -> vm.launch { saveDayLog(day) { it.copy(evLearning = v) } } })
        }
    }
    Spacer(Modifier.height(4.dp))
    BigButton("FINISH THE DAY", lab.text, onFinish)
}

@Composable
private fun CompleteStep(ui: UiState, correct: Int, asked: Int, onClose: () -> Unit) {
    val lab = LocalLab.current
    Spacer(Modifier.height(40.dp))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(120.dp).clip(CircleShape).background(lab.done), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
        }
    }
    Text("Day complete!", style = LabType.greeting.copy(fontSize = 34.sp), color = lab.text,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatBox("Streak", "${ui.streak + if (ui.activeDays.contains(ui.today)) 0 else 1} days", Modifier.weight(1f), Color(0xFFFF9600))
        StatBox("Plan done", Fmt.pct(ui.progress.percent), Modifier.weight(1f), lab.ai)
        if (asked > 0) StatBox("Recall", "$correct / $asked", Modifier.weight(1f), lab.done)
    }
    val next = ui.tomorrow.firstOrNull()
    if (next != null) {
        Panel {
            Label("What's next")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(lab.track(next.track)))
                Spacer(Modifier.width(12.dp))
                Text(next.title, style = MaterialTheme.typography.titleMedium)
            }
            ui.progress.nextMilestone?.let { m ->
                Spacer(Modifier.height(8.dp))
                Muted("Then: ${m.title} · ${Fmt.relDays(ui.progress.daysToMilestone ?: 0)}")
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    BigButton("BACK TO MY PATH", lab.text, onClose)
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier, color: Color) {
    val lab = LocalLab.current
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.12f), modifier = modifier) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (label == "Streak") Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = lab.text)
            Muted(label)
        }
    }
}
