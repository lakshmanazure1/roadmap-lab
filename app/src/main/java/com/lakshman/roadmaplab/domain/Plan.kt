package com.lakshman.roadmaplab.domain

// The plan: every row of the roadmap's CSV (Eight_Area_Schedule_15Nov2026-31Jan2027.csv) is one task.
// It is app content, never edited on the phone. Your own records (ticks, scores, evidence) point at
// task ids (T001…T380) and live in the database, so a newer plan file keeps them.

data class Area(
    val id: String,
    val name: String,
    val short: String,
    val objective: String,
    val byEnd: String,
    val budget: String,
    val assessed: String,
    val notEnough: String,
    val success: String,
    val targetLevel: String,
    val stretch: String,
    val rules: List<String>,
)

/** How a task is scheduled. */
enum class TaskKind {
    /** Numbered work that follows the order, not the dates (accounting, French, Python, app, LinkedIn, recordings). */
    SEQUENCE,
    /** Belongs to its own day (evidence line, work habits, health habits, weekly review). */
    DAILY,
    /** Belongs to a day but waits until done (STAR stories, money checks, family steps, trainings). */
    CARRY,
    /** Planned rest day (25 Dec, 1 Jan). */
    REST,
}

/** How a task can be checked. Completion is never the same as mastery. */
sealed interface CheckSpec {
    val title: String

    /** Questions the app marks for you (French words both ways, accounting, Python/RAG). */
    data class Quiz(
        override val title: String,
        val bank: String,
        val count: Int,
        /** Pass mark as a number of correct answers; 0 = the default 70 %. */
        val target: Int = 0,
        /** French: the plan weeks whose words or sentences are asked. */
        val weeks: List<Int> = emptyList(),
        /** French: "words 1–5" = 0, "words 6–10" = 1, all = -1. */
        val half: Int = -1,
        val timedMinutes: Int = 0,
    ) : CheckSpec

    /** A score you got in your notebook ("10 of 12 correct", "Quiz 8/10"). target 0 = any score counts. */
    data class Score(override val title: String, val label: String, val max: Int, val target: Int) : CheckSpec

    /** "Done when" criteria you tick. */
    data class Checklist(override val title: String, val criteria: List<String>) : CheckSpec

    /** The 5-criterion rubric, 0–2 each, out of 10. */
    data class Rubric(override val title: String, val target: Int) : CheckSpec

    /** Explain it aloud without notes or AI (the plan's test of real understanding). */
    data class Explain(override val title: String, val prompt: String) : CheckSpec

    /** Shown or explained to someone else, unaided (level 6). */
    data class Demonstrate(override val title: String) : CheckSpec

    /** STAR story format check. */
    data class Star(override val title: String) : CheckSpec

    /** LinkedIn: did they reply? A conversation is useful after 2+ messages. */
    data class Outcome(override val title: String) : CheckSpec
}

data class PlanTask(
    val id: String,
    val day: Long,
    val weekday: String,
    /** Plan week 0 (lead-in) … 11. */
    val week: Int,
    val area: String,
    val areaLabel: String,
    val learn: String,
    val build: String,
    /** Planned minutes; 0 = done inside work. */
    val minutes: Int,
    val doneWhen: String,
    val evidence: String,
    val milestone: String,
    /** Level written in the CSV's status column (1 = not started). */
    val csvLevel: Int,
    val csvScore: String,
    val csvNotes: String,
    val title: String,
    val kind: TaskKind,
    val seq: String,
    val checks: List<CheckSpec>,
    val order: Int,
) {
    /** Study time (health, family and money is tracked separately and never counts toward the study minimum). */
    val study: Boolean get() = area != "life"
    val atWork: Boolean get() = minutes == 0 && area == "comms"
    val isMilestone: Boolean get() = milestone.isNotBlank()
    val minutesLabel: String get() = when {
        atWork -> "at work"
        minutes <= 0 -> ""
        minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60} h"
        minutes > 60 -> "${minutes / 60} h ${minutes % 60}"
        else -> "$minutes min"
    }
    val gradedChecks: List<CheckSpec> get() = checks.filter { it !is CheckSpec.Outcome }

    /** The optional lead-in (Sun 15 Nov, week 0). It never holds up the order: T029 covers it if skipped. */
    val optional: Boolean get() = week == 0
}

data class FrenchWord(val fr: String, val en: String)
data class FrenchSentence(val fr: String, val en: String)
data class FrenchWeek(val week: Int, val theme: String, val words: List<FrenchWord>, val sentences: List<FrenchSentence>)

data class JournalTx(val id: String, val week: Int, val text: String, val dr: String, val cr: String)

