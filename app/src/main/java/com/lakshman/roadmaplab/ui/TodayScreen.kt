package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Consistency
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayType
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.NextKind
import com.lakshman.roadmaplab.domain.Section
import com.lakshman.roadmaplab.domain.TodayEngine
import com.lakshman.roadmaplab.domain.TodayEntry
import com.lakshman.roadmaplab.domain.TodayRef
import com.lakshman.roadmaplab.ui.components.Banner
import com.lakshman.roadmaplab.ui.components.Chip
import com.lakshman.roadmaplab.ui.components.ChipRow
import com.lakshman.roadmaplab.ui.components.Divider
import com.lakshman.roadmaplab.ui.components.Field
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.PrimaryButton
import com.lakshman.roadmaplab.ui.components.ProgressRing
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SecondaryButton
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Tone
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type

/**
 * Today first. The screen answers, top to bottom: what day is it in the plan, how much is done,
 * what to do next and why, then the list itself. Everything else lives on other tabs.
 */
@Composable
fun TodayScreen(ui: UiState, act: Actions, nav: Nav) {
    val p = LocalPalette.current
    val t = ui.todayPlan
    var dayOptions by remember { mutableStateOf(false) }
    var dayReview by remember { mutableStateOf(false) }
    fun open(e: TodayEntry) = when (val r = e.ref) {
        is TodayRef.Task -> nav.open(Screen.Task(r.task.id))
        TodayRef.Flashcards -> nav.open(Screen.Practice)
        is TodayRef.Reduced -> toggleEntry(ui, act, e)
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(ui, onSettings = { nav.open(Screen.Settings) }) }
        item { DayBanners(ui, act) }
        item { TodayCard(ui) }
        item { NextActionCard(ui, act, nav) }

        val main = t.section(Section.MAIN)
        val preview = t.section(Section.PREVIEW)
        if (preview.isNotEmpty()) {
            item { SectionTitle("Coming up") }
            item { EntryCard(ui, preview, act, ::open) }
        }
        if (main.isNotEmpty()) {
            item { SectionTitle(if (t.type == DayType.REDUCED) "Today's minimum (reduced routine)" else "Today",
                trailing = if (t.minutesPlanned > 0) "${t.minutesPlanned} min planned" else null) }
            item { EntryCard(ui, main, act, ::open) }
        }
        val health = t.section(Section.HEALTH)
        if (health.isNotEmpty() || (ui.plan.inPlan(ui.today) && t.type != DayType.REST)) {
            item { SectionTitle("Health · family · money", trailing = "not counted as study") }
            item {
                LabCard(padding = PaddingValues(0.dp)) {
                    SleepQuestion(ui, act)
                    health.forEachIndexed { i, e ->
                        Divider(Modifier.padding(start = 56.dp))
                        EntryRow(ui, e, { toggleEntry(ui, act, e) }, { open(e) })
                    }
                }
            }
        }
        for ((section, title) in listOf(Section.OPTIONAL to "Optional", Section.LATER to "Not today (keeps its place)",
            Section.PAUSED to "Paused during the reduced routine")) {
            val list = t.section(section)
            if (list.isNotEmpty()) {
                item { SectionTitle(title) }
                item { EntryCard(ui, list, act, ::open) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Review the day", { dayReview = true }, Modifier.weight(1f), icon = Icons.Outlined.EventNote)
                SecondaryButton("Day options", { dayOptions = true }, Modifier.weight(1f), icon = Icons.Outlined.Tune)
            }
        }
    }
    if (dayOptions) DayOptionsDialog(ui, act) { dayOptions = false }
    if (dayReview) DayReviewDialog(ui, act, nav) { dayReview = false }
}

@Composable
private fun Header(ui: UiState, onSettings: () -> Unit) {
    val p = LocalPalette.current
    val t = ui.todayPlan
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(Days.long(ui.today), style = Type.display, color = p.text)
            val week = t.week
            val sub = when {
                ui.today < ui.plan.leadIn -> "Plan starts ${Days.short(ui.plan.leadIn)} · ${t.daysLeft} days to 31 Jan"
                ui.today == ui.plan.leadIn -> "Optional lead-in · day 1 is tomorrow"
                t.planDay != null -> "Day ${t.planDay} of ${ui.plan.totalDays} · Week $week" +
                    (week?.let { ui.plan.meta.weekFocus[it] }?.let { " · $it" } ?: "") + " · ${t.daysLeft} days left"
                else -> "Plan finished on 31 Jan"
            }
            Muted(sub, Modifier.padding(top = 2.dp))
        }
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings", tint = p.text2) }
    }
}

