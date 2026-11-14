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
        val mon = d("2026-11-16")
        val ev = (0L..4L).flatMap { k ->
            plan.tasksOn(mon + k).filter { it.area in setOf("accounting", "french", "evidence") }.map { ev(it.id, mon + k) }
        }
        val ui = derive(state(ev), mon + 5)!! // Saturday
        assertEquals(15, ui.totalDone)
        assertEquals(5, ui.consistency.current) // 45 min each weekday ≥ 20
        assertEquals(5, ui.consistency.activeThisWeek)
        val acc = ui.areas.single { it.area.id == "accounting" }
        assertEquals(5, acc.completed); assertEquals(0, acc.behind)
        assertTrue(ui.todayPlan.main.any { it.key == "T029" })
        assertEquals(NextKind.TASK, ui.next.kind)
    }

    @Test
    fun undoingATaskUpdatesEveryMetric() {
        val mon = d("2026-11-16")
        val ui = derive(state(listOf(ev("T004", mon), ev("T004", mon, EventType.RESET))), mon)!!
        assertEquals(0, ui.totalDone)
        assertEquals(0, ui.minutes[mon] ?: 0)
        assertEquals(Level.NOT_STARTED, ui.level("T004"))
    }

    @Test
    fun aFailedCheckShowsUpAsRevisionAndNextAction() {
        val wed = d("2026-11-18")
        val ui = derive(state(
            (0L..2L).flatMap { k -> plan.tasksOn(d("2026-11-16") + k).filter { it.area != "life" }.map { ev(it.id, d("2026-11-16") + k) } },
            listOf(check("T014", wed, 50.0, type = "score"), check("T015", wed, 80.0, area = "french")),
        ), wed)!!
        assertEquals(Level.NEEDS_REVISION, ui.level("T014"))
        assertTrue(ui.needsRevision.any { it.id == "T014" })
        assertEquals(NextKind.REVISE, ui.next.kind)
    }

    @Test
    fun beforeThePlanStarts() {
        val ui = derive(state(), d("2026-11-14"))!!
        assertEquals(NextKind.BEFORE_START, ui.next.kind)
        assertEquals(0, ui.dueByToday)
        assertEquals(0, ui.consistency.current)
    }
}