data class BankQuestion(
    val id: String,
    val week: Int,
    val prompt: String,
    val options: List<String>,
    val answer: String,
    val explanation: String,
)

data class RubricCriterion(val name: String, val levels: List<String>)

data class ReducedAction(val id: String, val area: String, val action: String, val time: String, val minutes: Int, val days: Set<Int>)

data class PlanMeta(
    val version: String,
    val title: String,
    val subtitle: String,
    val csvName: String,
    val start: Long,
    val leadIn: Long,
    val end: Long,
    val bottomLine: String,
    val areas: List<Area>,
    val rubric: List<RubricCriterion>,
    val rubricNote: String,
    val levels: List<Pair<String, String>>,
    val french: List<FrenchWeek>,
    /** week → area → weekly target. */
    val checkpoints: Map<Int, Map<String, String>>,
    val weekFocus: Map<Int, String>,
    val reduced: List<ReducedAction>,
    val recovery: List<String>,
    val rules: List<String>,
    val monthly: Map<String, List<String>>,
    val weekRoutine: List<List<String>>,
    val titles: Map<String, String>,
    val accountingQuestions: List<BankQuestion>,
    val journal: List<JournalTx>,
    val accounts: List<String>,
    val calcs: List<Pair<String, Int>>,
    val pythonQuestions: List<BankQuestion>,
    val cardUnlocks: Map<String, String>,
)

/** The whole plan: the CSV's tasks plus the roadmap's notes. */
class DayPlan(val meta: PlanMeta, val tasks: List<PlanTask>, val freshStart: Long = 0) {
    companion object {
        /** The Python recap week added on your fresh start (PR1–PR7). */
        const val RECAP = "recap"
    }

    /** The same plan, counted from a fresh start: days before it are "nothing scheduled", never "missed". */
    fun withFreshStart(day: Long): DayPlan = if (day == freshStart) this else DayPlan(meta, tasks, day)

    /** Tasks that come from the roadmap's CSV (not the recap you added). */
    val csvTasks: List<PlanTask> get() = tasks.filter { it.seq != RECAP }

    /** A sequence that waits for another to finish first: the plan's Python waits for your recap. */
    val waitsFor: Map<String, String> = if (tasks.any { it.seq == RECAP }) mapOf("python" to RECAP) else emptyMap()
    private val byId = tasks.associateBy { it.id }
    private val areaById = meta.areas.associateBy { it.id }

    val start: Long get() = meta.start
    val leadIn: Long get() = meta.leadIn
    val end: Long get() = meta.end
    val totalDays: Int get() = (end - start + 1).toInt()

    fun task(id: String): PlanTask? = byId[id]
    fun area(id: String): Area? = areaById[id]
    fun inPlan(day: Long): Boolean = day in maxOf(start, freshStart)..end

    /** Planned rest days come from the plan's own "Rest day" rows. */
    val restDays: Map<Long, PlanTask> = tasks.filter { it.kind == TaskKind.REST }.associateBy { it.day }

    fun tasksOn(day: Long): List<PlanTask> = tasks.filter { it.day == day }

    fun frenchWeek(week: Int): FrenchWeek? = meta.french.firstOrNull { it.week == week }

    /** Plan week (1…11) of a day, 0 for the lead-in, or null outside the plan. */
    fun weekOf(day: Long): Int? = when {
        day == leadIn -> 0
        day in start..end -> ((day - start) / 7).toInt() + 1
        else -> null
    }

    fun weekStartOf(week: Int): Long = start + (week - 1) * 7L

    /** Each sequence's own weekdays: the weekdays it has 3 or more tasks on (e.g. Python: Sat and Sun). */
    val slotDays: Map<String, Set<Int>> = tasks.filter { it.kind == TaskKind.SEQUENCE && !it.optional }.groupBy { it.seq }.mapValues { (seq, l) ->
        // The recap is meant to be done fast, any day of its week (and after, if it runs over).
        if (seq == RECAP) (1..7).toSet() else l.groupingBy { Days.dow(it.day) }.eachCount().filterValues { it >= 3 }.keys
    }

    /** The sequences in order: next undone first. */
    val sequences: Map<String, List<PlanTask>> = tasks.filter { it.kind == TaskKind.SEQUENCE && !it.optional }.groupBy { it.seq }
        .mapValues { (_, l) -> l.sortedWith(compareBy({ it.day }, { it.order })) }
}

/** Display order of the 8 areas (the weekly review is shown separately). */
val AREA_ORDER = listOf("python", "accounting", "french", "comms", "linkedin", "evidence", "app", "life")
