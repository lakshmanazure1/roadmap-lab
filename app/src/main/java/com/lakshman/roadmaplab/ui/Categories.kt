package com.lakshman.roadmaplab.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.CATEGORY_ORDER
import com.lakshman.roadmaplab.domain.CatStatus
import com.lakshman.roadmaplab.domain.CategoryDay
import com.lakshman.roadmaplab.domain.CategoryStats
import com.lakshman.roadmaplab.domain.DayHistory
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.ItemEvent
import com.lakshman.roadmaplab.domain.Level
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.Streaks
import com.lakshman.roadmaplab.domain.TaskKind
import com.lakshman.roadmaplab.domain.TodayEngine
import com.lakshman.roadmaplab.ui.components.AreaDot
import com.lakshman.roadmaplab.ui.components.CheckCircle
import com.lakshman.roadmaplab.ui.components.Divider
import com.lakshman.roadmaplab.ui.components.KeyValue
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.ProgressRing
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Stat
import com.lakshman.roadmaplab.ui.components.Tag
import com.lakshman.roadmaplab.ui.components.Tone
import com.lakshman.roadmaplab.ui.components.TopBar
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type
import kotlin.math.roundToInt

private fun pct(x: Double?) = if (x == null) "—" else "${x.roundToInt()}%"

/** A short name that fits under a ring. */
fun UiState.tileLabel(id: String): String = when (id) {
    "python" -> "Python"
    else -> areaShort(id)
}

/**
 * All 10 categories at the top of Today, every one visible at once (2 rows of 5): the ring is the share of the
 * category's tasks done, the number is its streak (days in a row), a green tick means done today and a small
 * circle means it has something planned today. Tap one for its details.
 */
@Composable
fun CategoryHeader(ui: UiState, nav: Nav) {
    val p = LocalPalette.current
    LabCard(padding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Small phones: the legend goes under the title and long names are shortened, so all 10 still fit.
            val narrow = maxWidth < 330.dp
            Column {
                val legend: @Composable () -> Unit = {
                    Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(14.dp), tint = p.text2)
                    Text(" streak · ring = % done", style = Type.caption, color = p.text2)
                }
                if (narrow) {
                    Text("Your 10 categories", style = Type.section, color = p.text, modifier = Modifier.padding(horizontal = 8.dp))
                    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { legend() }
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Your 10 categories", style = Type.section, color = p.text, modifier = Modifier.weight(1f))
                        legend()
                    }
                }
                CATEGORY_ORDER.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        row.forEach { id -> CategoryTile(ui, id, Modifier.weight(1f), narrow) { nav.open(Screen.Area(id)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(ui: UiState, id: String, modifier: Modifier, narrow: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    val c = ui.category(id) ?: return
    val color = p.area(id)
    val label = if (narrow && id == "accounting") "Acctg." else ui.tileLabel(id)
    val state = when {
        c.hitToday -> "done today"
        c.plannedToday -> "planned today, not done yet"
        else -> "nothing planned today"
    }
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${ui.plan.area(id)?.name ?: label}: streak ${c.current} " +
                    (if (c.current == 1) "day" else "days") + ", ${pct(c.pct)} of tasks done, $state"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            ProgressRing((c.pct / 100).toFloat(), size = 52.dp, stroke = 5.dp, color = color, track = color.copy(alpha = 0.18f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(12.dp), tint = if (c.current > 0) color else p.text3)
                    Text("${c.current}", style = Type.statSmall, color = p.text)
                }
            }
            val badge by animateColorAsState(if (c.hitToday) p.success else Color.Transparent, label = "badge")
            if (c.hitToday) {
                Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(badge).border(2.dp, p.surface, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Check, null, Modifier.size(11.dp), tint = Color.White)
                }
            } else if (c.plannedToday) {
                Box(Modifier.align(Alignment.TopEnd).padding(2.dp).size(12.dp).clip(CircleShape).background(p.surface).border(2.dp, color, CircleShape))
            }
        }
        Text(label, style = Type.caption, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp))
        Text(pct(c.pct), style = Type.caption, color = p.text2, maxLines = 1)
    }
}

/**
 * "I did it, I just didn't tick it": yesterday's planned tasks that aren't ticked. Ticking one here counts
 * for yesterday (the day it was planned), so the streaks are right.
 */
