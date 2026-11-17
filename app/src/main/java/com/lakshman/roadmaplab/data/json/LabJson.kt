package com.lakshman.roadmaplab.data.json

import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayMeta
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Evidence
import com.lakshman.roadmaplab.domain.ItemEvent
import com.lakshman.roadmaplab.domain.RoutineCheck
import com.lakshman.roadmaplab.domain.Targets
import com.lakshman.roadmaplab.domain.WeeklyReview
import com.lakshman.roadmaplab.domain.Card
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.CardType
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.DayPlanEntry
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.LegacyMilestone
import com.lakshman.roadmaplab.domain.Mode
import com.lakshman.roadmaplab.domain.LegacyPhase
import com.lakshman.roadmaplab.domain.LegacyTask
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.ReviewLog
import com.lakshman.roadmaplab.domain.LegacyRoadmap
import com.lakshman.roadmaplab.domain.TaskLog
import com.lakshman.roadmaplab.domain.TaskStatus
import org.json.JSONArray
import org.json.JSONObject

/** A complete copy of the app's data: what a backup file holds. */
data class Snapshot(
    /** The version-1 roadmap, if this phone ever had it (null otherwise). */
    val roadmap: LegacyRoadmap?,
    val cardsVersion: String,
    val cards: List<Card>,
    val taskLogs: List<TaskLog>,
    val dayLogs: List<DayLog>,
    val cardStates: List<CardState>,
    val reviewLogs: List<ReviewLog>,
    val dayPlans: List<DayPlanEntry>,
    val settings: AppSettings,
    // ---- version 2
    val events: List<ItemEvent> = emptyList(),
    val assessments: List<Assessment> = emptyList(),
    val ticks: List<RoutineCheck> = emptyList(),
    val evidence: List<Evidence> = emptyList(),
    val dayMeta: List<DayMeta> = emptyList(),
    val weeklyReviews: List<WeeklyReview> = emptyList(),
    val planVersion: String = "",
)

class LabFileException(message: String) : Exception(message)

/** Reads and writes roadmap.json, cards.json and backup files. */
object LabJson {
    const val BACKUP_FORMAT = 2

    // ------------------------------------------------------------- roadmap.json
    fun parseRoadmap(text: String): LegacyRoadmap = parseRoadmap(obj(text, "roadmap"))

    fun parseRoadmap(o: JSONObject): LegacyRoadmap {
        if (o.optString("kind", "roadmap") != "roadmap" || !o.has("tasks")) {
            throw LabFileException("This isn't a roadmap.json file.")
        }
        val timeline = o.optJSONObject("timeline") ?: JSONObject()
        val phases = o.optJSONArray("phases").objects().mapIndexed { i, p ->
            LegacyPhase(p.req("id"), p.optString("track"), p.optString("title"), day(p, "start"), day(p, "end"), i)
        }
        val milestones = o.optJSONArray("milestones").objects().map { m ->
            LegacyMilestone(m.req("id"), m.optString("title"), day(m, "date"), m.optString("track"))
        }
        val seen = HashSet<String>()
        val tasks = o.optJSONArray("tasks").objects().mapIndexed { i, t ->
            val id = t.req("id")
            if (!seen.add(id)) throw LabFileException("Task id $id appears twice.")
            val min = t.optJSONObject("minutes") ?: JSONObject()
            val full = min.optInt("full", 30)
            LegacyTask(
                id = id,
                phaseId = t.optString("phase"),
                track = t.optString("track"),
                plannedDay = day(t, "plannedDate"),
                title = t.optString("title"),
                learn = t.optString("learn"),
                apply = t.optString("apply"),
                doneWhen = t.optString("doneWhen"),
                minFull = full,
                minDrc = min.optInt("drc", full),
                minMinimum = min.optInt("minimum", 15),
                unlocks = t.optJSONArray("unlocksCards").strings(),
                core = t.optBoolean("core", true),
                assumedDone = t.optBoolean("assumedDone", false),
                order = i,
            )
        }
        val tracks = o.optJSONArray("tracks").strings().ifEmpty { tasks.map { it.track }.distinct() }
        val start = if (timeline.has("start")) day(timeline, "start") else tasks.minOfOrNull { it.plannedDay } ?: Days.today()
        val end = if (timeline.has("end")) day(timeline, "end") else tasks.maxOfOrNull { it.plannedDay } ?: Days.today()
        return LegacyRoadmap(o.optString("version"), o.optString("title"), start, end, tracks, phases, milestones, tasks)
    }

