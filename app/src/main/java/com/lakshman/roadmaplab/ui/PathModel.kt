package com.lakshman.roadmaplab.ui

import com.lakshman.roadmaplab.domain.Milestone
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.TaskStatus

/** One row on the path: a phase banner, a task node or a milestone. */
sealed interface PathItem {
    val key: String

    data class Banner(val phaseId: String, val title: String, val track: String) : PathItem {
        override val key get() = "banner-$phaseId"
    }

    data class Node(val task: PlanTask, val state: NodeState, val index: Int) : PathItem {
        override val key get() = "node-${task.id}"
    }

    data class Trophy(val milestone: Milestone, val index: Int) : PathItem {
        override val key get() = "ms-${milestone.id}"
    }
}

enum class NodeState { DONE, TODAY, TODAY_DONE, NEXT }


/** Builds the path: last 3 done, today's tasks, the next 10, with milestones and one banner per phase. */
fun buildPath(ui: UiState): List<PathItem> {
    val r = ui.s.roadmap ?: return emptyList()
    val reduced = ui.s.settings.reducedPlan
    val todayIds = ui.s.dayPlans[ui.today].orEmpty().toSet()
    val active = r.tasks.filter { Planner.isActive(it, reduced) }.sortedWith(compareBy({ it.plannedDay }, { it.order }))
    val done = active.filter { ui.latest[it.id]?.status == TaskStatus.DONE && it.id !in todayIds }
        .sortedBy { ui.latest[it.id]?.createdAt ?: 0 }
    val today = active.filter { it.id in todayIds }
    val upcoming = active.filter { ui.latest[it.id]?.status != TaskStatus.DONE && it.id !in todayIds }.take(10)
    val shown = done.takeLast(3) + today + upcoming

    val phases = r.phases.associateBy { it.id }
    val out = ArrayList<PathItem>()
    var lastPhase: String? = null
    val seenPhases = HashSet<String>()
    var i = 0
    val lastDay = shown.maxOfOrNull { it.plannedDay } ?: ui.today
    val milestones = r.milestones.filter { it.day >= ui.today && it.day <= lastDay + 1 }.toMutableList()
    for (t in shown) {
        while (milestones.isNotEmpty() && milestones.first().day < t.plannedDay && t.id !in todayIds && ui.latest[t.id]?.status != TaskStatus.DONE) {
            out.add(PathItem.Trophy(milestones.removeAt(0), i++))
        }
        if (t.phaseId != lastPhase && seenPhases.add(t.phaseId)) {
            val p = phases[t.phaseId]
            if (p != null) out.add(PathItem.Banner(p.id, p.title, p.track))
        }
        lastPhase = t.phaseId
        val todayStatus = ui.todayTasks.firstOrNull { it.task.id == t.id }?.todayStatus
        val state = when {
            t.id in todayIds && todayStatus != null && todayStatus != TaskStatus.SKIPPED -> NodeState.TODAY_DONE
            t.id in todayIds -> NodeState.TODAY
            ui.latest[t.id]?.status == TaskStatus.DONE -> NodeState.DONE
            else -> NodeState.NEXT
        }
        out.add(PathItem.Node(t, state, i++))
    }
    milestones.forEach { out.add(PathItem.Trophy(it, i++)) }
    return out
}

