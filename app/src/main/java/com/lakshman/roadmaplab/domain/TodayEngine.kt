package com.lakshman.roadmaplab.domain

sealed interface TodayRef {
    data class Task(val task: PlanTask) : TodayRef
    /** A reduced-routine action (section 14), ticked per day. */
    data class Reduced(val action: ReducedAction) : TodayRef
    /** Optional flashcard practice. */
    data object Flashcards : TodayRef
}

enum class Section {
    /** What today asks for (study, at-work habits, dated steps). */
    MAIN,
    /** Health, family and money: counted as habits, never part of the study minimum. */
    HEALTH,
    /** Daily practice (mantra, silence, the day's remedy): a habit list of its own, never study. */
    PRACTICE,
    /** Fine to skip: getting ahead, extra practice, dated steps during a reduced week. */
    OPTIONAL,
    /** You said "not today". It keeps its place in the order. */
    LATER,
    /** Paused by the reduced routine; they wait for you, in order. */
    PAUSED,
    /** Before the plan starts: a look at the first day. */
    PREVIEW,
}

data class TodayEntry(
    val key: String,
    val ref: TodayRef,
    val area: String,
    /** Task id (T029), or "" for reduced actions and flashcards. */
    val code: String,
    val title: String,
    val minutes: Int,
    val minutesLabel: String,
    val done: Boolean,
    val skipped: Boolean,
    val inProgress: Boolean,
    /** Days since it was planned (0 = today, negative = later). */
    val lateDays: Int,
    val priority: Boolean,
    /** One line: why it's here. */
    val reason: String,
    val section: Section,
) {
    val task: PlanTask? get() = (ref as? TodayRef.Task)?.task
    val study: Boolean get() = !isHabit(area)
}

data class TodayPlan(
    val day: Long,
    val type: DayType,
    /** 1…76 from Tue 17 Nov; null outside the plan. */
    val planDay: Int?,
    val week: Int?,
    val daysLeft: Int,
    val entries: List<TodayEntry>,
    val minutesDone: Int,
    /** Minutes of today's main study (done or not). */
    val minutesPlanned: Int,
    val target: Int,
    val focusKey: String?,
    /** Earlier tasks in a sequence you moved past without ticking (shown quietly; a catch-up day picks them up). */
    val holes: List<PlanTask> = emptyList(),
) {
    fun section(s: Section): List<TodayEntry> = entries.filter { it.section == s }
    val main: List<TodayEntry> get() = section(Section.MAIN)
    val health: List<TodayEntry> get() = section(Section.HEALTH)
    val practice: List<TodayEntry> get() = section(Section.PRACTICE)
    val qualified: Boolean get() = target > 0 && minutesDone >= target
    val mainDone: Int get() = main.count { it.done }
    val restDay: Boolean get() = type == DayType.REST
}

data class TodayInput(
    val plan: DayPlan,
    val states: Map<String, ItemState>,
    /** Small ticks for the day (reduced actions, flashcards, sleep), by id. */
    val checks: Map<String, RoutineCheck>,
    val meta: DayMeta?,
    /** Monday of every week you ran the reduced routine (for the plan's date-shift rule). */
    val reducedWeeks: Set<Long>,
    val dueCards: Int,
    val minutesDone: Int,
    val targets: Targets,
)

/**
 * Builds today's list from the plan's rows. The roadmap's rule: follow the order, not the dates.
 *  - Numbered work (accounting, French, Python, app steps, LinkedIn, Sunday recordings) shows the next
 *    task you haven't done, once its date has come, on the weekdays that sequence uses. A missed day
 *    moves the next task one slot later; nothing is lost, and it never doubles up.
 *  - Day-bound rows (the evidence line, work habits, health habits) belong to their own day.
 *  - Dated steps (STAR stories, money checks, family steps, trainings, the weekly review) wait until done.
 *  - Health and daily practice are habit lists of their own; they never count as study.
 *  - A reduced-routine day shows section 14's minimum actions; the rest waits, in order.
 */
object TodayEngine {
    const val FLASHCARDS = "flashcards"

