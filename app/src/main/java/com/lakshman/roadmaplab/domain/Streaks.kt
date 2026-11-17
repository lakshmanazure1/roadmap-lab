package com.lakshman.roadmaplab.domain

/** How one category did on one day. */
enum class CatStatus {
    /** You did something in this category that day (any of its tasks, or its reduced-routine action). */
    HIT,
    /** It had a task that day and nothing was done. */
    MISSED,
    /** Missed, but forgiven: the first miss in a Monday–Sunday week of a near-daily category. The streak pauses. */
    GRACE,
    /** Today, still open: never a miss. */
    OPEN,
    /** Nothing planned for this category that day (or a rest day, or before your fresh start): neutral. */
    OFF,
    FUTURE,
}

data class CatDay(val day: Long, val status: CatStatus)

/** One category's streak and percentages (the header on Today, and the category screen). */
data class CategoryStats(
    val area: String,
    /** Days in a row with this category done (days it has nothing planned don't break it). */
    val current: Int,
    val longest: Int,
    val hitToday: Boolean,
    /** It has something planned today (or its reduced-routine action). */
    val plannedToday: Boolean,
    /** Percent of its planned days that were done, over the last 7 / 28 days (null with no planned days yet). */
    val rolling7: Double?,
    val rolling28: Double?,
    val total: Int,
    val completed: Int,
    /** Tasks due by today (planned on or before today; after your fresh start for day-bound ones). */
    val due: Int,
    val dueDone: Int,
    /** Its planned days so far, and how many were done (all-time "most days" measure, used for habits). */
    val daysPlanned: Int,
    val daysDone: Int,
    /** Every day from the first day that counts to the plan's end. */
    val days: List<CatDay>,
    /** A near-daily category: one missed day a week is forgiven. */
    val graceRule: Boolean,
) {
    /** Share of all its tasks done, 0–100. */
    val pct: Double get() = if (total == 0) 0.0 else 100.0 * completed / total
    /** Share of the tasks due so far that are done, 0–100 (null before anything is due). */
    val onTrackPct: Double? get() = if (due == 0) null else 100.0 * dueDone / due
    /** Share of its planned days you did it, 0–100 (null before any day). */
    val daysPct: Double? get() = if (daysPlanned == 0) null else 100.0 * daysDone / daysPlanned
    fun on(day: Long): CatStatus? = days.firstOrNull { it.day == day }?.status
}

/**
 * Per-category streaks. Transparent rules, the same spirit as the overall streak:
 *  1. A day counts for a category when you completed any of its tasks that day (credited to the day you did
 *     it, including days you tick later with "I did it yesterday"), or ticked its reduced-routine action.
 *  2. Days the category has nothing planned are neutral, as are planned rest days, days before your fresh
 *     start and reduced-routine days with no action for it. Doing it on such a day still counts.
 *  3. Near-daily categories (3 or more planned days that week): the first miss in a Monday–Sunday week is
 *     forgiven; the streak pauses instead of resetting. Weekly ones (SAP, Career, Review) have no grace.
 *  4. Today never breaks a streak while it is open. Nothing is deleted: the longest streak stays.
 */
object Streaks {
    fun all(
        plan: DayPlan,
        states: Map<String, ItemState>,
        checks: List<RoutineCheck>,
        meta: Map<Long, DayMeta>,
        reducedWeeks: Set<Long>,
        today: Long,
    ): Map<String, CategoryStats> {
        // Credit days per category: completed tasks (on their done day), partial work, and reduced-routine ticks.
        val credit = HashMap<String, HashSet<Long>>()
        for (s in states.values) {
            val t = plan.task(s.itemId) ?: continue
            val set = credit.getOrPut(t.area) { HashSet() }
            if (s.progress == Progress.COMPLETED && s.doneDay != null) set += s.doneDay
            set += s.partMinutes.filterValues { it > 0 }.keys
        }
        val reducedArea = plan.meta.reduced.associate { it.id to it.area }
        for (c in checks) {
            if (!c.done) continue
            val area = reducedArea[c.routineId] ?: if (c.routineId == TodayEngine.FLASHCARDS) "accounting" else null
            if (area != null) credit.getOrPut(area) { HashSet() } += c.day
        }
        val byAreaDay = plan.tasks.filter { it.kind != TaskKind.REST && !it.optional }.groupBy { it.area }
            .mapValues { (_, l) -> l.mapTo(HashSet()) { it.day } }
        return CATEGORY_ORDER.associateWith { area ->
            stats(plan, area, states, credit[area].orEmpty(), byAreaDay[area].orEmpty(), meta, reducedWeeks, today)
        }
    }

