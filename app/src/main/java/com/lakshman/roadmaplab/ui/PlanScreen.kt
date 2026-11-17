package com.lakshman.roadmaplab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lakshman.roadmaplab.domain.AREA_ORDER
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.ui.components.AreaLabel
import com.lakshman.roadmaplab.ui.components.Chip
import com.lakshman.roadmaplab.ui.components.Divider
import com.lakshman.roadmaplab.ui.components.LabCard
import com.lakshman.roadmaplab.ui.components.Muted
import com.lakshman.roadmaplab.ui.components.ScreenPadding
import com.lakshman.roadmaplab.ui.components.SecondaryButton
import com.lakshman.roadmaplab.ui.components.SectionTitle
import com.lakshman.roadmaplab.ui.theme.LocalPalette
import com.lakshman.roadmaplab.ui.theme.Type

/**
 * The whole plan, week by week, straight from the CSV. Tick anything you've already done:
 * the plan's rule is "already ahead? tick it and carry on".
 */
@Composable
fun PlanScreen(ui: UiState, act: Actions, nav: Nav) {
    val p = LocalPalette.current
    val current = ui.plan.weekOf(ui.today)?.coerceIn(0, 11) ?: if (ui.today < ui.plan.leadIn) 0 else 11
    var week by rememberSaveable { mutableIntStateOf(current) }
    var area by rememberSaveable { mutableStateOf<String?>(null) }
    val tasks = ui.plan.tasks.filter { it.week == week && (area == null || it.area == area) }
    val done = tasks.count { ui.states[it.id]?.progress == Progress.COMPLETED }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp)) {
                Text("Plan", style = Type.display, color = p.text)
                Muted("${ui.plan.tasks.size} tasks from ${ui.plan.meta.csvName} · follow the order, not the dates")
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = ScreenPadding), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0..11).toList()) { w -> Chip(if (w == 0) "Lead-in" else if (w == current) "Week $w (now)" else "Week $w", week == w, { week = w }) }
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = ScreenPadding), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip("All categories", area == null, { area = null }) }
                items(AREA_ORDER + "review") { a -> Chip(ui.areaShort(a), area == a, { area = if (area == a) null else a }, dot = a) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = ScreenPadding)) {
                val title = if (week == 0) "Optional lead-in · ${Days.short(ui.plan.leadIn)}" else
                    "Week $week · ${Days.short(maxOf(ui.plan.start, ui.plan.weekStartOf(week)))} – ${Days.short(ui.plan.weekStartOf(week) + 6)}"
                Text(title, style = Type.section, color = p.text)
                Muted(listOfNotNull(ui.plan.meta.weekFocus[week], "$done of ${tasks.size} done").joinToString(" · "))
            }
        }
        val targets = ui.plan.meta.checkpoints[week]
        if (targets != null && area == null) item {
            LabCard(Modifier.padding(horizontal = ScreenPadding)) {
                Text("This week's targets (checkpoint)", style = Type.section, color = p.text)
                AREA_ORDER.forEach { a ->
                    targets[a]?.let { text ->
                        Row(Modifier.padding(top = 6.dp)) {
                            AreaLabel(a, ui.areaShort(a), Modifier.padding(end = 8.dp))
                            Text(text, style = Type.small, color = p.text, modifier = Modifier.weight(1f))
                        }
                    }
                }
                SecondaryButton("Open the week $week review", { nav.open(Screen.Weekly(week)) }, Modifier.fillMaxWidth().padding(top = 10.dp),
                    icon = Icons.Outlined.FactCheck)
            }
        }
        for ((day, list) in tasks.groupBy { it.day }) {
            item {
                Column(Modifier.padding(horizontal = ScreenPadding)) {
                    val dayMinutes = list.filter { it.study }.sumOf { it.minutes }
                    SectionTitle(Days.short(day) + if (day == ui.today) " · today" else "",
                        trailing = if (ui.plan.restDays.containsKey(day)) "rest day" else if (dayMinutes > 0) "$dayMinutes min study" else null)
                    LabCard(padding = PaddingValues(0.dp)) {
                        list.forEachIndexed { k, t ->
                            if (k > 0) Divider(Modifier.padding(start = 56.dp))
                            TaskLine(ui, t, { nav.open(Screen.Task(t.id)) }, { toggleTask(ui, act, t, ui.states[t.id]?.progress == Progress.COMPLETED) })
                        }
                    }
                }
            }
        }
    }
}
