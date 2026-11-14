package com.lakshman.roadmaplab.ui

import com.lakshman.roadmaplab.data.LabState
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Motivation
import com.lakshman.roadmaplab.domain.Phase
import com.lakshman.roadmaplab.domain.PlanTask
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.ProgressInfo
import com.lakshman.roadmaplab.domain.Streak
import com.lakshman.roadmaplab.domain.TaskLog
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.domain.Wellbeing

data class TodayTask(
    val task: PlanTask,
    /** Status logged today, if any. */
    val todayStatus: TaskStatus?,
    val todayMinutes: Int,
)

data class PhaseProgress(val phase: Phase, val tasks: List<PlanTask>, val percent: Double, val done: Int)

/** Everything the screens show, computed from the stored data and today's date. */
data class UiState(
    val s: LabState,
    val today: Long,
    val latest: Map<String, TaskLog>,
    val tasksById: Map<String, PlanTask>,
    val todayTasks: List<TodayTask>,
    val canPull: PlanTask?,
    val progress: ProgressInfo,
    val phases: List<PhaseProgress>,
    val activeDays: Set<Long>,
    val streak: Int,
    val restart: Boolean,
    val reviewQueue: List<CardState>,
    val dueCount: Int,
    val reviewedToday: Int,
    val learned: Int,
    val unlocked: Int,
    val recall: Double?,
    val motivation: Motivation.Line,
    val todayLog: DayLog,
    val lowEnergy: Boolean,
    val lowMood: Boolean,
    val backupDue: Boolean,
    val tomorrow: List<PlanTask>,
)

fun derive(s: LabState, today: Long): UiState {
    val roadmap = s.roadmap
    val tasks = roadmap?.tasks.orEmpty()
    val tasksById = tasks.associateBy { it.id }
    val latest = Planner.latestByTask(s.taskLogs)
    val reduced = s.settings.reducedPlan
    val todayIds = s.dayPlans[today].orEmpty()
    val todayLogsByTask = s.taskLogs.filter { it.day == today }.groupBy { it.taskId }
    val todayTasks = todayIds.mapNotNull { id ->
        val t = tasksById[id] ?: return@mapNotNull null
        val last = todayLogsByTask[id]?.maxByOrNull { it.createdAt }
        TodayTask(t, last?.status, todayLogsByTask[id]?.sumOf { it.minutes } ?: 0)
    }
    val allTodayFinished = todayTasks.isNotEmpty() && todayTasks.all { it.todayStatus != null }
    val canPull = if (allTodayFinished || todayTasks.isEmpty()) Planner.nextToPull(tasks, latest, todayIds, reduced) else null

    val progress = Progress.compute(tasks, latest, roadmap?.milestones.orEmpty(), today, reduced)
    val phases = roadmap?.phases.orEmpty().map { p ->
        val pts = tasks.filter { it.phaseId == p.id && Planner.isActive(it, reduced) }
        val total = pts.sumOf { it.minFull }
        val done = pts.sumOf { it.minFull * Progress.credit(latest[it.id]?.status) }
        PhaseProgress(p, pts, if (total == 0) 0.0 else 100.0 * done / total, pts.count { latest[it.id]?.status == TaskStatus.DONE })
    }

    val dayLogs = s.dayLogs.values.toList()
    val active = Streak.activeDays(s.taskLogs, s.reviewLogs, dayLogs)
    val reviewedToday = s.reviewLogs.count { it.day == today }
    val todayLog = s.dayLogs[today] ?: DayLog(today)
    val lowEnergy = Wellbeing.isLowEnergy(todayLog.energy)

    val next = Planner.pending(tasks, latest, Long.MAX_VALUE, reduced).firstOrNull()
    val nextPhase = next?.let { n -> phases.firstOrNull { it.phase.id == n.phaseId } }
    val pastLines = dayLogs.mapNotNull { d ->
        val text = d.evLearning.ifBlank { d.note }
        if (text.isBlank()) null else d.day to text
    }
    val motivation = Motivation.lineFor(
        Motivation.Inputs(
            day = today, lowEnergy = lowEnergy, phaseTitle = nextPhase?.phase?.title,
            phaseDone = nextPhase?.done ?: 0, phaseTotal = nextPhase?.tasks?.size ?: 0,
            overallPercent = progress.percent, nextMilestone = progress.nextMilestone,
            daysToMilestone = progress.daysToMilestone, pastLines = pastLines,
        )
    )
    val lastBackup = s.settings.lastBackupDay
    val backupDue = lastBackup != today && (Days.isSunday(today) || (lastBackup > 0 && today - lastBackup > 8))

    return UiState(
        s = s, today = today, latest = latest, tasksById = tasksById, todayTasks = todayTasks, canPull = canPull,
        progress = progress, phases = phases, activeDays = active, streak = Streak.current(active, today),
        restart = s.settings.onboarded && s.settings.restartDismissedDay != today &&
            Planner.needsRestart(today, active, s.settings.firstUseDay),
        reviewQueue = Leitner.queue(s.cards, s.cardStates, today, reviewedToday),
        dueCount = Leitner.dueCount(s.cardStates, today), reviewedToday = reviewedToday,
        learned = Leitner.learnedCount(s.cardStates), unlocked = s.cardStates.size,
        recall = Leitner.recallRate(s.reviewLogs, today), motivation = motivation, todayLog = todayLog,
        lowEnergy = lowEnergy,
        lowMood = today >= s.settings.lowMoodHiddenUntil && Wellbeing.lowMoodRun(dayLogs, today),
        backupDue = s.settings.onboarded && backupDue,
        tomorrow = Planner.planDay(tasks, latest, today + 1, s.settings.mode, reduced, false).mapNotNull { tasksById[it] },
    )
}
