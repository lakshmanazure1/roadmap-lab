package com.lakshman.roadmaplab.domain

// Plain Kotlin models used by the logic and the screens. Days are epoch days (LocalDate.toEpochDay()).

enum class Mode(val key: String, val label: String, val capacityMinutes: Int) {
    INDIA("india", "India", 120),
    DRC("drc", "DRC", 60),
    HEAVY("heavy", "Heavy week", 15);

    companion object {
        fun of(key: String?): Mode = entries.firstOrNull { it.key == key } ?: DRC
    }
}

enum class TaskStatus(val key: String, val label: String) {
    DONE("done", "Done"),
    PARTLY("partly", "Partly done"),
    SKIPPED("skipped", "Skipped");

    companion object {
        fun of(key: String?): TaskStatus? = entries.firstOrNull { it.key == key }
    }
}

data class LegacyPhase(
    val id: String,
    val track: String,
    val title: String,
    val startDay: Long,
    val endDay: Long,
    val order: Int,
)

data class LegacyTask(
    val id: String,
    val phaseId: String,
    val track: String,
    val plannedDay: Long,
    val title: String,
    val learn: String,
    val apply: String,
    val doneWhen: String,
    val minFull: Int,
    val minDrc: Int,
    val minMinimum: Int,
    val unlocks: List<String>,
    val core: Boolean,
    val assumedDone: Boolean,
    val order: Int,
) {
    fun minutes(mode: Mode): Int = when (mode) {
        Mode.INDIA -> minFull
        Mode.DRC -> minDrc
        Mode.HEAVY -> minMinimum
    }
}

data class LegacyMilestone(val id: String, val title: String, val day: Long, val track: String)

data class TaskLog(
    val id: Long,
    val taskId: String,
    val day: Long,
    val status: TaskStatus,
    val minutes: Int,
    val note: String,
    val createdAt: Long,
    val usedMinimum: Boolean,
)

data class DayLog(
    val day: Long,
    val mood: Int? = null,
    val energy: Int? = null,
    val sleepHours: Float? = null,
    val ifThen: String = "",
    val note: String = "",
    val evModule: String = "",
    val evProblem: String = "",
    val evAction: String = "",
    val evResult: String = "",
    val evLearning: String = "",
    val eveningDone: Boolean = false,
    val logNumber: Int? = null,
    val finishedAt: Long? = null,
) {
    val hasEvidence: Boolean
        get() = listOf(evModule, evProblem, evAction, evResult, evLearning).any { it.isNotBlank() }

    /** Writing a note or evidence line, or finishing the evening log, counts as an action for the streak. */
    val counts: Boolean get() = eveningDone || note.isNotBlank() || hasEvidence
}

enum class CardType { MCQ, FLASHCARD }

data class Card(
    val id: String,
    val topic: String,
    val type: CardType,
    val prompt: String,
    val options: List<String>,
    val answer: String,
    val explanation: String,
    val unlockedBy: String?,
)

/** Leitner state of one unlocked card. box 0 = new, 1..6 = learning boxes. */
data class CardState(
    val cardId: String,
    val box: Int,
    val mcCorrect: Int,
    val dueDay: Long,
    val lapses: Int,
    val reviews: Int,
    val lastReviewDay: Long?,
    val unlockedDay: Long,
)

enum class Rating(val label: String) { AGAIN("Again"), HARD("Hard"), GOOD("Good"), EASY("Easy") }

data class ReviewLog(
    val cardId: String,
    val day: Long,
    val rating: Rating,
    val correct: Boolean,
    val asMcq: Boolean,
    val createdAt: Long,
)

data class AppSettings(
    val onboarded: Boolean = false,
    val mode: Mode = Mode.DRC,
    val reducedPlan: Boolean = false,
    val reminderOn: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    /** "light", "dark" or "system" (older values "soft" and "classic" mean light). */
    val theme: String = "light",
    val firstUseDay: Long = 0,
    val lastBackupDay: Long = 0,
    val restartDismissedDay: Long = 0,
    val lowMoodHiddenUntil: Long = 0,
    val showEvidenceLog: Boolean = true,
    // ---- version 2
    /** The "what's new in 2.0" screen has been seen. */
    val v2Intro: Boolean = false,
    /** Minimum minutes of study that make a day count, per kind of day. */
    val targets: Targets = Targets(),
    /** Your stable cue for the evening session, e.g. "After dinner, at my desk". */
    val cue: String = "After dinner, at my desk",
    /** Reminder on Sunday for the weekly review (same time as the daily one). */
    val weeklyReminder: Boolean = true,
    // ---- version 2.1
    /** The day you started fresh (everything before it was cleared), or 0 if you never did. */
    val freshStartDay: Long = 0,
    /** The one-time "start fresh?" card has been answered. */
    val freshStartAsked: Boolean = false,
)

data class DayPlanEntry(val day: Long, val taskId: String, val position: Int)

/** The version-1 roadmap (Oct 2026 plan), kept so nothing recorded with it is lost. */
data class LegacyRoadmap(
    val version: String,
    val title: String,
    val timelineStart: Long,
    val timelineEnd: Long,
    val tracks: List<String>,
    val phases: List<LegacyPhase>,
    val milestones: List<LegacyMilestone>,
    val tasks: List<LegacyTask>,
)
