package com.lakshman.roadmaplab.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lakshman.roadmaplab.data.LabRepository
import com.lakshman.roadmaplab.data.SettingsStore
import com.lakshman.roadmaplab.data.db.LabDatabase
import com.lakshman.roadmaplab.data.db.toDomain
import com.lakshman.roadmaplab.data.json.LabJson
import com.lakshman.roadmaplab.domain.Assessment
import com.lakshman.roadmaplab.domain.DayKind
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.EventType
import com.lakshman.roadmaplab.domain.Evidence
import com.lakshman.roadmaplab.domain.Mastery
import com.lakshman.roadmaplab.domain.Progress
import com.lakshman.roadmaplab.domain.WeeklyReview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant

/** Data integrity on the real database code: no duplicates on edit, history kept, backup → restore keeps everything. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RepositoryTest {
    private lateinit var db: LabDatabase
    private lateinit var repo: LabRepository
    private val ctx: Context get() = ApplicationProvider.getApplicationContext()
    private val mon = Days.of("2026-11-16")

    @Before
    fun setUp() {
        Days.clock = Clock.fixed(Instant.parse("2026-11-16T19:00:00Z"), Days.ZONE) // Mon 16 Nov, 20:00 in Kinshasa
        db = Room.inMemoryDatabaseBuilder(ctx, LabDatabase::class.java).allowMainThreadQueries().build()
        repo = LabRepository(ctx, db, SettingsStore(ctx))
        runBlocking { repo.loadPlan() }
    }

    @After
    fun tearDown() {
        db.close()
        Days.clock = Clock.system(Days.ZONE)
    }

    private fun line(id: Long = 0, result: String = "2nd upload: 0 errors") = Evidence(id, mon, "evidence", null, "line", "Vendor upload",
        "About 40 rows rejected", "Sorted by blanks", result, "Check mandatory fields first", "Data checking", "Upload report", 4, false,
        "", "", "", "", 0, 0)

    @Test
    fun editingALogLineNeverDuplicatesAndTicksTheDayOnce() = runBlocking {
        val id = repo.saveEvidence(line())
        repo.saveEvidence(line(id, "2nd upload: 0 errors, 40 rows saved"))
        repo.saveEvidence(line(id, "2nd upload: 0 errors, 40 rows saved (checked)"))
        val all = db.dao().evidence()
        assertEquals(1, all.size)
        assertEquals("2nd upload: 0 errors, 40 rows saved (checked)", all.single().result)
        // Monday's log-line task (T007) is ticked once, not once per save.
        val t007 = db.dao().taskEventsFor("T007")
        assertEquals(1, t007.size)
        assertEquals(Progress.COMPLETED, Mastery.states(t007.map { it.toDomain() })["T007"]!!.progress)
    }

    @Test
    fun undoKeepsHistoryAndTheLatestStateWins() = runBlocking {
        repo.setTask("T004", EventType.DONE, 30)
        repo.setTask("T004", EventType.RESET)
        repo.setTask("T004", EventType.DONE, 25, independent = true)
        val events = db.dao().taskEventsFor("T004").map { it.toDomain() }
        assertEquals(3, events.size)
        val st = Mastery.states(events)["T004"]!!
        assertEquals(Progress.COMPLETED, st.progress)
        assertEquals(25, st.doneMinutes)
        assertTrue(st.independent)
    }

    @Test
    fun weeklyReviewIsOneRowAndSetsNextWeeksLoad() = runBlocking {
        val ws = mon
        val r = WeeklyReview(ws, mapOf("python" to "All 10 found"), emptyMap(), emptyMap(), "", emptyList(), "", "", "", "reduced", "\$100", null, 0)
        repo.saveWeeklyReview(r, complete = false)
        repo.saveWeeklyReview(r, complete = true)
        repo.saveWeeklyReview(r.copy(spending = "\$110"), complete = true)
        assertEquals(1, db.dao().weeklyReviews().size)
        assertEquals("\$110", db.dao().weeklyReviews().single().spending)
        assertEquals(1, db.dao().taskEventsFor("T038").size) // the week-1 review task, ticked once
        val next = (ws + 7..ws + 13).map { db.dao().dayMetaOn(it)?.toDomain()?.kind }
        assertTrue(next.all { it == DayKind.REDUCED })
    }

    @Test(expected = IllegalArgumentException::class)
    fun restDaysMustBePlannedAhead() = runBlocking {
        repo.setDayKind(mon - 1, DayKind.REST)
    }

    @Test
    fun backupThenRestoreKeepsEverythingAndMakesASafetyCopy() = runBlocking {
        repo.setTask("T004", EventType.DONE, 30)
        repo.addAssessment(Assessment(0, "T014", "accounting", "Basics", "score", 10.0, 12.0, true, false, "{}", mon, 0, 0))
        repo.saveEvidence(line())
        repo.tick(mon, "sleep7", true, 0)
        repo.addCard("Accounting", "Who uses accounts?", "Owners, banks, the tax office")
        val text = LabJson.encodeBackup(repo.snapshot(), "2.0.0", "2026-11-16T20:00:00")

        // A fresh phone: empty database, then restore.
        val db2 = Room.inMemoryDatabaseBuilder(ctx, LabDatabase::class.java).allowMainThreadQueries().build()
        val repo2 = LabRepository(ctx, db2, SettingsStore(ctx))
        repo2.restore(LabJson.decodeBackup(text), "2.0.0")
        val a = repo.snapshot()
        val b = repo2.snapshot()
        assertEquals(a.events, b.events)
        assertEquals(a.assessments, b.assessments)
        assertEquals(a.evidence, b.evidence)
        assertEquals(a.ticks, b.ticks)
        assertEquals(a.cards.size, b.cards.size)
        assertTrue(b.cards.any { it.id.startsWith("U-") }) // your own card came back
        assertTrue(repo2.safetyCopies().isNotEmpty())
        db2.close()
    }

    @Test
    fun yourOwnCardsSurviveReloadingThePlan() = runBlocking {
        repo.addCard("French", "« une facture »", "an invoice")
        repo.loadPlan()
        assertTrue(db.dao().cards().any { it.id.startsWith("U-") })
    }

    @Test
    fun startFreshClearsEveryRecordKeepsASafetyCopyAndCanBeUndone() = runBlocking {
        repo.setTask("T004", EventType.DONE, 30)
        repo.saveEvidence(line())
        repo.addAssessment(Assessment(0, "T014", "accounting", "Basics", "score", 10.0, 12.0, true, false, "{}", mon, 0, 0))
        repo.setDayKind(mon + 3, DayKind.REST)
        repo.addCard("Accounting", "Capital is…", "What the owner put in")
        val before = db.dao().taskEvents().size to db.dao().evidence().size
        val bundledCards = db.dao().cards().count { !it.id.startsWith("U-") }

        val copy = repo.startFresh("test")

        assertTrue(copy.exists() && copy.name.contains("fresh-start"))
        assertEquals(0, db.dao().taskEvents().size)
        assertEquals(0, db.dao().evidence().size)
        assertEquals(0, db.dao().assessments().size)
        assertEquals(0, db.dao().dayMeta().size)
        assertEquals(0, db.dao().cardStates().size)
        assertEquals(bundledCards, db.dao().cards().size) // your own card is gone, the bundled deck stays
        val s = repo.settings.current()
        assertEquals(mon, s.freshStartDay)
        assertTrue(s.freshStartAsked)
        assertTrue(repo.latestSafetyCopyLabel()!!.contains("before your fresh start"))

        // Undo: "Bring back" restores exactly what was there.
        repo.restoreLatestSafetyCopy("test")
        assertEquals(before, db.dao().taskEvents().size to db.dao().evidence().size)
        assertEquals(1, db.dao().assessments().size)
        assertEquals(DayKind.REST, db.dao().dayMetaOn(mon + 3)!!.toDomain().kind)
    }

    @Test
    fun theRecapIsLoadedWithThePlanAndTheCsvStaysWhole() = runBlocking {
        val plan = repo.state.first { it.plan != null }.plan!!
        assertEquals(380, plan.csvTasks.size)
        assertEquals(listOf("PR1", "PR2", "PR3", "PR4", "PR5", "PR6", "PR7"), plan.tasks.filter { it.seq == "recap" }.map { it.id })
    }
}
