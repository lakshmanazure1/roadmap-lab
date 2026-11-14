package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.log
import com.lakshman.roadmaplab.TestData.task
import com.lakshman.roadmaplab.domain.Milestone
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hand-calculated test set (acceptance check: "you are here" and % match a hand calculation).
 *   Day 1: A 60 min · Day 2: B 30 min · Day 3: C 30 min · Day 5: D 60 min   → total 180 min
 */
class ProgressTest {
    private val tasks = listOf(
        task("A", 1, 60, order = 1, track = "AI"),
        task("B", 2, 30, order = 2, track = "Acc"),
        task("C", 3, 30, order = 3, track = "Acc"),
        task("D", 5, 60, order = 4, track = "AI"),
    )
    private val milestones = listOf(Milestone("M", "Demo", 6, "AI"))

    @Test
    fun aDoneBPartly_onDay3_isOneDayBehind() {
        // done = 60 + 30×0.5 = 75 → 75/180 = 41.67 %. Day 1 covered (60 ≤ 75), day 2 not (90 > 75) → You = day 1.
        // Today = day 3, so plan expects yesterday (day 2) covered → 1 day behind.
        val latest = Planner.latestByTask(listOf(log("A", 1, TaskStatus.DONE), log("B", 2, TaskStatus.PARTLY)))
        val p = Progress.compute(tasks, latest, milestones, today = 3, reducedPlan = false)
        assertEquals(41.67, p.percent, 0.01)
        assertEquals(1L, p.youDay)
        assertEquals(-1, p.gapDays)
        assertEquals("1 day behind", p.label)
        assertEquals(50.0, p.trackPercent["AI"]!!, 0.01)   // 60 of 120
        assertEquals(25.0, p.trackPercent["Acc"]!!, 0.01)  // 15 of 60
        assertEquals(3, p.daysToMilestone)
    }

    @Test
    fun onPlanWhileTodaysWorkIsOpen() {
        // A and B done = 90 → covers days 1–2. Today = 3 → on plan.
        val latest = Planner.latestByTask(listOf(log("A", 1, TaskStatus.DONE), log("B", 2, TaskStatus.DONE)))
        val p = Progress.compute(tasks, latest, milestones, today = 3, reducedPlan = false)
        assertEquals(2L, p.youDay)
        assertEquals(0, p.gapDays)
        assertEquals("On plan", p.label)
    }

    @Test
    fun aheadWhenFutureWorkIsDone() {
        // A, B, C done = 120 → covers days 1–3, and day 4 has nothing planned, so You = day 4. Today = 2 → 2 days ahead.
        val latest = Planner.latestByTask(listOf(log("A", 1, TaskStatus.DONE), log("B", 2, TaskStatus.DONE), log("C", 2, TaskStatus.DONE)))
        val p = Progress.compute(tasks, latest, milestones, today = 2, reducedPlan = false)
        assertEquals(66.67, p.percent, 0.01)
        assertEquals(4L, p.youDay)
        assertEquals(2, p.gapDays)
        assertEquals("2 days ahead", p.label)
    }

    @Test
    fun nothingDoneMeansYouIsBeforeTheStart() {
        val p = Progress.compute(tasks, emptyMap(), milestones, today = 1, reducedPlan = false)
        assertEquals(0L, p.youDay)
        assertEquals(0, p.gapDays) // day 1 is still open
    }

    @Test
    fun reducedPlanOfferOnlyBeyond14DaysBehind() {
        assertFalse(Progress.compute(tasks, emptyMap(), milestones, today = 15, reducedPlan = false).offerReducedPlan)
        assertTrue(Progress.compute(tasks, emptyMap(), milestones, today = 16, reducedPlan = false).offerReducedPlan)
    }

    @Test
    fun realRoadmapStartsWithPreAppWorkCounted() {
        val r = TestData.roadmap
        val latest = Planner.latestByTask(r.tasks.filter { it.assumedDone }.map { log(it.id, it.plannedDay, TaskStatus.DONE) })
        val p = Progress.compute(r.tasks, latest, r.milestones, today = com.lakshman.roadmaplab.domain.Days.of("2026-11-14"), reducedPlan = false)
        assertTrue("percent ${p.percent}", p.percent > 3 && p.percent < 10)
        assertEquals("On plan", p.label)
    }
}
