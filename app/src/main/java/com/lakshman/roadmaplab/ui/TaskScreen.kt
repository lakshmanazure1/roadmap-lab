package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.CheckSpec
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Level
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.TaskKind
import com.lakshman.roadmaplab.domain.TodayEngine
import com.lakshman.roadmaplab.ui.components.AreaLabel
import com.lakshman.roadmaplab.ui.components.Banner
import com.lakshman.roadmaplab.ui.components.Divider
import com.lakshman.roadmaplab.ui.components.Field
import com.lakshman.roadmaplab.ui.components.KeyValue
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.LevelTag
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.PrimaryButton
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SecondaryButton
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Stepper
import com.lakshman.roadmaplab.ui.components.Tag
import com.lakshman.roadmaplab.ui.components.Tone
import com.lakshman.roadmaplab.ui.components.TopBar
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type

/** One task: what to do, how you'll know it's done, its level on the 7-level scale, and its checks. */
@Composable
fun TaskScreen(ui: UiState, act: Actions, nav: Nav, id: String) {
    val p = LocalPalette.current
    val t = ui.plan.task(id) ?: return Column { TopBar("Not found", nav.back); Muted("This task isn't in the plan.", Modifier.padding(16.dp)) }
    val st = ui.states[id]
    val level = ui.level(id)
    val checks = ui.checks(id)
    var completing by remember { mutableStateOf(false) }
    val planned = TodayEngine.effectiveDay(t, ui.reducedWeeks)

    Column(Modifier.fillMaxSize()) {
        TopBar(t.id, nav.back)
        LazyColumn(contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column {
                    AreaLabel(t.area, ui.plan.area(t.area)?.name ?: t.areaLabel)
                    Text(t.title, style = Type.title, color = p.text, modifier = Modifier.padding(top = 6.dp))
                    Muted(listOf("${t.weekday} ${Days.dayMonth(t.day)}" + if (planned != t.day) " (moved to ${Days.short(planned)})" else "",
                        "Week ${t.week}", t.minutesLabel, kindLabel(t.kind)).filter { it.isNotBlank() }.joinToString(" · "),
                        Modifier.padding(top = 4.dp))
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LevelTag(level)
                        if (t.isMilestone) Tag("★ ${t.milestone}", Tone.WARNING)
                    }
                }
            }
            item {
                LabCard {
                    if (t.learn.isNotBlank()) KeyValue("Learn / practise", t.learn)
                    if (t.build.isNotBlank()) KeyValue("Build / deliver", t.build)
                    if (t.doneWhen.isNotBlank()) KeyValue("Done when", t.doneWhen)
                    if (t.evidence.isNotBlank()) KeyValue("Evidence to keep", t.evidence)
                    if (t.atWork) Muted("Done inside work: it takes no study time.", Modifier.padding(top = 4.dp))
                }
            }
            if (t.area == "accounting" || t.area == "python") item {
                Muted(if (t.area == "accounting") "Fictional data only (Ravi Traders, made-up amounts). Do the work before you look at the answer."
                else "Fake data only. Predict the output before you run anything; stuck 15 minutes? Ask for a hint, not the solution.")
            }
            item { TaskActions(ui, act, nav, id, onComplete = { completing = true }) }
            item { LevelLadder(level) }
            if (t.checks.isNotEmpty()) {
                item { SectionTitle("Check it", trailing = "completion isn't mastery") }
                item {
                    LabCard(padding = PaddingValues(0.dp)) {
                        t.checks.forEachIndexed { i, c ->
                            if (i > 0) Divider()
                            CheckRow(c, checks) { nav.open(Screen.Check(id, i)) }
                        }
                    }
                }
            }
            if (t.area == "evidence") item {
                SecondaryButton(if (t.build.contains("STAR #")) "Write today's line or the STAR story" else "Write the log line",
                    { nav.open(Screen.EvidenceEdit(null, ui.today, id, false)) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Edit)
            }
            if (checks.isNotEmpty()) {
                item { SectionTitle("History", trailing = "${checks.size} attempt${if (checks.size == 1) "" else "s"}") }
                item {
                    LabCard {
                        checks.sortedByDescending { it.createdAt }.forEach { a -> AttemptLine(a) }
                    }
                }
            }
            val events = ui.s.events.filter { it.itemId == id }.sortedByDescending { it.createdAt }
            if (events.isNotEmpty()) item {
                Muted("Changes: " + events.take(6).joinToString(" · ") { "${it.type.key} ${Days.dayMonth(it.day)}" + if (it.minutes > 0) " (${it.minutes} min)" else "" })
            }
        }
    }
    if (completing) CompleteDialog(ui, act, id) { completing = false }
}

