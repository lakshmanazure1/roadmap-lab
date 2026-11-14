package com.lakshman.roadmaplab.domain

/**
 * Chooses each day's tasks. Rules from the brief:
 *  - at most 3 tasks a day, sized to the mode's capacity (the first task always fits);
 *  - unfinished tasks move to the next free slot, oldest first, never more than 3 on a day;
 *  - after 3+ missed days, a Restart view shows only the next 3 priorities (core tasks first).
 */
object Planner {
    const val MAX_TASKS_PER_DAY = 3
    const val RESTART_AFTER_MISSED_DAYS = 3

    /** The most recent log of each task. */
    fun latestByTask(logs: List<TaskLog>): Map<String, TaskLog> {
        val out = HashMap<String, TaskLog>()
        for (log in logs) {
            val old = out[log.taskId]
            if (old == null || log.createdAt > old.createdAt || (log.createdAt == old.createdAt && log.id > old.id)) {
                out[log.taskId] = log
            }
        }
        return out
    }

    fun isActive(task: PlanTask, reducedPlan: Boolean): Boolean = !reducedPlan || task.core

    fun isDone(task: PlanTask, latest: Map<String, TaskLog>): Boolean = latest[task.id]?.status == TaskStatus.DONE

    /** Tasks still to do, planned on or before [day], in plan order. */
    fun pending(tasks: List<PlanTask>, latest: Map<String, TaskLog>, day: Long, reducedPlan: Boolean): List<PlanTask> =
        tasks.asSequence()
            .filter { isActive(it, reducedPlan) && !isDone(it, latest) && it.plannedDay <= day }
            .sortedWith(compareBy<PlanTask>({ it.plannedDay }, { it.order }))
            .toList()

    /** Picks the task ids for a fresh day. */
    fun planDay(
        tasks: List<PlanTask>,
        latest: Map<String, TaskLog>,
        day: Long,
        mode: Mode,
        reducedPlan: Boolean,
        restart: Boolean,
    ): List<String> {
        var candidates = pending(tasks, latest, day, reducedPlan)
        if (restart) candidates = candidates.sortedWith(compareBy<PlanTask>({ !it.core }, { it.plannedDay }, { it.order }))
        val chosen = ArrayList<String>()
        var used = 0
        for (t in candidates) {
            val m = t.minutes(mode)
            if (chosen.isEmpty() || used + m <= mode.capacityMinutes) {
                chosen.add(t.id)
                used += m
            }
            if (chosen.size == MAX_TASKS_PER_DAY) break
        }
        return chosen
    }

    /** The next task to pull into today when today's tasks are finished, or null if today is full. */
    fun nextToPull(
        tasks: List<PlanTask>,
        latest: Map<String, TaskLog>,
        todayIds: List<String>,
        reducedPlan: Boolean,
    ): PlanTask? {
        if (todayIds.size >= MAX_TASKS_PER_DAY) return null
        return tasks.asSequence()
            .filter { isActive(it, reducedPlan) && !isDone(it, latest) && it.id !in todayIds }
            .sortedWith(compareBy<PlanTask>({ it.plannedDay }, { it.order }))
            .firstOrNull()
    }

    /** Consecutive days with no logged action just before [today]. */
    fun missedDaysBefore(today: Long, activeDays: Set<Long>, firstUseDay: Long): Int {
        var n = 0
        var d = today - 1
        while (d >= firstUseDay && d !in activeDays) {
            n++
            d--
        }
        return n
    }

    fun needsRestart(today: Long, activeDays: Set<Long>, firstUseDay: Long): Boolean =
        missedDaysBefore(today, activeDays, firstUseDay) >= RESTART_AFTER_MISSED_DAYS
}
