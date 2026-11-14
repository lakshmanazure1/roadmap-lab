package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.AREA_ORDER
import com.lakshman.roadmaplab.domain.Analytics
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.WeeklyReview
import com.lakshman.roadmaplab.ui.components.AreaLabel
import com.lakshman.roadmaplab.ui.components.Banner
import com.lakshman.roadmaplab.ui.components.Chip
import com.lakshman.roadmaplab.ui.components.ChipRow
import com.lakshman.roadmaplab.ui.components.Field
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.PrimaryButton
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SecondaryButton
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.components.Stat
import com.lakshman.roadmaplab.ui.components.Tag
import com.lakshman.roadmaplab.ui.components.Tone
import com.lakshman.roadmaplab.ui.components.TopBar
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type

private val OBSTACLES = listOf("Go-live work ran late", "Low energy", "No internet", "Travel", "Family", "Unwell", "Task unclear", "Too much planned")

/**
 * The Sunday checkpoint table (roadmap section 11): the week's numbers are filled in for you; you write
 * what actually happened per area, what got in the way, and next week's load. Honest, not just ticks.
 */
@Composable
fun WeeklyReviewScreen(ui: UiState, act: Actions, nav: Nav, week: Int) {
    val p = LocalPalette.current
    val plan = ui.plan
    val ws = plan.weekStartOf(week)
    val prev = if (week > 1) Analytics.weekSummary(plan, week - 1, ui.states, ui.s.assessments, ui.s.evidence, ui.consistency.days, ui.sleep,
        ui.reducedWeeks, null, ui.today) else null
    val sum = Analytics.weekSummary(plan, week, ui.states, ui.s.assessments, ui.s.evidence, ui.consistency.days, ui.sleep, ui.reducedWeeks, prev, ui.today)
    val old = ui.s.weeklyReviews[ws]
    val actual = remember(ws) { mutableStateMapOf<String, String>().apply { old?.actual?.let { putAll(it) } } }
    val assess = remember(ws) { mutableStateMapOf<String, String>().apply { old?.assessment?.let { putAll(it) } } }
    val adjust = remember(ws) { mutableStateMapOf<String, String>().apply { old?.adjustment?.let { putAll(it) } } }
    var missed by remember(ws) { mutableStateOf(old?.missedWhy ?: "") }
    var tags by remember(ws) { mutableStateOf(old?.obstacleTags ?: emptyList()) }
    var best by remember(ws) { mutableStateOf(old?.best ?: sum.best?.let { "${it.task}: ${it.result}" } ?: "") }
    var weakest by remember(ws) { mutableStateOf(old?.weakest ?: sum.weakestArea?.let { plan.area(it)?.short } ?: "") }
    var priorities by remember(ws) { mutableStateOf(old?.priorities ?: "") }
    var load by remember(ws) { mutableStateOf(old?.nextLoad ?: "normal") }
    var spending by remember(ws) { mutableStateOf(old?.spending ?: "") }
    fun review() = WeeklyReview(ws, actual.toMap(), assess.toMap(), adjust.toMap(), missed, tags, best, weakest, priorities, load, spending, old?.completedAt, 0)

    Column(Modifier.fillMaxSize()) {
        TopBar("Week $week review", nav.back)
        LazyColumn(contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Muted("${Days.short(ws)} – ${Days.short(ws + 6)} · ${plan.meta.weekFocus[week] ?: ""} · 15 minutes. " +
                    "A ticked task with a weak result is written as weak.")
            }
            if (old?.completedAt != null) item { Banner("Completed ${Days.short(Days.dayOf(old.completedAt))}. You can still edit it.", Tone.SUCCESS) }
            item {
                LabCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stat("Done / planned", "${sum.completed}/${sum.planned}", Modifier.weight(1f), sub = "tasks")
                        Stat("Days with the minimum", "${sum.qualifiedDays}/${sum.scheduledDays}", Modifier.weight(1f))
                        Stat("Checks", if (sum.avgScore != null) "%.0f%%".format(sum.avgScore) else "—", Modifier.weight(1f), sub = "${sum.checks} this week")
                    }
                    val h = sum.habits
                    Muted("Study ${sum.minutes} min · slept 7 h on ${h.sleepNights} nights · exercise ${h.exercise}/${h.exercisePlanned}" +
                        (h.calledHome?.let { if (it) " · called home" else " · no call ticked" } ?: ""), Modifier.padding(top = 8.dp))
                }
            }
            if (sum.suggestions.isNotEmpty()) item {
                LabCard(tint = p.accentSoft, border = p.accentSoft) {
                    Text("Suggestions from the plan's rules", style = Type.section, color = p.text)
                    sum.suggestions.forEach { Text("• $it", style = Type.small, color = p.text, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
            item { SectionTitle("Checkpoint table") }
            val targets = plan.meta.checkpoints[week].orEmpty()
            AREA_ORDER.forEach { a ->
                item {
                    LabCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AreaLabel(a, plan.area(a)?.short ?: a, Modifier.weight(1f))
                            sum.byArea[a]?.let { (d, n) -> Tag("$d/$n done", if (d == n) Tone.SUCCESS else Tone.NEUTRAL) }
                        }
                        targets[a]?.let { Text("Target: $it", style = Type.small, color = p.text, modifier = Modifier.padding(top = 4.dp)) }
                        Spacer(Modifier.padding(top = 6.dp))
                        Field(actual[a] ?: "", { actual[a] = it }, "Actual result")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            Field(assess[a] ?: "", { assess[a] = it }, "Assessment", Modifier.weight(1f))
                            Field(adjust[a] ?: "", { adjust[a] = it }, "Next week", Modifier.weight(1f))
                        }
                    }
                }
            }
            if (sum.repeatedlyMissed.isNotEmpty()) item {
                LabCard {
                    Text("Waiting for a while", style = Type.section, color = p.text)
                    sum.repeatedlyMissed.forEach { t -> Muted("${t.id} · ${t.title} (planned ${Days.short(t.day)})", Modifier.padding(top = 2.dp)) }
                    Muted("They wait in order. Don't double up: one a day is enough.", Modifier.padding(top = 4.dp))
                }
            }
            item {
                Text("Missed, and why", style = Type.section, color = p.text)
                ChipRow { OBSTACLES.forEach { o -> Chip(o, o in tags, { tags = if (o in tags) tags - o else tags + o }) } }
                Spacer(Modifier.padding(top = 6.dp))
                Field(missed, { missed = it }, "What got in the way (optional)", minLines = 2)
            }
            item { Field(best, { best = it }, "Best achievement of the week", minLines = 2) }
            item { Field(weakest, { weakest = it }, "Weakest area" + (sum.weakestArea?.let { " (by the numbers: ${plan.area(it)?.short})" } ?: "")) }
            item { Field(priorities, { priorities = it }, "Top 3 priorities next week", minLines = 3) }
            item { Field(spending, { spending = it }, "The week's spending total", singleLine = true) }
            item {
                Text("Next week's load", style = Type.section, color = p.text)
                ChipRow {
                    Chip("Normal", load == "normal", { load = "normal" })
                    Chip("Reduced routine (about 20 min a day)", load == "reduced", { load = "reduced" })
                }
                Muted("Reduced weeks never break your streak. Two in a row move later dates by a week; the order stays.", Modifier.padding(top = 4.dp))
            }
            if (sum.nextWeek.isNotEmpty()) item {
                LabCard(tint = p.surface2) {
                    Text("Coming next week", style = Type.smallStrong, color = p.text2)
                    sum.nextWeek.forEach { t -> Muted("${t.id} · ${t.title}", maxLines = 1) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Save draft", { act.saveWeeklyReview(review(), false); act.note(Note("Draft saved")) }, Modifier.weight(1f))
                    PrimaryButton("Complete", { act.saveWeeklyReview(review(), true); act.note(Note("Week $week review completed")); nav.back() },
                        Modifier.weight(1f))
                }
            }
        }
    }
}
