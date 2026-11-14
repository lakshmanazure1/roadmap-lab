package com.lakshman.roadmaplab.domain

data class AreaProgress(
    val area: Area,
    val total: Int,
    /** Tasks planned on or before today. */
    val dueByToday: Int,
    val completed: Int,
    /** Planned on or before today, not done and not skipped. */
    val behind: Int,
    val next: PlanTask?,
    /** Level counts across the area's tasks (by Level). */
    val levels: Map<Level, Int>,
    /** Average of the latest score per task, or null with no scores yet. */
    val avgScore: Double?,
    val scoredTasks: Int,
) {
    val pct: Double get() = if (total == 0) 0.0 else 100.0 * completed / total
    val duePct: Double get() = if (total == 0) 0.0 else 100.0 * dueByToday / total
    val needsRevision: Int get() = levels[Level.NEEDS_REVISION] ?: 0
    fun atLeast(level: Level): Int = levels.filterKeys { it.number >= level.number && it != Level.NEEDS_REVISION && it != Level.SKIPPED }.values.sum()
}

data class WeekBar(
    val week: Int,
    val weekStart: Long,
    val planned: Int,
    val completed: Int,
    val minutes: Int,
    val qualifiedDays: Int,
    val scheduledDays: Int,
    val current: Boolean,
)

/** The health counts the roadmap measures (section 10). */
data class HabitWeek(
    val sleepNights: Int,
    val sleepAsked: Int,
    val exercise: Int,
    val exercisePlanned: Int,
    val aloneTime: Int,
    val dayOff: Boolean?,
    val calledHome: Boolean?,
)

enum class NextKind { TASK, CHECK, REVISE, PRACTICE, WEEKLY_REVIEW, RESUME, DONE, BEFORE_START, REST, REDUCED }

data class NextAction(val kind: NextKind, val title: String, val why: String, val key: String?)

data class WeekSummary(
    val week: Int,
    val weekStart: Long,
    val planned: Int,
    val completed: Int,
    val minutes: Int,
    val qualifiedDays: Int,
    val scheduledDays: Int,
    val checks: Int,
    val avgScore: Double?,
    val revision: List<String>,
    val best: Evidence?,
    val bestCheck: Assessment?,
    /** area id → done / planned this week. */
    val byArea: Map<String, Pair<Int, Int>>,
    val weakestArea: String?,
    val repeatedlyMissed: List<PlanTask>,
    val nextWeek: List<PlanTask>,
    val habits: HabitWeek,
    val suggestions: List<String>,
)

object Analytics {
    fun areaProgress(
        plan: DayPlan,
        states: Map<String, ItemState>,
        levels: Map<String, Level>,
        checks: List<Assessment>,
        reducedWeeks: Set<Long>,
        today: Long,
    ): List<AreaProgress> = AREA_ORDER.mapNotNull { id ->
        val area = plan.area(id) ?: return@mapNotNull null
        val mine = plan.tasks.filter { it.area == id && it.kind != TaskKind.REST }
        fun p(t: PlanTask) = states[t.id]?.progress
        val due = mine.filter { TodayEngine.effectiveDay(it, reducedWeeks) <= today }
        val next = mine.filter { p(it) != Progress.COMPLETED && p(it) != Progress.SKIPPED }
            .minWithOrNull(compareBy({ TodayEngine.effectiveDay(it, reducedWeeks) }, { it.order }))
        val scored = checks.filter { it.area == id && it.type in Mastery.SCORED_TYPES && it.max > 0 && it.itemId != null }
        val latest = scored.groupBy { it.itemId }.values.map { l -> l.maxBy { it.createdAt } }
        AreaProgress(
            area = area, total = mine.size, dueByToday = due.size, completed = mine.count { p(it) == Progress.COMPLETED },
            behind = due.count { p(it) != Progress.COMPLETED && p(it) != Progress.SKIPPED && it.kind != TaskKind.DAILY },
            next = next,
            levels = mine.groupingBy { levels[it.id] ?: Level.NOT_STARTED }.eachCount(),
            avgScore = if (latest.isEmpty()) null else latest.map { it.pct }.average(),
            scoredTasks = latest.size,
        )
    }