    fun roadmapToJson(r: LegacyRoadmap): JSONObject = JSONObject().apply {
        put("app", "roadmaplab")
        put("kind", "roadmap")
        put("version", r.version)
        put("title", r.title)
        put("timeline", JSONObject().put("start", Days.iso(r.timelineStart)).put("end", Days.iso(r.timelineEnd)))
        put("tracks", JSONArray(r.tracks))
        put("phases", JSONArray().apply {
            r.phases.forEach {
                put(JSONObject().put("id", it.id).put("track", it.track).put("title", it.title)
                    .put("start", Days.iso(it.startDay)).put("end", Days.iso(it.endDay)))
            }
        })
        put("milestones", JSONArray().apply {
            r.milestones.forEach {
                put(JSONObject().put("id", it.id).put("title", it.title).put("date", Days.iso(it.day)).put("track", it.track))
            }
        })
        put("tasks", JSONArray().apply {
            r.tasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("phase", t.phaseId); put("track", t.track)
                    put("plannedDate", Days.iso(t.plannedDay)); put("title", t.title)
                    put("learn", t.learn); put("apply", t.apply); put("doneWhen", t.doneWhen)
                    put("minutes", JSONObject().put("full", t.minFull).put("drc", t.minDrc).put("minimum", t.minMinimum))
                    put("unlocksCards", JSONArray(t.unlocks)); put("core", t.core); put("assumedDone", t.assumedDone)
                })
            }
        })
    }

    // ------------------------------------------------------------- cards.json
    fun parseCards(text: String): Pair<String, List<Card>> = parseCards(obj(text, "cards"))

    fun parseCards(o: JSONObject): Pair<String, List<Card>> {
        if (o.optString("kind", "cards") != "cards" || !o.has("cards")) {
            throw LabFileException("This isn't a cards.json file.")
        }
        val seen = HashSet<String>()
        val cards = o.optJSONArray("cards").objects().map { c ->
            val id = c.req("id")
            if (!seen.add(id)) throw LabFileException("Card id $id appears twice.")
            val type = if (c.optString("type") == "flashcard") CardType.FLASHCARD else CardType.MCQ
            val options = c.optJSONArray("options").strings()
            val answer = c.optString("answer")
            if (type == CardType.MCQ && answer !in options) throw LabFileException("Card $id: the answer isn't one of its options.")
            Card(
                id = id, topic = c.optString("topic", "General"), type = type, prompt = c.optString("prompt"),
                options = options, answer = answer, explanation = c.optString("explanation"),
                unlockedBy = c.optString("unlockedBy").ifBlank { null },
            )
        }
        return o.optString("version") to cards
    }

    fun cardsToJson(version: String, cards: List<Card>): JSONObject = JSONObject().apply {
        put("app", "roadmaplab")
        put("kind", "cards")
        put("version", version)
        put("cards", JSONArray().apply {
            cards.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("topic", c.topic)
                    put("type", if (c.type == CardType.FLASHCARD) "flashcard" else "mcq")
                    put("prompt", c.prompt); put("options", JSONArray(c.options)); put("answer", c.answer)
                    put("explanation", c.explanation); put("unlockedBy", c.unlockedBy ?: "")
                })
            }
        })
    }

    // ------------------------------------------------------------- backup
    fun encodeBackup(s: Snapshot, appVersion: String, exportedAtIso: String): String {
        val root = JSONObject()
        root.put("app", "roadmaplab")
        root.put("kind", "backup")
        root.put("format", BACKUP_FORMAT)
        root.put("appVersion", appVersion)
        root.put("exportedAt", exportedAtIso)
        root.put("settings", settingsToJson(s.settings))
        root.put("planVersion", s.planVersion)
        s.roadmap?.let { root.put("roadmap", roadmapToJson(it)) }
        root.put("cards", cardsToJson(s.cardsVersion, s.cards))
        root.put("taskLogs", JSONArray().apply {
            s.taskLogs.forEach {
                put(JSONObject().put("id", it.id).put("task", it.taskId).put("day", Days.iso(it.day))
                    .put("status", it.status.key).put("minutes", it.minutes).put("note", it.note)
                    .put("createdAt", it.createdAt).put("usedMinimum", it.usedMinimum))
            }
        })
        root.put("dayLogs", JSONArray().apply {
            s.dayLogs.forEach { d ->
                put(JSONObject().apply {
                    put("day", Days.iso(d.day))
                    d.mood?.let { put("mood", it) }
                    d.energy?.let { put("energy", it) }
                    d.sleepHours?.let { put("sleepHours", it.toDouble()) }
                    put("ifThen", d.ifThen); put("note", d.note)
                    put("evidence", JSONObject().put("module", d.evModule).put("problem", d.evProblem)
                        .put("action", d.evAction).put("result", d.evResult).put("learning", d.evLearning))
                    put("eveningDone", d.eveningDone)
                    d.logNumber?.let { put("logNumber", it) }
                    d.finishedAt?.let { put("finishedAt", it) }
                })
            }
        })
        root.put("cardStates", JSONArray().apply {
            s.cardStates.forEach { c ->
                put(JSONObject().apply {
                    put("card", c.cardId); put("box", c.box); put("mcCorrect", c.mcCorrect)
                    put("due", Days.iso(c.dueDay)); put("lapses", c.lapses); put("reviews", c.reviews)
                    c.lastReviewDay?.let { put("lastReview", Days.iso(it)) }
                    put("unlocked", Days.iso(c.unlockedDay))
                })
            }
        })
        root.put("reviewLogs", JSONArray().apply {
            s.reviewLogs.forEach {
                put(JSONObject().put("card", it.cardId).put("day", Days.iso(it.day)).put("rating", it.rating.name)
                    .put("correct", it.correct).put("asMcq", it.asMcq).put("createdAt", it.createdAt))
            }
        })
        root.put("dayPlans", JSONArray().apply {
            s.dayPlans.forEach { put(JSONObject().put("day", Days.iso(it.day)).put("task", it.taskId).put("position", it.position)) }
        })
        // ---- version 2
        root.put("taskEvents", JSONArray().apply {
            s.events.forEach {
                put(JSONObject().put("id", it.id).put("task", it.itemId).put("day", Days.iso(it.day)).put("type", it.type.key)
                    .put("minutes", it.minutes).put("independent", it.independent).put("note", it.note).put("createdAt", it.createdAt))
            }
        })
        root.put("assessments", JSONArray().apply {
            s.assessments.forEach {
                put(JSONObject().put("id", it.id).put("task", it.itemId ?: "").put("area", it.area).put("topic", it.topic)
                    .put("type", it.type).put("score", it.score).put("max", it.max).put("passed", it.passed)
                    .put("independent", it.independent).put("detail", it.detail).put("day", Days.iso(it.day))
                    .put("createdAt", it.createdAt).put("seconds", it.seconds))
            }
        })
        root.put("dayTicks", JSONArray().apply {
            s.ticks.forEach {
                put(JSONObject().put("day", Days.iso(it.day)).put("id", it.routineId).put("done", it.done)
                    .put("minutes", it.minutes).put("updatedAt", it.updatedAt))
            }
        })
        root.put("evidence", JSONArray().apply {
            s.evidence.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("day", Days.iso(e.day)); put("area", e.area); put("task", e.itemId ?: "")
                    put("kind", e.kind); put("what", e.task); put("problem", e.problem); put("action", e.action)
                    put("result", e.result); put("lesson", e.lesson); put("skill", e.skill); put("proof", e.proof)
                    e.confidence?.let { put("confidence", it) }
                    put("starred", e.starred); put("starS", e.starS); put("starT", e.starT); put("starA", e.starA); put("starR", e.starR)
                    put("createdAt", e.createdAt); put("updatedAt", e.updatedAt)
                })
            }
        })
        root.put("dayMeta", JSONArray().apply {
            s.dayMeta.forEach { m ->
                put(JSONObject().apply {
                    put("day", Days.iso(m.day)); m.kind?.let { put("kind", it.key) }; m.focusItemId?.let { put("focus", it) }
                    m.kindSetAt?.let { put("kindSetAt", it) }; put("updatedAt", m.updatedAt)
                })
            }
        })
        root.put("weeklyReviews", JSONArray().apply {
            s.weeklyReviews.forEach { w ->
                put(JSONObject().apply {
                    put("weekStart", Days.iso(w.weekStart)); put("actual", JSONObject(w.actual as Map<*, *>))
                    put("assessment", JSONObject(w.assessment as Map<*, *>)); put("adjustment", JSONObject(w.adjustment as Map<*, *>))
                    put("missedWhy", w.missedWhy); put("tags", JSONArray(w.obstacleTags)); put("best", w.best); put("weakest", w.weakest)
                    put("priorities", w.priorities); put("nextLoad", w.nextLoad); put("spending", w.spending)
                    w.completedAt?.let { put("completedAt", it) }; put("updatedAt", w.updatedAt)
                })
            }
        })
        return root.toString(1)
    }

    fun decodeBackup(text: String): Snapshot {
        val root = obj(text, "backup")
        if (root.optString("app") != "roadmaplab" || root.optString("kind") != "backup") {
            throw LabFileException("This isn't a Roadmap Lab backup file.")
        }
        if (root.optInt("format", 0) > BACKUP_FORMAT) {
            throw LabFileException("This backup was made by a newer version of the app. Update the app first.")
        }
        val roadmap = root.optJSONObject("roadmap")?.let { parseRoadmap(it) }
        val (cardsVersion, cards) = parseCards(root.getJSONObject("cards"))
        val taskLogs = root.optJSONArray("taskLogs").objects().map {
            TaskLog(
                id = it.optLong("id", 0), taskId = it.req("task"), day = day(it, "day"),
                status = TaskStatus.of(it.optString("status")) ?: TaskStatus.SKIPPED,
                minutes = it.optInt("minutes", 0), note = it.optString("note"),
                createdAt = it.optLong("createdAt", 0), usedMinimum = it.optBoolean("usedMinimum", false),
            )
        }
        val dayLogs = root.optJSONArray("dayLogs").objects().map { d ->
            val ev = d.optJSONObject("evidence") ?: JSONObject()
            DayLog(
                day = day(d, "day"),
                mood = if (d.has("mood")) d.getInt("mood") else null,
                energy = if (d.has("energy")) d.getInt("energy") else null,
                sleepHours = if (d.has("sleepHours")) d.getDouble("sleepHours").toFloat() else null,
                ifThen = d.optString("ifThen"), note = d.optString("note"),
                evModule = ev.optString("module"), evProblem = ev.optString("problem"), evAction = ev.optString("action"),
                evResult = ev.optString("result"), evLearning = ev.optString("learning"),
                eveningDone = d.optBoolean("eveningDone", false),
                logNumber = if (d.has("logNumber")) d.getInt("logNumber") else null,
                finishedAt = if (d.has("finishedAt")) d.getLong("finishedAt") else null,
            )
        }
        val states = root.optJSONArray("cardStates").objects().map { c ->
            CardState(
                cardId = c.req("card"), box = c.optInt("box", 0), mcCorrect = c.optInt("mcCorrect", 0),
                dueDay = day(c, "due"), lapses = c.optInt("lapses", 0), reviews = c.optInt("reviews", 0),
                lastReviewDay = if (c.has("lastReview")) day(c, "lastReview") else null,
                unlockedDay = day(c, "unlocked"),
            )
        }
        val reviews = root.optJSONArray("reviewLogs").objects().map {
            ReviewLog(
                cardId = it.req("card"), day = day(it, "day"),
                rating = runCatching { Rating.valueOf(it.optString("rating")) }.getOrDefault(Rating.GOOD),
                correct = it.optBoolean("correct", true), asMcq = it.optBoolean("asMcq", false),
                createdAt = it.optLong("createdAt", 0),
            )
        }
        val plans = root.optJSONArray("dayPlans").objects().map {
            DayPlanEntry(day(it, "day"), it.req("task"), it.optInt("position", 0))
        }
        val events = root.optJSONArray("taskEvents").objects().map {
            ItemEvent(it.optLong("id", 0), it.req("task"), day(it, "day"), EventType.of(it.optString("type")), it.optInt("minutes"),
                it.optBoolean("independent"), it.optString("note"), it.optLong("createdAt"))
        }
        val assessments = root.optJSONArray("assessments").objects().map {
            Assessment(it.optLong("id", 0), it.optString("task").ifBlank { null }, it.optString("area"), it.optString("topic"),
                it.optString("type"), it.optDouble("score", 0.0), it.optDouble("max", 0.0), it.optBoolean("passed"),
                it.optBoolean("independent"), it.optString("detail"), day(it, "day"), it.optLong("createdAt"), it.optInt("seconds"))
        }
        val ticks = root.optJSONArray("dayTicks").objects().map {
            RoutineCheck(day(it, "day"), it.req("id"), it.optBoolean("done"), it.optInt("minutes"), it.optLong("updatedAt"))
        }
        var evidence = root.optJSONArray("evidence").objects().map {
            Evidence(it.optLong("id", 0), day(it, "day"), it.optString("area", "evidence"), it.optString("task").ifBlank { null },
                it.optString("kind", "line"), it.optString("what"), it.optString("problem"), it.optString("action"), it.optString("result"),
                it.optString("lesson"), it.optString("skill"), it.optString("proof"), if (it.has("confidence")) it.getInt("confidence") else null,
                it.optBoolean("starred"), it.optString("starS"), it.optString("starT"), it.optString("starA"), it.optString("starR"),
                it.optLong("createdAt"), it.optLong("updatedAt"))
        }
        if (root.optInt("format", 1) < 2) {
            // A version-1 backup: its evidence lines move into the evidence log, exactly as the database upgrade does.
            evidence = dayLogs.filter { it.hasEvidence }.map { d ->
                val at = d.finishedAt ?: (d.day * 86_400_000L)
                Evidence(0, d.day, "evidence", null, "line", d.evModule, d.evProblem, d.evAction, d.evResult, d.evLearning, "", "", null,
                    false, "", "", "", "", at, at)
            }
        }
        val dayMeta = root.optJSONArray("dayMeta").objects().map {
            DayMeta(day(it, "day"), DayKind.of(it.optString("kind").ifBlank { null }), it.optString("focus").ifBlank { null },
                if (it.has("kindSetAt")) it.getLong("kindSetAt") else null, it.optLong("updatedAt"))
        }
        val weekly = root.optJSONArray("weeklyReviews").objects().map { w ->
            fun map(key: String) = w.optJSONObject(key)?.let { o -> o.keys().asSequence().associateWith { o.getString(it) } } ?: emptyMap()
            WeeklyReview(day(w, "weekStart"), map("actual"), map("assessment"), map("adjustment"), w.optString("missedWhy"),
                w.optJSONArray("tags").strings(), w.optString("best"), w.optString("weakest"), w.optString("priorities"),
                w.optString("nextLoad", "normal"), w.optString("spending"), if (w.has("completedAt")) w.getLong("completedAt") else null,
                w.optLong("updatedAt"))
        }
        return Snapshot(roadmap, cardsVersion, cards, taskLogs, dayLogs, states, reviews, plans,
            settingsFromJson(root.optJSONObject("settings") ?: JSONObject()),
            events, assessments, ticks, evidence, dayMeta, weekly, root.optString("planVersion"))
    }

    fun settingsToJson(s: AppSettings): JSONObject = JSONObject().apply {
        put("onboarded", s.onboarded); put("mode", s.mode.key); put("reducedPlan", s.reducedPlan)
        put("reminderOn", s.reminderOn); put("reminderHour", s.reminderHour); put("reminderMinute", s.reminderMinute)
        put("theme", s.theme); put("firstUseDay", s.firstUseDay); put("lastBackupDay", s.lastBackupDay)
        put("showEvidenceLog", s.showEvidenceLog)
        put("v2Intro", s.v2Intro); put("cue", s.cue); put("weeklyReminder", s.weeklyReminder)
        put("freshStartDay", s.freshStartDay); put("freshStartAsked", s.freshStartAsked)
        put("freshStartPlan", s.freshStartPlan); put("dynamicColor", s.dynamicColor)
        put("targets", JSONObject().put("weekday", s.targets.weekday).put("saturday", s.targets.saturday).put("sunday", s.targets.sunday)
            .put("reduced", s.targets.reduced).put("catchup", s.targets.catchup))
    }

    fun settingsFromJson(o: JSONObject): AppSettings = AppSettings(
        onboarded = o.optBoolean("onboarded", true),
        mode = Mode.of(o.optString("mode")),
        reducedPlan = o.optBoolean("reducedPlan", false),
        reminderOn = o.optBoolean("reminderOn", false),
        reminderHour = o.optInt("reminderHour", 21).coerceIn(0, 23),
        reminderMinute = o.optInt("reminderMinute", 0).coerceIn(0, 59),
        theme = o.optString("theme", "light").ifBlank { "light" },
        firstUseDay = o.optLong("firstUseDay", 0),
        lastBackupDay = o.optLong("lastBackupDay", 0),
        showEvidenceLog = o.optBoolean("showEvidenceLog", true),
        v2Intro = o.optBoolean("v2Intro", false),
        cue = o.optString("cue", AppSettings().cue),
        weeklyReminder = o.optBoolean("weeklyReminder", true),
        freshStartDay = o.optLong("freshStartDay", 0),
        freshStartAsked = o.optBoolean("freshStartAsked", false),
        freshStartPlan = o.optString("freshStartPlan", ""),
        dynamicColor = o.optBoolean("dynamicColor", true),
        targets = o.optJSONObject("targets")?.let { t ->
            val d = Targets()
            Targets(t.optInt("weekday", d.weekday).coerceIn(0, 240), t.optInt("saturday", d.saturday).coerceIn(0, 240),
                t.optInt("sunday", d.sunday).coerceIn(0, 240), t.optInt("reduced", d.reduced).coerceIn(0, 240),
                t.optInt("catchup", d.catchup).coerceIn(0, 240))
        } ?: Targets(),
    )

    // ------------------------------------------------------------- helpers
    private fun obj(text: String, what: String): JSONObject = try {
        JSONObject(text)
    } catch (e: Exception) {
        throw LabFileException("The $what file couldn't be read (it isn't valid JSON).")
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONObject.req(key: String): String =
        optString(key).ifBlank { throw LabFileException("A record is missing its \"$key\".") }

    private fun day(o: JSONObject, key: String): Long = try {
        Days.of(o.getString(key))
    } catch (e: Exception) {
        throw LabFileException("Bad or missing date in \"$key\" (use YYYY-MM-DD).")
    }
}
