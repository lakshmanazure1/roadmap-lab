package com.lakshman.roadmaplab.data

import android.content.Context
import androidx.room.withTransaction
import com.lakshman.roadmaplab.data.db.CardEntity
import com.lakshman.roadmaplab.data.db.CardStateEntity
import com.lakshman.roadmaplab.data.db.DayLogEntity
import com.lakshman.roadmaplab.data.db.DayMetaEntity
import com.lakshman.roadmaplab.data.db.DayTickEntity
import com.lakshman.roadmaplab.data.db.EvidenceEntity
import com.lakshman.roadmaplab.data.db.AssessmentEntity
import com.lakshman.roadmaplab.data.db.LabDatabase
import com.lakshman.roadmaplab.data.db.MetaEntity
import com.lakshman.roadmaplab.data.db.ReviewLogEntity
import com.lakshman.roadmaplab.data.db.TaskEventEntity
import com.lakshman.roadmaplab.data.db.TaskLogEntity
import com.lakshman.roadmaplab.data.db.WeeklyReviewEntity
import com.lakshman.roadmaplab.data.db.toDomain
import com.lakshman.roadmaplab.data.db.toEntity
import com.lakshman.roadmaplab.data.json.LabJson
import com.lakshman.roadmaplab.data.json.PlanMetaJson
import com.lakshman.roadmaplab.data.json.Snapshot
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.CardType
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.DayMeta
import com.lakshman.roadmaplab.domain.DayPlan
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Evidence
import com.lakshman.roadmaplab.domain.LegacyRoadmap
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.PlanCsv
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.TaskKind
import com.lakshman.roadmaplab.domain.TodayEngine
import com.lakshman.roadmaplab.domain.WeeklyReview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The one place that reads and writes your data. Everything lives on this phone: a Room database
 * (roadmaplab.db) and a small settings file, both in the app's private storage. The app has no
 * internet permission. Backups go only where you choose to save them.
 */
class LabRepository(private val context: Context, val db: LabDatabase, val settings: SettingsStore) {
    private val dao = db.dao()

    data class PlanLoad(val plan: DayPlan?, val error: String?)

    private val planFlow = MutableStateFlow<PlanLoad?>(null)

    val state: Flow<LabState> = combine(
        listOf(
            dao.cardsFlow(), dao.cardStatesFlow(), dao.reviewLogsFlow(), dao.dayLogsFlow(),
            dao.taskEventsFlow(), dao.assessmentsFlow(), dao.dayTicksFlow(), dao.evidenceFlow(),
            dao.dayMetaFlow(), dao.weeklyReviewsFlow(), dao.taskLogsFlow(), settings.flow, planFlow,
        )
    ) { p ->
        @Suppress("UNCHECKED_CAST")
        val load = p[12] as PlanLoad?
        @Suppress("UNCHECKED_CAST")
        LabState(
            settings = p[11] as AppSettings,
            plan = load?.plan,
            planError = load?.error,
            cards = (p[0] as List<CardEntity>).map { it.toDomain() }.associateBy { it.id },
            cardStates = (p[1] as List<CardStateEntity>).map { it.toDomain() },
            reviewLogs = (p[2] as List<ReviewLogEntity>).map { it.toDomain() },
            dayLogs = (p[3] as List<DayLogEntity>).map { it.toDomain() }.associateBy { it.day },
            events = (p[4] as List<TaskEventEntity>).map { it.toDomain() },
            assessments = (p[5] as List<AssessmentEntity>).map { it.toDomain() },
            ticks = (p[6] as List<DayTickEntity>).map { it.toDomain() },
            evidence = (p[7] as List<EvidenceEntity>).map { it.toDomain() },
            dayMeta = (p[8] as List<DayMetaEntity>).map { it.toDomain() }.associateBy { it.day },
            weeklyReviews = (p[9] as List<WeeklyReviewEntity>).map { it.toDomain() }.associateBy { it.weekStart },
            legacyTaskLogs = (p[10] as List<TaskLogEntity>).size,
        )
    }

    // ------------------------------------------------------------------ the plan

    private fun asset(name: String): String = context.assets.open(name).use { String(it.readBytes(), Charsets.UTF_8) }

    /** A plan CSV you imported replaces the bundled one (kept in the app's private files). */
    private val importedCsv: File get() = File(context.filesDir, "plan/schedule.csv")

