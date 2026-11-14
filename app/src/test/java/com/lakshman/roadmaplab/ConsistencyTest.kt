package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.TestData.tick
import com.lakshman.roadmaplab.domain.Consistency
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayMeta
import com.lakshman.roadmaplab.domain.DayStatus
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.Targets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant

class ConsistencyTest {
    private val mon = d("2026-11-16")

    private fun minutes(vararg days: Pair<Long, Int>): Map<Long, Int> = days.toMap()

    @After
    fun resetClock() { Days.clock = Clock.system(Days.ZONE) }

    @Test
    fun onlyRealStudyCountsNotOpeningTheAppOrHealthTicks() {
        val states = Mastery.states(listOf(ev("T008", mon), ev("T007", mon))) // exercise (health) + log line 5 min
        val m = Consistency.minutesByDay(plan, states.values, listOf(tick(Consistency.SLEEP, mon, 0)))
        assertEquals(5, m[mon])
        val info = Consistency.evaluate(plan, emptyMap(), m, Targets(), mon + 1)
        assertEquals(DayStatus.GRACE, info.days.single { it.day == mon }.status) // 5 < 20 minutes: missed, forgiven (first in the week)
        assertEquals(0, info.current)
    }

    @Test
    fun streakCountsQualifiedDaysAndForgivesOneMissAWeek() {
        val m = minutes(mon to 45, mon + 1 to 45, mon + 3 to 45, mon + 4 to 30) // Wed missed
        val info = Consistency.evaluate(plan, emptyMap(), m, Targets(), mon + 4)
        assertEquals(DayStatus.GRACE, info.days.single { it.day == mon + 2 }.status)
        assertEquals(4, info.current)
        assertEquals(4, info.longest)
        assertEquals(4, info.activeThisWeek)
    }

    @Test
    fun aSecondMissInTheWeekBreaksTheStreakButNotTheHistory() {
        val m = minutes(mon to 45, mon + 1 to 45, mon + 4 to 30) // Wed and Thu missed
        val info = Consistency.evaluate(plan, emptyMap(), m, Targets(), mon + 4)
        assertEquals(DayStatus.MISSED, info.days.single { it.day == mon + 3 }.status)
        assertEquals(1, info.current)
        assertEquals(2, info.longest) // never erased
    }

    @Test
    fun restDaysAndReducedDaysNeverBreakIt() {
        val xmas = d("2026-12-25")
        val m = minutes(xmas - 1 to 45, xmas + 1 to 125, xmas + 2 to 20) // Thu, (Fri rest), Sat, Sun
        val info = Consistency.evaluate(plan, emptyMap(), m, Targets(), xmas + 2)
        assertEquals(DayStatus.REST, info.days.single { it.day == xmas }.status)
        assertEquals(3, info.current)
        // A reduced day needs only 15 minutes (evidence line 5 + flashcards 15 = 20).
        val wed = d("2026-12-09")
        val meta = mapOf(wed to DayMeta(wed, DayKind.REDUCED, null, 0, 0))
        val r = Consistency.evaluate(plan, meta, minutes(wed to 20), Targets(), wed)
        assertEquals(DayStatus.QUALIFIED, r.days.single { it.day == wed }.status)
    }

    @Test
    fun todayIsNeverAMissWhileOpen() {
        val info = Consistency.evaluate(plan, emptyMap(), minutes(mon to 45), Targets(), mon + 1)
        assertEquals(DayStatus.TODAY, info.days.single { it.day == mon + 1 }.status)
        assertEquals(1, info.current)
    }

    @Test
    fun rollingConsistencyUsesScheduledDaysOnly() {
        val m = (0L..6L).filter { it != 2L }.associate { (mon + it) to 60 }
        val info = Consistency.evaluate(plan, emptyMap(), m, Targets(), mon + 7)
        assertEquals(100.0 * 6 / 7, info.rolling7!!, 0.01)
        val before = Consistency.evaluate(plan, emptyMap(), emptyMap(), Targets(), d("2026-11-14"))
        assertNull(before.rolling7) // nothing scheduled yet: no percentage, not 0 %
    }

    @Test
    fun theDayFollowsKinshasaNotThePhone() {
        // 23:30 UTC on 15 Nov is already 00:30 on 16 Nov in Kinshasa (UTC+1).
        Days.clock = Clock.fixed(Instant.parse("2026-11-15T23:30:00Z"), java.time.ZoneId.of("Asia/Kolkata"))
        assertEquals(mon, Days.today())
        assertEquals(mon, Days.dayOf(Instant.parse("2026-11-15T23:30:00Z").toEpochMilli()))
        assertTrue(Days.dow(mon) == 1)
    }
}
