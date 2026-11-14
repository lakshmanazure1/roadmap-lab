package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.data.LabState
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Mode
import com.lakshman.roadmaplab.domain.Planner
import com.lakshman.roadmaplab.domain.TaskLog
import com.lakshman.roadmaplab.domain.TaskStatus
import com.lakshman.roadmaplab.ui.PathItem
import com.lakshman.roadmaplab.ui.buildPath
import com.lakshman.roadmaplab.ui.derive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The home path must never contain two items with the same key (that crashes the list). */
class PathTest {
    @Test
    fun pathKeysAreUniqueOverManyDays() {
        val r = TestData.roadmap
        val logs = ArrayList<TaskLog>()
        var id = 1L
        for (t in r.tasks.filter { it.assumedDone }) logs.add(TaskLog(id++, t.id, t.plannedDay, TaskStatus.DONE, 0, "", id, false))
        val plans = HashMap<Long, List<String>>()
        val start = Days.of("2026-11-14")
        for (day in start until start + 60) {
            val latest = Planner.latestByTask(logs)
            plans[day] = Planner.planDay(r.tasks, latest, day, Mode.DRC, false, false)
            val cards = TestData.cards.associateBy { it.id }
            val states = Leitner.unlockOnDone(r.tasks.filter { latest[it.id]?.status == TaskStatus.DONE }, cards.values, emptyList(), day)
            val s = LabState(AppSettings(onboarded = true, firstUseDay = start), r, "v", cards, states, logs.toList(),
                emptyMap(), emptyList(), plans.toMap())
            val path = buildPath(derive(s, day))
            val keys = path.map { it.key }
            assertEquals("duplicate keys on day $day: $keys", keys.size, keys.toSet().size)
            assertTrue("today's tasks missing on day $day", path.filterIsInstance<PathItem.Node>().size >= plans[day]!!.size)
            plans[day]!!.forEachIndexed { i, tid -> if (i < 2) logs.add(TaskLog(id++, tid, day, TaskStatus.DONE, 30, "", id, false)) }
        }
    }
}
