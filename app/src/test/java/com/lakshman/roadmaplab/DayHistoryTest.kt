package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.check
import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.TestData.tick
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.CatStatus
import com.lakshman.roadmaplab.domain.CategoryDay
import com.lakshman.roadmaplab.domain.DayHistory
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.DayMeta
import com.lakshman.roadmaplab.domain.DayRecord.Kind
import com.lakshman.roadmaplab.domain.Evidence
import com.lakshman.roadmaplab.domain.ItemEvent
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.RoutineCheck
import com.lakshman.roadmaplab.domain.Streaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Opening a category lists its dates, each with its plan, status and the notes that belong to that day. */
class DayHistoryTest {
    private val tue = d("2026-11-17")
    private val sat = d("2026-11-21")
    private val sun = d("2026-11-22")

    private fun line(day: Long, area: String, result: String, kind: String = "line") =
        Evidence(day * 10, day, area, null, kind, "", "", "", result, "", "", "", null, false, "", "", "", "", day * 100_000, day * 100_000)

    private fun days(
        area: String, today: Long, events: List<ItemEvent> = emptyList(), evidence: List<Evidence> = emptyList(),
        checks: List<Assessment> = emptyList(), ticks: List<RoutineCheck> = emptyList(), logs: Map<Long, DayLog> = emptyMap(),
        meta: Map<Long, DayMeta> = emptyMap(),
    ): Map<Long, CategoryDay> {
        val states = Mastery.states(events)
        val stats = Streaks.all(plan, states, ticks, meta, emptySet(), today)[area]
        return DayHistory.build(plan, area, states, evidence, checks, ticks, logs, meta, emptySet(), stats, today).associateBy { it.day }
    }

    @Test
    fun everyPlannedDateIsListedWithItsTasks() {
        for (area in listOf("python", "accounting", "sap", "career", "french", "comms", "evidence", "life", "practice", "review")) {
            val list = days(area, tue)
            val planned = plan.tasks.filter { it.area == area }
            assertEquals(area, planned.map { it.day }.toSortedSet(), list.keys.toSortedSet())
            assertEquals(area, planned.size, list.values.sumOf { it.planned.size })
            assertEquals(area, list.keys.sorted(), list.keys.toList()) // oldest date first
        }
        val py = days("python", tue)
        assertTrue(py[sat]!!.planned.any { it.id == "T1023" })
        assertTrue(py.keys.none { it < sat }) // Python starts on the first Saturday
    }

    @Test
    fun aTaskDoneLateShowsOnBothDates() {
        val py = days("python", sun + 1, events = listOf(ev("T1023", sun)))
        assertTrue(py[sat]!!.planned.any { it.id == "T1023" })
        assertTrue(py[sat]!!.alsoDone.isEmpty())
        assertEquals(listOf("T1023"), py[sun]!!.alsoDone.map { it.id })
        assertEquals(CatStatus.HIT, py[sun]!!.status)
    }

    @Test
    fun notesBelongToTheirDay() {
        val wed = tue + 1
        val py = days(
            "python", sun,
            evidence = listOf(line(wed, "python", "is_missing() passes 6 of 6 cases"), line(sat, "accounting", "Trial balance agrees")),
            checks = listOf(check("T1023", sat, 80.0, area = "python", topic = "Recap")),
            ticks = listOf(tick("red-python", sat, minutes = 20)),
            logs = mapOf(sat to DayLog(sat, note = "Tired but finished"), tue + 2 to DayLog(tue + 2, note = "Only French today")),
        )
        // Evidence for Python on a day with no Python planned still adds that date.
        assertEquals(listOf(Kind.EVIDENCE), py[wed]!!.records.map { it.kind })
        assertTrue(py[wed]!!.planned.isEmpty())
        // The day's own note shows under a listed date, but never adds a date by itself.
        assertFalse(tue + 2 in py)
        val satRecords = py[sat]!!.records
        assertEquals(listOf(Kind.CHECK, Kind.ROUTINE, Kind.DAY_NOTE), satRecords.map { it.kind })
        assertTrue(satRecords[0].text.endsWith("80/100 · passed"))
        assertTrue(satRecords[1].text.endsWith("· 20 min"))
        assertEquals("Tired but finished", satRecords[2].text)
        // Another category's evidence is not shown here...
        assertTrue(satRecords.none { "Trial balance" in it.text })
        // ...but the Evidence log category lists every line.
        val log = days("evidence", sun, evidence = listOf(line(sat, "accounting", "Trial balance agrees"), line(sun, "python", "Story", kind = "star")))
        assertEquals(Kind.EVIDENCE, log[sat]!!.records.first().kind)
        assertEquals(Kind.STORY, log[sun]!!.records.first().kind)
    }

    @Test
    fun dayKindAndOnlyYourOwnTaskNotes() {
        val py = days("python", sun, meta = mapOf(sat to DayMeta(sat, DayKind.REST, null, sat, sat)))
        assertEquals(DayKind.REST, py[sat]!!.dayKind)
        assertEquals(CatStatus.OFF, py[sat]!!.status) // a rest day never counts as a miss
        val mine = ev("T1023", sat).copy(note = "Used a dict for the checks")
        val auto = ev("T1024", sat).copy(note = "Ticked when you saved the log line")
        val notes = DayHistory.taskNotes(listOf(mine, auto, ev("T1027", sun)))
        assertEquals(listOf("T1023"), notes.keys.toList())
        assertEquals("Used a dict for the checks", notes["T1023"]!!.single().note)
    }
}
