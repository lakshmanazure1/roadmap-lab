package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.domain.Consistency
import com.lakshman.roadmaplab.domain.DayStatus
import com.lakshman.roadmaplab.domain.DayType
import com.lakshman.roadmaplab.domain.ItemEvent
import com.lakshman.roadmaplab.domain.Leitner
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.Section
import com.lakshman.roadmaplab.domain.Targets
import com.lakshman.roadmaplab.domain.TodayEngine
import com.lakshman.roadmaplab.domain.TodayInput
import com.lakshman.roadmaplab.domain.TodayPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Your fresh start on Mon 16 Nov: Python is redone from the basics in a one-week recap (PR1–PR7)
 * alongside accounting, then the plan's own Python continues in order. Nothing in the CSV changes.
 */
class FreshStartTest {
    private fun today(day: Long, events: List<ItemEvent> = emptyList(), p: com.lakshman.roadmaplab.domain.DayPlan = plan): TodayPlan =
        TodayEngine.build(TodayInput(p, Mastery.states(events), emptyMap(), null, emptySet(), 0, 0, Targets()), day)

    private val mon = d("2026-11-16")
    private fun recapDone(upTo: Int, day: Long = mon) = (1..upTo).map { ev("PR$it", day) }

    @Test
    fun day1HasPythonRecapAndAccountingSideBySideWithinThe70MinuteCap() {
        val t = today(mon)
        assertEquals(1, t.planDay)
        val main = t.main.map { it.key }.toSet()
        assertEquals(setOf("T004", "T005", "T006", "T007", "PR1"), main)
        assertEquals(70, t.minutesPlanned) // accounting 30 + recap 25 + French 10 + log 5
        assertTrue(t.minutesPlanned <= 70) // the roadmap's weekday limit
    }

    @Test
    fun recapCanBeDoneFasterThanPlanned() {
        // You said you'd redo it in a day or two: after PR1, PR2 is offered straight away (optional, never required).
        val t = today(mon, recapDone(1))
        assertTrue(t.main.single { it.key == "PR1" }.done)
        assertEquals(Section.OPTIONAL, t.entries.single { it.key == "PR2" }.section)
        // All seven done on Tuesday: nothing left of the recap on Wednesday.
        val wed = today(mon + 2, recapDone(7, mon + 1))
        assertTrue(wed.entries.none { it.key.startsWith("PR") })
    }

    @Test
    fun thePlansPythonWaitsForTheRecapThenContinuesInOrder() {
        // Sat 21 Nov with only 3 recap steps done: the next recap step shows, not T029.
        val sat = today(d("2026-11-21"), recapDone(3))
        assertTrue("PR4" in sat.main.map { it.key })
        assertFalse(sat.entries.any { it.key == "T029" })
        // Recap finished on Sunday 22 Nov: T029 is next on the following Saturday (one slot later, nothing lost).
        val sat2 = today(d("2026-11-28"), recapDone(7, d("2026-11-22")))
        assertTrue("T029" in sat2.main.map { it.key })
        assertEquals(7, sat2.main.single { it.key == "T029" }.lateDays)
    }

    @Test
    fun theRecapUnlocksTheBasicsFlashcards() {
        val got = Leitner.unlockForItems(setOf("PR1"), TestData.cards, plan.meta.cardUnlocks, emptyList(), mon).map { it.cardId }.toSet()
        assertTrue("PY-001" in got && "PY-005" in got)
        assertFalse("PY-009" in got) // lists come with PR2
        val pan = Leitner.unlockForItems(setOf("PR5"), TestData.cards, plan.meta.cardUnlocks, emptyList(), mon).map { it.cardId }
        assertTrue("ER-02" in pan)
    }

    @Test
    fun daysBeforeAFreshStartAreNeverMissed() {
        // Started fresh on Tue 1 Dec: 16–30 Nov had no records, but they count as "nothing scheduled", not as missed days.
        val fresh = plan.withFreshStart(d("2026-12-01"))
        val info = Consistency.evaluate(fresh, emptyMap(), mapOf(d("2026-12-01") to 45), Targets(), d("2026-12-01"))
        assertTrue(info.days.filter { it.day < d("2026-12-01") }.all { it.status == DayStatus.NONE })
        assertEquals(DayType.NONE, Consistency.dayType(d("2026-11-20"), fresh, null))
        assertEquals(1, info.current)
        assertEquals(100.0, info.rolling7!!, 0.01)
        // Without the fresh start the same empty fortnight would be missed days.
        val plain = Consistency.evaluate(plan, emptyMap(), mapOf(d("2026-12-01") to 45), Targets(), d("2026-12-01"))
        assertTrue(plain.days.any { it.status == DayStatus.MISSED })
    }

    @Test
    fun theRecapAddsNothingInsideTheCsv() {
        assertEquals(381, com.lakshman.roadmaplab.domain.PlanCsv.parse(TestData.csv).size)
        assertTrue(TestData.csv.lines().none { it.startsWith("PR") })
        assertEquals(275, plan.tasks.filter { it.seq == "recap" }.sumOf { it.minutes }) // 5 × 25 + 90 + 60
    }
}