@Composable
private fun DayBanners(ui: UiState, act: Actions) {
    val t = ui.todayPlan
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ui.s.planError?.let { Banner(it, Tone.WARNING) }
        when (t.type) {
            DayType.REST -> Banner("Planned rest day. No study; call home or a friend, sleep in. Rest never breaks your streak.", Tone.SUCCESS,
                title = "Rest day")
            DayType.REDUCED -> Banner("About 20 minutes a day; every area still gets a minimum action. Missed tasks wait in order.",
                Tone.ACCENT, "Back to the normal routine", { act.setReducedWeek(ui.today, false) }, title = "Reduced routine this week")
            DayType.CATCHUP -> Banner("Overdue tasks from every area are shown, up to two each. Stop when your minimum is met.",
                Tone.ACCENT, "Make it a normal day", { act.setDayKind(ui.today, null) }, title = "Catch-up day")
            else -> {}
        }
    }
}

@Composable
private fun TodayCard(ui: UiState) {
    val p = LocalPalette.current
    val t = ui.todayPlan
    LabCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val frac = if (t.target > 0) t.minutesDone.toFloat() / t.target else if (t.mainDone > 0) 1f else 0f
            ProgressRing(frac, color = if (t.qualified) p.success else p.accent) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${t.minutesDone}", style = Type.statSmall, color = p.text)
                    Text("min", style = Type.caption, color = p.text2)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                val headline = when {
                    t.type == DayType.REST -> "Rest day"
                    t.type == DayType.NONE -> if (ui.today < ui.plan.start) "Before the plan" else "Outside the plan"
                    t.qualified -> "Today's minimum is met"
                    else -> "${(t.target - t.minutesDone).coerceAtLeast(0)} min to today's minimum"
                }
                Text(headline, style = Type.section, color = if (t.qualified) p.success else p.text)
                val main = t.main.filter { it.study }
                Muted(buildString {
                    if (t.target > 0) append("Minimum ${t.target} min (${t.type.label.lowercase()})")
                    if (main.isNotEmpty()) append(if (isNotEmpty()) " · " else "").append("${main.count { it.done }} of ${main.size} tasks done")
                })
                Muted("Your cue: ${ui.s.settings.cue}" + if (ui.s.settings.reminderOn) " · reminder ${"%02d:%02d".format(ui.s.settings.reminderHour, ui.s.settings.reminderMinute)}" else "",
                    Modifier.padding(top = 2.dp), maxLines = 2)
            }
        }
    }
}

@Composable
private fun NextActionCard(ui: UiState, act: Actions, nav: Nav) {
    val p = LocalPalette.current
    val n = ui.next
    LabCard(tint = p.accentSoft, border = p.accentSoft) {
        Text("Next", style = Type.caption, color = p.accent)
        Text(n.title, style = Type.section, color = p.text, modifier = Modifier.padding(top = 2.dp))
        Muted(n.why, Modifier.padding(top = 2.dp))
        val key = n.key
        if (key != null && n.kind !in setOf(NextKind.DONE, NextKind.REST)) {
            Spacer(Modifier.padding(top = 10.dp))
            PrimaryButton(
                when (n.kind) {
                    NextKind.CHECK -> "Score it"
                    NextKind.REVISE -> "Revise"
                    NextKind.PRACTICE -> "Practise"
                    NextKind.WEEKLY_REVIEW -> "Open the review"
                    NextKind.BEFORE_START -> "Look at it"
                    else -> "Open"
                },
                onClick = {
                    when {
                        key == TodayEngine.FLASHCARDS -> nav.open(Screen.Practice)
                        n.kind == NextKind.WEEKLY_REVIEW -> nav.open(Screen.Weekly(ui.plan.task(key)?.week ?: (ui.todayPlan.week ?: 1)))
                        key.startsWith("red-") -> ui.todayPlan.entries.firstOrNull { it.key == key }?.let { toggleEntry(ui, act, it) }
                        else -> nav.open(Screen.Task(key))
                    }
                },
                icon = Icons.Outlined.PlayArrow,
            )
        }
    }
}