    /** Loads the plan (and the practice cards on first run). Safe to call again. */
    suspend fun loadPlan() = withContext(Dispatchers.IO) {
        val load = try {
            val meta = asset("plan_meta.json")
            val csv = if (importedCsv.exists()) importedCsv.readText() else asset("schedule.csv")
            PlanLoad(PlanMetaJson.plan(meta, csv), null)
        } catch (e: Exception) {
            // Never lose data over a bad plan file: fall back to the bundled plan and say what went wrong.
            try {
                PlanLoad(PlanMetaJson.plan(asset("plan_meta.json"), asset("schedule.csv")), "Your imported plan couldn't be read: ${e.message}")
            } catch (e2: Exception) {
                PlanLoad(null, e2.message)
            }
        }
        if (dao.cardCount() == 0) {
            val (version, cards) = LabJson.parseCards(asset("cards.json"))
            db.withTransaction {
                dao.upsertCards(cards.map { it.toEntity() })
                dao.upsertMeta(listOf(MetaEntity("cardsVersion", version)))
            }
        }
        planFlow.value = load
        load.plan?.let { unlockCards(it, Days.today()) }
    }

    /** Checks a CSV before using it: same columns as the roadmap's CSV, unique task ids, valid dates. */
    fun checkCsv(text: String): Result<Int> = runCatching {
        val tasks = PlanCsv.tasks(text)
        if (tasks.isEmpty()) throw PlanCsv.CsvException("The file has no tasks.")
        tasks.size
    }

    /** Uses an imported plan CSV from now on. Your ticks and scores stay: they're linked by task id. */
    suspend fun importPlanCsv(text: String): Int = withContext(Dispatchers.IO) {
        val count = checkCsv(text).getOrThrow()
        importedCsv.parentFile?.mkdirs()
        importedCsv.writeText(text)
        loadPlan()
        count
    }

    suspend fun useBundledPlan() = withContext(Dispatchers.IO) {
        importedCsv.delete()
        loadPlan()
    }

    fun usingImportedPlan(): Boolean = importedCsv.exists()

    private suspend fun unlockCards(plan: DayPlan, day: Long) {
        val done = Mastery.states(dao.taskEvents().map { it.toDomain() }).filterValues { it.progress == Progress.COMPLETED }.keys
        val fresh = Leitner.unlockForItems(done, dao.cards().map { it.toDomain() }, plan.meta.cardUnlocks,
            dao.cardStates().map { it.toDomain() }, day)
        if (fresh.isNotEmpty()) dao.upsertCardStates(fresh.map { it.toEntity() })
    }

    // ------------------------------------------------------------------ tasks

    /**
     * Records what happened to a task. Every change is a new row; "undo" is a RESET row, so
     * statistics always follow the latest state and nothing is ever counted twice.
     */
    suspend fun setTask(id: String, type: EventType, minutes: Int = 0, independent: Boolean = false, note: String = "", day: Long = Days.today()) {
        db.withTransaction {
            dao.insertTaskEvent(TaskEventEntity(itemId = id, day = day, type = type.key, minutes = minutes, independent = independent,
                note = note, createdAt = Days.nowMillis()))
        }
        if (type == EventType.DONE) planFlow.value?.plan?.let { unlockCards(it, day) }
    }

    /** Saves one attempt at a check. Retakes add rows; nothing is overwritten. */
    suspend fun addAssessment(a: Assessment): Long = dao.insertAssessment(a.copy(id = 0, createdAt = Days.nowMillis()).toEntity())

    /** A small daily tick (slept 7 h, a reduced-routine action, flashcards). Ticking again edits the same row. */
    suspend fun tick(day: Long, id: String, done: Boolean, minutes: Int) {
        dao.upsertDayTick(DayTickEntity(day, id, done, minutes, Days.nowMillis()))
    }

    // ------------------------------------------------------------------ evidence