@Composable
fun YesterdayCard(ui: UiState, act: Actions) {
    val p = LocalPalette.current
    val list = ui.yesterday
    if (list.isEmpty()) return
    var all by remember { mutableStateOf(false) }
    val day = list.first().day
    LabCard(padding = PaddingValues(top = 14.dp, bottom = 6.dp), tint = p.tertiarySoft) {
        Text("Yesterday · ${Days.short(day)}", style = Type.section, color = p.text, modifier = Modifier.padding(horizontal = 16.dp))
        Muted("Did any of these yesterday? Tick them now: they count for yesterday, streaks included.",
            Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp))
        val shown = if (all) list else list.take(5)
        shown.forEach { t -> BackfillRow(ui, act, t) }
        if (list.size > 5) TextButton(onClick = { all = !all }, modifier = Modifier.padding(start = 8.dp)) {
            Text(if (all) "Show fewer" else "Show all ${list.size}", style = Type.smallStrong, color = p.accent)
        }
    }
}

/** One task you can credit to the day it was planned. */
@Composable
private fun BackfillRow(ui: UiState, act: Actions, t: PlanTask) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        CheckCircle(false, {
            act.setTask(t.id, EventType.DONE, t.minutes, day = t.day)
            act.note(Note("${t.id} counted for ${Days.short(t.day)}", "Undo") { act.setTask(t.id, EventType.RESET) })
        }, "Did ${t.title} on ${Days.short(t.day)}", color = p.area(t.area))
        Column(Modifier.weight(1f)) {
            Text(t.title, style = Type.body, color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AreaDot(t.area)
                Text("  ${ui.areaShort(t.area)} · ${Days.short(t.day)}" + if (t.study && t.minutes > 0) " · ${t.minutesLabel}" else "",
                    style = Type.caption, color = p.text2, maxLines = 1)
            }
        }
    }
}

/** Mood 1–5 for the day: one tap (section 12: the log line includes a mood score). */
@Composable
fun MoodRow(ui: UiState, act: Actions, day: Long, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val mood = ui.mood(day)
    val buttons: @Composable () -> Unit = {
        (1..5).forEach { m ->
            val on = mood == m
            val bg by animateColorAsState(if (on) p.accent else Color.Transparent, label = "mood")
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(bg)
                    .border(1.dp, if (on) Color.Transparent else p.text3, CircleShape)
                    .clickable(role = Role.RadioButton) { act.setMood(day, if (on) null else m) }
                    .semantics { contentDescription = "Mood $m of 5"; stateDescription = if (on) "Selected" else "Not selected" },
                contentAlignment = Alignment.Center,
            ) { Text("$m", style = Type.smallStrong, color = if (on) p.onAccent else p.text) }
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp) {
            // Small phones: the label above, the five buttons spread across the card.
            Column {
                Text("Mood today · 1 low, 5 great", style = Type.bodyStrong, color = p.text)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { buttons() }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mood today", style = Type.bodyStrong, color = p.text)
                    Muted("1 low · 5 great", style = Type.caption)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { buttons() }
            }
        }
    }
}

