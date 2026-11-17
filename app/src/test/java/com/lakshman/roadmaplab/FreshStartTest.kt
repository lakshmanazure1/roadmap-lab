package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.data.LabState
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.Consistency
import com.lakshman.roadmaplab.domain.DayStatus
import com.lakshman.roadmaplab.domain.DayType
import com.lakshman.roadmaplab.domain.ItemEvent
import com.lakshman.roadmaplab.domain.Targets
import com.lakshman.roadmaplab.ui.derive
import com.lakshman.roadmaplab.ui.hasEarlierRecords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Fresh Start plan (Tue 17 Nov = day 1) replaces the Eight-Area plan. Records from the earlier plan are
 * offered a fresh start once (a safety copy is always made first), and days before it are never "missed".
 */
class FreshStartTest {
    private fun state(settings: AppSettings, events: List<ItemEvent> = emptyList(), checks: List<Assessment> = emptyList(), legacy: Int = 0) =
        LabState(
            settings = settings, plan = plan, planError = null, cards = TestData.cards.associateBy { it.id }, cardStates = emptyList(),
            reviewLogs = emptyList(), dayLogs = emptyMap(), events = events, assessments = checks, ticks = emptyList(), evidence = emptyList(),
            dayMeta = emptyMap(), weeklyReviews = emptyMap(), legacyTaskLogs = legacy,
        )

    private val tue = d("2026-11-17")
    private val base = AppSettings(onboarded = true, v2Intro = true)

    @Test
    fun recordsFromTheEightAreaPlanAreOfferedAFreshStartOnce() {
        // Ticks on old task ids (T004…) and the v2.1 fresh start on Mon 16 Nov.
        val old = state(base.copy(freshStartDay = d("2026-11-16"), freshStartAsked = true), listOf(ev("T004", d("2026-11-16"))))
        assertTrue(hasEarlierRecords(old, plan))
        assertTrue(derive(old, tue)!!.offerFreshStart)
        // Answered for this plan ("keep my records" or a fresh start): never asked again.
        val answered = old.copy(settings = old.settings.copy(freshStartPlan = plan.meta.version))
        assertFalse(derive(answered, tue)!!.offerFreshStart)
    }

    @Test
    fun aNewUserOrRecordsOnThisPlanAreNeverAsked() {
        assertFalse(derive(state(base), tue)!!.offerFreshStart)
        assertFalse(derive(state(base, listOf(ev("T1001", tue))), tue)!!.offerFreshStart)
    }

    @Test
    fun day1IsTuesday17November() {
        val fresh = plan.withFreshStart(tue)
        assertEquals(tue, fresh.firstDay)
        assertEquals(1, fresh.weekOf(tue))
        assertEquals(2, fresh.weekOf(d("2026-11-23"))) // weeks run Monday–Sunday, like the CSV
        assertEquals(DayType.NONE, Consistency.dayType(d("2026-11-16"), fresh, null))
    }

    @Test
    fun daysBeforeAFreshStartAreNeverMissed() {
        // Started fresh on Tue 1 Dec: 17–30 Nov had no records, but they count as "nothing scheduled", not as missed days.
        val fresh = plan.withFreshStart(d("2026-12-01"))
        val info = Consistency.evaluate(fresh, emptyMap(), mapOf(d("2026-12-01") to 45), Targets(), d("2026-12-01"))
        assertTrue(info.days.filter { it.day < d("2026-12-01") }.all { it.status == DayStatus.NONE })
        assertEquals(1, info.current)
        assertEquals(100.0, info.rolling7!!, 0.01)
        val plain = Consistency.evaluate(plan, emptyMap(), mapOf(d("2026-12-01") to 45), Targets(), d("2026-12-01"))
        assertTrue(plain.days.any { it.status == DayStatus.MISSED })
    }

    @Test
    fun theCsvIsBundledUnchanged() {
        val lines = TestData.csv.trimEnd().lines()
        assertEquals("task_id,date,weekday,week,area,learn_practise,build_deliver,minutes,done_when,evidence,milestone,status,score,notes",
            lines.first().trimStart('﻿'))
        assertTrue(lines[1].startsWith("T1001,2026-11-17,Tue,1,Accounting,"))
        assertTrue(lines.last().startsWith("T1428,2027-01-31,Sun,11,"))
    }
}
