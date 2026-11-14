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