/**
 * One category, in two tabs. "Days": its streak, calendar and every date with what was planned, how it went
 * and the notes from that day. "About": its goals, rules, levels and scores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaScreen(ui: UiState, act: Actions, nav: Nav, id: String, startTab: Int = 0) {
    val area = ui.plan.area(id) ?: return
    var tab by rememberSaveable(id) { mutableIntStateOf(startTab) }
    Column(Modifier.fillMaxSize()) {
        TopBar(area.name, nav.back)
        PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
            listOf("Days", "About").forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, style = Type.smallStrong) })
            }
        }
        if (tab == 0) AreaDays(ui, act, nav, id) else AreaAbout(ui, id)
    }
}

/** Streak, calendar, anything not ticked, then every date of this category (oldest first). */
@Composable
private fun AreaDays(ui: UiState, act: Actions, nav: Nav, id: String) {
    val c = ui.category(id)
    val open = remember(ui, id) { Streaks.untickedRecent(ui.plan, ui.states, ui.today, back = 7).filter { it.area == id } }
    val days = remember(ui, id) {
        DayHistory.build(ui.plan, id, ui.states, ui.s.evidence, ui.s.assessments, ui.s.ticks, ui.s.dayLogs, ui.s.dayMeta,
            ui.reducedWeeks, c, ui.today)
    }
    val notes = remember(ui.s.events) { DayHistory.taskNotes(ui.s.events) }
    val listState = rememberLazyListState()
    var jumpTo by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(jumpTo) {
        val target = jumpTo ?: return@LaunchedEffect
        val i = days.indexOfFirst { it.day >= target }.let { if (it < 0) days.lastIndex else it }
        // The dates are the last items of the list, so their position doesn't depend on the cards above.
        if (i >= 0) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - days.size + i)
        jumpTo = null
    }
    LazyColumn(state = listState, contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (c != null) {
            item { CategorySummary(ui, id, c) }
            item { CategoryCalendar(ui, id, c, listed = days.mapTo(HashSet()) { it.day }, onDay = { jumpTo = it }) }
        }
        if (open.isNotEmpty()) {
            item { SectionTitle("Not ticked yet", trailing = "last 7 days") }
            item {
                LabCard(padding = PaddingValues(vertical = 6.dp)) {
                    Muted("Did it on the day? Tick it: it counts for the day it was planned.", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    open.forEach { t -> BackfillRow(ui, act, t) }
                }
            }
        }
        val todayInList = days.isNotEmpty() && ui.today in days.first().day..days.last().day
        item {
            SectionTitle("Day by day", trailing = if (todayInList) "Go to today" else "${days.size} dates",
                onTrailing = if (todayInList) ({ jumpTo = ui.today }) else null)
        }
        if (days.isEmpty()) item { Muted("Nothing planned for this category.") }
        items(days, key = { it.day }) { d -> DayBlock(ui, nav, d, notes) }
    }
}

/** What the category is for, how it is assessed, its levels and every score. */
@Composable
private fun AreaAbout(ui: UiState, id: String) {
    val p = LocalPalette.current
    val area = ui.plan.area(id) ?: return
    val prog = ui.areas.firstOrNull { it.area.id == id }
    val checks = ui.s.assessments.filter { it.area == id }.sortedByDescending { it.createdAt }
    LazyColumn(contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LabCard {
                KeyValue("Main objective", area.objective)
                KeyValue("By 31 Jan", area.byEnd)
                if (area.success.isNotBlank()) KeyValue("Success (must have)", area.success + if (area.targetLevel.isNotBlank()) " · target level ${area.targetLevel}" else "")
                if (area.stretch.isNotBlank()) KeyValue("Stretch", area.stretch)
                KeyValue("Time", area.budget)
                if (area.assessed.isNotBlank()) KeyValue("What counts as assessed", area.assessed)
                if (area.notEnough.isNotBlank()) KeyValue("Not enough on its own", area.notEnough)
                ui.plan.meta.monthly[id]?.let { m -> KeyValue("Dated checks", m.joinToString("\n")) }
            }
        }
        if (prog != null && Level.entries.any { (prog.levels[it] ?: 0) > 0 }) item {
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
        item { SectionTitle("Scores", trailing = if (checks.isEmpty()) "none yet" else "${checks.size} attempts") }
        if (checks.isEmpty()) item { Muted("Scores you record for this category's checks appear here, and under their date in Days.") }
        else item {
            LabCard {
                checks.forEach { a ->
                    Text("${Days.dayMonth(a.day)} · ${a.itemId ?: ""} · ${ui.plan.task(a.itemId ?: "")?.title ?: a.topic}", style = Type.caption, color = p.text3,
                        modifier = Modifier.padding(top = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    AttemptLine(a)
                }
            }
        }
    }
}

@Composable
private fun CategorySummary(ui: UiState, id: String, c: CategoryStats) {
    val p = LocalPalette.current
    val color = p.area(id)
    LabCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing((c.pct / 100).toFloat(), size = 96.dp, stroke = 9.dp, color = color, track = color.copy(alpha = 0.18f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pct(c.pct), style = Type.statSmall, color = p.text)
                    Text("done", style = Type.caption, color = p.text2)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.size(22.dp), tint = if (c.current > 0) color else p.text3)
                    Text(" ${c.current}", style = Type.stat, color = p.text)
                    Text(if (c.current == 1) " day in a row" else " days in a row", style = Type.small, color = p.text2)
                }
                Muted("Best: ${c.longest} · ${c.completed} of ${c.total} tasks done", Modifier.padding(top = 2.dp))
                Muted(when {
                    c.hitToday -> "Done today."
                    c.plannedToday -> "Planned today: one task keeps the streak."
                    else -> "Nothing planned today: the streak is safe."
                }, Modifier.padding(top = 2.dp))
            }
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Last 7 days", pct(c.rolling7), Modifier.weight(1f), sub = "planned days done")
            Stat("Last 28 days", pct(c.rolling28), Modifier.weight(1f), sub = "planned days done")
            Stat("On track", if (c.due == 0) "—" else "${c.dueDone}/${c.due}", Modifier.weight(1f), sub = "due by today")
        }
        Muted(
            (if (c.graceRule) "One missed day a week is forgiven: the streak pauses instead of restarting. " else "Each planned day counts. ") +
                "Days with nothing planned, rest days and reduced days without an action never break it.",
            Modifier.padding(top = 10.dp), style = Type.caption,
        )
    }
}