    fun build(input: TodayInput, day: Long): TodayPlan {
        val plan = input.plan
        val type = Consistency.dayType(day, plan, input.meta)
        val dow = Days.dow(day)
        val entries = ArrayList<TodayEntry>()
        val holes = ArrayList<PlanTask>()
        val states = input.states
        fun progress(id: String) = states[id]?.progress ?: Progress.NOT_STARTED
        fun closed(id: String) = progress(id).let { it == Progress.COMPLETED || it == Progress.SKIPPED }
        fun planned(t: PlanTask) = effectiveDay(t, input.reducedWeeks)

        if (day < plan.leadIn) {
            if (plan.hasLeadIn) entries += plan.tasksOn(plan.leadIn).map { taskEntry(it, input, day, Section.PREVIEW, "${Days.short(plan.leadIn)} · optional lead-in") }
            entries += plan.tasksOn(plan.start).filter { it.study }.map { taskEntry(it, input, day, Section.PREVIEW, "${Days.short(plan.start)} · day 1") }
            return TodayPlan(day, type, null, null, (plan.end - day).toInt(), entries, input.minutesDone, 0, 0, input.meta?.focusItemId)
        }

        // Done today always shows, so the day's work is visible.
        for (t in plan.tasks) {
            val st = states[t.id] ?: continue
            if (st.progress == Progress.COMPLETED && st.doneDay == day) {
                entries += taskEntry(t, input, day, habitSection(t.area) ?: Section.MAIN, "Done today")
            }
        }

        when {
            plan.hasLeadIn && day == plan.leadIn -> {
                plan.tasksOn(day).filter { !closed(it.id) }.forEach {
                    entries += taskEntry(it, input, day, habitSection(it.area) ?: Section.MAIN, "Optional lead-in")
                }
            }
            type == DayType.REST -> {
                // The rest itself, plus the morning practice if the plan keeps it that day (it isn't study).
                plan.tasksOn(day).filter { !closed(it.id) || it.area == "practice" }.forEach {
                    entries += taskEntry(it, input, day, if (it.area == "practice") Section.PRACTICE else Section.MAIN, "Planned rest day")
                }
            }
            type == DayType.REDUCED -> reducedDay(input, day, dow, entries)
            else -> {
                val catchup = type == DayType.CATCHUP
                for ((seq, list) in plan.sequences) {
                    val (ahead, earlier) = frontier(list) { closed(it) }
                    holes += earlier.filter { planned(it) <= day }
                    val undone = if (catchup) earlier + ahead else ahead
                    if (undone.isEmpty()) continue
                    val slots = plan.slotDays[seq] ?: emptySet()
                    val next = undone.first()
                    val due = planned(next) <= day && (dow in slots || planned(next) == day || catchup)
                    if (due) {
                        val picks = if (catchup) undone.filter { planned(it) <= day }.take(2) else listOf(next)
                        for ((k, t) in picks.withIndex()) {
                            val reason = when {
                                planned(t) == day -> "Planned for today"
                                catchup -> "Catch-up day · planned ${Days.short(planned(t))}"
                                else -> "Next in order · planned ${Days.short(planned(t))}"
                            }
                            entries += taskEntry(t, input, day, if (day in (states[t.id]?.laterDays ?: emptySet())) Section.LATER else Section.MAIN,
                                reason, anchor = k == 0 && seq == anchor(dow))
                        }
                    } else if (dow in slots && planned(next) > day && planned(next) - day <= 6 && entries.none { it.area == next.area && it.done }) {
                        entries += taskEntry(next, input, day, Section.OPTIONAL, "You're ahead · planned ${Days.short(planned(next))}")
                    }
                }
                for (t in plan.tasksOn(day)) {
                    if (t.kind != TaskKind.DAILY || closed(t.id) || t.optional) continue
                    val later = day in (states[t.id]?.laterDays ?: emptySet())
                    entries += taskEntry(t, input, day, habitSection(t.area) ?: if (later) Section.LATER else Section.MAIN,
                        if (t.atWork) "At work today" else "Today")
                }
                carry(input, day, entries, Section.MAIN)
            }
        }
        if (type != DayType.REST && (input.dueCards > 0 || input.checks[FLASHCARDS]?.done == true) && entries.none { it.key == FLASHCARDS }) {
            val done = input.checks[FLASHCARDS]?.done == true
            entries += TodayEntry(FLASHCARDS, TodayRef.Flashcards, "accounting", "", "Flashcards", 15, "15 min", done, false, false, 0,
                false, if (done) "Done today" else "${plural(input.dueCards, "card")} due · fine to skip", Section.OPTIONAL)
        }

        val focus = input.meta?.focusItemId
        val sorted = entries.distinctBy { it.key }.sortedWith(
            compareBy<TodayEntry>(
                { it.section.ordinal },
                { if (it.key == focus) 0 else 1 },
                { if (it.done) 1 else 0 },
                { if (it.priority) 0 else 1 },
                { areaRank(it.area, dow) },
                { -it.lateDays },
                { it.code },
            )
        )
        val main = sorted.filter { it.section == Section.MAIN && it.study }
        return TodayPlan(
            day = day, type = type,
            planDay = if (plan.inPlan(day)) (day - plan.start + 1).toInt() else null,
            week = plan.weekOf(day),
            daysLeft = maxOf(0, (plan.end - day).toInt()),
            entries = sorted,
            minutesDone = input.minutesDone,
            minutesPlanned = main.sumOf { it.minutes },
            target = input.targets.of(type),
            focusKey = focus,
            holes = if (type == DayType.CATCHUP) emptyList() else holes.filter { h -> sorted.none { it.key == h.id } },
        )
    }

