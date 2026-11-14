package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Level
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.TodayEntry
import com.lakshman.roadmaplab.domain.TodayRef
import com.lakshman.roadmaplab.ui.components.AreaDot
import com.lakshman.roadmaplab.ui.components.CheckCircle
import com.lakshman.roadmaplab.ui.components.LevelTag
import com.lakshman.roadmaplab.ui.components.Tag
import com.lakshman.roadmaplab.ui.components.Tone
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type

fun UiState.areaShort(id: String): String = plan.area(id)?.short ?: id

/** Tick or untick from a list. Every change is recorded; Undo puts the previous state back. */
fun toggleEntry(ui: UiState, act: Actions, e: TodayEntry) {
    when (val r = e.ref) {
        is TodayRef.Task -> toggleTask(ui, act, r.task, e.done)
        is TodayRef.Reduced -> act.tick(ui.today, r.action.id, !e.done, if (!e.done) r.action.minutes else 0)
        TodayRef.Flashcards -> act.tick(ui.today, e.key, !e.done, if (!e.done) 15 else 0)
    }
}

fun toggleTask(ui: UiState, act: Actions, t: PlanTask, done: Boolean) {
    if (done) {
        act.setTask(t.id, EventType.RESET)
        act.note(Note("${t.id} marked not done", "Undo") { act.setTask(t.id, EventType.DONE, t.minutes) })
    } else {
        act.setTask(t.id, EventType.DONE, t.minutes)
        val prev = ui.states[t.id]?.progress
        act.note(Note("${t.id} done${if (t.study && t.minutes > 0) " · ${t.minutesLabel}" else ""}", "Undo") {
            act.setTask(t.id, if (prev == Progress.IN_PROGRESS) EventType.START else EventType.RESET)
        })
    }
}

@Composable
fun EntryRow(ui: UiState, e: TodayEntry, onToggle: () -> Unit, onOpen: () -> Unit, showReason: Boolean = true) {
    val p = LocalPalette.current
    val level = e.task?.let { ui.level(it.id) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).heightIn(min = 60.dp).padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(e.done, onToggle, "${e.code} ${e.title}".trim(), e.skipped, color = p.accent)
        Column(Modifier.weight(1f)) {
            Text(e.title, style = Type.bodyStrong, color = if (e.done) p.text2 else p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                AreaDot(e.area, 7.dp)
                Spacer(Modifier.width(6.dp))
                val reason = e.reason.takeIf { showReason && !e.done && it !in QUIET_REASONS && !it.startsWith("Reduced routine") }.orEmpty()
                val meta = listOf(ui.areaShort(e.area), e.code, e.minutesLabel, reason)
                    .filter { it.isNotBlank() }.distinct().joinToString(" · ")
                Text(meta, style = Type.small, color = p.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            level == Level.NEEDS_REVISION -> ShortLevel(level)
            e.priority && !e.done -> Tag("Priority", Tone.WARNING)
            e.inProgress -> Tag("Started", Tone.ACCENT)
            level != null && level.number >= Level.PRACTISED.number -> ShortLevel(level)
        }
    }
}

/** A plan row (Plan tab, area pages): status, id, title, date. */
@Composable
fun TaskLine(ui: UiState, t: PlanTask, onOpen: () -> Unit, onToggle: (() -> Unit)? = null, showDate: Boolean = false) {
    val p = LocalPalette.current
    val st = ui.states[t.id]?.progress
    val level = ui.level(t.id)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).heightIn(min = 56.dp).padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckCircle(st == Progress.COMPLETED, onToggle, "${t.id} ${t.title}", st == Progress.SKIPPED)
        Column(Modifier.weight(1f)) {
            Text(t.title, style = Type.body, color = if (st == Progress.COMPLETED) p.text2 else p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AreaDot(t.area, 7.dp)
                Spacer(Modifier.width(6.dp))
                Text(listOf(ui.areaShort(t.area), t.id, if (showDate) Days.short(t.day) else "", t.minutesLabel).filter { it.isNotBlank() }.joinToString(" · "),
                    style = Type.small, color = p.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (level.number >= Level.PRACTISED.number || level == Level.NEEDS_REVISION) ShortLevel(level, Modifier.padding(start = 6.dp))
        else if (t.isMilestone) Tag("★", Tone.WARNING, Modifier.padding(start = 6.dp))
    }
}

/** Reasons that are the normal case: not repeated on every row. */
private val QUIET_REASONS = setOf("Planned for today", "Today", "At work today", "Done today", "Optional lead-in")

/** Compact level label for lists ("Practised", "Assessed", "Revise"…). */
@Composable
fun ShortLevel(level: Level, modifier: Modifier = Modifier) {
    val (text, tone) = when (level) {
        Level.NEEDS_REVISION -> "Revise" to Tone.DANGER
        Level.PRACTISED -> "Practised" to Tone.ACCENT
        Level.ASSESSED -> "Assessed" to Tone.SUCCESS
        Level.DEMONSTRATED -> "Demonstrated" to Tone.SUCCESS
        Level.MASTERED -> "Mastered" to Tone.SUCCESS
        else -> level.label to Tone.NEUTRAL
    }
    Tag(text, tone, modifier)
}

@Composable
fun Gap(h: Int = 12) = Spacer(Modifier.padding(top = h.dp))

@Composable
fun RowGap() = Spacer(Modifier.width(12.dp))

val SpacedBy8 = Arrangement.spacedBy(8.dp)
val SpacedBy12 = Arrangement.spacedBy(12.dp)