    fun stats(
        plan: DayPlan,
        area: String,
        states: Map<String, ItemState>,
        credit: Set<Long>,
        plannedDays: Set<Long>,
        meta: Map<Long, DayMeta>,
        reducedWeeks: Set<Long>,
        today: Long,
    ): CategoryStats {
        val first = plan.firstDay
        val reducedDays = plan.meta.reduced.filter { it.area == area }.flatMap { it.days }.toSet()
        fun planned(d: Long): Boolean = when (Consistency.dayType(d, plan, meta[d])) {
            DayType.NONE, DayType.REST -> false
            DayType.REDUCED -> Days.dow(d) in reducedDays
            else -> d in plannedDays
        }
        // Grace applies in weeks where the category is planned on 3 or more days.
        fun weekCount(ws: Long) = (ws..ws + 6).count { it in plannedDays }
        val graceRule = plannedDays.groupBy { Days.weekStart(it) }.values.map { it.size }.let { s -> s.isNotEmpty() && s.sorted()[s.size / 2] >= 3 }
        val graceWeeks = HashSet<Long>()
        val days = ArrayList<CatDay>()
        var d = first
        while (d <= plan.end) {
            val type = Consistency.dayType(d, plan, meta[d])
            val status = when {
                d > today -> CatStatus.FUTURE
                d in credit -> CatStatus.HIT
                !planned(d) -> CatStatus.OFF
                d == today -> CatStatus.OPEN
                // A catch-up day is for catching up: not doing this category then is never a miss.
                type == DayType.CATCHUP -> CatStatus.OFF
                weekCount(Days.weekStart(d)) >= 3 && graceWeeks.add(Days.weekStart(d)) -> CatStatus.GRACE
                else -> CatStatus.MISSED
            }
            days += CatDay(d, status)
            d++
        }
        var current = 0
        for (r in days.asReversed()) {
            if (r.day > today) continue
            if (r.status == CatStatus.HIT) current++ else if (r.status == CatStatus.MISSED) break
        }
        var longest = 0
        var run = 0
        for (r in days) {
            if (r.day > today) break
            when (r.status) {
                CatStatus.HIT -> { run++; longest = maxOf(longest, run) }
                CatStatus.MISSED -> run = 0
                else -> {}
            }
        }
        val hitToday = today in credit
        fun rolling(n: Int): Double? {
            val end = if (hitToday) today else today - 1
            val window = days.filter { it.day in (end - n + 1)..end && planned(it.day) }
            if (window.isEmpty()) return null
            return 100.0 * window.count { it.status == CatStatus.HIT } / window.size
        }
        val mine = plan.tasks.filter { it.area == area && it.kind != TaskKind.REST && !it.optional }
        fun done(t: PlanTask) = states[t.id]?.progress == Progress.COMPLETED
        val due = mine.filter { t ->
            TodayEngine.effectiveDay(t, reducedWeeks) <= today && !(t.kind == TaskKind.DAILY && t.day < first)
        }
        // Planned days so far; today only once it's done (an open day isn't a miss).
        val plannedSoFar = days.filter { planned(it.day) && (it.day < today || (it.day == today && hitToday)) }
        return CategoryStats(
            area = area, current = current, longest = longest, hitToday = hitToday,
            plannedToday = today in first..plan.end && planned(today),
            rolling7 = rolling(7), rolling28 = rolling(28),
            total = mine.size, completed = mine.count(::done), due = due.size, dueDone = due.count(::done),
            daysPlanned = plannedSoFar.size, daysDone = plannedSoFar.count { it.status == CatStatus.HIT },
            days = days, graceRule = graceRule,
        )
    }

    /**
     * Yesterday's planned tasks you haven't ticked (and the days before, up to [back] days, after the first day
     * that counts): "I did it, I just didn't tick it". Ticking one credits the day it was planned.
     */
    fun untickedRecent(plan: DayPlan, states: Map<String, ItemState>, today: Long, back: Int = 1): List<PlanTask> {
        val from = maxOf(plan.firstDay, today - back)
        fun closed(id: String) = states[id]?.progress.let { it == Progress.COMPLETED || it == Progress.SKIPPED }
        // Numbered work: only the next one in its order (if you're a step behind, yesterday's step isn't due yet).
        val frontier = plan.sequences.values.mapNotNullTo(HashSet()) { l -> l.firstOrNull { !closed(it.id) }?.id }
        return plan.tasks.filter { t ->
            t.day in from until today && t.kind != TaskKind.REST && !t.optional && !closed(t.id) &&
                (t.kind != TaskKind.SEQUENCE || t.id in frontier)
        }.sortedWith(compareBy({ it.day }, { CATEGORY_ORDER.indexOf(it.area) }, { it.order }))
    }
}
