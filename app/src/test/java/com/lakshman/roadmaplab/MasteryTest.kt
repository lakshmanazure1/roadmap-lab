package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.check
import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Level
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.Progress
import org.junit.Assert.assertEquals
import org.junit.Test

class MasteryTest {
    private val day = d("2026-11-18")
    private fun lvl(events: List<com.lakshman.roadmaplab.domain.ItemEvent>, checks: List<com.lakshman.roadmaplab.domain.Assessment> = emptyList(), area: String = "accounting") =
        Mastery.level(area, Mastery.states(events)["T014"], checks)

    @Test
    fun completionIsNotMastery() {
        assertEquals(Level.NOT_STARTED, lvl(emptyList()))
        assertEquals(Level.IN_PROGRESS, lvl(listOf(ev("T014", day, EventType.START, 10))))
        assertEquals(Level.COMPLETED, lvl(listOf(ev("T014", day))))
        assertEquals(Level.PRACTISED, lvl(listOf(ev("T014", day, independent = true))))
    }

    @Test
    fun checksMoveTheLevelUpOrMarkRevision() {
        val done = listOf(ev("T014", day))
        assertEquals(Level.ASSESSED, lvl(done, listOf(check("T014", day, 83.0)))) // 10 of 12
        assertEquals(Level.NEEDS_REVISION, lvl(done, listOf(check("T014", day, 83.0), check("T014", day + 1, 50.0))))
        // Retaking and passing again clears it, and keeps the history.
        assertEquals(Level.ASSESSED, lvl(done, listOf(check("T014", day, 50.0), check("T014", day + 2, 90.0))))
    }

    @Test
    fun demonstratedAndMastered() {
        val done = listOf(ev("T014", day))
        val pass = check("T014", day, 90.0)
        assertEquals(Level.DEMONSTRATED, lvl(done, listOf(pass, check("T014", day, 100.0, type = "demo", passed = true))))
        // Still passing 14+ days later (and the latest check passed): mastered.
        assertEquals(Level.MASTERED, lvl(done, listOf(pass, check("T014", day, 100.0, type = "demo", passed = true), check("T014", day + 14, 90.0))))
        assertEquals(Level.DEMONSTRATED, lvl(done, listOf(pass, check("T014", day, 100.0, type = "demo", passed = true), check("T014", day + 13, 90.0))))
    }

    @Test
    fun outputAreasStopAtAssessedAndHealthIsCounted() {
        val done = listOf(ev("T014", day))
        assertEquals(Level.ASSESSED, Mastery.level("linkedin", Mastery.states(done)["T014"], listOf(check("T014", day, 100.0, type = "checklist"), check("T014", day + 20, 100.0, type = "checklist"))))
        assertEquals(Level.COMPLETED, Mastery.level("life", Mastery.states(done)["T014"], emptyList()))
    }

    @Test
    fun undoAndSkipAreRecordedNotDeleted() {
        val e = listOf(ev("T014", day), ev("T014", day, EventType.RESET))
        assertEquals(Progress.NOT_STARTED, Mastery.states(e)["T014"]!!.progress)
        assertEquals(Level.SKIPPED, lvl(listOf(ev("T014", day, EventType.SKIP))))
    }

    @Test
    fun weakTopicsAndImprovement() {
        val c = listOf(
            check("A", day, 40.0, topic = "Journal"), check("A", day + 1, 50.0, topic = "Journal"),
            check("A", day + 2, 80.0, topic = "Journal"), check("A", day + 3, 90.0, topic = "Journal"),
            check("B", day, 60.0, topic = "Ledger"),
        )
        val scores = Mastery.topicScores(c)
        assertEquals(listOf("Ledger"), Mastery.weakTopics(c).map { it.topic })
        assertEquals(40.0, scores.single { it.topic == "Journal" }.trend!!, 0.01) // 85 − 45
    }
}
