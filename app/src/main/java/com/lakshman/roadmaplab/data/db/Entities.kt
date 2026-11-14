package com.lakshman.roadmaplab.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Database tables. Changing any of these needs a new database version AND a Migration in
// LabDatabase.MIGRATIONS, so that updates never delete data.

@Entity(tableName = "phases")
data class PhaseEntity(
    @PrimaryKey val id: String,
    val track: String,
    val title: String,
    val startDay: Long,
    val endDay: Long,
    val sortOrder: Int,
)

@Entity(tableName = "tasks", indices = [Index("plannedDay")])
data class TaskEntity(
    @PrimaryKey val id: String,
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
    /** Card ids, comma-separated. */
    val unlocks: String,
    val core: Boolean,
    val assumedDone: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "milestones")
data class MilestoneEntity(
    @PrimaryKey val id: String,
    val title: String,
    val day: Long,
    val track: String,
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val topic: String,
    /** "mcq" or "flashcard". */
    val type: String,
    val prompt: String,
    /** JSON array of option strings. */
    val optionsJson: String,
    val answer: String,
    val explanation: String,
    val unlockedBy: String?,
)

@Entity(tableName = "card_states")
data class CardStateEntity(
    @PrimaryKey val cardId: String,
    val box: Int,
    val mcCorrect: Int,
    val dueDay: Long,
    val lapses: Int,
    val reviews: Int,
    val lastReviewDay: Long?,
    val unlockedDay: Long,
)

@Entity(tableName = "task_logs", indices = [Index("taskId"), Index("day")])
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val day: Long,
    /** "done", "partly" or "skipped". */
    val status: String,
    val minutes: Int,
    val note: String,
    val createdAt: Long,
    val usedMinimum: Boolean,
)

@Entity(tableName = "day_logs")
data class DayLogEntity(
    @PrimaryKey val day: Long,
    val mood: Int?,
    val energy: Int?,
    val sleepHours: Float?,
    val ifThen: String,
    val note: String,
    val evModule: String,
    val evProblem: String,
    val evAction: String,
    val evResult: String,
    val evLearning: String,
    val eveningDone: Boolean,
    val logNumber: Int?,
    val finishedAt: Long?,
)

@Entity(tableName = "review_logs", indices = [Index("day")])
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val day: Long,
    val rating: String,
    val correct: Boolean,
    val asMcq: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "day_plans", primaryKeys = ["day", "taskId"])
data class DayPlanEntity(
    val day: Long,
    val taskId: String,
    val position: Int,
)

/** Small key/value facts: roadmap version and title, timeline, tracks, cards version. */
@Entity(tableName = "meta")
data class MetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

// ---------------------------------------------------------------------------------------------------
// Version 2 (Eight-Area Roadmap). New tables only: every version-1 table above is kept as it was,
// so nothing recorded before the upgrade is lost. Added by AutoMigration 1 → 2 (see LabDatabase).

/** Every change to a task (start, done, undo, skip, not today). Append-only: history is never rewritten. */
@Entity(tableName = "task_events", indices = [Index("itemId"), Index("day")])
data class TaskEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val day: Long,
    /** start, done, reset, later, skip */
    val type: String,
    val minutes: Int,
    val independent: Boolean,
    val note: String,
    val createdAt: Long,
)

/** One attempt at a check. Retakes add rows, so improvement shows. */
@Entity(tableName = "assessments", indices = [Index("itemId"), Index("day")])
data class AssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String?,
    val area: String,
    val topic: String,
    val type: String,
    val score: Double,
    val maxScore: Double,
    val passed: Boolean,
    val independent: Boolean,
    val detail: String,
    val day: Long,
    val createdAt: Long,
    val seconds: Int,
)

/** Small daily ticks (slept 7 h, reduced-routine actions, flashcards). One row per day and tick. */
@Entity(tableName = "day_ticks", primaryKeys = ["day", "tickId"])
data class DayTickEntity(
    val day: Long,
    val tickId: String,
    val done: Boolean,
    val minutes: Int,
    val updatedAt: Long,
)

/** Evidence log: the roadmap's 9 fields, confidence, the weekly star and STAR-story notes. Edited in place. */
@Entity(tableName = "evidence", indices = [Index("day")])
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val area: String,
    val itemId: String?,
    val kind: String,
    val task: String,
    val problem: String,
    val action: String,
    val result: String,
    val lesson: String,
    val skill: String,
    val proof: String,
    val confidence: Int?,
    val starred: Boolean,
    val starS: String,
    val starT: String,
    val starA: String,
    val starR: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Planned rest, catch-up and reduced-routine days, and the day's chosen focus. */
@Entity(tableName = "day_meta")
data class DayMetaEntity(
    @PrimaryKey val day: Long,
    val kind: String?,
    val focusItemId: String?,
    val kindSetAt: Long?,
    val updatedAt: Long,
)

/** The Sunday checkpoint table. One row per week, edited in place. */
@Entity(tableName = "weekly_reviews")
data class WeeklyReviewEntity(
    @PrimaryKey val weekStart: Long,
    /** JSON objects: area id → text. */
    val actualJson: String,
    val assessmentJson: String,
    val adjustmentJson: String,
    val missedWhy: String,
    /** Comma-separated. */
    val obstacleTags: String,
    val best: String,
    val weakest: String,
    val priorities: String,
    val nextLoad: String,
    val spending: String,
    val completedAt: Long?,
    val updatedAt: Long,
)