private fun kindLabel(k: TaskKind) = when (k) {
    TaskKind.SEQUENCE -> "follows the order"
    TaskKind.CARRY -> "dated step, waits until done"
    TaskKind.DAILY -> "belongs to its day"
    TaskKind.REST -> "rest day"
}

@Composable
private fun TaskActions(ui: UiState, act: Actions, nav: Nav, id: String, onComplete: () -> Unit) {
    val t = ui.plan.task(id)!!
    val st = ui.states[id]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (st?.progress) {
            Progress.COMPLETED -> {
                Banner("Done ${st.doneDay?.let { Days.relative(it, ui.today) } ?: ""}" + (if (st.independent) " · without help" else "") +
                    (if (st.doneMinutes > 0) " · ${st.doneMinutes} min" else ""), Tone.SUCCESS)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Edit", onComplete, Modifier.weight(1f))
                    SecondaryButton("Mark not done", { toggleTask(ui, act, t, true) }, Modifier.weight(1f))
                }
            }
            Progress.SKIPPED -> {
                Banner("You let this one go. It isn't counted as done.", Tone.NEUTRAL)
                SecondaryButton("Bring it back", { act.setTask(id, EventType.RESET) }, Modifier.fillMaxWidth())
            }
            else -> {
                PrimaryButton("Mark done", onComplete, Modifier.fillMaxWidth(), icon = Icons.Outlined.Check)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (st?.progress != Progress.IN_PROGRESS) SecondaryButton("Started", { act.setTask(id, EventType.START) }, Modifier.weight(1f))
                    else SecondaryButton("Did part (15 min)", { act.setTask(id, EventType.START, 15); act.note(Note("15 minutes recorded for ${t.id}")) }, Modifier.weight(1f))
                    SecondaryButton("Not today", {
                        act.setTask(id, EventType.LATER); act.note(Note("${t.id} moved off today. It keeps its place in the order.")); nav.back()
                    }, Modifier.weight(1f))
                }
                if (t.kind != TaskKind.SEQUENCE) SecondaryButton("Let this one go", { act.setTask(id, EventType.SKIP) }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CompleteDialog(ui: UiState, act: Actions, id: String, onClose: () -> Unit) {
    val p = LocalPalette.current
    val t = ui.plan.task(id)!!
    val st = ui.states[id]
    var minutes by remember { mutableStateOf(if (st?.progress == Progress.COMPLETED) st.doneMinutes else t.minutes) }
    var alone by remember { mutableStateOf(st?.independent ?: false) }
    var note by remember { mutableStateOf("") }
    var day by remember { mutableStateOf(st?.doneDay ?: ui.today) }
    // Forgot to tick? Pick the day you really did it (up to a week back, not before your fresh start).
    val days = (0L..6L).map { ui.today - it }.filter { it >= ui.plan.firstDay || it == ui.today }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Mark ${t.id} done", style = Type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Done on (it counts for that day's streaks)", style = Type.smallStrong, color = p.text2)
                com.lakshman.roadmaplab.ui.components.ChipRow {
                    days.forEach { d ->
                        com.lakshman.roadmaplab.ui.components.Chip(when (d) { ui.today -> "Today"; ui.today - 1 -> "Yesterday"; else -> Days.short(d) },
                            day == d, { day = d })
                    }
                }
                if (t.study) Stepper(minutes, { minutes = it }, 0, 240, 5, "min", "Time it took")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Done without help or answers", style = Type.bodyStrong, color = p.text)
                        Muted("No lecture, notes, AI or solution in view (level 4).")
                    }
                    Switch(alone, { alone = it }, colors = SwitchDefaults.colors(checkedTrackColor = p.accent))
                }
                Field(note, { note = it }, "Note (optional, no client data)")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                act.setTask(id, EventType.DONE, minutes, alone, note, day)
                act.note(Note("${t.id} done" + if (t.gradedChecks.isNotEmpty()) ". Next: record how it went below." else ""))
                onClose()
            }) { Text("Save", style = Type.smallStrong, color = p.accent) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel", style = Type.bodyStrong, color = p.text2) } },
    )
}

