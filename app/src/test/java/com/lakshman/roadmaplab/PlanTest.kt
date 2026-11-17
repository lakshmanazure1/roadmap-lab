package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.domain.AREA_ORDER
import com.lakshman.roadmaplab.domain.CATEGORY_ORDER
import com.lakshman.roadmaplab.domain.CardType
import com.lakshman.roadmaplab.domain.CheckSpec
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.PlanCsv
import com.lakshman.roadmaplab.domain.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The app's plan must be the Fresh Start roadmap's CSV: every row, nothing invented, nothing dropped. */
class PlanTest {
    @Test
    fun everyCsvRowIsATask() {
        val rows = PlanCsv.parse(TestData.csv)
        assertEquals(429, rows.size) // header + 428
        assertEquals(428, plan.tasks.size)
        assertEquals("T1001", plan.tasks.first().id)
        assertEquals("T1428", plan.tasks.last().id)
        assertEquals(d("2026-11-17"), plan.start)
        assertFalse(plan.hasLeadIn)
        assertEquals(d("2027-01-31"), plan.end)
        assertEquals(76, plan.totalDays)
        assertEquals(CATEGORY_ORDER, plan.meta.areas.map { it.id })
        assertEquals(10, CATEGORY_ORDER.size)
        // Fields come through exactly, including quoted commas.
        val t = plan.task("T1007")!!
        assertEquals("Week 1 · Greetings and basics: bonjour, merci, s'il vous plaît, excusez-moi, je m'appelle, enchanté", t.learn)
        assertEquals(10, t.minutes)
        assertEquals("6 sentences written", t.doneWhen)
        assertEquals("Validator finds all 10 errors", plan.task("T1027")!!.milestone)
    }

    @Test
    fun categoriesMinutesAndRestDays() {
        val byArea = plan.tasks.groupingBy { it.area }.eachCount()
        assertEquals(mapOf("life" to 76, "practice" to 76, "evidence" to 74, "comms" to 63, "accounting" to 43, "french" to 32,
            "python" to 22, "sap" to 19, "career" to 12, "review" to 11), byArea)
        assertEquals(listOf(d("2026-12-25"), d("2027-01-01")), plan.restDays.keys.sorted())
        assertEquals(TaskKind.REST, plan.task("T1219")!!.kind)
        // Study minutes per week match the roadmap's load check (section 4). Health and practice are not study.
        val study = (1..11).map { w -> plan.tasks.filter { it.week == w && it.study }.sumOf { it.minutes } }
        assertEquals(listOf(445, 500, 520, 515, 500, 430, 460, 500, 515, 500, 510), study)
        assertFalse(plan.task("T1005")!!.study) // practice
        assertFalse(plan.task("T1004")!!.study) // health
    }

    @Test
    fun followTheOrderSequencesUseTheirOwnWeekdays() {
        assertEquals(setOf(1, 2, 3, 4), plan.slotDays["accounting"])
        assertEquals(setOf(5), plan.slotDays["sap"])           // SAP Friday
        assertEquals(setOf(6, 7), plan.slotDays["python"])
        assertEquals(setOf(7), plan.slotDays["career"])
        assertEquals(setOf(7), plan.slotDays["recording"])
        assertEquals(setOf(1, 3, 4), plan.slotDays["french"])
        assertEquals(TaskKind.DAILY, plan.task("T1041")!!.kind) // Tuesday SAP flashcards belong to their day
        assertEquals(TaskKind.CARRY, plan.task("T1162")!!.kind) // the December conversation (a dated step)
        assertEquals(TaskKind.CARRY, plan.task("T1077")!!.kind) // STAR #1
        assertEquals(TaskKind.CARRY, plan.task("T1078")!!.kind) // money check #1
        assertEquals(TaskKind.CARRY, plan.task("T1134")!!.kind) // user training #1
        assertEquals(TaskKind.DAILY, plan.task("T1009")!!.kind) // a daily log line
        assertEquals(TaskKind.DAILY, plan.task("T1011")!!.kind) // daily practice
        assertEquals(AREA_ORDER, plan.meta.areas.map { it.id }.filter { it != "review" })
    }

