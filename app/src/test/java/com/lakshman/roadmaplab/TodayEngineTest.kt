package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.TestData.csvPlan as plan
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
    fun day1ShowsMondaysRows() {
        val t = today(d("2026-11-16"))
        assertEquals(1, t.planDay)
        assertEquals(DayType.WEEKDAY, t.type)
        assertEquals(setOf("T004", "T005", "T006", "T007"), ids(t, Section.MAIN))
        assertEquals(setOf("T008"), ids(t, Section.HEALTH))
        assertEquals(45, t.minutesPlanned) // accounting 30 + French 10 + log 5 (comms is at work)
        assertEquals(76, t.daysLeft)
    }

    @Test
    fun aMissedDayMovesTheNextTaskOneSlotLaterWithoutDoublingUp() {
        // Nothing done on Monday: on Tuesday accounting shows T004 (not T009), only one of it.
        val t = today(d("2026-11-17"))
        val acc = t.main.filter { it.area == "accounting" }
        assertEquals(listOf("T004"), acc.map { it.key })
        assertEquals(1, acc.single().lateDays)
        // The daily rows belong to Tuesday itself.
        assertTrue("T012" in ids(t, Section.MAIN)) // Tuesday's log line
        assertFalse("T007" in ids(t, Section.MAIN)) // Monday's log line doesn't carry over
    }

    @Test
    fun aSkippedOldTaskNeverBlocksYouCarryOnFromWhereYouAre() {
        // Tuesday's T009 done, Monday's T004 never ticked: Wednesday shows T014, and T004 is listed quietly as earlier.
        val t = today(d("2026-11-18"), listOf(ev("T009", d("2026-11-17"))))
        assertEquals(listOf("T014"), t.main.filter { it.area == "accounting" }.map { it.key })
        assertEquals(listOf("T004"), t.holes.filter { it.area == "accounting" }.map { it.id })
        // Already ahead: ticking the app steps you've done moves the next one on.
        val sun = today(d("2026-12-06"), listOf(ev("T103", d("2026-11-29")), ev("T138", d("2026-11-29"))))
        assertFalse(sun.entries.any { it.key == "T103" && it.section == Section.MAIN })
        assertTrue(sun.entries.none { it.key == "T033" && it.section == Section.MAIN })
    }

    @Test
    fun aheadAndDoneTodayAreShownQuietly() {
        val mon = d("2026-11-16")
        val t = today(mon, listOf(ev("T004", mon)))
        assertTrue(t.main.single { it.key == "T004" }.done)
        assertFalse(t.entries.any { it.key == "T009" && it.section == Section.MAIN })
    }

    @Test
    fun weekendsArePythonAppRecordingsLinkedInAndTheReview() {
        val sat = today(d("2026-11-21"))
        assertTrue("T029" in ids(sat, Section.MAIN)) // the lead-in T001 was skipped: it never blocks T029
        assertFalse(sat.entries.any { it.area == "accounting" && it.section == Section.MAIN })
        val sun = today(d("2026-11-22"), listOf(ev("T029", d("2026-11-21"))))
        assertEquals(setOf("T032", "T033", "T034", "T035", "T036", "T038"), ids(sun, Section.MAIN))
    }

    @Test
    fun datedStepsWaitUntilDone() {
        val dec2 = d("2026-12-02")
        val t = today(dec2)
        assertTrue("T077" in ids(t, Section.MAIN)) // STAR #1 from Mon 30 Nov
        assertTrue("T078" in ids(t, Section.HEALTH)) // money check #1
        val done = today(dec2, listOf(ev("T077", d("2026-12-01"))))
        assertFalse(done.entries.any { it.key == "T077" })
    }

    @Test
    fun restDaysShowOnlyRest() {
        val t = today(d("2026-12-25"))
        assertEquals(DayType.REST, t.type)
        assertEquals(listOf("T200"), t.entries.map { it.key })
        assertEquals(0, t.target)
    }

    @Test
    fun reducedDayShowsSection13AndPausesTheRest() {
        val wed = d("2026-12-09")
        val t = today(wed, kind = DayKind.REDUCED)
        assertEquals(DayType.REDUCED, t.type)
        val main = ids(t, Section.MAIN)
        assertTrue("T122" in main) // the day's log line
        assertTrue("red-accounting" in main) // flashcards only
        assertTrue("red-comms" in main)
        assertTrue(t.section(Section.PAUSED).any { it.area == "accounting" })
        assertEquals(15, t.target)
    }

    @Test
    fun catchUpDayShowsOverdueWorkFromEveryArea() {
        // Nothing done for a week; Saturday 28 Nov marked as a catch-up day.
        val t = today(d("2026-11-28"), kind = DayKind.CATCHUP)
        val main = ids(t, Section.MAIN)
        assertTrue("T004" in main && "T009" in main) // up to two per sequence
        assertTrue("T029" in main)
    }

    @Test
    fun twoReducedWeeksInARowShiftLaterDatesByAWeek() {
        val w3 = d("2026-11-30"); val w4 = d("2026-12-07")
        val t = plan.task("T164")!! // test #1, Fri 18 Dec
        assertEquals(t.day, TodayEngine.effectiveDay(t, setOf(w3)))
        assertEquals(t.day + 7, TodayEngine.effectiveDay(t, setOf(w3, w4)))
    }

    @Test
    fun beforeTheLeadInShowsAPreview() {
        val t = today(d("2026-11-14"))
        assertTrue(t.entries.all { it.section == Section.PREVIEW })
        assertTrue(t.entries.any { it.key == "T001" } && t.entries.any { it.key == "T004" })
    }

    @Test
    fun deferredTasksMoveToNotToday() {
        val mon = d("2026-11-16")
        val t = today(mon, listOf(ev("T005", mon, EventType.LATER)))
        assertTrue("T005" in ids(t, Section.LATER))
    }
}