/** The plan as a calendar (Mon–Sun rows), each day coloured by how this category went. */
@Composable
private fun CategoryCalendar(ui: UiState, id: String, c: CategoryStats, listed: Set<Long>, onDay: (Long) -> Unit) {
    val p = LocalPalette.current
    val color = p.area(id)
    val planned = ui.plan.tasks.filter { it.area == id && it.kind != TaskKind.REST && !it.optional }.mapTo(HashSet()) { it.day }
    val byDay = c.days.associate { it.day to it.status }
    val first = Days.weekStart(ui.plan.start)
    LabCard {
        Text("Calendar", style = Type.section, color = p.text)
        Row(Modifier.padding(top = 8.dp)) {
            Spacer(Modifier.width(36.dp))
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = Type.caption, color = p.text3, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        var ws = first
        var week = 1
        while (ws <= ui.plan.end) {
            val w = ws
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("W$week", style = Type.caption, color = if (ui.today in w..w + 6) p.text else p.text3, modifier = Modifier.width(36.dp))
                for (k in 0L..6L) {
                    val d = w + k
                    val s = byDay[d]
                    Box(Modifier.weight(1f).padding(2.dp).heightIn(min = 26.dp).clickable(enabled = d in listed, onClickLabel = "Show ${Days.long(d)}") { onDay(d) }, contentAlignment = Alignment.Center) {
                        val shape = RoundedCornerShape(8.dp)
                        val mod = Modifier.fillMaxWidth().heightIn(min = 26.dp).clip(shape)
                        when {
                            s == CatStatus.HIT -> Box(mod.background(color), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = Color.White)
                            }
                            s == CatStatus.GRACE -> Box(mod.background(color.copy(alpha = 0.28f)))
                            s == CatStatus.MISSED -> Box(mod.border(1.5.dp, p.text3, shape))
                            s == CatStatus.OPEN -> Box(mod.border(2.dp, p.accent, shape))
                            s == CatStatus.FUTURE && d in planned -> Box(mod.background(p.surface2))
                            d in ui.plan.restDays -> Text("rest", style = Type.caption, color = p.text3)
                            else -> Box(Modifier.size(4.dp).clip(CircleShape).background(p.border))
                        }
                    }
                }
            }
            ws += 7
            week++
        }
        Muted("Tap a day to see it below. Filled: done · pale: forgiven · outline: missed · bold outline: today · grey: planned later · dot: nothing planned",
            Modifier.padding(top = 8.dp), style = Type.caption)
    }
}


