package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayMeta
import com.lakshman.roadmaplab.domain.DayType
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.ItemEvent
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

class TodayEngineTest {
    private fun today(day: Long, events: List<ItemEvent> = emptyList(), kind: DayKind? = null, reducedWeeks: Set<Long> = emptySet()): TodayPlan =
        TodayEngine.build(
            TodayInput(plan, Mastery.states(events), emptyMap(), kind?.let { DayMeta(day, it, null, 0, 0) }, reducedWeeks, 0, 0, Targets()),
            day,
        )

    private fun ids(t: TodayPlan, s: Section) = t.section(s).map { it.key }.toSet()

    @Test
    fun day1ShowsTuesday17NovembersRows() {
        val t = today(d("2026-11-17"))
        assertEquals(1, t.planDay)
        assertEquals(DayType.WEEKDAY, t.type)
        assertEquals(setOf("T1001", "T1002", "T1003"), ids(t, Section.MAIN))
        assertEquals(setOf("T1004"), ids(t, Section.HEALTH))
        assertEquals(setOf("T1005"), ids(t, Section.PRACTICE)) // practice: its own list, never study
        assertEquals(35, t.minutesPlanned) // accounting 30 + log 5 (comms is at work)
        assertEquals(75, t.daysLeft)
    }

    @Test
    fun aMissedDayMovesTheNextTaskOneSlotLaterWithoutDoublingUp() {
        // Nothing done on Tuesday: on Wednesday accounting shows T1001 (not T1006), only one of it.
        val t = today(d("2026-11-18"))
        val acc = t.main.filter { it.area == "accounting" }
        assertEquals(listOf("T1001"), acc.map { it.key })
        assertEquals(1, acc.single().lateDays)
        assertTrue("T1007" in ids(t, Section.MAIN)) // French week 1 starts on Wednesday
        assertTrue("T1009" in ids(t, Section.MAIN)) // Wednesday's log line
        assertFalse("T1003" in ids(t, Section.MAIN)) // Tuesday's log line doesn't carry over (the Yesterday card has it)
    }

    @Test
    fun aSkippedOldTaskNeverBlocksYouCarryOnFromWhereYouAre() {
        val t = today(d("2026-11-19"), listOf(ev("T1006", d("2026-11-18"))))
        assertEquals(listOf("T1012"), t.main.filter { it.area == "accounting" }.map { it.key })
        assertEquals(listOf("T1001"), t.holes.filter { it.area == "accounting" }.map { it.id })
    }

    @Test
    fun aheadAndDoneTodayAreShownQuietly() {
        val tue = d("2026-11-17")
        val t = today(tue, listOf(ev("T1001", tue)))
        assertTrue(t.main.single { it.key == "T1001" }.done)
        assertFalse(t.entries.any { it.key == "T1006" && it.section == Section.MAIN })
    }

    @Test
    fun fridayIsSapFridayAndWeekendsArePythonCareerRecordingsAndTheReview() {
        val fri = today(d("2026-11-20"))
        assertEquals("T1018", fri.main.first { it.study }.key)
        val sat = today(d("2026-11-21"))
        assertTrue("T1023" in ids(sat, Section.MAIN))
        assertFalse(sat.entries.any { it.area == "accounting" && it.section == Section.MAIN })
        val sun = today(d("2026-11-22"), listOf(ev("T1023", d("2026-11-21"))))
        assertEquals(setOf("T1027", "T1028", "T1029", "T1030", "T1033"), ids(sun, Section.MAIN))
        assertEquals(setOf("T1004", "T1031"), ids(sun, Section.HEALTH)) // the lights-out step (17 Nov) waits until done
    }

    @Test
    fun datedStepsWaitUntilDone() {
        val dec2 = d("2026-12-02")
        val t = today(dec2)
        assertTrue("T1077" in ids(t, Section.MAIN)) // STAR #1 from Mon 30 Nov
        assertTrue("T1078" in ids(t, Section.HEALTH)) // money check #1
        val done = today(dec2, listOf(ev("T1077", d("2026-12-01"))))
        assertFalse(done.entries.any { it.key == "T1077" })
    }

    @Test
    fun restDaysShowRestAndTheMorningPractice() {
        val t = today(d("2026-12-25"))
        assertEquals(DayType.REST, t.type)
        assertEquals(listOf("T1219"), t.main.map { it.key })
        assertEquals(listOf("T1220"), t.practice.map { it.key })
        assertEquals(0, t.target)
    }

    @Test
    fun reducedDayShowsSection14AndPausesTheRest() {
        val wed = d("2026-12-09")
        val t = today(wed, kind = DayKind.REDUCED)
        assertEquals(DayType.REDUCED, t.type)
        val main = ids(t, Section.MAIN)
        assertTrue("T1129" in main) // the day's log line
        assertTrue("red-accounting" in main) // flashcards only
        assertTrue("red-comms" in main)
        assertTrue("red-practice" in ids(t, Section.PRACTICE))
        assertTrue(t.section(Section.PAUSED).any { it.area == "accounting" })
        assertEquals(15, t.target)
        // Tuesday of a reduced week: the real SAP flashcards task stays.
        assertTrue("T1121" in ids(today(d("2026-12-08"), kind = DayKind.REDUCED), Section.MAIN))
    }

    @Test
    fun catchUpDayShowsOverdueWorkFromEveryArea() {
        val t = today(d("2026-11-28"), kind = DayKind.CATCHUP)
        val main = ids(t, Section.MAIN)
        assertTrue("T1001" in main && "T1006" in main) // up to two per sequence
        assertTrue("T1023" in main)
    }

    @Test
    fun twoReducedWeeksInARowShiftLaterDatesByAWeek() {
        val w3 = d("2026-11-30"); val w4 = d("2026-12-07")
        val t = plan.task("T1195")!! // test #1, Mon 21 Dec
        assertEquals(t.day, TodayEngine.effectiveDay(t, setOf(w3)))
        assertEquals(t.day + 7, TodayEngine.effectiveDay(t, setOf(w3, w4)))
    }

    @Test
    fun beforeTheStartShowsAPreview() {
        val t = today(d("2026-11-16"))
        assertTrue(t.entries.all { it.section == Section.PREVIEW })
        assertTrue(t.entries.any { it.key == "T1001" })
        assertTrue(t.entries.first().reason.contains("day 1"))
    }

    @Test
    fun deferredTasksMoveToNotToday() {
        val tue = d("2026-11-17")
        val t = today(tue, listOf(ev("T1001", tue, EventType.LATER)))
        assertTrue("T1001" in ids(t, Section.LATER))
    }
}