    /**
     * Where you are in a sequence: the tasks after the last one you ticked (done or let go), and the
     * earlier ones you moved past without ticking. "Already ahead? Tick what you've done and carry on from there."
     */
    fun frontier(list: List<PlanTask>, closed: (String) -> Boolean): Pair<List<PlanTask>, List<PlanTask>> {
        val last = list.indexOfLast { closed(it.id) }
        val ahead = list.drop(last + 1)
        val earlier = if (last <= 0) emptyList() else list.take(last).filter { !closed(it.id) }
        return ahead to earlier
    }

    /** Dated steps that wait until done (STAR stories, money checks, family steps, trainings, weekly reviews). */
    private fun carry(input: TodayInput, day: Long, entries: MutableList<TodayEntry>, section: Section) {
        for (t in input.plan.tasks) {
            if (t.kind != TaskKind.CARRY || t.day > day || t.optional) continue
            val p = input.states[t.id]?.progress
            if (p == Progress.COMPLETED || p == Progress.SKIPPED) continue
            val sec = when {
                day in (input.states[t.id]?.laterDays ?: emptySet()) -> Section.LATER
                else -> habitSection(t.area) ?: section
            }
            entries += taskEntry(t, input, day, sec, if (t.day == day) "Today" else "Waiting since ${Days.short(t.day)}")
        }
    }

    private fun reducedDay(input: TodayInput, day: Long, dow: Int, entries: MutableList<TodayEntry>) {
        val plan = input.plan
        val todays = plan.tasksOn(day)
        val evidenceRow = todays.firstOrNull { it.area == "evidence" && it.kind != TaskKind.CARRY }
        for (a in plan.meta.reduced) {
            if (dow !in a.days) continue
            when {
                a.area == "evidence" && evidenceRow != null -> {
                    if (input.states[evidenceRow.id]?.progress != Progress.COMPLETED) {
                        entries += taskEntry(evidenceRow, input, day, Section.MAIN, "Reduced routine: " + a.action)
                    }
                }
                a.area == "python" -> {
                    val next = plan.sequences["python"]?.let { l -> frontier(l) { id -> input.states[id]?.progress.let { p -> p == Progress.COMPLETED || p == Progress.SKIPPED } }.first.firstOrNull() }
                    if (next != null && effectiveDay(next, input.reducedWeeks) <= day) {
                        entries += taskEntry(next, input, day, Section.MAIN, "Reduced routine: 15-min revision + finish only this step")
                    }
                }
                a.area == "sap" && todays.any { it.area == "sap" && it.kind == TaskKind.DAILY } -> {
                    val cards = todays.first { it.area == "sap" && it.kind == TaskKind.DAILY }
                    if (input.states[cards.id]?.progress != Progress.COMPLETED) {
                        entries += taskEntry(cards, input, day, Section.MAIN, "Reduced routine: " + a.action)
                    }
                }
                a.area == "review" -> {
                    val review = todays.firstOrNull { it.area == "review" }
                    if (review != null && input.states[review.id]?.progress != Progress.COMPLETED) {
                        entries += taskEntry(review, input, day, Section.MAIN, "Reduced routine: 10 min, which week to repeat")
                    } else if (review == null) entries += reducedEntry(a, input)
                }
                else -> entries += reducedEntry(a, input)
            }
        }
        // Everything else waits, in order.
        for ((seq, list) in plan.sequences) {
            val next = frontier(list) { id -> input.states[id]?.progress.let { p -> p == Progress.COMPLETED || p == Progress.SKIPPED } }.first.firstOrNull() ?: continue
            if (entries.any { it.key == next.id }) continue
            if (effectiveDay(next, input.reducedWeeks) <= day && seq != "python") {
                entries += taskEntry(next, input, day, Section.PAUSED, "Waits for the end of the reduced week")
            }
        }
        carry(input, day, entries, Section.OPTIONAL)
    }