@Composable
private fun EntryCard(ui: UiState, list: List<TodayEntry>, act: Actions, open: (TodayEntry) -> Unit) {
    LabCard(padding = PaddingValues(0.dp)) {
        list.forEachIndexed { i, e ->
            if (i > 0) Divider(Modifier.padding(start = 56.dp))
            EntryRow(ui, e, { if (e.section != Section.PREVIEW || e.task != null) toggleEntry(ui, act, e) }, { open(e) })
        }
    }
}

@Composable
private fun SleepQuestion(ui: UiState, act: Actions) {
    val p = LocalPalette.current
    val answer = ui.sleep[ui.today]
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Slept 7 hours last night?", style = Type.bodyStrong, color = p.text)
            Muted("This week: ${ui.habitsThisWeek.sleepNights} of 7 nights (target 5)")
        }
        ChipRow {
            Chip("Yes", answer == true, { act.tick(ui.today, Consistency.SLEEP, true, 0) })
            Chip("No", answer == false, { act.tick(ui.today, Consistency.SLEEP, false, 0) })
        }
    }
}

@Composable
private fun DayOptionsDialog(ui: UiState, act: Actions, onClose: () -> Unit) {
    val p = LocalPalette.current
    val kind = ui.s.dayMeta[ui.today]?.kind
    var restPick by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = p.surface,
        title = { Text(if (restPick) "Plan a rest day" else "Day options", style = Type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!restPick) {
                    Option("Reduced routine this week", "Section 13: about 20 minutes a day for a busy go-live week. Reduced days never break your streak.",
                        kind == DayKind.REDUCED) { act.setReducedWeek(ui.today, kind != DayKind.REDUCED); onClose() }
                    Option("Catch-up day", "Shows overdue tasks from every area (up to two each). Don't double up on other days.",
                        kind == DayKind.CATCHUP) { act.setDayKind(ui.today, if (kind == DayKind.CATCHUP) null else DayKind.CATCHUP); onClose() }
                    Option("Plan a rest day", "Plan it ahead: today or a later day. It never breaks your streak.", false) { restPick = true }
                    if (kind != null) Option("Back to a normal day", "", false) { act.setDayKind(ui.today, null); onClose() }
                } else {
                    (0L..13L).map { ui.today + it }.filter { ui.plan.inPlan(it) }.forEach { d ->
                        val isRest = ui.s.dayMeta[d]?.kind == DayKind.REST || ui.plan.restDays.containsKey(d)
                        Option(Days.short(d) + if (d == ui.today) " (today)" else "", if (isRest) "Already a rest day · tap to undo" else "", isRest) {
                            if (!ui.plan.restDays.containsKey(d)) act.setDayKind(d, if (isRest) null else DayKind.REST)
                            onClose()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close", style = Type.bodyStrong, color = p.accent) } },
    )
}

@Composable
private fun Option(title: String, detail: String, on: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 48.dp).padding(vertical = 6.dp)) {
        Text((if (on) "✓ " else "") + title, style = Type.bodyStrong, color = if (on) p.accent else p.text)
        if (detail.isNotBlank()) Muted(detail)
    }
}

@Composable
private fun DayReviewDialog(ui: UiState, act: Actions, nav: Nav, onClose: () -> Unit) {
    val p = LocalPalette.current
    val t = ui.todayPlan
    var note by remember { mutableStateOf(ui.s.dayLogs[ui.today]?.note ?: "") }
    val hasLine = ui.s.evidence.any { it.day == ui.today && !it.isStory }
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = p.surface,
        title = { Text("Review the day", style = Type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${t.minutesDone} min of study · ${t.main.count { it.done }} of ${t.main.size} tasks done", style = Type.body, color = p.text)
                Muted(if (t.qualified) "Today's minimum is met. That's a day that counts." else if (t.target > 0) "Below today's minimum. That's fine: tomorrow continues in order, no doubling up." else "")
                if (!hasLine && ui.plan.inPlan(ui.today)) {
                    Banner("No evidence line yet today. One line with a real result is enough.", Tone.ACCENT, "Write today's line", {
                        onClose(); nav.open(Screen.EvidenceEdit(null, ui.today, null, false))
                    })
                }
                Field(note, { note = it }, "One line: how did today go? (optional)", minLines = 2)
            }
        },
        confirmButton = { TextButton(onClick = { act.saveDayNote(ui.today, note); onClose() }) { Text("Save", style = Type.bodyStrong, color = p.accent) } },
        dismissButton = { TextButton(onClick = onClose) { Text("Close", style = Type.bodyStrong, color = p.text2) } },
    )
}
