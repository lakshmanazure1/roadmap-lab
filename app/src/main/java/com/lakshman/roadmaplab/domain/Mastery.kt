package com.lakshman.roadmaplab.domain

/** Progress of one task from its events. */
enum class Progress { NOT_STARTED, IN_PROGRESS, COMPLETED, SKIPPED }

data class ItemState(
    val itemId: String,
    val progress: Progress,
    /** The day it was completed (latest DONE). */
    val doneDay: Long?,
    val doneMinutes: Int,
    /** Completed without help, notes or answers in view. */
    val independent: Boolean,
    /** Minutes of partial work per day ("did part of it"). */
    val partMinutes: Map<Long, Int>,
    val laterDays: Set<Long>,
    val lastChange: Long,
)

/**
 * The roadmap's 7-level scale (section 12), plus "Needs revision" from the brief:
 * shown instead of the level when the latest marked check is below its target.
 */
enum class Level(val number: Int, val label: String, val meaning: String) {
    NOT_STARTED(1, "Not started", "Nothing done yet"),
    IN_PROGRESS(2, "In progress", "Started, not finished"),
    COMPLETED(3, "Completed", "Watched / built once, possibly with help"),
    PRACTISED(4, "Practised independently", "Done again without help or answers"),
    ASSESSED(5, "Assessed", "Passed a timed or marked check"),
    DEMONSTRATED(6, "Demonstrated independently", "Shown or explained to someone else, unaided"),
    MASTERED(7, "Mastered", "Consistent over time, after a gap"),
    NEEDS_REVISION(0, "Needs revision", "Your latest check is below its target"),
    SKIPPED(0, "Skipped", "You let this one go"),
}

object Mastery {
    /** Default pass mark when the plan gives no target. */
    const val PASS = 70.0
    /** A passing check at least this many days after the first pass shows it lasted ("still 9/10 a month later"). */
    const val RETENTION_DAYS = 14

    /** Areas where the full scale applies. Elsewhere the output is the proof (up to "Assessed"); health is counted, not graded. */
    val FULL_SCALE = setOf("python", "accounting", "french", "comms")

    fun states(events: List<ItemEvent>): Map<String, ItemState> {
        val out = HashMap<String, ItemState>()
        for ((id, list) in events.groupBy { it.itemId }) {
            val sorted = list.sortedWith(compareBy({ it.createdAt }, { it.id }))
            var progress = Progress.NOT_STARTED
            var doneDay: Long? = null
            var doneMinutes = 0
            var independent = false
            val part = HashMap<Long, Int>()
            val later = HashSet<Long>()
            for (e in sorted) when (e.type) {
                EventType.START -> {
                    if (progress != Progress.COMPLETED) progress = Progress.IN_PROGRESS
                    if (e.minutes > 0) part[e.day] = (part[e.day] ?: 0) + e.minutes
                }
                EventType.DONE -> { progress = Progress.COMPLETED; doneDay = e.day; doneMinutes = e.minutes; independent = e.independent }
                EventType.RESET -> { progress = Progress.NOT_STARTED; doneDay = null; doneMinutes = 0; independent = false }
                EventType.SKIP -> { progress = Progress.SKIPPED; doneDay = null; doneMinutes = 0; independent = false }
                EventType.LATER -> later.add(e.day)
            }
            out[id] = ItemState(id, progress, doneDay, doneMinutes, independent, part, later, sorted.last().createdAt)
        }
        return out
    }

    /** Pass mark in percent for a check with this max and target (target 0 = default). */
    fun passMark(type: String, max: Double, target: Double): Double = when {
        target > 0 && max > 0 -> 100.0 * target / max
        type == "checklist" || type == "star" -> 100.0
        type == "score" && max <= 0 -> 0.0
        type == "explain" || type == "demo" -> 100.0
        else -> PASS
    }

    /**
     * The level, from what you recorded:
     *  1 Not started · 2 In progress · 3 Completed (ticked done)
     *  4 Practised independently: ticked "without help", or any check done cold (no notes, AI or answers)
     *  5 Assessed: a marked check passed its target (score, quiz, rubric or checklist)
     *  6 Demonstrated independently: shown or explained to someone else, unaided (a demo, training,
     *    recording scored with the rubric, or "explained it aloud without notes")
     *  7 Mastered: passed again at least 14 days after the first pass, and the latest check passed
     *  Needs revision: the latest marked check is below its target.
     */
    fun level(area: String, state: ItemState?, checks: List<Assessment>): Level {
        val progress = state?.progress ?: Progress.NOT_STARTED
        if (progress == Progress.SKIPPED) return Level.SKIPPED
        if (progress == Progress.NOT_STARTED) return if (checks.isEmpty()) Level.NOT_STARTED else Level.IN_PROGRESS
        if (progress == Progress.IN_PROGRESS) return Level.IN_PROGRESS
        if (area == "life" || area == "review") return Level.COMPLETED
        val graded = checks.filter { it.type != "outcome" }.sortedWith(compareBy({ it.createdAt }, { it.id }))
        val marked = graded.filter { it.type in MARKED }
        if (marked.isNotEmpty() && !marked.last().passed) return Level.NEEDS_REVISION
        val cap = if (area in FULL_SCALE) Level.MASTERED else Level.ASSESSED
        val passes = marked.filter { it.passed }
        var level = if (state?.independent == true || graded.any { it.independent && it.passed }) Level.PRACTISED else Level.COMPLETED
        if (passes.isNotEmpty()) level = Level.ASSESSED
        if (cap == Level.ASSESSED) return level
        if (graded.any { it.passed && it.type in DEMO_TYPES }) level = Level.DEMONSTRATED
        val first = passes.minOfOrNull { it.day }
        if (first != null && level.number >= Level.ASSESSED.number && passes.any { it.day - first >= RETENTION_DAYS }) level = Level.MASTERED
        return level
    }

    /** Checks that have a mark and a target. */
    val MARKED = setOf("quiz", "score", "rubric", "checklist", "star")
    /** Checks that show you can do it in front of someone, unaided. */
    val DEMO_TYPES = setOf("demo", "rubric")
    /** Checks with a real score (for averages and weak topics). */
    val SCORED_TYPES = setOf("quiz", "score", "rubric")

    data class TopicScore(val area: String, val topic: String, val avg: Double, val items: Int, val attempts: Int, val trend: Double?)

    /** Average of the latest score per task, per area/topic, with the trend across attempts. */
    fun topicScores(checks: List<Assessment>): List<TopicScore> {
        val graded = checks.filter { it.type in SCORED_TYPES && it.max > 0 }
        return graded.groupBy { it.area to it.topic }.map { (key, list) ->
            val latestPerItem = list.groupBy { it.itemId ?: "-" }.values.map { l -> l.maxBy { it.createdAt } }
            val sorted = list.sortedBy { it.createdAt }
            val trend = if (sorted.size >= 4) {
                val h = sorted.size / 2
                sorted.takeLast(h).map { it.pct }.average() - sorted.take(h).map { it.pct }.average()
            } else null
            TopicScore(key.first, key.second, latestPerItem.map { it.pct }.average(), latestPerItem.size, list.size, trend)
        }.sortedBy { it.avg }
    }

    fun weakTopics(checks: List<Assessment>): List<TopicScore> = topicScores(checks).filter { it.avg < PASS }
}
