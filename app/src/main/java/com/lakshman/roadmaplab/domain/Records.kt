package com.lakshman.roadmaplab.domain

// Your own records. Every change is a new row (history is never rewritten),
// except things you edit in place (an evidence entry, a weekly review), which keep their id.

/** What happened to a task. The latest START/DONE/RESET/SKIP decides its progress. */
enum class EventType(val key: String) {
    /** Started, or did part of it (minutes > 0 count for that day). */
    START("start"),
    DONE("done"),
    /** Undo: back to not started. */
    RESET("reset"),
    /** "Not today": hides it from today's list only. It keeps its place in the order. */
    LATER("later"),
    /** "Won't do this one": a daily task you let go. It is shown as skipped, never as done. */
    SKIP("skip");

    companion object {
        fun of(key: String): EventType = entries.firstOrNull { it.key == key } ?: START
    }
}

data class ItemEvent(
    val id: Long,
    val itemId: String,
    val day: Long,
    val type: EventType,
    val minutes: Int,
    /** Done without help, notes or answers in view (level 4: practised independently). */
    val independent: Boolean,
    val note: String,
    val createdAt: Long,
)

/**
 * A small daily tick that isn't a plan row: "slept 7 h", a reduced-routine action, flashcard practice.
 * One row per (day, id); ticking again edits it.
 */
data class RoutineCheck(val day: Long, val routineId: String, val done: Boolean, val minutes: Int, val updatedAt: Long)

/** One attempt at a check. Never overwritten: retakes add new rows, so improvement shows. */
data class Assessment(
    val id: Long,
    val itemId: String?,
    val area: String,
    val topic: String,
    /** quiz, score, checklist, rubric, explain, demo, star, outcome */
    val type: String,
    val score: Double,
    val max: Double,
    val passed: Boolean,
    /** Done without notes, AI or the answer in view. */
    val independent: Boolean,
    /** JSON with the answers, criteria or notes, for the history screen. */
    val detail: String,
    val day: Long,
    val createdAt: Long,
    val seconds: Int,
) {
    val pct: Double get() = if (max > 0) 100.0 * score / max else 0.0
}

/**
 * An evidence-log entry with the roadmap's 9 fields (Date, Area, Task, Problem, Action, Result, Skill,
 * Evidence, Lesson), a confidence rating, the weekly star and STAR-story notes.
 */
data class Evidence(
    val id: Long,
    val day: Long,
    val area: String,
    val itemId: String?,
    /** "line" (daily line) or "star" (a STAR story). */
    val kind: String,
    val task: String,
    val problem: String,
    val action: String,
    val result: String,
    val lesson: String,
    val skill: String,
    val proof: String,
    /** 1–5, or null. */
    val confidence: Int?,
    val starred: Boolean,
    val starS: String,
    val starT: String,
    val starA: String,
    val starR: String,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val isStory: Boolean get() = kind == "star"
    val text: String get() = listOf(task, problem, action, result, skill, proof, lesson, starS, starT, starA, starR).filter { it.isNotBlank() }.joinToString(" · ")

    /** The plan's test: a line needs a real result, not just “worked on X”. */
    val hasRealResult: Boolean get() = result.isNotBlank() && !result.trim().lowercase().startsWith("worked on")
}

/** What kind of day it is, set by you. Rest days must be set on or before the day. */
enum class DayKind(val key: String, val label: String) {
    REST("rest", "Planned rest day"),
    CATCHUP("catchup", "Catch-up day"),
    /** The reduced routine for busy go-live weeks (about 20 minutes a day). */
    REDUCED("reduced", "Reduced routine");

    companion object {
        fun of(key: String?): DayKind? = entries.firstOrNull { it.key == key }
    }
}

data class DayMeta(val day: Long, val kind: DayKind?, val focusItemId: String?, val kindSetAt: Long?, val updatedAt: Long)

/** The Sunday checkpoint table (roadmap section 11). */
data class WeeklyReview(
    val weekStart: Long,
    /** area id → the actual result you wrote. */
    val actual: Map<String, String>,
    /** area id → your assessment of that result (e.g. "8/10", "weak"). */
    val assessment: Map<String, String>,
    /** area id → next week's adjustment. */
    val adjustment: Map<String, String>,
    val missedWhy: String,
    val obstacleTags: List<String>,
    val best: String,
    val weakest: String,
    val priorities: String,
    /** "normal" or "reduced" */
    val nextLoad: String,
    /** The week's spending total, as you wrote it. */
    val spending: String,
    val completedAt: Long?,
    val updatedAt: Long,
)

/** Minimum minutes of planned work that make a day count, per kind of day. You choose them. */
data class Targets(
    val weekday: Int = 20,
    val saturday: Int = 30,
    val sunday: Int = 20,
    val reduced: Int = 15,
    val catchup: Int = 20,
) {
    fun of(type: DayType): Int = when (type) {
        DayType.WEEKDAY -> weekday
        DayType.SATURDAY -> saturday
        DayType.SUNDAY -> sunday
        DayType.REDUCED -> reduced
        DayType.CATCHUP -> catchup
        DayType.REST, DayType.NONE -> 0
    }
}