    private fun reducedEntry(a: ReducedAction, input: TodayInput): TodayEntry {
        val done = input.checks[a.id]?.done == true
        return TodayEntry(a.id, TodayRef.Reduced(a), a.area, "", a.action, a.minutes, a.time, done, false, false, 0, false,
            "Reduced routine (section 14)", habitSection(a.area) ?: Section.MAIN)
    }

    private fun taskEntry(t: PlanTask, input: TodayInput, day: Long, section: Section, reason: String, anchor: Boolean = false): TodayEntry {
        val st = input.states[t.id]
        val planned = effectiveDay(t, input.reducedWeeks)
        return TodayEntry(
            key = t.id, ref = TodayRef.Task(t), area = t.area, code = t.id, title = t.title,
            minutes = t.minutes, minutesLabel = t.minutesLabel,
            done = st?.progress == Progress.COMPLETED, skipped = st?.progress == Progress.SKIPPED,
            inProgress = st?.progress == Progress.IN_PROGRESS,
            lateDays = (day - planned).toInt(),
            priority = anchor || t.isMilestone || t.checks.any { it is CheckSpec.Quiz && (it.bank == "journal" || it.timedMinutes > 0) } ||
                (t.kind == TaskKind.CARRY && t.day < day),
            reason = reason, section = section,
        )
    }

    /**
     * The roadmap's date-shift rule: "Two reduced weeks in a row? Shift every later date by one week."
     * Each pair of back-to-back reduced weeks before a task moves its date 7 days later.
     */
    fun effectiveDay(t: PlanTask, reducedWeeks: Set<Long>): Long {
        if (t.kind != TaskKind.SEQUENCE || reducedWeeks.isEmpty()) return t.day
        val before = reducedWeeks.filter { it + 6 < t.day }.sorted()
        var shift = 0L
        var run = 0
        var prev: Long? = null
        for (w in before) {
            run = if (prev != null && w - prev == 7L) run + 1 else 1
            if (run % 2 == 0) shift += 7
            prev = w
        }
        return t.day + shift
    }

    /** Which habit list a category's tasks go to, or null for study. */
    fun habitSection(area: String): Section? = when (area) {
        "life" -> Section.HEALTH
        "practice" -> Section.PRACTICE
        else -> null
    }

    /** The sequence each day is built around (section 4: Friday is SAP Friday, the weekend is Python). */
    fun anchor(dow: Int): String = when (dow) {
        5 -> "sap"
        6, 7 -> "python"
        else -> "accounting"
    }

    private fun areaRank(area: String, dow: Int): Int {
        val order = when (dow) {
            5 -> listOf("sap", "comms", "evidence", "accounting", "french", "python", "career", "review", "life", "practice")
            6, 7 -> listOf("python", "career", "comms", "evidence", "review", "sap", "accounting", "french", "life", "practice")
            else -> listOf("accounting", "sap", "french", "comms", "evidence", "python", "career", "review", "life", "practice")
        }
        return order.indexOf(area).let { if (it < 0) 99 else it }
    }
}
