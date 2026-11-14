package com.lakshman.roadmaplab.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lakshman.roadmaplab.data.db.LabDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Proves the upgrade from version 1.x keeps every record: a real version-1 database is created from the
 * saved schema (app/schemas/…/1.json), filled with data, upgraded to version 2 and checked against 2.json.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MigrationTest {
    private val name = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), LabDatabase::class.java,
        listOf(LabDatabase.Migration1To2()), FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun version1RecordsSurviveTheUpgradeAndEvidenceMovesToTheLog() {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO day_logs (day, mood, energy, sleepHours, ifThen, note, evModule, evProblem, evAction, evResult, evLearning, eveningDone, logNumber, finishedAt) " +
                "VALUES (20407, 3, 4, 7.0, '', 'A note', 'Vendor upload', 'About 40 rows rejected', 'Sorted by blanks', '2nd upload: 0 errors', 'Check mandatory fields first', 1, 1, 1700000000000)")
            execSQL("INSERT INTO day_logs (day, mood, energy, sleepHours, ifThen, note, evModule, evProblem, evAction, evResult, evLearning, eveningDone, logNumber, finishedAt) " +
                "VALUES (20408, NULL, NULL, NULL, '', 'Only a note', '', '', '', '', '', 0, NULL, NULL)")
            execSQL("INSERT INTO task_logs (taskId, day, status, minutes, note, createdAt, usedMinimum) VALUES ('A-PY-09', 20407, 'done', 30, '', 1, 0)")
            execSQL("INSERT INTO cards (id, topic, type, prompt, optionsJson, answer, explanation, unlockedBy) VALUES ('AC-01', 'Accounting', 'mcq', 'Q', '[\"a\",\"b\"]', 'a', '', NULL)")
            execSQL("INSERT INTO card_states (cardId, box, mcCorrect, dueDay, lapses, reviews, lastReviewDay, unlockedDay) VALUES ('AC-01', 2, 1, 20410, 0, 3, 20407, 20400)")
            execSQL("INSERT INTO review_logs (cardId, day, rating, correct, asMcq, createdAt) VALUES ('AC-01', 20407, 'GOOD', 1, 1, 5)")
            close()
        }

        val db = helper.runMigrationsAndValidate(name, 2, true)
        db.query("SELECT day, area, kind, task, problem, action, result, lesson FROM evidence").use { c ->
            assertEquals(1, c.count) // only the day that had evidence
            c.moveToFirst()
            assertEquals(20407L, c.getLong(0))
            assertEquals("evidence", c.getString(1))
            assertEquals("line", c.getString(2))
            assertEquals("Vendor upload", c.getString(3))
            assertEquals("About 40 rows rejected", c.getString(4))
            assertEquals("Sorted by blanks", c.getString(5))
            assertEquals("2nd upload: 0 errors", c.getString(6))
            assertEquals("Check mandatory fields first", c.getString(7))
        }
        db.query("SELECT COUNT(*) FROM day_logs").use { c -> c.moveToFirst(); assertEquals(2, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM task_logs").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.query("SELECT box FROM card_states WHERE cardId = 'AC-01'").use { c -> c.moveToFirst(); assertEquals(2, c.getInt(0)) }
        db.close()

        // The app itself opens the upgraded file with its normal builder (no destructive fallback).
        val room = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), LabDatabase::class.java, name)
            .addMigrations(*LabDatabase.MIGRATIONS).build()
        runBlocking {
            val dao = room.dao()
            assertEquals(1, dao.evidence().size)
            assertEquals(1, dao.taskLogs().size)
            assertEquals(1, dao.reviewLogs().size)
            assertEquals(0, dao.taskEvents().size)
        }
        room.close()
    }
}
