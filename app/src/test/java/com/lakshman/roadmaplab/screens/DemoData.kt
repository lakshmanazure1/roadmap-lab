package com.lakshman.roadmaplab.screens

import com.lakshman.roadmaplab.TestData
import com.lakshman.roadmaplab.data.LabState
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.CheckSpec
import com.lakshman.roadmaplab.domain.Consistency
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayMeta
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Evidence
import com.lakshman.roadmaplab.domain.ItemEvent
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.RoutineCheck
import com.lakshman.roadmaplab.domain.Topics
import com.lakshman.roadmaplab.domain.WeeklyReview
import kotlin.random.Random

/**
 * DEMO DATA FOR SCREEN PICTURES ONLY. It lives in the test folder and is never part of the app:
 * the real dashboard only ever shows your own records.
 */
object DemoData {
    private val plan get() = TestData.plan

    fun empty(theme: String = "light") = LabState(
        settings = AppSettings(onboarded = true, v2Intro = true, theme = theme, reminderOn = true, reminderHour = 20, reminderMinute = 30,
            freshStartAsked = true, freshStartPlan = TestData.plan.meta.version),
        plan = plan, planError = null, cards = TestData.cards.associateBy { it.id }, cardStates = emptyList(), reviewLogs = emptyList(),
        dayLogs = emptyMap(), events = emptyList(), assessments = emptyList(), ticks = emptyList(), evidence = emptyList(),
        dayMeta = emptyMap(), weeklyReviews = emptyMap(), legacyTaskLogs = 0,
    )

    /** About three and a half weeks of realistic, imperfect use, up to (not including) [until]. */
    fun midPlan(until: Long, theme: String = "light", reducedToday: Boolean = false): LabState {
        val rnd = Random(7)
        val events = ArrayList<ItemEvent>()
        val checks = ArrayList<Assessment>()
        val ticks = ArrayList<RoutineCheck>()
        val evidence = ArrayList<Evidence>()
        var id = 1L
        var d = plan.start
        val skipDays = setOf(plan.start + 9, plan.start + 17) // two missed weekdays
        while (d < until) {
            if (d !in skipDays) {
                for (t in plan.tasksOn(d)) {
                    if (rnd.nextDouble() > 0.88) continue
                    events += ItemEvent(id, t.id, d, EventType.DONE, t.minutes, rnd.nextDouble() > 0.6, "", d * 100_000 + id); id++
                    val score = t.checks.filterIsInstance<CheckSpec.Score>().firstOrNull()
                    if (score != null && score.max > 0) {
                        val got = (score.target + rnd.nextInt(-2, 3)).coerceIn(0, score.max)
                        checks += Assessment(id++, t.id, t.area, Topics.of(plan, t), "score", got.toDouble(), score.max.toDouble(),
                            got >= score.target, false, "{}", d, d * 100_000 + id, 0)
                    }
                    if (t.checks.any { it is CheckSpec.Rubric }) {
                        val s = 5 + rnd.nextInt(0, 4)
                        checks += Assessment(id++, t.id, t.area, Topics.of(plan, t), "rubric", s.toDouble(), 10.0, s >= 6, true, "{}", d, d * 100_000 + id, 0)
                    }
                }
                ticks += RoutineCheck(d, Consistency.SLEEP, rnd.nextDouble() > 0.3, 0, d * 100_000)
            }
            if (com.lakshman.roadmaplab.domain.Days.dow(d) <= 5 && d !in skipDays) {
                evidence += Evidence(id++, d, listOf("evidence", "accounting", "python").random(rnd), null, "line",
                    listOf("Vendor upload", "Customer master", "Opening balances", "Item master").random(rnd),
                    listOf("About 40 rows rejected", "Blank tax IDs in 12 rows", "Duplicate vendors suspected").random(rnd),
                    "Sorted by blanks; sent the list to the client team", listOf("2nd upload: 0 errors", "12 rows fixed in 1 day", "3 duplicates merged").random(rnd),
                    "Check mandatory fields before every upload", "Data checking", "Upload report", 3 + rnd.nextInt(0, 3),
                    false, "", "", "", "", d * 86_400_000, d * 86_400_000)
            }
            d++
        }
        // One ★ per finished week, a STAR story, two completed weekly reviews, and one weak quiz.
        val starred = evidence.groupBy { com.lakshman.roadmaplab.domain.Days.weekStart(it.day) }.values.map { it.first().id }.toSet()
        val ev = evidence.map { if (it.id in starred) it.copy(starred = true) else it } + Evidence(id++, plan.start + 14, "evidence", "T1077", "star",
            "", "", "", "", "", "", "", null, false, "Go-live vendor upload was failing for the client.", "Get the vendor master loaded before cutover.",
            "Sorted rejected rows by blank columns. Found missing tax IDs. Sent a clear fix list to the client team.", "Second upload had 0 errors; 40 rows saved.",
            0, 0)
        checks += Assessment(id++, "T1154", "accounting", "Journal entries", "quiz", 5.0, 10.0, false, true, "{}", until - 3, (until - 3) * 100_000, 400)
        val reviews = (1..2).associate { w ->
            val ws = plan.weekStartOf(w)
            ws to WeeklyReview(ws, mapOf("python" to "All 10 errors found"), emptyMap(), emptyMap(), "Go-live ran late twice", listOf("Go-live work ran late"),
                "Error report works", "French", "1. Retrieval 1 2. Cold quiz 3. Sleep", "normal", "\$120", ws * 86_400_000 + 7 * 86_400_000, 0)
        }
        val cardStates = TestData.cards.filter { plan.meta.cardUnlocks[it.id]?.let { t -> plan.task(t)!!.day < until } == true }
            .map { Leitner.newState(it.id, until - 1) }
        // A mood most days (1–5).
        val moods = (plan.start until until).filter { rnd.nextDouble() > 0.2 }
            .associateWith { com.lakshman.roadmaplab.domain.DayLog(it, mood = 2 + rnd.nextInt(0, 4)) }
        val meta = if (reducedToday) (0L..4L).associate { k -> (until + k) to DayMeta(until + k, DayKind.REDUCED, null, 0, 0) } else emptyMap()
        return empty(theme).copy(events = events, assessments = checks, ticks = ticks, evidence = ev, weeklyReviews = reviews,
            cardStates = cardStates, dayMeta = meta, dayLogs = moods)
    }
}

/** Screens call these; in pictures they do nothing. */
object NoActions : com.lakshman.roadmaplab.ui.Actions {
    override fun setTask(id: String, type: EventType, minutes: Int, independent: Boolean, note: String, day: Long?) {}
    override fun addAssessment(a: Assessment) {}
    override fun tick(day: Long, id: String, done: Boolean, minutes: Int) {}
    override fun saveEvidence(e: Evidence, onSaved: (Long) -> Unit) {}
    override fun deleteEvidence(id: Long) {}
    override fun setDayKind(day: Long, kind: DayKind?) {}
    override fun setReducedWeek(from: Long, on: Boolean) {}
    override fun setFocus(day: Long, id: String?) {}
    override fun saveDayNote(day: Long, note: String) {}
    override fun saveWeeklyReview(r: WeeklyReview, complete: Boolean) {}
    override fun review(cardId: String, rating: com.lakshman.roadmaplab.domain.Rating, asMcq: Boolean) {}
    override fun addCard(topic: String, prompt: String, answer: String) {}
    override fun updateSettings(change: (AppSettings) -> AppSettings) {}
    override fun note(n: com.lakshman.roadmaplab.ui.Note) {}
    override fun startFresh() {}
    override fun setMood(day: Long, mood: Int?) {}
}
