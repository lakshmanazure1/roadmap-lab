package com.lakshman.roadmaplab.domain

/**
 * Overall progress = completed task minutes ÷ total planned minutes (full minutes, so the
 * mode doesn't change your %). Done counts fully, partly done counts half.
 *
 * "You" = the last day whose planned work you have fully covered. Compared with today:
 *  - You ≥ today: on plan (0) or ahead (+days);
 *  - You = yesterday: on plan (today's work is still open);
 *  - earlier: behind by (yesterday − You) days.
 */
data class ProgressInfo(
    val percent: Double,
    val doneMinutes: Double,
    val totalMinutes: Int,
    val youDay: Long,
    val gapDays: Int,
    val trackPercent: Map<String, Double>,
    val nextMilestone: Milestone?,
    val daysToMilestone: Int?,
) {
    val label: String
        get() = when {
            gapDays > 0 -> "${plural(gapDays, "day")} ahead"
            gapDays < 0 -> "${plural(-gapDays, "day")} behind"
            else -> "On plan"
        }

    /** Beyond 14 days behind, the app offers the reduced plan instead of turning red. */
    val offerReducedPlan: Boolean get() = gapDays < -14
}

fun plural(n: Int, one: String, many: String = one + "s"): String = "$n ${if (n == 1) one else many}"

object Progress {
    fun credit(status: TaskStatus?): Double = when (status) {
        TaskStatus.DONE -> 1.0
        TaskStatus.PARTLY -> 0.5
        else -> 0.0
    }

    fun compute(
        tasks: List<PlanTask>,
        latest: Map<String, TaskLog>,
        milestones: List<Milestone>,
        today: Long,
        reducedPlan: Boolean,
    ): ProgressInfo {
        val active = tasks.filter { Planner.isActive(it, reducedPlan) }
        val total = active.sumOf { it.minFull }
        val done = active.sumOf { it.minFull * credit(latest[it.id]?.status) }
        val you = youDay(active, done)
        val byTrack = active.groupBy { it.track }.mapValues { (_, ts) ->
            val t = ts.sumOf { it.minFull }
            if (t == 0) 0.0 else 100.0 * ts.sumOf { it.minFull * credit(latest[it.id]?.status) } / t
        }
        val next = milestones.filter { it.day >= today }.minByOrNull { it.day }
        return ProgressInfo(
            percent = if (total == 0) 0.0 else 100.0 * done / total,
            doneMinutes = done,
            totalMinutes = total,
            youDay = you,
            gapDays = gap(you, today),
            trackPercent = byTrack,
            nextMilestone = next,
            daysToMilestone = next?.let { (it.day - today).toInt() },
        )
    }

    /** The last day d such that all minutes planned on or before d are covered by [doneMinutes]. */
    fun youDay(active: List<PlanTask>, doneMinutes: Double): Long {
        if (active.isEmpty()) return Days.today()
        val perDay = java.util.TreeMap<Long, Int>()
        for (t in active) perDay[t.plannedDay] = (perDay[t.plannedDay] ?: 0) + t.minFull
        var cumulative = 0.0
        for ((day, minutes) in perDay) {
            cumulative += minutes
            if (cumulative > doneMinutes + 1e-6) return day - 1
        }
        return perDay.lastKey()
    }

    fun gap(youDay: Long, today: Long): Int = when {
        youDay >= today -> (youDay - today).toInt()
        youDay == today - 1 -> 0
        else -> -((today - 1) - youDay).toInt()
    }

    /** Planned cumulative % at the end of each given day (for the timeline line). */
    fun plannedPercentAt(active: List<PlanTask>, day: Long): Double {
        val total = active.sumOf { it.minFull }
        if (total == 0) return 0.0
        return 100.0 * active.filter { it.plannedDay <= day }.sumOf { it.minFull } / total
    }
}
