package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.TestData.tick
import com.lakshman.roadmaplab.domain.CATEGORY_ORDER
import com.lakshman.roadmaplab.domain.CatStatus
import com.lakshman.roadmaplab.domain.CategoryStats
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayMeta
import com.lakshman.roadmaplab.domain.DayPlan
import com.lakshman.roadmaplab.domain.ItemEvent
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.RoutineCheck
import com.lakshman.roadmaplab.domain.Streaks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every category keeps its own streak and percentages, and a forgotten tick can be credited to the day you did it. */
class StreaksTest {
    private fun stats(today: Long, events: List<ItemEvent> = emptyList(), ticks: List<RoutineCheck> = emptyList(),
                      meta: Map<Long, DayMeta> = emptyMap(), p: DayPlan = plan): Map<String, CategoryStats> =
        Streaks.all(p, Mastery.states(events), ticks, meta, emptySet(), today)

    private val tue = d("2026-11-17")

    @Test
    fun allTenCategoriesAreTracked() {
        val s = stats(tue)
        assertEquals(CATEGORY_ORDER, s.keys.toList())
        assertTrue(s.values.all { it.current == 0 && it.completed == 0 })
        assertEquals(43, s["accounting"]!!.total)
        assertTrue(s["accounting"]!!.plannedToday)
        assertFalse(s["sap"]!!.plannedToday) // SAP is Tuesday flashcards from week 2, and Fridays
        assertEquals(CatStatus.OPEN, s["accounting"]!!.on(tue)) // today is never a miss while it's open
    }

    @Test
    fun eachCategoryHasItsOwnStreak() {
        // Accounting Tue–Thu; the log line only on Tuesday.
        val ev = listOf(ev("T1001", tue), ev("T1006", tue + 1), ev("T1012", tue + 2), ev("T1003", tue))
        val s = stats(tue + 3, ev) // Friday: no accounting planned, so it can't break
        assertEquals(3, s["accounting"]!!.current)
        assertEquals(3, s["accounting"]!!.longest)
        assertEquals(CatStatus.OFF, s["accounting"]!!.on(tue + 3))
        val evidence = s["evidence"]!!
        assertEquals(CatStatus.GRACE, evidence.on(tue + 1)) // first miss of the week: forgiven
        assertEquals(CatStatus.MISSED, evidence.on(tue + 2)) // second miss: the streak restarts
        assertEquals(0, evidence.current)
        assertEquals(1, evidence.longest) // never erased
    }

    @Test
    fun percentagesShowDoneOfAllAndDoneOfDue() {
        val ev = listOf(ev("T1001", tue), ev("T1006", tue + 1), ev("T1012", tue + 2))
        val acc = stats(tue + 3, ev)["accounting"]!!
        assertEquals(3, acc.completed)
        assertEquals(100.0 * 3 / 43, acc.pct, 0.01)
        assertEquals(3, acc.due)
        assertEquals(100.0, acc.onTrackPct!!, 0.01)
        assertEquals(100.0, acc.daysPct!!, 0.01)
        assertEquals(100.0, acc.rolling7!!, 0.01)
        assertNull(stats(tue)["accounting"]!!.rolling7) // nothing planned before today yet: no percentage, not 0 %
    }

    @Test
    fun iDidItYesterdayCountsForYesterday() {
        val wed = tue + 1
        // Wednesday morning: Tuesday's tasks were never ticked.
        val open = Streaks.untickedRecent(plan, Mastery.states(emptyList()), wed)
        assertEquals(listOf("T1001", "T1002", "T1003", "T1004", "T1005").sorted(), open.map { it.id }.sorted())
        // You did the log line and the practice yesterday, and tick them now for Tuesday.
        val ev = listOf(ev("T1003", tue), ev("T1005", tue))
        val s = stats(wed, ev)
        assertEquals(CatStatus.HIT, s["evidence"]!!.on(tue))
        assertEquals(1, s["evidence"]!!.current)
        assertEquals(1, s["practice"]!!.current)
        assertFalse(Streaks.untickedRecent(plan, Mastery.states(ev), wed).any { it.id == "T1003" })
    }

    @Test
    fun yesterdaysListOnlyHasTheNextStepOfNumberedWork() {
        // Thursday, nothing done since Tuesday: accounting's next step is still T1001, so Wednesday's T1006 isn't due yet.
        val open = Streaks.untickedRecent(plan, Mastery.states(emptyList()), tue + 2).map { it.id }
        assertFalse("T1006" in open)
        assertTrue("T1007" in open) // French week 1's first step was planned for Wednesday
        assertTrue("T1009" in open) // Wednesday's log line
        assertFalse("T1003" in open) // only yesterday by default
    }

    @Test
    fun weeklyCategoriesHaveNoGrace() {
        // SAP Friday (20 Nov) missed; Tuesday flashcards (24 Nov) done.
        val s = stats(d("2026-11-25"), listOf(ev("T1041", d("2026-11-24"))))["sap"]!!
        assertEquals(CatStatus.MISSED, s.on(d("2026-11-20")))
        assertEquals(1, s.current)
        assertFalse(s.graceRule)
        assertTrue(stats(tue)["practice"]!!.graceRule)
    }

    @Test
    fun restDaysAndReducedDaysWithoutAnActionAreNeutral() {
        val xmas = d("2026-12-25")
        fun lifeOn(day: Long) = plan.tasksOn(day).first { it.area == "life" }.id
        val life = stats(xmas + 1, listOf(ev(lifeOn(xmas - 1), xmas - 1), ev(lifeOn(xmas + 1), xmas + 1)))["life"]!!
        assertEquals(CatStatus.OFF, life.on(xmas))
        assertEquals(2, life.current)
        // Reduced week of 7 Dec: SAP's only action is Tuesday's flashcards, so Friday 11 Dec is neutral for SAP.
        val wed = d("2026-12-09")
        val meta = (0L..6L).associate { (d("2026-12-07") + it) to DayMeta(d("2026-12-07") + it, DayKind.REDUCED, null, 0, 0) }
        val sap = stats(d("2026-12-12"), meta = meta)["sap"]!!
        assertEquals(CatStatus.OFF, sap.on(d("2026-12-11")))
        // A ticked reduced-routine action counts for its category.
        val acc = stats(wed, ticks = listOf(tick("red-accounting", wed, 15)), meta = meta)["accounting"]!!
        assertEquals(CatStatus.HIT, acc.on(wed))
    }

    @Test
    fun daysBeforeAFreshStartAreNeutral() {
        val fresh = plan.withFreshStart(d("2026-11-24"))
        val s = stats(d("2026-11-25"), listOf(ev("T1040", d("2026-11-24"))), p = fresh)["accounting"]!!
        assertTrue(s.days.none { it.day < d("2026-11-24") })
        assertEquals(1, s.current)
        assertEquals(0, s.days.count { it.status == CatStatus.MISSED })
    }
}
