package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.AreaProgress
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Level
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.TaskKind
import com.lakshman.roadmaplab.domain.TodayEngine
import com.lakshman.roadmaplab.ui.components.AreaLabel
import com.lakshman.roadmaplab.ui.components.DayStrip
import com.lakshman.roadmaplab.ui.components.Divider
import com.lakshman.roadmaplab.ui.components.KeyValue
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.ProgressLine
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SecondaryButton
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Stat
import com.lakshman.roadmaplab.ui.components.Tag
import com.lakshman.roadmaplab.ui.components.Tone
import com.lakshman.roadmaplab.ui.components.TopBar
import com.lakshman.roadmaplab.ui.components.WeekBars
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type

private fun pct(x: Double?) = if (x == null) "—" else "%.0f%%".format(x)

/** Overall progress, consistency, the 8 areas, what needs attention and what's next. */
@Composable
fun ProgressScreen(ui: UiState, nav: Nav) {
    val p = LocalPalette.current
    val c = ui.consistency
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Progress", style = Type.display, color = p.text)
                    Muted("${ui.todayPlan.daysLeft} days left until 31 Jan 2027")
                }
                IconButton(onClick = { nav.open(Screen.HowItWorks) }) { Icon(Icons.Outlined.HelpOutline, "How levels and streaks work", tint = p.text2) }
            }
        }
        // ---- Consistency
        item {
            LabCard {
                Text("Consistency", style = Type.section, color = p.text)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat("Current streak", "${c.current}", Modifier.weight(1f), sub = "days")
                    Stat("Longest", "${c.longest}", Modifier.weight(1f), sub = "days")
                    Stat("This week", "${c.activeThisWeek}/${c.scheduledThisWeek}", Modifier.weight(1f), sub = "active days")
                }
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat("Last 7 days", pct(c.rolling7), Modifier.weight(1f))
                    Stat("Last 28 days", pct(c.rolling28), Modifier.weight(1f))
                    val w = ui.weeks.firstOrNull { it.current }
                    Stat("Study tasks", if (w != null) "${w.completed}/${w.planned}" else "—", Modifier.weight(1f), sub = "this week, done / planned")
                }
                Spacer(Modifier.padding(top = 10.dp))
                val last28 = c.days.filter { it.day in (ui.today - 27)..ui.today }
                DayStrip(last28)
                Muted("Last 28 days · filled: minimum met · pale: forgiven (1 a week) · outline: missed · dot: rest or nothing scheduled",
                    Modifier.padding(top = 6.dp), style = Type.caption)
                if (c.graceUsedThisWeek) Muted("This week's forgiven day is used. A missed day pauses the streak; it never erases the longest one.",
                    Modifier.padding(top = 4.dp))
            }
        }
        // ---- Overall
        item {
            LabCard {
                Text("Overall", style = Type.section, color = p.text)
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat("Tasks done", "${ui.totalDone}", Modifier.weight(1f), sub = "of ${ui.plan.tasks.count { it.kind != TaskKind.REST }} (rest days not counted)")
                    Stat("Planned by today", "${ui.dueByToday}", Modifier.weight(1f))
                    val mins = ui.minutes.values.sum()
                    Stat("Study time", if (mins >= 60) "%.1f h".format(mins / 60.0) else "$mins min", Modifier.weight(1f))
                }
                Spacer(Modifier.padding(top = 12.dp))
                Text("Weekly: study tasks done against planned", style = Type.smallStrong, color = p.text2)
                WeekBars(ui.weeks.map { it.planned }, ui.weeks.map { it.completed }, ui.weeks.indexOfFirst { it.current },
                    Modifier.padding(top = 8.dp), labels = ui.weeks.map { "${it.week}" })
                Muted("Weeks 1–11", style = Type.caption)
                Spacer(Modifier.padding(top = 10.dp))
                ui.months.forEach { m ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(m.label, style = Type.small, color = p.text, modifier = Modifier.width(96.dp))
                        Muted("${m.completed}/${m.planned} study tasks · ${m.qualifiedDays}/${m.scheduledDays} days met the minimum")
                    }
                }
            }
        }
        // ---- Areas
        item { SectionTitle("The 8 areas", trailing = "bar = done · line = planned by today") }
        item {
            LabCard(padding = PaddingValues(0.dp)) {
                ui.areas.forEachIndexed { i, a ->
                    if (i > 0) Divider()
                    AreaRow(ui, a) { nav.open(Screen.Area(a.area.id)) }
                }
            }
        }
        // ---- Attention
        if (ui.needsRevision.isNotEmpty() || ui.weak.isNotEmpty()) {
            item { SectionTitle("Needs attention") }
            item {
                LabCard {
                    ui.weak.take(4).forEach { w ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            AreaLabel(w.area, ui.areaShort(w.area), Modifier.width(110.dp))
                            Text(w.topic, style = Type.small, color = p.text, modifier = Modifier.weight(1f))
                            Tag(pct(w.avg), Tone.DANGER)
                        }
                    }
                    if (ui.needsRevision.isNotEmpty()) {
                        Muted("Retake: " + ui.needsRevision.take(6).joinToString(" · ") { it.id }, Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        // ---- Milestones and outputs
        item { SectionTitle("Next milestones") }
        item {
            LabCard {
                if (ui.milestones.isEmpty()) Muted("No milestones left. Well done.")
                ui.milestones.forEach { m ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Days.short(m.day), style = Type.smallStrong, color = p.text, modifier = Modifier.width(96.dp))
                        Text("★ ${m.milestone}", style = Type.small, color = p.text, modifier = Modifier.weight(1f))
                        Muted(Days.relative(m.day, ui.today), style = Type.caption)
                    }
                }
            }
        }
        item {
            LabCard {
                Text("Outputs", style = Type.section, color = p.text)
                val (sent, useful) = ui.conversations
                val (lines, days) = ui.logged
                val h = ui.habitsThisWeek
                KeyValue("LinkedIn", "$sent of 9 conversations started · $useful useful (2+ messages)")
                KeyValue("Evidence log", "$lines of $days working days logged" + if (days > 0) " (${100 * lines / days}%, target 80%)" else "")
                KeyValue("Health this week", "Slept 7 h: ${h.sleepNights} nights (target 5) · exercise ${h.exercise} of ${h.exercisePlanned} (2 is fine)" +
                    (h.calledHome?.let { if (it) " · called home" else "" } ?: ""))
            }
        }
        val week = ui.plan.weekOf(ui.today)?.coerceIn(1, 11)
        if (week != null) item {
            SecondaryButton("Weekly review · week $week", { nav.open(Screen.Weekly(week)) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.FactCheck)
        }
    }
}

@Composable
private fun AreaRow(ui: UiState, a: AreaProgress, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).then(Modifier)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AreaLabel(a.area.id, a.area.name, Modifier.weight(1f))
            Text("${a.completed}/${a.total}", style = Type.smallStrong, color = p.text)
        }
        ProgressLine((a.pct / 100).toFloat(), Modifier.padding(top = 6.dp), color = p.area(a.area.id), marker = (a.duePct / 100).toFloat())
        val bits = buildList {
            if (a.behind > 0) add("${a.behind} waiting") else if (a.dueByToday > 0) add("on track")
            a.avgScore?.let { add("scores ${pct(it)}") }
            val adv = a.atLeast(Level.ASSESSED)
            if (adv > 0) add("$adv assessed+")
            if (a.needsRevision > 0) add("${a.needsRevision} to revise")
            a.next?.let { add("next ${it.id}") }
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Muted(bits.joinToString(" · "), Modifier.weight(1f), maxLines = 1)
            androidx.compose.material3.TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text("Details", style = Type.smallStrong, color = p.accent)
            }
        }
    }
}

