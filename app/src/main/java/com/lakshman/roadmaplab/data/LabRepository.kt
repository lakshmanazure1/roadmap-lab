package com.lakshman.roadmaplab.data

import android.content.Context
import androidx.room.withTransaction
import com.lakshman.roadmaplab.data.db.CardEntity
import com.lakshman.roadmaplab.data.db.CardStateEntity
import com.lakshman.roadmaplab.data.db.DayLogEntity
import com.lakshman.roadmaplab.data.db.DayPlanEntity
import com.lakshman.roadmaplab.data.db.LabDatabase
import com.lakshman.roadmaplab.data.db.MetaEntity
import com.lakshman.roadmaplab.data.db.MilestoneEntity
import com.lakshman.roadmaplab.data.db.PhaseEntity
import com.lakshman.roadmaplab.data.db.ReviewLogEntity
import com.lakshman.roadmaplab.data.db.TaskEntity
import com.lakshman.roadmaplab.data.db.TaskLogEntity
import com.lakshman.roadmaplab.data.db.toDomain
import com.lakshman.roadmaplab.data.db.toEntity
import com.lakshman.roadmaplab.data.json.LabJson
import com.lakshman.roadmaplab.data.json.Snapshot
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.ReviewLog
import com.lakshman.roadmaplab.domain.Roadmap
import com.lakshman.roadmaplab.domain.Streak
import com.lakshman.roadmaplab.domain.TaskLog
import com.lakshman.roadmaplab.domain.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LabRepository(private val context: Context, val db: LabDatabase, val settings: SettingsStore) {
    private val dao = db.dao()
    private val planLock = Mutex()

    val state: Flow<LabState> = combine(
        listOf(
            dao.phasesFlow(), dao.tasksFlow(), dao.milestonesFlow(), dao.cardsFlow(), dao.cardStatesFlow(),
            dao.taskLogsFlow(), dao.dayLogsFlow(), dao.reviewLogsFlow(), dao.dayPlansFlow(), dao.metaFlow(),
            settings.flow,
        )
    ) { parts ->
        @Suppress("UNCHECKED_CAST")
        val meta = (parts[9] as List<MetaEntity>).associate { it.key to it.value }
        @Suppress("UNCHECKED_CAST")
        val tasks = (parts[1] as List<TaskEntity>).map { it.toDomain() }
        @Suppress("UNCHECKED_CAST")
        val roadmap = if (tasks.isEmpty()) null else Roadmap(
            version = meta["roadmapVersion"] ?: "",
            title = meta["roadmapTitle"] ?: "",
            timelineStart = meta["timelineStart"]?.toLongOrNull() ?: tasks.minOf { it.plannedDay },
            timelineEnd = meta["timelineEnd"]?.toLongOrNull() ?: tasks.maxOf { it.plannedDay },
            tracks = meta["tracks"]?.split('\n')?.filter { it.isNotBlank() } ?: tasks.map { it.track }.distinct(),
            phases = (parts[0] as List<PhaseEntity>).map { it.toDomain() },
            milestones = (parts[2] as List<MilestoneEntity>).map { it.toDomain() },
            tasks = tasks,
        )
        LabState(
            settings = parts[10] as AppSettings,
            roadmap = roadmap,
            cardsVersion = meta["cardsVersion"] ?: "",
            cards = (parts[3] as List<CardEntity>).map { it.toDomain() }.associateBy { it.id },
            cardStates = (parts[4] as List<CardStateEntity>).map { it.toDomain() },
            taskLogs = (parts[5] as List<TaskLogEntity>).map { it.toDomain() },
            dayLogs = (parts[6] as List<DayLogEntity>).map { it.toDomain() }.associateBy { it.day },
            reviewLogs = (parts[7] as List<ReviewLogEntity>).map { it.toDomain() },
            dayPlans = (parts[8] as List<DayPlanEntity>).groupBy({ it.day }, { it.taskId }),
        )
    }

    // ------------------------------------------------------------------ first launch + roadmap files

    /** Loads the roadmap and cards bundled with the app the first time it runs. */
    suspend fun importBundledIfEmpty() {
        if (dao.taskCount() == 0) {
            importRoadmap(LabJson.parseRoadmap(asset("roadmap.json")))
        }
        if (dao.cardCount() == 0) {
            val (version, cards) = LabJson.parseCards(asset("cards.json"))
            importCards(version, cards)
        }
    }

    private fun asset(name: String): String = context.assets.open(name).use { String(it.readBytes(), Charsets.UTF_8) }

    /** Replaces the plan (phases, tasks, milestones). Your logs, card progress and history stay. */
    suspend fun importRoadmap(r: Roadmap) {
        val today = Days.today()
        db.withTransaction {
            dao.clearPhases(); dao.clearTasks(); dao.clearMilestones()
            dao.upsertPhases(r.phases.map { it.toEntity() })
            dao.upsertTasks(r.tasks.map { it.toEntity() })
            dao.upsertMilestones(r.milestones.map { it.toEntity() })
            dao.upsertMeta(
                listOf(
                    MetaEntity("roadmapVersion", r.version), MetaEntity("roadmapTitle", r.title),
                    MetaEntity("timelineStart", r.timelineStart.toString()), MetaEntity("timelineEnd", r.timelineEnd.toString()),
                    MetaEntity("tracks", r.tracks.joinToString("\n")),
                )
            )
            if (dao.logCount(today) == 0) dao.clearDayPlan(today) // re-plan today with the new file
            unlockForFinishedTasks(today)
        }
    }

    /** Replaces the question bank. Card progress for cards that still exist is kept. */
    suspend fun importCards(version: String, cards: List<Card>) {
        db.withTransaction {
            dao.clearCards()
            dao.upsertCards(cards.map { it.toEntity() })
            dao.upsertMeta(listOf(MetaEntity("cardsVersion", version)))
            unlockForFinishedTasks(Days.today())
        }
    }

    private suspend fun unlockForFinishedTasks(today: Long) {
        val tasks = dao.tasks().map { it.toDomain() }
        val latest = Planner.latestByTask(dao.taskLogs().map { it.toDomain() })
        val done = tasks.filter { latest[it.id]?.status == TaskStatus.DONE }
        val fresh = Leitner.unlockOnDone(done, dao.cards().map { it.toDomain() }, dao.cardStates().map { it.toDomain() }, today)
        if (fresh.isNotEmpty()) dao.upsertCardStates(fresh.map { it.toEntity() })
    }

    // ------------------------------------------------------------------ onboarding

    suspend fun completeOnboarding(alreadyDone: Set<String>, change: (AppSettings) -> AppSettings) {
        val today = Days.today()
        val now = System.currentTimeMillis()
        db.withTransaction {
            val tasks = dao.tasks().associateBy { it.id }
            for (id in alreadyDone) {
                val t = tasks[id] ?: continue
                dao.insertTaskLog(TaskLogEntity(taskId = id, day = t.plannedDay, status = TaskStatus.DONE.key, minutes = 0,
                    note = "Done before the app", createdAt = now, usedMinimum = false))
            }
            unlockForFinishedTasks(today)
        }
        settings.update { change(it).copy(onboarded = true, firstUseDay = today) }
    }

    // ------------------------------------------------------------------ the day

    /** Makes today's task list once per day (at most 3, sized to the mode). */
    suspend fun ensurePlan(today: Long) = planLock.withLock {
        if (dao.planCount(today) > 0) return@withLock
        val s = settings.current()
        if (!s.onboarded) return@withLock
        val tasks = dao.tasks().map { it.toDomain() }
        if (tasks.isEmpty()) return@withLock
        val logs = dao.taskLogs().map { it.toDomain() }
        val active = Streak.activeDays(logs, dao.reviewLogs().map { it.toDomain() }, dao.dayLogs().map { it.toDomain() })
        val restart = Planner.needsRestart(today, active, s.firstUseDay)
        val ids = Planner.planDay(tasks, Planner.latestByTask(logs), today, s.mode, s.reducedPlan, restart)
        dao.insertDayPlans(ids.mapIndexed { i, id -> DayPlanEntity(today, id, i) })
    }

    /** Re-plans today, e.g. after changing the mode, as long as nothing is logged yet today. */
    suspend fun replanToday() {
        val today = Days.today()
        planLock.withLock {
            if (dao.logCount(today) == 0) dao.clearDayPlan(today)
        }
        ensurePlan(today)
    }

    suspend fun pullIntoToday(taskId: String) {
        val today = Days.today()
        val count = dao.planCount(today)
        if (count < Planner.MAX_TASKS_PER_DAY) dao.insertDayPlans(listOf(DayPlanEntity(today, taskId, count)))
    }

    suspend fun logTask(taskId: String, status: TaskStatus, minutes: Int, usedMinimum: Boolean, note: String = "") {
        val today = Days.today()
        db.withTransaction {
            dao.insertTaskLog(TaskLogEntity(taskId = taskId, day = today, status = status.key, minutes = minutes,
                note = note, createdAt = System.currentTimeMillis(), usedMinimum = usedMinimum))
            if (status == TaskStatus.DONE) unlockForFinishedTasks(today)
        }
    }

    suspend fun saveDayLog(day: Long, change: (DayLog) -> DayLog) {
        db.withTransaction {
            val old = dao.dayLog(day)?.toDomain() ?: DayLog(day)
            dao.upsertDayLog(change(old).toEntity())
        }
    }

    /** Marks the evening log done and gives it the next log number (Log #001, #002…). */
    suspend fun finishEvening(day: Long) {
        db.withTransaction {
            val old = dao.dayLog(day)?.toDomain() ?: DayLog(day)
            val number = old.logNumber ?: ((dao.maxLogNumber() ?: 0) + 1)
            dao.upsertDayLog(old.copy(eveningDone = true, logNumber = number, finishedAt = old.finishedAt ?: System.currentTimeMillis()).toEntity())
        }
    }

    suspend fun reopenEvening(day: Long) = saveDayLog(day) { it.copy(eveningDone = false) }

    suspend fun review(cardId: String, rating: Rating, asMcq: Boolean) {
        val today = Days.today()
        db.withTransaction {
            val state = dao.cardState(cardId)?.toDomain() ?: Leitner.newState(cardId, today)
            dao.upsertCardState(Leitner.apply(state, rating, today, asMcq).toEntity())
            dao.insertReviewLog(ReviewLogEntity(cardId = cardId, day = today, rating = rating.name,
                correct = rating != Rating.AGAIN, asMcq = asMcq, createdAt = System.currentTimeMillis()))
        }
    }

    // ------------------------------------------------------------------ backup

    suspend fun snapshot(): Snapshot {
        val meta = dao.meta().associate { it.key to it.value }
        val tasks = dao.tasks().map { it.toDomain() }
        return Snapshot(
            roadmap = Roadmap(
                version = meta["roadmapVersion"] ?: "",
                title = meta["roadmapTitle"] ?: "",
                timelineStart = meta["timelineStart"]?.toLongOrNull() ?: (tasks.minOfOrNull { it.plannedDay } ?: Days.today()),
                timelineEnd = meta["timelineEnd"]?.toLongOrNull() ?: (tasks.maxOfOrNull { it.plannedDay } ?: Days.today()),
                tracks = meta["tracks"]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList(),
                phases = dao.phases().map { it.toDomain() },
                milestones = dao.milestones().map { it.toDomain() },
                tasks = tasks,
            ),
            cardsVersion = meta["cardsVersion"] ?: "",
            cards = dao.cards().map { it.toDomain() },
            taskLogs = dao.taskLogs().map { it.toDomain() },
            dayLogs = dao.dayLogs().map { it.toDomain() },
            cardStates = dao.cardStates().map { it.toDomain() },
            reviewLogs = dao.reviewLogs().map { it.toDomain() },
            dayPlans = dao.dayPlans().map { it.toDomain() },
            settings = settings.current(),
        )
    }

    /** Replaces everything on the phone with the backup's contents. */
    suspend fun restore(s: Snapshot) {
        db.withTransaction {
            dao.clearPhases(); dao.clearTasks(); dao.clearMilestones(); dao.clearCards(); dao.clearCardStates()
            dao.clearTaskLogs(); dao.clearDayLogs(); dao.clearReviewLogs(); dao.clearDayPlans(); dao.clearMeta()
            dao.upsertPhases(s.roadmap.phases.map { it.toEntity() })
            dao.upsertTasks(s.roadmap.tasks.map { it.toEntity() })
            dao.upsertMilestones(s.roadmap.milestones.map { it.toEntity() })
            dao.upsertMeta(
                listOf(
                    MetaEntity("roadmapVersion", s.roadmap.version), MetaEntity("roadmapTitle", s.roadmap.title),
                    MetaEntity("timelineStart", s.roadmap.timelineStart.toString()),
                    MetaEntity("timelineEnd", s.roadmap.timelineEnd.toString()),
                    MetaEntity("tracks", s.roadmap.tracks.joinToString("\n")),
                    MetaEntity("cardsVersion", s.cardsVersion),
                )
            )
            dao.upsertCards(s.cards.map { it.toEntity() })
            dao.upsertCardStates(s.cardStates.map { it.toEntity() })
            dao.upsertTaskLogs(s.taskLogs.map { it.toEntity() })
            dao.upsertDayLogs(s.dayLogs.map { it.toEntity() })
            dao.upsertReviewLogs(s.reviewLogs.map { it.toEntity() })
            dao.insertDayPlans(s.dayPlans.map { it.toEntity() })
        }
        settings.update { s.settings.copy(onboarded = true) }
    }

    suspend fun markBackedUp() {
        val today = Days.today()
        settings.update { it.copy(lastBackupDay = today) }
    }
}