    @Test
    fun checksComeFromTheDoneWhenColumn() {
        fun score(id: String) = plan.task(id)!!.checks.filterIsInstance<CheckSpec.Score>().single()
        assertEquals(12 to 10, score("T1012").let { it.max to it.target })   // "10 of 12 correct"
        assertEquals(10 to 8, score("T1034").let { it.max to it.target })    // "Quiz 8/10"
        assertEquals(20 to 16, score("T1074").let { it.max to it.target })   // November check
        assertEquals(100 to 80, score("T1327").let { it.max to it.target })  // "80% or better"
        assertEquals(12 to 10, score("T1407").let { it.max to it.target })   // test #2
        assertEquals(10 to 8, score("T1041").let { it.max to it.target })    // SAP flashcards: 8 of 10 without looking
        val test1 = plan.task("T1195")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals("journal", test1.bank); assertEquals(10, test1.count); assertEquals(8, test1.target); assertEquals(20, test1.timedMinutes)
        assertEquals(6, plan.task("T1029")!!.checks.filterIsInstance<CheckSpec.Rubric>().single().target)
        assertEquals(7, plan.task("T1264")!!.checks.filterIsInstance<CheckSpec.Rubric>().single().target)
        // SAP check 1: 25-term glossary quiz 20/25 and the rubric 7/10.
        val sap = plan.task("T1413")!!.checks
        assertEquals(25 to 20, sap.filterIsInstance<CheckSpec.Score>().single().let { it.max to it.target })
        assertEquals(7, sap.filterIsInstance<CheckSpec.Rubric>().single().target)
        // French: this week's 6 + last week's 6 (10/12); the December check 20 words from weeks 1–5 (16/20).
        val thu = plan.task("T1053")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals("french", thu.bank); assertEquals(12, thu.count); assertEquals(10, thu.target); assertEquals(listOf(1, 2), thu.weeks)
        val dec = plan.task("T1214")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals(20, dec.count); assertEquals(16, dec.target); assertEquals(listOf(1, 2, 3, 4, 5), dec.weeks)
        val w6 = plan.task("T1196")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals(20, w6.count); assertEquals(listOf(1, 2, 3, 4, 5), w6.weeks)
        val w11 = plan.task("T1390")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals(listOf(7, 8, 9, 10), w11.weeks)
        val mon = plan.task("T1007")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals(6, mon.count); assertEquals(listOf(1), mon.weeks)
        assertEquals("french-sentences", plan.task("T1402")!!.checks.filterIsInstance<CheckSpec.Quiz>().single().bank)
        assertTrue(plan.task("T1408")!!.checks.any { it is CheckSpec.Demonstrate })
        assertTrue(plan.task("T1077")!!.checks.any { it is CheckSpec.Star })
        assertTrue(plan.task("T1189")!!.checks.any { it is CheckSpec.Outcome }) // LinkedIn message 1 of 4
        assertTrue(plan.task("T1005")!!.checks.isEmpty()) // practice is a tick, not a test
    }

    @Test
    fun frenchWordsMatchTheCsvAndHaveMeanings() {
        assertEquals(54, plan.meta.french.sumOf { it.words.size })
        assertTrue(plan.meta.french.filter { it.week !in setOf(6, 11) }.all { it.words.size == 6 })
        assertTrue(plan.meta.french.flatMap { it.words }.all { it.en.isNotBlank() })
        assertEquals(5, plan.frenchWeek(11)!!.sentences.size) // the introduction
    }

    @Test
    fun theRoadmapNotesAreComplete() {
        val m = plan.meta
        assertEquals((1..11).toList(), m.checkpoints.keys.sorted())
        assertEquals(listOf("Clarity", "Structure", "Accuracy", "Audience", "Delivery"), m.rubric.map { it.name })
        assertEquals("Fresh start · the validator finds all 10 errors", m.weekFocus[1])
        assertTrue(m.reduced.any { it.area == "sap" } && m.reduced.any { it.area == "practice" })
        assertTrue(m.rules.first().startsWith("Fictional data only"))
        assertEquals(7, m.levels.size)
    }

    @Test
    fun titlesAndCardsAreConsistent() {
        assertTrue(plan.tasks.all { it.title.isNotBlank() && it.title.length <= 72 })
        val cardIds = TestData.cards.map { it.id }.toSet()
        for ((card, task) in plan.meta.cardUnlocks) {
            assertTrue(card, card in cardIds)
            assertNotNull(task, plan.task(task))
        }
        assertTrue(TestData.cards.filter { it.type == CardType.MCQ }.all { it.answer in it.options })
        assertTrue(plan.tasks.filter { it.area == "accounting" || it.area == "french" }.all { Days.dow(it.day) in 1..4 })
        assertTrue(plan.tasks.filter { it.area == "sap" }.all { Days.dow(it.day) in setOf(2, 5) })
    }

    @Test
    fun csvWithQuotesCommasAndLineBreaksParses() {
        val rows = PlanCsv.parse("a,b,c\r\n\"x, y\",\"he said \"\"hi\"\"\",\"two\nlines\"\n")
        assertEquals(listOf("x, y", "he said \"hi\"", "two\nlines"), rows[1])
    }
}
