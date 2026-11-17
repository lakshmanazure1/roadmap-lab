package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.check
import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.data.LabState
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Level
import com.lakshman.roadmaplab.domain.NextKind
import com.lakshman.roadmaplab.ui.derive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Realistic scenarios: what the dashboard shows after real use (no fake data in the app itself). */
class UiStateTest {
    private fun state(events: List<com.lakshman.roadmaplab.domain.ItemEvent> = emptyList(),
                      checks: List<com.lakshman.roadmaplab.domain.Assessment> = emptyList()) = LabState(
        settings = AppSettings(onboarded = true, v2Intro = true), plan = plan, planError = null,
        cards = TestData.cards.associateBy { it.id }, cardStates = emptyList(), reviewLogs = emptyList(), dayLogs = emptyMap(),
        events = events, assessments = checks, ticks = emptyList(), evidence = emptyList(), dayMeta = emptyMap(),
        weeklyReviews = emptyMap(), legacyTaskLogs = 0,
    )

    @Test
    fun aNormalFirstWeek() {
        val tue = d("2026-11-17")
        val ev = (0L..3L).flatMap { k ->
            plan.tasksOn(tue + k).filter { it.area in setOf("accounting", "french", "evidence", "sap") }.map { ev(it.id, tue + k) }
        }
        val ui = derive(state(ev), tue + 4)!! // Saturday
        assertEquals(10, ui.totalDone) // Tue 2 + Wed 3 + Thu 3 + Fri 2
        assertEquals(4, ui.consistency.current) // 35–45 min each weekday ≥ 20
        val acc = ui.areas.single { it.area.id == "accounting" }
        assertEquals(3, acc.completed); assertEquals(0, acc.behind)
        assertTrue(ui.todayPlan.main.any { it.key == "T1023" })
        assertEquals(NextKind.TASK, ui.next.kind)
        // Each category keeps its own streak.
        assertEquals(3, ui.category("accounting")!!.current)
        assertEquals(1, ui.category("sap")!!.current)
        assertEquals(0, ui.category("practice")!!.current)
    }

    @Test
    fun undoingATaskUpdatesEveryMetric() {
        val tue = d("2026-11-17")
        val ui = derive(state(listOf(ev("T1001", tue), ev("T1001", tue, EventType.RESET))), tue)!!
        assertEquals(0, ui.totalDone)
        assertEquals(0, ui.minutes[tue] ?: 0)
        assertEquals(Level.NOT_STARTED, ui.level("T1001"))
        assertEquals(0, ui.category("accounting")!!.current)
    }

    @Test
    fun aFailedCheckShowsUpAsRevisionAndNextAction() {
        val thu = d("2026-11-19")
        val ui = derive(state(
            (0L..2L).flatMap { k -> plan.tasksOn(d("2026-11-17") + k).filter { it.study }.map { ev(it.id, d("2026-11-17") + k) } },
            listOf(check("T1012", thu, 50.0, type = "score"), check("T1013", thu, 80.0, area = "french")),
        ), thu)!!
        assertEquals(Level.NEEDS_REVISION, ui.level("T1012"))
        assertTrue(ui.needsRevision.any { it.id == "T1012" })
        assertEquals(NextKind.REVISE, ui.next.kind)
    }

    @Test
    fun beforeThePlanStarts() {
        val ui = derive(state(), d("2026-11-16"))!!
        assertEquals(NextKind.BEFORE_START, ui.next.kind)
        assertEquals(0, ui.dueByToday)
        assertEquals(0, ui.consistency.current)
    }
}