    /**
     * Saves an evidence line or a STAR story (new or edited: same id, no duplicates).
     * A daily line with a real result also ticks that day's "log line" task, as the plan asks.
     */
    suspend fun saveEvidence(e: Evidence): Long {
        val now = Days.nowMillis()
        return db.withTransaction {
            val id = if (e.id == 0L) dao.upsertEvidence(e.copy(createdAt = now, updatedAt = now).toEntity())
            else { dao.upsertEvidence(e.copy(updatedAt = now).toEntity()); e.id }
            if (e.starred) {
                val ws = Days.weekStart(e.day)
                dao.unstarOthers(ws, ws + 6, id, now)
            }
            val plan = planFlow.value?.plan
            if (plan != null && e.kind == "line" && e.hasRealResult) {
                val row = plan.tasksOn(e.day).firstOrNull { it.area == "evidence" && it.kind == TaskKind.DAILY }
                if (row != null) {
                    val states = Mastery.states(dao.taskEventsFor(row.id).map { it.toDomain() })
                    if (states[row.id]?.progress != Progress.COMPLETED) {
                        dao.insertTaskEvent(TaskEventEntity(itemId = row.id, day = e.day, type = EventType.DONE.key, minutes = row.minutes,
                            independent = false, note = "Ticked when you saved the log line", createdAt = now))
                    }
                }
            }
            id
        }
    }

    suspend fun deleteEvidence(id: Long) = dao.deleteEvidence(id)

    // ------------------------------------------------------------------ days

    /**
     * Marks a day as a planned rest day, a catch-up day or a reduced-routine day (or back to normal).
     * Rest days must be planned: only today or a later day can become one.
     */
    suspend fun setDayKind(day: Long, kind: DayKind?) {
        require(kind != DayKind.REST || day >= Days.today()) { "Rest days must be planned ahead (today or later)." }
        val old = dao.dayMetaOn(day)?.toDomain()
        val now = Days.nowMillis()
        dao.upsertDayMeta(DayMeta(day, kind, old?.focusItemId, if (kind != null) now else null, now).toEntity())
    }

    /** The reduced routine for a whole week (from [from] to that week's Sunday). [on] = false goes back to normal. */
    suspend fun setReducedWeek(from: Long, on: Boolean) {
        val end = Days.weekStart(from) + 6
        db.withTransaction {
            for (d in from..end) {
                val old = dao.dayMetaOn(d)?.toDomain()
                if (on && old?.kind == DayKind.REST) continue
                if (!on && old?.kind != DayKind.REDUCED) continue
                setDayKind(d, if (on) DayKind.REDUCED else null)
            }
        }
    }

    suspend fun setFocus(day: Long, itemId: String?) {
        val old = dao.dayMetaOn(day)?.toDomain()
        dao.upsertDayMeta(DayMeta(day, old?.kind, itemId, old?.kindSetAt, Days.nowMillis()).toEntity())
    }

    /** The end-of-day review: a short note. */
    suspend fun saveDayNote(day: Long, note: String) {
        db.withTransaction {
            val old = dao.dayLog(day)?.toDomain() ?: DayLog(day)
            dao.upsertDayLog(old.copy(note = note, eveningDone = true, finishedAt = old.finishedAt ?: Days.nowMillis()).toEntity())
        }
    }

    // ------------------------------------------------------------------ weekly review

    /**
     * Saves the checkpoint table. When completed: ticks the week's review task, and if you chose
     * "reduced" for next week, sets the reduced routine for every day of it.
     */
    suspend fun saveWeeklyReview(r: WeeklyReview, complete: Boolean) {
        val now = Days.nowMillis()
        val plan = planFlow.value?.plan
        db.withTransaction {
            val old = dao.weeklyReview(r.weekStart)?.toDomain()
            val completedAt = if (complete) (old?.completedAt ?: now) else old?.completedAt
            dao.upsertWeeklyReview(r.copy(completedAt = completedAt, updatedAt = now).toEntity())
            if (complete && plan != null) {
                val row = plan.tasks.firstOrNull { it.area == "review" && it.day in r.weekStart..(r.weekStart + 6) }
                if (row != null && Mastery.states(dao.taskEventsFor(row.id).map { it.toDomain() })[row.id]?.progress != Progress.COMPLETED) {
                    dao.insertTaskEvent(TaskEventEntity(itemId = row.id, day = Days.today(), type = EventType.DONE.key, minutes = row.minutes,
                        independent = false, note = "Weekly review completed", createdAt = now))
                }
            }
        }
        if (complete) {
            val next = r.weekStart + 7
            if (r.nextLoad == "reduced") setReducedWeek(next, true) else setReducedWeek(next, false)
        }
    }

    // ------------------------------------------------------------------ flashcards

