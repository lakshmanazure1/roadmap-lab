package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.data.json.LabFileException
import com.lakshman.roadmaplab.data.json.LabJson
import com.lakshman.roadmaplab.data.json.Snapshot
import com.lakshman.roadmaplab.domain.AppSettings
import com.lakshman.roadmaplab.domain.CardState
import com.lakshman.roadmaplab.domain.DayLog
import com.lakshman.roadmaplab.domain.DayPlanEntry
import com.lakshman.roadmaplab.domain.Mode
import com.lakshman.roadmaplab.domain.Rating
import com.lakshman.roadmaplab.domain.ReviewLog
import com.lakshman.roadmaplab.domain.TaskLog
import com.lakshman.roadmaplab.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    @Test
    fun backupRoundTripKeepsEverything() {
        val r = TestData.roadmap
        val s = Snapshot(
            roadmap = r,
            cardsVersion = "2026-11-14",
            cards = TestData.cards,
            taskLogs = listOf(TaskLog(7, "A-PY-09", 20407, TaskStatus.PARTLY, 25, "half done", 1_700_000_000_000, true)),
            dayLogs = listOf(DayLog(20407, mood = 4, energy = 2, sleepHours = 6.5f, ifThen = "After dinner, at my desk, I will do Python Day 9.",
                note = "Fixed the NaN bug", evModule = "Vendor", evProblem = "Blank cells", evAction = "pd.isna", evResult = "No crash",
                evLearning = "NaN is a float", eveningDone = true, logNumber = 1, finishedAt = 1_700_000_100_000)),
            cardStates = listOf(CardState("PY-033", 2, 1, 20409, 0, 3, 20407, 20400)),
            reviewLogs = listOf(ReviewLog("PY-033", 20407, Rating.GOOD, true, true, 1_700_000_050_000)),
            dayPlans = listOf(DayPlanEntry(20407, "A-PY-09", 0)),
            settings = AppSettings(onboarded = true, mode = Mode.HEAVY, reducedPlan = true, reminderOn = true,
                reminderHour = 20, reminderMinute = 45, theme = "light", firstUseDay = 20406, lastBackupDay = 20407),
        )
        val text = LabJson.encodeBackup(s, "1.0.0", "2026-11-22T21:40:00")
        val back = LabJson.decodeBackup(text)
        assertEquals(s.roadmap, back.roadmap)
        assertEquals(s.cards, back.cards)
        assertEquals(s.taskLogs, back.taskLogs)
        assertEquals(s.dayLogs, back.dayLogs)
        assertEquals(s.cardStates, back.cardStates)
        assertEquals(s.reviewLogs, back.reviewLogs)
        assertEquals(s.dayPlans, back.dayPlans)
        assertEquals(s.settings, back.settings)
    }

    @Test
    fun foreignFilesAreRejectedWithAClearMessage() {
        var message = ""
        try { LabJson.decodeBackup("{\"hello\": 1}") } catch (e: LabFileException) { message = e.message ?: "" }
        assertTrue(message.contains("isn't a Roadmap Lab backup"))
        try { LabJson.parseRoadmap("not json") } catch (e: LabFileException) { message = e.message ?: "" }
        assertTrue(message.contains("valid JSON"))
    }
}