    fun weekBars(plan: DayPlan, states: Map<String, ItemState>, days: List<DayResult>, today: Long): List<WeekBar> = (1..11).map { w ->
        val ws = plan.weekStartOf(w)
        val range = ws..(ws + 6)
        val study = plan.tasks.filter { it.study && it.kind != TaskKind.REST && it.day in range }
        val dr = days.filter { it.day in range }
        WeekBar(
            week = w, weekStart = ws, planned = study.size,
            completed = states.values.count { s -> s.progress == Progress.COMPLETED && s.doneDay in range && plan.task(s.itemId)?.study == true },
            minutes = dr.filter { it.day <= today }.sumOf { it.minutes },
            qualifiedDays = dr.count { it.qualified }, scheduledDays = dr.count { it.scheduled }, current = today in range,
        )
    }

    data class MonthTotal(val label: String, val planned: Int, val completed: Int, val qualifiedDays: Int, val scheduledDays: Int)

    fun months(plan: DayPlan, states: Map<String, ItemState>, days: List<DayResult>): List<MonthTotal> =
        listOf(Triple("November", 2026, 11), Triple("December", 2026, 12), Triple("January", 2027, 1)).map { (label, y, m) ->
            fun inMonth(d: Long) = Days.date(d).let { it.year == y && it.monthValue == m }
            MonthTotal(
                label,
                plan.tasks.count { it.study && it.kind != TaskKind.REST && inMonth(it.day) },
                states.values.count { s -> s.progress == Progress.COMPLETED && s.doneDay?.let(::inMonth) == true && plan.task(s.itemId)?.study == true },
                days.count { inMonth(it.day) && it.qualified },
                days.count { inMonth(it.day) && it.scheduled },
            )
        }

    /** Sleep nights, exercise, alone time, day off and the call home for one week. */
    fun habits(plan: DayPlan, weekStart: Long, states: Map<String, ItemState>, sleep: Map<Long, Boolean>, today: Long): HabitWeek {
        val range = weekStart..minOf(weekStart + 6, today)
        val life = plan.tasks.filter { it.area == "life" && it.day in weekStart..(weekStart + 6) }
        fun done(t: PlanTask) = states[t.id]?.progress == Progress.COMPLETED
        val exercise = life.filter { it.build.startsWith("Exercise") }
        val alone = life.filter { it.build.contains("alone time") }
        val off = life.firstOrNull { it.learn == "Recovery" }
        val call = life.firstOrNull { it.learn == "Family" }
        return HabitWeek(
            sleepNights = range.count { sleep[it] == true },
            sleepAsked = range.count { sleep.containsKey(it) },
            exercise = exercise.count(::done), exercisePlanned = exercise.size,
            aloneTime = alone.count(::done),
            dayOff = off?.let(::done), calledHome = call?.let(::done),
        )
    }

