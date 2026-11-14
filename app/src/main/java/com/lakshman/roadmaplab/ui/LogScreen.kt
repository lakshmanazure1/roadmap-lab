package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.AREA_ORDER
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Evidence
import com.lakshman.roadmaplab.domain.Privacy
import com.lakshman.roadmaplab.domain.StarCheck
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.Topics
import com.lakshman.roadmaplab.ui.components.AreaLabel
import com.lakshman.roadmaplab.ui.components.Banner
import com.lakshman.roadmaplab.ui.components.Chip
import com.lakshman.roadmaplab.ui.components.ChipRow
import com.lakshman.roadmaplab.ui.components.ConfirmDialog
import com.lakshman.roadmaplab.ui.components.Divider
import com.lakshman.roadmaplab.ui.components.Field
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.PrimaryButton
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Tag
import com.lakshman.roadmaplab.ui.components.Tone
import com.lakshman.roadmaplab.ui.components.TopBar
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type
import org.json.JSONObject

/** The evidence log: one line a working day, the weekly ★, and the three STAR stories. */
@Composable
fun LogScreen(ui: UiState, act: Actions, nav: Nav, backup: BackupHooks) {
    val p = LocalPalette.current
    var area by rememberSaveable { mutableStateOf<String?>(null) }
    val lines = ui.s.evidence.filter { !it.isStory && (area == null || it.area == area) }.sortedWith(compareByDescending<Evidence> { it.day }.thenByDescending { it.id })
    val stories = ui.s.evidence.filter { it.isStory }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Evidence log", style = Type.display, color = p.text)
                        val (n, d) = ui.logged
                        Muted("$n of $d working days logged · ${ui.s.evidence.count { it.starred }} ★ · ${stories.size} of 3 STAR stories")
                    }
                    IconButton(onClick = backup.exportEvidence) { Icon(Icons.Outlined.IosShare, "Export the log as text", tint = p.text2) }
                }
            }
            item {
                Banner("Say “the client”, never a name. Rough numbers. No PAN, GSTIN, bank details, invoices or anything confidential: the app warns you if it sees one.",
                    Tone.NEUTRAL)
            }
            item { SectionTitle("STAR stories", trailing = "#1 30 Nov · #2 31 Dec · #3 29 Jan") }
            item {
                LabCard(padding = PaddingValues(0.dp)) {
                    listOf("T077" to 1, "T228" to 2, "T369" to 3).forEachIndexed { i, (taskId, n) ->
                        if (i > 0) Divider()
                        val s = stories.firstOrNull { it.itemId == taskId }
                        Row(Modifier.fillMaxWidth().clickable {
                            nav.open(Screen.EvidenceEdit(s?.id, s?.day ?: ui.today, taskId, true))
                        }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("STAR #$n", style = Type.bodyStrong, color = p.text)
                                Muted(s?.starR?.ifBlank { null } ?: if (s == null) "From ${listOf("November", "December", "January")[n - 1]}'s starred lines" else "Draft")
                            }
                            if (s != null) {
                                val c = StarCheck.check(s)
                                Tag(if (c.ok) "Ready" else "${c.passed}/${c.total}", if (c.ok) Tone.SUCCESS else Tone.WARNING)
                            } else Tag("Not written", Tone.NEUTRAL)
                        }
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Chip("All", area == null, { area = null }) }
                    items(AREA_ORDER) { a -> Chip(ui.areaShort(a), area == a, { area = if (area == a) null else a }, dot = a) }
                }
            }
            if (lines.isEmpty()) item { Muted("No lines yet. One line at the end of each working day, even on bad days: “Nothing new today” is a valid line.") }
            for ((week, list) in lines.groupBy { Days.weekStart(it.day) }) {
                item { SectionTitle("Week of ${Days.short(week)}", trailing = if (list.any { it.starred }) "★ chosen" else "star the best line") }
                item {
                    LabCard(padding = PaddingValues(0.dp)) {
                        list.forEachIndexed { i, e ->
                            if (i > 0) Divider()
                            EvidenceRow(ui, e, onOpen = { nav.open(Screen.EvidenceEdit(e.id, e.day, e.itemId, false)) },
                                onStar = { act.saveEvidence(e.copy(starred = !e.starred)) })
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { nav.open(Screen.EvidenceEdit(null, ui.today, null, false)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = p.accent, contentColor = p.onAccent,
            icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Today's line", style = Type.bodyStrong) },
        )
    }
}

@Composable
private fun EvidenceRow(ui: UiState, e: Evidence, onOpen: () -> Unit, onStar: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 16.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Days.short(e.day), style = Type.smallStrong, color = p.text)
                Spacer(Modifier.width(8.dp))
                AreaLabel(e.area, ui.areaShort(e.area))
            }
            Text(listOf(e.task, e.problem).filter { it.isNotBlank() }.joinToString(": ").ifBlank { "(no task)" }, style = Type.bodyStrong,
                color = p.text, maxLines = 2, modifier = Modifier.padding(top = 2.dp))
            if (e.result.isNotBlank()) Text("Result: ${e.result}", style = Type.small, color = p.text, maxLines = 2)
            if (!e.hasRealResult) Text("Add a real result, not just “worked on X”", style = Type.caption, color = p.warning)
            if (Privacy.scan(e.text).isNotEmpty()) Text("Check for confidential data", style = Type.caption, color = p.danger)
        }
        IconButton(onClick = onStar) {
            Icon(if (e.starred) Icons.Filled.Star else Icons.Outlined.StarOutline, if (e.starred) "Starred: best line of the week" else "Star as the week's best line",
                tint = if (e.starred) p.warning else p.text3)
        }
    }
}