/** One area: its goal by 31 Jan, what counts as assessed, levels, scores and tasks. */
@Composable
fun AreaScreen(ui: UiState, nav: Nav, id: String) {
    val p = LocalPalette.current
    val area = ui.plan.area(id) ?: return
    val prog = ui.areas.firstOrNull { it.area.id == id }
    val tasks = ui.plan.tasks.filter { it.area == id }
    val checks = ui.s.assessments.filter { it.area == id }.sortedByDescending { it.createdAt }
    Column(Modifier.fillMaxSize()) {
        TopBar(area.name, nav.back)
        LazyColumn(contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                LabCard {
                    KeyValue("Main objective", area.objective)
                    KeyValue("By 31 Jan", area.byEnd)
                    if (area.success.isNotBlank()) KeyValue("Success (must have)", area.success + if (area.targetLevel.isNotBlank()) " · target level ${area.targetLevel}" else "")
                    if (area.stretch.isNotBlank()) KeyValue("Stretch", area.stretch)
                    KeyValue("Time", area.budget)
                }
            }
            item {
                LabCard {
                    KeyValue("What counts as assessed", area.assessed)
                    KeyValue("Not enough on its own", area.notEnough)
                    ui.plan.meta.monthly[id]?.let { m -> KeyValue("Monthly checks", m.joinToString("\n")) }
                }
            }
            if (prog != null) item {
                LabCard {
                    Text("Levels", style = Type.section, color = p.text)
                    Level.entries.filter { (prog.levels[it] ?: 0) > 0 }.forEach { l ->
                        Row(Modifier.padding(top = 4.dp)) {
                            Text(if (l.number > 0) "${l.number} ${l.label}" else l.label, style = Type.small, color = p.text, modifier = Modifier.weight(1f))
                            Text("${prog.levels[l]}", style = Type.smallStrong, color = p.text)
                        }
                    }
                }
            }
            if (area.rules.isNotEmpty()) item {
                LabCard { area.rules.forEach { Muted("• $it", Modifier.padding(vertical = 2.dp)) } }
            }
            if (checks.isNotEmpty()) {
                item { SectionTitle("Scores", trailing = "${checks.size} attempts") }
                item {
                    LabCard {
                        checks.take(12).forEach { a ->
                            Text("${a.itemId ?: ""} · ${ui.plan.task(a.itemId ?: "")?.title ?: a.topic}", style = Type.caption, color = p.text3,
                                modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                            AttemptLine(a)
                        }
                    }
                }
            }
            item { SectionTitle("Tasks", trailing = "${tasks.count { ui.states[it.id]?.progress == Progress.COMPLETED }}/${tasks.size} done") }
            item {
                LabCard(padding = PaddingValues(0.dp)) {
                    val upcoming = tasks.sortedBy { TodayEngine.effectiveDay(it, ui.reducedWeeks) }
                    upcoming.forEachIndexed { i, t ->
                        if (i > 0) Divider(Modifier.padding(start = 56.dp))
                        TaskLine(ui, t, { nav.open(Screen.Task(t.id)) }, null, showDate = true)
                    }
                }
            }
        }
    }
}
