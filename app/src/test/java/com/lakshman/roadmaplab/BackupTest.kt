package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.check
import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.ev
import com.lakshman.roadmaplab.TestData.tick
import com.lakshman.roadmaplab.data.json.LabFileException
import com.lakshman.roadmaplab.data.json.LabJson
import com.lakshman.roadmaplab.data.json.Snapshot
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.DayMeta
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Evidence
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.ReviewLog
import com.lakshman.roadmaplab.domain.Targets
import com.lakshman.roadmaplab.domain.WeeklyReview
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private val mon = d("2026-11-16")

    private fun snapshot() = Snapshot(
        roadmap = null, cardsVersion = "2026-11-14", cards = TestData.cards, taskLogs = emptyList(),
        dayLogs = listOf(DayLog(mon, note = "First day", eveningDone = true, logNumber = 1, finishedAt = 1_700_000_100_000)),
        cardStates = listOf(CardState("AC-01", 2, 1, mon + 2, 0, 3, mon, mon)),
        reviewLogs = listOf(ReviewLog("AC-01", mon, Rating.GOOD, true, true, 1_700_000_050_000)),
        dayPlans = emptyList(),
        settings = AppSettings(onboarded = true, reminderOn = true, reminderHour = 20, reminderMinute = 45, theme = "dark",
            v2Intro = true, targets = Targets(25, 40, 30, 10, 20), cue = "After dinner"),
        events = listOf(ev("T004", mon), ev("T005", mon, EventType.START, 5), ev("T009", mon + 1, independent = true)),
        assessments = listOf(check("T014", mon + 2, 83.0), check(null, mon + 2, 40.0, type = "score")),
        ticks = listOf(tick("sleep7", mon, 0), tick("red-accounting", mon + 1, 15)),
        evidence = listOf(Evidence(4, mon, "evidence", "T007", "line", "Vendor upload", "About 40 rows rejected", "Sorted by blanks",
            "2nd upload: 0 errors", "Check mandatory fields first", "Data checking", "Upload report", 4, true, "", "", "", "", 1, 2)),
        dayMeta = listOf(DayMeta(mon + 3, DayKind.REDUCED, "T019", 5, 6), DayMeta(mon + 4, null, "T024", null, 7)),
        weeklyReviews = listOf(WeeklyReview(mon, mapOf("python" to "All 10 found"), mapOf("python" to "5"), mapOf("python" to "Same load"),
            "Go-live ran late", listOf("Work ran late", "No internet"), "Error report works", "French", "1. RAG 2. Quiz", "reduced",
            "\$140", 99, 100)),
        planVersion = "drc-2026-11-14",
    )

    @Test
    fun backupRoundTripKeepsEverything() {
        val s = snapshot()
        val back = LabJson.decodeBackup(LabJson.encodeBackup(s, "2.0.0", "2026-11-22T21:40:00"))
        assertNull(back.roadmap)
        assertEquals(s.cards, back.cards)
        assertEquals(s.dayLogs, back.dayLogs)
        assertEquals(s.cardStates, back.cardStates)
        assertEquals(s.reviewLogs, back.reviewLogs)
        assertEquals(s.settings, back.settings)
        assertEquals(s.events, back.events)
        assertEquals(s.assessments, back.assessments)
        assertEquals(s.ticks, back.ticks)
        assertEquals(s.evidence, back.evidence)
        assertEquals(s.dayMeta, back.dayMeta)
        assertEquals(s.weeklyReviews, back.weeklyReviews)
        assertEquals("drc-2026-11-14", back.planVersion)
    }

    @Test
    fun aVersion1BackupStillRestoresAndItsEvidenceMovesToTheLog() {
        val v1 = JSONObject(LabJson.encodeBackup(snapshot().copy(evidence = emptyList(), dayLogs = listOf(
            DayLog(mon, evModule = "Vendor", evProblem = "Blank cells", evAction = "pd.isna", evResult = "No crash", evLearning = "NaN is a float",
                finishedAt = 1_700_000_100_000)
        )), "1.1.1", "2026-11-14T20:00:00"))
        v1.put("format", 1)
        for (k in listOf("taskEvents", "assessments", "dayTicks", "evidence", "dayMeta", "weeklyReviews")) v1.remove(k)
        val back = LabJson.decodeBackup(v1.toString())
        val e = back.evidence.single()
        assertEquals("Vendor", e.task); assertEquals("No crash", e.result); assertEquals("NaN is a float", e.lesson)
        assertTrue(back.events.isEmpty())
    }

    @Test
    fun foreignAndNewerFilesAreRejectedWithAClearMessage() {
        var message = ""
        try { LabJson.decodeBackup("{\"hello\": 1}") } catch (e: LabFileException) { message = e.message ?: "" }
        assertTrue(message, message.contains("isn't a Roadmap Lab backup"))
        val newer = JSONObject(LabJson.encodeBackup(snapshot(), "9.0.0", "2027-01-01T00:00:00")).put("format", 3).toString()
        try { LabJson.decodeBackup(newer) } catch (e: LabFileException) { message = e.message ?: "" }
        assertTrue(message, message.contains("newer version"))
    }
}