/** The 7 levels from the roadmap (section 3), with this task's position. */
@Composable
private fun LevelLadder(level: Level) {
    val p = LocalPalette.current
    LabCard {
        Text("Level", style = Type.section, color = p.text)
        Spacer(Modifier.padding(top = 6.dp))
        val steps = Level.entries.filter { it.number > 0 }
        // With "needs revision", the work you did still counts up to completed (or practised).
        val base = if (level == Level.NEEDS_REVISION) Level.COMPLETED.number else if (level == Level.SKIPPED) 0 else level.number
        for (l in steps) {
            val reached = base >= l.number
            val here = l == level
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (reached) (if (here) p.accent else p.success) else p.surface2))
                Spacer(Modifier.width(10.dp))
                Text("${l.number} ${l.label}", style = if (here) Type.bodyStrong else Type.small, color = if (reached) p.text else p.text3,
                    modifier = Modifier.weight(1f))
                if (here) Muted(l.meaning, style = Type.caption, maxLines = 1)
            }
        }
        if (level == Level.NEEDS_REVISION) Banner("Your latest check is below its target. Revise, then retake it: the earlier results stay in the history.", Tone.DANGER)
    }
}

@Composable
private fun CheckRow(c: CheckSpec, attempts: List<Assessment>, onOpen: () -> Unit) {
    val p = LocalPalette.current
    val mine = attempts.filter { it.type == typeOf(c) }
    val last = mine.maxByOrNull { it.createdAt }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(c.title, style = Type.bodyStrong, color = p.text)
            Muted(describe(c))
            if (last != null) Text("Last: ${scoreText(last)} · ${if (last.passed) "passed" else "below target"} · ${Days.dayMonth(last.day)}",
                style = Type.small, color = if (last.passed) p.success else p.danger)
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onOpen) { Text(if (last == null) "Start" else "Again", style = Type.bodyStrong, color = p.accent) }
    }
}

fun typeOf(c: CheckSpec): String = when (c) {
    is CheckSpec.Quiz -> "quiz"
    is CheckSpec.Score -> "score"
    is CheckSpec.Checklist -> "checklist"
    is CheckSpec.Rubric -> "rubric"
    is CheckSpec.Explain -> "explain"
    is CheckSpec.Demonstrate -> "demo"
    is CheckSpec.Star -> "star"
    is CheckSpec.Outcome -> "outcome"
}

fun describe(c: CheckSpec): String = when (c) {
    is CheckSpec.Quiz -> "${c.count} questions in the app" + (if (c.target > 0) ", target ${c.target}/${c.count}" else ", pass 70%") +
        (if (c.timedMinutes > 0) ", ${c.timedMinutes} min" else "") + ", cold (no notes)"
    is CheckSpec.Score -> if (c.max > 0) "Enter your score" + (if (c.target > 0) ", target ${c.target}${if (c.label == "%") "%" else "/${c.max}"}" else "")
    else "Enter your score from the notebook"
    is CheckSpec.Checklist -> c.criteria.joinToString(" · ")
    is CheckSpec.Rubric -> "5 criteria, 0–2 each, target ${c.target}/10"
    is CheckSpec.Explain -> "Say it aloud without notes or AI"
    is CheckSpec.Demonstrate -> "Shown or explained to someone, unaided"
    is CheckSpec.Star -> "Situation · Task · Action · Result, under 150 words, with a number"
    is CheckSpec.Outcome -> "Started = sent; useful = 2+ messages"
}

fun scoreText(a: Assessment): String = when (a.type) {
    "checklist", "explain", "demo", "star", "outcome" -> if (a.max > 0) "${a.score.toInt()}/${a.max.toInt()}" else if (a.passed) "yes" else "no"
    else -> if (a.max == 100.0 && a.score <= 100.0 && a.type == "score") "${a.score.toInt()}%" else "${fmt(a.score)}/${fmt(a.max)}"
}

private fun fmt(d: Double) = if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d)

@Composable
fun AttemptLine(a: Assessment) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(Days.short(a.day), style = Type.small, color = p.text2, modifier = Modifier.width(84.dp))
        Text(a.type, style = Type.small, color = p.text2, modifier = Modifier.width(72.dp))
        Text(scoreText(a), style = Type.bodyStrong, color = p.text, modifier = Modifier.weight(1f))
        Tag(if (a.passed) "Passed" else "Below target", if (a.passed) Tone.SUCCESS else Tone.DANGER)
    }
    if (a.independent && a.type in Mastery.MARKED) Muted("Cold: no notes or answers", style = Type.caption)
}
