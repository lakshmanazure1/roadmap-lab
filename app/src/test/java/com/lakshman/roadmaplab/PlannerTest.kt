package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.log
import com.lakshman.roadmaplab.TestData.task
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Mode
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.TaskLog
import com.lakshman.roadmaplab.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {
    @Test
    fun neverMoreThanThreeTasks() {
        val tasks = (1..10).map { task("T$it", 100, 5, order = it) }
        val plan = Planner.planDay(tasks, emptyMap(), 100, Mode.INDIA, reducedPlan = false, restart = false)
        assertEquals(3, plan.size)
    }

    @Test
    fun capacityRespectedButFirstTaskAlwaysFits() {
        val tasks = listOf(task("Big", 100, 90, order = 1), task("Small", 100, 10, order = 2))
        val plan = Planner.planDay(tasks, emptyMap(), 100, Mode.DRC, false, false)
        assertEquals(listOf("Big"), plan) // 90 > 60 but it's first; 90 + 10 > 60 so Small waits
    }

    @Test
    fun unfinishedTasksRollToTheNextDayOldestFirst() {
        val tasks = listOf(task("Mon", 100, 30, order = 1), task("Tue", 101, 30, order = 2))
        val latest = Planner.latestByTask(listOf(log("Mon", 100, TaskStatus.SKIPPED)))
        val plan = Planner.planDay(tasks, latest, 101, Mode.DRC, false, false)
        assertEquals(listOf("Mon", "Tue"), plan)
    }

    @Test
    fun doneTasksDontComeBack() {
        val tasks = listOf(task("A", 100, 30, order = 1), task("B", 101, 30, order = 2))
        val latest = Planner.latestByTask(listOf(log("A", 100, TaskStatus.DONE)))
        assertEquals(listOf("B"), Planner.planDay(tasks, latest, 101, Mode.DRC, false, false))
    }

    @Test
    fun latestLogWins() {
        val logs = listOf(log("A", 100, TaskStatus.PARTLY, at = 1), log("A", 101, TaskStatus.DONE, at = 2))
        assertEquals(TaskStatus.DONE, Planner.latestByTask(logs)["A"]?.status)
    }

    @Test
    fun skippingFourDaysOpensRestartWithCoreFirst() {
        val active = setOf(100L) // last action on day 100
        assertTrue(Planner.needsRestart(105, active, firstUseDay = 90)) // days 101–104 missed
        assertFalse(Planner.needsRestart(103, active, firstUseDay = 90)) // only 2 missed
        val tasks = listOf(task("Opt", 101, 10, core = false, order = 1), task("Core", 102, 10, order = 2))
        val plan = Planner.planDay(tasks, emptyMap(), 105, Mode.DRC, false, restart = true)
        assertEquals("Core", plan.first())
    }

    @Test
    fun reducedPlanHidesOptionalTasks() {
        val tasks = listOf(task("Opt", 100, 10, core = false, order = 1), task("Core", 100, 10, order = 2))
        assertEquals(listOf("Core"), Planner.planDay(tasks, emptyMap(), 100, Mode.DRC, reducedPlan = true, restart = false))
    }

    /** Simulates 120 days on the real roadmap with misses and partial days: no day ever gets more than 3 tasks. */
    @Test
    fun realRoadmapSimulationNeverExceedsThreeADay() {
        val tasks = TestData.roadmap.tasks
        val logs = ArrayList<TaskLog>()
        for (t in tasks.filter { it.assumedDone }) logs.add(log(t.id, t.plannedDay, TaskStatus.DONE))
        val start = Days.of("2026-11-14")
        var at = 1000L
        for (day in start until start + 120) {
            val latest = Planner.latestByTask(logs)
            val plan = Planner.planDay(tasks, latest, day, Mode.DRC, false, false)
            assertTrue("day $day has ${plan.size} tasks", plan.size <= Planner.MAX_TASKS_PER_DAY)
            if (day % 7 == 3L || (day in start + 30 until start + 35)) continue // missed days, incl. a 5-day gap
            plan.forEachIndexed { i, id ->
                val status = if (i == 2 && day % 2 == 0L) TaskStatus.SKIPPED else TaskStatus.DONE
                logs.add(TaskLog(at, id, day, status, 30, "", at, false)); at++
            }
        }
    }

    @Test
    fun pullAheadOnlyWhenTodayHasRoom() {
        val tasks = (1..5).map { task("T$it", 100L + it, 10, order = it) }
        assertEquals("T1", Planner.nextToPull(tasks, emptyMap(), emptyList(), false)?.id)
        assertEquals(null, Planner.nextToPull(tasks, emptyMap(), listOf("a", "b", "c"), false))
    }
}