    suspend fun review(cardId: String, rating: Rating, asMcq: Boolean) {
        val today = Days.today()
        db.withTransaction {
            val state = dao.cardState(cardId)?.toDomain() ?: Leitner.newState(cardId, today)
            dao.upsertCardState(Leitner.apply(state, rating, today, asMcq).toEntity())
            dao.insertReviewLog(ReviewLogEntity(cardId = cardId, day = today, rating = rating.name,
                correct = rating != Rating.AGAIN, asMcq = asMcq, createdAt = Days.nowMillis()))
        }
    }

    /** Your own flashcard (A5-style: "make 3 flashcards"). Due today; your history stays if the bundled cards change. */
    suspend fun addCard(topic: String, prompt: String, answer: String) {
        val id = Leitner.USER_PREFIX + Days.nowMillis()
        db.withTransaction {
            dao.upsertCards(listOf(Card(id, topic, CardType.FLASHCARD, prompt.trim(), emptyList(), answer.trim(), "", null).toEntity()))
            dao.upsertCardState(Leitner.newState(id, Days.today()).toEntity())
        }
    }

    // ------------------------------------------------------------------ backup and restore

    suspend fun snapshot(): Snapshot {
        val meta = dao.meta().associate { it.key to it.value }
        val legacyTasks = dao.tasks().map { it.toDomain() }
        val legacy = if (legacyTasks.isEmpty()) null else LegacyRoadmap(
            version = meta["roadmapVersion"] ?: "", title = meta["roadmapTitle"] ?: "",
            timelineStart = meta["timelineStart"]?.toLongOrNull() ?: legacyTasks.minOf { it.plannedDay },
            timelineEnd = meta["timelineEnd"]?.toLongOrNull() ?: legacyTasks.maxOf { it.plannedDay },
            tracks = meta["tracks"]?.split('\n')?.filter { it.isNotBlank() } ?: emptyList(),
            phases = dao.phases().map { it.toDomain() }, milestones = dao.milestones().map { it.toDomain() }, tasks = legacyTasks,
        )
        return Snapshot(
            roadmap = legacy, cardsVersion = meta["cardsVersion"] ?: "", cards = dao.cards().map { it.toDomain() },
            taskLogs = dao.taskLogs().map { it.toDomain() }, dayLogs = dao.dayLogs().map { it.toDomain() },
            cardStates = dao.cardStates().map { it.toDomain() }, reviewLogs = dao.reviewLogs().map { it.toDomain() },
            dayPlans = dao.dayPlans().map { it.toDomain() }, settings = settings.current(),
            events = dao.taskEvents().map { it.toDomain() }, assessments = dao.assessments().map { it.toDomain() },
            ticks = dao.dayTicks().map { it.toDomain() }, evidence = dao.evidence().map { it.toDomain() },
            dayMeta = dao.dayMeta().map { it.toDomain() }, weeklyReviews = dao.weeklyReviews().map { it.toDomain() },
            planVersion = planFlow.value?.plan?.meta?.version ?: "",
        )
    }

    /** Before any restore, a copy of what's on the phone now is saved privately (the last 3 are kept). */
    private suspend fun safetyCopy(appVersion: String): File {
        val dir = File(context.filesDir, "safety").apply { mkdirs() }
        val file = File(dir, "before-restore-${Days.nowMillis()}.json")
        file.writeText(LabJson.encodeBackup(snapshot(), appVersion, java.time.LocalDateTime.now(Days.ZONE).withNano(0).toString()))
        dir.listFiles()?.sortedByDescending { it.name }?.drop(3)?.forEach { it.delete() }
        return file
    }

    fun safetyCopies(): List<File> = File(context.filesDir, "safety").listFiles()?.sortedByDescending { it.name } ?: emptyList()