/** Add or edit a line (the roadmap's 9 fields) or a STAR story. Edits keep the same entry: no duplicates. */
@Composable
fun EvidenceScreen(ui: UiState, act: Actions, nav: Nav, s: Screen.EvidenceEdit) {
    val p = LocalPalette.current
    val old = s.id?.let { id -> ui.s.evidence.firstOrNull { it.id == id } }
    val task = (old?.itemId ?: s.itemId)?.let { ui.plan.task(it) }
    var e by remember(s) {
        mutableStateOf(old ?: Evidence(0, s.day, task?.area?.takeIf { it != "evidence" && it != "review" } ?: "evidence", s.itemId,
            if (s.story) "star" else "line", "", "", "", "", "", "", "", null, false, "", "", "", "", 0, 0))
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val warning = Privacy.warning(e.text)
    Column(Modifier.fillMaxSize()) {
        TopBar(if (e.isStory) "STAR story" else if (old == null) "New log line" else "Log line", nav.back) {
            if (old != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete", tint = p.text2) }
        }
        LazyColumn(contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                ChipRow {
                    listOf(ui.today, ui.today - 1, ui.today - 2).plus(e.day).distinct().sortedDescending().forEach { d ->
                        Chip(when (d) { ui.today -> "Today"; ui.today - 1 -> "Yesterday"; else -> Days.short(d) }, e.day == d, { e = e.copy(day = d) })
                    }
                }
            }
            item {
                Text("Area", style = Type.smallStrong, color = p.text2)
                ChipRow { (AREA_ORDER).forEach { a -> Chip(ui.areaShort(a), e.area == a, { e = e.copy(area = a) }, dot = a) } }
            }
            if (warning != null) item { Banner(warning, Tone.DANGER, title = "Privacy check") }
            if (!e.isStory) {
                item { Field(e.task, { e = e.copy(task = it) }, "Task (e.g. Vendor upload)") }
                item { Field(e.problem, { e = e.copy(problem = it) }, "Problem (e.g. About 40 rows rejected)", minLines = 2) }
                item { Field(e.action, { e = e.copy(action = it) }, "Action: what you did", minLines = 2) }
                item {
                    Field(e.result, { e = e.copy(result = it) }, "Result (with a number if you can)", minLines = 2,
                        supporting = if (e.result.isNotBlank() && !e.hasRealResult) "Write what changed, not just “worked on X”." else null, isError = true)
                }
                item { Field(e.skill, { e = e.copy(skill = it) }, "Skill shown (e.g. Data checking)") }
                item { Field(e.proof, { e = e.copy(proof = it) }, "Evidence (e.g. Upload report, screenshot name)") }
                item { Field(e.lesson, { e = e.copy(lesson = it) }, "Lesson", minLines = 2) }
                item {
                    Text("Confidence (optional)", style = Type.smallStrong, color = p.text2)
                    ChipRow { (1..5).forEach { n -> Chip("$n", e.confidence == n, { e = e.copy(confidence = if (e.confidence == n) null else n) }) } }
                }
                item {
                    Chip(if (e.starred) "★ Best line of the week" else "☆ Star as the week's best line", e.starred, { e = e.copy(starred = !e.starred) })
                }
            } else {
                item { Muted("Build it from this month's starred lines. Situation (1 line) · Task (1) · Action (2–3) · Result (1, with a number). Under 150 words.") }
                val month = Days.date(e.day).monthValue
                val stars = ui.s.evidence.filter { it.starred && !it.isStory && Days.date(it.day).monthValue == month }
                if (stars.isNotEmpty()) item {
                    LabCard(tint = p.surface2) {
                        Text("Your ★ lines this month", style = Type.smallStrong, color = p.text2)
                        stars.forEach { Muted("• ${Days.dayMonth(it.day)}: ${it.text}", maxLines = 2) }
                    }
                }
                item { Field(e.starS, { e = e.copy(starS = it) }, "Situation (1 line)", minLines = 2) }
                item { Field(e.starT, { e = e.copy(starT = it) }, "Task (1 line)", minLines = 2) }
                item { Field(e.starA, { e = e.copy(starA = it) }, "Action (2–3 lines)", minLines = 3) }
                item { Field(e.starR, { e = e.copy(starR = it) }, "Result (1 line, with a number)", minLines = 2) }
                item {
                    val c = StarCheck.check(e)
                    LabCard {
                        Text("STAR check: ${c.passed} of ${c.total}", style = Type.section, color = if (c.ok) p.success else p.text)
                        c.criteria.forEach { (label, ok) -> Text((if (ok) "✓ " else "○ ") + label, style = Type.small, color = if (ok) p.success else p.text2) }
                    }
                }
            }
            item {
                PrimaryButton("Save", {
                    act.saveEvidence(e) { }
                    if (e.isStory && task != null) {
                        val c = StarCheck.check(e)
                        act.addAssessment(Assessment(0, task.id, task.area, Topics.of(ui.plan, task), "star", c.passed.toDouble(), c.total.toDouble(),
                            c.ok, true, JSONObject().put("words", StarCheck.words(listOf(e.starS, e.starT, e.starA, e.starR).joinToString(" "))).toString(),
                            ui.today, 0, 0))
                    }
                    nav.back()
                }, Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = if (e.isStory) e.starS.isNotBlank() || e.starR.isNotBlank() else listOf(e.task, e.problem, e.action, e.result).any { it.isNotBlank() })
            }
        }
    }
    if (confirmDelete && old != null) ConfirmDialog("Delete this entry?", "It's removed from the log and from future backups.", "Delete",
        { act.deleteEvidence(old.id); confirmDelete = false; nav.back() }, { confirmDelete = false }, danger = true)
}