/** How one date went for this category, in a word and a colour. */
private fun dayLabel(ui: UiState, d: CategoryDay): Pair<String, Tone> = when {
    d.status == CatStatus.HIT -> (if (d.day == ui.today) "Done today" else "Done") to Tone.SUCCESS
    d.status == CatStatus.OPEN -> "Today" to Tone.ACCENT
    d.status == CatStatus.GRACE -> "Missed, forgiven" to Tone.WARNING
    d.status == CatStatus.MISSED -> "Missed" to Tone.DANGER
    d.status == CatStatus.FUTURE -> "Planned" to Tone.NEUTRAL
    d.day == ui.today -> "Today" to Tone.ACCENT
    d.dayKind == DayKind.REST -> "Rest day" to Tone.NEUTRAL
    d.dayKind == DayKind.CATCHUP -> "Catch-up day" to Tone.NEUTRAL
    d.dayKind == DayKind.REDUCED -> "Reduced day" to Tone.NEUTRAL
    d.status == null -> "Before start" to Tone.NEUTRAL
    d.planned.isNotEmpty() && d.planned.all { it.optional } -> "Optional" to Tone.NEUTRAL
    else -> "Not counted" to Tone.NEUTRAL
}

/** What a task's tick says, for the date it is listed under. */
private fun taskState(ui: UiState, t: PlanTask, day: Long): String {
    val st = ui.states[t.id]
    return when (st?.progress) {
        Progress.COMPLETED -> "Done" + (st.doneDay?.takeIf { it != day }?.let { " on ${Days.dayMonth(it)}" } ?: "") +
            if (st.doneMinutes > 0) " · ${st.doneMinutes} min" else ""
        Progress.SKIPPED -> "Skipped"
        Progress.IN_PROGRESS -> "Started" + st.partMinutes.values.sum().takeIf { it > 0 }?.let { " · $it min so far" }.orEmpty()
        else -> when {
            day < ui.today -> "Not done"
            day == ui.today -> "To do today"
            else -> "Not started"
        }
    }
}

/** One date in this category: what was planned and how it went, what else you did, and the notes from that day. */
@Composable
internal fun DayBlock(ui: UiState, nav: Nav, d: CategoryDay, notes: Map<String, List<ItemEvent>>) {
    val p = LocalPalette.current
    val (label, tone) = dayLabel(ui, d)
    val sub = listOfNotNull(ui.plan.weekOf(d.day)?.let { "Week $it" }, Days.relative(d.day, ui.today), d.dayKind?.label)
    LabCard(padding = PaddingValues(0.dp), border = if (d.day == ui.today) p.accent else null) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Days.long(d.day), style = Type.section, color = p.text)
                Text(sub.joinToString(" · "), style = Type.caption, color = p.text3)
            }
            Tag(label, tone)
        }
        d.planned.forEachIndexed { i, t ->
            if (i > 0) Divider(Modifier.padding(start = 56.dp))
            TaskLine(ui, t, { nav.open(Screen.Task(t.id)) }, null)
            Text(taskState(ui, t, d.day), style = Type.caption,
                color = if (ui.states[t.id]?.progress == Progress.COMPLETED) p.text2 else p.text3,
                modifier = Modifier.padding(start = 56.dp, end = 12.dp, bottom = 4.dp))
            notes[t.id].orEmpty().forEach { n ->
                NoteLine(if (n.day == d.day) "Note" else "Note, ${Days.dayMonth(n.day)}", n.note, Modifier.padding(start = 56.dp, end = 12.dp, bottom = 6.dp))
            }
        }
        if (d.alsoDone.isNotEmpty()) {
            if (d.planned.isNotEmpty()) Divider(Modifier.padding(start = 16.dp))
            Text("Also done this day", style = Type.caption, color = p.text3, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
            d.alsoDone.forEach { t ->
                TaskLine(ui, t, { nav.open(Screen.Task(t.id)) }, null)
                Text("Planned for ${Days.dayMonth(TodayEngine.effectiveDay(t, ui.reducedWeeks))} · ${taskState(ui, t, d.day)}", style = Type.caption,
                    color = p.text3, modifier = Modifier.padding(start = 56.dp, end = 12.dp, bottom = 4.dp))
            }
        }
        if (d.records.isNotEmpty()) {
            Divider(Modifier.padding(start = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                d.records.forEach { r -> NoteLine(r.kind.label, r.text) }
            }
        }
        Spacer(Modifier.size(4.dp))
    }
}

/** "Label: text", the label in bold, at most four lines. */
@Composable
private fun NoteLine(label: String, text: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(label) }
            append(": ")
            append(text)
        },
        style = Type.small, color = p.text, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = modifier,
    )
}