    /**
     * Replaces everything on the phone with the backup's contents, in one transaction (all or nothing).
     * The file is fully read and checked before anything is touched, and a safety copy is saved first.
     */
    suspend fun restore(s: Snapshot, appVersion: String) {
        safetyCopy(appVersion)
        db.withTransaction {
            dao.clearPhases(); dao.clearTasks(); dao.clearMilestones(); dao.clearCards(); dao.clearCardStates()
            dao.clearTaskLogs(); dao.clearDayLogs(); dao.clearReviewLogs(); dao.clearDayPlans(); dao.clearMeta()
            dao.clearTaskEvents(); dao.clearAssessments(); dao.clearDayTicks(); dao.clearEvidence(); dao.clearDayMeta(); dao.clearWeeklyReviews()
            s.roadmap?.let { r ->
                dao.upsertPhases(r.phases.map { it.toEntity() })
                dao.upsertTasks(r.tasks.map { it.toEntity() })
                dao.upsertMilestones(r.milestones.map { it.toEntity() })
                dao.upsertMeta(listOf(MetaEntity("roadmapVersion", r.version), MetaEntity("roadmapTitle", r.title),
                    MetaEntity("timelineStart", r.timelineStart.toString()), MetaEntity("timelineEnd", r.timelineEnd.toString()),
                    MetaEntity("tracks", r.tracks.joinToString("\n"))))
            }
            dao.upsertMeta(listOf(MetaEntity("cardsVersion", s.cardsVersion)))
            dao.upsertCards(s.cards.map { it.toEntity() })
            dao.upsertCardStates(s.cardStates.map { it.toEntity() })
            dao.upsertTaskLogs(s.taskLogs.map { it.toEntity() })
            dao.upsertDayLogs(s.dayLogs.map { it.toEntity() })
            dao.upsertReviewLogs(s.reviewLogs.map { it.toEntity() })
            dao.insertDayPlans(s.dayPlans.map { it.toEntity() })
            dao.upsertTaskEvents(s.events.map { it.toEntity() })
            dao.upsertAssessments(s.assessments.map { it.toEntity() })
            dao.upsertDayTicks(s.ticks.map { it.toEntity() })
            dao.upsertEvidenceList(s.evidence.map { it.toEntity() })
            dao.upsertDayMetaList(s.dayMeta.map { it.toEntity() })
            dao.upsertWeeklyReviews(s.weeklyReviews.map { it.toEntity() })
        }
        settings.update { s.settings.copy(onboarded = true, v2Intro = true) }
        if (dao.cardCount() == 0) loadPlan()
    }

    suspend fun markBackedUp() {
        val today = Days.today()
        settings.update { it.copy(lastBackupDay = today) }
    }

    /** Your evidence log and STAR stories as plain text, for interviews (fictional/anonymised data only). */
    suspend fun evidenceText(plan: DayPlan?): String {
        val all = dao.evidence().map { it.toDomain() }
        val sb = StringBuilder("# DRC evidence log\n\nSay “the client”, never a name. No PAN, GSTIN, bank details or invoices.\n\n")
        val stories = all.filter { it.isStory }
        if (stories.isNotEmpty()) {
            sb.append("## STAR stories\n\n")
            for (e in stories) {
                sb.append("### ${plan?.task(e.itemId ?: "")?.title ?: "STAR story"} (${Days.short(e.day)})\n")
                sb.append("- Situation: ${e.starS}\n- Task: ${e.starT}\n- Action: ${e.starA}\n- Result: ${e.starR}\n\n")
            }
        }
        sb.append("## Daily lines\n\n| Date | Area | Task | Problem | Action | Result | Skill | Evidence | Lesson |\n|---|---|---|---|---|---|---|---|---|\n")
        for (e in all.filter { !it.isStory }) {
            fun c(t: String) = t.replace("|", "/").replace("\n", " ")
            sb.append("| ${Days.iso(e.day)}${if (e.starred) " ★" else ""} | ${c(plan?.area(e.area)?.short ?: e.area)} | ${c(e.task)} | ${c(e.problem)} | " +
                "${c(e.action)} | ${c(e.result)} | ${c(e.skill)} | ${c(e.proof)} | ${c(e.lesson)} |\n")
        }
        return sb.toString()
    }

    /** Due flashcards today (for the reminder). */
    suspend fun dueCards(day: Long): Int = dao.cardStates().count { it.dueDay <= day }.coerceAtMost(Leitner.DAILY_CAP)

    /** Today's study minutes and target, for the reminder (it stays quiet once today's minimum is met). */
    suspend fun todayMinutes(day: Long): Pair<Int, Int> {
        val plan = planFlow.value?.plan ?: return 0 to 0
        val states = Mastery.states(dao.taskEvents().map { it.toDomain() })
        val minutes = com.lakshman.roadmaplab.domain.Consistency.minutesByDay(plan, states.values, dao.dayTicksOn(day).map { it.toDomain() })[day] ?: 0
        val type = com.lakshman.roadmaplab.domain.Consistency.dayType(day, plan, dao.dayMetaOn(day)?.toDomain())
        return minutes to settings.current().targets.of(type)
    }

    fun flashcardsId(): String = TodayEngine.FLASHCARDS
}
