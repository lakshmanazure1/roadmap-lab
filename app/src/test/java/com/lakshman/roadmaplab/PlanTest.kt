package com.lakshman.roadmaplab

import com.lakshman.roadmaplab.TestData.d
import com.lakshman.roadmaplab.TestData.plan
import com.lakshman.roadmaplab.domain.AREA_ORDER
import com.lakshman.roadmaplab.domain.CardType
import com.lakshman.roadmaplab.domain.CheckSpec
import com.lakshman.roadmaplab.domain.Days
import com.lakshman.roadmaplab.domain.PlanCsv
import com.lakshman.roadmaplab.domain.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The app's plan must be the roadmap's CSV: every row, nothing invented, nothing dropped. */
class PlanTest {
    @Test
    fun everyCsvRowIsATask() {
        val rows = PlanCsv.parse(TestData.csv)
        assertEquals(381, rows.size) // header + 380
        assertEquals(380, plan.csvTasks.size)
        assertEquals("T001", plan.csvTasks.first().id)
        assertEquals("T380", plan.csvTasks.last().id)
        // The app adds only the 7-step Python recap from your fresh start (16 Nov), never inside the CSV.
        assertEquals(387, plan.tasks.size)
        assertEquals((1..7).map { "PR$it" }, plan.tasks.filter { it.seq == "recap" }.map { it.id })
        assertEquals(d("2026-11-15"), plan.leadIn)
        assertEquals(d("2026-11-16"), plan.start)
        assertEquals(d("2027-01-31"), plan.end)
        assertEquals(AREA_ORDER, plan.meta.areas.map { it.id }.filter { it != "review" })
        // Fields come through exactly, including quoted commas.
        val t005 = plan.task("T005")!!
        assertEquals("Week 1 · Greetings and basics: words 1–5: bonjour, merci, s'il vous plaît, excusez-moi, je m'appelle", t005.learn)
        assertEquals(10, t005.minutes)
        assertEquals("5 sentences written", t005.doneWhen)
        assertEquals("Validator update (22 Nov)", plan.task("T032")!!.milestone)
    }

    @Test
    fun areasMinutesAndRestDays() {
        val byArea = plan.tasks.groupingBy { it.area }.eachCount()
        assertEquals(53, byArea["accounting"]) // weekdays 16 Nov – 29 Jan minus 2 rest days
        assertEquals(11, byArea["review"])
        assertEquals(listOf(d("2026-12-25"), d("2027-01-01")), plan.restDays.keys.sorted())
        assertEquals(TaskKind.REST, plan.task("T200")!!.kind)
        // Week 1 study minutes: 5 weekdays × 45 + Sat 125 + Sun 165 = 515 (roadmap section 4).
        val w1 = plan.csvTasks.filter { it.week == 1 && it.study }.sumOf { it.minutes }
        assertEquals(515, w1)
    }

    @Test
    fun followTheOrderSequencesUseTheirOwnWeekdays() {
        assertEquals(setOf(1, 2, 3, 4, 5), plan.slotDays["accounting"])
        assertEquals(setOf(6, 7), plan.slotDays["python"])
        assertEquals(setOf(7), plan.slotDays["app"])
        assertEquals(setOf(7), plan.slotDays["linkedin"])
        assertEquals(setOf(7), plan.slotDays["recording"])
        assertTrue(plan.sequences.values.flatten().none { it.optional }) // the lead-in never blocks the order
        assertEquals(TaskKind.CARRY, plan.task("T077")!!.kind) // STAR #1
        assertEquals(TaskKind.CARRY, plan.task("T078")!!.kind) // money check #1
        assertEquals(TaskKind.CARRY, plan.task("T126")!!.kind) // user training #1
        assertEquals(TaskKind.DAILY, plan.task("T007")!!.kind) // a daily log line
    }

    @Test
    fun checksComeFromTheDoneWhenColumn() {
        fun score(id: String) = plan.task(id)!!.checks.filterIsInstance<CheckSpec.Score>().single()
        assertEquals(12 to 10, score("T014").let { it.max to it.target })   // "10 of 12 correct"
        assertEquals(10 to 8, score("T039").let { it.max to it.target })    // "Quiz 8/10 or better"
        assertEquals(20 to 16, score("T074").let { it.max to it.target })   // "16/20 or better"
        assertEquals(100 to 80, score("T286").let { it.max to it.target })  // "80% or better"
        assertEquals(12 to 10, score("T361").let { it.max to it.target })   // test #2
        assertEquals(20 to 16, score("T169").let { it.max to it.target })   // RAG target 16/20
        val test1 = plan.task("T164")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals("journal", test1.bank); assertEquals(10, test1.count); assertEquals(8, test1.target); assertEquals(20, test1.timedMinutes)
        val rec = plan.task("T034")!!.checks.filterIsInstance<CheckSpec.Rubric>().single()
        assertEquals(6, rec.target)
        assertEquals(7, plan.task("T236")!!.checks.filterIsInstance<CheckSpec.Rubric>().single().target)
        val fri = plan.task("T025")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals("french", fri.bank); assertEquals(10, fri.count); assertEquals(8, fri.target); assertEquals(listOf(1), fri.weeks)
        val nov = plan.task("T061")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals(20, nov.count); assertEquals(16, nov.target); assertEquals(listOf(1, 2), nov.weeks)
        val review = plan.task("T347")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals(listOf(1, 2, 3, 4), review.weeks)
        val wed = plan.task("T015")!!.checks.filterIsInstance<CheckSpec.Quiz>().single()
        assertEquals("french-sentences", wed.bank); assertEquals(4, wed.target)
        assertTrue(plan.task("T077")!!.checks.any { it is CheckSpec.Star })
        assertTrue(plan.task("T070")!!.checks.any { it is CheckSpec.Outcome })
    }

    @Test
    fun frenchWordsMatchTheCsvAndHaveMeanings() {
        assertEquals(100, plan.meta.french.sumOf { it.words.size })
        assertTrue(plan.meta.french.all { it.sentences.size == 5 })
        assertTrue(plan.meta.french.flatMap { it.words }.all { it.en.isNotBlank() })
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
        assertTrue(plan.tasks.filter { it.area == "accounting" || it.area == "french" }.all { Days.dow(it.day) in 1..5 })
    }

    @Test
    fun csvWithQuotesCommasAndLineBreaksParses() {
        val rows = PlanCsv.parse("a,b,c\r\n\"x, y\",\"he said \"\"hi\"\"\",\"two\nlines\"\n")
        assertEquals(listOf("x, y", "he said \"hi\"", "two\nlines"), rows[1])
    }
}