    fun nextAction(
        plan: DayPlan,
        todayPlan: TodayPlan,
        levels: Map<String, Level>,
        checks: List<Assessment>,
        dueCards: Int,
        daysSinceActive: Int?,
        today: Long,
    ): NextAction {
        if (today < plan.leadIn) {
            return NextAction(NextKind.BEFORE_START, "Your plan starts ${Days.long(plan.leadIn)}",
                "Optional lead-in: T001 validate_gstin(), then Monday 16 Nov is day 1 (accounting 1.2, French week 1).", "T001")
        }
        if (todayPlan.type == DayType.REST) {
            return NextAction(NextKind.REST, "Rest day", "No study today. Call home or a friend; sleep in. It never breaks your streak.", null)
        }
        val open = todayPlan.main.filter { !it.done && !it.skipped }
        if (daysSinceActive != null && daysSinceActive >= 3 && open.isNotEmpty() && todayPlan.minutesDone == 0) {
            val e = open.first()
            return NextAction(NextKind.RESUME, "Welcome back. Pick up with ${label(e)}",
                "Don't double up: do the next task in order. Missed a week? Repeat the Monday quiz first.", e.key)
        }
        todayPlan.focusKey?.let { f -> open.firstOrNull { it.key == f }?.let { return NextAction(NextKind.TASK, "Start your focus: ${label(it)}", reasonLine(it), it.key) } }
        open.firstOrNull { it.task?.area == "review" }?.let {
            return NextAction(NextKind.WEEKLY_REVIEW, "Fill the week ${it.task?.week} checkpoint", "15 min: an actual result for every area, then next week's load.", it.key)
        }
        open.firstOrNull { it.priority }?.let { return NextAction(NextKind.TASK, "Start ${label(it)}", reasonLine(it), it.key) }
        // Done today but not checked: completion isn't mastery.
        val unchecked = todayPlan.main.filter { it.done }.mapNotNull { it.task }
            .firstOrNull { t -> t.gradedChecks.any { it is CheckSpec.Quiz || it is CheckSpec.Score || it is CheckSpec.Rubric } && checks.none { c -> c.itemId == t.id } }
        if (unchecked != null) {
            return NextAction(NextKind.CHECK, "Score ${unchecked.id}: ${unchecked.title}", "Done isn't the same as learned. Record the score, or take the check cold.", unchecked.id)
        }
        open.firstOrNull()?.let { return NextAction(if (todayPlan.type == DayType.REDUCED) NextKind.REDUCED else NextKind.TASK, "Next: ${label(it)}", reasonLine(it), it.key) }
        val revise = levels.filterValues { it == Level.NEEDS_REVISION }.keys.mapNotNull { plan.task(it) }
            .minByOrNull { t -> checks.filter { it.itemId == t.id }.maxByOrNull { it.createdAt }?.pct ?: 0.0 }
        if (revise != null) {
            val last = checks.filter { it.itemId == revise.id }.maxByOrNull { it.createdAt }
            return NextAction(NextKind.REVISE, "Revise ${revise.id}: ${revise.title}",
                "Your last score was ${last?.pct?.let { "%.0f".format(it) } ?: "?"}%. Look at it again, then retake the check.", revise.id)
        }
        if (dueCards > 0) return NextAction(NextKind.PRACTICE, "Optional: ${plural(dueCards, "flashcard")} due", "About 15 min. Fine to skip.", TodayEngine.FLASHCARDS)
        return NextAction(NextKind.DONE, if (todayPlan.qualified) "Today's minimum is met" else "Nothing left on today's list",
            "Stop here and rest. Tomorrow's task will be ready, in order.", null)
    }

    private fun label(e: TodayEntry) = listOf(e.code, e.title).filter { it.isNotBlank() }.joinToString(" · ")
    private fun reasonLine(e: TodayEntry) = listOf(e.reason, e.minutesLabel).filter { it.isNotBlank() }.joinToString(" · ")

