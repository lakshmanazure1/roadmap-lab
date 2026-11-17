package com.lakshman.roadmaplab.domain

import kotlin.math.roundToLong

/** Something recorded on one day that belongs to a category, shown under that date. */
data class DayRecord(val kind: Kind, val text: String, val itemId: String? = null) {
    enum class Kind(val label: String) {
        EVIDENCE("Evidence"),
        STORY("STAR story"),
        CHECK("Check"),
        ROUTINE("Reduced routine"),
        DAY_NOTE("Day note"),
    }
}

/** One date in one category: what was planned, how it went, what else was done, and the notes for it. */
data class CategoryDay(
    val day: Long,
    /** How the category did that day (null before your first counted day). */
    val status: CatStatus?,
    /** Rest, catch-up or reduced-routine day, if you set one. */
    val dayKind: DayKind?,
    /** Its tasks planned for this date (moved by reduced weeks, as on Today). */
    val planned: List<PlanTask>,
    /** Its tasks planned for another date but done, or worked on, this date. */
    val alsoDone: List<PlanTask>,
    val records: List<DayRecord>,
)

/**
 * The category screen's day-by-day list. A date is listed when the category has something planned on it,
 * or when you did or wrote something for the category that date (a task done early or late, evidence,
 * a check, a reduced-routine tick). The day's own note is shown under every category listed that date,
 * but never adds a date by itself. Pure: no database, so it is easy to test.
 */
object DayHistory {
    /** Notes the app writes by itself; they are not shown as your notes. */
    val AUTO_NOTES = setOf("Ticked when you saved the log line", "Weekly review completed")

    fun build(
        plan: DayPlan,
        area: String,
        states: Map<String, ItemState>,
        evidence: List<Evidence>,
        assessments: List<Assessment>,
        ticks: List<RoutineCheck>,
        dayLogs: Map<Long, DayLog>,
        dayMeta: Map<Long, DayMeta>,
        reducedWeeks: Set<Long>,
        stats: CategoryStats?,
        today: Long,
    ): List<CategoryDay> {
        val tasks = plan.tasks.filter { it.area == area }
        val planned = tasks.groupBy { TodayEngine.effectiveDay(it, reducedWeeks) }

        val also = HashMap<Long, LinkedHashSet<PlanTask>>()
        for (t in tasks) {
            val home = TodayEngine.effectiveDay(t, reducedWeeks)
            val st = states[t.id] ?: continue
            val worked = st.partMinutes.filterValues { it > 0 }.keys +
                listOfNotNull(st.doneDay.takeIf { st.progress == Progress.COMPLETED })
            for (d in worked) if (d != home && d <= today) also.getOrPut(d) { LinkedHashSet() } += t
        }

        val own = HashMap<Long, MutableList<DayRecord>>()
        fun add(d: Long, r: DayRecord) { own.getOrPut(d) { ArrayList() } += r }

        for (e in evidence.sortedBy { it.createdAt }) {
            val belongs = area == "evidence" || e.area == area || (e.itemId != null && plan.task(e.itemId)?.area == area)
            if (!belongs || e.text.isBlank()) continue
            val star = if (e.starred) "★ " else ""
            add(e.day, DayRecord(if (e.isStory) DayRecord.Kind.STORY else DayRecord.Kind.EVIDENCE, star + e.text, e.itemId))
        }
        for (a in assessments.filter { it.area == area }.sortedBy { it.createdAt }) {
            val what = a.itemId?.let { plan.task(it)?.title } ?: a.topic
            val result = if (a.max > 0) "${num(a.score)}/${num(a.max)}" else ""
            val verdict = if (a.passed) "passed" else "not yet"
            add(a.day, DayRecord(DayRecord.Kind.CHECK, listOf(what, result, verdict).filter { it.isNotBlank() }.joinToString(" · "), a.itemId))
        }
        val reduced = plan.meta.reduced.associateBy { it.id }
        for (c in ticks) {
            if (!c.done) continue
            val r = reduced[c.routineId]
            val text = when {
                r != null && r.area == area -> r.action
                // The same mapping the streaks use for the daily flashcard tick.
                r == null && c.routineId == TodayEngine.FLASHCARDS && area == "accounting" -> "Flashcard practice"
                else -> continue
            }
            add(c.day, DayRecord(DayRecord.Kind.ROUTINE, text + if (c.minutes > 0) " · ${c.minutes} min" else ""))
        }

        val dates = (planned.keys + also.keys + own.keys).toSortedSet()
        return dates.map { d ->
            val note = dayLogs[d]?.note?.trim().orEmpty()
            CategoryDay(
                day = d,
                status = stats?.on(d),
                dayKind = dayMeta[d]?.kind,
                planned = planned[d].orEmpty(),
                alsoDone = also[d]?.toList().orEmpty(),
                records = own[d].orEmpty() + listOfNotNull(note.takeIf { it.isNotEmpty() }?.let { DayRecord(DayRecord.Kind.DAY_NOTE, it) }),
            )
        }
    }

    /** Notes you wrote on a task (not the ones the app adds), oldest first. */
    fun taskNotes(events: List<ItemEvent>): Map<String, List<ItemEvent>> =
        events.filter { it.note.isNotBlank() && it.note !in AUTO_NOTES }.sortedBy { it.createdAt }.groupBy { it.itemId }

    private fun num(x: Double): String = if (x == x.roundToLong().toDouble()) x.roundToLong().toString() else "%.1f".format(x)
}