    fun weekSummary(
        plan: DayPlan,
        week: Int,
        states: Map<String, ItemState>,
        checks: List<Assessment>,
        evidence: List<Evidence>,
        days: List<DayResult>,
        sleep: Map<Long, Boolean>,
        reducedWeeks: Set<Long>,
        previous: WeekSummary?,
        today: Long,
    ): WeekSummary {
        val ws = plan.weekStartOf(week)
        val range = ws..(ws + 6)
        fun p(t: PlanTask) = states[t.id]?.progress
        val planned = plan.tasks.filter { it.day in range && it.kind != TaskKind.REST }
        val done = planned.filter { p(it) == Progress.COMPLETED }
        val wc = checks.filter { it.day in range }
        val scored = wc.filter { it.type in Mastery.SCORED_TYPES && it.max > 0 }
        val byArea = (AREA_ORDER + "review").associateWith { a -> planned.count { it.area == a && p(it) == Progress.COMPLETED } to planned.count { it.area == a } }
            .filterValues { it.second > 0 }
        val weakest = byArea.filterKeys { it != "review" && it != "life" }.minByOrNull { (_, v) -> v.first.toDouble() / v.second }?.key
        val end = minOf(today, ws + 6)
        val missed = plan.tasks.filter { t ->
            val st = states[t.id]
            st?.progress != Progress.COMPLETED && st?.progress != Progress.SKIPPED && t.kind != TaskKind.DAILY &&
                (TodayEngine.effectiveDay(t, reducedWeeks) <= end - 7 || (st?.laterDays?.size ?: 0) >= 2)
        }.take(5)
        val next = plan.tasks.filter { it.day in (ws + 7)..(ws + 13) && it.study && it.kind == TaskKind.SEQUENCE }.distinctBy { it.seq }
        val dr = days.filter { it.day in range && it.day <= end }
        val sug = ArrayList<String>()
        val qualified = dr.count { it.qualified }
        val sched = dr.count { it.scheduled }
        if (sched > 0 && qualified * 2 < sched) {
            sug += "If go-live work is taking your evenings, choose the reduced routine for next week (about 20 minutes a day). It never breaks your streak."
        }
        // Section 12: two weeks in a row below target in one area → change the plan, not your sleep.
        val lowNow = byArea.filter { (a, v) -> a != "review" && v.second >= 2 && v.first * 2 < v.second }.keys
        val lowBefore = previous?.byArea?.filter { (a, v) -> a != "review" && v.second >= 2 && v.first * 2 < v.second }?.keys ?: emptySet()
        for (a in lowNow intersect lowBefore) {
            sug += "${plan.area(a)?.short ?: a} was below target two weeks in a row: fewer new topics, more practice next week. Don't try harder on less sleep."
        }
        if (planned.filter { it.study }.sumOf { it.minutes } > 0 && dr.sumOf { it.minutes } > planned.filter { it.study }.sumOf { it.minutes } * 13 / 10) {
            sug += "You did more than the plan asked. Keep a pace you can hold: stop at the planned time."
        }
        if (wc.any { !it.passed && it.type in Mastery.MARKED }) sug += "Retake the checks marked 'Needs revision' early next week, while it's fresh."
        if (missed.isNotEmpty()) sug += "Missed tasks wait in order. Don't double up: do the next one each day."
        if (sug.isEmpty() && sched > 0) sug += "Keep the same load next week. The plan is working; rest is part of it."
        return WeekSummary(
            week = week, weekStart = ws, planned = planned.size, completed = done.size, minutes = dr.sumOf { it.minutes },
            qualifiedDays = qualified, scheduledDays = sched, checks = wc.size,
            avgScore = if (scored.isEmpty()) null else scored.map { it.pct }.average(),
            revision = wc.filter { !it.passed && it.type in Mastery.MARKED }.mapNotNull { it.itemId }.distinct(),
            best = evidence.filter { it.day in range && it.starred && !it.isStory }.maxByOrNull { it.updatedAt },
            bestCheck = scored.filter { it.passed }.maxByOrNull { it.pct },
            byArea = byArea, weakestArea = weakest, repeatedlyMissed = missed, nextWeek = next,
            habits = habits(plan, ws, states, sleep, today), suggestions = sug,
        )
    }

    /** LinkedIn: conversations started (messages sent) and useful (they replied: 2+ messages). */
    fun conversations(plan: DayPlan, states: Map<String, ItemState>, checks: List<Assessment>): Pair<Int, Int> {
        val sent = plan.tasks.filter { it.area == "linkedin" && it.doneWhen.contains("Message") && states[it.id]?.progress == Progress.COMPLETED }
        val useful = sent.count { t -> checks.filter { it.itemId == t.id && it.type == "outcome" }.maxByOrNull { it.createdAt }?.passed == true }
        return sent.size to useful
    }

    /** Evidence: share of working days (Mon–Fri, not rest days) up to today with a log line ticked. Target 80 %. */
    fun loggedWorkingDays(plan: DayPlan, states: Map<String, ItemState>, today: Long): Pair<Int, Int> {
        val rows = plan.tasks.filter { it.area == "evidence" && it.day <= today && it.day >= plan.start && Days.dow(it.day) <= 5 }
        return rows.count { states[it.id]?.progress == Progress.COMPLETED } to rows.size
    }
}
